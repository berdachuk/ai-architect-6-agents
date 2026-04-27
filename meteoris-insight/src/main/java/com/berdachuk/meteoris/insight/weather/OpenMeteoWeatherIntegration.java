package com.berdachuk.meteoris.insight.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Profile("!stub-ai")
public class OpenMeteoWeatherIntegration implements WeatherIntegration {

    private static final String GEO = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FC = "https://api.open-meteo.com/v1/forecast";

    private final RestClient http = RestClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, "Meteoris-Insight/1.0")
            .build();

    private static final ObjectMapper OM = new ObjectMapper();

    @Override
    public String forecast(String city) {
        String q = city == null || city.isBlank() ? "Unknown" : city.trim();
        try {
            String geoUrl = GEO + "?name=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
                    + "&count=1&language=en&format=json";
            String geoBody = http.get().uri(geoUrl).retrieve().body(String.class);
            JsonNode geoRoot = OM.readTree(geoBody);
            JsonNode results = geoRoot.path("results");
            if (!results.isArray() || results.isEmpty()) {
                return "Weather: could not geocode '" + q + "'. Try a larger nearby city.";
            }
            JsonNode hit = results.get(0);
            double lat = hit.path("latitude").asDouble();
            double lon = hit.path("longitude").asDouble();
            String name = hit.path("name").asText(q);
            String country = hit.path("country").asText("");

            String fcUrl = FC + "?latitude="
                    + lat + "&longitude=" + lon
                    + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
                    + "&timezone=auto";
            String fcBody = http.get().uri(fcUrl).retrieve().body(String.class);
            JsonNode fcRoot = OM.readTree(fcBody);
            JsonNode cur = fcRoot.path("current");
            double temp = cur.path("temperature_2m").asDouble(Double.NaN);
            int code = cur.path("weather_code").asInt(-1);
            String time = cur.path("time").asText("unknown time");

            String conditions = describeWmo(code);
            return "Weather in "
                    + name
                    + (country.isBlank() ? "" : ", " + country)
                    + ": "
                    + (Double.isNaN(temp) ? "n/a" : String.format(java.util.Locale.ROOT, "%.1f°C", temp))
                    + ", "
                    + conditions
                    + ", observation time "
                    + time
                    + " (Open-Meteo).";
        } catch (RestClientException ex) {
            return "Weather: upstream Open-Meteo request failed: " + ex.getMessage();
        } catch (Exception ex) {
            return "Weather: failed to parse Open-Meteo response: " + ex.getMessage();
        }
    }

    private static String describeWmo(int code) {
        if (code < 0) {
            return "unknown conditions";
        }
        return switch (code) {
            case 0 -> "clear sky";
            case 1, 2, 3 -> "mainly clear to cloudy";
            case 45, 48 -> "foggy";
            case 51, 53, 55 -> "drizzle";
            case 61, 63, 65 -> "rain";
            case 71, 73, 75 -> "snow";
            case 80, 81, 82 -> "rain showers";
            case 95, 96, 99 -> "thunderstorm";
            default -> "conditions (WMO " + code + ")";
        };
    }
}
