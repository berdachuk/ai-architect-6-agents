package com.berdachuk.meteoris.insight.agent;

import com.berdachuk.meteoris.insight.memory.AutoMemoryService;
import java.io.IOException;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * AutoMemory tools exposed to the orchestrator LLM.
 *
 * <p>All tool methods return {@code void} (Spring AI convention for side-effect-only tools).
 * The caller receives a synthetic success/error message appended to the context by the framework.
 */
@Component
public class AutoMemoryTools {

    private final AutoMemoryService autoMemoryService;

    public AutoMemoryTools(AutoMemoryService autoMemoryService) {
        this.autoMemoryService = autoMemoryService;
    }

    /**
     * Append a durable markdown preference line to AutoMemory.
     *
     * @param type        one of {@code user}, {@code feedback}, {@code project}, {@code reference}
     * @param markdownLine the content line (do not prefix with "-"; it is added automatically)
     */
    @Tool(description = "Append a durable markdown preference line to AutoMemory (type: user/feedback/project/reference).")
    public void automemory_append(String type, String markdownLine) {
        try {
            String sid = OrchestrationContextHolder.sessionIdOrNull();
            if (sid == null) {
                throw new IllegalStateException("No active session context for AutoMemory.");
            }
            if (type == null || type.isBlank() || markdownLine == null || markdownLine.isBlank()) {
                throw new IllegalArgumentException("type and markdownLine required.");
            }
            autoMemoryService.appendEntry(type, "- " + markdownLine.trim());
        } catch (IOException e) {
            throw new IllegalStateException("AutoMemory write failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convenience overload defaulting to {@code user} type.
     */
    @Tool(description = "Append a markdown preference line to AutoMemory (defaults to 'user' type).")
    public void appendPreference(String markdownLine) {
        automemory_append("user", markdownLine);
    }

    @Tool(description = "Read AutoMemory entries of a given type (user/feedback/project/reference). Use 'all' for everything.")
    public String automemory_read(String type) {
        try {
            if (
                    "all".equalsIgnoreCase(type) || type == null || type.isBlank()) {
                return autoMemoryService.readAll();
            }
            return autoMemoryService.readEntries(type);
        } catch (IOException e) {
            throw new IllegalStateException("AutoMemory read failed: " + e.getMessage(), e);
        }
    }

    @Tool(description = "View the MEMORY.md index.")
    public String automemory_index() {
        try {
            return autoMemoryService.readIndex();
        } catch (IOException e) {
            throw new IllegalStateException("AutoMemory index read failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convenience method defaulting to {@code user} type.
     */
    @Tool(description = "Read user-type AutoMemory entries.")
    public String readPreferences() {
        return automemory_read("user");
    }
}
