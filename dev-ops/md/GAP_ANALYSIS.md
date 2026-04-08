# PLAN.md vs Implementation - Gap Analysis

## ✅ Fully Completed

### 1. Method Source Code Extraction (PLAN Section 四)
| Requirement | Status | Implementation |
|---|---|---|
| source_code (full method) | ✅ | `extractNodeSource()` + `extractCallableBody()` |
| body_code (body only) | ✅ | `extractCallableBody()` |
| line_count | ✅ | `calculateLineCount()` |
| key_statements (if/throw/return/calls) | ✅ | `extractKeyStatements()` - 5 types |
| code_summary | ✅ | `summarizeMethodBody()` |
| line_start/line_end | ✅ | Already existed, retained |

### 2. Structured Comment Extraction (PLAN Section 五)
| Requirement | Status | Implementation |
|---|---|---|
| summary (first sentence) | ✅ | `extractCommentDetails()` |
| description (cleaned) | ✅ | `cleanText()` + extractCommentDetails |
| @param extraction | ✅ | Parsed into `[{name, description}]` |
| @return extraction | ✅ | `return_description` field |
| @throws extraction | ✅ | Parsed into `[{exception, description}]` |
| @deprecated | ✅ | Dedicated field |
| @since | ✅ | Dedicated field |
| @author | ✅ | Dedicated field |
| @see | ✅ | List field |
| semantic_notes (thread-safe, etc.) | ✅ | `extractSemanticNotes()` - 12 keywords |
| raw_comment (preserve original) | ✅ | `raw_comment` field |
| brute-force fallback | ✅ | `bruteForceCommentFromLines()` |

### 3. Multi-File Parser Framework (PLAN Section 六)
| Requirement | Status | Implementation |
|---|---|---|
| File discovery layer | ✅ | `ProjectScanner.walk()` with exclusion rules |
| File parser layer | ✅ | `FileParser` interface + 6 implementations |
| Asset standardization | ✅ | `FileAsset` unified model with `toMap()` |
| Parser registry | ✅ | `ParserRegistry` with runtime extensibility |
| Directory exclusion | ✅ | target, .git, .idea, node_modules, etc. |

### 4. File Type Support - Phase 1 (Core Loop)
| File Type | Status | Parser |
|---|---|---|
| .java | ✅ | `SourceUniversePro` (enhanced) |
| pom.xml | ✅ | `PomXmlParser` - groupId, artifactId, version, deps, plugins, modules, profiles, properties |
| application.yml/properties | ✅ | `ConfigFileParser` - port, datasource, redis, middleware detection |
| .sql | ✅ | `SqlFileParser` - CREATE TABLE, columns, indexes, table refs |
| *Mapper.xml | ✅ | `MyBatisXmlParser` - namespace, statements, resultMaps, table refs |
| README.md | ✅ | `MarkdownParser` - headings, sections, code blocks, links |

### 5. File Type Support - Phase 2 (Deployment)
| File Type | Status | Parser |
|---|---|---|
| Dockerfile | ✅ | `DockerfileParser` - base image, ports, ENV, ENTRYPOINT/CMD, build stages |
| logback.xml | ❌ | Not implemented |
| docker-compose.yml | ❌ | Not implemented |
| .sh scripts | ❌ | Not implemented |
| bootstrap.yml | ✅ | `ConfigFileParser` (same as application.yml) |
| Spring XML | ❌ | Not implemented |

### 6. CLI Arguments (PLAN Section 10, Item 1)
| Requirement | Status | Implementation |
|---|---|---|
| Remove hardcoded paths | ✅ | `--sourceRoot`, `--outputDir` |
| Version override | ✅ | `--version` |
| Artifact name override | ✅ | `--artifactName` |
| Package prefix | ✅ | `--internalPkgPrefix` |
| Help message | ✅ | `--help` |

### 7. Fixed All Identified Bugs (PLAN Section 7)
| Bug | Status | Fix |
|---|---|---|
| methods field overwritten | ✅ | Renamed to `methods_full`, `methods_intent` |
| Hardcoded paths | ✅ | CLI args |
| Resource loading inconsistency | ✅ | All use `ClassLoader.getResource()` with `InputStreamReader` |
| Null safety on namingTags | ✅ | Guard clauses added |

### 8. Build System
| Requirement | Status | Implementation |
|---|---|---|
| Fat JAR with dependencies | ✅ | maven-shade-plugin |
| Main class in Manifest | ✅ | ManifestResourceTransformer |
| Signature file filtering | ✅ | Excludes in shade plugin |
| Java 8 target | ✅ | maven-compiler-plugin source/target 8 |

---

## ❌ Not Yet Implemented (Gaps)

### 1. Cross-File Relationship Engine (PLAN Section 六.4, 十一.7)
**Status: NOT IMPLEMENTED**
This is the most significant remaining item.

Required relationships:
- Java class ↔ MyBatis Mapper XML (via namespace matching)
- Config item ↔ @Value/@ConfigurationProperties class
- POM dependency ↔ actual imported framework classes
- SQL table ↔ Entity/DTO class
- Dockerfile → startup JAR
- Shell script → startup command/profile

### 2. Unified Project-Level Output JSON (PLAN Section 三)
**Status: PARTIALLY IMPLEMENTED**

Current output structure:
```
{
  "framework": "...",
  "version": "...",
  "scan_date": "...",
  "assets": [...],          // Java classes
  "dependencies": [...],    // Java call graph
  "project_assets": {       // Non-Java files
    "maven_pom": [...],
    "yaml_config": [...],
    "sql_script": [...],
    ...
  }
}
```

Missing from the planned structure:
- No `project_overview` section
- No `cross_file_relations` section
- No `comment_coverage_metrics` section
- No per-module breakdown for non-Java assets

### 3. Comment Coverage Metrics (PLAN Section 五.5)
**Status: NOT IMPLEMENTED**

Required metrics:
- Class comment coverage (%)
- Method comment coverage (%)
- Field comment coverage (%)
- Javadoc completeness score

### 4. Import Dependency Extraction (PLAN Section 二.1)
**Status: NOT EXPLICITLY IMPLEMENTED**

The plan asks for:
- import dependency tracking per Java file
- Package-level hierarchy structure

### 5. Annotation Parameter Extraction (PLAN Section 二.1)
**Status: BASIC**

Current: extracts annotation names only
Missing: annotation parameter values (e.g., `@RequestMapping(value="/api", method=GET)`)

### 6. Project Type Detection (PLAN Section 八.1)
**Status: NOT IMPLEMENTED**

Should auto-detect:
- Maven single/multi module
- Gradle
- Spring Boot
- Spring Cloud
- MyBatis
- Dubbo
- Plain Java SE

### 7. Multi-Module Deep Support (PLAN Section 八.2)
**Status: BASIC**

Current: extracts module names from pom.xml
Missing:
- Parent-child module hierarchy
- Inter-module dependency graph
- Cross-module class references

### 8. File Size Threshold / Performance Control (PLAN Section 八.4)
**Status: NOT IMPLEMENTED**

Missing:
- File size threshold configuration
- Whitelist/blacklist
- Incremental scan
- Cache mechanism

### 9. Unit Tests (PLAN Section 10, Item 5)
**Status: NOT IMPLEMENTED**

Required tests:
- CamelCase splitting
- Dictionary translation
- Method intent tag recognition
- Module extraction
- Comment extraction
- Version detection

### 10. Remaining File Types (Phase 3)
| File Type | Status |
|---|---|
| build.gradle | ❌ |
| .env | ❌ |
| k8s yaml | ❌ |
| OpenAPI/Swagger | ❌ |
| .jsp/.ftl/.vm | ❌ |
| package.json | ❌ |
| *.json config files | ❌ |

---

## 📊 Completion Summary

### By Priority Tier

| Tier | Items | Completed | Rate |
|---|---|---|---|
| **Phase 1** (Core Loop) | 6 file types + method extraction + comment parsing | 6/6 | **100%** |
| **Phase 2** (Deployment) | 6 file types | 1/6 | **17%** |
| **Phase 3** (Ecosystem) | 8+ file types | 0/8+ | **0%** |
| **Architecture** | Parser framework + unified model | 3/4 | **75%** |
| **Engineering** | CLI args + fat JAR + resource loading | 3/3 | **100%** |
| **Cross-file relations** | Relationship engine | 0/1 | **0%** |
| **Output structure** | Unified JSON output | 2/3 | **67%** |
| **Testing** | Unit tests | 0/1 | **0%** |

### Overall: ~55% of PLAN.md completed

### What's Done Well
✅ All **Phase 1** core loop items complete
✅ Method source code extraction fully working
✅ Structured Javadoc parsing fully working
✅ Parser framework is clean and extensible
✅ CLI arguments eliminate hardcoded paths
✅ Fat JAR builds and runs end-to-end
✅ All identified bugs fixed

### What's Missing (by priority)
1. **🔴 HIGH** - Cross-file relationship engine (the "真正理解项目" piece)
2. **🟡 MEDIUM** - Comment coverage metrics
3. **🟡 MEDIUM** - Unified project-level output JSON
4. **🟡 MEDIUM** - Project type detection
5. **🟢 LOW** - Phase 2/3 file types (logback, docker-compose, .sh, etc.)
6. **🟢 LOW** - Unit tests
7. **🟢 LOW** - Multi-module deep support
8. **🟢 LOW** - Performance controls
