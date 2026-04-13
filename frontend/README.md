# Java Source Analyzer - React Frontend

企业级代码审查前端 - React + TypeScript + Vite 重构版本

## 🚀 技术栈

### 核心框架
- **React 18.2** - UI组件库
- **TypeScript 5.3** - 类型安全
- **Vite 5.1** - 构建工具
- **React Router 6.22** - 路由管理

### 状态管理
- **Zustand 4.5** - 轻量级状态管理（替代legacy ApplicationState）

### 数据可视化
- **ECharts 5.5** - 图表库（力导向图、桑基图等）
- **Mermaid 10.9** - UML图表渲染

### UI工具
- **Lucide React** - 图标库
- **clsx** - 条件类名处理
- **DOMPurify** - XSS防护

### HTTP客户端
- **Axios 1.6** - API请求（带重试机制）

---

## 📁 项目结构

```
frontend/
├── src/
│   ├── components/          # 通用组件
│   │   ├── Layout.tsx       # 主布局（Sidebar + TopBar + ViewContainer）
│   │   ├── Sidebar.tsx      # 侧边栏导航
│   │   ├── TopBar.tsx       # 顶部标签栏
│   │   ├── ViewContainer.tsx # 视图容器（路由驱动）
│   │   ├── LoadingOverlay.tsx # 加载遮罩
│   │   └── ErrorBoundary.tsx  # 错误边界
│   ├── views/               # 页面视图（待迁移）
│   ├── store/               # Zustand状态管理
│   │   ├── app-store.ts     # 应用全局状态
│   │   └── index.ts         # Store导出
│   ├── services/            # API服务层
│   │   ├── api.service.ts   # Axios封装（重试+错误处理）
│   │   ├── data-fetcher.service.ts # 数据获取+缓存
│   │   └── index.ts
│   ├── types/               # TypeScript类型定义
│   │   ├── unified-issue.ts # 统一问题类型（含Harness Engineering扩展）
│   │   ├── graph.ts         # 图谱数据类型
│   │   ├── project.ts       # 项目和资产类型
│   │   └── index.ts
│   ├── styles/              # 全局样式
│   │   └── index.css        # 深色主题样式
│   ├── App.tsx              # 根组件
│   └── main.tsx             # 应用入口
├── package.json
├── tsconfig.json
├── vite.config.ts
└── index.html
```

---

## ✅ 已完成功能

### 1. 项目初始化
- ✅ Vite + TypeScript + React脚手架
- ✅ ESLint配置（严格模式）
- ✅ 路径别名配置（@components, @views等）

### 2. 类型系统
- ✅ `UnifiedIssue`类型（包含Harness Engineering扩展字段）
  - `validationAction`: ACCEPT/DOWNGRADE/FLAG_FOR_REVIEW/RETRY_WITH_CONTEXT
  - `degradationStrategy`: RETURN_EMPTY/USE_STATIC_ONLY/REDUCE_CONTEXT
  - `pipelineMetrics`: embedding/vector-search/llm-inference性能指标
  - `validationStats`: 验证统计信息
- ✅ `GraphData`类型（力导向图节点和边）
- ✅ `ProjectInfo`和`Asset`类型

### 3. 状态管理（Zustand）
- ✅ `useAppStore`全局状态
  - 项目管理（currentProject, projects）
  - 图谱数据（graphData）
  - 统一报告（unifiedReport）
  - 视图切换（currentView）
  - 加载状态（isLoading, error）
- ✅ 选择器函数
  - `selectActiveIssues`: 过滤活跃问题
  - `selectHighConfidenceIssues`: 高置信度问题
  - `selectValidationStats`: 验证统计

### 4. API服务层
- ✅ `ApiService`类
  - Axios拦截器（统一错误处理）
  - 指数退避重试机制（最多3次）
  - 超时检测（30秒）
- ✅ `DataFetcherService`类
  - 5分钟TTL缓存
  - 统一报告优先加载
  - 遗留格式降级支持

### 5. 路由系统
- ✅ React Router配置
- ✅ 12个视图路由注册
  - explorer, graph, relations, quality
  - frontend-quality, metrics, assets
  - ai-review, **pipeline** (新增), **performance** (新增)

### 6. 布局框架
- ✅ `Layout`组件（主容器）
- ✅ `Sidebar`组件（项目选择器+搜索框）
- ✅ `TopBar`组件（标签页导航）
- ✅ `ViewContainer`组件（路由驱动的视图渲染）
- ✅ `LoadingOverlay`组件（加载状态）
- ✅ `ErrorBoundary`组件（错误捕获）

### 7. 样式系统
- ✅ 深色主题CSS变量
- ✅ 响应式布局（Flexbox）
- ✅ 复用旧前端样式（保持一致性）

---

## ⏳ 待迁移视图（14个）

### P0 - 核心视图
1. **ForceGraphView** - 力导向依赖图（需集成echarts-for-react）
2. **QualityDashboardView** - 质量分析仪表板
3. **CodeExplorerView** - 代码浏览器（树形结构）

### P1 - 重要视图
4. **AiReviewView** - AI审查（需增强验证反馈可视化）
5. **FrontendQualityView** - 前端质量分析
6. **CrossFileRelationsView** - 跨文件关系图
7. **MetricsDashboardView** - 代码指标仪表板

### P2 - 辅助视图
8. **ProjectAssetsView** - 项目资产列表
9. **ArchitectureLayerView** - 架构分层视图
10. **MethodCallView** - 方法调用桑基图
11. **CallChainView** - 调用链路泳道图
12. **ClassInspectorPanel** - 类检查器面板
13. **ComponentExplorerView** - 组件浏览器
14. **ApiEndpointView** - API端点视图

### ✨ 新增视图
15. **RagPipelineView** - RAG管道流程可视化（Harness Engineering）
   - CodeSlicer切片展示
   - 向量检索过程
   - LLM推理步骤
   - ResultMerger合并逻辑
16. **PerformanceMetricsView** - 性能监控面板
   - Embedding耗时趋势
   - Vector Search延迟
   - LLM Inference时间
   - 降级策略触发次数

---

## 🛠️ 开发指南

### 安装依赖
```bash
cd frontend
npm install
```

### 启动开发服务器
```bash
npm run dev
# 访问 http://localhost:3000
```

### 生产构建
```bash
npm run build
# 输出到 dist/ 目录
```

### 代码检查
```bash
npm run lint      # 检查
npm run lint:fix  # 自动修复
```

---

## 🔧 配置说明

### Vite代理配置（vite.config.ts）
```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // SpringBoot后端
      changeOrigin: true,
    },
    '/data': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

### 路径别名
```typescript
import { useAppStore } from '@store/app-store';
import type { UnifiedIssue } from '@types/unified-issue';
import Sidebar from '@components/Sidebar';
```

---

## 📊 迁移进度

| 阶段 | 任务 | 状态 |
|------|------|------|
| 1 | 项目初始化（Vite + TS + React） | ✅ 完成 |
| 2 | 配置Vite和TypeScript | ✅ 完成 |
| 3 | 安装核心依赖 | ✅ 完成 |
| 4 | 创建类型系统 | ✅ 完成 |
| 5 | 实现Zustand状态管理 | ✅ 完成 |
| 6 | 创建API服务层 | ✅ 完成 |
| 7 | 搭建路由和布局框架 | ✅ 完成 |
| 8 | 迁移ForceGraphView | ⏳ 待开始 |
| 9 | 迁移质量分析视图 | ⏳ 待开始 |
| 10 | 迁移代码浏览视图 | ⏳ 待开始 |
| 11 | 迁移AI审查视图 | ⏳ 待开始 |
| 12 | 新增RagPipelineView | ⏳ 待开始 |
| 13 | 新增PerformanceMetricsView | ⏳ 待开始 |
| 14 | 迁移剩余视图 | ⏳ 待开始 |
| 15 | 集成Mermaid.js | ⏳ 待开始 |
| 16 | Nginx部署配置 | ⏳ 待开始 |
| 17 | 端到端测试 | ⏳ 待开始 |

**总体进度**: 7/17 (41%)

---

## 🎯 下一步行动

### 立即执行
1. **安装依赖**: `npm install`
2. **启动开发服务器**: `npm run dev`
3. **验证基础功能**: 确认布局、路由、状态管理正常工作

### 短期计划（1-2天）
1. 迁移`ForceGraphView`（最复杂的视图，优先级最高）
2. 迁移`QualityDashboardView`和`FrontendQualityView`
3. 集成ECharts图表库

### 中期计划（3-5天）
1. 迁移`AiReviewView`并增强Harness Engineering可视化
2. 创建`RagPipelineView`（全新视图）
3. 创建`PerformanceMetricsView`（全新视图）

### 长期计划（1-2周）
1. 完成所有14个视图的迁移
2. 集成Mermaid.js支持UML图表
3. 配置Nginx生产部署
4. 端到端测试和优化

---

## 📝 技术亮点

### 相比旧前端的优势
1. **现代技术栈**: React生态 > 原生TypeScript组件系统
2. **更好的状态管理**: Zustand > 自定义ApplicationState
3. **路由驱动**: React Router > 手动视图切换
4. **类型安全**: 完整的TypeScript类型推导
5. **构建速度**: Vite HMR > esbuild全量重建
6. **代码分割**: 自动按需加载（manualChunks配置）
7. **Harness Engineering支持**: 新增验证反馈和性能监控维度

### 保留的优点
- ✅ 完全复用旧前端深色主题
- ✅ 保持相同的布局和交互逻辑
- ✅ 兼容后端现有API接口
- ✅ 支持遗留数据格式降级

---

## 🐛 已知问题

1. **视图内容占位**: 所有视图当前显示"待迁移"占位符
2. **项目树未实现**: Sidebar中的树形结构待迁移
3. **Mermaid未集成**: 需要在具体视图中按需加载
4. **无WebSocket**: 暂不支持实时数据更新

---

## 📞 联系方式

项目负责人: Mingxi Lv
技术栈: Java Source Analyzer + RAG + Harness Engineering
