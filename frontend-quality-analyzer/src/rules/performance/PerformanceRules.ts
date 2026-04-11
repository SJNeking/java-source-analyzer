/**
 * Frontend Performance Rules
 * 
 * Detects performance anti-patterns and optimization opportunities.
 * Mirrors backend PerformanceRules architecture.
 */

import { QualityIssue, QualityRule, IssueCategory, Severity } from '../../types';

// ==================== Base Class ====================

abstract class AbstractPerformanceRule implements QualityRule {
  abstract getRuleKey(): string;
  abstract getName(): string;
  
  getCategory(): IssueCategory {
    return 'PERFORMANCE';
  }

  check(sourceCode: string, filePath: string): QualityIssue[] {
    return this.checkPerformance(sourceCode, filePath);
  }

  protected abstract checkPerformance(sourceCode: string, filePath: string): QualityIssue[];
}

// ==================== Rule Implementations ====================

/**
 * FE-PERF-001: Large bundle size indicator
 * 
 * Detects patterns that lead to large bundle sizes.
 */
export class BundleSizeRule extends AbstractPerformanceRule {
  getRuleKey(): string { return 'FE-PERF-001'; }
  getName(): string { return 'Import pattern may increase bundle size'; }

  protected checkPerformance(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect importing entire libraries instead of specific modules
      const largeLibraryPatterns = [
        { pattern: /import\s+.*\s+from\s+['"]lodash['"]/, suggestion: 'lodash-es or lodash/get' },
        { pattern: /import\s+.*\s+from\s+['"]moment['"]/, suggestion: 'dayjs or date-fns' },
        { pattern: /import\s+.*\s+from\s+['"]antd['"]/, suggestion: 'antd/es/component' },
        { pattern: /import\s+.*\s+from\s+['"]@mui\/material['"]/, suggestion: '@mui/material/Component' },
        { pattern: /require\s*\(\s*['"]lodash['"]\s*\)/, suggestion: 'lodash-es' }
      ];

      largeLibraryPatterns.forEach(({ pattern, suggestion }) => {
        if (pattern.test(line)) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MAJOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: `Importing entire library increases bundle size`,
            evidence: line.trim(),
            remediation: `Use tree-shakeable import: import ${suggestion}`
          });
        }
      });

      // Detect dynamic imports that could be lazy loaded
      if (line.match(/import\s*\(\s*['"].*['"]\s*\)/) && !filePath.includes('route')) {
        // This is actually good - dynamic import for code splitting
        // But we can suggest lazy loading for components
      }
    });

    return issues;
  }
}

/**
 * FE-PERF-002: Missing lazy loading
 * 
 * Detects components/routes that should be lazy loaded.
 */
export class LazyLoadingRule extends AbstractPerformanceRule {
  getRuleKey(): string { return 'FE-PERF-002'; }
  getName(): string { return 'Component/route should use lazy loading'; }

  protected checkPerformance(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    
    // Only check route files or page components
    if (!filePath.includes('route') && !filePath.includes('page') && !filePath.includes('Route')) {
      return [];
    }

    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect static imports of components in route files
      const componentImportPattern = /import\s+\w+\s+from\s+['"].*\/(components|views|pages)\//;
      if (componentImportPattern.test(line) && !line.includes('lazy')) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.MINOR,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'Component imported statically in route file',
          evidence: line.trim(),
          remediation: 'Use React.lazy(): const Component = lazy(() => import("./Component"))'
        });
      }
    });

    return issues;
  }
}

/**
 * FE-PERF-003: Image optimization
 * 
 * Detects unoptimized image usage.
 */
export class ImageOptimizationRule extends AbstractPerformanceRule {
  getRuleKey(): string { return 'FE-PERF-003'; }
  getName(): string { return 'Image not optimized'; }

  protected checkPerformance(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect <img> tags without optimization
      if (line.includes('<img') || line.includes('<img ')) {
        const checks = [
          { attr: 'loading="lazy"', message: 'Missing lazy loading' },
          { attr: 'width=', message: 'Missing width attribute (causes layout shift)' },
          { attr: 'height=', message: 'Missing height attribute (causes layout shift)' },
          { attr: 'alt=', message: 'Missing alt text' }
        ];

        checks.forEach(({ attr, message }) => {
          if (!line.includes(attr)) {
            issues.push({
              rule_key: this.getRuleKey(),
              rule_name: this.getName(),
              severity: Severity.MINOR,
              category: this.getCategory(),
              file_path: filePath,
              line: lineNum,
              message: message,
              evidence: line.trim(),
              remediation: `Add ${attr} to <img> tag`
            });
          }
        });

        // Check for large image formats
        if (line.match(/\.(jpg|jpeg|png|bmp|tiff)['"]/i)) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.INFO,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'Consider using modern image format (WebP, AVIF)',
            evidence: line.trim(),
            remediation: 'Convert to WebP or AVIF for better compression'
          });
        }
      }

      // Detect background images in inline styles
      if (line.match(/backgroundImage.*url\(/) || line.match(/background-image.*url\(/)) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.INFO,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'Background image in inline style (hard to optimize)',
          evidence: line.trim(),
          remediation: 'Use CSS classes or Next.js/Image component'
        });
      }
    });

    return issues;
  }
}

/**
 * FE-PERF-004: Unnecessary re-renders
 * 
 * Detects missing useMemo/useCallback that could prevent re-renders.
 */
export class MemoizationRule extends AbstractPerformanceRule {
  getRuleKey(): string { return 'FE-PERF-004'; }
  getName(): string { return 'Missing memoization opportunity'; }

  protected checkPerformance(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    
    // Only check React files
    if (!filePath.match(/\.(tsx|jsx)$/)) {
      return [];
    }

    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect expensive operations without memoization
      const expensivePatterns = [
        /\.map\s*\([^)]*\)\s*\./,  // Chained array methods
        /\.filter\s*\([^)]*\)\s*\./,
        /\.reduce\s*\(/,
        /JSON\.parse\s*\(/,
        /new\s+Date\s*\(/,
        /\[\.\.\..*\]/  // Array spread in render
      ];

      expensivePatterns.forEach(pattern => {
        if (pattern.test(line)) {
          // Check if wrapped in useMemo
          let hasMemoization = false;
          for (let i = Math.max(0, index - 5); i < index; i++) {
            if (lines[i].includes('useMemo')) {
              hasMemoization = true;
              break;
            }
          }

          if (!hasMemoization) {
            issues.push({
              rule_key: this.getRuleKey(),
              rule_name: this.getName(),
              severity: Severity.INFO,
              category: this.getCategory(),
              file_path: filePath,
              line: lineNum,
              message: 'Expensive operation not memoized',
              evidence: line.trim(),
              remediation: 'Wrap in useMemo: const result = useMemo(() => ..., [deps])'
            });
          }
        }
      });

      // Detect inline function definitions in JSX props
      if (line.match(/<\w+[^>]*\s+on\w+=\s*{/) && line.includes('=>')) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.INFO,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'Inline arrow function in JSX prop causes re-renders',
          evidence: line.trim(),
          remediation: 'Extract to useCallback or class method'
        });
      }
    });

    return issues;
  }
}

/**
 * FE-PERF-005: Console statements in production
 * 
 * Detects console.log/debug statements that should be removed.
 */
export class ConsoleStatementRule extends AbstractPerformanceRule {
  getRuleKey(): string { return 'FE-PERF-005'; }
  getName(): string { return 'Console statement should be removed in production'; }

  protected checkPerformance(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    // Skip test files
    if (filePath.includes('.test.') || filePath.includes('.spec.')) {
      return [];
    }

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      const consolePatterns = [
        /\bconsole\.log\s*\(/,
        /\bconsole\.debug\s*\(/,
        /\bconsole\.warn\s*\(/,
        /\bconsole\.error\s*\(/,
        /\bconsole\.table\s*\(/,
        /\bconsole\.time\s*\(/,
        /\bconsole\.trace\s*\(/
      ];

      consolePatterns.forEach(pattern => {
        if (pattern.test(line)) {
          const severity = line.includes('console.error') ? Severity.MINOR : Severity.INFO;
          
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: severity,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'Console statement found',
            evidence: line.trim(),
            remediation: 'Remove console statements or use a logging library with levels'
          });
        }
      });

      // Detect debugger statements
      if (line.match(/\bdebugger\b/)) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.MAJOR,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'Debugger statement will pause execution in production',
          evidence: line.trim(),
          remediation: 'Remove debugger statement'
        });
      }
    });

    return issues;
  }
}

/**
 * FE-PERF-006: Synchronous operations on main thread
 * 
 * Detects blocking synchronous operations.
 */
export class BlockingOperationRule extends AbstractPerformanceRule {
  getRuleKey(): string { return 'FE-PERF-006'; }
  getName(): string { return 'Synchronous operation may block main thread'; }

  protected checkPerformance(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      const blockingPatterns = [
        { pattern: /\bXMLHttpRequest\s*\(/, sync: true, message: 'Synchronous XHR blocks main thread' },
        { pattern: /alert\s*\(/, message: 'alert() blocks UI thread' },
        { pattern: /confirm\s*\(/, message: 'confirm() blocks UI thread' },
        { pattern: /prompt\s*\(/, message: 'prompt() blocks UI thread' },
        { pattern: /document\.write\s*\(/, message: 'document.write() blocks parsing' }
      ];

      blockingPatterns.forEach(({ pattern, sync, message }) => {
        if (pattern.test(line)) {
          if (sync && !line.includes('async')) {
            issues.push({
              rule_key: this.getRuleKey(),
              rule_name: this.getName(),
              severity: Severity.MAJOR,
              category: this.getCategory(),
              file_path: filePath,
              line: lineNum,
              message: message,
              evidence: line.trim(),
              remediation: 'Use fetch() with async/await instead'
            });
          } else if (!sync) {
            issues.push({
              rule_key: this.getRuleKey(),
              rule_name: this.getName(),
              severity: Severity.MINOR,
              category: this.getCategory(),
              file_path: filePath,
              line: lineNum,
              message: message,
              evidence: line.trim(),
              remediation: 'Use custom modal/dialog component instead'
            });
          }
        }
      });
    });

    return issues;
  }
}

// Export all rules for easy registration
export const PerformanceRules = {
  BundleSizeRule,
  LazyLoadingRule,
  ImageOptimizationRule,
  MemoizationRule,
  ConsoleStatementRule,
  BlockingOperationRule
};
