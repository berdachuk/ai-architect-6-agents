package com.berdachuk.meteoris.insight.news;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Keyless headlines via Google News RSS (no API key). This satisfies the assignment constraint
 * for a keyless news source; swap to an MCP client under {@code local} if you standardize on
 * a specific packaged news MCP server.
 */
@Component
@Profile("!stub-ai")
public class KeylessNewsIntegration implements NewsIntegration {

    private static final Pattern ITEM = Pattern.compile("<item>[\\s\\S]*?</item>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE =
            Pattern.compile("<title>\\s*<!\\[CDATA\\[([^\\]]+)\\]\\]>\\s*</title>|<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE);

    private final RestClient http = RestClient.create();

    @Override
    public String latestHeadlines(String topic) {
        String t = topic == null || topic.isBlank() ? "world" : topic.trim();
        try {
            String url = "https://news.google.com/rss/search?q="
                    + URLEncoder.encode(t, StandardCharsets.UTF_8)
                    + "&hl=en-US&gl=US&ceid=US:en";
            String xml = http.get().uri(url).retrieve().body(String.class);
            Matcher im = ITEM.matcher(xml);
            Set<String> titles = new LinkedHashSet<>();
            while (im.find() && titles.size() < 8) {
                String block = im.group();
                Matcher tm = TITLE.matcher(block);
                if (tm.find()) {
                    String title = tm.group(1) != null ? tm.group(1) : tm.group(2);
                    if (title != null && !title.isBlank() && !title.contains("Google News")) {
                        titles.add(title.trim());
                    }
                }
            }
            if (titles.isEmpty()) {
                return "News: could not parse headlines from RSS for topic '" + t + "'.";
            }
            List<String> lines = new ArrayList<>();
            int i = 1;
            for (String headline : titles) {
                lines.add(i++ + ". " + headline);
            }
            return "News digest (" + t + ", Google News RSS, keyless):\n" + String.join("\n", lines);
        } catch (RestClientException ex) {
            return "News: upstream RSS request failed: " + ex.getMessage();
        }
    }
}
