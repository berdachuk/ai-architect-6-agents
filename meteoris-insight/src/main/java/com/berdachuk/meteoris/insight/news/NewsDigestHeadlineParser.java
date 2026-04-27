package com.berdachuk.meteoris.insight.news;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts numbered headline lines from digest strings produced by {@link StubNewsIntegration} and
 * {@link KeylessNewsIntegration}.
 */
final class NewsDigestHeadlineParser {

    private static final Pattern NUMBERED_LINE = Pattern.compile("^\\s*\\d+\\.\\s+(.+)$");

    private NewsDigestHeadlineParser() {}

    static List<String> parseHeadlines(String digest) {
        if (digest == null || digest.isBlank()) {
            return List.of();
        }
        if (digest.startsWith("News: could not") || digest.startsWith("News: upstream")) {
            return List.of();
        }
        Set<String> ordered = new LinkedHashSet<>();
        for (String line : digest.split("\\R")) {
            Matcher m = NUMBERED_LINE.matcher(line);
            if (m.find()) {
                String h = m.group(1).trim();
                if (!h.isEmpty()) {
                    ordered.add(h);
                }
            }
        }
        return new ArrayList<>(ordered);
    }
}
