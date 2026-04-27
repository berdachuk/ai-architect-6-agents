package com.berdachuk.meteoris.insight.weather;

import com.berdachuk.meteoris.insight.weather.api.WeatherForecastApi;

public interface WeatherIntegration extends WeatherForecastApi {

    /**
     * Returns a short natural-language forecast snippet for the given city.
     */
    String forecast(String city);
}
