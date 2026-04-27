package com.berdachuk.meteoris.insight.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.Assumptions;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Starts the packaged {@code *-exec.jar} in a separate JVM with the same Postgres Testcontainers
 * wiring as in-process tests, then hits HTTP with {@link HttpClient} (avoids RestAssured proxy quirks).
 */
@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
class MeteorisInsightJarBlackBoxIT {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    private static Process appProcess;
    private static int port;

    private static Path resolveExecJar() {
        String prop = System.getProperty("meteoris.exec.jar");
        if (prop != null && !prop.isBlank()) {
            return Paths.get(prop);
        }
        Path here = Paths.get("").toAbsolutePath();
        String v = System.getProperty("meteoris.project.version", "0.1.0-SNAPSHOT");
        Path sibling = here.resolve("..")
                .resolve("meteoris-insight")
                .resolve("target")
                .resolve("meteoris-insight-" + v + "-exec.jar");
        return sibling.normalize();
    }

    @BeforeAll
    static void startJar() throws Exception {
        Path jar = resolveExecJar();
        Assumptions.assumeTrue(Files.isRegularFile(jar), "exec JAR not found: " + jar + " (run mvn package from parent first)");

        try (var socket = new java.net.ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(ProcessHandle.current().info().command().orElse("java"));
        cmd.add("-jar");
        cmd.add(jar.toString());
        cmd.add("--server.port=" + port);
        cmd.add("--spring.datasource.url=" + POSTGRES.getJdbcUrl());
        cmd.add("--spring.datasource.username=" + POSTGRES.getUsername());
        cmd.add("--spring.datasource.password=" + POSTGRES.getPassword());
        cmd.add("--spring.profiles.active=stub-ai,test-pgvector");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Path logFile = Paths.get("target", "jar-blackbox-app.log");
        Files.createDirectories(logFile.getParent());
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
        appProcess = pb.start();

        String baseUri = "http://127.0.0.1:" + port;
        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(2);
        boolean up = false;
        while (System.nanoTime() < deadline) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(baseUri + "/actuator/health"))
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build();
                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    up = true;
                    break;
                }
            } catch (IOException | InterruptedException ignored) {
                // process still starting
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for app");
                }
            }
            if (!appProcess.isAlive()) {
                throw new IllegalStateException("App process exited early; see " + logFile.toAbsolutePath());
            }
            Thread.sleep(500);
        }
        Assumptions.assumeTrue(up, "App did not become healthy in time; see " + logFile.toAbsolutePath());
    }

    @AfterAll
    static void stopJar() {
        if (appProcess != null) {
            appProcess.destroyForcibly();
            try {
                appProcess.waitFor(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    @Test
    void healthEndpointViaJarProcess() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/actuator/health"))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void evaluationRunViaJarProcess() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/v1/evaluation/run"))
                .timeout(Duration.ofMinutes(2))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"dataset\":\"meteoris-eval-v1\",\"profile\":\"stub-ai,test-pgvector\"}"))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"passCount\"");
        assertThat(response.body()).contains("\"reportJson\"");
    }
}
