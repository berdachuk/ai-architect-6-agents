package com.berdachuk.meteoris.insight.agent.api;

import java.util.List;

public record ChatTurnResult(
        String sessionId,
        Status status,
        String reply,
        String modelName,
        String ticketId,
        String prompt,
        List<AskUserOptionView> options) {

    public enum Status {
        COMPLETE,
        ASK_USER
    }

    public record AskUserOptionView(String id, String label) {}
}
