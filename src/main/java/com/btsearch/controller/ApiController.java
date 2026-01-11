package com.btsearch.controller;

import com.btsearch.model.document.TorrentDocument;
import com.btsearch.model.dto.SearchRequest;
import com.btsearch.model.dto.SearchResult;
import com.btsearch.service.SearchService;
import com.btsearch.service.SyncService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * RESTful API控制器
 * 提供搜索、种子详情、统计等API
 */
@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    private SearchService searchService;

    @Autowired
    private SyncService syncService;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    /**
     * 搜索接口
     *
     * @param request 搜索请求
     * @return 搜索结果
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(@Valid SearchRequest request) {
        log.info("Search request: keyword='{}', page={}, size={}, limitedSize={}, sort={}",
                request.getKeyword(), request.getPage(), request.getSize(), request.getLimitedSize(), request.getSort());

        SearchResult result = searchService.search(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 种子详情接口
     *
     * @param infoHash InfoHash
     * @return 种子详情
     */
    @GetMapping("/torrents/{infoHash}")
    public ResponseEntity<?> getTorrent(@PathVariable String infoHash) {
        try {
            TorrentDocument doc = elasticsearchOperations.get(
                    infoHash,
                    TorrentDocument.class
            );

            if (doc == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            log.error("Failed to get torrent: {}", infoHash, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 统计信息接口
     */
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            long totalTorrents = syncService.getEsDocumentCount();
            stats.put("totalTorrents", totalTorrents);

            // 这里可以添加更多统计信息
            stats.put("syncProgress", syncService.getSyncProgress());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}
