package com.berdachuk.meteoris.insight.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.berdachuk.meteoris.insight.MeteorisInsightApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = MeteorisInsightApplication.class)
@Testcontainers
@ActiveProfiles({"stub-ai", "test-pgvector"})
class EvaluationModuleTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @Autowired
    EvaluationService evaluationService;

    @Test
    void evaluationModuleProvidesRunner() {
        assertThat(evaluationService).isNotNull();
    }
}
