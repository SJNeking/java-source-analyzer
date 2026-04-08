# Java Source Analyzer - 使用指南

一个面向完整 Java 项目的语义分析引擎，能够从源码、配置、SQL、Mapper、Dockerfile 等 11 种文件类型中提取结构化知识，输出 JSON 格式的语义资产库。

---

## 快速开始

### 1. 构建项目

```bash
cd /Users/mingxilv/learn/java-source-analyzer
mvn clean package -Dmaven.test.skip=true
```

构建成功后，会在 `target/` 目录生成一个 **3.4MB 的 Fat JAR**，包含所有依赖，可以直接运行。

### 2. 运行分析

```bash
# 基本用法：分析任意 Java 项目
java -jar target/glossary-java-source-analyzer-1.0.jar \
  --sourceRoot /path/to/your/java/project

# 完整参数
java -jar target/glossary-java-source-analyzer-1.0.jar \
  --sourceRoot /path/to/project \
  --outputDir /tmp/output \
  --version 2.0 \
  --artifactName MyFramework \
  --internalPkgPrefix com.mycompany
```

### 3. 查看输出

所有 JSON 文件输出到指定的 `--outputDir`（默认 `./dev-ops/output`）：

```
/tmp/output/
├── myframework_v2.0_full_20260408.json          # 全量扫描结果
├── myframework_v2.0_glossary_raw_20260408.json  # 原始术语表（用于翻译）
├── myframework_v2.0_core_20260408.json          # 按模块拆分
├── myframework_v2.0_web_20260408.json
├── myframework_semantic_dictionary.json          # 语义字典（类名后缀/方法名前缀模式）
```

---

## 命令行参数

| 参数 | 必填 | 默认值 | 说明 |
|---|---|---|---|
| `--sourceRoot` | 否 | 内置默认路径 | 要分析的 Java 项目根目录 |
| `--outputDir` | 否 | `./dev-ops/output` | JSON 输出目录 |
| `--version` | 否 | 从 pom.xml 自动检测 | 版本号字符串 |
| `--artifactName` | 否 | 从 pom.xml 自动检测 | 项目名称 |
| `--internalPkgPrefix` | 否 | `java` | 内部包前缀（用于字段分类） |
| `--help` | 否 | - | 显示帮助信息 |

---

## 支持的文件类型

| 文件类型 | 扩展名 | 提取内容 |
|---|---|---|
| **Java 源码** | `.java` | 类/方法/字段 AST、注释、调用图、继承关系、方法体源码 |
| **Maven 构建** | `pom.xml` | groupId/artifactId/version、依赖、插件、模块、profiles |
| **应用配置** | `application.yml/yaml/properties` | 端口、数据源、Redis、中间件检测 |
| **Bootstrap 配置** | `bootstrap.yml/yaml` | 注册中心、配置中心 |
| **SQL 脚本** | `.sql` | CREATE TABLE、列定义、索引、INSERT/ALTER 统计 |
| **MyBatis Mapper** | `*Mapper.xml` | namespace、select/insert/update/delete、resultMap、表引用 |
| **日志配置** | `logback.xml/log4j2.xml` | root level、loggers、appenders、日志路径 |
| **Dockerfile** | `Dockerfile` | 基础镜像、暴露端口、ENV、ENTRYPOINT/CMD、构建阶段 |
| **Docker Compose** | `docker-compose.yml` | 服务列表、镜像、中间件栈 |
| **Shell 脚本** | `.sh` | 环境变量、启动命令、Spring Profile、脚本用途 |
| **Markdown 文档** | `README.md/*.md` | 标题、章节、代码块、链接、项目描述 |

---

## 输出 JSON 结构详解

### 顶层结构

```json
{
  "framework": "my-framework",
  "version": "2.0",
  "scan_date": "2026-04-08 16:43:00",
  "project_type": { ... },
  "cross_file_relations": { ... },
  "comment_coverage": { ... },
  "assets": [ ... ],
  "dependencies": [ ... ],
  "project_assets": { ... }
}
```

### 项目类型检测

```json
{
  "project_type": {
    "primary_type": "SPRING_BOOT",
    "all_types": ["SPRING_BOOT", "MYBATIS"],
    "evidence": [
      "application.yml found → Spring Boot",
      "3 MyBatis mapper XML files found"
    ]
  }
}
```

### 注释覆盖率

```json
{
  "comment_coverage": {
    "class_comment_coverage_pct": 88.9,
    "method_comment_coverage_pct": 50.2,
    "field_comment_coverage_pct": 30.4,
    "total_classes": 120,
    "classes_with_comments": 107
  }
}
```

### 跨文件关联

```json
{
  "cross_file_relations": {
    "total_relations": 15,
    "relations_by_type": {
      "JAVA_TO_MAPPER": [
        {
          "source_path": "src/main/resources/mapper/UserMapper.xml",
          "source_asset": "Mapper: com.example.mapper.UserMapper",
          "target_asset": "Java: com.example.mapper.UserMapper",
          "relation_type": "JAVA_TO_MAPPER",
          "evidence": "namespace=\"com.example.mapper.UserMapper\"",
          "confidence": 0.95
        }
      ],
      "SQL_TO_ENTITY": [ ... ],
      "CONFIG_TO_CLASS": [ ... ]
    }
  }
}
```

### Java 类资产

```json
{
  "address": "com.example.service.UserService",
  "kind": "CLASS",
  "description": "用户服务，处理用户注册、登录、权限校验",
  "comment_details": {
    "summary": "用户服务",
    "description": "用户服务，处理用户注册、登录、权限校验",
    "params": [],
    "return_description": "",
    "throws": [],
    "deprecated": "",
    "since": "",
    "author": "",
    "see": [],
    "semantic_notes": ["Thread-Safe"],
    "raw_comment": "/**\n * 用户服务\n * 线程安全\n */"
  },
  "source_file": "/path/to/UserService.java",
  "modifiers": ["public"],
  "import_dependencies": ["org.springframework.stereotype.Service", ...],
  "annotation_params": [
    {
      "name": "Service",
      "parameters": [{"key": "value", "value": "\"userService\""}]
    }
  ],
  "methods_full": [ ... ],
  "methods_intent": [ ... ],
  "fields_matrix": [ ... ],
  "constructor_matrix": [ ... ]
}
```

### 方法级信息

```json
{
  "address": "com.example.service.UserService#getUser(java.lang.Long)",
  "name": "getUser",
  "description": "根据 ID 获取用户信息",
  "comment_details": {
    "summary": "根据 ID 获取用户信息",
    "params": [{"name": "id", "description": "用户 ID"}],
    "return_description": "用户对象，不存在时返回 null",
    "throws": [{"exception": "UserNotFoundException", "description": "用户不存在"}]
  },
  "modifiers": ["public"],
  "line_start": 42,
  "line_end": 65,
  "signature": "public User getUser(Long id)",
  "source_code": "public User getUser(Long id) {\n  ...完整方法源码...\n}",
  "body_code": "if (id == null) throw ...; return userRepository.findById(id);",
  "code_summary": "2 个条件分支, 3 次方法调用, 1 个返回点",
  "key_statements": [
    {"type": "CONDITION", "condition": "id == null", "line": "44"},
    {"type": "EXTERNAL_CALL", "target": "userRepository.findById", "line": "50"}
  ],
  "line_count": 24,
  "tags": ["Query", "Validation"]
}
```

---

## 使用场景

### 场景 1：分析开源框架源码

```bash
java -jar target/glossary-java-source-analyzer-1.0.jar \
  --sourceRoot ~/projects/spring-framework \
  --outputDir ~/analysis/spring \
  --artifactName "Spring Framework"
```

输出会包含：
- 所有核心类的完整结构
- 方法调用关系图
- 继承/实现关系
- 注释覆盖率报告
- 术语表（可用于翻译）

### 场景 2：企业内部项目知识沉淀

```bash
java -jar target/glossary-java-source-analyzer-1.0.jar \
  --sourceRoot /workspace/pay-mall-service \
  --outputDir /docs/knowledge-base \
  --version 3.2
```

可用于：
- 新人入职快速了解项目结构
- 架构评审
- 生成术语表，辅助 AI 代码理解
- 生成 RAG 知识库

### 场景 3：项目迁移/重构参考

```bash
java -jar target/glossary-java-source-analyzer-1.0.jar \
  --sourceRoot /old-project \
  --outputDir /migration-reference
```

输出包含：
- SQL 表结构提取
- MyBatis Mapper ↔ Java 接口映射关系
- 配置文件中的中间件信息
- Docker 部署信息

---

## 输出文件用途

| 文件 | 用途 |
|---|---|
| `*_full_*.json` | 完整分析结果，用于可视化、知识图谱构建 |
| `*_glossary_raw_*.json` | 原始术语表，可送入翻译系统 |
| `*_module_*.json` | 按模块拆分，便于分模块分析 |
| `*_semantic_dictionary.json` | 类名后缀和方法名前缀模式，用于命名规范检查 |

---

## 项目增量学习

每次扫描后，项目专属词汇表会自动保存到项目根目录的 `.universe/tech-glossary.json`。

下次扫描同一项目时，会自动加载已有词汇，翻译质量会逐步提升。

```
your-project/
├── src/
├── pom.xml
└── .universe/
    └── tech-glossary.json   ← 自动生成的项目专属词典
```

---

## 性能调优

对于大型项目，可通过以下方式优化：

1. **文件扫描自动过滤**：内置 10MB 文件大小限制和扩展名白名单
2. **目录排除**：自动忽略 `target/`, `.git/`, `.idea/`, `node_modules/` 等
3. **方法调用过滤**：只分析 public/protected 方法的跨类调用，减少噪音

---

## 常见问题

### Q: 扫描报错 "No such file or directory"
确保 `--sourceRoot` 指向的是 Java 项目的**根目录**（包含 pom.xml 或 src/ 的目录）。

### Q: 输出中没有方法体源码
检查被扫描项目的源码是否有方法体（abstract/native 方法没有 body）。

### Q: 跨文件关联关系很少
确保项目中包含 MyBatis Mapper XML 文件和 SQL 文件，且 namespace 与 Java 类匹配。

### Q: 注释覆盖率很低
这是正常的，反映项目实际注释情况。可用来指导代码质量改进。

---

## 脚本工具

项目提供了便捷脚本：

```bash
# 使用当前项目路径快速分析
./scripts/analyze.sh /path/to/target/project

# 带自定义输出目录
./scripts/analyze.sh /path/to/target/project /tmp/output
```
