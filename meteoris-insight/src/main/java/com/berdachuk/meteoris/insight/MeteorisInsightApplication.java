package com.berdachuk.meteoris.insight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class MeteorisInsightApplication {

    private static final Logger log = LoggerFactory.getLogger(MeteorisInsightApplication.class);

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(MeteorisInsightApplication.class);
        application.addListeners((ApplicationListener<ApplicationReadyEvent>) MeteorisInsightApplication::logStartupDetails);
        application.run(args);
    }

    private static void logStartupDetails(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String name = env.getProperty("spring.application.name", "meteoris-insight");
        Package pkg = MeteorisInsightApplication.class.getPackage();
        String version = pkg != null && pkg.getImplementationVersion() != null
                ? pkg.getImplementationVersion()
                : "dev";

        String[] active = env.getActiveProfiles();
        String profiles = active.length > 0 ? String.join(", ", active) : "(default)";

        log.info("========================================");
        log.info("  {} ({})", capitalize(name), version);
        log.info("  Meteoris Insight — AI Orchestrator");
        log.info("  Active profiles: {}", profiles);
        log.info("  Java: {} — {}", System.getProperty("java.version"), System.getProperty("java.runtime.name"));
        log.info("  PID: {}", ProcessHandle.current().pid());

        String chatUrl = env.getProperty("spring.ai.custom.chat.base-url", "");
        String embUrl = env.getProperty("spring.ai.custom.embedding.base-url", "");
        String chatModel = env.getProperty("spring.ai.custom.chat.model", "");
        String embModel = env.getProperty("spring.ai.custom.embedding.model", "");
        log.info("  Chat:     base-url={}, model={}", orDash(chatUrl), orDash(chatModel));
        log.info("  Embed:    base-url={}, model={}", orDash(embUrl), orDash(embModel));

        String jdbcUrl = env.getProperty("spring.datasource.url");
        if (jdbcUrl != null && !jdbcUrl.isBlank()) {
            log.info("  Datasource: {}", sanitizeJdbcUrl(jdbcUrl));
        } else {
            log.info("  Datasource: (not configured)");
        }

        String webPort = env.getProperty("local.server.port", env.getProperty("server.port", "8080"));
        log.info("  Local URL: http://localhost:{}", webPort);
        log.info("  Home page: http://localhost:{}/", webPort);
        log.info("  API docs: http://localhost:{}/v3/api-docs", webPort);
        log.info("  Swagger UI: http://localhost:{}/swagger-ui/index.html", webPort);
        log.info("  Health: http://localhost:{}/actuator/health", webPort);
        log.info("========================================");
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String sanitizeJdbcUrl(String url) {
        if (url == null) {
            return "—";
        }
        return url.replaceAll("(?i)(password=)[^&]+", "$1***")
                .replaceAll("(?i)(user=)[^&]+", "$1***");
    }
}
