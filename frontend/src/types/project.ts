/**
 * Project and Asset Types
 */

export interface ProjectInfo {
  name: string;
  file: string;
  version?: string;
  lastAnalyzed?: string;
}

export interface Asset {
  address: string;
  kind: string;
  name: string;
  path: string;
  lines?: number;
  complexity?: number;
  qualityIssues?: number;
  [key: string]: any;
}

export interface QualityIssue {
  rule_key: string;
  severity: 'CRITICAL' | 'MAJOR' | 'MINOR' | 'INFO';
  file: string;
  line: number;
  message: string;
  method?: string;
  className?: string;
}

export interface CodeMetrics {
  totalLines: number;
  codeLines: number;
  commentLines: number;
  blankLines: number;
  classes: number;
  methods: number;
  avgComplexity: number;
  maxComplexity: number;
}
