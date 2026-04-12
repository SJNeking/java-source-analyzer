/**
 * Frontend Quality Analysis Types
 */

export interface FrontendQualityIssue {
  file: string;
  line: number;
  column?: number;
  severity: 'CRITICAL' | 'MAJOR' | 'MINOR' | 'INFO';
  category: string;
  rule_key: string;
  message: string;
  remediation?: string;
  evidence?: string;
}

export interface FrontendFileAnalysis {
  file_path: string;
  issues: FrontendQualityIssue[];
}

export interface QualityGate {
  passed: boolean;
  reasons?: string[];
}

export interface TechnicalDebt {
  total_remediation_hours: number;
  debt_ratio_percentage?: number;
}

export interface XSSFlow {
  source: { type: string };
  path_length: number;
  sink: { type: string };
}

export interface ComponentGraph {
  total_components: number;
  total_dependencies: number;
  circular_dependencies: number;
  coupling_score: number;
}

export interface CrossFileRelations {
  total_relations: number;
  missing_test_files?: number;
  orphan_files?: number;
}

export interface AdvancedAnalysis {
  xss_taint_flows?: XSSFlow[];
  component_graph?: ComponentGraph;
  cross_file_relations?: CrossFileRelations;
}

export interface FrontendAnalysisResult {
  project_name?: string;
  framework_detected?: string;
  total_files: number;
  total_lines: number;
  files: FrontendFileAnalysis[];
  quality_gate?: QualityGate;
  technical_debt?: TechnicalDebt;
  advanced_analysis?: AdvancedAnalysis;
}
