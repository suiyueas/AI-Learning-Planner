package com.ai.learning.planner.service;

import com.ai.learning.planner.dto.auth.LoginRequest;
import com.ai.learning.planner.dto.auth.RegisterRequest;
import com.ai.learning.planner.entity.User;
import com.ai.learning.planner.repository.UserRepository;
import com.ai.learning.planner.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 认证服务单元测试
 * 覆盖注册（密码一致性/用户名邮箱查重/成功）与登录（成功/密码错误/用户不存在）
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil);
    }

    private RegisterRequest buildRegisterRequest(String password, String confirm) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword(password);
        request.setConfirmPassword(confirm);
        return request;
    }

    @Test
    void register_passwordMismatch_throwsAndDoesNotSave() {
        var request = buildRegisterRequest("password123", "different123");

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_usernameExists_throws() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        var request = buildRegisterRequest("password123", "password123");

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_emailExists_throws() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        var request = buildRegisterRequest("password123", "password123");

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void register_success_encodesPasswordSavesAndReturnsToken() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(jwtUtil.generateToken(1L, "testuser")).thenReturn("jwt-token");

        Map<String, Object> result = authService.register(buildRegisterRequest("password123", "password123"));

        assertEquals("jwt-token", result.get("token"));
        assertEquals(1L, result.get("userId"));
        assertEquals("testuser", result.get("username"));
        assertEquals("USER", result.get("role"));
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void login_success_returnsUserInfoWithToken() {
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encoded-password")
                .nickname("测试用户")
                .role("USER")
                .build();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "testuser")).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        Map<String, Object> result = authService.login(request);

        assertEquals("jwt-token", result.get("token"));
        assertEquals(1L, result.get("userId"));
        assertEquals("测试用户", result.get("nickname"));
        assertEquals("USER", result.get("role"));
    }

    @Test
    void login_wrongPassword_throws() {
        User user = User.builder().id(1L).username("testuser").passwordHash("encoded-password").build();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrong");

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void login_userNotFound_throws() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nobody")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setUsername("nobody");
        request.setPassword("whatever");

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void login_byEmail_success() {
        User user = User.builder().id(2L).username("testuser").email("test@example.com")
                .passwordHash("encoded-password").build();
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken(anyLong(), anyString())).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest();
        request.setUsername("test@example.com");
        request.setPassword("password123");
        Map<String, Object> result = authService.login(request);

        assertEquals(2L, result.get("userId"));
    }
}
