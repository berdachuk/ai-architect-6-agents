package com.berdachuk.meteoris.insight.agent;

/**
 * JDBC chat memory conversation ids: {@code <sessionId>:orch}, {@code <sessionId>:orch.weather}, etc.
 * Aligns with WBS 3.2 branch isolation for orchestrator vs specialist paths.
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
