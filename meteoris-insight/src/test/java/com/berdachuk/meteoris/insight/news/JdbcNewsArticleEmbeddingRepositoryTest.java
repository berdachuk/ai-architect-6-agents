package com.berdachuk.meteoris.insight.news;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JdbcNewsArticleEmbeddingRepositoryTest {

    @Test
    void vectorLiteralFormatsFloats() {
        float[] v = {1f, 2.5f, -3f};
        assertThat(JdbcNewsArticleEmbeddingRepository.toVectorLiteral(v)).isEqualTo("[1.0,2.5,-3.0]");
    }
}
