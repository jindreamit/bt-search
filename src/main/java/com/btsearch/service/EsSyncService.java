package com.btsearch.service;

import com.btsearch.model.document.TorrentDocument;
import com.btsearch.model.document.TorrentDocument.FileInfo;
import com.btsearch.model.dto.TorrentMetadata;
import com.btsearch.parser.BencodeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EsSyncService {

    private static final Logger log = LoggerFactory.getLogger(EsSyncService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private BencodeParser bencodeParser;

    @Autowired
    private LanguageDetectionService languageDetectionService;

    private static final int BATCH_SIZE = 500;
    private static final int QUERY_SIZE = 1000;
    private static final int SLEEP_INTERVAL_MS = 1000;
    private static final int MAX_BATCH_BYTES = 50 * 1024 * 1024; // 50MB max per batch

    /**
     * 手动触发同步
     */
    public void sync() {
        log.info("Starting ES sync process");

        try {
            // 使用 JdbcTemplate 直接查询，避免 JPA 连接泄漏
            Long maxSyncedId = jdbcTemplate.queryForObject(
                "SELECT max_synced_id FROM torrent_sync_record LIMIT 1",
                Long.class
            );

            if (maxSyncedId == null) {
                log.info("No sync record found, starting from id=0");
                maxSyncedId = 0L;
            }

            long lastId = getLastId();

            if (maxSyncedId >= lastId) {
                log.info("Already up to date, max_synced_id={}, last_id={}", maxSyncedId, lastId);
                return;
            }

            log.info("Syncing from id {} to {}", maxSyncedId + 1, lastId);

            long currentId = maxSyncedId;
            int totalSynced = 0;
            int errorCount = 0;

            while (currentId < lastId) {
                try {
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT id, infohash, infohashDictionary, seeders, leechers, peers, find_time, lang " +
                        "FROM info_dict WHERE id > ? ORDER BY id LIMIT ?",
                        currentId, QUERY_SIZE
                    );

                    if (rows.isEmpty()) {
                        log.warn("No records found for id > {}, checking last id", currentId);
                        break;
                    }

                    List<IndexQuery> indexQueries = new ArrayList<>();
                    long batchMaxId = currentId;
                    long batchBytes = 0;
                    int recordsInBatch = 0;

                    for (Map<String, Object> row : rows) {
                        Long id = getLong(row.get("id"));
                        batchMaxId = Math.max(batchMaxId, id);

                        try {
                            TorrentDocument doc = buildDocument(row);
                            if (doc != null) {
                                // Estimate document size (rough approximation)
                                long docSize = estimateDocumentSize(doc);

                                // If adding this doc would exceed batch limit, flush current batch
                                if (!indexQueries.isEmpty() && batchBytes + docSize > MAX_BATCH_BYTES) {
                                    elasticsearchTemplate.bulkIndex(indexQueries, TorrentDocument.class);
                                    totalSynced += indexQueries.size();
                                    log.info("Flushed batch: {} records, {} bytes, current_id={}, total_synced={}",
                                        indexQueries.size(), formatBytes(batchBytes), currentId, totalSynced);
                                    indexQueries.clear();
                                    batchBytes = 0;
                                    recordsInBatch = 0;
                                }

                                IndexQuery query = new IndexQuery();
                                query.setId(doc.getInfoHash());
                                query.setObject(doc);
                                indexQueries.add(query);
                                batchBytes += docSize;
                                recordsInBatch++;

                                // Update sync record periodically
                                if (recordsInBatch >= BATCH_SIZE) {
                                    updateSyncRecord(currentId);
                                    recordsInBatch = 0;
                                }
                            }
                        } catch (Exception e) {
                            log.error("Failed to process record id={}: {}", id, e.getMessage(), e);
                            errorCount++;
                        }
                    }

                    // Flush remaining documents
                    if (!indexQueries.isEmpty()) {
                        elasticsearchTemplate.bulkIndex(indexQueries, TorrentDocument.class);
                        totalSynced += indexQueries.size();
                        log.info("Flushed final batch: {} records, {} bytes, current_id={}, batch_max_id={}, total_synced={}, errors={}",
                            indexQueries.size(), formatBytes(batchBytes), currentId, batchMaxId, totalSynced, errorCount);
                    }

                    currentId = batchMaxId;
                    updateSyncRecord(currentId);
                    Thread.sleep(SLEEP_INTERVAL_MS);

                } catch (Exception e) {
                    log.error("Error during sync at id={}: {}", currentId, e.getMessage(), e);
                    errorCount++;
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            updateSyncRecord(currentId);
            log.info("Sync completed: total_synced={}, errors={}, final_id={}", totalSynced, errorCount, currentId);

        } catch (Exception e) {
            log.error("Fatal error during sync: {}", e.getMessage(), e);
        }
    }

    /**
     * 更新同步记录 - 使用 JdbcTemplate 直接执行，避免 JPA 事务连接泄漏
     */
    public void updateSyncRecord(long maxId) {
        try {
            // 使用 id=1 作为固定主键，确保只保留一条记录
            int updated = jdbcTemplate.update(
                "INSERT INTO torrent_sync_record (id, max_synced_id, last_sync_time) " +
                "VALUES (1, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE max_synced_id = VALUES(max_synced_id), last_sync_time = VALUES(last_sync_time)",
                maxId
            );
            log.debug("Updated sync record: maxId={}, affected={}", maxId, updated);
        } catch (Exception e) {
            log.error("Failed to update sync record for maxId={}", maxId, e);
        }
    }

    private long getLastId() {
        Long result = jdbcTemplate.queryForObject(
            "SELECT MAX(id) FROM info_dict", Long.class
        );
        return result != null ? result : 0L;
    }

    private TorrentDocument buildDocument(Map<String, Object> row) {
        String infoHash = getInfoHash(row.get("infohash"));
        byte[] dictBytes = (byte[]) row.get("infohashDictionary");

        if (dictBytes == null || dictBytes.length == 0) {
            return null;
        }

        try {
            TorrentDocument doc = new TorrentDocument();
            doc.setInfoHash(infoHash);
            doc.setSeeders(getInteger(row.get("seeders")));
            doc.setLeechers(getInteger(row.get("leechers")));
            doc.setPeers(getInteger(row.get("peers")));

            Long findTime = getLong(row.get("find_time"));
            if (findTime != null) {
                doc.setCreateTime(findTime);
            }

            TorrentMetadata metadata = bencodeParser.parse(dictBytes);
            doc.setName(metadata.getName());
            doc.setSize(metadata.getSize());
            doc.setFiles(metadata.getFiles());

            // Convert Metadata FileInfo to Document FileInfo
            if (metadata.getFileList() != null) {
                List<TorrentDocument.FileInfo> fileInfoList = new ArrayList<>();
                for (TorrentMetadata.FileInfo metaFileInfo : metadata.getFileList()) {
                    TorrentDocument.FileInfo docFileInfo = new TorrentDocument.FileInfo();
                    docFileInfo.setPath(metaFileInfo.getPath());
                    docFileInfo.setSize(metaFileInfo.getSize());
                    fileInfoList.add(docFileInfo);
                }
                doc.setFileList(fileInfoList);
            }

            java.util.Set<LanguageDetectionService.Language> languages =
                languageDetectionService.detectLanguages(metadata.getName());
            doc.setDetectedLanguages(languages.stream()
                .map(LanguageDetectionService.Language::getCode)
                .collect(java.util.stream.Collectors.toList()));

            doc.setMagnetUri(buildMagnetUri(infoHash, metadata.getName()));

            return doc;

        } catch (Exception e) {
            throw new RuntimeException("Failed to build document for infohash=" + infoHash, e);
        }
    }

    private String buildMagnetUri(String infoHash, String name) {
        return String.format("magnet:?xt=urn:btih:%s&dn=%s",
            infoHash,
            java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    private Integer getInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Number) return ((Number) value).intValue();
        return null;
    }

    private Long getLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return null;
    }

    private String getInfoHash(Object value) {
        if (value == null) return null;
        if (value instanceof String) return (String) value;
        if (value instanceof byte[]) {
            return bytesToHex((byte[]) value);
        }
        return null;
    }

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    /**
     * 估算文档大小（字节数）用于批量控制
     */
    private long estimateDocumentSize(TorrentDocument doc) {
        long size = 0;
        // Base document overhead
        size += 100;

        // Info hash
        size += doc.getInfoHash() != null ? doc.getInfoHash().length() * 2 : 40;

        // Name
        size += doc.getName() != null ? doc.getName().length() * 3 : 100;

        // Magnet URI
        size += doc.getMagnetUri() != null ? doc.getMagnetUri().length() * 2 : 200;

        // File list (major contributor)
        if (doc.getFileList() != null) {
            for (FileInfo file : doc.getFileList()) {
                size += file.getPath() != null ? file.getPath().length() * 3 : 100;
                size += 20; // size field overhead
            }
        }

        // Other fields
        size += 50; // metadata overhead

        return size;
    }

    /**
     * 格式化字节数为可读格式
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
