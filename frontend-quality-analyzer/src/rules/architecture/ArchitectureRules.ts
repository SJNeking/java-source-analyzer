/**
 * Architecture quality rules
 * Detects structural and organizational anti-patterns
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class ArchitectureRules {
  static all(): QualityRule[] {
    return [
      new ArchNoCircularImport(),
      new ArchComponentTooDeep(),
      new ArchPageDirectApiCall(),
      new ArchTooManyProps(),
      new ArchMixedResponsibility(),
      new ArchMagicString(),
      new ArchDeepImportPath(),
      new ArchNoBarrelExport(),
    ];
  }
}

class ArchNoCircularImport implements QualityRule {
  getRuleKey() { return 'FE-ARCH-001'; }
  getName() { return 'Circular imports detected'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Circular imports require graph analysis; this is a placeholder
    // A real implementation would track all imports and detect cycles
    return issues;
  }
}

class ArchComponentTooDeep implements QualityRule {
  getRuleKey() { return 'FE-ARCH-002'; }
  getName() { return 'Component nesting too deep'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Check JSX nesting depth
    const openTags = sourceCode.match(/<\w+/g) || [];
    let maxDepth = 0;
    let currentDepth = 0;
    const closeTags = sourceCode.match(/<\/\w+>/g) || [];
    const allTags = [...openTags.map(t => ({ type: 'open', tag: t })), ...closeTags.map(t => ({ type: 'close', tag: t }))].sort(
      (a, b) => (sourceCode.indexOf(a.tag) - sourceCode.indexOf(b.tag))
    );
    allTags.forEach(t => {
      if (t.type === 'open') { currentDepth++; maxDepth = Math.max(maxDepth, currentDepth); }
      else currentDepth--;
    });
    if (maxDepth > 10) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `Component nesting depth: ${maxDepth} (max recommended: 10)`,
        remediation: 'Extract sub-components to reduce nesting',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class ArchPageDirectApiCall implements QualityRule {
  getRuleKey() { return 'FE-ARCH-003'; }
  getName() { return 'Page components should not call APIs directly'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/(pages|views|screens)\//.test(filePath) &&
        /(fetch|axios|useQuery|useMutation|\.get\(|\.post\()/g.test(sourceCode) &&
        !/(useSelector|useStore|inject|@inject)/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Page component making direct API calls',
        remediation: 'Use a service layer, store, or data fetching hook',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class ArchTooManyProps implements QualityRule {
  getRuleKey() { return 'FE-ARCH-004'; }
  getName() { return 'Component has too many props'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const propsMatch = sourceCode.match(/interface\s+\w*Props\s*\{([^}]+)\}/s);
    if (propsMatch) {
      const propCount = (propsMatch[1].match(/\w+\s*:/g) || []).length;
      if (propCount > 10) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: `Component has ${propCount} props (max recommended: 10)`,
          remediation: 'Consider splitting component or using composition pattern',
          confidence: 0.85,
        });
      }
    }
    return issues;
  }
}

class ArchMixedResponsibility implements QualityRule {
  getRuleKey() { return 'FE-ARCH-005'; }
  getName() { return 'Component mixes concerns (UI + logic + data)'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const hasUI = /<\w+.*>/.test(sourceCode);
    const hasDataFetch = /fetch|axios|useQuery/.test(sourceCode);
    const hasState = /useState|useReducer|ref\s*\(/.test(sourceCode);
    const hasEffects = /useEffect|watch|onMounted/.test(sourceCode);
    if (hasUI && hasDataFetch && hasState && hasEffects) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Component handles UI, data fetching, state, and effects',
        remediation: 'Separate concerns: container/presentational or custom hooks',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class ArchMagicString implements QualityRule {
  getRuleKey() { return 'FE-ARCH-006'; }
  getName() { return 'Magic strings should be extracted to constants'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Check repeated strings
    const stringMatches = sourceCode.match(/['"]{1}[^'"]{3,}['"]{1}/g) || [];
    const counts: Record<string, number> = {};
    stringMatches.forEach(s => { counts[s] = (counts[s] || 0) + 1; });
    const magicStrings = Object.entries(counts).filter(([_, count]) => count >= 3);
    if (magicStrings.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${magicStrings.length} repeated string literals found`,
        remediation: 'Extract to constants or enum file',
        confidence: 0.75,
      });
    }
    return issues;
  }
}

class ArchDeepImportPath implements QualityRule {
  getRuleKey() { return 'FE-ARCH-007'; }
  getName() { return 'Import path too deep (cross-layer coupling)'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const imports = sourceCode.match(/from\s+['"](\..*?)['"]/g) || [];
    imports.forEach(imp => {
      const depth = (imp.match(/\.\.\//g) || []).length;
      if (depth > 4) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: `Deep relative import: ${depth} levels`,
          remediation: 'Use path aliases or absolute imports',
          confidence: 0.9,
        });
      }
    });
    return issues;
  }
}

class ArchNoBarrelExport implements QualityRule {
  getRuleKey() { return 'FE-ARCH-008'; }
  getName() { return 'Consider using barrel exports for module organization'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Only applies to index files
    if (/\/index\.(ts|tsx|js|jsx)$/.test(filePath)) {
      if (!/export\s+\{/.test(sourceCode) && !/export\s+\*/.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'index file without barrel exports',
          remediation: 'Add export { ... } or export * from ... for cleaner imports',
          confidence: 0.8,
        });
      }
    }
    return issues;
  }
}
