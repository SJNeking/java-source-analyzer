/**
 * Event Handling quality rules
 * Detects event handling anti-patterns and performance issues
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class EventHandlingRules {
  static all(): QualityRule[] {
    return [
      new EventPassiveListenerMissing(),
      new EventInlineHandlerInLoop(),
      new EventMissingDelegation(),
      new EventRaceCondition(),
      new EventNamingInconsistency(),
      new EventMissingCleanup(),
    ];
  }
}

class EventPassiveListenerMissing implements QualityRule {
  getRuleKey() { return 'FE-EVENT-001'; }
  getName() { return 'Missing passive event listener'; }
  getCategory(): IssueCategory { return 'EVENT_HANDLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Scroll/touch/wheel events without passive: true
    const scrollTouchEvents = /addEventListener\s*\(\s*['"](scroll|touchstart|touchmove|wheel|mousewheel)['"]/g;
    const matches = sourceCode.match(scrollTouchEvents) || [];
    const hasPassive = /passive\s*:\s*true/g;
    const passiveMatches = sourceCode.match(hasPassive) || [];
    // If there are scroll/touch events but no passive flag
    if (matches.length > 0 && passiveMatches.length < matches.length) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${matches.length - passiveMatches.length} scroll/touch event(s) without passive option`,
        remediation: 'Add { passive: true } to scroll/touch listeners to improve scroll performance',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class EventInlineHandlerInLoop implements QualityRule {
  getRuleKey() { return 'FE-EVENT-002'; }
  getName() { return 'Inline event handler in loop creates N functions'; }
  getCategory(): IssueCategory { return 'EVENT_HANDLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // onClick={() => ...} inside .map()
    const inlineInLoop = /\.(?:map|forEach|reduce)\([^)]*=>\s*<[^>]*\s+(?:onClick|onChange|onSubmit|onHover)\s*=\s*\{[\s\(\)]*=>/gs;
    const matches = sourceCode.match(inlineInLoop) || [];
    if (matches.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${matches.length} inline event handler(s) in loop creates unnecessary functions`,
        remediation: 'Extract handler outside loop or use useCallback with stable reference',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class EventMissingDelegation implements QualityRule {
  getRuleKey() { return 'FE-EVENT-003'; }
  getName() { return 'Missing event delegation for similar handlers'; }
  getCategory(): IssueCategory { return 'EVENT_HANDLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Multiple similar event handlers that could use delegation
    const onClickCount = (sourceCode.match(/onClick\s*=/g) || []).length;
    const onChangeCount = (sourceCode.match(/onChange\s*=/g) || []).length;
    const hasDelegation = /event\.target|event\.currentTarget|e\.target|e\.currentTarget/.test(sourceCode);
    if ((onClickCount > 5 || onChangeCount > 5) && !hasDelegation) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${Math.max(onClickCount, onChangeCount)} similar event handlers may benefit from delegation`,
        remediation: 'Use event delegation: onClick={e => handleClick(e.target.dataset.id)}',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class EventRaceCondition implements QualityRule {
  getRuleKey() { return 'FE-EVENT-004'; }
  getName() { return 'Event handler race condition (missing debounce/throttle)'; }
  getCategory(): IssueCategory { return 'EVENT_HANDLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Rapid-fire events without debounce/throttle
    const hasRapidEvent = /onScroll|onResize|onMouseMove|onInput|onKeyDown.*search/i.test(sourceCode);
    const hasDebounce = /debounce|throttle|lodash|use-debounce|use-throttle/i.test(sourceCode);
    const hasFetch = /fetch\(|axios\.|useQuery/i.test(sourceCode);
    if (hasRapidEvent && hasFetch && !hasDebounce) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Rapid-fire event may cause excessive API calls',
        remediation: 'Debounce or throttle event handler to prevent excessive network requests',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class EventNamingInconsistency implements QualityRule {
  getRuleKey() { return 'FE-EVENT-005'; }
  getName() { return 'Event handler naming inconsistency'; }
  getCategory(): IssueCategory { return 'EVENT_HANDLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Mixed naming: handleClick vs onClick vs on-click vs onPress
    const patterns = {
      handleX: (sourceCode.match(/handle[A-Z]\w+/g) || []).length,
      onX: (sourceCode.match(/on[A-Z]\w+/g) || []).length,
      onPress: (sourceCode.match(/onPress/g) || []).length,
    };
    const usedPatterns = Object.entries(patterns).filter(([_, count]) => count > 0).length;
    if (usedPatterns >= 2) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `Inconsistent event handler naming: handleX(${patterns.handleX}), onX(${patterns.onX}), onPress(${patterns.onPress})`,
        remediation: 'Use consistent naming convention (e.g., always handleX or onX)',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class EventMissingCleanup implements QualityRule {
  getRuleKey() { return 'FE-EVENT-006'; }
  getName() { return 'Event listener missing cleanup'; }
  getCategory(): IssueCategory { return 'EVENT_HANDLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isComponent = /\.(tsx|jsx|vue)$/.test(filePath);
    if (!isComponent) return issues;
    // addEventListener without removeEventListener in cleanup
    const addCount = (sourceCode.match(/addEventListener/g) || []).length;
    const removeCount = (sourceCode.match(/removeEventListener/g) || []).length;
    const hasCleanup = /useEffect\([^)]*\)\s*=>\s*{[^}]*return\s*\(\)\s*=>/s.test(sourceCode);
    if (addCount > 0 && (removeCount === 0 || !hasCleanup)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${addCount} event listener(s) added but may not be cleaned up`,
        remediation: 'Remove event listeners in useEffect cleanup to prevent memory leaks',
        confidence: 0.85,
      });
    }
    return issues;
  }
}
