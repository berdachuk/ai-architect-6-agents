package com.berdachuk.meteoris.insight.agent.api;

import com.berdachuk.meteoris.insight.agent.api.ChatTurnResult;
import java.util.List;

/**
 * Public facade for chat orchestration consumed by REST and Thymeleaf controllers.
 */
public interface ChatOrchestration {

    ChatTurnResult exchange(String sessionId, String userMessage);

    ChatTurnResult resumeAnswer(
            String sessionId, String ticketId, List<String> selectedOptionIds, String freeText);
}
