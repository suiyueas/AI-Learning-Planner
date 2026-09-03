package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.CheckinConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 签到配置表 Repository
 */
@Repository
public interface CheckinConfigRepository extends JpaRepository<CheckinConfig, Long> {

    Optional<CheckinConfig> findByConfigKey(String configKey);

    @Query("SELECT c.configValue FROM CheckinConfig c WHERE c.configKey = :key")
    Optional<String> findConfigValue(@Param("key") String key);

    @Modifying
    @Query("UPDATE CheckinConfig c SET c.configValue = :value WHERE c.configKey = :key")
    int updateConfigValue(@Param("key") String key, @Param("value") String value);
}
