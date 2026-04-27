package com.berdachuk.meteoris.insight.news;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.ObjectProvider;

public class NewsTools {

    private final NewsIntegration integration;
    private final ObjectProvider<NewsEmbeddingWriteService> embeddingWriteService;
    private final ObjectProvider<NewsArticleEmbeddingRepository> embeddingRepository;
    private final ObjectProvider<EmbeddingModel> embeddingModel;

    public NewsTools(
            NewsIntegration integration,
            ObjectProvider<NewsEmbeddingWriteService> embeddingWriteService,
            ObjectProvider<NewsArticleEmbeddingRepository> embeddingRepository,
            ObjectProvider<EmbeddingModel> embeddingModel) {
        this.integration = integration;
        this.embeddingWriteService = embeddingWriteService;
        this.embeddingRepository = embeddingRepository;
        this.embeddingModel = embeddingModel;
    }

    @Tool(description = """
            Search for the latest news headlines about a given topic.
            Returns a numbered list of headlines (e.g. "1. Headline text\n2. Headline text").
            """)
    public String findNews(String topic) {
        String t = topic == null || topic.isBlank() ? "general" : topic.trim();

        String cached = searchSimilarHeadlines(t);
        if (cached != null && !cached.isBlank()) {
            return cached + "\n(Source: cached vector search)";
        }

        String digest = integration.latestHeadlines(t);
        embeddingWriteService.ifAvailable(s -> s.persistFromDigest(t, digest));
        return digest;
    }

    private String searchSimilarHeadlines(String topic) {
        NewsArticleEmbeddingRepository repo = embeddingRepository.getIfAvailable();
        EmbeddingModel model = embeddingModel.getIfAvailable();
        if (repo == null || model == null) {
            return null;
        }
        if (repo.countByTopic(topic) < 3) {
            return null;
        }
        float[] query = model.embed(topic);
        java.util.List<HeadlineDistance> results = repo.findNearestHeadlines(query, 5);
        if (results.isEmpty()) {
            return null;
        }
        java.util.List<String> lines = new java.util.ArrayList<>();
        int i = 1;
        for (HeadlineDistance hd : results) {
            lines.add(i++ + ". " + hd.headline() + "  (dist=" + String.format(java.util.Locale.ROOT, "%.4f", hd.distance()) + ")");
        }
        return "Cached news digest (" + topic + "):\n" + String.join("\n", lines);
    }
}
