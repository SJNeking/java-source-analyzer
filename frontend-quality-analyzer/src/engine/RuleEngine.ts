/**
 * Frontend Quality Rule Engine
 * 
 * Orchestrates rule execution and aggregates results.
 * Mirrors the backend Java RuleEngine architecture with enhancements for frontend specifics.
 */

import {
  QualityRule,
  QualityIssue,
  ProjectAnalysis,
  FileAnalysis,
  RulesConfig,
  Reporter,
  FrontendMetrics,
  QualityGateResult,
  TechnicalDebtEstimate,
  IssueCategory,
  Severity
} from '../types';
import { getAllDefaultRules } from '../rules';

export class RuleEngine {
  private rules: Map<string, QualityRule> = new Map();
  private ruleHitCounts: Map<string, number> = new Map();
  private reporters: Map<string, Reporter> = new Map();

  constructor() {
    // Auto-register all default rules
    this.registerDefaultRules();
  }

  /**
   * Register all default rules
   */
  private registerDefaultRules(): void {
    const allRules = getAllDefaultRules();
    allRules.forEach(rule => this.registerRule(rule));
  }

  /**
   * Register a quality rule
   */
  public registerRule(rule: QualityRule): void {
    const key = rule.getRuleKey();
    this.rules.set(key, rule);
    this.ruleHitCounts.set(key, 0);
  }

  /**
   * Register multiple rules at once
   * If called with a RulesConfig, re-registers rules respecting the config
   */
  public registerRules(rulesOrConfig?: QualityRule[] | RulesConfig): void {
    if (Array.isArray(rulesOrConfig)) {
      rulesOrConfig.forEach(rule => this.registerRule(rule));
    }
    // If a config is passed, filtering is handled in filterRules() during run()
  }

  /**
   * Register a reporter for output formatting
   */
  public registerReporter(reporter: Reporter): void {
    this.reporters.set(reporter.getFormat(), reporter);
  }

  /**
   * Run all enabled rules against a single file
   * Used by CLI for per-file analysis
   */
  public runSingle(
    content: string,
    filePath: string,
    options?: {
      framework?: 'react' | 'vue' | 'angular' | 'svelte';
      language?: string;
      config?: RulesConfig;
    }
  ): QualityIssue[] {
    const activeRules = this.filterRules(options?.config);
    const issues: QualityIssue[] = [];

    // Skip rule implementation files themselves — prevents self-analysis false positives
    // e.g., SecurityRules.ts shouldn't trigger FE-SEC-001 for its own eval() detection code
    if (this.isRuleImplementationFile(filePath) || this.isEngineFile(filePath)) {
      return issues;
    }

    for (const rule of activeRules) {
      try {
        const ruleIssues = rule.check(content, filePath, {
          framework: options?.framework,
          config: options?.config?.framework_config,
        });
        issues.push(...ruleIssues);

        const currentCount = this.ruleHitCounts.get(rule.getRuleKey()) || 0;
        this.ruleHitCounts.set(rule.getRuleKey(), currentCount + ruleIssues.length);
      } catch (error) {
        // Rule should not crash the engine
      }
    }

    return issues;
  }

  /**
   * Check if a file is a rule implementation file (should skip self-analysis)
   */
  private isRuleImplementationFile(filePath: string): boolean {
    return /rules\/\w+\/\w+Rules\.ts$/.test(filePath);
  }

  /**
   * Check if a file is an engine/core file (infrastructure, skip style/security checks)
   */
  private isEngineFile(filePath: string): boolean {
    return /engine\/RuleEngine\.ts$/.test(filePath);
  }

  /**
   * Run all enabled rules against frontend source files
   *
   * @param files - Array of file contents to analyze
   * @param config - Optional configuration for rule filtering and thresholds
   * @returns Complete project analysis result
   */
  public run(
    files: Array<{ path: string; content: string }>,
    config?: RulesConfig
  ): ProjectAnalysis {
    const allIssues: QualityIssue[] = [];
    const fileAnalyses: FileAnalysis[] = [];

    // Filter rules by config if provided
    const activeRules = this.filterRules(config);

    console.log(`🔍 Analyzing ${files.length} files with ${activeRules.length} rules...`);

    // Analyze each file
    for (const file of files) {
      const fileIssues: QualityIssue[] = [];

      for (const rule of activeRules) {
        try {
          const issues = rule.check(file.content, file.path, {
            framework: this.normalizeFramework(this.detectFramework(file.path)),
            config: config?.framework_config
          });

          fileIssues.push(...issues);

          // Update hit counts
          const currentCount = this.ruleHitCounts.get(rule.getRuleKey()) || 0;
          this.ruleHitCounts.set(rule.getRuleKey(), currentCount + issues.length);
        } catch (error) {
          console.error(
            `❌ Rule ${rule.getRuleKey()} failed on ${file.path}:`,
            error instanceof Error ? error.message : error
          );
          // Continue with other rules - one failure shouldn't stop analysis
        }
      }

      const fileAnalysis: FileAnalysis = {
        file_path: file.path,
        file_size: Buffer.byteLength(file.content, 'utf-8'),
        line_count: file.content.split('\n').length,
        language: this.detectLanguage(file.path),
        framework: this.normalizeFramework(this.detectFramework(file.path)),
        issues: fileIssues,
        metrics: this.calculateFileMetrics(file.content, file.path)
      };

      fileAnalyses.push(fileAnalysis);
      allIssues.push(...fileIssues);
    }

    console.log(`✅ Found ${allIssues.length} issues across ${files.length} files`);

    // Calculate aggregated metrics
    const metrics = this.calculateMetrics(fileAnalyses, allIssues);

    // Build summary
    const bySeverity = this.summarizeBySeverity(allIssues);
    const byCategory = this.summarizeByCategory(allIssues);

    // Estimate technical debt
    const technicalDebt = this.estimateTechnicalDebt(allIssues);

    // Evaluate quality gate
    const qualityGate = config ? this.evaluateQualityGate(allIssues, metrics, config) : undefined;

    const analysis: ProjectAnalysis = {
      project_name: this.extractProjectName(files[0]?.path || ''),
      scan_date: new Date().toISOString(),
      framework_detected: this.detectDominantFramework(fileAnalyses),
      total_files: files.length,
      total_lines: fileAnalyses.reduce((sum, f) => sum + f.line_count, 0),
      total_issues: allIssues.length,
      by_severity: bySeverity,
      by_category: byCategory,
      files: fileAnalyses,
      metrics: metrics,
      quality_gate: qualityGate,
      technical_debt: technicalDebt
    };

    return analysis;
  }

  /**
   * Generate report in specified format
   */
  public generateReport(analysis: ProjectAnalysis, format: string = 'json'): string | Buffer {
    const reporter = this.reporters.get(format);
    if (!reporter) {
      throw new Error(`No reporter registered for format: ${format}`);
    }
    return reporter.generate(analysis);
  }

  /**
   * Get registered rules
   */
  public getRules(): QualityRule[] {
    return Array.from(this.rules.values());
  }

  /**
   * Get rule hit counts (for analytics)
   */
  public getRuleHitCounts(): Map<string, number> {
    return new Map(this.ruleHitCounts);
  }

  /**
   * Get most frequently violated rules
   */
  public getTopViolations(limit: number = 10): Array<{ rule_key: string; count: number }> {
    return Array.from(this.ruleHitCounts.entries())
      .map(([key, count]) => ({ rule_key: key, count }))
      .filter(item => item.count > 0)
      .sort((a, b) => b.count - a.count)
      .slice(0, limit);
  }

  // ==================== Private Helpers ====================

  /**
   * Filter rules based on configuration
   */
  private filterRules(config?: RulesConfig): QualityRule[] {
    if (!config) {
      return Array.from(this.rules.values());
    }

    return Array.from(this.rules.values()).filter(rule => {
      const key = rule.getRuleKey();
      
      // Explicitly disabled rules
      if (config.disabled_rules.includes(key)) {
        return false;
      }

      // If enabled_rules is specified, only include those
      if (config.enabled_rules.length > 0) {
        return config.enabled_rules.includes(key);
      }

      // Default: all rules enabled
      return true;
    });
  }

  /**
   * Detect programming language from file extension
   */
  private detectLanguage(filePath: string): FileAnalysis['language'] {
    if (filePath.endsWith('.ts')) return 'typescript';
    if (filePath.endsWith('.tsx')) return 'tsx';
    if (filePath.endsWith('.js')) return 'javascript';
    if (filePath.endsWith('.jsx')) return 'jsx';
    if (filePath.endsWith('.vue')) return 'vue';
    if (filePath.endsWith('.css')) return 'css';
    if (filePath.endsWith('.scss') || filePath.endsWith('.sass')) return 'scss';
    if (filePath.endsWith('.html')) return 'html';
    return 'typescript'; // default
  }

  /**
   * Detect frontend framework from file patterns
   */
  private detectFramework(filePath: string): 'react' | 'vue' | 'angular' | 'svelte' | 'nextjs' | 'nuxt' | undefined {
    if (filePath.includes('node_modules/')) return undefined;
    
    if (filePath.endsWith('.tsx') || filePath.endsWith('.jsx')) {
      if (filePath.includes('pages/') || filePath.includes('app/')) {
        return 'nextjs';
      }
      return 'react';
    }
    
    if (filePath.endsWith('.vue')) {
      if (filePath.includes('pages/') || filePath.includes('layouts/')) {
        return 'nuxt';
      }
      return 'vue';
    }
    
    if (filePath.endsWith('.svelte')) return 'svelte';
    if (filePath.endsWith('.component.ts')) return 'angular';
    
    return undefined;
  }

  /**
   * Normalize framework type to match FileAnalysis interface
   * Converts nextjs -> react, nuxt -> vue
   */
  private normalizeFramework(
    framework: 'react' | 'vue' | 'angular' | 'svelte' | 'nextjs' | 'nuxt' | undefined
  ): 'react' | 'vue' | 'angular' | 'svelte' | undefined {
    if (framework === 'nextjs') return 'react';
    if (framework === 'nuxt') return 'vue';
    return framework;
  }

  /**
   * Detect dominant framework in the project
   */
  private detectDominantFramework(fileAnalyses: FileAnalysis[]): string | undefined {
    const frameworkCounts: Record<string, number> = {};
    
    fileAnalyses.forEach(file => {
      if (file.framework) {
        frameworkCounts[file.framework] = (frameworkCounts[file.framework] || 0) + 1;
      }
    });

    const dominant = Object.entries(frameworkCounts)
      .sort((a, b) => b[1] - a[1])[0];

    return dominant ? dominant[0] : undefined;
  }

  /**
   * Calculate per-file metrics
   */
  private calculateFileMetrics(sourceCode: string, filePath: string): FileAnalysis['metrics'] {
    const lines = sourceCode.split('\n');
    const anyMatches = sourceCode.match(/\bany\b/g);
    
    return {
      cyclomatic_complexity: this.estimateCyclomaticComplexity(sourceCode),
      function_count: (sourceCode.match(/function\s+\w+|const\s+\w+\s*=\s*\(|=>/g) || []).length,
      component_count: filePath.match(/\.(tsx|jsx|vue)$/) ? 1 : 0,
      import_count: (sourceCode.match(/^import\s/mg) || []).length,
      any_type_usage: anyMatches ? anyMatches.length : 0
    };
  }

  /**
   * Estimate cyclomatic complexity (simplified version)
   */
  private estimateCyclomaticComplexity(sourceCode: string): number {
    let complexity = 1;
    const decisionPoints = [
      /\bif\s*\(/g,
      /\belse\s+if\s*\(/g,
      /\bfor\s*\(/g,
      /\bwhile\s*\(/g,
      /\bcase\s+/g,
      /\bcatch\s*\(/g,
      /&&/g,
      /\|\|/g,
      /\?\./g  // Optional chaining
    ];

    decisionPoints.forEach(pattern => {
      const matches = sourceCode.match(pattern);
      if (matches) {
        complexity += matches.length;
      }
    });

    return complexity;
  }

  /**
   * Calculate aggregated project metrics
   */
  private calculateMetrics(fileAnalyses: FileAnalysis[], allIssues: QualityIssue[]): FrontendMetrics {
    const tsFiles = fileAnalyses.filter(f => 
      ['typescript', 'tsx'].includes(f.language)
    );

    const totalLines = tsFiles.reduce((sum, f) => sum + f.line_count, 0);
    const totalAnyUsage = tsFiles.reduce((sum, f) => 
      sum + (f.metrics?.any_type_usage || 0), 0
    );

    const componentFiles = fileAnalyses.filter(f => 
      f.metrics?.component_count && f.metrics.component_count > 0
    );

    const componentSizes = componentFiles.map(f => f.line_count);
    const avgComponentSize = componentSizes.length > 0
      ? Math.round(componentSizes.reduce((a, b) => a + b, 0) / componentSizes.length)
      : 0;

    const maxComponentSize = componentSizes.length > 0
      ? Math.max(...componentSizes)
      : 0;

    // Count issues by category
    const securityIssues = allIssues.filter(i => i.category === 'SECURITY').length;
    const memoryIssues = allIssues.filter(i => i.category === 'MEMORY').length;
    const accessibilityIssues = allIssues.filter(i => i.category === 'ACCESSIBILITY').length;

    return {
      typescript: {
        coverage_percentage: totalLines > 0
          ? Math.round(((totalLines - totalAnyUsage) / totalLines) * 100)
          : 100,
        any_usage_count: totalAnyUsage,
        implicit_any_count: 0,  // Requires AST analysis
        explicit_any_count: totalAnyUsage,
        type_safety_score: Math.max(0, 100 - totalAnyUsage * 2)
      },
      performance: {
        estimated_bundle_size_kb: undefined,  // Requires build analysis
        lazy_loading_coverage: undefined,
        memoization_ratio: undefined,
        image_optimization_score: undefined,
        core_web_vitals_estimate: undefined
      },
      memory: {
        potential_leak_sites: memoryIssues,
        event_listener_cleanup_ratio: undefined,
        timer_cleanup_ratio: undefined,
        observer_cleanup_ratio: undefined
      },
      accessibility: {
        wcag_aa_compliance_score: accessibilityIssues > 0
          ? Math.max(0, 100 - accessibilityIssues * 5)
          : 100,
        aria_attribute_coverage: undefined,
        keyboard_navigation_score: undefined,
        color_contrast_violations: accessibilityIssues,
        missing_alt_texts: 0  // Requires HTML analysis
      },
      architecture: {
        component_count: componentFiles.length,
        average_component_size_lines: avgComponentSize,
        max_component_size_lines: maxComponentSize,
        circular_dependencies: 0,  // Requires dependency graph analysis
        coupling_score: undefined,
        cohesion_score: undefined
      },
      testing: {
        test_file_count: fileAnalyses.filter(f => 
          f.file_path.includes('.test.') || f.file_path.includes('.spec.')
        ).length,
        test_coverage_estimate: undefined,
        assertion_density: undefined
      },
      maintainability: {
        average_function_length: 0,  // Requires detailed parsing
        max_function_length: 0,
        comment_ratio: this.calculateCommentRatio(fileAnalyses),
        duplicate_code_blocks: 0,  // Requires clone detection
        code_smell_count: allIssues.filter(i => 
          ['MINOR', 'INFO'].includes(i.severity)
        ).length
      }
    };
  }

  /**
   * Calculate comment ratio across all files
   */
  private calculateCommentRatio(fileAnalyses: FileAnalysis[]): number {
    let totalLines = 0;
    let commentLines = 0;

    fileAnalyses.forEach(file => {
      const lines = file.file_path.endsWith('.css') || file.file_path.endsWith('.scss')
        ? []  // Skip CSS for now
        : file.issues.length > 0 
          ? []  // Would need actual file content
          : [];
      
      // Simplified: would need actual file content for accurate calculation
      totalLines += file.line_count;
    });

    return totalLines > 0 ? 0 : 0;  // Placeholder
  }

  /**
   * Summarize issues by severity
   */
  private summarizeBySeverity(issues: QualityIssue[]): {
    CRITICAL: number;
    MAJOR: number;
    MINOR: number;
    INFO: number;
  } {
    return {
      CRITICAL: issues.filter(i => i.severity === 'CRITICAL').length,
      MAJOR: issues.filter(i => i.severity === 'MAJOR').length,
      MINOR: issues.filter(i => i.severity === 'MINOR').length,
      INFO: issues.filter(i => i.severity === 'INFO').length
    };
  }

  /**
   * Summarize issues by category
   */
  private summarizeByCategory(issues: QualityIssue[]): Partial<Record<IssueCategory, number>> {
    const summary: Partial<Record<IssueCategory, number>> = {};
    
    issues.forEach(issue => {
      summary[issue.category] = (summary[issue.category] || 0) + 1;
    });

    return summary;
  }

  /**
   * Estimate technical debt in hours
   */
  public estimateTechnicalDebt(issues: QualityIssue[]): TechnicalDebtEstimate {
    // Industry standard remediation time estimates (in minutes)
    const remediationTime: Record<Severity, number> = {
      CRITICAL: 60,   // 1 hour
      MAJOR: 30,      // 30 minutes
      MINOR: 15,      // 15 minutes
      INFO: 5         // 5 minutes
    };

    const bySeverity = {
      CRITICAL: 0,
      MAJOR: 0,
      MINOR: 0,
      INFO: 0
    };

    const byCategory: Partial<Record<IssueCategory, number>> = {};
    let totalMinutes = 0;

    issues.forEach(issue => {
      const time = remediationTime[issue.severity];
      bySeverity[issue.severity] += time;
      byCategory[issue.category] = (byCategory[issue.category] || 0) + time;
      totalMinutes += time;
    });

    const totalHours = Math.round(totalMinutes / 60);

    // Convert to hours
    const bySeverityHours = {
      CRITICAL: Math.round(bySeverity.CRITICAL / 60),
      MAJOR: Math.round(bySeverity.MAJOR / 60),
      MINOR: Math.round(bySeverity.MINOR / 60),
      INFO: Math.round(bySeverity.INFO / 60)
    };

    const byCategoryHours: Partial<Record<IssueCategory, number>> = {};
    Object.entries(byCategory).forEach(([cat, mins]) => {
      byCategoryHours[cat as IssueCategory] = Math.round(mins / 60);
    });

    return {
      total_remediation_hours: totalHours,
      by_severity: bySeverityHours,
      by_category: byCategoryHours,
      debt_ratio_percentage: 0  // Would need total project cost
    };
  }

  /**
   * Evaluate quality gate (PASS/FAIL)
   */
  public evaluateQualityGate(
    issues: QualityIssue[],
    metrics: FrontendMetrics,
    config?: RulesConfig
  ): QualityGateResult {
    // Default thresholds if no config provided
    const defaultConfig: RulesConfig = config || {
      enabled_rules: [],
      disabled_rules: [],
      thresholds: {
        max_critical_issues: 0,
        max_major_issues: 10,
        max_total_issues: 50,
        min_typescript_coverage: 80,
        max_any_usage: 5,
        max_bundle_size_kb: 500,
        max_component_size_lines: 300,
        min_wcag_score: 80,
        max_circular_dependencies: 0,
        max_function_length: 50,
      },
    };

    const reasons: string[] = [];
    let passed = true;

    const criticalCount = issues.filter(i => i.severity === 'CRITICAL').length;
    const majorCount = issues.filter(i => i.severity === 'MAJOR').length;
    const minorCount = issues.filter(i => i.severity === 'MINOR').length;
    const totalCount = issues.length;

    const thresholds = defaultConfig.thresholds;

    // Check critical issues
    if (criticalCount > thresholds.max_critical_issues) {
      passed = false;
      reasons.push(
        `Critical issues exceed threshold: ${criticalCount} > ${thresholds.max_critical_issues}`
      );
    }

    // Check major issues
    if (majorCount > thresholds.max_major_issues) {
      passed = false;
      reasons.push(
        `Major issues exceed threshold: ${majorCount} > ${thresholds.max_major_issues}`
      );
    }

    // Check total issues
    if (totalCount > thresholds.max_total_issues) {
      passed = false;
      reasons.push(
        `Total issues exceed threshold: ${totalCount} > ${thresholds.max_total_issues}`
      );
    }

    // Check TypeScript coverage
    if (metrics.typescript.coverage_percentage < thresholds.min_typescript_coverage) {
      passed = false;
      reasons.push(
        `TypeScript coverage below threshold: ${metrics.typescript.coverage_percentage}% < ${thresholds.min_typescript_coverage}%`
      );
    }

    // Check accessibility score (if configured)
    if (thresholds.min_wcag_score && metrics.accessibility.wcag_aa_compliance_score) {
      if (metrics.accessibility.wcag_aa_compliance_score < thresholds.min_wcag_score) {
        passed = false;
        reasons.push(
          `Accessibility score below threshold: ${metrics.accessibility.wcag_aa_compliance_score} < ${thresholds.min_wcag_score}`
        );
      }
    }

    if (passed) {
      reasons.push('All quality gates passed ✅');
    }

    return {
      passed,
      reasons,
      metrics: {
        critical_count: criticalCount,
        major_count: majorCount,
        minor_count: minorCount,
        total_issues: totalCount,
        typescript_coverage: metrics.typescript.coverage_percentage,
        accessibility_score: metrics.accessibility.wcag_aa_compliance_score,
        security_vulnerabilities: issues.filter(i => i.category === 'SECURITY').length
      },
      thresholds: {
        max_critical: thresholds.max_critical_issues,
        max_major: thresholds.max_major_issues,
        max_total: thresholds.max_total_issues,
        min_typescript_coverage: thresholds.min_typescript_coverage,
        min_accessibility_score: thresholds.min_wcag_score
      }
    };
  }

  /**
   * Extract project name from file path
   */
  private extractProjectName(filePath: string): string {
    const parts = filePath.split('/');
    
    // Try to find common project directory patterns
    const srcIndex = parts.indexOf('src');
    if (srcIndex !== -1 && srcIndex > 0) {
      return parts[srcIndex - 1];
    }

    // Fallback: use first directory
    return parts[0] || 'unknown-project';
  }
}
