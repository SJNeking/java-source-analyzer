-- ============================================================
-- Unified Storage Schema
-- Shared by: Java Source Analyzer + CodeGuardian AI
-- ============================================================

-- 1. Analysis Results (业务数据 + 统一报告)
CREATE TABLE IF NOT EXISTS analysis_results (
    id              BIGSERIAL PRIMARY KEY,
    project_name    VARCHAR(255) NOT NULL,
    project_version VARCHAR(50),
    commit_sha      VARCHAR(64),
    branch          VARCHAR(100),
    source          VARCHAR(20) NOT NULL,        -- 'static' | 'ai' | 'unified'
    engine_version  VARCHAR(50),                  -- 分析引擎版本
    ai_model        VARCHAR(50),                  -- AI 模型名称 (仅 AI/unified)
    
    -- 统计摘要
    total_issues    INTEGER DEFAULT 0,
    active_issues   INTEGER DEFAULT 0,
    critical_count  INTEGER DEFAULT 0,
    major_count     INTEGER DEFAULT 0,
    minor_count     INTEGER DEFAULT 0,
    info_count      INTEGER DEFAULT 0,
    
    -- 原始 JSON (用于完整还原)
    raw_json        JSONB NOT NULL,
    
    -- RAG 上下文 (AST 切片, 仅 unified 有)
    rag_context     JSONB,
    
    -- 元数据
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    analysis_ms     BIGINT,                       -- 分析耗时(毫秒)
    
    -- 索引
    CONSTRAINT chk_source CHECK (source IN ('static', 'ai', 'unified'))
);

CREATE INDEX IF NOT EXISTS idx_results_project ON analysis_results(project_name, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_results_commit ON analysis_results(commit_sha);
CREATE INDEX IF NOT EXISTS idx_results_source ON analysis_results(source);

-- GIN 索引支持 JSONB 全文检索
CREATE INDEX IF NOT EXISTS idx_results_raw_gin ON analysis_results USING GIN (raw_json);


-- 2. Unified Issues (标准化问题列表, 支持快速查询)
CREATE TABLE IF NOT EXISTS unified_issues (
    id                  BIGSERIAL PRIMARY KEY,
    result_id           BIGINT REFERENCES analysis_results(id) ON DELETE CASCADE,
    
    -- 基础字段
    issue_uuid          UUID NOT NULL DEFAULT gen_random_uuid(),
    source              VARCHAR(10) NOT NULL,       -- 'static' | 'ai' | 'merged'
    rule_key            VARCHAR(100),
    rule_name           VARCHAR(255),
    severity            VARCHAR(20) NOT NULL,       -- 'CRITICAL' | 'MAJOR' | 'MINOR' | 'INFO'
    category            VARCHAR(50),                -- 'BUG' | 'CODE_SMELL' | 'SECURITY' | 'DESIGN'
    
    -- 位置信息
    file_path           VARCHAR(500),
    class_name          VARCHAR(300),
    method_name         VARCHAR(200),
    line_number         INTEGER,
    message             TEXT,
    
    -- AI 特有字段
    confidence          DOUBLE PRECISION,           -- 0.0-1.0
    ai_suggestion       TEXT,
    ai_fixed_code       TEXT,
    ai_reasoning        TEXT,
    auto_filtered       BOOLEAN DEFAULT FALSE,
    
    -- 静态分析特有字段
    cyclomatic_complexity INTEGER,
    loc                 INTEGER,
    related_assets      TEXT[],                     -- PostgreSQL array
    
    -- 关联 ID
    static_issue_id     VARCHAR(100),
    ai_issue_id         VARCHAR(100),
    
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    
    CONSTRAINT chk_issue_source CHECK (source IN ('static', 'ai', 'merged')),
    CONSTRAINT chk_severity CHECK (severity IN ('CRITICAL', 'MAJOR', 'MINOR', 'INFO'))
);

CREATE INDEX IF NOT EXISTS idx_issues_result ON unified_issues(result_id);
CREATE INDEX IF NOT EXISTS idx_issues_file_line ON unified_issues(file_path, line_number);
CREATE INDEX IF NOT EXISTS idx_issues_severity ON unified_issues(severity);
CREATE INDEX IF NOT EXISTS idx_issues_confidence ON unified_issues(confidence) WHERE confidence IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_issues_filtered ON unified_issues(auto_filtered) WHERE auto_filtered = TRUE;


-- 3. Semantic Fingerprints (跳审缓存)
CREATE TABLE IF NOT EXISTS semantic_fingerprints (
    id              BIGSERIAL PRIMARY KEY,
    fingerprint     VARCHAR(32) NOT NULL UNIQUE,    -- SHA-256 前 16 位
    project_name    VARCHAR(255) NOT NULL,
    commit_sha      VARCHAR(64),
    
    -- 缓存的分析结果 ID
    result_id       BIGINT REFERENCES analysis_results(id),
    
    -- 过期策略
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    expires_at      TIMESTAMPTZ,
    hit_count       INTEGER DEFAULT 0,
    
    CONSTRAINT chk_fingerprint_length CHECK (length(fingerprint) >= 8)
);

CREATE INDEX IF NOT EXISTS idx_fingerprints_project ON semantic_fingerprints(project_name);
CREATE INDEX IF NOT EXISTS idx_fingerprints_expires ON semantic_fingerprints(expires_at);


-- 4. Code Embeddings (PGVector - AI 审查用)
-- 需要: -- CREATE EXTENSION IF NOT EXISTS vector; -- (optional, requires pgvector)
CREATE TABLE IF NOT EXISTS code_embeddings (
    id              BIGSERIAL PRIMARY KEY,
    project_name    VARCHAR(255) NOT NULL,
    file_path       VARCHAR(500) NOT NULL,
    method_name     VARCHAR(200),
    class_name      VARCHAR(300),
    
    -- 向量 (384 维 all-MiniLM-L6-v2, 或 1536 维 text-embedding-ada-002)
    embedding       BYTEA,
    
    -- 关联切片
    slice_code      TEXT,                           -- 代码原文
    slice_start     INTEGER,
    slice_end       INTEGER,
    
    -- 元数据
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    result_id       BIGINT REFERENCES analysis_results(id),
    
    -- Token 估算
    token_count     INTEGER
);

CREATE INDEX IF NOT EXISTS idx_embeddings_project ON code_embeddings(project_name);
CREATE INDEX IF NOT EXISTS idx_embeddings_file ON code_embeddings(file_path);

-- HNSW 向量索引 (PGVector 0.5+)
-- CREATE INDEX IF NOT EXISTS idx_embeddings_vector 
--     ON code_embeddings USING hnsw (embedding vector_cosine_ops)
--     WITH (m = 16, ef_construction = 64);


-- 5. Review Reports (审查报告元数据, 轻量查询)
CREATE TABLE IF NOT EXISTS review_reports (
    id              BIGSERIAL PRIMARY KEY,
    project_name    VARCHAR(255) NOT NULL,
    commit_sha      VARCHAR(64) NOT NULL,
    branch          VARCHAR(100),
    
    -- 报告路径 (MinIO)
    minio_bucket    VARCHAR(100) DEFAULT 'codeguardian',
    minio_path      VARCHAR(500),                   -- 如: reviews/{commit}/unified-report.json
    
    -- 统计摘要 (冗余, 避免查 JSON)
    total_issues    INTEGER,
    active_issues   INTEGER,
    critical_count  INTEGER,
    
    -- 状态
    status          VARCHAR(20) DEFAULT 'pending',  -- 'pending' | 'completed' | 'failed'
    
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    
    CONSTRAINT chk_report_status CHECK (status IN ('pending', 'completed', 'failed'))
);

CREATE INDEX IF NOT EXISTS idx_reports_project ON review_reports(project_name, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reports_commit ON review_reports(commit_sha);
CREATE INDEX IF NOT EXISTS idx_reports_status ON review_reports(status);


-- 6. Quality Gates (门禁结果)
CREATE TABLE IF NOT EXISTS quality_gates (
    id              BIGSERIAL PRIMARY KEY,
    project_name    VARCHAR(255) NOT NULL,
    commit_sha      VARCHAR(64),
    branch          VARCHAR(100),
    
    passed          BOOLEAN NOT NULL,
    
    -- 条件
    max_critical    INTEGER DEFAULT 0,
    max_major       INTEGER DEFAULT 10,
    min_confidence  DOUBLE PRECISION DEFAULT 0.5,
    
    -- 实际值
    actual_critical INTEGER,
    actual_major    INTEGER,
    avg_confidence  DOUBLE PRECISION,
    
    -- 原因 (JSON array of strings)
    failure_reasons JSONB,
    
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gates_project ON quality_gates(project_name, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_gates_passed ON quality_gates(passed);


-- ============================================================
-- 辅助视图
-- ============================================================

-- 最新审查报告
CREATE OR REPLACE VIEW v_latest_reports AS
SELECT rr.*, ar.rag_context
FROM review_reports rr
LEFT JOIN analysis_results ar ON ar.commit_sha = rr.commit_sha
WHERE rr.status = 'completed'
ORDER BY rr.created_at DESC;

-- 问题趋势 (按天)
CREATE OR REPLACE VIEW v_issue_trends AS
SELECT 
    DATE(created_at) AS date,
    severity,
    COUNT(*) AS count
FROM unified_issues
WHERE auto_filtered = FALSE
GROUP BY DATE(created_at), severity
ORDER BY date DESC, severity;

-- 跳审命中率
CREATE OR REPLACE VIEW v_cache_stats AS
SELECT 
    COUNT(*) AS total_fingerprints,
    COUNT(*) FILTER (WHERE hit_count > 0) AS hit_fingerprints,
    SUM(hit_count) AS total_hits,
    AVG(hit_count) AS avg_hits,
    MAX(expires_at) AS latest_expiry
FROM semantic_fingerprints;
