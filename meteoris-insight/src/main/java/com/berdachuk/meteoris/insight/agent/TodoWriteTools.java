package com.berdachuk.meteoris.insight.agent;

import com.berdachuk.meteoris.insight.memory.TodoStateStore;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Todo-list tools exposed to the orchestrator LLM.
 *
 * <p>All write methods return {@code void} (Spring AI convention for side-effect-only tools).
 */
@Component
public class TodoWriteTools {

    private final TodoStateStore todoStateStore;

    public TodoWriteTools(TodoStateStore todoStateStore) {
        this.todoStateStore = todoStateStore;
    }

    @Tool(description = "Append a todo item for the current chat session (multi-step user requests).")
    public void todo_write(String item) {
        String sid = OrchestrationContextHolder.sessionIdOrNull();
        if (sid == null) {
            throw new IllegalStateException("No active session context for todos.");
        }
        if (item == null || item.isBlank()) {
            throw new IllegalArgumentException("Refused: empty todo item.");
        }
        todoStateStore.addTodo(sid, item.trim());
    }

    @Tool(description = "List current todo items for this session.")
    public String todo_list() {
        String sid = OrchestrationContextHolder.sessionIdOrNull();
        if (sid == null) {
            return "";
        }
        return String.join("\n", todoStateStore.list(sid));
    }
}
