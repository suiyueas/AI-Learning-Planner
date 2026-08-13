package com.ai.learning.planner.service;

import com.ai.learning.planner.dto.auth.LoginRequest;
import com.ai.learning.planner.dto.auth.RegisterRequest;
import com.ai.learning.planner.entity.User;
import com.ai.learning.planner.repository.UserRepository;
import com.ai.learning.planner.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务
 * 提供用户注册、登录等功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    public Map<String, Object> register(RegisterRequest request) {
        // 校验两次密码一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已被注册");
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("邮箱已被注册");
        }

        // 创建用户
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getUsername())
                .build();
        userRepository.save(user);

        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("role", user.getRole());

        log.info("用户注册成功: {}", user.getUsername());
        return result;
    }

    /**
     * 用户登录
     */
    public Map<String, Object> login(LoginRequest request) {
        // 根据用户名或邮箱查找用户
        User user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() -> new IllegalArgumentException("用户名/邮箱或密码错误"));

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名/邮箱或密码错误");
        }

        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("role", user.getRole());

        log.info("用户登录成功: {}", user.getUsername());
        return result;
    }
}