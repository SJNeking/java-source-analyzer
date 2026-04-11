/**
 * Frontend Code Quality Analyzer - Enterprise Edition
 * 
 * Type definitions mirroring the backend Java quality analysis system.
 * Supports multi-framework, multi-domain frontend code quality assessment.
 */

// ==================== Core Types ====================

/**
 * Severity level for quality issues (mirrors backend Severity enum)
 */
export type Severity = 'CRITICAL' | 'MAJOR' | 'MINOR' | 'INFO';

/**
 * Severity constants for runtime usage
 */
export const Severity = {
  CRITICAL: 'CRITICAL' as Severity,
  MAJOR: 'MAJOR' as Severity,
  MINOR: 'MINOR' as Severity,
  INFO: 'INFO' as Severity,
} as const;

/**
 * Issue category classification
 */
export type IssueCategory =
  | 'TYPESCRIPT'        // TypeScript best practices
  | 'REACT'             // React-specific patterns
  | 'VUE'               // Vue-specific patterns
  | 'SECURITY'          // Security vulnerabilities (XSS, CSRF, etc.)
  | 'PERFORMANCE'       // Performance optimization
  | 'MEMORY'            // Memory leak detection
  | 'ACCESSIBILITY'     // WCAG compliance
  | 'ARCHITECTURE'      // Code architecture
  | 'STYLING'           // CSS/styling consistency
  | 'TESTING'           // Test quality
  | 'BUILD'             // Build configuration
  | 'I18N';             // Internationalization

/**
 * IssueCategory constants for runtime usage
 */
export const IssueCategory = {
  TYPESCRIPT: 'TYPESCRIPT' as IssueCategory,
  REACT: 'REACT' as IssueCategory,
  VUE: 'VUE' as IssueCategory,
  SECURITY: 'SECURITY' as IssueCategory,
  PERFORMANCE: 'PERFORMANCE' as IssueCategory,
  MEMORY: 'MEMORY' as IssueCategory,
  ACCESSIBILITY: 'ACCESSIBILITY' as IssueCategory,
  ARCHITECTURE: 'ARCHITECTURE' as IssueCategory,
  STYLING: 'STYLING' as IssueCategory,
  TESTING: 'TESTING' as IssueCategory,
  BUILD: 'BUILD' as IssueCategory,
  I18N: 'I18N' as IssueCategory,
} as const;

/**
 * Represents a quality issue found in frontend code
 * (Mirrors backend QualityIssue structure)
 */
export interface QualityIssue {
  rule_key: string;           // e.g., "FE-TS-001", "FE-REACT-015"
  rule_name: string;          // Human-readable name
  severity: Severity;
  category: IssueCategory;
  file_path: string;
  line: number;
  column?: number;
  message: string;
  evidence?: string;          // Code snippet showing the issue
  remediation?: string;       // How to fix it
  confidence?: number;        // Detection confidence (0-1)
  
  // Optional: AST node information for precise location
  ast_node_type?: string;
  function_name?: string;
  component_name?: string;
}

/**
 * Analysis result for a single file
 */
export interface FileAnalysis {
  file_path: string;
  file_size: number;          // bytes
  line_count: number;
  language: 'typescript' | 'javascript' | 'jsx' | 'tsx' | 'vue' | 'css' | 'scss' | 'html';
  framework?: 'react' | 'vue' | 'angular' | 'svelte' | 'nextjs' | 'nuxt';
  issues: QualityIssue[];
  
  // File-level metrics
  metrics?: {
    cyclomatic_complexity?: number;
    function_count?: number;
    component_count?: number;
    import_count?: number;
    any_type_usage?: number;
  };
}

/**
 * Complete project analysis result
 * (Mirrors backend AnalysisResult structure)
 */
export interface ProjectAnalysis {
  project_name: string;
  scan_date: string;
  framework_detected?: string;
  
  // Summary statistics
  total_files: number;
  total_lines: number;
  total_issues: number;
  
  // Breakdown by severity
  by_severity: {
    CRITICAL: number;
    MAJOR: number;
    MINOR: number;
    INFO: number;
  };
  
  // Breakdown by category
  by_category: {
    [key in IssueCategory]?: number;
  };
  
  // Detailed file analyses
  files: FileAnalysis[];
  
  // Aggregated metrics
  metrics: FrontendMetrics;
  
  // Quality gate result (for CI/CD integration)
  quality_gate?: QualityGateResult;
  
  // Technical debt estimation
  technical_debt?: TechnicalDebtEstimate;
}

/**
 * Frontend-specific metrics dashboard
 */
export interface FrontendMetrics {
  // TypeScript Quality Metrics
  typescript: {
    coverage_percentage: number;      // % of code with type annotations
    any_usage_count: number;
    implicit_any_count: number;
    explicit_any_count: number;
    type_safety_score: number;        // 0-100
  };
  
  // Performance Metrics
  performance: {
    estimated_bundle_size_kb?: number;
    lazy_loading_coverage?: number;   // % of routes/components lazy loaded
    memoization_ratio?: number;       // useMemo/useCallback usage
    image_optimization_score?: number;
    core_web_vitals_estimate?: {
      lcp_estimate_ms?: number;
      fid_estimate_ms?: number;
      cls_estimate?: number;
    };
  };
  
  // Memory Safety Metrics
  memory: {
    potential_leak_sites: number;
    event_listener_cleanup_ratio?: number;
    timer_cleanup_ratio?: number;
    observer_cleanup_ratio?: number;
  };
  
  // Accessibility Metrics
  accessibility: {
    wcag_aa_compliance_score?: number;  // 0-100
    aria_attribute_coverage?: number;
    keyboard_navigation_score?: number;
    color_contrast_violations: number;
    missing_alt_texts: number;
  };
  
  // Architecture Metrics
  architecture: {
    component_count: number;
    average_component_size_lines: number;
    max_component_size_lines: number;
    circular_dependencies: number;
    coupling_score?: number;            // 0-100, lower is better
    cohesion_score?: number;            // 0-100, higher is better
  };
  
  // Testing Metrics
  testing: {
    test_file_count?: number;
    test_coverage_estimate?: number;    // % if available
    assertion_density?: number;         // assertions per test
  };
  
  // Maintainability Metrics
  maintainability: {
    average_function_length: number;
    max_function_length: number;
    comment_ratio: number;              // % of comments
    duplicate_code_blocks: number;
    code_smell_count: number;
  };
}

/**
 * Quality Gate result (PASS/FAIL for CI/CD)
 * (Mirrors backend QualityGate.GateResult)
 */
export interface QualityGateResult {
  passed: boolean;
  reasons: string[];
  metrics: {
    critical_count: number;
    major_count: number;
    minor_count: number;
    total_issues: number;
    typescript_coverage: number;
    accessibility_score?: number;
    security_vulnerabilities: number;
  };
  thresholds: {
    max_critical: number;
    max_major: number;
    max_total: number;
    min_typescript_coverage: number;
    min_accessibility_score?: number;
  };
}

/**
 * Technical debt estimation
 * (Mirrors backend TechnicalDebt)
 */
export interface TechnicalDebtEstimate {
  total_remediation_hours: number;
  by_severity: {
    CRITICAL: number;
    MAJOR: number;
    MINOR: number;
    INFO: number;
  };
  by_category: {
    [key in IssueCategory]?: number;
  };
  debt_ratio_percentage: number;  //相对于总开发成本的比例
}

/**
 * Rule interface (mirrors backend QualityRule)
 */
export interface QualityRule {
  /**
   * Unique rule key (e.g., "FE-TS-001")
   */
  getRuleKey(): string;
  
  /**
   * Human-readable rule name
   */
  getName(): string;
  
  /**
   * Category classification
   */
  getCategory(): IssueCategory;
  
  /**
   * Check source code and return quality issues found
   * @param sourceCode - File content
   * @param filePath - File path for context
   * @param options - Additional context (framework, config, etc.)
   */
  check(
    sourceCode: string,
    filePath: string,
    options?: RuleCheckOptions
  ): QualityIssue[];
  
  /**
   * Optional: auto-fix suggestion
   */
  getFixSuggestion?(issue: QualityIssue): FixSuggestion | null;
}

/**
 * Options passed to rule check method
 */
export interface RuleCheckOptions {
  framework?: 'react' | 'vue' | 'angular' | 'svelte';
  config?: Record<string, any>;
  ast?: any;  // Parsed AST if available
  dependencies?: string[];  // Import dependencies
}

/**
 * Auto-fix suggestion
 */
export interface FixSuggestion {
  type: 'auto-fix' | 'manual-fix';
  description: string;
  code_before?: string;
  code_after?: string;
  confidence: number;  // 0-1, confidence in the fix
}

/**
 * Configuration for rules engine
 * (Mirrors backend RulesConfig)
 */
export interface RulesConfig {
  enabled_rules: string[];
  disabled_rules: string[];
  
  // Severity thresholds
  thresholds: {
    max_critical_issues: number;
    max_major_issues: number;
    max_total_issues: number;
    
    // TypeScript thresholds
    min_typescript_coverage: number;
    max_any_usage: number;
    
    // Performance thresholds
    max_bundle_size_kb: number;
    max_component_size_lines: number;
    
    // Accessibility thresholds
    min_wcag_score: number;
    
    // Architecture thresholds
    max_circular_dependencies: number;
    max_function_length: number;
  };
  
  // Framework-specific settings
  framework_config?: {
    react?: {
      hooks_strict_mode: boolean;
      enforce_memoization: boolean;
    };
    vue?: {
      composition_api_only: boolean;
      enforce_setup_return: boolean;
    };
  };
}

/**
 * AST Parser interface (pluggable)
 */
export interface ASTParser {
  parse(sourceCode: string, filePath: string): any;
  getLanguage(): string;
}

/**
 * Reporter interface for output formatting
 */
export interface Reporter {
  generate(analysis: ProjectAnalysis): string | Buffer;
  getFormat(): string;  // 'json' | 'html' | 'markdown' | 'pdf'
}
