/**
 * Java Source Analyzer - Unified Issue & Report Types
 * 
 * 与 Java 后端 cn.dolphinmind...unified 包严格一致
 * 扩展了Harness Engineering相关字段
 */

// ========== 枚举 ==========

export type IssueSource = 'static' | 'ai' | 'merged';

export type ConfidenceLevel = 'HIGH' | 'MEDIUM' | 'LOW' | 'FILTERED';

export type Severity = 'CRITICAL' | 'MAJOR' | 'MINOR' | 'INFO';

export type IssueCategory = 'BUG' | 'CODE_SMELL' | 'SECURITY' | 'DESIGN' | 'PERFORMANCE';

// Harness Engineering: 验证动作
export type ValidationAction = 'ACCEPT' | 'DOWNGRADE' | 'FLAG_FOR_REVIEW' | 'RETRY_WITH_CONTEXT';

// RAG管道降级策略
export type DegradationStrategy = 'RETURN_EMPTY' | 'USE_STATIC_ONLY' | 'REDUCE_CONTEXT';

// ========== 统一问题 ==========

export interface UnifiedIssue {
  // 基础字段
  id: string;
  source: IssueSource;
  sourceLabel: string;
  ruleKey: string;
  ruleName: string;
  severity: Severity;
  category: IssueCategory;
  filePath: string;
  className: string;
  methodName?: string;
  line: number;
  message: string;

  // AI 字段 (可选)
  confidence?: number;
  confidenceLevel?: ConfidenceLevel;
  aiSuggestion?: string;
  aiFixedCode?: string;
  aiReasoning?: string;
  aiModel?: string;
  autoFiltered?: boolean;

  // 静态分析字段 (可选)
  cyclomaticComplexity?: number;
  loc?: number;
  cognitiveComplexity?: number;
  relatedAssets?: string[];
  staticEngineVersion?: string;

  // 关联 ID
  staticIssueId?: string;
  aiIssueId?: string;

  // ========== Harness Engineering 扩展字段 ==========
  
  // 验证反馈回路
  validationAction?: ValidationAction;
  retryCount?: number;
  maxRetries?: number;
  
  // 降级策略
  degradationStrategy?: DegradationStrategy;
  degradationReason?: string;
  
  // 管道性能指标
  pipelineMetrics?: {
    embeddingTimeMs: number;
    vectorSearchTimeMs: number;
    llmInferenceTimeMs: number;
    totalPipelineTimeMs: number;
    tokensUsed?: number;
  };
  
  // 验证统计
  validationStats?: {
    originalConfidence: number;
    adjustedConfidence: number;
    validationTimestamp: number;
    validatorVersion: string;
  };

  // 元数据
  createdAt: string;
  updatedAt: string;
}

// ========== 引擎信息 ==========

export interface EngineInfo {
  name: string;
  version: string;
  modelUsed?: string;
}

// ========== 统计摘要 ==========

export interface ReviewSummary {
  totalIssues: number;
  staticOnly: number;
  aiOnly: number;
  merged: number;
  autoFiltered: number;
  activeIssues: number;
  critical: number;
  major: number;
  minor: number;
  info: number;
  aiAvgConfidence: number;
  aiHighConfidenceRate: number;
  byCategory: Record<string, number>;
  
  // Harness Engineering统计
  validationStats?: {
    accepted: number;
    downgraded: number;
    flaggedForReview: number;
    retried: number;
    maxRetriesExceeded: number;
  };
  
  degradationStats?: {
    useStaticOnly: number;
    reduceContext: number;
    returnEmpty: number;
  };
}

// ========== 统一报告 ==========

export interface UnifiedReport {
  projectName: string;
  projectVersion: string;
  commitSha: string;
  branch: string;
  timestamp: number;
  analysisDurationMs: number;
  staticEngine?: EngineInfo;
  aiEngine?: EngineInfo;
  summary?: ReviewSummary;
  issues: UnifiedIssue[];
}

// ========== 遗留格式兼容 ==========

export interface AnalysisResult {
  assets?: any[];
  nodes?: any[];
  edges?: any[];
  quality_issues?: any[];
  unified_issues?: UnifiedIssue[];
  summary?: ReviewSummary;
  aiEngine?: EngineInfo;
}
