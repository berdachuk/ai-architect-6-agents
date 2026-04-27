package com.berdachuk.meteoris.insight.agent;

import com.berdachuk.meteoris.insight.agent.api.ChatOrchestration;
import com.berdachuk.meteoris.insight.agent.api.ChatTurnResult;
import com.berdachuk.meteoris.insight.memory.SessionService;
import com.berdachuk.meteoris.insight.news.api.NewsDigestApi;
import com.berdachuk.meteoris.insight.weather.api.WeatherForecastApi;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Service
public class OrchestrationService implements ChatOrchestration {

    private static final Pattern WEATHER_CITY =
            Pattern.compile("(?i)(?:weather|forecast|conditions).*(?:in|for)\\s+([A-Za-z]+)");

    private final WeatherForecastApi weatherIntegration;
    private final NewsDigestApi newsIntegration;
    private final Optional<ChatClient> liveChatClient;
    private final ChatMemory chatMemory;
    private final QuestionHub questionHub;
    private final SessionService sessionService;

    public OrchestrationService(
            WeatherForecastApi weatherIntegration,
            NewsDigestApi newsIntegration,
            Optional<ChatClient> liveChatClient,
            ChatMemory chatMemory,
            QuestionHub questionHub,
            SessionService sessionService) {
        this.weatherIntegration = weatherIntegration;
        this.newsIntegration = newsIntegration;
        this.liveChatClient = liveChatClient;
        this.chatMemory = chatMemory;
        this.questionHub = questionHub;
        this.sessionService = sessionService;
    }

    @Override
    public ChatTurnResult exchange(String sessionId, String userMessage) {
        sessionService.touch(sessionId);
        String msg = userMessage == null ? "" : userMessage.trim();
        String lower = msg.toLowerCase(Locale.ROOT);

        // Direct routing for weather and news to avoid relying on model tool-calling reliability.
        if (lower.contains("weather")) {
            String city = extractCity(msg);
            String reply = weatherIntegration.forecast(city);
            return new ChatTurnResult(
                    sessionId, ChatTurnResult.Status.COMPLETE, reply, "live", null, null, null);
        }
        if (lower.contains("news")) {
            String topic = extractTopic(msg);
            String reply = newsIntegration.latestHeadlines(topic);
            return new ChatTurnResult(
                    sessionId, ChatTurnResult.Status.COMPLETE, reply, "live", null, null, null);
        }

        ChatClient client =
                liveChatClient.orElseThrow(() -> new IllegalStateException("Live ChatClient missing"));
        OrchestrationContextHolder.setSessionId(sessionId);
        try {
            String reply = client.prompt()
                    .user(userMessage)
                    .advisors(a ->
                            a.param(ChatMemory.CONVERSATION_ID, ConversationBranches.orchestrator(sessionId)))
                    .call()
                    .content();
            return new ChatTurnResult(
                    sessionId, ChatTurnResult.Status.COMPLETE, reply, "live", null, null, null);
        } finally {
            OrchestrationContextHolder.clear();
        }
    }

    @Override
    public ChatTurnResult resumeAnswer(
            String sessionId, String ticketId, List<String> selectedOptionIds, String freeText) {
        sessionService.touch(sessionId);
        StringBuilder answer = new StringBuilder();
        if (selectedOptionIds != null && !selectedOptionIds.isEmpty()) {
            answer.append(String.join(", ", selectedOptionIds));
        }
        if (freeText != null && !freeText.isBlank()) {
            if (!answer.isEmpty()) {
                answer.append(' ');
            }
            answer.append(freeText.trim());
        }
        if (!questionHub.complete(ticketId, answer.toString())) {
            throw new UnknownAskUserTicketException(ticketId);
        }
        ChatClient client =
                liveChatClient.orElseThrow(() -> new IllegalStateException("Live ChatClient missing"));
        OrchestrationContextHolder.setSessionId(sessionId);
        try {
            String reply = client.prompt()
                    .user("User answered AskUser ticket "
                            + ticketId
                            + " with: "
                            + answer
                            + ". Continue the task.")
                    .advisors(a ->
                            a.param(ChatMemory.CONVERSATION_ID, ConversationBranches.orchestrator(sessionId)))
                    .call()
                    .content();
            return new ChatTurnResult(
                    sessionId, ChatTurnResult.Status.COMPLETE, reply, "live", null, null, null);
        } finally {
            OrchestrationContextHolder.clear();
        }
    }

    static String extractCity(String userMessage) {
        String msg = userMessage == null ? "" : userMessage.trim();
        Matcher m = WEATHER_CITY.matcher(msg);
        if (m.find()) {
            return m.group(1).replaceAll("\\s+", " ").trim();
        }
        return "Brest";
    }

    static String extractTopic(String userMessage) {
        String lower = userMessage == null ? "" : userMessage.toLowerCase(Locale.ROOT);
        int about = lower.indexOf("about");
        if (about >= 0 && about + 5 < userMessage.length()) {
            return userMessage.substring(about + 5).trim();
        }
        if (lower.contains("climate")) return "climate";
        if (lower.contains("space")) return "space";
        if (lower.contains("technology") || lower.contains("tech")) return "technology";
        return "general";
    }

    private void appendMemory(String sessionId, String userLine, String assistantLine) {
        chatMemory.add(
                ConversationBranches.orchestrator(sessionId),
                List.of(new UserMessage(userLine), new AssistantMessage(assistantLine)));
    }
}
