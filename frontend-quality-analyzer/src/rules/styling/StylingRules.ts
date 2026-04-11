/**
 * Styling quality rules
 * CSS/SCSS/styled-components best practices
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class StylingRules {
  static all(): QualityRule[] {
    return [
      new StyleNoImportant(),
      new StyleInlineStyleAbuse(),
      new StyleNoCssVariable(),
      new StyleDeepNesting(),
      new StyleMagicNumber(),
      new StyleNoResetNormalize(),
      new StyleDuplicateProperties(),
      new StyleHardcodedColor(),
    ];
  }
}

class StyleNoImportant implements QualityRule {
  getRuleKey() { return 'FE-STYLE-001'; }
  getName() { return '!important should not be used'; }
  getCategory(): IssueCategory { return 'STYLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const matches = sourceCode.match(/!important/g) || [];
    if (matches.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${matches.length} use of !important`,
        remediation: 'Improve specificity or refactor CSS architecture',
        confidence: 0.95,
      });
    }
    return issues;
  }
}

class StyleInlineStyleAbuse implements QualityRule {
  getRuleKey() { return 'FE-STYLE-002'; }
  getName() { return 'Excessive inline styles'; }
  getCategory(): IssueCategory { return 'STYLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const inlineStyles = (sourceCode.match(/style\s*=\s*\{/g) || []).length;
    if (inlineStyles > 10) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${inlineStyles} inline style usages`,
        remediation: 'Extract to CSS modules, styled-components, or Tailwind',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class StyleNoCssVariable implements QualityRule {
  getRuleKey() { return 'FE-STYLE-003'; }
  getName() { return 'CSS custom properties should be used for theming'; }
  getCategory(): IssueCategory { return 'STYLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/\.(css|scss|sass|less)$/.test(filePath) && !/--\w+|var\(/.test(sourceCode)) {
      const colorCount = (sourceCode.match(/#[0-9a-fA-F]{3,8}|rgb\(|hsl\(/g) || []).length;
      if (colorCount > 3) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'No CSS variables used, hardcoded values throughout',
          remediation: 'Define CSS custom properties for colors, spacing, etc.',
          confidence: 0.7,
        });
      }
    }
    return issues;
  }
}

class StyleDeepNesting implements QualityRule {
  getRuleKey() { return 'FE-STYLE-004'; }
  getName() { return 'CSS selectors should not be nested too deep'; }
  getCategory(): IssueCategory { return 'STYLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');
    let maxIndent = 0;
    lines.forEach(line => {
      const match = line.match(/^(\s+)/);
      if (match) maxIndent = Math.max(maxIndent, match[1].length);
    });
    const maxNesting = Math.floor(maxIndent / 2);
    if (maxNesting > 4) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `CSS nesting depth: ${maxNesting} levels`,
        remediation: 'Flatten selectors or use BEM methodology',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class StyleMagicNumber implements QualityRule {
  getRuleKey() { return 'FE-STYLE-005'; }
  getName() { return 'Magic numbers in CSS should be avoided'; }
  getCategory(): IssueCategory { return 'STYLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Check for unusual pixel values that are likely not from a design system
    const magicPixels = sourceCode.match(/:\s*\d+px/g) || [];
    const unusual = magicPixels.filter(v => {
      const num = parseInt(v.replace(/:\s*/, '').replace('px', ''));
      return num % 4 !== 0 && num % 5 !== 0; // Not a standard spacing token
    });
    if (unusual.length > 5) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${unusual.length} non-standard pixel values found`,
        remediation: 'Use a design token system (4px or 8px grid)',
        confidence: 0.6,
      });
    }
    return issues;
  }
}

class StyleNoResetNormalize implements QualityRule {
  getRuleKey() { return 'FE-STYLE-006'; }
  getName() { return 'Project should use CSS reset or normalize'; }
  getCategory(): IssueCategory { return 'STYLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/global\.(css|scss)|main\.(css|scss)|index\.(css|scss)/.test(filePath)) {
      if (!/normalize|reset|modern-normalize|sanitize\.css/.test(sourceCode.toLowerCase())) {
        if (!/[*]\s*\{|box-sizing|margin:\s*0|padding:\s*0/.test(sourceCode)) {
          issues.push({
            rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
            category: this.getCategory(), file_path: filePath, line: 1,
            message: 'No CSS reset/normalize detected',
            remediation: 'Import normalize.css or add reset styles',
            confidence: 0.7,
          });
        }
      }
    }
    return issues;
  }
}

class StyleDuplicateProperties implements QualityRule {
  getRuleKey() { return 'FE-STYLE-007'; }
  getName() { return 'Duplicate CSS properties in same rule'; }
  getCategory(): IssueCategory { return 'STYLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Extract CSS blocks and check for duplicate properties
    const blocks = sourceCode.match(/\{([^}]+)\}/g) || [];
    blocks.forEach(block => {
      const props = block.match(/(\w[\w-]*)\s*:/g) || [];
      const counts: Record<string, number> = {};
      props.forEach(p => { const key = p.replace(':', '').trim(); counts[key] = (counts[key] || 0) + 1; });
      const dupes = Object.entries(counts).filter(([_, c]) => c > 1).map(([k]) => k);
      if (dupes.length > 0) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: `Duplicate properties: ${dupes.join(', ')}`,
          remediation: 'Remove duplicate declarations',
          confidence: 0.9,
        });
      }
    });
    return issues;
  }
}

class StyleHardcodedColor implements QualityRule {
  getRuleKey() { return 'FE-STYLE-008'; }
  getName() { return 'Colors should use CSS variables or a theme'; }
  getCategory(): IssueCategory { return 'STYLING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const colorValues = sourceCode.match(/(color|background-color|border-color|fill|stroke)\s*:\s*(#[0-9a-fA-F]{3,8}|rgb\([^)]+\))/g) || [];
    const uniqueColors = new Set(colorValues.map(v => v.split(':').pop()?.trim()));
    if (uniqueColors.size > 5 && !/var\(--/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${uniqueColors.size} unique hardcoded colors`,
        remediation: 'Define a color palette using CSS custom properties',
        confidence: 0.75,
      });
    }
    return issues;
  }
}
