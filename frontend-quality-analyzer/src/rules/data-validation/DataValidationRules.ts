/**
 * Data Validation quality rules
 * Detects missing or inadequate data validation patterns
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class DataValidationRules {
  static all(): QualityRule[] {
    return [
      new ValMissingFormValidation(),
      new ValMissingInputSanitization(),
      new ValMissingNullCheck(),
      new ValMissingTypeGuard(),
      new ValMissingSchemaValidation(),
      new ValUnsafeJSONParse(),
    ];
  }
}

class ValMissingFormValidation implements QualityRule {
  getRuleKey() { return 'FE-VAL-001'; }
  getName() { return 'Form submission without validation'; }
  getCategory(): IssueCategory { return 'DATA_VALIDATION'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isComponent = /\.(tsx|jsx|vue)$/.test(filePath);
    if (!isComponent) return issues;
    const hasForm = /<form|onSubmit|handleSubmit/i.test(sourceCode);
    const hasValidation = /required|minLength|maxLength|pattern|validate|yup|zod|joi|valibot/i.test(sourceCode);
    const hasHTML5Validation = /noValidate|validate={false}/i.test(sourceCode);
    if (hasForm && !hasValidation && !hasHTML5Validation) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Form submission without validation allows invalid data',
        remediation: 'Add validation: HTML5 attributes, yup/zod schema, or custom validation',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class ValMissingInputSanitization implements QualityRule {
  getRuleKey() { return 'FE-VAL-002'; }
  getName() { return 'User input used without sanitization'; }
  getCategory(): IssueCategory { return 'DATA_VALIDATION'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // User input directly used in dangerous contexts
    const userInputPatterns = [
      /(?:props|params|query|input|formData)\.\w+.*dangerouslySetInnerHTML/i,
      /(?:props|params|query|input).*\.innerHTML\s*=/i,
      /document\.write\([^)]*(?:props|params|query|input)/i,
    ];
    const hasUnsafeUsage = userInputPatterns.some(pattern => pattern.test(sourceCode));
    const hasSanitization = /DOMPurify|sanitize|escape|encode/i.test(sourceCode);
    if (hasUnsafeUsage && !hasSanitization) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.CRITICAL,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'User input used in DOM without sanitization',
        remediation: 'Sanitize user input with DOMPurify or framework escaping',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class ValMissingNullCheck implements QualityRule {
  getRuleKey() { return 'FE-VAL-003'; }
  getName() { return 'Missing null/undefined check before property access'; }
  getCategory(): IssueCategory { return 'DATA_VALIDATION'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isTypeScript = /\.tsx?$/.test(filePath);
    if (isTypeScript) return issues; // TS catches this at compile time
    // Property access without optional chaining or null check
    const unsafeAccess = /\w+\.\w+\.\w+/g;
    const matches = sourceCode.match(unsafeAccess) || [];
    const hasSafeAccess = /\?\.\w+|if\s*\(\w+\s*[!=]==\s*null|&&\s*\w+\./.test(sourceCode);
    if (matches.length > 5 && !hasSafeAccess) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${matches.length} property access(es) without null checks`,
        remediation: 'Use optional chaining (?.) or nullish coalescing (??)',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class ValMissingTypeGuard implements QualityRule {
  getRuleKey() { return 'FE-VAL-004'; }
  getName() { return 'Missing TypeScript type guard for runtime validation'; }
  getCategory(): IssueCategory { return 'DATA_VALIDATION'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isTypeScript = /\.tsx?$/.test(filePath);
    if (!isTypeScript) return issues;
    // Type assertion without runtime check
    const hasTypeAssertion = /as\s+\w+|<\w+>[\s\S]*(?:JSON\.parse|fetch|response)/g.test(sourceCode);
    const hasTypeGuard = /is\s+\w+|type\s+guard|instanceof|typeof/.test(sourceCode);
    const hasExternalData = /fetch\(|axios\.|API|response|data\s*from/i.test(sourceCode);
    if (hasExternalData && hasTypeAssertion && !hasTypeGuard) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'External data type assertion without runtime validation',
        remediation: 'Use runtime type checking: zod, io-ts, or custom type guards',
        confidence: 0.75,
      });
    }
    return issues;
  }
}

class ValMissingSchemaValidation implements QualityRule {
  getRuleKey() { return 'FE-VAL-005'; }
  getName() { return 'Complex form without schema validation'; }
  getCategory(): IssueCategory { return 'DATA_VALIDATION'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const hasComplexForm = /useState.*form|useForm|formData|formState/i.test(sourceCode);
    const fieldCount = (sourceCode.match(/name=["'][^"']*["']|label=["'][^"']*["']/g) || []).length;
    const hasSchemaValidation = /yup|zod|valibot|joi|schema|validationSchema/i.test(sourceCode);
    if (hasComplexForm && fieldCount > 5 && !hasSchemaValidation) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `Complex form (${fieldCount} fields) without schema validation`,
        remediation: 'Use zod, yup, or valibot for declarative schema validation',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class ValUnsafeJSONParse implements QualityRule {
  getRuleKey() { return 'FE-VAL-006'; }
  getName() { return 'Unsafe JSON.parse without error handling'; }
  getCategory(): IssueCategory { return 'DATA_VALIDATION'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // JSON.parse without try/catch
    const jsonParseCount = (sourceCode.match(/JSON\.parse\(/g) || []).length;
    const hasTryCatch = /try\s*\{[\s\S]*JSON\.parse/g.test(sourceCode);
    const hasSafeParse = /\.match(/g.test(sourceCode) || /safeJsonParse/gi.test(sourceCode);
    if (jsonParseCount > 0 && !hasTryCatch && !hasSafeParse) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${jsonParseCount} JSON.parse() without error handling`,
        remediation: 'Wrap JSON.parse in try/catch or use safe parsing libraries',
        confidence: 0.85,
      });
    }
    return issues;
  }
}
