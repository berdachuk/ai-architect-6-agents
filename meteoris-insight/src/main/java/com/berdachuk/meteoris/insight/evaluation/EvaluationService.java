package com.berdachuk.meteoris.insight.evaluation;

import com.berdachuk.meteoris.insight.agent.api.ChatTurnResult;
import com.berdachuk.meteoris.insight.agent.api.ChatOrchestration;
import com.berdachuk.meteoris.insight.core.IdGenerator;
import com.berdachuk.meteoris.insight.memory.TodoStateStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class EvaluationService {

    private static final String DEFAULT_RESOURCE = "eval/meteoris-eval-v1.yaml";

    private final EvalDatasetLoader loader;
    private final ChatOrchestration orchestrationService;
    private final IdGenerator idGenerator;
    private final TodoStateStore todoStateStore;
    private final Environment environment;
    private final ObjectMapper objectMapper;

    public EvaluationService(
            EvalDatasetLoader loader,
            ChatOrchestration orchestrationService,
            IdGenerator idGenerator,
            TodoStateStore todoStateStore,
            Environment environment,
            ObjectMapper objectMapper) {
        this.loader = loader;
        this.orchestrationService = orchestrationService;
        this.idGenerator = idGenerator;
        this.todoStateStore = todoStateStore;
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    public String run(String datasetKey, String profileOverride) throws JsonProcessingException {
        String resource = mapDatasetKey(datasetKey);
        EvalDataset dataset = loader.load(resource);
        String profile =
                profileOverride == null || profileOverride.isBlank()
                        ? String.join(",", environment.getActiveProfiles())
                        : profileOverride;

        List<Map<String, Object>> rows = new ArrayList<>();
        int pass = 0;
        int fail = 0;
        for (EvalCase c : dataset.cases()) {
            String sessionId = idGenerator.generateId();
            try {
                ChatTurnResult turn = orchestrationService.exchange(sessionId, c.question());
                String answer =
                        turn.status() == ChatTurnResult.Status.COMPLETE && turn.reply() != null ? turn.reply() : "";
                List<String> reasons = new ArrayList<>();
                boolean ok = EvalScorer.score(c, answer, reasons);
                if (ok) {
                    pass++;
                } else {
                    fail++;
                }
                rows.add(Map.of(
                        "case_id",
                        c.id(),
                        "pass",
                        ok,
                        "reason_codes",
                        reasons,
                        "answer_preview",
                        answer.length() > 240 ? answer.substring(0, 240) : answer));
            } finally {
                todoStateStore.clear(sessionId);
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("dataset_id", dataset.datasetId());
        report.put("dataset_version", dataset.version());
        report.put("profile", profile);
        report.put("generated_at", Instant.now().toString());
        report.put("pass_count", pass);
        report.put("fail_count", fail);
        report.put("cases", rows);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
    }

    private static String mapDatasetKey(String datasetKey) {
        if (datasetKey == null || datasetKey.isBlank() || "meteoris-eval-v1".equals(datasetKey)) {
            return DEFAULT_RESOURCE;
        }
        if (datasetKey.endsWith(".yaml") || datasetKey.endsWith(".yml")) {
            return datasetKey.startsWith("eval/") ? datasetKey : "eval/" + datasetKey;
        }
        return "eval/" + datasetKey + ".yaml";
    }
}
