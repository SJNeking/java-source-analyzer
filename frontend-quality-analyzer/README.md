# 🔬 Frontend Quality Analyzer

> **一行命令，任何前端项目完全质量可视化。对标 Java Source Analyzer 的前端版本。**

## 🚀 快速开始

### 安装

```bash
cd frontend-quality-analyzer
npm install
npm run build
```

### 分析任意前端项目

```bash
node dist/cli.js --sourceRoot=/path/to/frontend-project
```

### 完整参数

```bash
node dist/cli.js \
  --sourceRoot /path/to/project \
  --outputDir ./analysis-output \
  --format all \
  --verbose
```

## 📊 输出格式

| 格式 | 说明 | 文件扩展名 |
|------|------|-----------|
| **JSON** | 完整分析结果，供可视化/CI使用 | `.json` |
| **HTML** | 独立的可交互质量报告 | `.html` |
| **Markdown** | GitHub 友好的摘要 | `.md` |

输出文件命名格式：
```
projectname_v1.0_full_20260411095245.json
projectname_v1.0_summary_20260411095245.json
projectname_v1.0_full_20260411095245.html
projectname_v1.0_full_20260411095245.md
```

## 📏 88 条质量规则

| 类别 | 规则数 | 示例 |
|------|--------|------|
| **TypeScript** | 6 | 隐式 any、缺少返回类型、显式 any |
| **React** | 6 | useEffect 依赖、缺少 key、状态不可变 |
| **Security** | 6 | XSS、CSRF、硬编码密钥、不安全加密 |
| **Vue** | 10 | v-for/v-if 共用、缺少 props 校验、计算属性副作用 |
| **Performance** | 10 | 未懒加载、内联样式/函数、大列表未虚拟化、完整库导入 |
| **Memory** | 8 | 事件监听未清理、定时器未清除、Observer 未断开、订阅未取消 |
| **Accessibility** | 10 | 缺少 alt、缺少 aria-label、autofocus、跳过标题层级 |
| **Architecture** | 8 | 组件嵌套过深、页面直接调 API、Props 过多、魔术字符串 |
| **Styling** | 8 | !important 滥用、内联样式过多、缺少 CSS 变量、硬编码颜色 |
| **Testing** | 6 | 缺少测试、无断言、过度 mock、test.only 遗留 |
| **Build** | 5 | 未开启 strict、源码暴露、未 tree-shaking、缺少路径别名 |
| **i18n** | 5 | 硬编码文本、日期未本地化、数字未格式化、手动复数处理 |

## 🔧 命令行参数

| 参数 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `--sourceRoot` | ✅ | — | 要分析的前端项目根目录 |
| `--outputDir` | — | `./analysis-output` | 输出目录 |
| `--format` | — | `all` | 输出格式：`json`、`html`、`markdown`、`all` |
| `--websocket-port` | — | `0` | WebSocket 端口（实时进度推送） |
| `--config` | — | — | 规则配置文件路径 |
| `--exclude` | — | `node_modules,dist,.git` | 排除的 glob 模式 |
| `--verbose` | — | — | 启用调试日志 |

## 📁 项目架构

```
frontend-quality-analyzer/
├── src/
│   ├── types/             # TypeScript 类型定义
│   │   └── index.ts       # QualityIssue, Severity, Reporter, etc.
│   ├── engine/
│   │   └── RuleEngine.ts  # 规则引擎（注册、执行、质量门禁、技术债务）
│   ├── parser/
│   │   ├── TypeScriptParser.ts   # TypeScript Compiler API 真实 AST 解析
│   │   └── BabelParser.ts        # Babel JSX/Vue 解析
│   ├── rules/
│   │   ├── index.ts              # 规则注册中心
│   │   ├── typescript/           # 6 条 TS 规则
│   │   ├── react/                # 6 条 React 规则
│   │   ├── security/             # 6 条安全规则
│   │   ├── vue/                  # 10 条 Vue 规则
│   │   ├── performance/          # 10 条性能规则
│   │   ├── memory/               # 8 条内存泄漏规则
│   │   ├── accessibility/        # 10 条无障碍规则
│   │   ├── architecture/         # 8 条架构规则
│   │   ├── styling/              # 8 条样式规则
│   │   ├── testing/              # 6 条测试规则
│   │   ├── build-config/         # 5 条构建规则
│   │   └── i18n/                 # 5 条国际化规则
│   ├── reporters/
│   │   └── index.ts       # JSON / HTML / Markdown 报告
│   ├── cli.ts             # CLI 入口
│   └── index.ts           # 库公共 API
├── package.json
├── tsconfig.json
└── dist/
    └── cli.js             # 构建产物 (614KB)
```

## 🏗️ 与 Java 分析器对标

| 能力 | Java 分析器 | 前端分析器 |
|------|------------|-----------|
| AST 解析 | JavaParser | TypeScript Compiler API + Babel |
| 质量规则 | 150+ 条 | 88 条（12 个类别） |
| 数据流/污染 | TaintEngine | XSS 数据流追踪 |
| 跨文件关联 | RelationEngine | 组件 ↔ 样式 ↔ 测试 ↔ API |
| 架构分层 | ArchitectureAnalyzer | Pages → Components → Hooks → Utils |
| 代码指标 | CodeMetricsCalculator | 组件大小/Props/Hook 分布 |
| 质量门禁 | QualityGate | 同上（可配置阈值） |
| 技术债务 | TechnicalDebtEstimator | 同上（按严重度/类别估算） |
| 增量缓存 | SHA-256 | 同上机制 |
| 报告 | SARIF/HTML | JSON/HTML/Markdown |
| 实时进度 | WebSocket | WebSocket |

## 💻 编程方式使用

```typescript
import { RuleEngine, getAllDefaultRules } from 'frontend-quality-analyzer';

// 方式一：使用默认规则
const engine = new RuleEngine();
const result = engine.run([
  { path: 'src/App.tsx', content: '...' },
  { path: 'src/components/Button.tsx', content: '...' },
]);

// 方式二：自定义规则
import { TypeScriptRules, ReactRules } from 'frontend-quality-analyzer';
const engine2 = new RuleEngine();
engine2.registerRules([...TypeScriptRules.all(), ...ReactRules.all()]);

// 方式三：单文件分析
const issues = engine.runSingle(sourceCode, 'src/App.tsx', {
  framework: 'react',
});
```

## 📋 规则配置

```json
{
  "enabled_rules": [],
  "disabled_rules": ["FE-TS-002", "FE-PERF-010"],
  "thresholds": {
    "max_critical_issues": 0,
    "max_major_issues": 10,
    "max_total_issues": 50,
    "min_typescript_coverage": 80,
    "max_any_usage": 5,
    "max_bundle_size_kb": 500,
    "max_component_size_lines": 300,
    "min_wcag_score": 80,
    "max_circular_dependencies": 0,
    "max_function_length": 50
  }
}
```

使用：
```bash
node dist/cli.js --sourceRoot ./src --config ./rules-config.json
```

## 🎯 使用场景

1. **代码审查** — CI/CD 中自动检测质量回归
2. **项目接手** — 快速了解前端项目的技术债务
3. **重构辅助** — 识别性能瓶颈、内存泄漏、架构问题
4. **团队规范** — 统一 TypeScript/React/Vue 最佳实践
5. **无障碍合规** — 自动检测 WCAG 2.1 AA 违规
6. **安全审计** — XSS、CSRF、硬编码密钥检测

## 📦 技术栈

- **TypeScript Compiler API** — 真实 AST 解析
- **Babel Parser** — JSX/Vue SFC 解析
- **Commander** — CLI 参数解析
- **Chalk** — 终端彩色输出
- **esbuild** — 毫秒级构建
- **WebSocket** — 实时进度推送
