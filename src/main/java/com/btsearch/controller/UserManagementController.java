package com.btsearch.controller;

import com.btsearch.dto.UserResponse;
import com.btsearch.model.entity.User;
import com.btsearch.service.UserService;
import com.btsearch.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
public class UserManagementController {

    private static final Logger log = LoggerFactory.getLogger(UserManagementController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return userService.getCurrentUser(token).orElse(null);
    }

    /**
     * 检查是否为管理员
     */
    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equals(user.getRole());
    }

    /**
     * 获取所有用户列表（分页）
     */
    @GetMapping
    public ResponseEntity<?> getUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User currentUser = getCurrentUser(authHeader);
        if (currentUser == null || !isAdmin(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足"));
        }

        List<User> users = userService.findAll(page, size);
        long total = userService.count();

        List<UserResponse> userResponses = users.stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole(),
                        user.getEnabled(),
                        user.getCreatedAt(),
                        user.getUpdatedAt()
                ))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("users", userResponses);
        response.put("total", total);
        response.put("page", page);
        response.put("size", size);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取单个用户信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {

        User currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }

        // 用户只能查看自己的信息，管理员可以查看所有用户
        if (!isAdmin(currentUser) && !currentUser.getId().equals(id)) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足"));
        }

        User user = userService.findById(id)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
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
    }

    /**
     * 更新用户信息（管理员）
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        User currentUser = getCurrentUser(authHeader);
        if (currentUser == null || !isAdmin(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足"));
        }

        try {
            String email = updates.get("email") != null ? updates.get("email").toString() : null;
            String role = updates.get("role") != null ? updates.get("role").toString() : null;
            Boolean enabled = updates.get("enabled") != null ? Boolean.valueOf(updates.get("enabled").toString()) : null;

            User user = userService.updateUser(id, email, role, enabled);

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
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> passwordData) {

        User currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }

        // 用户只能修改自己的密码
        if (!currentUser.getId().equals(id)) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足"));
        }

        try {
            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");

            if (oldPassword == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "参数错误"));
            }

            userService.updatePassword(id, oldPassword, newPassword);
            return ResponseEntity.ok(Map.of("message", "密码修改成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除用户（仅管理员）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {

        User currentUser = getCurrentUser(authHeader);
        if (currentUser == null || !isAdmin(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足"));
        }

        // 不能删除自己
        if (currentUser.getId().equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "不能删除自己"));
        }

        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "用户已删除"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
