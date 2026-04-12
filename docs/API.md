# API Reference

## 1. Unified Report Schema

The output of `ResultMerger` and `RagPipelineCli` follows a unified JSON schema.

```json
{
  "projectName": "string",
  "projectVersion": "string",
  "commitSha": "string",
  "branch": "string",
  "timestamp": 1712900000000,
  "analysisDurationMs": 12000,
  "staticEngine": {
    "name": "Java Source Analyzer",
    "version": "1.0.0"
  },
  "aiEngine": {
    "name": "CodeGuardian AI",
    "version": "1.0.0",
    "modelUsed": "qwen2.5-coder:32b"
  },
  "summary": {
    "totalIssues": 191,
    "activeIssues": 191,
    "critical": 1,
    "major": 17,
    "minor": 173,
    "info": 0,
    "aiAvgConfidence": 0.85,
    "aiHighConfidenceRate": 100.0,
    "byCategory": {
      "BUG": 28,
      "CODE_SMELL": 239,
      "SECURITY": 34
    }
  },
  "ragContext": {
    "totalSlices": 191,
    "totalEstimatedTokens": 12824,
    "slices": [...]
  },
  "issues": [
    {
      "id": "uuid",
      "source": "static|ai|merged",
      "severity": "CRITICAL|MAJOR|MINOR|INFO",
      "category": "BUG|CODE_SMELL|SECURITY|DESIGN",
      "ruleKey": "RSPEC-123",
      "ruleName": "Short description",
      "filePath": "src/main/java/.../File.java",
      "className": "com.example.Class",
      "methodName": "doSomething",
      "line": 42,
      "message": "Detailed issue message",
      "confidence": 0.85,
      "aiSuggestion": "How to fix this issue",
      "aiFixedCode": "public void doSomething() { ... }",
      "aiReasoning": "Why this is a problem"
    }
  ]
}
```

## 2. CLI Options

### RagPipelineCli

```bash
java -cp target/glossary-java-source-analyzer-1.0.jar \
  cn.dolphinmind.glossary.java.analyze.rag.RagPipelineCli [OPTIONS]
```

| Option | Required | Default | Description |
|--------|----------|---------|-------------|
| `--sourceRoot` | Yes | - | Java source root directory |
| `--analysisResult` | Yes | - | Static analysis JSON path |
| `--health` | No | - | Run health checks and exit |
| `--metrics` | No | - | Print Prometheus metrics and exit |
| `--output` | No | stdout | Output JSON file path |
| `--query` | No | "review code quality" | RAG search query |
| `--embedding-provider` | No | "ollama" | "ollama" or "openai" |
| `--llm-model` | No | "qwen2.5-coder:32b" | LLM model name |
| `--database-url` | No | application.yml | PostgreSQL JDBC URL |

### ResultMerger

```bash
java -cp target/glossary-java-source-analyzer-1.0.jar \
  cn.dolphinmind.glossary.java.analyze.unified.ResultMerger [OPTIONS]
```

| Option | Required | Default | Description |
|--------|----------|---------|-------------|
| `--static` | Yes | - | Static analysis JSON path |
| `--ai` | No | - | AI review JSON path |
| `--sourceRoot` | No | - | Source root for RAG slicing |
| `--output` | No | stdout | Unified report JSON path |
