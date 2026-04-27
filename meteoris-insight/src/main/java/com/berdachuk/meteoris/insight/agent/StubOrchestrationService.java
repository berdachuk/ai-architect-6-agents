package com.berdachuk.meteoris.insight.agent;

import com.berdachuk.meteoris.insight.agent.api.ChatOrchestration;
import com.berdachuk.meteoris.insight.agent.api.ChatTurnResult;
import com.berdachuk.meteoris.insight.memory.SessionService;
import com.berdachuk.meteoris.insight.news.api.NewsDigestApi;
import com.berdachuk.meteoris.insight.weather.api.WeatherForecastApi;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Deterministic stub orchestration used in CI / {@code stub-ai} profile.
 * Implements {@link ChatOrchestration} without requiring a live LLM.
 */
@Service
@Profile("stub-ai")
public class StubOrchestrationService implements ChatOrchestration {

    private static final Pattern WEATHER_CITY =
            Pattern.compile("(?i)weather.*\\bin\\s+([A-Za-z][A-Za-z\\s,-]{0,40})");

    private final WeatherForecastApi weatherIntegration;
    private final NewsDigestApi newsIntegration;
    private final QuestionHub questionHub;

    public StubOrchestrationService(
            WeatherForecastApi weatherIntegration,
            NewsDigestApi newsIntegration,
            QuestionHub questionHub) {
        this.weatherIntegration = weatherIntegration;
        this.newsIntegration = newsIntegration;
        this.questionHub = questionHub;
    }

    @Override
    public ChatTurnResult exchange(String sessionId, String userMessage) {
        String msg = userMessage == null ? "" : userMessage.trim();
        String lower = msg.toLowerCase(Locale.ROOT);
        if (lower.equals("askuser")) {
            String ticket = questionHub.register(new CompletableFuture<>());
            return new ChatTurnResult(
                    sessionId,
                    ChatTurnResult.Status.ASK_USER,
                    null,
                    "stub-ai",
                    ticket,
                    "Which city should we use for the forecast?",
                    List.of(
                            new ChatTurnResult.AskUserOptionView("brest", "Brest"),
                            new ChatTurnResult.AskUserOptionView("minsk", "Minsk")));
        }
        if (lower.contains("weather")) {
            Matcher m = WEATHER_CITY.matcher(msg);
            String city = m.find() ? m.group(1).replace(',', ' ').trim() : "Brest";
            String reply = weatherIntegration.forecast(city);
            return new ChatTurnResult(
                    sessionId, ChatTurnResult.Status.COMPLETE, reply, "stub-ai", null, null, null);
        }
        if (lower.contains("news")) {
            String topic = "general";
            int about = lower.indexOf("about");
            if (about >= 0 && about + 5 < msg.length()) {
                topic = msg.substring(about + 5).trim();
            }
            String reply = newsIntegration.latestHeadlines(topic);
            return new ChatTurnResult(
                    sessionId, ChatTurnResult.Status.COMPLETE, reply, "stub-ai", null, null, null);
        }
        String fallback =
                "Stub profile: ask about **weather** (include a city) or **news** (optionally \"about …\").";
        return new ChatTurnResult(
                sessionId, ChatTurnResult.Status.COMPLETE, fallback, "stub-ai", null, null, null);
    }

    @Override
    public ChatTurnResult resumeAnswer(
            String sessionId, String ticketId, List<String> selectedOptionIds, String freeText) {
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
        String city = answer.toString().trim();
        String reply = weatherIntegration.forecast(city.isEmpty() ? "Brest" : city);
        return new ChatTurnResult(
                sessionId, ChatTurnResult.Status.COMPLETE, reply, "stub-ai", null, null, null);
    }
}
