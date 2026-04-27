package com.berdachuk.meteoris.insight.weather;

import org.springframework.ai.tool.annotation.Tool;

public class WeatherTools {

    private final WeatherIntegration integration;

    public WeatherTools(WeatherIntegration integration) {
        this.integration = integration;
    }

    @Tool(description = """
            Get the current weather forecast for a given city name.
            Returns a single sentence containing: city name, temperature in °C, weather conditions,
            and the ISO observation time. Example: "Weather in Brest, Belarus: 14.0°C, cloudy, observation time 2025-08-25T09:00 (Open-Meteo)."
            """)
    public String getWeatherForecast(String city) {
        return integration.forecast(city);
    }
}
