package com.berdachuk.meteoris.insight.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class EvalScorer {

    private static final Pattern HEADLINE_LINE = Pattern.compile("(?m)^\\s*(?:\\d+\\.\\s+|[-*]\\s+).+$");

    private EvalScorer() {}

    public static boolean score(EvalCase c, String answer, List<String> reasonCodes) {
        if (answer == null || answer.isBlank()) {
            reasonCodes.add("empty_answer");
            return false;
        }
        String lower = answer.toLowerCase(Locale.ROOT);
        if ("weather".equalsIgnoreCase(c.type())) {
            return scoreWeather(c, lower, answer, reasonCodes);
        }
        if ("news".equalsIgnoreCase(c.type())) {
            return scoreNews(c, answer, reasonCodes);
        }
        reasonCodes.add("unknown_type");
        return false;
    }

    private static boolean scoreWeather(EvalCase c, String lower, String answer, List<String> reasonCodes) {
        if (c.expectedCity() != null && !lower.contains(c.expectedCity().toLowerCase(Locale.ROOT))) {
            reasonCodes.add("missing_city");
            return false;
        }
        List<String> fields = c.requiredFields() == null ? List.of() : c.requiredFields();
        for (String f : fields) {
            if (!hasWeatherField(lower, f)) {
                reasonCodes.add("missing_field:" + f);
                return false;
            }
        }
        return true;
    }

    private static boolean hasWeatherField(String lower, String field) {
        return switch (field) {
            case "city" -> lower.chars().anyMatch(Character::isLetter);
            case "temperature" -> lower.contains("°") || lower.contains("c") || lower.contains("temp");
            case "conditions" -> lower.contains("cloud") || lower.contains("fair") || lower.contains("rain")
                    || lower.contains("snow") || lower.contains("clear") || lower.contains("partly")
                    || lower.contains("drizzle") || lower.contains("foggy") || lower.contains("shower")
                    || lower.contains("storm");
            case "time" -> lower.contains("time") || lower.contains("tz") || lower.matches(".*\\d{4}-\\d{2}-\\d{2}.*");
            default -> true;
        };
    }

    private static boolean scoreNews(EvalCase c, String answer, List<String> reasonCodes) {
        int min = c.minHeadlines() == null ? 1 : c.minHeadlines();
        var m = HEADLINE_LINE.matcher(answer);
        List<String> hits = new ArrayList<>();
        while (m.find()) {
            hits.add(m.group());
        }
        if (hits.size() < min) {
            reasonCodes.add("min_headlines:" + hits.size() + "<" + min);
            return false;
        }
        if (Boolean.TRUE.equals(c.requireSourceOrTime())) {
            if (!answer.contains("http") && !answer.matches("(?i).*(today|yesterday|202[0-9]).*")) {
                reasonCodes.add("missing_source_or_time");
                return false;
            }
        }
        return true;
    }
}
