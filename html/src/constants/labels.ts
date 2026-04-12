/**
 * UI Labels & Text
 *
 * Centralized UI text for all user-facing strings.
 * Eliminates hardcoded Chinese text scattered in template strings.
 *
 * Usage:
 *   import { LABEL } from '../constants';
 *   el('div', null, [this.text(LABEL.QUALITY.TOTAL_ISSUES)])
 */
export const LABEL = {
  // Quality Dashboard
  QUALITY: {
    TOTAL_ISSUES: '总问题数',
    CRITICAL: '严重',
    MAJOR: '重要',
    MINOR: '次要',
    INFO_LABEL: '信息',
    ALL: '全部',
    NO_ISSUES_IN_FILTER: '当前筛选条件下无问题',
    NO_QUALITY_ISSUES: '✅ 无质量问题',
    SOURCE_NOT_FOUND: (name: string) => `// Source not found for ${name}`,
    NO_SOURCE_CODE: (name: string) => `// No source code for ${name}`,
  },

  // Code Explorer
  EXPLORER: {
    PACKAGE_VIEW: '📦 包视图',
    LIST_VIEW: '📄 列表视图',
    SEARCH_PLACEHOLDER: '🔍 搜索...',
    DIAGRAM_TAB: '📐 类图',
    SOURCE_TAB: '💻 源码',
    CALLS_TAB: '🔗 调用',
    DETAIL_HEADER: '📋 详情',
    WELCOME_SUB: (classCount: number, moduleInfo: string) =>
      `${classCount} 个类 · ${moduleInfo}`,
    SINGLE_MODULE: '单模块',
    MULTI_MODULE: (count: number) => `${count} 个模块`,
    PROJECT_STRUCTURE: (typeName: string) => `📊 项目结构 (${typeName})`,
    LAYER_CLASS_COUNT: (count: number) => `${count} 个类`,
    NO_MATCH: '未找到匹配项',
    SELECT_METHOD: '选择方法查看源码',
    SELECT_CALL: '选择方法查看调用关系',
    SELECT_DETAIL: '选择类或方法\n查看详情',
    COPY_CODE: '📋 复制',
    OVERVIEW: '📊 概览',
    METHODS: '方法',
    FIELDS: '字段',
    ISSUES: (count: number) => `⚠️ 问题 (${count})`,
    CALLS: (count: number) => `🔗 调用 (${count})`,
    QUALITY_HOTSPOTS: (count: number) => `🔴 质量热点 (${count})`,
    API_ENDPOINTS: (count: number) => `🌐 API 端点 (${count})`,
    HIGH_COMPLEXITY: (count: number) => `🔥 高复杂度方法 (${count})`,
    QUICK_ACCESS: '⚡ 快捷入口',
    NO_SOURCE_FOR: (name: string) => `// No source for ${name}`,
    JAVA_LABEL: 'Java',
  },

  // Architecture
  ARCHITECTURE: {
    LAYER_VIEW: '🏛️ 架构分层',
    VIOLATIONS: (count: number) => `⚠️ 架构违规 (${count})`,
    SOURCE_LAYER: '源层',
    TARGET_LAYER: '目标层',
    VIOLATION_TYPE: '违规类型',
    CLASS: '类',
    NO_VIOLATIONS: '✅ 无架构违规',
    NO_VIOLATIONS_MSG: '恭喜！未检测到架构分层违规。',
    LAYER_GRAPH_TITLE: '🔀 层间依赖关系',
    EMPTY_TITLE: '暂无架构分层数据',
    EMPTY_DESC: '当前数据文件未包含架构分层分析结果。',
    EMPTY_SUGGEST: '使用最新版本的 Java 分析工具重新分析项目。',
    CAN_DETECT_1: '自动识别 Controller / Service / Repository / Entity 层',
    CAN_DETECT_2: '显示层间依赖关系矩阵',
    CAN_DETECT_3: '检测架构违规（如 Controller 直接调用 Repository）',
    LAYERS: '层级',
    DEPENDENCIES: '依赖',
  },

  // Metrics
  METRICS: {
    TOTAL_CLASSES: '总类数',
    TOTAL_METHODS: '总方法数',
    TOTAL_LOC: '代码行数',
    COMMENT_RATIO: '注释率',
    AVG_COMPLEXITY: '平均复杂度',
    COHESION_INDEX: '内聚指数',
    COMPLEXITY_DISTRIBUTION: '📈 复杂度分布',
    CLASS_TYPE_DISTRIBUTION: '📊 类类型分布',
    AVG: '平均',
    MAX: '最大',
    INHERITANCE_DEPTH: '继承深度',
    METHOD_LENGTH: '方法长度',
    CLASS_TYPE_LABEL: '类类型',
  },

  // Quality Gate
  QUALITY_GATE: {
    PASSED: '✅ 质量门禁通过',
    FAILED: '❌ 质量门禁未通过',
    PASSED_LABEL: '通过',
    FAILED_LABEL: '未通过',
    CONDITIONS: '条件',
    METRIC: '指标',
    THRESHOLD: '阈值',
    ACTUAL: '实际值',
    STATUS: '状态',
  },

  // Project Assets
  ASSETS: {
    TITLE: '📦 项目资产',
    TOTAL: (count: number) => `共 ${count} 个资产`,
    NO_ASSETS: '暂无项目资产数据',
    NO_ASSETS_DESC: '当前项目未包含非 Java 文件资产信息。请使用 Java 分析工具的完整分析模式重新扫描，以获取 XML、YAML、SQL、Dockerfile 等配置文件详情。',
    TOTAL_LABEL: '总资产数',
    TYPE_COUNT_LABEL: '资产类型数',
    COUNT_ITEMS: (count: number) => `${count} 项`,
    GROUP_LABEL: 'Group:',
    VERSION_LABEL: 'Version:',
    DEPS_LABEL: '依赖数:',
    MIDDLEWARE_LABEL: '中间件:',
    NAMESPACE_LABEL: 'Namespace:',
    TABLES_LABEL: '表数:',
    JAR_LABEL: 'JAR:',
    TYPE_NAMES: {
      maven_pom: 'Maven POM',
      yaml_config: 'YAML 配置',
      properties_config: 'Properties 配置',
      sql_script: 'SQL 脚本',
      mybatis_mapper: 'MyBatis Mapper',
      dockerfile: 'Dockerfile',
      docker_compose: 'Docker Compose',
      shell_script: 'Shell 脚本',
      log_config: '日志配置',
      markdown_doc: 'Markdown 文档',
      modules: '模块',
      scan_summary: '扫描摘要',
      errors: '错误',
    },
  },

  // Component Explorer
  COMPONENT: {
    TITLE: (count: number) => `📦 项目组件 (${count})`,
    NOT_FOUND: '⚠️ 未找到组件',
    BACK: '← 返回',
    DESCRIPTION: '📝 描述',
    FIELDS: (count: number) => `📊 字段 (${count})`,
    METHODS: (count: number) => `⚙️ 方法 (${count})`,
    METHOD_COUNT: (count: number) => `⚙️ ${count} 方法`,
    MOD_PUBLIC: 'public',
    MOD_STATIC: 'static',
    PACKAGE_PREFIX: (name: string) => `📂 ${name}`,
  },

  // Cross-File Relations
  RELATIONS: {
    TITLE: '🔗 跨文件关系',
    TOTAL_RELATIONS: (count: number) => `共 ${count} 个关系`,
    NO_RELATIONS: '暂无跨文件关系',
    BY_TYPE: '按类型',
  },

  // Method Call
  METHOD_CALL: {
    TITLE: '📞 方法调用图',
    SELECT_CLASS: '选择类以查看方法调用关系',
    NO_METHODS: '该类没有方法',
    NO_CALLS: '该类方法之间没有内部调用关系',
    TIP_EXTERNAL: '提示: 方法可能只调用了外部类的方法',
    CALL_COUNT: (count: number) => `调用 ${count} 次`,
    CALL_TIMES: '调用次数:',
    MORE_CALLS: (count: number) => `... 还有 ${count} 次调用`,
    SHOWING_FIRST: (count: number) => `显示前 ${count} 个类，使用搜索框查找特定类`,
  },

  // Call Chain
  CALL_CHAIN: {
    TITLE: '⛓️ 调用链',
    SELECT_ENTRY: '选择入口方法以追踪调用链路',
    NO_CONTROLLER: '未检测到 Controller 类',
    TIP_CONTROLLER: '提示: 确保后端分析包含了 Spring MVC 或 REST API 控制器',
    DETECTED_CONTROLLERS: (count: number) => `检测到 ${count} 个 Controller 类`,
    SHOWING_FIRST: '显示前 20 个 Controller，使用搜索框查找特定类',
    NO_CHAIN: '未检测到调用链路',
    TIP_NO_CHAIN: '该方法可能没有调用其他内部方法',
    LAYER_LABEL: '层级:',
    LINK_TYPES: {
      VIOLATION: '⚠️ 架构违规',
      RISK: '🔥 高风险调用',
      NORMAL: '📞 正常调用',
    },
    LAYER_NAMES: {
      controller: '🌐 Controller 层',
      service: '⚙️ Service 层',
      repository: '💾 Repository 层',
      external: '🔌 外部调用',
    },
  },

  // API Endpoint
  API: {
    TITLE: '🌐 API 端点',
    NO_ENDPOINTS: '暂无 API 端点数据',
    NO_ENDPOINTS_DESC: '当前项目未检测到 Spring API 端点。',
    REASON_1: '项目未使用 Spring Boot 框架',
    REASON_2: '未使用 @RestController / @Controller 注解',
    REASON_3: '数据文件是早期版本，未包含 Spring 分析',
    SUGGEST: '确保项目是 Spring Boot 项目，使用最新版分析工具重新分析。',
    LABEL: 'API 端点',
    BEANS: 'Spring Beans',
    BEAN_DEPS: 'Bean 依赖',
    FILTER_ALL: (count: number) => `全部 (${count})`,
    HEADER: (count: number) => `📋 API 端点 (${count})`,
    ENDPOINT_COUNT: (count: number) => `${count} 个端点`,
    PARAMS: '参数:',
    NO_MATCH: '无匹配的端点',
  },

  // Force Graph Tooltip
  GRAPH: {
    JDK_LIB: 'JDK 库',
    THIRD_PARTY: '第三方库',
    KIND_INTERFACE: '接口',
    KIND_ABSTRACT: '抽象类',
    KIND_ENUM: '枚举',
    KIND_CLASS: '类',
    NO_DESC: '暂无描述',
    STAT_METHODS: (count: number) => `📊 ${count} 方法`,
    STAT_FIELDS: (count: number) => `📝 ${count} 字段`,
    STAT_DEPS: (count: number) => `🔗 ${count} 依赖`,
    SEV_CRITICAL: (count: number) => `🔴 ${count} 严重`,
    SEV_MAJOR: (count: number) => `🟠 ${count} 重要`,
    SEV_MINOR: (count: number) => `🔵 ${count} 提示`,
    SEV_OK: '✅ 无问题',
    EDGE_IMPORT: '📦 Import 依赖关系',
    EDGE_VIOLATION: '⚠️ 架构违规：越层调用',
    EDGE_REF: '引用该类的数量',
  },

  // Class Inspector
  INSPECTOR: {
    LOADING: '加载中...',
    NO_SOURCE: '暂无源码',
    TAB_INFO: '📋 信息',
    TAB_SOURCE: '📄 源码',
    TYPE_LABEL: '类型',
    DESC_LABEL: '描述',
    FIELDS_LABEL: '字段',
    UNKNOWN: 'Unknown',
  },

  // Common
  COMMON: {
    EMPTY: '暂无数据',
    LOADING: '加载中...',
    ERROR: '加载失败',
    UNKNOWN: 'Unknown',
    CLASS_TYPE: '类型',
    DESCRIPTION: '描述',
    LINE: '行',
    TOTAL: '总计',
    DETAIL: '详情',
  },

  // Empty states
  EMPTY: {
    QUALITY: {
      TITLE: '暂无质量分析数据',
      DESC: '💡 使用最新版 Java 分析工具重新分析项目。',
    },
    METRICS: {
      TITLE: '暂无代码指标数据',
      DESC: '当前数据文件未包含代码指标分析结果。<br><br>💡 使用最新版本的 Java 分析工具重新分析项目，即可获取 LOC、复杂度、耦合度、内聚度等详细指标。',
    },
    GENERIC: {
      TITLE: '暂无数据',
      DESC: '当前没有可用数据。',
    },
  },
} as const;
