package com.berdachuk.meteoris.insight.news.api;

/**
 * Public facade for news digests consumed by orchestration and API layers.
 */
public interface NewsDigestApi {

    /**
     * Returns a short digest with multiple headline-like lines for the topic.
     */
    String latestHeadlines(String topic);
}
