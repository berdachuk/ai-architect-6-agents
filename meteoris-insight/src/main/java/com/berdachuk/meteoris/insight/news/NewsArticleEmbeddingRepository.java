package com.berdachuk.meteoris.insight.news;

import java.util.List;

/**
 * Optional persistence for news headline embeddings (pgvector). Active only when
 * {@code meteoris.news.embedding.enabled=true} and Flyway has created {@code news_article_embedding}.
 */
public interface NewsArticleEmbeddingRepository {

    void insert(String id, String topic, String headline, float[] embedding);

    List<String> findHeadlinesByTopic(String topic, int limit);

    /**
     * Nearest headlines by embedding L2 distance (requires pgvector; enabled with
     * {@code meteoris.news.embedding.enabled=true}).
     */
    List<HeadlineDistance> findNearestHeadlines(float[] queryEmbedding, int limit);

    /**
     * Number of headlines stored for a topic (useful to decide whether to fetch from upstream).
     */
    default long countByTopic(String topic) {
        return findHeadlinesByTopic(topic, Integer.MAX_VALUE).size();
    }
}
