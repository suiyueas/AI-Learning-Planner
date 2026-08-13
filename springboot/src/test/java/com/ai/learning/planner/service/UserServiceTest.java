package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.User;
import com.ai.learning.planner.repository.LearningEventRepository;
import com.ai.learning.planner.repository.LearningRecordRepository;
import com.ai.learning.planner.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 * 覆盖密码修改核心逻辑（用户存在性/旧密码校验/新密码持久化）
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private LearningEventRepository learningEventRepository;

    @Mock
    private LearningRecordRepository learningRecordRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository, passwordEncoder, fileUploadService,
                learningEventRepository, learningRecordRepository);
    }

    @Test
    void changePassword_success_encodesAndSaves() {
        User user = User.builder().id(1L).username("testuser").passwordHash("old-encoded").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword1", "old-encoded")).thenReturn(true);
        when(passwordEncoder.encode("newPassword2")).thenReturn("new-encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.changePassword(1L, "oldPassword1", "newPassword2");

        assertEquals("new-encoded", user.getPasswordHash());
        verify(passwordEncoder).encode("newPassword2");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_wrongOldPassword_throws() {
        User user = User.builder().id(1L).passwordHash("old-encoded").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-encoded")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword(1L, "wrong", "newPassword2"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.changePassword(99L, "old", "newPassword2"));
    }
}
