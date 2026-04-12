/**
 * State Management quality rules
 * Detects state management anti-patterns and best practice violations
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class StateManagementRules {
  static all(): QualityRule[] {
    return [
      new StateDerivedStateNotMemoized(),
      new StateStaleClosure(),
      new StateAsyncStateSync(),
      new StateNotNormalized(),
      new StateSelectorRecomputation(),
      new StateMissingOptimisticUpdate(),
      new StateUnnecessaryRerender(),
      new StateRaceCondition(),
    ];
  }
}

class StateDerivedStateNotMemoized implements QualityRule {
  getRuleKey() { return 'FE-STATE-001'; }
  getName() { return 'Derived state not memoized'; }
  getCategory(): IssueCategory { return 'STATE_MANAGEMENT'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Computed values in useState instead of useMemo
    const derivedInState = /useState\([^)]*(?:map|filter|reduce|sort|concat|split)/g;
    const matches = sourceCode.match(derivedInState) || [];
    const hasUseMemo = /useMemo/.test(sourceCode);
    if (matches.length > 0 && !hasUseMemo) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${matches.length} derived value(s) stored in state instead of useMemo`,
        remediation: 'Use useMemo for derived/computed values to avoid unnecessary calculations',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class StateStaleClosure implements QualityRule {
  getRuleKey() { return 'FE-STATE-002'; }
  getName() { return 'Stale closure in state update'; }
  getCategory(): IssueCategory { return 'STATE_MANAGEMENT'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // setState(currentVal) instead of setState(prev => ...)
    // This is a common bug pattern in async updates
    const staleUpdate = /setState\([^p][^)]*\)/g;  // Not using prev => pattern
    const directUpdate = /setState\(\w+\)/g;
    const matches = sourceCode.match(directUpdate) || [];
    const functionalUpdate = /setState\(prev\s*=>|setState\(\(\)/g;
    const functionalMatches = sourceCode.match(functionalUpdate) || [];
    // Only flag if there are direct updates and no functional updates
    if (matches.length > 2 && functionalMatches.length === 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${matches.length} state update(s) may cause stale closure`,
        remediation: 'Use functional update: setState(prev => newValue) for async/dependent updates',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class StateAsyncStateSync implements QualityRule {
  getRuleKey() { return 'FE-STATE-003'; }
  getName() { return 'Async state update without mounted check'; }
  getCategory(): IssueCategory { return 'STATE_MANAGEMENT'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isComponent = /\.(tsx|jsx|vue)$/.test(filePath);
    if (!isComponent) return issues;
    // Async setState without cleanup
    const hasAsyncEffect = /useEffect\(\s*\(\)\s*=>\s*(async|\{[\s\S]*async)/.test(sourceCode);
    const hasMountedCheck = /isMounted|aborted|cancelled/i.test(sourceCode);
    const hasSetState = /setState\(|dispatch\(/.test(sourceCode);
    if (hasAsyncEffect && hasSetState && !hasMountedCheck) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Async state update may execute after unmount',
        remediation: 'Add cleanup: return () => { aborted = true } or use AbortController',
        confidence: 0.75,
      });
    }
    return issues;
  }
}

class StateNotNormalized implements QualityRule {
  getRuleKey() { return 'FE-STATE-004'; }
  getName() { return 'State not normalized (nested/array data)'; }
  getCategory(): IssueCategory { return 'STATE_MANAGEMENT'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Storing arrays of objects instead of normalized map
    const hasArrayOfObjects = /useState<\w+\[\]>\(\[\)|useState\(\[[^\]]*\{/.test(sourceCode);
    const hasNestedAccess = /\w+\[\w+\]\.\w+/g;
    const nestedMatches = sourceCode.match(hasNestedAccess) || [];
    const isNormalized = /Record<|Map<|{ \[id:|byId|entities/i.test(sourceCode);
    if (hasArrayOfObjects && nestedMatches.length > 3 && !isNormalized) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'State stores nested arrays which may cause performance issues',
        remediation: 'Normalize state: use Record<ID, Entity> or Map for O(1) lookups',
        confidence: 0.65,
      });
    }
    return issues;
  }
}

class StateSelectorRecomputation implements QualityRule {
  getRuleKey() { return 'FE-STATE-005'; }
  getName() { return 'Selector recomputes on every render'; }
  getCategory(): IssueCategory { return 'STATE_MANAGEMENT'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // useSelector with expensive computation without createSelector
    const hasExpensiveSelector = /useSelector\(state\s*=>\s*(?:map|filter|reduce|sort)/g;
    const matches = sourceCode.match(hasExpensiveSelector) || [];
    const hasCreateSelector = /createSelector|reselect|useMemoSelector/.test(sourceCode);
    if (matches.length > 0 && !hasCreateSelector) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${matches.length} selector(s) may recompute on every render`,
        remediation: 'Use createSelector (reselect) or useMemo to memoize expensive selectors',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class StateMissingOptimisticUpdate implements QualityRule {
  getRuleKey() { return 'FE-STATE-006'; }
  getName() { return 'Mutation without optimistic update or rollback'; }
  getCategory(): IssueCategory { return 'STATE_MANAGEMENT'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Mutations without optimistic UI or rollback
    const hasMutation = /useMutation|mutate|\.post\(|\.put\(|\.delete\(/.test(sourceCode);
    const hasOptimistic = /optimistic|onMutate|rollback|revert/i.test(sourceCode);
    const hasErrorRollback = /onError.*setState|catch.*setState|rollback/i.test(sourceCode);
    if (hasMutation && !hasOptimistic && !hasErrorRollback) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Mutation without optimistic update or error rollback',
        remediation: 'Implement optimistic updates with rollback on error for better UX',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class StateUnnecessaryRerender implements QualityRule {
  getRuleKey() { return 'FE-STATE-007'; }
  getName() { return 'Context value causes unnecessary rerenders'; }
  getCategory(): IssueCategory { return 'STATE_MANAGEMENT'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Inline object/array as context value
    const inlineContextValue = /<\w+Provider\s+value=\{[\s\{]*(?:\{|\[)/g;
    const matches = sourceCode.match(inlineContextValue) || [];
    const hasUseMemo = /useMemo\([^)]*value/.test(sourceCode);
    if (matches.length > 0 && !hasUseMemo) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${matches.length} context provider(s) with inline value causes rerenders`,
        remediation: 'Memoize context value: const value = useMemo(() => ({...}), [deps])',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class StateRaceCondition implements QualityRule {
  getRuleKey() { return 'FE-STATE-008'; }
  getName() { return 'Race condition in state updates'; }
  getCategory(): IssueCategory { return 'STATE_MANAGEMENT'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Multiple setState in same function without coordination
    const multipleSetStates = /(setState|dispatch)[^{]*(?:setState|dispatch)/g;
    const matches = sourceCode.match(multipleSetStates) || [];
    const hasCoordination = /Promise\.all|await.*await|batch\(/.test(sourceCode);
    if (matches.length > 0 && !hasCoordination) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Multiple state updates may cause race condition',
        remediation: 'Use batch() or coordinate updates to avoid race conditions',
        confidence: 0.7,
      });
    }
    return issues;
  }
}
