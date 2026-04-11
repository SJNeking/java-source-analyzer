/**
 * Accessibility (a11y) quality rules
 * WCAG 2.1 AA compliance checks
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class AccessibilityRules {
  static all(): QualityRule[] {
    return [
      new A11yMissingAltText(),
      new A11yMissingAriaLabel(),
      new A11yNoAutofocus(),
      new A11yMissingHeadingHierarchy(),
      new A11yNoInteractiveInsideInteractive(),
      new A11yMissingButtonTitle(),
      new A11yMissingLangAttribute(),
      new A11yNoPositiveTabindex(),
      new A11yAnchorHasHref(),
      new A11yFormLabelAssociated(),
    ];
  }
}

class A11yMissingAltText implements QualityRule {
  getRuleKey() { return 'FE-A11Y-001'; }
  getName() { return 'img elements must have alt text'; }
  getCategory(): IssueCategory { return 'ACCESSIBILITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const imgMatches = sourceCode.match(/<img\s[^>]*>/g) || [];
    imgMatches.forEach((img, idx) => {
      if (!/alt\s*=/.test(img)) {
        const lineNum = (sourceCode.indexOf(img));
        const ln = sourceCode.substring(0, lineNum).split('\n').length;
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
          category: this.getCategory(), file_path: filePath, line: ln + 1,
          message: 'Image missing alt text',
          remediation: 'Add alt attribute: alt="description" or alt="" for decorative images',
          confidence: 0.95,
        });
      }
    });
    return issues;
  }
}

class A11yMissingAriaLabel implements QualityRule {
  getRuleKey() { return 'FE-A11Y-002'; }
  getName() { return 'Interactive elements must have accessible labels'; }
  getCategory(): IssueCategory { return 'ACCESSIBILITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Buttons without text or aria-label
    const btnMatches = sourceCode.match(/<button\s[^>]*>/g) || [];
    btnMatches.forEach(btn => {
      if (!/>.*\w+.*</.test(btn) && !/aria-label/.test(btn) && !/aria-labelledby/.test(btn)) {
        const ln = sourceCode.substring(0, sourceCode.indexOf(btn)).split('\n').length;
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
          category: this.getCategory(), file_path: filePath, line: ln + 1,
          message: 'Button missing accessible label',
          remediation: 'Add text content, aria-label, or aria-labelledby',
          confidence: 0.9,
        });
      }
    });
    return issues;
  }
}

class A11yNoAutofocus implements QualityRule {
  getRuleKey() { return 'FE-A11Y-003'; }
  getName() { return 'autofocus should not be used'; }
  getCategory(): IssueCategory { return 'ACCESSIBILITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/autofocus/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'autofocus can be disruptive for screen reader users',
        remediation: 'Use focus management in useEffect instead',
        confidence: 0.95,
      });
    }
    return issues;
  }
}

class A11yMissingHeadingHierarchy implements QualityRule {
  getRuleKey() { return 'FE-A11Y-004'; }
  getName() { return 'Heading levels should not be skipped'; }
  getCategory(): IssueCategory { return 'ACCESSIBILITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const headings = sourceCode.match(/<h([1-6])\b/g) || [];
    const levels = headings.map(h => parseInt(h.match(/\d/)![0]));
    for (let i = 1; i < levels.length; i++) {
      if (levels[i] > levels[i - 1] + 1) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: `Heading level skipped from h${levels[i-1]} to h${levels[i]}`,
          remediation: 'Use sequential heading levels (h1 → h2 → h3)',
          confidence: 0.9,
        });
        break;
      }
    }
    return issues;
  }
}

class A11yNoInteractiveInsideInteractive implements QualityRule {
  getRuleKey() { return 'FE-A11Y-005'; }
  getName() { return 'Interactive elements must not contain interactive elements'; }
  getCategory(): IssueCategory { return 'ACCESSIBILITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Check for truly nested interactive elements (button inside button, etc.)
    // Only flag if there's no closing tag between the opening tags (real nesting)
    if (/<button\s[^>]*>(?![\s\S]*?<\/button>)[\s\S]*?<button\s/.test(sourceCode) ||
        /<button\s[^>]*>(?![\s\S]*?<\/button>)[\s\S]*?<a\s/.test(sourceCode) ||
        /<a\s[^>]*>(?![\s\S]*?<\/a>)[\s\S]*?<button\s/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Nested interactive elements detected',
        remediation: 'Do not nest buttons, links, or other interactive elements',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class A11yMissingButtonTitle implements QualityRule {
  getRuleKey() { return 'FE-A11Y-006'; }
  getName() { return 'Buttons must have visible text or aria-label'; }
  getCategory(): IssueCategory { return 'ACCESSIBILITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const emptyButtons = sourceCode.match(/<button\s[^>]*>\s*<\/button>/g) || [];
    emptyButtons.forEach(btn => {
      if (!/aria-label|aria-labelledby|title/.test(btn)) {
        const ln = sourceCode.substring(0, sourceCode.indexOf(btn)).split('\n').length;
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
          category: this.getCategory(), file_path: filePath, line: ln + 1,
          message: 'Empty button without accessible label',
          remediation: 'Add visible text, aria-label, or title attribute',
          confidence: 0.95,
        });
      }
    });
    return issues;
  }
}

class A11yMissingLangAttribute implements QualityRule {
  getRuleKey() { return 'FE-A11Y-007'; }
  getName() { return 'html element must have lang attribute'; }
  getCategory(): IssueCategory { return 'ACCESSIBILITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/<html/.test(sourceCode) && !/<html[^>]*\slang=/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: '<html> missing lang attribute',
        remediation: 'Add lang attribute: <html lang="en">',
        confidence: 0.95,
      });
    }
    return issues;
  }
}

class A11yNoPositiveTabindex implements QualityRule {
  getRuleKey() { return 'FE-A11Y-008'; }
  getName() { return 'Avoid positive tabindex values'; }
  getCategory(): IssueCategory { return 'ACCESSIBILITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const positiveTabindex = sourceCode.match(/tabindex\s*=\s*["'][1-9]\d*["']/g) || [];
    if (positiveTabindex.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Positive tabindex disrupts natural tab order',
        remediation: 'Use tabindex="0" or tabindex="-1" instead',
        confidence: 0.95,
      });
    }
    return issues;
  }
}

class A11yAnchorHasHref implements QualityRule {
  getRuleKey() { return 'FE-A11Y-009'; }
  getName() { return 'Anchor elements must have href attribute'; }
  getCategory(): IssueCategory { return 'ACCESSIBILITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const anchors = sourceCode.match(/<a\s[^>]*>/g) || [];
    anchors.forEach(a => {
      if (!/href\s*=/.test(a)) {
        const ln = sourceCode.substring(0, sourceCode.indexOf(a)).split('\n').length;
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
          category: this.getCategory(), file_path: filePath, line: ln + 1,
          message: 'Anchor without href is not keyboard accessible',
          remediation: 'Add href or use <button> for click-only actions',
          confidence: 0.95,
        });
      }
    });
    return issues;
  }
}

class A11yFormLabelAssociated implements QualityRule {
  getRuleKey() { return 'FE-A11Y-010'; }
  getName() { return 'Form inputs must have associated labels'; }
  getCategory(): IssueCategory { return 'ACCESSIBILITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const inputs = sourceCode.match(/<(input|select|textarea)\s[^>]*>/g) || [];
    inputs.forEach(input => {
      const hasId = /id\s*=\s*["'](\w+)["']/.exec(input);
      const hasAriaLabel = /aria-label|aria-labelledby|placeholder/.test(input);
      const hasLabel = hasId && new RegExp(`<label[^>]*for\\s*=\\s*["']${hasId[1]}["']`).test(sourceCode);
      if (!hasId && !hasAriaLabel && !/<label[^>]*>[\s\S]*?<\/label>/.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'Form input may not have associated label',
          remediation: 'Add <label for="id"> or aria-label attribute',
          confidence: 0.7,
        });
      }
    });
    return issues;
  }
}
