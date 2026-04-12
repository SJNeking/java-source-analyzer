/**
 * Error Handling quality rules
 * Detects error handling anti-patterns and missing error paths
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class ErrorHandlingRules {
  static all(): QualityRule[] {
    return [
      new ErrUncaughtPromise(),
      new ErrSwallowedError(),
      new ErrGenericErrorType(),
      new ErrMissingErrorBoundary(),
      new ErrSilentFailure(),
      new ErrMissingLoadingState(),
      new ErrInconsistentErrorHandling(),
      new ErrMissingErrorMessages(),
    ];
  }
}

class ErrUncaughtPromise implements QualityRule {
  getRuleKey() { return 'FE-ERR-001'; }
  getName() { return 'Uncaught promise rejection'; }
  getCategory(): IssueCategory { return 'ERROR_HANDLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // .then() without .catch()
    const thenWithoutCatch = /(?:\.then\([^)]+\)(?!\s*\.catch))/g;
    const matches = sourceCode.match(thenWithoutCatch) || [];
    // async function without try/catch
    const asyncWithoutTry = /async\s+\w+\s*\([^)]*\)\s*(?!\s*{[\s\S]*try)[^{]*{/g;
    const asyncMatches = sourceCode.match(asyncWithoutTry) || [];
    if (matches.length > 0 || asyncMatches.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${matches.length + asyncMatches.length} promise(s) without error handling`,
        remediation: 'Add .catch() or wrap async code in try/catch',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class ErrSwallowedError implements QualityRule {
  getRuleKey() { return 'FE-ERR-002'; }
  getName() { return 'Swallowed error (empty catch block)'; }
  getCategory(): IssueCategory { return 'ERROR_HANDLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Empty catch blocks: catch(e) {} or catch(e) { /* comment only */ }
    const emptyCatch = /catch\s*\([^)]*\)\s*\{[\s\/*]*\}/g;
    const matches = sourceCode.match(emptyCatch) || [];
    // Catch with only console.error
    const onlyConsoleError = /catch\s*\([^)]*\)\s*\{[\s]*console\.(error|warn|log)[\s\S]*?\}/g;
    const consoleMatches = sourceCode.match(onlyConsoleError) || [];
    if (matches.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.CRITICAL,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${matches.length} empty catch block(s) - errors are silently ignored`,
        remediation: 'Handle error: show user feedback, log to error tracking, or rethrow',
        confidence: 0.95,
      });
    }
    return issues;
  }
}

class ErrGenericErrorType implements QualityRule {
  getRuleKey() { return 'FE-ERR-003'; }
  getName() { return 'Generic error type in catch block'; }
  getCategory(): IssueCategory { return 'ERROR_HANDLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isTypeScript = /\.tsx?$/.test(filePath);
    if (!isTypeScript) return issues;
    // catch(e: any) or catch(e)
    const genericCatch = /catch\s*\(\s*\w+\s*:\s*any\s*\)/g;
    const anyCatch = sourceCode.match(genericCatch) || [];
    const untypedCatch = /catch\s*\(\s*\w+\s*\)/g;
    const untyped = sourceCode.match(untypedCatch) || [];
    if (anyCatch.length > 0 || untyped.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `Generic error type: ${anyCatch.length + untyped.length} catch block(s)`,
        remediation: 'Use proper error type: catch(e: Error) or instanceof checks',
        confidence: 0.9,
      });
    }
    return issues;
  }
}

class ErrMissingErrorBoundary implements QualityRule {
  getRuleKey() { return 'FE-ERR-004'; }
  getName() { return 'Component may need Error Boundary wrapper'; }
  getCategory(): IssueCategory { return 'ERROR_HANDLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isReactComponent = /\.tsx$/.test(filePath);
    if (!isReactComponent) return issues;
    // Component with async operations but no error boundary detection
    const hasAsync = /useEffect.*async|useQuery|useMutation|fetch\(|axios\./.test(sourceCode);
    const hasErrorHandling = /catch|onError|error\s*=/i.test(sourceCode);
    const hasErrorBoundary = /ErrorBoundary|error\s*boundary/i.test(sourceCode);
    if (hasAsync && !hasErrorHandling && !hasErrorBoundary) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Async component without error handling or Error Boundary',
        remediation: 'Wrap in Error Boundary or add error state handling',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class ErrSilentFailure implements QualityRule {
  getRuleKey() { return 'FE-ERR-005'; }
  getName() { return 'Silent failure (console.error in production code)'; }
  getCategory(): IssueCategory { return 'ERROR_HANDLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // console.error without proper error handling
    const consoleErrorInCatch = /catch[^{]*\{[^}]*console\.error/g;
    const matches = sourceCode.match(consoleErrorInCatch) || [];
    if (matches.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${matches.length} console.error without proper error handling`,
        remediation: 'Use error tracking service (Sentry, etc.) and show user feedback',
        confidence: 0.75,
      });
    }
    return issues;
  }
}

class ErrMissingLoadingState implements QualityRule {
  getRuleKey() { return 'FE-ERR-006'; }
  getName() { return 'Async data fetching without loading state'; }
  getCategory(): IssueCategory { return 'ERROR_HANDLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isComponent = /\.(tsx|jsx|vue)$/.test(filePath);
    if (!isComponent) return issues;
    const hasDataFetching = /useQuery|useMutation|fetch\(|axios\./.test(sourceCode);
    const hasLoadingState = /loading|isLoading|pending|fetching/i.test(sourceCode);
    if (hasDataFetching && !hasLoadingState) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Data fetching without loading indicator',
        remediation: 'Add loading state to improve UX during async operations',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class ErrInconsistentErrorHandling implements QualityRule {
  getRuleKey() { return 'FE-ERR-007'; }
  getName() { return 'Inconsistent error handling patterns'; }
  getCategory(): IssueCategory { return 'ERROR_HANDLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const patterns = {
      tryCatch: (sourceCode.match(/try\s*\{/g) || []).length,
      dotCatch: (sourceCode.match(/\.catch\(/g) || []).length,
      onError: (sourceCode.match(/onError|onFailure/g) || []).length,
    };
    const usedPatterns = Object.values(patterns).filter(count => count > 0).length;
    if (usedPatterns >= 2) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `Mixed error handling: try/catch(${patterns.tryCatch}), .catch(${patterns.dotCatch}), onError(${patterns.onError})`,
        remediation: 'Use consistent error handling approach throughout the file',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class ErrMissingErrorMessages implements QualityRule {
  getRuleKey() { return 'FE-ERR-008'; }
  getName() { return 'Missing user-facing error messages'; }
  getCategory(): IssueCategory { return 'ERROR_HANDLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isComponent = /\.(tsx|jsx|vue)$/.test(filePath);
    if (!isComponent) return issues;
    const hasErrorHandling = /catch|\.catch|onError/.test(sourceCode);
    const hasUserFeedback = /setError|showError|toast|notification|alert|message/i.test(sourceCode);
    if (hasErrorHandling && !hasUserFeedback) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Error handling without user feedback',
        remediation: 'Show error message to user via toast, notification, or inline message',
        confidence: 0.7,
      });
    }
    return issues;
  }
}
