package com.berdachuk.meteoris.insight.api;

import com.berdachuk.meteoris.insight.api.generated.EvaluationApi;
import com.berdachuk.meteoris.insight.api.generated.model.EvaluationRunRequest;
import com.berdachuk.meteoris.insight.api.generated.model.EvaluationRunResponse;
import com.berdachuk.meteoris.insight.core.IdGenerator;
import com.berdachuk.meteoris.insight.evaluation.EvaluationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvaluationApiController implements EvaluationApi {

    private final EvaluationService evaluationService;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public EvaluationApiController(
            EvaluationService evaluationService, IdGenerator idGenerator, ObjectMapper objectMapper) {
        this.evaluationService = evaluationService;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<EvaluationRunResponse> postEvaluationRun(EvaluationRunRequest evaluationRunRequest) {
        if (evaluationRunRequest.getDataset() == null || evaluationRunRequest.getDataset().isBlank()) {
            throw new IllegalArgumentException("dataset is required");
        }
        try {
            String reportJson = evaluationService.run(
                    evaluationRunRequest.getDataset(), evaluationRunRequest.getProfile());
            JsonNode root = objectMapper.readTree(reportJson);
            int pass = root.path("pass_count").asInt(0);
            int fail = root.path("fail_count").asInt(0);
            EvaluationRunResponse resp = new EvaluationRunResponse();
            resp.setRunId(idGenerator.generateId());
            resp.setDatasetId(root.path("dataset_id").asText(""));
            resp.setDatasetVersion(root.path("dataset_version").asText(""));
            resp.setProfile(root.path("profile").asText(""));
            resp.setPassCount(pass);
            resp.setFailCount(fail);
            resp.setReportJson(reportJson);
            return ResponseEntity.ok(resp);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse evaluation report", e);
        } catch (Exception e) {
            throw new IllegalStateException("Evaluation run failed", e);
        }
    }
}
