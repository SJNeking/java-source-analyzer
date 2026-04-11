/**
 * TypeScript Best Practices Rules
 * 
 * Detects TypeScript anti-patterns and enforces type safety.
 * Mirrors backend CodeOrganizationRules architecture.
 */

import { QualityIssue, QualityRule, IssueCategory, Severity } from '../../types';

// ==================== Base Class ====================

abstract class AbstractTypeScriptRule implements QualityRule {
  abstract getRuleKey(): string;
  abstract getName(): string;
  
  getCategory(): IssueCategory {
    return 'TYPESCRIPT';
  }

  check(sourceCode: string, filePath: string): QualityIssue[] {
    // Only analyze TypeScript files
    if (!filePath.match(/\.(ts|tsx)$/)) {
      return [];
    }

    return this.checkTypeScript(sourceCode, filePath);
  }

  protected abstract checkTypeScript(sourceCode: string, filePath: string): QualityIssue[];
}

// ==================== Rule Implementations ====================

/**
 * FE-TS-001: No implicit any
 * 
 * Detects variables/parameters without explicit type annotations
 * that would default to 'any'.
 */
export class NoImplicitAnyRule extends AbstractTypeScriptRule {
  getRuleKey(): string { return 'FE-TS-001'; }
  getName(): string { return 'No implicit any types'; }

  /** Parameters that commonly have implicit any by design */
  private readonly SKIP_PARAMS = new Set([
    'event', 'e', 'evt', 'err', 'error', 'cb', 'callback',
    'next', 'resolve', 'reject', 'done', 'acc', 'cur',
    'prev', 'item', 'index', 'i', 'j', 'key', 'value',
    '_', 'ctx', 'req', 'res', 'next',
  ]);

  protected checkTypeScript(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];

    // Skip test files entirely — test utilities often use implicit any intentionally
    if (/\.(test|spec)\.(ts|tsx)$/.test(filePath)) {
      return issues;
    }

    const lines = sourceCode.split('\n');
    let fileIssueCount = 0;
    const MAX_ISSUES_PER_FILE = 5; // Cap per file to avoid noise

    lines.forEach((line, index) => {
      // Stop reporting for this file once we hit the cap
      if (fileIssueCount >= MAX_ISSUES_PER_FILE) {
        return;
      }

      const lineNum = index + 1;

      // Skip comments and strings
      if (line.trim().startsWith('//') || line.trim().startsWith('*')) {
        return;
      }

      // Only flag module-level exported functions/consts (public API surface)
      // Skip class methods, internal functions, and callbacks
      const isModuleLevelExport = /^(export\s+(default\s+)?)?(async\s+)?function\s+\w+\s*\(/.test(line.trim()) ||
                                   /^export\s+const\s+\w+\s*=/.test(line.trim());
      if (!isModuleLevelExport) {
        return;
      }

      const paramPattern = /(?:function\s+\w+|const\s+\w+\s*=\s*(?:async\s*)?)\(([^)]*)\)/g;
      let match;

      while ((match = paramPattern.exec(line)) !== null && fileIssueCount < MAX_ISSUES_PER_FILE) {
        const params = match[1].split(',').map(p => p.trim());

        params.forEach(param => {
          if (fileIssueCount >= MAX_ISSUES_PER_FILE) return;

          // Skip destructured params with defaults, rest params, etc.
          if (!param || param.includes('=') || param.startsWith('...')) {
            return;
          }

          // Skip common callback/parameter names that intentionally omit type
          const paramName = param.split(':')[0].replace(/\s+/g, '');
          if (this.SKIP_PARAMS.has(paramName.toLowerCase())) {
            return;
          }

          // Check if parameter has type annotation
          if (!param.includes(':')) {
            fileIssueCount++;
            issues.push({
              rule_key: this.getRuleKey(),
              rule_name: this.getName(),
              severity: Severity.MAJOR,
              category: this.getCategory(),
              file_path: filePath,
              line: lineNum,
              message: `Parameter '${param}' has implicit 'any' type`,
              evidence: line.trim(),
              remediation: `Add explicit type annotation: ${param}: Type`
            });
          }
        });
      }

      // Detect variable declarations without type annotations
      // Pattern: let/const/var name = value (without : Type)
      const varPattern = /\b(let|const|var)\s+(\w+)\s*=\s*/g;
      while ((match = varPattern.exec(line)) !== null) {
        const varName = match[2];
        
        // Skip if there's a type annotation before =
        const beforeEquals = line.substring(0, match.index + match[0].length);
        if (beforeEquals.includes(':')) {
          continue;
        }

        // Skip obvious type inferences (literals, new expressions)
        const afterEquals = line.substring(match.index + match[0].length).trim();
        if (
          afterEquals.startsWith('"') ||
          afterEquals.startsWith("'") ||
          afterEquals.startsWith('`') ||
          /^\d+$/.test(afterEquals) ||
          afterEquals.startsWith('true') ||
          afterEquals.startsWith('false') ||
          afterEquals.startsWith('null') ||
          afterEquals.startsWith('new ') ||
          afterEquals.startsWith('[') ||
          afterEquals.startsWith('{')
        ) {
          continue;
        }

        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.MINOR,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: `Variable '${varName}' has implicit 'any' type`,
          evidence: line.trim(),
          remediation: `Add explicit type: ${match[1]} ${varName}: Type = ...`
        });
      }
    });

    return issues;
  }
}

/**
 * FE-TS-002: Explicit return type for public functions
 * 
 * Enforces explicit return type annotations on exported/public functions.
 */
export class ExplicitReturnTypeRule extends AbstractTypeScriptRule {
  getRuleKey(): string { return 'FE-TS-002'; }
  getName(): string { return 'Explicit return type required for public functions'; }

  protected checkTypeScript(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Skip non-exported functions
      if (!line.match(/^(export\s+)?(default\s+)?(async\s+)?function\s+/)) {
        return;
      }

      // Check if return type is specified
      // Pattern: function name(...): ReturnType
      if (!line.match(/function\s+\w+\([^)]*\)\s*:/)) {
        const funcMatch = line.match(/function\s+(\w+)/);
        if (funcMatch) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MINOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: `Exported function '${funcMatch[1]}' missing return type annotation`,
            evidence: line.trim(),
            remediation: 'Add return type: function name(...): ReturnType'
          });
        }
      }
    });

    return issues;
  }
}

/**
 * FE-TS-003: Interface naming convention
 * 
 * Enforces PascalCase naming for interfaces and types.
 */
export class InterfaceNamingRule extends AbstractTypeScriptRule {
  getRuleKey(): string { return 'FE-TS-003'; }
  getName(): string { return 'Interface/type names should be PascalCase'; }

  protected checkTypeScript(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Match interface or type declarations
      const interfaceMatch = line.match(/^\s*(export\s+)?(interface|type)\s+(\w+)/);
      if (!interfaceMatch) {
        return;
      }

      const name = interfaceMatch[3];
      
      // Check if name is PascalCase (starts with uppercase, no underscores)
      if (!/^[A-Z][a-zA-Z0-9]*$/.test(name)) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.MINOR,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: `Interface/type '${name}' should be PascalCase`,
          evidence: line.trim(),
          remediation: `Rename to: ${this.toPascalCase(name)}`
        });
      }
    });

    return issues;
  }

  private toPascalCase(name: string): string {
    return name
      .split(/[_-]/)
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join('');
  }
}

/**
 * FE-TS-004: No explicit any usage
 * 
 * Detects explicit use of 'any' type (should use 'unknown' instead).
 */
export class NoExplicitAnyRule extends AbstractTypeScriptRule {
  getRuleKey(): string { return 'FE-TS-004'; }
  getName(): string { return 'Avoid explicit any type, use unknown instead'; }

  protected checkTypeScript(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Skip comments
      if (line.trim().startsWith('//') || line.trim().startsWith('*')) {
        return;
      }

      // Detect explicit 'any' type usage
      // Pattern: : any or Array<any> or Record<string, any>
      const anyPattern = /:\s*any\b|Array<\s*any\s*>|Record<[^,]+,\s*any\s*>/g;
      let match;
      
      while ((match = anyPattern.exec(line)) !== null) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.MAJOR,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'Explicit use of "any" type detected',
          evidence: line.trim(),
          remediation: 'Use "unknown" instead of "any" for better type safety'
        });
      }
    });

    return issues;
  }
}

/**
 * FE-TS-005: Consistent type imports
 * 
 * Enforces using 'import type' for type-only imports.
 */
export class TypeImportConsistencyRule extends AbstractTypeScriptRule {
  getRuleKey(): string { return 'FE-TS-005'; }
  getName(): string { return 'Use import type for type-only imports'; }

  protected checkTypeScript(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Match regular imports (not type imports)
      const importMatch = line.match(/^import\s+{([^}]+)}\s+from\s+['"](.+)['"]/);
      if (!importMatch) {
        return;
      }

      const imports = importMatch[1].split(',').map(i => i.trim());
      const source = importMatch[2];

      // Heuristic: if importing from .types.ts or @types, should use import type
      if (source.includes('.types') || source.includes('@types')) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.INFO,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: `Type-only imports should use 'import type' syntax`,
          evidence: line.trim(),
          remediation: `Change to: import type { ${imports.join(', ')} } from '${source}'`
        });
      }
    });

    return issues;
  }
}

/**
 * FE-TS-006: Enum naming convention
 * 
 * Enforces PascalCase naming for enums.
 */
export class EnumNamingRule extends AbstractTypeScriptRule {
  getRuleKey(): string { return 'FE-TS-006'; }
  getName(): string { return 'Enum names should be PascalCase'; }

  protected checkTypeScript(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      const enumMatch = line.match(/^\s*(export\s+)?enum\s+(\w+)/);
      if (!enumMatch) {
        return;
      }

      const name = enumMatch[2];
      
      if (!/^[A-Z][a-zA-Z0-9]*$/.test(name)) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.MINOR,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: `Enum '${name}' should be PascalCase`,
          evidence: line.trim(),
          remediation: `Rename to: ${this.toPascalCase(name)}`
        });
      }
    });

    return issues;
  }

  private toPascalCase(name: string): string {
    return name
      .split(/[_-]/)
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join('');
  }
}

// Export all rules for easy registration
export const TypeScriptRules = {
  all(): QualityRule[] {
    return [
      new NoImplicitAnyRule(),
      new ExplicitReturnTypeRule(),
      new InterfaceNamingRule(),
      new NoExplicitAnyRule(),
      new TypeImportConsistencyRule(),
      new EnumNamingRule(),
    ];
  },
  NoImplicitAnyRule,
  ExplicitReturnTypeRule,
  InterfaceNamingRule,
  NoExplicitAnyRule,
  TypeImportConsistencyRule,
  EnumNamingRule
};
