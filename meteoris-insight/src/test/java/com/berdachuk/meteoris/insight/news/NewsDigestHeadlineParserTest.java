package com.berdachuk.meteoris.insight.news;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NewsDigestHeadlineParserTest {

    @Test
    void parsesStubDigestNumberedLines() {
        String digest =
                """
                News digest (topic-x):
                1. Headline one about topic-x
                2. Headline two — industry update
                3. Headline three — policy note
                Source: stub profile (keyless MCP not invoked in CI).""";
        assertThat(NewsDigestHeadlineParser.parseHeadlines(digest))
                .containsExactly(
                        "Headline one about topic-x",
                        "Headline two — industry update",
                        "Headline three — policy note");
    }

    @Test
    void returnsEmptyForUpstreamFailureDigest() {
        assertThat(NewsDigestHeadlineParser.parseHeadlines("News: upstream RSS request failed: timeout"))
                .isEmpty();
    }
}
