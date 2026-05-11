package com.berdachuk.meteoris.insight.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.SlidingWindowCompactionStrategy;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.berdachuk.meteoris.insight.weather.WeatherTools;

@Configuration
@Profile("!stub-ai")
public class LiveAgentConfiguration {

    /**
     * Spring AI 2.x OpenAI client requires a non-empty API key at build time; LM Studio / Ollama
     * typically ignore it. Use a placeholder when {@code spring.ai.custom.chat.api-key} is unset.
     */
    private static final String KEYLESS_OPENAI_COMPAT_API_KEY = "not-used";

    @Bean
    ChatModel meteorisChatModel(
            @Value("${spring.ai.custom.chat.base-url}") String baseUrl,
            @Value("${spring.ai.custom.chat.api-key:}") String apiKey,
            @Value("${spring.ai.custom.chat.model:gemma4:26b}") String model,
            @Value("${spring.ai.custom.chat.temperature:0.7}") double temperature,
            @Value("${spring.ai.custom.chat.max-tokens:2048}") int maxTokens) {
        String effectiveApiKey =
                apiKey != null && !apiKey.isBlank() ? apiKey : KEYLESS_OPENAI_COMPAT_API_KEY;
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .baseUrl(baseUrl)
                .apiKey(effectiveApiKey)
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
        return OpenAiChatModel.builder()
                .options(options)
                .build();
    }

    @Bean
    @Primary
    EmbeddingModel meteorisEmbeddingModel(
            @Value("${spring.ai.custom.embedding.base-url:}") String baseUrl,
            @Value("${spring.ai.custom.embedding.api-key:}") String apiKey,
            @Value("${spring.ai.custom.embedding.model:nomic-embed-text:v1.5}") String model,
            @Value("${spring.ai.custom.embedding.dimensions:768}") int dimensions) {
        String resolvedBaseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl
                : System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
        String resolvedApiKey =
                apiKey != null && !apiKey.isBlank() ? apiKey : KEYLESS_OPENAI_COMPAT_API_KEY;
        OpenAiEmbeddingOptions embedOptions = OpenAiEmbeddingOptions.builder()
                .baseUrl(resolvedBaseUrl)
                .apiKey(resolvedApiKey)
                .model(model)
                .dimensions(dimensions)
                .build();
        return new ConfiguredDimensionsOpenAiEmbeddingModel(embedOptions);
    }

    /**
     * {@link org.springframework.ai.embedding.AbstractEmbeddingModel#dimensions()} probes the
     * remote endpoint via {@code embed(...)}. Some OpenAI-compatible servers (including LM
     * Studio in common setups) return embedding payloads that fail deserialization in the
     * official OpenAI Java SDK for that probe, while normal embedding calls still work. When
     * {@link OpenAiEmbeddingOptions#getDimensions()} is set (Meteoris sets this from config),
     * return it directly and skip the network round-trip.
     */
    private static final class ConfiguredDimensionsOpenAiEmbeddingModel extends OpenAiEmbeddingModel {

        ConfiguredDimensionsOpenAiEmbeddingModel(OpenAiEmbeddingOptions options) {
            super(null, MetadataMode.EMBED, options, null);
        }

        @Override
        public int dimensions() {
            Integer configured = getOptions().getDimensions();
            if (configured != null && configured > 0) {
                return configured;
            }
            return super.dimensions();
        }
    }

    @Bean
    WeatherTools weatherTools(com.berdachuk.meteoris.insight.weather.WeatherIntegration weatherIntegration) {
        return new WeatherTools(weatherIntegration);
    }

    // Specialist ChatClients removed — DelegationTools now calls live integrations directly
    // to avoid nested LLM round-trip timeouts.

    @Bean
    DelegationTools delegationTools(
            com.berdachuk.meteoris.insight.weather.api.WeatherForecastApi weatherForecastApi,
            com.berdachuk.meteoris.insight.news.api.NewsDigestApi newsDigestApi) {
        return new DelegationTools(weatherForecastApi, newsDigestApi);
    }

    @Bean
    MethodToolCallbackProvider orchestratorToolCallbacks(
            DelegationTools delegationTools,
            SkillsTools skillsTools,
            TodoWriteTools todoWriteTools,
            AutoMemoryTools autoMemoryTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(delegationTools, skillsTools, todoWriteTools, autoMemoryTools)
                .build();
    }

    /**
     * Short-term conversation memory via Spring AI Session (JDBC), with turn-safe compaction
     * comparable to the former {@code MessageWindowChatMemory} window (~40 events).
     */
    @Bean
    SessionMemoryAdvisor sessionMemoryAdvisor(SessionService sessionService) {
        return SessionMemoryAdvisor.builder(sessionService)
                .compactionTrigger(new TurnCountTrigger(20))
                .compactionStrategy(SlidingWindowCompactionStrategy.builder().maxEvents(40).build())
                .build();
    }

    @Bean
    @Primary
    ChatClient orchestratorChatClient(
            ChatModel meteorisChatModel,
            SessionMemoryAdvisor sessionMemoryAdvisor,
            MethodToolCallbackProvider orchestratorToolCallbacks) {
        return ChatClient.builder(meteorisChatModel)
                .defaultSystem(
                        """
                        You are Meteoris Insight — the main orchestrator.
                        For weather questions, call delegate_weather with the user's question (include city if known).
                        For news questions, call delegate_news with the user's question.
                        Return the specialist's answer to the user as-is — do not summarize or rephrase it.
                        You may use list_skills / load_skill for extra guidance, todo_write for multi-step work,
                        and automemory_append / automemory_read for durable user preferences.
                        """)
                .defaultAdvisors(sessionMemoryAdvisor)
                .defaultToolCallbacks(orchestratorToolCallbacks)
                .build();
    }
}
