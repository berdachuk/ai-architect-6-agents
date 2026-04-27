package com.berdachuk.meteoris.insight.agent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class QuestionHub {

    private final Map<String, CompletableFuture<String>> futures = new ConcurrentHashMap<>();

    public String register(CompletableFuture<String> future) {
        String id = UUID.randomUUID().toString();
        futures.put(id, future);
        return id;
    }

    public CompletableFuture<String> get(String ticketId) {
        return futures.get(ticketId);
    }

    /**
     * @return {@code true} if a pending ticket existed and was completed
     */
    public boolean complete(String ticketId, String answerText) {
        if (ticketId == null || ticketId.isBlank()) {
            return false;
        }
        CompletableFuture<String> f = futures.remove(ticketId);
        if (f != null) {
            f.complete(answerText == null ? "" : answerText);
            return true;
        }
        return false;
    }
}
