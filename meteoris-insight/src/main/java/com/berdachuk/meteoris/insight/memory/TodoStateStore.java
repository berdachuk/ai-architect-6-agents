package com.berdachuk.meteoris.insight.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class TodoStateStore {

    private final Map<String, List<String>> todosBySession = new ConcurrentHashMap<>();

    public void addTodo(String sessionId, String item) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        todosBySession
                .computeIfAbsent(sessionId, k -> new ArrayList<>())
                .add(item);
    }

    public List<String> list(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return List.of();
        }
        return List.copyOf(todosBySession.getOrDefault(sessionId, List.of()));
    }

    public void clear(String sessionId) {
        if (sessionId != null) {
            todosBySession.remove(sessionId);
        }
    }
}
