package com.berdachuk.meteoris.insight.api;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MeteorisHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(MeteorisHealthIndicator.class);
    private static final Duration LLM_CACHE_UP = Duration.ofMinutes(5);
    private static final Duration LLM_CACHE_DOWN = Duration.ofSeconds(30);

    private final NamedParameterJdbcTemplate jdbc;
    private final Environment environment;
    @Nullable
    private final ChatModel chatModel;
    @Nullable
    private final EmbeddingModel embeddingModel;

    private final AtomicReference<CachedHealthResult> llmHealthCache = new AtomicReference<>();
    private final AtomicReference<CachedHealthResult> embeddingHealthCache = new AtomicReference<>();

    public MeteorisHealthIndicator(NamedParameterJdbcTemplate jdbc,
                                   Environment environment,
                                   @Nullable ChatModel chatModel,
                                   @Nullable EmbeddingModel embeddingModel) {
        this.jdbc = jdbc;
        this.environment = environment;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public Health health() {
        Instant start = Instant.now();
        Map<String, Object> details = new HashMap<>();
        boolean up = true;

        Health dbHealth = checkDatabase();
        details.put("database", dbHealth.getDetails());
        if (!"UP".equals(dbHealth.getStatus().getCode())) {
            up = false;
            log.warn("Meteoris health: database {}", dbHealth.getStatus());
        }

        Health llmHealth = checkLlmModel();
        details.put("chatModel", llmHealth.getDetails());
        if (!"UP".equals(llmHealth.getStatus().getCode())) {
            up = false;
            log.warn("Meteoris health: chatModel {}", llmHealth.getStatus());
        }

        Health embHealth = checkEmbeddingModel();
        details.put("embeddingModel", embHealth.getDetails());
        if (!"UP".equals(embHealth.getStatus().getCode())) {
            up = false;
            log.warn("Meteoris health: embeddingModel {}", embHealth.getStatus());
        }

        details.put("checkDurationMs", Duration.between(start, Instant.now()).toMillis());
        details.put("timestamp", Instant.now().toString());

        String[] activeProfiles = environment.getActiveProfiles();
        details.put("activeProfiles", activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "(default)");

        Health.Builder builder = up ? Health.up() : Health.down();
        builder.withDetails(details);
        return builder.build();
    }

    private Health checkDatabase() {
        try {
            Integer one = jdbc.queryForObject("SELECT 1", Map.of(), Integer.class);
            if (one != null && one == 1) {
                Map<String, Object> d = new HashMap<>();
                d.put("status", "UP");
                d.put("description", "PostgreSQL application database (Flyway + JDBC)");
                return Health.up().withDetails(d).build();
            }
            return Health.down().withDetail("error", "Unexpected SELECT 1 result").build();
        } catch (Exception e) {
            log.debug("Database health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .build();
        }
    }

    private Health checkLlmModel() {
        CachedHealthResult cached = llmHealthCache.get();
        if (cached != null && !cached.isExpired()) {
            return cached.health();
        }
        try {
            if (chatModel == null) {
                Health r = Health.down()
                        .withDetail("error", "ChatModel bean not available")
                        .withDetail("cached", false)
                        .build();
                cacheResult(llmHealthCache, r);
                return r;
            }
            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("modelType", chatModel.getClass().getSimpleName());
            details.put("cached", false);
            details.putAll(extractChatModelConfig());
            Health r = Health.up().withDetails(details).build();
            cacheResult(llmHealthCache, r);
            return r;
        } catch (Exception e) {
            Health r = Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("cached", false)
                    .build();
            cacheResult(llmHealthCache, r);
            return r;
        }
    }

    private Health checkEmbeddingModel() {
        CachedHealthResult cached = embeddingHealthCache.get();
        if (cached != null && !cached.isExpired()) {
            return cached.health();
        }
        try {
            if (embeddingModel == null) {
                Health r = Health.down()
                        .withDetail("error", "EmbeddingModel bean not available")
                        .withDetail("cached", false)
                        .build();
                cacheResult(embeddingHealthCache, r);
                return r;
            }
            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("modelType", embeddingModel.getClass().getSimpleName());
            details.put("dimensions", embeddingModel.dimensions());
            details.put("cached", false);
            details.putAll(extractEmbeddingModelConfig());
            Health r = Health.up().withDetails(details).build();
            cacheResult(embeddingHealthCache, r);
            return r;
        } catch (Exception e) {
            Health r = Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("cached", false)
                    .build();
            cacheResult(embeddingHealthCache, r);
            return r;
        }
    }

    private Map<String, Object> extractChatModelConfig() {
        Map<String, Object> config = new HashMap<>();
        putIfPresent(config, "model", "spring.ai.custom.chat.model");
        putIfPresent(config, "baseUrl", "spring.ai.custom.chat.base-url");
        putIfPresent(config, "completionsPath", "spring.ai.custom.chat.completions-path");
        putIfPresent(config, "temperature", "spring.ai.custom.chat.temperature");
        putIfPresent(config, "maxTokens", "spring.ai.custom.chat.max-tokens");
        config.put("provider", environment.getProperty("spring.ai.custom.chat.provider", "openai-compatible"));
        return config;
    }

    private Map<String, Object> extractEmbeddingModelConfig() {
        Map<String, Object> config = new HashMap<>();
        putIfPresent(config, "model", "spring.ai.custom.embedding.model");
        putIfPresent(config, "baseUrl", "spring.ai.custom.embedding.base-url");
        putIfPresent(config, "dimensions", "spring.ai.custom.embedding.dimensions");
        config.put("provider", environment.getProperty("spring.ai.custom.embedding.provider", "openai-compatible"));
        return config;
    }

    private void putIfPresent(Map<String, Object> config, String key, String property) {
        String value = environment.getProperty(property);
        if (value != null && !value.isBlank()) {
            config.put(key, value);
        }
    }

    private void cacheResult(AtomicReference<CachedHealthResult> cache, Health health) {
        Duration ttl = "UP".equals(health.getStatus().getCode()) ? LLM_CACHE_UP : LLM_CACHE_DOWN;
        cache.set(new CachedHealthResult(health, Instant.now(), ttl));
    }

    private record CachedHealthResult(Health health, Instant timestamp, Duration cacheDuration) {
        boolean isExpired() {
            return Duration.between(timestamp, Instant.now()).compareTo(cacheDuration) >= 0;
        }
    }
}