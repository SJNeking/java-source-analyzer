/**
 * Lifecycle/Timing quality rules
 * Detects React lifecycle issues and timing anti-patterns
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class LifecycleTimingRules {
  static all(): QualityRule[] {
    return [
      new LifeRaceConditionInEffect(),
      new LifeMissingMountedCheck(),
      new LifeStrictModeDoubleExecution(),
      new LifeCleanupOrdering(),
      new LifeTimerDrift(),
      new LifeMissingDependencyArray(),
    ];
  }
}

class LifeRaceConditionInEffect implements QualityRule {
  getRuleKey() { return 'FE-LIFE-001'; }
  getName() { return 'Race condition in useEffect async operation'; }
  getCategory(): IssueCategory { return 'LIFECYCLE_TIMING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isReact = /\.tsx?$/.test(filePath);
    if (!isReact) return issues;
    // Multiple useEffects with async ops without cancellation
    const asyncEffects = /useEffect\s*\(\s*\(\)\s*=>\s*(?:async|\{[\s\S]*?async)/g;
    const effects = sourceCode.match(asyncEffects) || [];
    const hasCancellation = /AbortController|let cancelled|aborted/i.test(sourceCode);
    if (effects.length > 1 && !hasCancellation) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${effects.length} async effect(s) may cause race condition`,
        remediation: 'Use AbortController or cancellation flag in effect cleanup',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class LifeMissingMountedCheck implements QualityRule {
  getRuleKey() { return 'FE-LIFE-002'; }
  getName() { return 'State update after unmount (React < 18)'; }
  getCategory(): IssueCategory { return 'LIFECYCLE_TIMING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isComponent = /\.(tsx|jsx|vue)$/.test(filePath);
    if (!isComponent) return issues;
    // Async operations without mounted check
    const hasAsyncOp = /setTimeout|setInterval|fetch\(|axios\.|Promise/g.test(sourceCode);
    const hasMountedCheck = /isMounted|isSubscribed|mounted.current/i.test(sourceCode);
    const hasStateUpdate = /setState\(|dispatch\(/g.test(sourceCode);
    if (hasAsyncOp && hasStateUpdate && !hasMountedCheck) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Async operation may update state after unmount',
        remediation: 'Track mounted state and check before setState in async callbacks',
        confidence: 0.75,
      });
    }
    return issues;
  }
}

class LifeStrictModeDoubleExecution implements QualityRule {
  getRuleKey() { return 'FE-LIFE-003'; }
  getName() { return 'Non-idempotent side effect in StrictMode'; }
  getCategory(): IssueCategory { return 'LIFECYCLE_TIMING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isReact = /\.tsx?$/.test(filePath);
    if (!isReact) return issues;
    // Effects with side effects that shouldn't run twice
    const hasSideEffects = [
      /fetch\(|axios\./,
      /localStorage\.|sessionStorage\./,
      /analytics\.|track\(|gtag\(/,
      /new\s+WebSocket|new\s+EventSource/,
    ].some(pattern => pattern.test(sourceCode));
    const hasCleanup = /return\s*\(\)\s*=>/.test(sourceCode);
    const hasIdempotent = /useRef|useMemo|useCallback/.test(sourceCode);
    if (hasSideEffects && !hasCleanup && !hasIdempotent) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Side effect may execute twice in React 18 StrictMode',
        remediation: 'Add cleanup function or make effect idempotent',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class LifeCleanupOrdering implements QualityRule {
  getRuleKey() { return 'FE-LIFE-004'; }
  getName() { return 'Cleanup function accesses cleared state'; }
  getCategory(): IssueCategory { return 'LIFECYCLE_TIMING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isReact = /\.tsx?$/.test(filePath);
    if (!isReact) return issues;
    // Cleanup that might access stale refs
    const hasCleanup = /useEffect\([^)]*return\s*\(\)\s*=>\s*\{[\s\S]*?\}/g;
    const cleanups = sourceCode.match(hasCleanup) || [];
    const accessesRefInCleanup = cleanups.some(cleanup =>
      /\.current/.test(cleanup) && /removeEventListener|clearTimeout|abort/i.test(cleanup)
    );
    if (accessesRefInCleanup) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Cleanup may access stale refs or cleared state',
        remediation: 'Capture refs/values in effect scope before cleanup returns',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class LifeTimerDrift implements QualityRule {
  getRuleKey() { return 'FE-LIFE-005'; }
  getName() { return 'Recurring task uses setTimeout instead of setInterval'; }
  getCategory(): IssueCategory { return 'LIFECYCLE_TIMING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // setTimeout used recursively for recurring tasks
    const hasRecursiveTimeout = /setTimeout\s*\(\s*\(\)\s*=>\s*\{[\s\S]*setTimeout/g;
    const hasTimerDrift = hasRecursiveTimeout.test(sourceCode);
    const hasSetInterval = /setInterval/.test(sourceCode);
    if (hasTimerDrift && !hasSetInterval) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Recursive setTimeout may cause timer drift',
        remediation: 'Use setInterval for recurring tasks or account for execution time',
        confidence: 0.75,
      });
    }
    return issues;
  }
}

class LifeMissingDependencyArray implements QualityRule {
  getRuleKey() { return 'FE-LIFE-006'; }
  getName() { return 'useEffect missing dependency array'; }
  getCategory(): IssueCategory { return 'LIFECYCLE_TIMING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isReact = /\.tsx?$/.test(filePath);
    if (!isReact) return issues;
    // useEffect without dependency array (runs every render)
    const effectNoDeps = /useEffect\s*\(\s*\(\)\s*=>\s*\{[^}]+\}\s*\)/g;
    const effects = sourceCode.match(effectNoDeps) || [];
    const hasDeps = /useEffect\s*\([^,]+,\s*\[/.test(sourceCode);
    if (effects.length > 0 && !hasDeps) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${effects.length} useEffect(s) without dependency array`,
        remediation: 'Add dependency array: useEffect(() => { ... }, [deps])',
        confidence: 0.85,
      });
    }
    return issues;
  }
}
