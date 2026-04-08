# 里程碑 M8-v2: 5 大核心功能（JavaParser AST 重写版）

**日期**: 2026-04-09
**Git 提交**: `4fad391`

---

## 问题

之前的 M8 版本用字符串/正则解析，结果全是错的：
- 包结构输出 `default (1 classes)` — 应该是 `com.zaxxer.hikari (59 classes)`
- 调用链追踪到 `ClassPool#importPackage` — 这是外部库，不是项目内部的
- 层推断 `pool [ENTITY]` — 应该是 `pool [CORE]`

## 修复

全部 5 个核心功能改用 JavaParser AST 解析：

### 1. 入口点发现 ✅
- `StaticJavaParser.parse()` + `cu.findAll(ClassOrInterfaceDeclaration.class)`
- 准确检测 main()、@RequestMapping、@EventListener、@Scheduled、@PostConstruct、消息监听器
- 提取 HTTP 方法、URL 路径、cron 表达式、Topic 名称

### 2. 调用链路追踪 ✅
- 第一遍：注册所有内部项目类
- 第二遍：构建调用图，**只追踪内部类之间的调用**
- 外部库（java.*、javax.*、org.*、com.*）全部排除
- 检测循环调用

### 3. 包结构地图 ✅
- `cu.getPackageDeclaration()` 获取准确包名
- 全限定名传递给层推断
- 每个包的类数量正确统计
- 修复 `.po` 误匹配 `pool` 的 bug
- 修复 `getClasses().addAll()` 在 unmodifiable list 上崩溃的 bug

### 4. 类型定义导航 ✅
- JavaParser AST 索引所有类/接口/枚举
- 简单名和全限定名都映射到文件
- 每个类型列出方法列表

### 5. 数据流追踪 ✅
- 索引所有方法定义
- 追踪参数通过内部方法调用传递
- 追踪变量返回和字段赋值

## 验证结果

### HikariCP 5.0.1

```
入口点: 0 个（正确，HikariCP 没有 main/REST/注解入口）
包结构: com.zaxxer.hikari → metrics[UTIL](17) / util[UTIL](15) / pool[CORE](18)
类型索引: 59 个
调用图: 仅内部调用（无外部库）
```

### 本项目 (java-source-analyzer)

```
入口点: 1 个 → SourceUniversePro.main()
包结构: cn.dolphinmind.glossary.java.analyze → core[CORE](12) / quality[OTHER](112) / config[CONFIG](3) ...
类型索引: 196 个
调用链: 86 条（从 main 开始，全部是内部调用）
```

### 对比之前

| 指标 | 之前（正则版） | 之后（AST 版） |
|------|---------------|---------------|
| 包结构 | `default (1 classes)` ❌ | `com.zaxxer.hikari (59 classes)` ✅ |
| 调用链 | 追踪外部库（ClassPool） ❌ | 只追踪内部调用（86 条） ✅ |
| 层推断 | pool [ENTITY] ❌ | pool [CORE] ✅ |
| 类计数 | 全 0 ❌ | 正确统计 ✅ |
| 入口点 | 1 个 main（正确） | 1 个 main（正确） ✅ |

## 下一步

核心功能现在准确了。后续可以：
1. 在 Spring Boot 项目上测试（有更多入口点：REST/事件/定时任务）
2. 增强调用链的变量类型解析（目前用简单的首字母大写猜测）
3. 增强数据流追踪（支持更复杂的参数传递路径）
