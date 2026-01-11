package com.btsearch.controller;

import com.btsearch.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理接口控制器
 * 提供数据同步控制等管理功能
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private SyncService syncService;

    /**
     * 测试同步1万条
     */
    @PostMapping("/sync/test/10k")
    public ResponseEntity<?> testSync10k() {
        log.info("Admin request: test sync 10k");
        syncService.testSync10k();

        return ResponseEntity.ok(Map.of(
                "status", "started",
                "limit", 10000,
                "message", "1万条测试同步已启动"
        ));
    }

    /**
     * 测试同步10万条
     */
    @PostMapping("/sync/test/100k")
    public ResponseEntity<?> testSync100k() {
        log.info("Admin request: test sync 100k");
        syncService.testSync100k();

        return ResponseEntity.ok(Map.of(
                "status", "started",
                "limit", 100000,
                "message", "10万条测试同步已启动"
        ));
    }

    /**
     * 全量同步
     */
    @PostMapping("/sync/full")
    public ResponseEntity<?> fullSync() {
        log.info("Admin request: full sync");
        syncService.fullSync();

        return ResponseEntity.ok(Map.of(
                "status", "started",
                "message", "全量同步已启动（989万条数据）"
        ));
    }

    /**
     * 获取同步进度
     */
    @GetMapping("/sync/progress")
    public ResponseEntity<?> getSyncProgress() {
        String progress = syncService.getSyncProgress();
        long documentCount = syncService.getEsDocumentCount();

        Map<String, Object> result = new HashMap<>();
        result.put("processed", progress != null ? progress : "0");
        result.put("documentCount", documentCount);
        result.put("status", progress != null ? "running" : "idle");

        return ResponseEntity.ok(result);
    }

    /**
     * 获取系统状态
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Map<String, Object> status = new HashMap<>();

        long documentCount = syncService.getEsDocumentCount();
        String progress = syncService.getSyncProgress();

        status.put("esDocumentCount", documentCount);
        status.put("syncProgress", progress != null ? progress : "0");
        status.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(status);
    }
}
