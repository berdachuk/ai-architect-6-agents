package com.berdachuk.meteoris.insight.agent;

/**
 * Holds the active conversation id for the current orchestration request so @Tool methods
 * can access session-scoped state (todos, AutoMemory) without changing Spring AI tool signatures.
 */
public final class OrchestrationContextHolder {

    private static final ThreadLocal<String> SESSION_ID = new ThreadLocal<>();

    private OrchestrationContextHolder() {}

    public static void setSessionId(String sessionId) {
        SESSION_ID.set(sessionId);
    }

    public static String sessionIdOrNull() {
        return SESSION_ID.get();
    }

    public static void clear() {
        SESSION_ID.remove();
    }
}
