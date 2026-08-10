package com.errorpurifier.domain.cache.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ProjectContextExtractor {

    private static final Pattern JAVA_VERSION = Pattern.compile("(?:sourceCompatibility|languageVersion|JavaVersion\\.VERSION_)(?:\\s*=\\s*|\\.of\\()['\\\"]?([0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPRING_BOOT = Pattern.compile("org\\.springframework\\.boot(?::|['\\\"])", Pattern.CASE_INSENSITIVE);
    private static final Pattern KOTLIN = Pattern.compile("(?:kotlin\\(|org\\.jetbrains\\.kotlin)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRADLE_GROUP = Pattern.compile("(?m)^\\s*group\\s*=\\s*['\"]([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)+)['\"]");

    public Map<String, String> extract(Map<String, String> projectFiles, Map<String, String> declaredTags) {
        Map<String, String> tags = new TreeMap<>();
        if (declaredTags != null) {
            declaredTags.forEach((key, value) -> {
                if (key != null && value != null && !key.isBlank() && !value.isBlank()) {
                    tags.put(key.trim().toLowerCase(Locale.ROOT), value.trim());
                }
            });
        }
        if (projectFiles == null) {
            return tags;
        }

        projectFiles.forEach((path, content) -> inspect(tags, path, content));
        return tags;
    }

    public String asPromptContext(Map<String, String> tags) {
        if (tags.isEmpty()) {
            return "프로젝트 환경 정보가 제공되지 않았습니다.";
        }
        StringBuilder result = new StringBuilder();
        tags.forEach((key, value) -> result.append("- ").append(key).append(": ").append(value).append('\n'));
        return result.toString().trim();
    }

    private void inspect(Map<String, String> tags, String path, String content) {
        if (path == null || content == null) {
            return;
        }
        String fileName = path.toLowerCase(Locale.ROOT);
        if (fileName.endsWith("build.gradle") || fileName.endsWith("build.gradle.kts") || fileName.endsWith("settings.gradle")) {
            tags.putIfAbsent("build-tool", "gradle");
        }
        if (fileName.endsWith("pom.xml")) {
            tags.putIfAbsent("build-tool", "maven");
        }
        if (fileName.endsWith("application.yml") || fileName.endsWith("application.yaml") || fileName.endsWith("application.properties")) {
            tags.putIfAbsent("configuration", "spring-application");
        }
        if (SPRING_BOOT.matcher(content).find()) {
            tags.putIfAbsent("framework", "spring-boot");
        }
        if (KOTLIN.matcher(content).find()) {
            tags.putIfAbsent("language", "kotlin");
        }
        Matcher group = GRADLE_GROUP.matcher(content);
        if (group.find()) {
            tags.putIfAbsent("project-package-prefix", group.group(1));
        }
        Matcher javaVersion = JAVA_VERSION.matcher(content);
        if (javaVersion.find()) {
            tags.putIfAbsent("java", javaVersion.group(1));
        }
    }
}
