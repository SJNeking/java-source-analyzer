/**
 * Frontend Security Rules
 * 
 * Detects security vulnerabilities in frontend code.
 * Mirrors backend OWASP Top 10 and SecurityEnhancedRules architecture.
 */

import { QualityIssue, QualityRule, IssueCategory, Severity } from '../../types';

// ==================== Base Class ====================

abstract class AbstractSecurityRule implements QualityRule {
  abstract getRuleKey(): string;
  abstract getName(): string;
  
  getCategory(): IssueCategory {
    return 'SECURITY';
  }

  check(sourceCode: string, filePath: string): QualityIssue[] {
    return this.checkSecurity(sourceCode, filePath);
  }

  protected abstract checkSecurity(sourceCode: string, filePath: string): QualityIssue[];
}

// ==================== Rule Implementations ====================

/**
 * FE-SEC-001: XSS via dangerouslySetInnerHTML
 * 
 * Detects React's dangerouslySetInnerHTML usage without sanitization.
 */
export class XSSPreventionRule extends AbstractSecurityRule {
  getRuleKey(): string { return 'FE-SEC-001'; }
  getName(): string { return 'Potential XSS vulnerability via dangerouslySetInnerHTML'; }

  protected checkSecurity(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect dangerouslySetInnerHTML
      if (line.includes('dangerouslySetInnerHTML')) {
        // Check if DOMPurify or similar sanitization is used
        const hasSanitization = 
          sourceCode.includes('DOMPurify.sanitize') ||
          sourceCode.includes('sanitize-html') ||
          sourceCode.includes('xss(');

        if (!hasSanitization) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.CRITICAL,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'dangerouslySetInnerHTML used without sanitization',
            evidence: line.trim(),
            remediation: 'Use DOMPurify: dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(html) }}'
          });
        }
      }

      // Detect innerHTML assignment in vanilla JS
      if (line.match(/\.innerHTML\s*=/)) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.MAJOR,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'Direct innerHTML assignment (XSS risk)',
          evidence: line.trim(),
          remediation: 'Use textContent for plain text or sanitize HTML first'
        });
      }

      // Detect eval() usage
      if (line.match(/\beval\s*\(/)) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.CRITICAL,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'eval() usage detected (code injection risk)',
          evidence: line.trim(),
          remediation: 'Avoid eval(), use safer alternatives like JSON.parse()'
        });
      }
    });

    return issues;
  }
}

/**
 * FE-SEC-002: CSRF token missing
 * 
 * Detects POST/PUT/DELETE requests without CSRF tokens.
 */
export class CSRFProtectionRule extends AbstractSecurityRule {
  getRuleKey(): string { return 'FE-SEC-002'; }
  getName(): string { return 'State-changing request may lack CSRF protection'; }

  protected checkSecurity(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect HTTP methods that change state
      const stateChangingMethods = ['post(', 'put(', 'delete(', 'patch('];
      const isStateChanging = stateChangingMethods.some(method => 
        line.toLowerCase().includes(`.${method}`) || 
        line.toLowerCase().includes(`method: '${method.slice(0, -1)}'`)
      );

      if (isStateChanging) {
        // Check for CSRF token
        const hasCsrfToken = 
          sourceCode.includes('csrf-token') ||
          sourceCode.includes('X-CSRF-Token') ||
          sourceCode.includes('X-XSRF-TOKEN') ||
          sourceCode.includes('_token');

        if (!hasCsrfToken) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MAJOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'State-changing request without visible CSRF token',
            evidence: line.trim(),
            remediation: 'Include CSRF token in request headers'
          });
        }
      }
    });

    return issues;
  }
}

/**
 * FE-SEC-003: Sensitive data in localStorage
 * 
 * Detects storage of sensitive data in localStorage/sessionStorage.
 */
export class AuthTokenStorageRule extends AbstractSecurityRule {
  getRuleKey(): string { return 'FE-SEC-003'; }
  getName(): string { return 'Sensitive data stored in insecure location'; }

  protected checkSecurity(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    const sensitivePatterns = [
      /localStorage\.(setItem|getItem)\(['"](password|token|secret|key|auth)/i,
      /sessionStorage\.(setItem|getItem)\(['"](password|token|secret|key|auth)/i,
      /localStorage\.\w+\s*=.*(?:password|token|secret)/i
    ];

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      sensitivePatterns.forEach(pattern => {
        if (pattern.test(line)) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MAJOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'Sensitive data stored in localStorage/sessionStorage',
            evidence: line.trim(),
            remediation: 'Use httpOnly cookies for tokens, or encrypt before storing'
          });
        }
      });
    });

    return issues;
  }
}

/**
 * FE-SEC-004: Insecure postMessage usage
 * 
 * Detects window.postMessage without target origin validation.
 */
export class PostMessageValidationRule extends AbstractSecurityRule {
  getRuleKey(): string { return 'FE-SEC-004'; }
  getName(): string { return 'postMessage without target origin validation'; }

  protected checkSecurity(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect postMessage with wildcard '*'
      if (line.match(/postMessage\s*\([^,]+,\s*['"]\*['"]/)) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.MAJOR,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'postMessage using wildcard "*" as target origin',
          evidence: line.trim(),
          remediation: 'Specify exact target origin: postMessage(data, "https://example.com")'
        });
      }

      // Detect message event listener without origin check
      if (line.includes('addEventListener(\'message\'') || 
          line.includes('addEventListener("message"')) {
        
        // Look for origin check in next 10 lines
        let hasOriginCheck = false;
        for (let i = index; i < Math.min(index + 10, lines.length); i++) {
          if (lines[i].includes('event.origin') || lines[i].includes('e.origin')) {
            hasOriginCheck = true;
            break;
          }
        }

        if (!hasOriginCheck) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MAJOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'Message event listener without origin validation',
            evidence: line.trim(),
            remediation: 'Add origin check: if (event.origin !== "https://trusted.com") return;'
          });
        }
      }
    });

    return issues;
  }
}

/**
 * FE-SEC-005: Hardcoded secrets
 * 
 * Detects hardcoded API keys, passwords, or secrets in source code.
 */
export class HardcodedSecretsRule extends AbstractSecurityRule {
  getRuleKey(): string { return 'FE-SEC-005'; }
  getName(): string { return 'Hardcoded secret or API key detected'; }

  protected checkSecurity(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    const secretPatterns = [
      /(?:api[_-]?key|apikey)\s*[:=]\s*['"][a-zA-Z0-9]{20,}['"]/i,
      /(?:secret|password|token)\s*[:=]\s*['"][^'"]{8,}['"]/i,
      /AWS_ACCESS_KEY_ID\s*[:=]\s*['"][A-Z0-9]{20}['"]/i,
      /AWS_SECRET_ACCESS_KEY\s*[:=]\s*['"][a-zA-Z0-9/+=]{40}['"]/i,
      /(?:private[_-]?key)\s*[:=]\s*['"]-----BEGIN/i
    ];

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Skip comments and test files
      if (line.trim().startsWith('//') || filePath.includes('.test.') || filePath.includes('.spec.')) {
        return;
      }

      secretPatterns.forEach(pattern => {
        if (pattern.test(line)) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.CRITICAL,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'Hardcoded secret or API key found in source code',
            evidence: line.trim().substring(0, 100),
            remediation: 'Use environment variables: process.env.API_KEY'
          });
        }
      });
    });

    return issues;
  }
}

/**
 * FE-SEC-006: Insecure dependencies
 * 
 * Detects usage of known vulnerable patterns (like MD5 for hashing).
 */
export class InsecureCryptoRule extends AbstractSecurityRule {
  getRuleKey(): string { return 'FE-SEC-006'; }
  getName(): string { return 'Use of weak or deprecated cryptographic function'; }

  protected checkSecurity(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    const insecurePatterns = [
      { pattern: /\bMD5\b|\bmd5\b/, replacement: 'SHA-256 or bcrypt' },
      { pattern: /\bSHA1\b|\bsha1\b/, replacement: 'SHA-256 or SHA-3' },
      { pattern: /\bMath\.random\s*\(/, replacement: 'crypto.getRandomValues()' },
      { pattern: /require\s*\(\s*['"]crypto-js['"]\s*\)/, note: 'Verify algorithm strength' }
    ];

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      insecurePatterns.forEach(({ pattern, replacement, note }) => {
        if (pattern.test(line)) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MAJOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: note || `Weak cryptographic function detected`,
            evidence: line.trim(),
            remediation: replacement ? `Use ${replacement} instead` : 'Review cryptographic implementation'
          });
        }
      });
    });

    return issues;
  }
}

// Export all rules for easy registration
export const SecurityRules = {
  XSSPreventionRule,
  CSRFProtectionRule,
  AuthTokenStorageRule,
  PostMessageValidationRule,
  HardcodedSecretsRule,
  InsecureCryptoRule
};
