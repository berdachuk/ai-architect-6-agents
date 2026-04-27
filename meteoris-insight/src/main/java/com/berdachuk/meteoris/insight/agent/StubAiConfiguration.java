package com.berdachuk.meteoris.insight.agent;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("stub-ai")
public class StubAiConfiguration {

    @Bean
    @Primary
    ChatModel stubChatModel() {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return ChatResponse.builder()
                        .generations(List.of(new Generation(new AssistantMessage(
                                "Stub ChatModel: orchestration handled deterministically."))))
                        .build();
            }
        };
    }

    @Bean
    @Primary
    ChatClient stubChatClient(ChatModel stubChatModel) {
        return ChatClient.builder(stubChatModel).build();
    }

    @Bean
    @Primary
    EmbeddingModel stubEmbeddingModel() {
        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                int dim = com.berdachuk.meteoris.insight.news.JdbcNewsArticleEmbeddingRepository.EMBEDDING_DIMENSION;
                List<org.springframework.ai.embedding.Embedding> embeddings = request.getInstructions()
                        .stream()
                        .map(text -> {
                            float[] vec = com.berdachuk.meteoris.insight.news.NewsHeadlineEmbeddingEncoder
                                    .deterministicEmbedding(text);
                            return new org.springframework.ai.embedding.Embedding(vec, null);
                        })
                        .toList();
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public int dimensions() {
                return com.berdachuk.meteoris.insight.news.JdbcNewsArticleEmbeddingRepository.EMBEDDING_DIMENSION;
            }

            @Override
            public float[] embed(Document document) {
                return com.berdachuk.meteoris.insight.news.NewsHeadlineEmbeddingEncoder
                        .deterministicEmbedding(document.getText());
            }
        };
    }
}
