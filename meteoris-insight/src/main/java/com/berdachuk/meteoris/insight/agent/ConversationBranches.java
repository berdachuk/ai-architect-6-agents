package com.berdachuk.meteoris.insight.agent;

/**
 * Conversation branch qualifiers for multi-agent routing ({@code <sessionId>:orch}, etc.).
 * Short-term history uses Spring AI Session with the client {@code sessionId}; branch suffixes remain
 * available for specialist paths or {@code EventFilter}-based isolation when needed.
 */
public final class ConversationBranches {

    public static final String ORCH = "orch";
    public static final String ORCH_WEATHER = "orch.weather";
    public static final String ORCH_NEWS = "orch.news";

    private ConversationBranches() {}

    public static String orchestrator(String sessionId) {
        return qualify(sessionId, ORCH);
    }

    public static String weather(String sessionId) {
        return qualify(sessionId, ORCH_WEATHER);
    }

    public static String news(String sessionId) {
        return qualify(sessionId, ORCH_NEWS);
    }

    private static String qualify(String sessionId, String suffix) {
        if (sessionId == null || sessionId.isBlank()) {
            return suffix;
        }
        return sessionId + ":" + suffix;
    }
}
