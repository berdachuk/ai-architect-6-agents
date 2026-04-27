package com.berdachuk.meteoris.insight.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles({"stub-ai", "test-pgvector"})
class MeteorisInsightE2EIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    /** Does not throw on 4xx/5xx so callers can assert status and body (e.g. Problem+JSON). */
    private final RestTemplate http = createLenientRestTemplate();

    private static RestTemplate createLenientRestTemplate() {
        RestTemplate t = new RestTemplate();
        t.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }
        });
        return t;
    }

    @Value("${local.server.port}")
    private int port;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void actuatorHealthIsUp() {
        ResponseEntity<String> response = http.getForEntity(baseUrl() + "/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void postNewChatSessionReturnsSessionId() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);
        ResponseEntity<String> response = http.postForEntity(baseUrl() + "/api/v1/chat/session", entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(Pattern.compile("\"sessionId\"\\s*:\\s*\"([0-9a-f]{24})\"").matcher(response.getBody()).find())
                .isTrue();
    }

    @Test
    void chatMessageStubWeatherFlow() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"message\":\"What is the current weather in Brest, Belarus?\"}";
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        ResponseEntity<String> response = http.postForEntity(baseUrl() + "/api/v1/chat/messages", entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).contains("COMPLETE");
    }

    @Test
    void evaluationRunStubProfile() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"dataset\":\"meteoris-eval-v1\",\"profile\":\"stub-ai,test-pgvector\"}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = http.postForEntity(baseUrl() + "/api/v1/evaluation/run", entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String rb = response.getBody();
        assertThat(rb).contains("\"passCount\"");
        assertThat(rb).contains("\"reportJson\"");
    }

    @Test
    void evaluationRunBlankDatasetReturns422ProblemJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.parseMediaType("application/problem+json")));
        HttpEntity<String> entity = new HttpEntity<>("{\"dataset\":\"  \",\"profile\":\"stub-ai\"}", headers);
        ResponseEntity<String> response = http.postForEntity(baseUrl() + "/api/v1/evaluation/run", entity, String.class);
        // Spring Framework 6.2+ uses 422 UNPROCESSABLE_CONTENT (RFC 9110); numeric code is still 422.
        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString()).contains("application/problem+json");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"status\":422");
        assertThat(response.getBody()).contains("\"detail\"");
    }
}
