package com.berdachuk.meteoris.insight.memory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.CreateSessionRequest;
import org.springframework.ai.session.SessionService;
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
    void appendMessagesAndDeleteRoundTrip() {
        var session = sessionService.create(CreateSessionRequest.builder().userId("test-user").build());
        String id = session.id();

        sessionService.appendMessage(id, new UserMessage("hello"));

        assertThat(sessionService.getMessages(id)).hasSize(1);

        sessionService.delete(id);

        assertThat(sessionService.findById(id)).isNull();
    }
}
