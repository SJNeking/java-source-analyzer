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

## 📊 7 大可视化视图

| 视图 | 功能 | 说明 |
|------|------|------|
| 🔗 **力导向图** | 类依赖关系图 | 交互式展示类之间的依赖、继承、实现关系 |
| ⚠️ **质量分析** | 代码质量问题 | 按严重程度/类别展示 Bug、Code Smell、安全问题 |
| 📊 **代码指标** | 代码度量统计 | LOC、复杂度、耦合度、内聚度、注释率等 |
| 🌐 **API 端点** | REST API 列表 | Spring `@RestController` / `@RequestMapping` 自动提取 |
| 🏗️ **架构分层** | 分层架构视图 | Controller → Service → Repository → Entity，自动检测违规 |
| 🔀 **跨文件关系** | 多文件关联 | Java ↔ XML (MyBatis) ↔ SQL ↔ Docker 配置关联 |
| 📁 **项目资产** | 非 Java 文件 | POM、YAML、SQL、Dockerfile、Shell 脚本、Markdown 文档 |

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (TypeScript)                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │ 力导向图  │ │ 质量分析  │ │ 代码指标  │ │ API 端点视图  │   │
│  │ (ECharts)│ │ 仪表盘   │ │ 图表     │ │ 树状列表     │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                    │
│  │ 架构分层  │ │ 跨文件关系│ │ 项目资产  │                    │
│  │ 矩阵视图  │ │ 表格     │ │ 树状结构  │                    │
│  └──────────┘ └──────────┘ └──────────┘                    │
└────────────────────────────┬────────────────────────────────┘
                             │
                    JSON 分析结果文件 (*.json)
                             │
┌────────────────────────────┴────────────────────────────────┐
│                      后端 (Java)                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              SourceUniversePro.runAnalysis()          │    │
│  │                                                       │    │
│  │  1. 文件扫描 ─── 增量缓存 (SHA-256)                    │    │
│  │  2. JavaParser ─── AST 解析                           │    │
│  │  3. Spring 分析 ─── @RestController, @Autowired       │    │
│  │  4. 质量规则引擎 ─── 150+ 条规则                      │    │
│  │  5. 跨文件关系 ─── RelationEngine                     │    │
│  │  6. 架构分层 ─── 自动识别层并检测违规                   │    │
│  │  7. 代码指标 ─── LOC, 复杂度, 耦合                    │    │
│  │  8. WebSocket ─── 实时推送进度                        │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## 🔧 技术栈

**后端 (Java)**
- JavaParser — AST 解析
- Java-WebSocket — 实时进度推送
- Gson — JSON 序列化
- ForkJoinPool — 并行文件扫描

**前端 (TypeScript)**
- ECharts 5.5 — 力导向图、饼图、柱状图
- esbuild — 快速构建（毫秒级）
- WebSocket Client — 实时连接分析进度

## 📦 输出数据格式

分析完成后，会在 `--outputDir` 生成：

```
analysis-output/
├── FrameworkName_v1.0_full_20260411.json       # 完整分析结果
├── FrameworkName_v1.0_summary_20260411.json    # 摘要
├── FrameworkName_v1.0_glossary_raw_20260411.json # 词汇表
└── FrameworkName_v1.0_<module>_20260411.json   # 按模块拆分
```

将 JSON 文件放入 `nginx/html/data/` 并更新 `projects.json` 即可在前端查看。

## ⚡ 性能优化

| 技术 | 效果 |
|------|------|
| **增量缓存** | 仅重新分析变更文件（SHA-256 对比） |
| **并行扫描** | ForkJoinPool，最多 8 线程 |
| **缓存合并** | 未变更文件直接从缓存恢复，无需重新解析 |
| **WebSocket 流** | 分析过程中实时显示进度，无需等待完成 |

## 📝 使用场景

1. **接手新项目** — 快速理解代码架构、依赖关系、API 端点
2. **代码审查** — 发现质量问题、架构违规、技术债务
3. **重构辅助** — 可视化依赖，识别紧耦合模块
4. **文档生成** — 自动生成 API 列表、架构图、依赖矩阵
5. **团队协作** — 统一理解，减少沟通成本

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
