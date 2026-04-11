/**
 * Testing quality rules
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class TestingRules {
  static all(): QualityRule[] {
    return [
      new TestNoTestFile(),
      new TestExcessiveMocking(),
      new TestNoAssertion(),
      new TestSnapshotMisuse(),
      new TestDescribeMissing(),
      new TestOnlyLeftBehind(),
    ];
  }
}

class TestNoTestFile implements QualityRule {
  getRuleKey() { return 'FE-TEST-001'; }
  getName() { return 'Component/module has no test file'; }
  getCategory(): IssueCategory { return 'TESTING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // This rule is checked at project level, not per-file
    return issues;
  }
}

class TestExcessiveMocking implements QualityRule {
  getRuleKey() { return 'FE-TEST-002'; }
  getName() { return 'Test may have excessive mocking'; }
  getCategory(): IssueCategory { return 'TESTING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/\.(test|spec)\.(ts|tsx|js|jsx)$/.test(filePath)) {
      const mockCount = (sourceCode.match(/jest\.mock|vi\.mock|\.mockImplementation|\.mockReturnValue/g) || []).length;
      if (mockCount > 5) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: `${mockCount} mocks in test file`,
          remediation: 'Consider integration tests or reduce mock count',
          confidence: 0.7,
        });
      }
    }
    return issues;
  }
}

class TestNoAssertion implements QualityRule {
  getRuleKey() { return 'FE-TEST-003'; }
  getName() { return 'Test case has no assertions'; }
  getCategory(): IssueCategory { return 'TESTING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/\.(test|spec)\.(ts|tsx|js|jsx)$/.test(filePath)) {
      const testBlocks = sourceCode.match(/(it|test)\s*\(/g) || [];
      const assertions = sourceCode.match(/expect\(|assert\(|\.toBe\(|\.toEqual\(|\.toMatch\(/g) || [];
      if (testBlocks.length > assertions.length) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: `${testBlocks.length - assertions.length} test(s) without assertions`,
          remediation: 'Add assertions to verify expected behavior',
          confidence: 0.8,
        });
      }
    }
    return issues;
  }
}

class TestSnapshotMisuse implements QualityRule {
  getRuleKey() { return 'FE-TEST-004'; }
  getName() { return 'Over-reliance on snapshot testing'; }
  getCategory(): IssueCategory { return 'TESTING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/\.(test|spec)\.(ts|tsx|js|jsx)$/.test(filePath)) {
      const snapshots = (sourceCode.match(/\.toMatchSnapshot|\.toMatchInlineSnapshot/g) || []).length;
      const tests = (sourceCode.match(/(it|test)\s*\(/g) || []).length;
      if (tests > 0 && snapshots / tests > 0.8) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'Most tests use snapshots without behavioral assertions',
          remediation: 'Add explicit assertions for critical behavior',
          confidence: 0.75,
        });
      }
    }
    return issues;
  }
}

class TestDescribeMissing implements QualityRule {
  getRuleKey() { return 'FE-TEST-005'; }
  getName() { return 'Test file missing describe block for organization'; }
  getCategory(): IssueCategory { return 'TESTING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/\.(test|spec)\.(ts|tsx|js|jsx)$/.test(filePath)) {
      const tests = (sourceCode.match(/(it|test)\s*\(/g) || []).length;
      if (tests > 3 && !/describe\s*\(/.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'Test file has multiple tests without describe grouping',
          remediation: 'Use describe() blocks to organize related tests',
          confidence: 0.85,
        });
      }
    }
    return issues;
  }
}

class TestOnlyLeftBehind implements QualityRule {
  getRuleKey() { return 'FE-TEST-006'; }
  getName() { return 'test.only/it.only should not be committed'; }
  getCategory(): IssueCategory { return 'TESTING'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    if (/\.(test|spec)\.(ts|tsx|js|jsx)$/.test(filePath)) {
      const onlyMatches = sourceCode.match(/(it|test|describe)\.only\s*\(/g) || [];
      if (onlyMatches.length > 0) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'test.only left behind — only runs this test',
          remediation: 'Remove .only before committing',
          confidence: 0.95,
        });
      }
    }
    return issues;
  }
}
