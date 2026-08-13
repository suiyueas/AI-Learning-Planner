package com.ai.learning.planner.runner;

import com.ai.learning.planner.entity.User;
import com.ai.learning.planner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 默认管理员账户初始化器
 *
 * 功能说明：
 * - 在应用首次启动时创建默认管理员账户
 * - 避免新部署环境无法登录的问题
 * - 仅当管理员账户不存在时创建
 * - 可通过配置 app.init.default-admin-enabled=false（或环境变量 INIT_DEFAULT_ADMIN=false）关闭，
 *   适用于生产环境自建管理员或已有数据迁移的场景
 *
 * 默认账户：
 * - 用户名：admin
 * - 邮箱：admin@example.com
 * - 密码：admin123（生产环境请及时修改）
 *
 * @author AI System
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "app.init.default-admin-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DefaultAdminInitializer implements ApplicationRunner {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@example.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 管理员初始密码（生产环境务必通过环境变量 INIT_ADMIN_PASSWORD 覆盖默认值）
     */
    @Value("${app.init.default-admin-password:${INIT_ADMIN_PASSWORD:admin123}}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        // 检查管理员账户是否已存在
        if (userRepository.existsByUsername(DEFAULT_ADMIN_USERNAME)) {
            // 已存在则补齐管理员角色（历史数据无 role 字段）
            userRepository.findByUsername(DEFAULT_ADMIN_USERNAME).ifPresent(user -> {
                if (!"ADMIN".equals(user.getRole())) {
                    user.setRole("ADMIN");
                    userRepository.save(user);
                    log.info("✅ 已为管理员补齐角色: ADMIN");
                }
            });
            log.debug("管理员账户已存在，跳过创建");
            return;
        }

        // 创建默认管理员账户（密码来自配置 INIT_ADMIN_PASSWORD，未配置时使用默认值并警告）
        User admin = User.builder()
                .username(DEFAULT_ADMIN_USERNAME)
                .email(DEFAULT_ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .nickname("系统管理员")
                .bio("系统默认管理员账户")
                .role("ADMIN")
                .level(99)
                .build();

        userRepository.save(admin);
        log.info("✅ 默认管理员账户创建成功");
        log.warn("⚠️ 请及时修改默认管理员密码！");
        if (DEFAULT_ADMIN_PASSWORD.equals(adminPassword)) {
            log.warn("[DefaultAdminInitializer] ⚠️ 正在使用默认密码 admin123！生产环境请设置环境变量 INIT_ADMIN_PASSWORD 覆盖，并尽快修改密码！");
        }
    }
}