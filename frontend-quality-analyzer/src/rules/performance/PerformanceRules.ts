/**
 * Performance quality rules
 * Detects performance anti-patterns in frontend code
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class PerformanceRules {
  static all(): QualityRule[] {
    return [
      new PerfNoLazyLoading(),
      new PerfInlineStyleObject(),
      new PerfInlineFunctionInJSX(),
      new PerfLargeListNoVirtual(),
      new PerfNoImageOptimization(),
      new PerfSyncInLoop(),
      new PerfUnnecessaryRerender(),
      new PerfBlockingMainThread(),
      new PerfNoCodeSplitting(),
      new PerfExcessiveBundleSize(),
    ];
  }
}

class PerfNoLazyLoading implements QualityRule {
  getRuleKey() { return 'FE-PERF-001'; }
  getName() { return 'Routes and heavy components should use lazy loading'; }
  getCategory(): IssueCategory { return 'PERFORMANCE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/(pages|routes|router)/.test(filePath.toLowerCase()) &&
        !/lazy\s*\(|React\.lazy|defineAsyncComponent|import\(/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Route/component not using lazy loading',
        remediation: 'Use React.lazy() or defineAsyncComponent() for route-level code splitting',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class PerfInlineStyleObject implements QualityRule {
  getRuleKey() { return 'FE-PERF-002'; }
  getName() { return 'Inline style objects create new references on every render'; }
  getCategory(): IssueCategory { return 'PERFORMANCE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const matches = sourceCode.match(/style\s*=\s*\{[^}]*\}/g) || [];
    if (matches.length > 3) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${matches.length} inline style objects detected`,
        remediation: 'Extract to CSS modules, styled-components, or useMemo',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class PerfInlineFunctionInJSX implements QualityRule {
  getRuleKey() { return 'FE-PERF-003'; }
  getName() { return 'Inline arrow functions in JSX props cause unnecessary re-renders'; }
  getCategory(): IssueCategory { return 'PERFORMANCE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const inlineCallbacks = (sourceCode.match(/=\s*\(\s*\)\s*=>/g) || []).length;
    const inlineHandlers = (sourceCode.match(/=\s*\([^)]*\)\s*=>/g) || []).length;
    const total = inlineCallbacks + inlineHandlers;
    if (total > 5) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${total} inline arrow functions in JSX`,
        remediation: 'Extract to useCallback or class methods',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class PerfLargeListNoVirtual implements QualityRule {
  getRuleKey() { return 'FE-PERF-004'; }
  getName() { return 'Large lists should use virtualization'; }
  getCategory(): IssueCategory { return 'PERFORMANCE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/.map\s*\(\s*\(?/g.test(sourceCode) &&
        !/react-window|react-virtualized|virtuoso|useVirtual|tanstack.*virtual/.test(sourceCode)) {
      if (/list|table|grid|feed|timeline/i.test(filePath)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'Large list may not use virtualization',
          remediation: 'Use react-window, react-virtuoso, or @tanstack/react-virtual',
          confidence: 0.6,
        });
      }
    }
    return issues;
  }
}

class PerfNoImageOptimization implements QualityRule {
  getRuleKey() { return 'FE-PERF-005'; }
  getName() { return 'Images should use optimization (lazy loading, srcset, WebP)'; }
  getCategory(): IssueCategory { return 'PERFORMANCE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const imgMatches = sourceCode.match(/<img\s/g) || [];
    const plainImgs = imgMatches.filter(tag =>
      !/loading\s*=\s*["']lazy["']/.test(sourceCode) &&
      !/srcset=/.test(sourceCode) &&
      !/<Image\s/.test(sourceCode) // Next.js Image or similar
    );
    if (plainImgs.length > 2) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${plainImgs.length} <img> tags without optimization`,
        remediation: 'Use next/image, add loading="lazy", or use srcset',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class PerfSyncInLoop implements QualityRule {
  getRuleKey() { return 'FE-PERF-006'; }
  getName() { return 'Synchronous operations in loops block the main thread'; }
  getCategory(): IssueCategory { return 'PERFORMANCE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const loopMatch = sourceCode.match(/for\s*\(.*\)\s*\{([\s\S]*?)\}/g);
    if (loopMatch) {
      loopMatch.forEach(loop => {
        if (/\.sort\(|\.reverse\(|JSON\.parse|JSON\.stringify/.test(loop)) {
          issues.push({
            rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
            category: this.getCategory(), file_path: filePath, line: 1,
            message: 'Expensive synchronous operation in loop',
            remediation: 'Move expensive operations outside the loop or use Web Workers',
            confidence: 0.85,
          });
        }
      });
    }
    return issues;
  }
}

class PerfUnnecessaryRerender implements QualityRule {
  getRuleKey() { return 'FE-PERF-007'; }
  getName() { return 'Components should prevent unnecessary re-renders'; }
  getCategory(): IssueCategory { return 'PERFORMANCE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/React\.memo|useMemo|useCallback|shouldComponentUpdate|PureComponent/.test(sourceCode)) {
      return issues; // Already optimized
    }
    if (/\.(tsx|jsx)$/.test(filePath) && /\{.*\}/.test(sourceCode)) {
      const propDestructuring = (sourceCode.match(/const\s*\{[^}]+\}\s*=/g) || []).length;
      if (propDestructuring > 5 && !/React\.memo/.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'Component not memoized, may cause unnecessary re-renders',
          remediation: 'Wrap with React.memo or implement shouldComponentUpdate',
          confidence: 0.6,
        });
      }
    }
    return issues;
  }
}

class PerfBlockingMainThread implements QualityRule {
  getRuleKey() { return 'FE-PERF-008'; }
  getName() { return 'Heavy computations should use Web Workers'; }
  getCategory(): IssueCategory { return 'PERFORMANCE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/(encryption|hashing|image.*process|data.*transform|large.*sort|large.*filter)/i.test(sourceCode) &&
        !/Worker|worker|comlink|offscreen/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Heavy computation may block main thread',
        remediation: 'Move to Web Worker using Worker API or comlink',
        confidence: 0.5,
      });
    }
    return issues;
  }
}

class PerfNoCodeSplitting implements QualityRule {
  getRuleKey() { return 'FE-PERF-009'; }
  getName() { return 'Application should implement code splitting'; }
  getCategory(): IssueCategory { return 'PERFORMANCE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/app\.(ts|tsx|js|jsx)$/.test(filePath.toLowerCase()) &&
        !/import\s*\(|React\.lazy|defineAsyncComponent|Suspense/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'No code splitting detected in main app file',
        remediation: 'Use dynamic imports or React.lazy for route-level splitting',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class PerfExcessiveBundleSize implements QualityRule {
  getRuleKey() { return 'FE-PERF-010'; }
  getName() { return 'Import statements may increase bundle size'; }
  getCategory(): IssueCategory { return 'PERFORMANCE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Check for full library imports instead of specific modules
    const fullImports = [
      { pattern: /from\s+['"]lodash['"]/, suggestion: "from 'lodash-es' or 'lodash/get'" },
      { pattern: /from\s+['"]moment['"]/, suggestion: "from 'dayjs' or 'date-fns'" },
      { pattern: /from\s+['"]@material-ui\/core['"]/, suggestion: "import specific components only" },
    ];
    fullImports.forEach(({ pattern, suggestion }) => {
      if (pattern.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: `Full library import detected: consider ${suggestion}`,
          remediation: `Use specific imports: ${suggestion}`,
          confidence: 0.9,
        });
      }
    });
    return issues;
  }
}
