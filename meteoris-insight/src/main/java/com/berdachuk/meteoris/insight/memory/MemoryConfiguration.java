package com.berdachuk.meteoris.insight.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemoryConfiguration {

    /**
     * JDBC-backed chat memory with a sliding window (turn-safe compaction by truncation per WBS 3.1 baseline).
     */
    @Bean
    ChatMemory chatMemory(ChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(40)
                .build();
    }

    @Bean
    SessionService sessionService(org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc, com.berdachuk.meteoris.insight.core.IdGenerator idGenerator) {
        return new SessionService(jdbc, idGenerator);
    }
}
