package com.btsearch.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 提供 token 验证接口
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /**
     * 验证 token
     * 该接口需要 TokenInterceptor 拦截验证
     * 如果能正常返回，说明 token 有效
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verify() {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }
}
