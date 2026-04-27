package com.berdachuk.meteoris.insight.news;

import com.berdachuk.meteoris.insight.core.IdGenerator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "meteoris.news.embedding", name = "enabled", havingValue = "true")
public class NewsEmbeddingWriteService {

    private final ObjectProvider<NewsArticleEmbeddingRepository> embeddingRepository;
    private final ObjectProvider<EmbeddingModel> embeddingModel;
    private final IdGenerator idGenerator;

    public NewsEmbeddingWriteService(
            ObjectProvider<NewsArticleEmbeddingRepository> embeddingRepository,
            ObjectProvider<EmbeddingModel> embeddingModel,
            IdGenerator idGenerator) {
        this.embeddingRepository = embeddingRepository;
        this.embeddingModel = embeddingModel;
        this.idGenerator = idGenerator;
    }

    /**
     * Persists embeddings for numbered headlines in a digest when the JDBC repository bean is
     * active (same property gate as {@link JdbcNewsArticleEmbeddingRepository}).
     */
    public void persistFromDigest(String topic, String digest) {
        NewsArticleEmbeddingRepository repo = embeddingRepository.getIfAvailable();
        if (repo == null) {
            throw new IllegalStateException("NewsArticleEmbeddingRepository not available. Check meteoris.news.embedding.enabled.");
        }
        EmbeddingModel model = embeddingModel.getIfAvailable();
        if (model == null) {
            throw new IllegalStateException("EmbeddingModel not available. Check AI model configuration.");
        }
        String t = topic == null || topic.isBlank() ? "general" : topic.trim();
        List<String> headlines = NewsDigestHeadlineParser.parseHeadlines(digest);
        if (headlines.isEmpty()) {
            return;
        }
        Set<String> existing = new HashSet<>(repo.findHeadlinesByTopic(t, 200));
        for (String headline : headlines) {
            if (existing.contains(headline)) {
                continue;
            }
            float[] vec = model.embed(headline);
            repo.insert(idGenerator.generateId(), t, headline, vec);
            existing.add(headline);
        }
    }

    private static float[] floatArray(float[] v) {
        return v;
    }
}
