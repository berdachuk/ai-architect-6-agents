package com.berdachuk.meteoris.insight.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

import com.berdachuk.meteoris.insight.weather.WeatherTools;

@Configuration
@Profile("!stub-ai")
public class LiveAgentConfiguration {

    @Bean
    OpenAiApi meteorisOpenAiApi(
            @Value("${spring.ai.custom.chat.base-url}") String baseUrl,
            @Value("${spring.ai.custom.chat.api-key:}") String apiKey,
            @Value("${spring.ai.custom.chat.completions-path:/v1/chat/completions}") String completionsPath) {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .completionsPath(completionsPath)
                .restClientBuilder(RestClient.builder())
                .build();
    }

    @Bean
    ChatModel meteorisChatModel(
            OpenAiApi openAiApi,
            @Value("${spring.ai.custom.chat.model:gemma4:26b}") String model,
            @Value("${spring.ai.custom.chat.temperature:0.7}") double temperature,
            @Value("${spring.ai.custom.chat.max-tokens:2048}") int maxTokens) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .build())
                .build();
    }

    @Bean
    @Primary
    EmbeddingModel meteorisEmbeddingModel(
            @Value("${spring.ai.custom.embedding.base-url:}") String baseUrl,
            @Value("${spring.ai.custom.embedding.api-key:}") String apiKey,
            @Value("${spring.ai.custom.embedding.model:nomic-embed-text}") String model,
            @Value("${spring.ai.custom.embedding.dimensions:768}") int dimensions) {
        String resolvedBaseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl
                : System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
        String resolvedApiKey = apiKey != null && !apiKey.isBlank() ? apiKey : "";
        OpenAiApi embApi = OpenAiApi.builder()
                .baseUrl(resolvedBaseUrl)
                .apiKey(resolvedApiKey)
                .restClientBuilder(RestClient.builder())
                .build();
        var embedOpts = new org.springframework.ai.openai.OpenAiEmbeddingOptions();
        embedOpts.setModel(model);
        embedOpts.setDimensions(dimensions);
        return new OpenAiEmbeddingModel(embApi, org.springframework.ai.document.MetadataMode.EMBED, embedOpts);
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

    @Bean
    @Primary
    ChatClient orchestratorChatClient(
            ChatModel meteorisChatModel,
            ChatMemory chatMemory,
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
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultToolCallbacks(orchestratorToolCallbacks)
                .build();
    }
}
