package com.ai.learning.planner.mcp.resource;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP 资源模板注册表（resources/templates 接口）
 * 支持 URI 模板（如 file:///project/{path}）与资源变更订阅（Subscription），
 * 文件变动时通过回调（SSE 推送 updated 事件）通知订阅者。
 */
@Slf4j
public class ResourceTemplateRegistry {

    /** 模板模式（支持 {var} 占位符） */
    private record Template(String uriTemplate, String description, Path basePath) {

        Pattern compilePattern() {
            StringBuilder regex = new StringBuilder("^");
            String template = uriTemplate.replace(".", "\\.").replace("/", "\\/");
            Matcher m = Pattern.compile("\\{(\\w+)}").matcher(template);
            while (m.find()) {
                template = template.replace("{" + m.group(1) + "}", "([^/]+)");
            }
            regex.append(template).append("$");
            return Pattern.compile(regex.toString());
        }
    }

    /** 已注册模板列表 */
    private final List<Template> templates = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** 订阅者（uri -> 回调集合） */
    private final Map<String, Set<Consumer<String>>> subscribers = new ConcurrentHashMap<>();

    /**
     * 注册资源模板
     *
     * @param uriTemplate URI 模板（如 file:///project/{path}）
     * @param description 模板描述
     * @param basePath    模板对应的本地文件根路径（用于资源解析与文件监听）
     */
    public void registerTemplate(String uriTemplate, String description, Path basePath) {
        templates.add(new Template(uriTemplate, description, basePath));
        log.info("[ResourceTemplateRegistry] 注册模板: {}（{}）", uriTemplate, description);
    }

    /**
     * 列出所有已注册模板（resources/templates/list）
     */
    public List<Map<String, String>> listTemplates() {
        return templates.stream()
                .map(t -> Map.of(
                        "uriTemplate", t.uriTemplate(),
                        "description", t.description() == null ? "" : t.description()
                ))
                .toList();
    }

    /**
     * 匹配 URI 并解析资源内容（resources/read）
     *
     * @param uri 实际 URI（如 file:///project/README.md）
     * @return 解析到的资源内容
     */
    public Optional<ResourceContent> readResource(String uri) {
        for (Template t : templates) {
            Matcher m = t.compilePattern().matcher(uri);
            if (m.matches()) {
                try {
                    Path resolved = t.basePath().resolve(m.group(1));
                    if (Files.exists(resolved) && Files.isRegularFile(resolved)) {
                        String content = Files.readString(resolved);
                        return Optional.of(new ResourceContent(uri, content, Files.getLastModifiedTime(resolved).toMillis()));
                    }
                    return Optional.of(new ResourceContent(uri, "[资源不存在: " + resolved + "]", 0));
                } catch (Exception e) {
                    log.warn("[ResourceTemplateRegistry] 读取资源失败: {} - {}", uri, e.getMessage());
                    return Optional.of(new ResourceContent(uri, "[读取失败: " + e.getMessage() + "]", 0));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 订阅资源变更（resources/subscribe）
     * 当文件变动时通过回调推送 updated 事件
     *
     * @param uri      订阅的 URI
     * @param callback 变更回调（SSE 推送 updated 事件）
     * @return 订阅是否成功（模板已匹配才可订阅）
     */
    public boolean subscribe(String uri, Consumer<String> callback) {
        boolean matched = templates.stream().anyMatch(t -> t.compilePattern().matcher(uri).matches());
        if (!matched) {
            log.warn("[ResourceTemplateRegistry] 订阅失败：URI 未匹配任何模板: {}", uri);
            return false;
        }
        subscribers.computeIfAbsent(uri, k -> new CopyOnWriteArraySet<>()).add(callback);
        log.info("[ResourceTemplateRegistry] 订阅资源: {}", uri);
        return true;
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(String uri, Consumer<String> callback) {
        Set<Consumer<String>> set = subscribers.get(uri);
        if (set != null) {
            set.remove(callback);
        }
    }

    /**
     * 模拟文件变更：推送 updated 事件给该 URI 的所有订阅者
     *
     * @param uri 发生变更的资源 URI
     */
    public void notifyUpdated(String uri) {
        Set<Consumer<String>> set = subscribers.get(uri);
        if (set == null || set.isEmpty()) {
            log.debug("[ResourceTemplateRegistry] 资源 {} 变更，但无订阅者", uri);
            return;
        }
        set.forEach(c -> {
            try {
                c.accept(uri); // SSE 推送 updated 事件
            } catch (Exception e) {
                log.warn("[ResourceTemplateRegistry] 推送 updated 事件失败: {}", e.getMessage());
            }
        });
        log.info("[ResourceTemplateRegistry] 资源 {} 变更，已推送 updated 事件给 {} 个订阅者", uri, set.size());
    }

    /**
     * 获取当前订阅数
     */
    public int subscriberCount(String uri) {
        Set<Consumer<String>> set = subscribers.get(uri);
        return set == null ? 0 : set.size();
    }

    /**
     * 资源内容
     *
     * @param uri        资源 URI
     * @param content    内容
     * @param modifiedAt 最后修改时间戳（毫秒）
     */
    public record ResourceContent(String uri, String content, long modifiedAt) {
    }
}
