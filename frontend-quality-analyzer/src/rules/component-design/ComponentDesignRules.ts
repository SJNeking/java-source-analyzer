/**
 * Component Design Patterns quality rules
 * Detects component design anti-patterns
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class ComponentDesignRules {
  static all(): QualityRule[] {
    return [
      new CompMissingForwardRef(),
      new CompMissingDisplayName(),
      new CompControlledUncontrolledMismatch(),
      new CompMissingChildrenType(),
      new CompBooleanPropsHell(),
      new CompMissingComponentComposition(),
    ];
  }
}

class CompMissingForwardRef implements QualityRule {
  getRuleKey() { return 'FE-DESIGN-001'; }
  getName() { return 'Component accepts ref but missing forwardRef'; }
  getCategory(): IssueCategory { return 'COMPONENT_DESIGN'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isReact = /\.tsx?$/.test(filePath);
    if (!isReact) return issues;
    // Component with ref prop but not using forwardRef
    const hasRefProp = /ref\s*[:=]|React\.Ref|ref:/.test(sourceCode);
    const usesForwardRef = /React\.forwardRef|forwardRef\(/.test(sourceCode);
    if (hasRefProp && !usesForwardRef) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Component accepts ref but does not use React.forwardRef',
        remediation: 'Wrap component with React.forwardRef to properly forward refs',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class CompMissingDisplayName implements QualityRule {
  getRuleKey() { return 'FE-DESIGN-002'; }
  getName() { return 'Anonymous component missing displayName'; }
  getCategory(): IssueCategory { return 'COMPONENT_DESIGN'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isReact = /\.tsx?$/.test(filePath);
    if (!isReact) return issues;
    // HOC or memo without displayName
    const hasHOC = /React\.memo\(|React\.forwardRef\(|connect\(|withRouter\(/.test(sourceCode);
    const hasDisplayName = /\.displayName\s*=/i.test(sourceCode);
    const isAnonymous = /export\s+default\s+(?:React\.memo|React\.forwardRef|connect)/.test(sourceCode);
    if (hasHOC && !hasDisplayName && isAnonymous) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Wrapped component missing displayName for React DevTools',
        remediation: 'Add Component.displayName = "ComponentName" for better debugging',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class CompControlledUncontrolledMismatch implements QualityRule {
  getRuleKey() { return 'FE-DESIGN-003'; }
  getName() { return 'Component mixes controlled and uncontrolled props'; }
  getCategory(): IssueCategory { return 'COMPONENT_DESIGN'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isComponent = /\.(tsx|jsx|vue)$/.test(filePath);
    if (!isComponent) return issues;
    // Using both value and defaultValue
    const hasValue = /value\s*=\{|defaultValue\s*=/g.test(sourceCode);
    const hasBoth = /value\s*=/.test(sourceCode) && /defaultValue\s*=/.test(sourceCode);
    const hasChecked = /checked\s*=/.test(sourceCode) && /defaultChecked\s*=/.test(sourceCode);
    if (hasBoth || hasChecked) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Component switches between controlled and uncontrolled mode',
        remediation: 'Use either controlled (value + onChange) or uncontrolled (defaultValue), not both',
        confidence: 0.9,
      });
    }
    return issues;
  }
}

class CompMissingChildrenType implements QualityRule {
  getRuleKey() { return 'FE-DESIGN-004'; }
  getName() { return 'Component children prop missing TypeScript type'; }
  getCategory(): IssueCategory { return 'COMPONENT_DESIGN'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isTypeScript = /\.tsx?$/.test(filePath);
    if (!isTypeScript) return issues;
    // Component with children but not typed
    const hasChildren = /children\s*[:?]|{ children }|props\.children/.test(sourceCode);
    const hasType = /ReactNode|React\.ReactNode|JSX\.Element|VNode/.test(sourceCode);
    if (hasChildren && !hasType) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Children prop not typed with ReactNode',
        remediation: 'Type children: { children?: React.ReactNode }',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class CompBooleanPropsHell implements QualityRule {
  getRuleKey() { return 'FE-DESIGN-005'; }
  getName() { return 'Component has too many boolean props (API complexity)'; }
  getCategory(): IssueCategory { return 'COMPONENT_DESIGN'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Component with many isXxx, hasXxx, showXxx props
    const booleanProps = /is[A-Z]\w+\??\s*:\s*boolean|has[A-Z]\w+\??\s*:\s*boolean|show[A-Z]\w+\??\s*:\s*boolean/g;
    const matches = sourceCode.match(booleanProps) || [];
    if (matches.length >= 4) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${matches.length} boolean props make API complex`,
        remediation: 'Use composition pattern or variant props instead of boolean flags',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class CompMissingComponentComposition implements QualityRule {
  getRuleKey() { return 'FE-DESIGN-006'; }
  getName() { return 'Large component should use composition pattern'; }
  getCategory(): IssueCategory { return 'COMPONENT_DESIGN'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isComponent = /\.(tsx|jsx|vue)$/.test(filePath);
    if (!isComponent) return issues;
    const lines = sourceCode.split('\n').length;
    const hasMultipleSections = [
      /header|navigation/i,
      /sidebar|aside/i,
      /content|main/i,
      /footer|action/i,
    ].filter(section => section.test(sourceCode)).length;
    if (lines > 200 && hasMultipleSections >= 3) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `Component (${lines} lines) with ${hasMultipleSections} sections needs composition`,
        remediation: 'Split into sub-components: Header, Sidebar, Content, Footer',
        confidence: 0.75,
      });
    }
    return issues;
  }
}
