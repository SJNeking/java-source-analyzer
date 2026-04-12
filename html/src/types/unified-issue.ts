/**
 * Unified Issue & Report Types
 * 
 * 与 Java 后端 cn.dolphinmind...unified 包严格一致
 * 前后端共用同一份 Schema
 */

// ========== 枚举 ==========

export type IssueSource = 'static' | 'ai' | 'merged';

export type ConfidenceLevel = 'HIGH' | 'MEDIUM' | 'LOW' | 'FILTERED';

export type Severity = 'CRITICAL' | 'MAJOR' | 'MINOR' | 'INFO';

export type IssueCategory = 'BUG' | 'CODE_SMELL' | 'SECURITY' | 'DESIGN' | 'PERFORMANCE';

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
