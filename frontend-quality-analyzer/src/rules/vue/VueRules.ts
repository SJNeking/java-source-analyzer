/**
 * Vue-specific quality rules
 * Detects common Vue anti-patterns and best practice violations
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class VueRules {
  static all(): QualityRule[] {
    return [
      new VueNoVForWithKey(),
      new VueNoComputedSideEffects(),
      new VueMissingPropsValidation(),
      new VueNoVIfWithVFor(),
      new VueMissingKeyInVFor(),
      new VueNoDirectDomAccess(),
      new VueReactiveStateMutation(),
      new VueMissingComponentName(),
      new VueNoInlineTemplate(),
      new VueNoOverlyComplexComputed(),
    ];
  }
}

class VueNoVForWithKey implements QualityRule {
  getRuleKey() { return 'FE-VUE-001'; }
  getName() { return 'v-for should not be used with v-if on same element'; }
  getCategory(): IssueCategory { return 'VUE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/<\w+[^>]*v-for\s*=.*v-if\s*=/.test(sourceCode) || /<\w+[^>]*v-if\s*=.*v-for\s*=/.test(sourceCode)) {
      const line = (sourceCode.match(/v-for.*v-if|v-if.*v-for/)?.index || 0);
      const lineNum = sourceCode.substring(0, line).split('\n').length;
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: lineNum,
        message: 'v-for and v-if should not be used on the same element',
        remediation: 'Use computed property to filter before rendering', confidence: 0.95,
      });
    }
    return issues;
  }
}

class VueNoComputedSideEffects implements QualityRule {
  getRuleKey() { return 'FE-VUE-002'; }
  getName() { return 'Computed properties should not have side effects'; }
  getCategory(): IssueCategory { return 'VUE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const computedMatch = sourceCode.match(/computed\s*:\s*\{([\s\S]*?)\}/);
    if (computedMatch) {
      const body = computedMatch[1];
      if (/this\.\w+\s*=|\.push\(|\.splice\(|console\./.test(body)) {
        const line = (computedMatch.index || 0);
        const lineNum = sourceCode.substring(0, line).split('\n').length;
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
          category: this.getCategory(), file_path: filePath, line: lineNum + 1,
          message: 'Computed property contains side effects',
          remediation: 'Move side effects to methods or watchers', confidence: 0.85,
        });
      }
    }
    return issues;
  }
}

class VueMissingPropsValidation implements QualityRule {
  getRuleKey() { return 'FE-VUE-003'; }
  getName() { return 'Props should have validation (type, required, default)'; }
  getCategory(): IssueCategory { return 'VUE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const propsMatch = sourceCode.match(/props\s*:\s*\{([\s\S]*?)\}/);
    if (propsMatch) {
      const propsBody = propsMatch[1];
      // Simple string props without validation
      const simpleProps = propsBody.match(/\w+\s*:/g) || [];
      simpleProps.forEach(prop => {
        const propName = prop.replace(':', '').trim();
        const propBlock = propsBody.substring(propsBody.indexOf(prop));
        const endIdx = propBlock.indexOf(',') > 0 ? propBlock.indexOf(',') : propBlock.length;
        const propDef = propBlock.substring(0, endIdx);
        if (/^\w+\s*:\s*['"]/.test(propDef) || /^\w+\s*:/.test(propDef) && !/type|required|default/.test(propDef)) {
          const line = (propsMatch.index || 0) + sourceCode.substring(propsMatch.index || 0).indexOf(prop);
          const lineNum = sourceCode.substring(0, line).split('\n').length;
          issues.push({
            rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
            category: this.getCategory(), file_path: filePath, line: lineNum,
            message: `Prop '${propName}' lacks type validation`,
            remediation: 'Define props with type, required, and default', confidence: 0.8,
          });
        }
      });
    }
    return issues;
  }
}

class VueMissingKeyInVFor implements QualityRule {
  getRuleKey() { return 'FE-VUE-004'; }
  getName() { return 'v-for should have a :key attribute'; }
  getCategory(): IssueCategory { return 'VUE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const vforMatches = sourceCode.match(/v-for\s*=/g);
    if (vforMatches) {
      const lines = sourceCode.split('\n');
      lines.forEach((line, idx) => {
        if (/v-for\s*=/.test(line) && !/:key\s*=/.test(line)) {
          issues.push({
            rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
            category: this.getCategory(), file_path: filePath, line: idx + 1,
            message: 'v-for without :key can cause rendering issues',
            remediation: 'Add :key with unique identifier', confidence: 0.95,
          });
        }
      });
    }
    return issues;
  }
}

class VueNoDirectDomAccess implements QualityRule {
  getRuleKey() { return 'FE-VUE-005'; }
  getName() { return 'Avoid direct DOM manipulation in Vue components'; }
  getCategory(): IssueCategory { return 'VUE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/document\.querySelector|document\.getElementById|element\.innerHTML/.test(sourceCode) &&
        !/ref\s*=|this\.\$refs/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Direct DOM manipulation detected, use refs instead',
        remediation: 'Use Vue refs or $refs for DOM access', confidence: 0.7,
      });
    }
    return issues;
  }
}

class VueReactiveStateMutation implements QualityRule {
  getRuleKey() { return 'FE-VUE-006'; }
  getName() { return 'Vuex/Pinia state should not be mutated directly'; }
  getCategory(): IssueCategory { return 'VUE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/(store\.state|this\.\$store\.state)\.\w+\s*=/.test(sourceCode) && !/mutation|commit|dispatch/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Direct state mutation detected',
        remediation: 'Use mutations or actions to modify state', confidence: 0.85,
      });
    }
    return issues;
  }
}

class VueMissingComponentName implements QualityRule {
  getRuleKey() { return 'FE-VUE-007'; }
  getName() { return 'Vue component should have a name property'; }
  getCategory(): IssueCategory { return 'VUE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/export\s+default\s*\{/.test(sourceCode) && !/name\s*:/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Component missing name property',
        remediation: 'Add name property for devtools and debugging', confidence: 0.9,
      });
    }
    return issues;
  }
}

class VueNoInlineTemplate implements QualityRule {
  getRuleKey() { return 'FE-VUE-008'; }
  getName() { return 'Avoid inline template strings'; }
  getCategory(): IssueCategory { return 'VUE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/template\s*:\s*`/.test(sourceCode) || /template\s*:\s*"/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Inline template string detected',
        remediation: 'Use .vue SFC files or separate template files', confidence: 0.9,
      });
    }
    return issues;
  }
}

class VueNoOverlyComplexComputed implements QualityRule {
  getRuleKey() { return 'FE-VUE-009'; }
  getName() { return 'Computed properties should not be overly complex'; }
  getCategory(): IssueCategory { return 'VUE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const computedMatch = sourceCode.match(/computed\s*:\s*\{([\s\S]*?)\}/);
    if (computedMatch) {
      const body = computedMatch[1];
      const lines = body.split('\n').length;
      if (lines > 30) {
        const lineNum = (computedMatch.index || 0);
        const ln = sourceCode.substring(0, lineNum).split('\n').length;
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
          category: this.getCategory(), file_path: filePath, line: ln + 1,
          message: `Computed property is ${lines} lines (max recommended: 30)`,
          remediation: 'Extract complex logic into separate methods', confidence: 0.8,
        });
      }
    }
    return issues;
  }
}

class VueNoVIfWithVFor implements QualityRule {
  getRuleKey() { return 'FE-VUE-010'; }
  getName() { return 'Use wrapper element for v-if with v-for'; }
  getCategory(): IssueCategory { return 'VUE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Already covered by FE-VUE-001, this is an alternative pattern
    return issues;
  }
}
