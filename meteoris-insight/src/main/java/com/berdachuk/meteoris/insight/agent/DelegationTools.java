package com.berdachuk.meteoris.insight.agent;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.berdachuk.meteoris.insight.news.api.NewsDigestApi;
import com.berdachuk.meteoris.insight.weather.api.WeatherForecastApi;

import org.springframework.ai.tool.annotation.Tool;

/**
 * Direct tool delegation to live weather / news integrations (Open-Meteo + Google News RSS).
 * No secondary LLM round-trip — the orchestrator calls these methods as tools,
 * which execute HTTP calls and return raw provider text.
 * This avoids nested ChatClient timeouts and keeps agentic orchestration visible in logs.
 */
public class DelegationTools {

    private static final Pattern WEATHER_CITY =
            Pattern.compile("(?i)(?:weather|forecast|conditions).*(?:in|for)\\s+([A-Za-z]+)");

    private final WeatherForecastApi weatherIntegration;
    private final NewsDigestApi newsIntegration;

    public DelegationTools(WeatherForecastApi weatherIntegration, NewsDigestApi newsIntegration) {
        this.weatherIntegration = weatherIntegration;
        this.newsIntegration = newsIntegration;
    }

    @Tool(description = """
            Answer weather questions by calling the Open-Meteo live API.
            Input: the original user question (used to extract a city name if possible).
            Returns: a sentence with city, temperature (°C), conditions, and ISO observation time.
            """)
    public String delegate_weather(String userQuestion) {
        String city = extractCity(userQuestion);
        return weatherIntegration.forecast(city);
    }

    @Tool(description = """
            Answer news questions by calling the Google News RSS live API (keyless).
            Input: the original user question (used to extract a topic if possible).
            Returns: a numbered list of headlines.
            """)
    public String delegate_news(String userQuestion) {
        String topic = extractTopic(userQuestion);
        return newsIntegration.latestHeadlines(topic);
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
}
