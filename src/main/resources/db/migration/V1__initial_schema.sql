-- V1__initial_schema.sql
-- Production-ready schema for CodeGuardian AI
-- Requires PostgreSQL 16+ with PGVector extension

-- 1. Enable PGVector Extension
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Core Tables

CREATE TABLE IF NOT EXISTS analysis_results (
    id              BIGSERIAL PRIMARY KEY,
    project_name    VARCHAR(255) NOT NULL,
    project_version VARCHAR(50),
    commit_sha      VARCHAR(64),
    branch          VARCHAR(100),
    source          VARCHAR(20) NOT NULL,        -- 'static' | 'ai' | 'unified'
    engine_version  VARCHAR(50),
    ai_model        VARCHAR(50),
    
    total_issues    INTEGER DEFAULT 0,
    active_issues   INTEGER DEFAULT 0,
    critical_count  INTEGER DEFAULT 0,
    major_count     INTEGER DEFAULT 0,
    minor_count     INTEGER DEFAULT 0,
    info_count      INTEGER DEFAULT 0,
    
    raw_json        JSONB NOT NULL,
    rag_context     JSONB,
    
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    analysis_ms     BIGINT
);

CREATE TABLE IF NOT EXISTS code_embeddings (
    id              BIGSERIAL PRIMARY KEY,
    project_name    VARCHAR(255) NOT NULL,
    file_path       VARCHAR(500) NOT NULL,
    method_name     VARCHAR(200),
    class_name      VARCHAR(300),
    start_line      INTEGER,
    end_line        INTEGER,
    slice_code      TEXT,
    token_count     INTEGER,
    slice_type      VARCHAR(20),
    metadata_json   JSONB,
    
    -- REAL VECTOR TYPE (768 dimensions for nomic-embed-text)
    embedding       vector(768),
    
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    result_id       BIGINT REFERENCES analysis_results(id),
    
    -- Unique constraint for Upsert operations
    CONSTRAINT unique_embedding UNIQUE (project_name, file_path, method_name, start_line)
);

-- 3. Indexes for Performance

-- HNSW Index for fast Vector Similarity Search (Cosine Distance)
CREATE INDEX IF NOT EXISTS idx_embeddings_vector 
    ON code_embeddings USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- B-Tree Index for metadata filtering
CREATE INDEX IF NOT EXISTS idx_embeddings_project 
    ON code_embeddings (project_name);

CREATE INDEX IF NOT EXISTS idx_analysis_results_project 
    ON analysis_results (project_name, created_at DESC);
