package com.btsearch.service;

import com.btsearch.dto.AuthResponse;
import com.btsearch.dto.LoginRequest;
import com.btsearch.dto.RegisterRequest;
import com.btsearch.model.entity.User;
import com.btsearch.repository.UserRepository;
import com.btsearch.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole("USER");
        user.setEnabled(true);

        user = userRepository.save(user);

        // Generate token
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        return new AuthResponse(token, user.getUsername(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        // Find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        // Check if user is enabled
        if (!user.getEnabled()) {
            throw new RuntimeException("用户已被禁用");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // Generate token
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        return new AuthResponse(token, user.getUsername(), user.getRole());
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findAll(int page, int size) {
        // Simple pagination using subList
        List<User> all = userRepository.findAll();
        int fromIndex = page * size;
        if (fromIndex >= all.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + size, all.size());
        return all.subList(fromIndex, toIndex);
    }

    public long count() {
        return userRepository.count();
    }

    @Transactional
    public User updateUser(Long id, String email, String role, Boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (email != null && !email.isBlank()) {
            // Check if email is taken by another user
            userRepository.findByEmail(email).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(id)) {
                    throw new RuntimeException("邮箱已被使用");
                }
            });
            user.setEmail(email);
        }

        if (role != null && (role.equals("USER") || role.equals("ADMIN"))) {
            user.setRole(role);
        }

        if (enabled != null) {
            user.setEnabled(enabled);
        }

        return userRepository.save(user);
    }

    @Transactional
    public void updatePassword(Long id, String oldPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public Optional<User> getCurrentUser(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!jwtUtil.validateToken(token)) {
            return Optional.empty();
        }

        String username = jwtUtil.getUsernameFromToken(token);
        return userRepository.findByUsername(username);
    }
}
