package com.berdachuk.meteoris.insight.news;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("stub-ai")
public class StubNewsIntegration implements NewsIntegration {

    @Override
    public String latestHeadlines(String topic) {
        String t = topic == null || topic.isBlank() ? "general" : topic.trim();
        return "News digest ("
                + t
                + "):\n"
                + "1. Headline one about "
                + t
                + "\n"
                + "2. Headline two — industry update\n"
                + "3. Headline three — policy note\n"
                + "Source: stub profile (keyless MCP not invoked in CI).";
    }
}
