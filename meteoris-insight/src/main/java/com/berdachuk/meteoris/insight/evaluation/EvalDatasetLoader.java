package com.berdachuk.meteoris.insight.evaluation;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
public class EvalDatasetLoader {

    public EvalDataset load(String resourcePath) {
        try (InputStream in = new ClassPathResource(resourcePath).getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(in);
            String datasetId = (String) root.get("dataset_id");
            String version = String.valueOf(root.get("version"));
            @SuppressWarnings({"unchecked", "rawtypes"})
            List<Map<String, Object>> rawCases = (List<Map<String, Object>>) root.get("cases");
            List<EvalCase> cases = new ArrayList<>();
            for (Map<String, Object> c : rawCases) {
                cases.add(new EvalCase(
                        (String) c.get("id"),
                        (String) c.get("type"),
                        (String) c.get("question"),
                        (String) c.get("expected_city"),
                        (List<String>) c.get("required_fields"),
                        c.get("min_headlines") instanceof Number n ? n.intValue() : null,
                        c.get("require_source_or_time") instanceof Boolean b ? b : null));
            }
            return new EvalDataset(datasetId, version, cases);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load eval dataset: " + resourcePath, e);
        }
    }
}
