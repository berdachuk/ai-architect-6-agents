CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
    conversation_id VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    "timestamp" TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX
    ON SPRING_AI_CHAT_MEMORY (conversation_id, "timestamp");

-- PostgreSQL-only: pgvector + optional news embedding cache (WBS 3.7 groundwork).
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS news_article_embedding (
    id VARCHAR(24) PRIMARY KEY,
    topic TEXT NOT NULL,
    headline TEXT NOT NULL,
    embedding vector(1536) NOT NULL
);

CREATE INDEX IF NOT EXISTS news_article_embedding_topic_idx ON news_article_embedding (topic);

CREATE TABLE IF NOT EXISTS ai_session (
    id VARCHAR(24) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    compacted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_session_event (
    id VARCHAR(24) PRIMARY KEY,
    session_id VARCHAR(24) NOT NULL,
    type VARCHAR(32) NOT NULL,
    role VARCHAR(32),
    content TEXT NOT NULL,
    metadata TEXT,
    branch VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_event_session_id FOREIGN KEY (session_id) REFERENCES ai_session (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_session_conversation_id ON ai_session (conversation_id);
CREATE INDEX IF NOT EXISTS idx_session_event_session_id ON ai_session_event (session_id);
CREATE INDEX IF NOT EXISTS idx_session_event_branch ON ai_session_event (branch, session_id);
