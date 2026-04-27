package com.berdachuk.meteoris.insight.news;

import com.berdachuk.meteoris.insight.news.api.NewsDigestApi;

public interface NewsIntegration extends NewsDigestApi {

    /**
     * Returns a short digest with multiple headline-like lines for the topic.
     */
    String latestHeadlines(String topic);
}
