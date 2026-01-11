package com.btsearch.service;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.btsearch.model.document.TorrentDocument;
import com.btsearch.model.document.TorrentDocument.FileInfo;
import com.btsearch.model.dto.TorrentMetadata;
import com.btsearch.model.entity.SyncRecord;
import com.btsearch.parser.BencodeParser;
import com.btsearch.repository.SyncRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EsSyncService {

    private static final Logger log = LoggerFactory.getLogger(EsSyncService.class);

    @Autowired
    private JdbcTemplateService jdbcTemplateService;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private BencodeParser bencodeParser;

    @Autowired
    private LanguageDetectionService languageDetectionService;

    @Autowired
    private SyncRecordRepository syncRecordRepository;

    private static final int BATCH_SIZE = 500;
    private static final int QUERY_SIZE = 1000;
    private static final int SLEEP_INTERVAL_MS = 1000;

    public void sync() {
        log.info("Starting ES sync process");

        try {
            SyncRecord record = syncRecordRepository.getSyncRecord();
            if (record == null) {
                log.error("Sync record not found, creating with max_synced_id=0");
                record = new SyncRecord(0L, null);
                syncRecordRepository.save(record);
                record = syncRecordRepository.getSyncRecord();
            }

            long maxSyncedId = record.getMaxSyncedId();
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
                    List<Map<String, Object>> rows = jdbcTemplateService.queryForList(
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

                    for (Map<String, Object> row : rows) {
                        Long id = getLong(row.get("id"));
                        batchMaxId = Math.max(batchMaxId, id);

                        try {
                            TorrentDocument doc = buildDocument(row);
                            if (doc != null) {
                                IndexQuery query = new IndexQuery();
                                query.setId(doc.getInfoHash());
                                query.setObject(doc);
                                indexQueries.add(query);
                            }
                        } catch (Exception e) {
                            log.error("Failed to process record id={}: {}", id, e.getMessage(), e);
                            errorCount++;
                        }
                    }

                    if (!indexQueries.isEmpty()) {
                        elasticsearchTemplate.bulkIndex(indexQueries, TorrentDocument.class);
                        totalSynced += indexQueries.size();
                        log.info("Synced batch: {} records, current_id={}, batch_max_id={}, total_synced={}, errors={}",
                            indexQueries.size(), currentId, batchMaxId, totalSynced, errorCount);
                    }

                    currentId = batchMaxId;

                    if (indexQueries.size() >= BATCH_SIZE) {
                        updateSyncRecord(currentId);
                    }

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

    @Transactional
    public void updateSyncRecord(long maxId) {
        syncRecordRepository.updateSyncRecord(maxId);
    }

    private long getLastId() {
        Long result = jdbcTemplateService.queryForObject(
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
