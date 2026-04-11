/**
 * Build configuration quality rules
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class BuildConfigRules {
  static all(): QualityRule[] {
    return [
      new BuildNoStrictTs(),
      new BuildSourceMapExposed(),
      new BuildNoTreeShaking(),
      new BuildDevInProd(),
      new BuildNoTsPathAlias(),
    ];
  }
}

class BuildNoStrictTs implements QualityRule {
  getRuleKey() { return 'FE-BUILD-001'; }
  getName() { return 'TypeScript strict mode is not enabled'; }
  getCategory(): IssueCategory { return 'BUILD'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/tsconfig\.json$/.test(filePath)) {
      if (!/"strict"\s*:\s*true/.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'TypeScript strict mode not enabled',
          remediation: 'Set "strict": true in tsconfig.json',
          confidence: 0.95,
        });
      }
    }
    return issues;
  }
}

class BuildSourceMapExposed implements QualityRule {
  getRuleKey() { return 'FE-BUILD-002'; }
  getName() { return 'Source maps should not be exposed in production'; }
  getCategory(): IssueCategory { return 'BUILD'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/vite\.config|webpack\.config|next\.config|angular\.json/.test(filePath)) {
      if (/sourcemap.*true|devtool.*source-map|devtool.*eval-source-map/.test(sourceCode) &&
          !/process\.env\.NODE_ENV.*production/.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'Source maps may be enabled in production build',
          remediation: 'Disable source maps for production or use hidden-source-map',
          confidence: 0.7,
        });
      }
    }
    return issues;
  }
}

class BuildNoTreeShaking implements QualityRule {
  getRuleKey() { return 'FE-BUILD-003'; }
  getName() { return 'Tree shaking may not be working'; }
  getCategory(): IssueCategory { return 'BUILD'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/package\.json$/.test(filePath)) {
      if (!/"sideEffects"\s*:/.test(sourceCode) && !/"type"\s*:\s*"module"/.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'No sideEffects field, tree shaking may include unused code',
          remediation: 'Add "sideEffects": false or list files with side effects',
          confidence: 0.8,
        });
      }
    }
    return issues;
  }
}

class BuildDevInProd implements QualityRule {
  getRuleKey() { return 'FE-BUILD-004'; }
  getName() { return 'Development mode should not run in production'; }
  getCategory(): IssueCategory { return 'BUILD'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/vite\.config|webpack\.config/.test(filePath)) {
      if (/mode\s*:\s*['"]development['"]|NODE_ENV\s*=\s*['"]development['"]/.test(sourceCode) &&
          !/process\.env/.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.CRITICAL,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'Development mode hardcoded in config',
          remediation: 'Use environment variables to switch mode',
          confidence: 0.9,
        });
      }
    }
    return issues;
  }
}

class BuildNoTsPathAlias implements QualityRule {
  getRuleKey() { return 'FE-BUILD-005'; }
  getName() { return 'Project should use TypeScript path aliases'; }
  getCategory(): IssueCategory { return 'BUILD'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/tsconfig\.json$/.test(filePath)) {
      if (!/"baseUrl"|paths\s*:/.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'No path aliases configured in tsconfig',
          remediation: 'Add baseUrl and paths in compilerOptions',
          confidence: 0.8,
        });
      }
    }
    return issues;
  }
}
