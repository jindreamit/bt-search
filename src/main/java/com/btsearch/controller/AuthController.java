package com.btsearch.controller;

import com.btsearch.dto.AuthResponse;
import com.btsearch.dto.LoginRequest;
import com.btsearch.dto.RegisterRequest;
import com.btsearch.model.entity.User;
import com.btsearch.service.UserService;
import com.btsearch.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            log.info("User registration attempt: {}", request.getUsername());
            AuthResponse response = userService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            log.info("User login attempt: {}", request.getUsername());
            AuthResponse response = userService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "未登录"));
            }

            String token = authHeader.substring(7);
            User user = userService.getCurrentUser(token)
                    .orElse(null);

            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Token无效"));
            }

            UserResponse response = new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole(),
                    user.getEnabled(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Get current user failed: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "获取用户信息失败"));
        }
    }

    /**
     * 验证Token是否有效
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.ok(Map.of("valid", false));
            }

            String token = authHeader.substring(7);
            boolean valid = userService.getCurrentUser(token).isPresent();

            return ResponseEntity.ok(Map.of("valid", valid));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("valid", false));
        }
    }
}
