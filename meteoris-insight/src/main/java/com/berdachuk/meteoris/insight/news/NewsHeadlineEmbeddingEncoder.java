package com.berdachuk.meteoris.insight.news;

import java.nio.charset.StandardCharsets;

/**
 * Deterministic pseudo-embeddings for optional headline persistence without calling a remote
 * embedding API (sufficient for pgvector round-trips and similarity in tests / stub profile).
 */
public final class NewsHeadlineEmbeddingEncoder {

    private NewsHeadlineEmbeddingEncoder() {}

    public static float[] deterministicEmbedding(String text) {
        int dim = JdbcNewsArticleEmbeddingRepository.EMBEDDING_DIMENSION;
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        float[] v = new float[dim];
        long state = 0x243F6A8885A308D3L ^ (long) text.length() * 0x9E3779B97F4A7C15L;
        for (int i = 0; i < dim; i++) {
            state = state * 6364136223846793005L + 1;
            int bi = bytes.length == 0 ? 0 : i % bytes.length;
            state ^= (long) (bytes[bi] & 0xFF) << ((i * 7) % 56);
            v[i] = (float) ((state >>> 33) / (double) (1L << 31) - 1.0);
        }
        normalizeL2(v);
        return v;
    }

    private static void normalizeL2(float[] v) {
        double sumSq = 0;
        for (float f : v) {
            sumSq += (double) f * f;
        }
        if (sumSq <= 1e-12) {
            return;
        }
        float inv = (float) (1.0 / Math.sqrt(sumSq));
        for (int i = 0; i < v.length; i++) {
            v[i] *= inv;
        }
    }
}
