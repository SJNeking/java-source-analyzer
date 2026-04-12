# Operations Runbook

## 1. Health Checks

**Endpoint**: CLI `--health` flag  
**Purpose**: Verify all external dependencies are reachable.

```bash
java -cp target/glossary-java-source-analyzer-1.0.jar \
  cn.dolphinmind.glossary.java.analyze.rag.RagPipelineCli \
  --health \
  --database-url jdbc:postgresql://localhost:15432/postgres
```

**Expected Response**:
```json
{
  "status": "UP",
  "components": {
    "database": { "status": "UP", "version": "16.13" },
    "ollama": { "status": "UP" },
    "minio": { "status": "DOWN", "error": "Connection refused" }
  }
}
```

**Alert Rules**:
- If `status` is `DOWN` → Check respective service logs.
- If `database` is `DOWN` → Check Docker container `redisson_brain_db`.

## 2. Metrics Scraping

**Endpoint**: CLI `--metrics` flag  
**Format**: Prometheus Text

```bash
java -cp target/glossary-java-source-analyzer-1.0.jar \
  cn.dolphinmind.glossary.java.analyze.rag.RagPipelineCli \
  --metrics
```

**Key Metrics**:
- `codeguardian_analyses_total`: Total static analyses run.
- `codeguardian_rag_reviews_total`: Total AI reviews run.
- `codeguardian_errors_total`: Total errors encountered.
- `codeguardian_rag_latency_seconds`: Average RAG latency.

## 3. Database Migration

**Tool**: Flyway 9.22.3

```bash
# Run all pending migrations
mvn flyway:migrate

# Check current version
mvn flyway:info
```

**Emergency Rollback**:
```bash
mvn flyway:undo
```

## 4. Log Analysis

**Logs**: Standard Output (CLI) / File (if configured in `application.yml`).  
**Pattern**: `[WARNING]` or `[ERROR]`.

**Common Errors**:
- `Connection refused`: Ollama/MinIO not running.
- `type "vector" does not exist`: PGVector extension missing.
- `Path traversal detected`: Malicious input blocked.

## 5. Disaster Recovery

1. **Stop Services**: `docker stop redisson_brain_db`
2. **Backup Data**: `pg_dump -U root postgres > backup.sql`
3. **Restore**: `docker exec -i redisson_brain_db psql -U root postgres < backup.sql`
