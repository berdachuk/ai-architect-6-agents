package com.berdachuk.meteoris.insight.agent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
public class SkillsTools {

    private static final String SKILLS_RESOURCE_PATTERN = "classpath:/skills/*/SKILL.md";

    private final ResourceLoader resourceLoader;
    private final Map<String, SkillMetadata> skillCatalog = new HashMap<>();

    public SkillsTools(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void discoverSkills() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(SKILLS_RESOURCE_PATTERN);
        Yaml yaml = new Yaml();

        for (Resource resource : resources) {
            String path = resource.getURL().getPath();
            String skillId = extractSkillId(path);

            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            SkillMetadata metadata = parseFrontMatter(content, skillId, yaml);
            skillCatalog.put(skillId, metadata);
        }
    }

    private String extractSkillId(String path) {
        int skillsIdx = path.lastIndexOf("/skills/");
        if (skillsIdx == -1) return path;
        String remainder = path.substring(skillsIdx + "/skills/".length());
        int slashIdx = remainder.indexOf('/');
        return slashIdx > 0 ? remainder.substring(0, slashIdx) : remainder;
    }

    private SkillMetadata parseFrontMatter(String content, String fallbackId, Yaml yaml) {
        String frontMatter = extractFrontMatter(content);
        if (frontMatter != null) {
            try {
                Map<String, Object> parsed = yaml.load(frontMatter);
                String name = parsed != null ? (String) parsed.get("name") : null;
                String description = parsed != null ? (String) parsed.get("description") : null;
                if (name != null || description != null) {
                    return new SkillMetadata(
                            name != null ? name : fallbackId,
                            description != null ? description : "");
                }
            } catch (Exception ignored) {
            }
        }
        return new SkillMetadata(fallbackId, "");
    }

    private String extractFrontMatter(String content) {
        String marker = "---";
        int start = content.indexOf(marker);
        if (start == -1) return null;
        int end = content.indexOf(marker, start + marker.length());
        if (end == -1) return null;
        return content.substring(start + marker.length(), end).trim();
    }

    @Tool(description = "List built-in skill ids that can be loaded with load_skill.")
    public String list_skills() {
        Set<String> ids = skillCatalog.keySet();
        return ids.isEmpty() ? "" : ids.stream().sorted().collect(Collectors.joining(", "));
    }

    @Tool(description = "Load a skill markdown (SKILL.md) by id, e.g. weather-skill or news-skill.")
    public String load_skill(String skillId) throws IOException {
        if (skillId == null || skillId.isBlank()) {
            return "skillId is required";
        }
        String id = skillId.trim();
        if (!skillCatalog.containsKey(id)) {
            String available = list_skills();
            return "Unknown skill: " + id + ". Available: " + (available.isEmpty() ? "none" : available);
        }
        Resource resource = resourceLoader.getResource("classpath:/skills/" + id + "/SKILL.md");
        if (!resource.exists()) {
            return "Skill file missing: " + id;
        }
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    record SkillMetadata(String name, String description) {}
}