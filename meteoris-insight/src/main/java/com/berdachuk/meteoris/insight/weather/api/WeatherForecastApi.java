package com.berdachuk.meteoris.insight.weather.api;

/**
 * Public facade for weather forecasts consumed by orchestration and API layers.
 */
public interface WeatherForecastApi {

    /**
     * Returns a short natural-language forecast snippet for the given city.
     */
    String forecast(String city);
}
