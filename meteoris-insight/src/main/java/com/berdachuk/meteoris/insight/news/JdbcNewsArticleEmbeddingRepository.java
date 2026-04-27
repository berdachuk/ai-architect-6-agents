package com.berdachuk.meteoris.insight.news;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "meteoris.news.embedding", name = "enabled", havingValue = "true")
public class JdbcNewsArticleEmbeddingRepository implements NewsArticleEmbeddingRepository {

    public static final int EMBEDDING_DIMENSION = 1536;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcNewsArticleEmbeddingRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(String id, String topic, String headline, float[] embedding) {
        if (embedding == null || embedding.length != EMBEDDING_DIMENSION) {
            throw new IllegalArgumentException("embedding must have length " + EMBEDDING_DIMENSION);
        }
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("topic", topic)
                .addValue("headline", headline)
                .addValue("embedding", toVectorLiteral(embedding));
        jdbc.update(
                """
                INSERT INTO news_article_embedding (id, topic, headline, embedding)
                VALUES (:id, :topic, :headline, CAST(:embedding AS vector))
                """,
                params);
    }

    @Override
    public List<String> findHeadlinesByTopic(String topic, int limit) {
        return jdbc.queryForList(
                "SELECT headline FROM news_article_embedding WHERE topic = :topic ORDER BY id LIMIT :limit",
                new MapSqlParameterSource("topic", topic).addValue("limit", limit),
                String.class);
    }

    @Override
    public List<HeadlineDistance> findNearestHeadlines(float[] queryEmbedding, int limit) {
        if (queryEmbedding == null || queryEmbedding.length != EMBEDDING_DIMENSION) {
            throw new IllegalArgumentException("queryEmbedding must have length " + EMBEDDING_DIMENSION);
        }
        var params = new MapSqlParameterSource()
                .addValue("q", toVectorLiteral(queryEmbedding))
                .addValue("limit", limit);
        return jdbc.query(
                """
                SELECT headline, (embedding <-> CAST(:q AS vector)) AS dist
                FROM news_article_embedding
                ORDER BY embedding <-> CAST(:q AS vector)
                LIMIT :limit
                """,
                params,
                (rs, rowNum) -> new HeadlineDistance(rs.getString("headline"), rs.getDouble("dist")));
    }

    static String toVectorLiteral(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 4);
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(Float.toString(v[i]));
        }
        sb.append(']');
        return sb.toString();
    }
}
