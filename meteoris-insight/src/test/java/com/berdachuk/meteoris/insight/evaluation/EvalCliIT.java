package com.berdachuk.meteoris.insight.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.berdachuk.meteoris.insight.MeteorisInsightApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
        webEnvironment = WebEnvironment.NONE,
        classes = MeteorisInsightApplication.class,
        args = {"--meteoris.eval.dataset=meteoris-eval-v1", "--meteoris.eval.profile=stub-ai,test-pgvector"})
@Testcontainers
@ActiveProfiles({"stub-ai", "test-pgvector", "eval-cli", "test-eval-cli"})
@ExtendWith(OutputCaptureExtension.class)
class EvalCliIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @Test
    void evalCliPrintsReportWithoutExitingJvm(CapturedOutput output) {
        assertThat(output.getOut()).contains("pass_count");
        assertThat(output.getOut()).contains("dataset_id");
    }
}
