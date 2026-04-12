# Developer Guide

## 1. Quick Start

### Prerequisites
- JDK 8+
- Maven 3.8+
- Node.js 18+
- Docker (for local DB/Ollama)

### Build
```bash
# Backend
mvn clean package -DskipTests

# Frontend
cd html && npm install && npm run build
```

### Run Tests
```bash
mvn test
```

## 2. Project Structure

```
├── src/main/java/.../analyze/
│   ├── unified/              # Unified Data Model
│   ├── slicing/              # AST Code Slicer
│   ├── rag/                  # RAG Pipeline
│   │   ├── service/          # Embedding Services
│   │   ├── store/            # Vector Stores (In-Memory, PGVector)
│   │   ├── search/           # Hybrid Search (Vector + BM25)
│   │   ├── llm/              # LLM Clients (OpenAI/Ollama)
│   │   └── prompt/           # Prompt Templates
│   ├── security/             # Security Utilities
│   └── monitoring/           # Health Checks & Metrics
├── src/test/java/.../        # Integration & Unit Tests
├── html/src/                 # TypeScript Frontend
└── docs/                     # Documentation
```

## 3. Adding a New Quality Rule

1. Create a rule in `QualityIssue` format:
```java
UnifiedIssue issue = UnifiedIssue.builder()
    .source(IssueSource.AI)
    .ruleKey("AI_RULE_001")
    .ruleName("Missing Null Check")
    .severity("MAJOR")
    .message("Method does not check for null parameter")
    .confidence(0.90)
    .build();
```

2. Add it to the `issues` list in `RagPipeline.review()`.

## 4. Debugging Tips

### Enable Detailed Logging
Set `logging.level: DEBUG` in `application.yml`.

### Check Vector Store
```bash
java -cp target/glossary-java-source-analyzer-1.0.jar \
  cn.dolphinmind.glossary.java.analyze.rag.RagPipelineCli \
  --sourceRoot . --analysisResult static.json --query "test"
```

## 5. Contribution Guidelines

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/my-feature`).
3. Commit your changes with a descriptive message.
4. Push to the branch (`git push origin feature/my-feature`).
5. Open a Pull Request.
