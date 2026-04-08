# 里程碑 M8: 5 大核心功能 — 解构任何 Java 项目

**日期**: 2026-04-09
**Git 提交**: `85c44b3`

---

## 一、5 大核心功能

### 1. 入口点发现 (EntryPointDiscovery)

**解决的问题**: "这个项目从哪里开始？"

| 入口类型 | 检测方式 | 示例 |
|---------|---------|------|
| main() | `public static void main(String[])` | `JavassistProxyFactory.main()` |
| REST 接口 | `@RequestMapping` + HTTP 方法注解 | `GET /api/users → UserController#list()` |
| 事件监听 | `@EventListener` | `Event → OrderEventListener#onOrderCreated()` |
| 定时任务 | `@Scheduled(cron=...)` | `Scheduled → ReportJob#generate()` |
| 初始化 | `@PostConstruct` | `Bean init → DataSourceConfig#init()` |
| 消息消费 | `@KafkaListener` / `@JmsListener` | `Message[order-topic] → OrderConsumer#consume()` |

### 2. 调用链路追踪 (CallChainTracer)

**解决的问题**: "从入口开始，完整调用了哪些方法？"

```
入口: JavassistProxyFactory#main (73 条链路)
  JavassistProxyFactory#main → ClassPool#importPackage
  JavassistProxyFactory#main → ClassPool#appendClassPath
  JavassistProxyFactory#main → JavassistProxyFactory#generateProxyClass → ClassPool#getCtClass
  JavassistProxyFactory#main → JavassistProxyFactory#generateProxyClass → SuperCt#getMethods
  ...
```

- 从任何入口点开始追踪
- 自动检测循环调用（标记 recursive）
- 可配置最大深度（默认 5 层）
- 输出完整调用图（节点数 + 边数）

### 3. 包结构地图 (PackageStructureMapper)

**解决的问题**: "项目有哪些包？每层做什么？"

```
└── com.zaxxer.hikari [OTHER] (65 classes)
    ├── pool [SERVICE] (20 classes)
    ├── pool.proxy [REPOSITORY] (5 classes)
    ├── metrics [UTIL] (8 classes)
    └── util [UTIL] (12 classes)
```

- 自动生成包树结构
- 层级推断（CONTROLLER/SERVICE/REPOSITORY/ENTITY/DTO/CONFIG/UTIL 等 12 种）
- 每个包的类数量
- 包之间的依赖关系

### 4. 类型定义导航 (TypeDefinitionNavigator)

**解决的问题**: "看到 userService.save()，UserService 定义在哪里？"

- 索引项目中所有类
- 从任何类型名（全限定或简单名）定位到定义文件
- 列出每个类的所有方法
- 支持泛型去除（`List<User>` → `List` → `java.util.List`）

### 5. 数据流追踪 (DataFlowTracer)

**解决的问题**: "这个参数传到了哪里？"

- 追踪方法参数在方法体内的使用
- 追踪参数通过方法调用传递的路径
- 追踪变量返回值
- 追踪变量赋值到字段

---

## 二、整合：CoreAnalysisEngine

将 5 个功能整合为一个 `analyze(Path)` 调用，输出统一的 JSON 结构：

```json
{
  "core_analysis": {
    "entry_points": {
      "total": 15,
      "by_type": {
        "REST_CONTROLLER": [ ... ],
        "SCHEDULED_TASK": [ ... ]
      }
    },
    "package_structure": {
      "layer_summary": { "SERVICE": 120, "REPOSITORY": 45 },
      "tree": { ... }
    },
    "call_graph": {
      "node_count": 5000,
      "edge_count": 15000,
      "chains_from_entry": { ... }
    },
    "type_definitions": {
      "total_types": 2695,
      "types": { ... }
    },
    "data_flows": {
      "total_flows": 120,
      "flows": [ ... ]
    }
  }
}
```

---

## 三、验证结果

### HikariCP 5.0.1

| 指标 | 值 |
|------|-----|
| 入口点 | 1 个（main 方法） |
| 调用链 | 73 条 |
| 类型索引 | 84 个 |
| 调用图节点 | 已构建 |
| 数据流 | 已追踪 |

### 输出文件

除了原有的 full JSON / summary JSON / HTML / SARIF，现在 full JSON 中包含 `core_analysis` 字段，包含以上所有核心分析结果。

---

## 四、与已有功能的关系

| 功能 | 类型 | 说明 |
|------|------|------|
| 5 大核心 | **新增核心** | 解构项目的核心能力 |
| 质量规则 105+ | **已有** | 代码质量/安全分析 |
| 并行扫描/增量 | **已有** | 性能优化 |
| HTML/SARIF 报告 | **已有** | 输出格式 |
| 规则配置/基线 | **已有** | 治理能力 |

核心功能不影响已有功能，只是增加了新的分析维度。
