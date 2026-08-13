package com.ai.learning.planner.mcp.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 资源模板注册表测试（URI 模板 + 订阅推送）
 */
class ResourceTemplateRegistryTest {

    private ResourceTemplateRegistry registry;
    private Path baseDir;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        registry = new ResourceTemplateRegistry();
        baseDir = Files.createDirectories(tempDir.resolve("project"));
        registry.registerTemplate("file:///project/{path}", "项目文件资源", baseDir);
    }

    @Test
    void listTemplates_containsRegistered() {
        var templates = registry.listTemplates();
        assertEquals(1, templates.size());
        assertTrue(templates.get(0).get("uriTemplate").contains("{path}"));
    }

    @Test
    void readResource_matchesTemplateAndReadsFile() throws Exception {
        Path file = baseDir.resolve("README.md");
        Files.writeString(file, "这是项目说明文档内容");

        var content = registry.readResource("file:///project/README.md");
        assertTrue(content.isPresent());
        assertEquals("这是项目说明文档内容", content.get().content());
        assertTrue(content.get().modifiedAt() > 0);
    }

    @Test
    void readResource_unmatchedUriReturnsEmpty() {
        assertTrue(registry.readResource("file:///other/README.md").isEmpty());
    }

    @Test
    void readResource_missingFileReturnsPlaceholder() {
        var content = registry.readResource("file:///project/not_exist.md");
        assertTrue(content.isPresent());
        assertTrue(content.get().content().contains("资源不存在"));
    }

    @Test
    void subscribe_notifyUpdated_pushesEvent() throws Exception {
        AtomicReference<String> received = new AtomicReference<>();
        boolean subscribed = registry.subscribe("file:///project/config.yaml", received::set);
        assertTrue(subscribed);

        registry.notifyUpdated("file:///project/config.yaml");
        assertEquals("file:///project/config.yaml", received.get());
    }

    @Test
    void subscribe_unmatchedUriRejected() {
        boolean subscribed = registry.subscribe("file:///other/config.yaml", uri -> {});
        assertFalse(subscribed);
    }

    @Test
    void unsubscribe_removesSubscriber() {
        AtomicReference<String> received = new AtomicReference<>();
        var callback = new java.util.function.Consumer<String>() {
            @Override
            public void accept(String uri) {
                received.set(uri);
            }
        };
        registry.subscribe("file:///project/a.txt", callback);
        assertEquals(1, registry.subscriberCount("file:///project/a.txt"));

        registry.unsubscribe("file:///project/a.txt", callback);
        assertEquals(0, registry.subscriberCount("file:///project/a.txt"));

        registry.notifyUpdated("file:///project/a.txt");
        assertNull(received.get());
    }
}
