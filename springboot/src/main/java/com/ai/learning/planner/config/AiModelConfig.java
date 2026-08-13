package com.ai.learning.planner.config;

import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * AI 模型配置
 * ChatClient 工厂，基于 ModelManager 动态路由到当前模型
 */
@Configuration
@Slf4j
public class AiModelConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ChatClient chatClient(ModelManager modelManager) {
        return ChatClient.builder(modelManager.getCurrentModel())
                .defaultSystem("""
                    你是一个专业的AI学习规划师，名叫"小智"。
                    你的职责：
                    1. 通过苏格拉底式提问引导学习者思考
                    2. 帮助学习者诊断学习水平
                    3. 制定个性化学习路径
                    4. 推荐合适的学习资源
                    5. 追踪学习进度并给予反馈
                    你的特点：
                    - 温和友善，鼓励学习者
                    - 不直接给出答案，而是引导思考
                    - 能够根据学习者的表现动态调整策略
                    - 支持多轮对话，记住上下文
                    请用中文回复，语言要简洁明了，适合学习者理解。
                    """)
                .build();
    }
}
