# 🔬 Java Source Analyzer — 全方位 Java 项目拆解可视化工具

> **一行命令，任何 Java 项目完全可视化。**

## 🚀 快速开始

### 方式一：仅查看已有分析数据

```bash
./start.sh
```

浏览器打开: http://localhost:8080/views/index.html

### 方式二：分析任意 Java 项目（推荐）

```bash
./start.sh --analyze --sourceRoot=/path/to/java-project
```

前端会实时显示分析进度，分析完自动渲染所有视图。

### 方式三：带实时分析流（WebSocket）

```bash
./start.sh --analyze --sourceRoot=/path/to/project --websocket-port=8887
```

### 方式四：统一审查流程 (静态 + AI 合并)

```bash
# 1. 静态分析
java -jar target/glossary-java-source-analyzer-1.0.jar \
  --sourceRoot /path/to/project --outputDir . --format json --outputFile static-results.json

# 2. AI 审查 (CodeGuardian AI)
python ai_review.py --sourceRoot /path/to/project --context static-results.json --output ai-results.json

# 3. 合并报告
java -cp target/glossary-java-source-analyzer-1.0.jar \
  cn.dolphinmind.glossary.java.analyze.unified.ResultMerger \
  --static static-results.json --ai ai-results.json \
  --sourceRoot /path/to/project \
  --output unified-report.json \
  --model qwen-max
```

## 📊 可视化视图 (8+)

| 视图 | 功能 | 说明 |
|------|------|------|
| 🔗 **力导向图** | 类依赖关系图 | 交互式展示类之间的依赖、继承、实现关系 |
| ⚠️ **质量分析** | 代码质量问题 | 按严重程度/类别展示 Bug、Code Smell、安全问题 |
| 📊 **代码指标** | 代码度量统计 | LOC、复杂度、耦合度、内聚度、注释率等 |
| 🌐 **API 端点** | REST API 列表 | Spring `@RestController` / `@RequestMapping` 自动提取 |
| 🏗️ **架构分层** | 分层架构视图 | Controller → Service → Repository → Entity，自动检测违规 |
| 🔀 **跨文件关系** | 多文件关联 | Java ↔ XML (MyBatis) ↔ SQL ↔ Docker 配置关联 |
| 📁 **项目资产** | 非 Java 文件 | POM、YAML、SQL、Dockerfile、Shell 脚本、Markdown 文档 |
| 🤖 **AI 审查** | AI 修复建议 | 置信度评分、修复代码、推理过程 (需 CodeGuardian AI) |

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (TypeScript)                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │ 力导向图  │ │ 质量分析  │ │ 代码指标  │ │ API 端点视图  │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │ 架构分层  │ │ 跨文件关系│ │ 项目资产  │ │ 🤖 AI 审查   │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘   │
└────────────────────────────┬────────────────────────────────┘
                             │
              unified-report.json (统一报告)
                             │
┌────────────────────────────┴────────────────────────────────┐
│                   ResultMerger (合并器)                       │
│  输入: static-results.json + ai-results.json + 源码         │
│  输出: unified-report.json                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ AST 切片器 (CodeSlicer) → RAG Context → AI 输入    │   │
│  └─────────────────────────────────────────────────────┘   │
└───────────────┬───────────────────────────┬─────────────────┘
                │                           │
┌───────────────┴─────────────┐ ┌──────────┴─────────────────┐
│   静态分析 (本工具)          │ │   AI 审查 (CodeGuardian)   │
│  ┌───────────────────────┐  │ │  ┌───────────────────────┐ │
│  │ 1. 文件扫描 (增量缓存) │  │ │  │ 1. RAG 混合检索        │ │
│  │ 2. JavaParser AST     │  │ │  │ 2. AST 精准切片        │ │
│  │ 3. Spring 分析        │  │ │  │ 3. 多模型 (GPT/Qwen)   │ │
│  │ 4. 质量规则 (150+ 条) │  │ │  │ 4. 置信度评分          │ │
│  │ 5. 跨文件关系         │  │ │  │ 5. 修复代码生成        │ │
│  │ 6. 架构分层检测       │  │ │  └───────────────────────┘ │
│  └───────────────────────┘  │ └────────────────────────────┘
└─────────────────────────────┘
```

## 🔧 技术栈

**后端 (Java)**
- JavaParser — AST 解析
- Gson — JSON 序列化
- ForkJoinPool — 并行文件扫描

**前端 (TypeScript)**
- ECharts 5.5 — 力导向图、饼图、柱状图
- esbuild — 快速构建（代码分割 + ESM 懒加载）
- Component Framework — 轻量级 DOM 渲染框架

**AI 集成 (CodeGuardian)**
- RAG 混合检索 (Vector + BM25)
- 多模型支持 (OpenAI / Qwen / DeepSeek)
- AST 精准切片 (避免无关代码入模)

**CI/CD**
- GitHub Actions — 统一审查流水线
- 语义指纹缓存 — 未变更代码跳审 (~40% 加速)
- 自动 PR 评论 + SARIF 上传

## 📦 输出数据格式

分析完成后，会在 `--outputDir` 生成：

```
analysis-output/
├── FrameworkName_v1.0_full_20260411.json       # 完整分析结果
├── FrameworkName_v1.0_summary_20260411.json    # 摘要
├── FrameworkName_v1.0_glossary_raw_20260411.json # 词汇表
└── FrameworkName_v1.0_<module>_20260411.json   # 按模块拆分

# 统一审查流程额外输出:
├── static-results.json                          # 静态分析结果
├── ai-results.json                              # AI 审查结果
├── rag-context.json                             # RAG 上下文 (AST 切片)
└── unified-report.json                          # 统一审查报告 (含 RAG Context)
```

将 JSON 文件放入 `html/data/` 并更新 `projects.json` 即可在前端查看。

## ⚡ 性能优化

| 技术 | 效果 |
|------|------|
| **增量缓存** | 仅重新分析变更文件（SHA-256 对比） |
| **并行扫描** | ForkJoinPool，最多 8 线程 |
| **缓存合并** | 未变更文件直接从缓存恢复，无需重新解析 |
| **AST 精准切片** | RAG Token 消耗降低 ~96% |
| **语义指纹缓存** | CI/CD 未变更代码跳审 (~40% 加速) |
| **代码分割** | 前端按需加载视图，首屏体积降低 97% |

## 📝 使用场景

1. **接手新项目** — 快速理解代码架构、依赖关系、API 端点
2. **代码审查** — 发现质量问题、架构违规、技术债务
3. **AI 辅助修复** — 获取 AI 生成的修复建议和代码
4. **CI/CD 集成** — PR 自动审查，置信度过滤
5. **重构辅助** — 可视化依赖，识别紧耦合模块
6. **文档生成** — 自动生成 API 列表、架构图、依赖矩阵
7. **团队协作** — 统一理解，减少沟通成本

## 🎯 支持的框架

- Spring Boot / Spring MVC（自动检测）
- MyBatis
- Apache Dubbo
- Netty
- 任何标准 Java 项目

## 📖 完整文档

详见 [FRONTEND_GUIDE.md](html/FRONTEND_GUIDE.md)

## 📄 License

MIT
