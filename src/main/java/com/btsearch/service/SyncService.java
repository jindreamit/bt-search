package com.btsearch.service;

import com.btsearch.model.document.TorrentDocument;
import com.btsearch.model.dto.TorrentMetadata;
import com.btsearch.parser.BencodeParser;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据同步服务
 * 将MySQL中的种子数据同步到Elasticsearch
 * 支持分阶段测试：1万条、10万条、全量同步
 */
@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private BencodeParser bencodeParser;

    @Value("${bt-search.sync.batch-size:5000}")
    private int batchSize;

    @Value("${bt-search.sync.bulk-size:500}")
    private int bulkSize;

    // 内存中的进度跟踪（不使用Redis）
    private final AtomicLong syncProgress = new AtomicLong(0);
    private volatile boolean isSyncing = false;

    /**
     * 测试同步1万条
     */
    @Async
    public void testSync10k() {
        log.info("Starting 10k test sync...");
        syncWithLimit(10000);
    }

    /**
     * 测试同步10万条
     */
    @Async
    public void testSync100k() {
        log.info("Starting 100k test sync...");
        syncWithLimit(100000);
    }

    /**
     * 全量同步（989万条）
     */
    @Async
    public void fullSync() {
        log.info("Starting full sync...");
        syncWithLimit(-1);  // -1表示全部
    }

    /**
     * 带限制的同步
     *
     * @param limit 最大同步记录数，-1表示全部
     */
    private void syncWithLimit(int limit) {
        // 检查是否已有同步任务在运行
        if (isSyncing) {
            log.warn("Sync already running, skip");
            return;
        }

        isSyncing = true;
        syncProgress.set(0);

        try {
            long startTime = System.currentTimeMillis();
            int offset = 0;
            int processed = 0;
            int errorCount = 0;

            log.info("Starting sync with limit: {}", limit == -1 ? "unlimited" : limit);

            while (true) {
                // 查询数据
                String sql = "SELECT id, infohash, infohashDictionary, seeders, leechers, peers, find_time, lang " +
                            "FROM info_dict ORDER BY id LIMIT ? OFFSET ?";

                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, batchSize, offset);

                if (rows.isEmpty()) {
                    break;
                }

                List<TorrentDocument> documents = new ArrayList<>();

                for (Map<String, Object> row : rows) {
                    try {
                        TorrentDocument doc = parseToDocument(row);
                        if (doc != null) {
                            documents.add(doc);
                        }

                        // 批量提交（每bulkSize条）
                        if (documents.size() >= bulkSize) {
                            elasticsearchOperations.save(documents);
                            documents.clear();
                        }

                        processed++;
                        syncProgress.set(processed);

                        // 检查是否达到测试限制
                        if (limit > 0 && processed >= limit) {
                            // 提交剩余文档
                            if (!documents.isEmpty()) {
                                elasticsearchOperations.save(documents);
                            }
                            log.info("测试同步完成，处理{}条记录", processed);
                            return;
                        }

                    } catch (Exception e) {
                        log.error("解析失败 id={}", row.get("id"), e);
                        errorCount++;
                        if (errorCount > 100) {
                            log.error("错误过多，停止同步");
                            break;
                        }
                    }
                }

                // 提交剩余文档
                if (!documents.isEmpty()) {
                    elasticsearchOperations.save(documents);
                }

                offset += batchSize;
                log.info("已同步{}条记录...", processed);

                // 每处理10000条记录，输出一次统计
                if (processed % 10000 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double rate = processed / (elapsed / 1000.0);
                    log.info("进度: {}条, 速度: {:.2f} 条/秒, 耗时: {}秒", processed, rate, elapsed / 1000);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("同步完成！总计: {}条, 耗时: {}秒, 平均速度: {:.2f} 条/秒",
                    processed, elapsed / 1000, processed / (elapsed / 1000.0));

        } catch (Exception e) {
            log.error("同步失败", e);
        } finally {
            isSyncing = false;
        }
    }

    /**
     * 解析为Elasticsearch文档
     */
    private TorrentDocument parseToDocument(Map<String, Object> row) {
        try {
            // 获取infoHash
            byte[] infohashBytes = (byte[]) row.get("infohash");
            String infoHash = bytesToHex(infohashBytes);

            // 解析bencode
            byte[] dictBytes = (byte[]) row.get("infohashDictionary");
            TorrentMetadata metadata = bencodeParser.parse(dictBytes);

            // 构建文档
            TorrentDocument doc = TorrentDocument.fromMetadata(metadata, infoHash);

            // 设置MySQL中的字段
            Object seeders = row.get("seeders");
            if (seeders instanceof Integer) {
                doc.setSeeders((Integer) seeders);
            }

            Object leechers = row.get("leechers");
            if (leechers instanceof Integer) {
                doc.setLeechers((Integer) leechers);
            }

            Object peers = row.get("peers");
            if (peers instanceof Integer) {
                doc.setPeers((Integer) peers);
            }

            Object findTime = row.get("find_time");
            if (findTime instanceof Long) {
                doc.setCreateTime((Long) findTime);
            }

            Object lang = row.get("lang");
            if (lang instanceof Byte) {
                doc.setLang((Byte) lang);
            }

            return doc;

        } catch (Exception e) {
            log.error("Failed to parse document id={}", row.get("id"), e);
            return null;
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        return Hex.encodeHexString(bytes);
    }

    /**
     * 获取同步进度
     */
    public String getSyncProgress() {
        long progress = syncProgress.get();
        return progress > 0 ? String.valueOf(progress) : null;
    }

    /**
     * 获取ES中的文档数量
     */
    public long getEsDocumentCount() {
        try {
            return elasticsearchOperations.count(
                    org.springframework.data.elasticsearch.core.query.Query.findAll(),
                    TorrentDocument.class
            );
        } catch (Exception e) {
            log.error("Failed to get ES document count", e);
            return -1;
        }
    }
}
