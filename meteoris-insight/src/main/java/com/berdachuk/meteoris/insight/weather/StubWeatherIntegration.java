package com.berdachuk.meteoris.insight.weather;

import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("stub-ai")
public class StubWeatherIntegration implements WeatherIntegration {

    @Override
    public String forecast(String city) {
        String safe = city == null || city.isBlank() ? "Unknown" : city.trim();
        return "Weather in " + safe + ": 12°C, partly cloudy, observation time " + Instant.parse("2026-04-18T12:00:00Z") + " (stub profile).";
    }
}
