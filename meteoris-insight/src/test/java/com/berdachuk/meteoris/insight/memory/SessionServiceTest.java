package com.berdachuk.meteoris.insight.memory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles({"stub-ai", "test-pgvector"})
class SessionServiceTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @Autowired
    SessionService sessionService;

    @Test
    void crudLifecycle() {
        SessionService.Session s = sessionService.createSession("conv-1");
        assertThat(s.id()).hasSize(24);
        assertThat(s.conversationId()).isEqualTo("conv-1");

        assertThat(sessionService.findById(s.id())).isPresent();
        sessionService.touch(s.id());
        sessionService.recordCompaction(s.id());

        SessionService.Session updated = sessionService.findById(s.id()).orElseThrow();
        assertThat(updated.updatedAt()).isAfter(s.updatedAt());
        assertThat(updated.compactedAt()).isPresent();

        sessionService.delete(s.id());
        assertThat(sessionService.findById(s.id())).isEmpty();
    }
}