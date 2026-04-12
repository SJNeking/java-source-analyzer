# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added
- Phase 7: Monitoring & Observability
  - `HealthChecker` for DB/Ollama/MinIO connectivity checks
  - `MetricsCollector` with Prometheus-compatible metrics export
  - `--health` and `--metrics` CLI flags
- Phase 6: Security Hardening
  - `SecurityUtils` for path traversal, extension, and log injection protection
  - PostgreSQL JDBC upgraded to 42.7.3 (CVE-2024-1597 fix)
  - `.env.example` for secrets management
- Phase 5: Database Migration Automation
  - Flyway integration (v9.22.3)
  - `V1__initial_schema.sql` (6 tables + 3 views)
- Phase 4: Integration Tests
  - `RagPipelineTest` with 5 tests (Slicing, Vector Search, Prompt, Embedding, JSON Parsing)
- Phase 3: Frontend AI View
  - `AiReviewView` with confidence filtering and fix suggestions
  - `unified-report-loader.ts` for data adaptation
- Phase 2: Production Dependencies
  - Jedis (Redis client), MinIO SDK, Flyway Core
  - `AppConfig` with YAML config + environment variable override
- Phase 1: Code Cleanup
  - Refactored 14 frontend views to Component Framework
  - ESM Code Splitting (15 chunks, 11KB entry)
  - `nomic-embed-text` verified (768 dimensions)

## [1.0.0] - 2026-04-12

### Added
- Initial release of CodeGuardian AI integration
- Unified Data Model (`UnifiedIssue`, `UnifiedReport`)
- Result Merger CLI (Static + AI → Unified)
- RAG Pipeline with Ollama/OpenAI support
- Frontend Component Framework (TypeScript)
