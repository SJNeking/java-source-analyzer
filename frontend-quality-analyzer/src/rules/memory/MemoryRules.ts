/**
 * Memory leak detection rules
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class MemoryRules {
  static all(): QualityRule[] {
    return [
      new MemEventListenerNoCleanup(),
      new MemTimerNoCleanup(),
      new MemObserverNoCleanup(),
      new MemSubscriptionNoCleanup(),
      new MemRefToDomNoCleanup(),
      new MemClosureInLoop(),
      new MemWindowListener(),
      new MemAbortControllerMissing(),
    ];
  }
}

class MemEventListenerNoCleanup implements QualityRule {
  getRuleKey() { return 'FE-MEM-001'; }
  getName() { return 'Event listeners added in useEffect must be cleaned up'; }
  getCategory(): IssueCategory { return 'MEMORY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/\.addEventListener\s*\(/.test(sourceCode)) {
      if (!/removeEventListener/.test(sourceCode) && !/cleanup|return\s*\(\)/.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'addEventListener without removeEventListener cleanup',
          remediation: 'Return cleanup function from useEffect that calls removeEventListener',
          confidence: 0.9,
        });
      }
    }
    return issues;
  }
}

class MemTimerNoCleanup implements QualityRule {
  getRuleKey() { return 'FE-MEM-002'; }
  getName() { return 'setInterval/setTimeout must be cleared on unmount'; }
  getCategory(): IssueCategory { return 'MEMORY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/setInterval\s*\(|setTimeout\s*\(/.test(sourceCode)) {
      if (!/clearInterval|clearTimeout/.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'Timer created without cleanup',
          remediation: 'Return cleanup function from useEffect that calls clearInterval/clearTimeout',
          confidence: 0.95,
        });
      }
    }
    return issues;
  }
}

class MemObserverNoCleanup implements QualityRule {
  getRuleKey() { return 'FE-MEM-003'; }
  getName() { return 'MutationObserver/IntersectionObserver must be disconnected'; }
  getCategory(): IssueCategory { return 'MEMORY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/new\s+(MutationObserver|IntersectionObserver|ResizeObserver)/.test(sourceCode)) {
      if (!/\.disconnect\(\)/.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'Observer created without disconnect',
          remediation: 'Call .disconnect() in useEffect cleanup function',
          confidence: 0.95,
        });
      }
    }
    return issues;
  }
}

class MemSubscriptionNoCleanup implements QualityRule {
  getRuleKey() { return 'FE-MEM-004'; }
  getName() { return 'Subscriptions must be unsubscribed on unmount'; }
  getCategory(): IssueCategory { return 'MEMORY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/\.subscribe\s*\(/.test(sourceCode) && !/\.unsubscribe\s*\(/.test(sourceCode) && !/\.dispose\s*\(/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Subscription without unsubscription',
        remediation: 'Return cleanup function that calls .unsubscribe() or .dispose()',
        confidence: 0.9,
      });
    }
    return issues;
  }
}

class MemRefToDomNoCleanup implements QualityRule {
  getRuleKey() { return 'FE-MEM-005'; }
  getName() { return 'DOM refs should be cleared on unmount'; }
  getCategory(): IssueCategory { return 'MEMORY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/useRef.*\.current\s*=\s*document|useRef.*current\s*=\s*window/.test(sourceCode)) {
      if (!/useEffect.*return.*null/.test(sourceCode) && !/onUnmounted/.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'DOM ref may not be cleared on unmount',
          remediation: 'Set ref.current = null in cleanup function',
          confidence: 0.6,
        });
      }
    }
    return issues;
  }
}

class MemClosureInLoop implements QualityRule {
  getRuleKey() { return 'FE-MEM-006'; }
  getName() { return 'Closures created in loops retain references to loop variables'; }
  getCategory(): IssueCategory { return 'MEMORY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/for\s*\(.*\)\s*\{[\s\S]*?\s*=>/.test(sourceCode) && !/let\s+/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Closure created in loop, may retain excessive memory',
        remediation: 'Use let instead of var, or extract function outside loop',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class MemWindowListener implements QualityRule {
  getRuleKey() { return 'FE-MEM-007'; }
  getName() { return 'window event listeners must be removed on unmount'; }
  getCategory(): IssueCategory { return 'MEMORY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/window\.addEventListener/.test(sourceCode) && !/window\.removeEventListener/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'window.addEventListener without removeEventListener',
        remediation: 'Remove window event listener in useEffect cleanup',
        confidence: 0.95,
      });
    }
    return issues;
  }
}

class MemAbortControllerMissing implements QualityRule {
  getRuleKey() { return 'FE-MEM-008'; }
  getName() { return 'Async fetch requests should use AbortController for cleanup'; }
  getCategory(): IssueCategory { return 'MEMORY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/fetch\s*\(|axios\./.test(sourceCode) && !/AbortController|abort|CancelToken|cancelToken/.test(sourceCode)) {
      if (/useEffect.*async|useEffect.*fetch|useEffect.*axios/.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'Async request without AbortController',
          remediation: 'Use AbortController to cancel requests on unmount',
          confidence: 0.7,
        });
      }
    }
    return issues;
  }
}
