package com.ai.learning.planner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI 学习规划器启动类
 * 基于 Spring AI + RAG + MCP 的个性化学习规划与 AI 对话应用
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
@Slf4j
public class AiLearningPlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiLearningPlannerApplication.class, args);
    }

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> {
            log.info("========================================");
            log.info(" AI 学习规划器应用启动成功！");
            log.info("========================================");
        };
    }
}