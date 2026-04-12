/**
 * Extended Security quality rules
 * Detects security vulnerabilities beyond basic XSS/CSRF
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class ExtendedSecurityRules {
  static all(): QualityRule[] {
    return [
      new SecOpenRedirect(),
      new SecInsecureDependency(),
      new SecCookieInsecure(),
      new SecFormInsecure(),
      new SecURLConstructorUnsafe(),
      new SecMissingCSP(),
      new SecSensitiveDataInStorage(),
      new SecInsecureCommunication(),
      new SecDOMBasedXSS(),
      new SecEvalUsage(),
    ];
  }
}

class SecOpenRedirect implements QualityRule {
  getRuleKey() { return 'FE-SEC-007'; }
  getName() { return 'Potential open redirect vulnerability'; }
  getCategory(): IssueCategory { return 'SECURITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // window.location.href = userInput
    const redirectPatterns = [
      /window\.location\.(?:href|assign|replace)\s*=\s*(?:props|state|params|query|search)/i,
      /location\.href\s*=\s*[^'"]/i,
      /router\.push\([^)]*\)/i,
    ];
    const hasUnsafeRedirect = redirectPatterns.some(pattern => pattern.test(sourceCode));
    const hasUserInput = /props\.|params\.|query\.|search\.|getParams/i.test(sourceCode);
    if (hasUnsafeRedirect && hasUserInput) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.CRITICAL,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Potential open redirect with user-controlled input',
        remediation: 'Validate and whitelist redirect URLs before navigation',
        confidence: 0.75,
      });
    }
    return issues;
  }
}

class SecInsecureDependency implements QualityRule {
  getRuleKey() { return 'FE-SEC-008'; }
  getName() { return 'Insecure script dependency loading'; }
  getCategory(): IssueCategory { return 'SECURITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Script tags without integrity attribute
    const scriptTags = /<script\s+src=["']https?:\/\/[^"']+["']/gi;
    const scripts = sourceCode.match(scriptTags) || [];
    const hasIntegrity = /integrity=["']sha/gi;
    const scriptsWithoutIntegrity = scripts.filter(script => !hasIntegrity.test(script));
    if (scriptsWithoutIntegrity.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${scriptsWithoutIntegrity.length} external script(s) without SRI (Subresource Integrity)`,
        remediation: 'Add integrity="sha384-..." and crossorigin="anonymous" to script tags',
        confidence: 0.9,
      });
    }
    return issues;
  }
}

class SecCookieInsecure implements QualityRule {
  getRuleKey() { return 'FE-SEC-009'; }
  getName() { return 'Insecure cookie configuration'; }
  getCategory(): IssueCategory { return 'SECURITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // document.cookie without secure flags
    const cookieSet = /document\.cookie\s*=\s*[^;]+/g;
    const cookies = sourceCode.match(cookieSet) || [];
    const missingSecure = cookies.filter(cookie => 
      !/Secure/i.test(cookie) && !/secure=true/i.test(cookie)
    );
    const missingHttpOnly = cookies.filter(cookie =>
      !/HttpOnly|httponly/i.test(cookie)
    );
    const missingSameSite = cookies.filter(cookie =>
      !/SameSite/i.test(cookie)
    );
    if (missingSecure.length > 0 || missingHttpOnly.length > 0 || missingSameSite.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `Cookie(s) missing security flags: Secure(${missingSecure.length}), HttpOnly(${missingHttpOnly.length}), SameSite(${missingSameSite.length})`,
        remediation: 'Set Secure, HttpOnly, and SameSite=Strict flags on cookies',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class SecFormInsecure implements QualityRule {
  getRuleKey() { return 'FE-SEC-010'; }
  getName() { return 'Form submits to non-HTTPS URL'; }
  getCategory(): IssueCategory { return 'SECURITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Form with http:// action
    const insecureForm = /<form[^>]+action=["']http:\/\/[^"']+["']/i;
    if (insecureForm.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.CRITICAL,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Form submits sensitive data over HTTP',
        remediation: 'Use HTTPS for all form submissions to protect user data',
        confidence: 0.95,
      });
    }
    return issues;
  }
}

class SecURLConstructorUnsafe implements QualityRule {
  getRuleKey() { return 'FE-SEC-011'; }
  getName() { return 'Unsafe URL construction with user input'; }
  getCategory(): IssueCategory { return 'SECURITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // new URL(userInput) without validation
    const urlConstructor = /new\s+URL\((?:props|params|query|input|user)/i;
    const hasUserInput = /props\.|params\.|query\.|input|userInput|searchParams/i.test(sourceCode);
    if (urlConstructor.test(sourceCode) && hasUserInput) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'URL constructed with potentially unsafe user input',
        remediation: 'Validate and sanitize user input before URL construction',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class SecMissingCSP implements QualityRule {
  getRuleKey() { return 'FE-SEC-012'; }
  getName() { return 'Missing Content Security Policy meta tag'; }
  getCategory(): IssueCategory { return 'SECURITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // HTML files without CSP meta tag
    const isHTML = /\.html?$/.test(filePath);
    if (!isHTML) return issues;
    const hasCSP = /<meta[^>]+http-equiv=["']Content-Security-Policy["']/i;
    if (!hasCSP.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'No Content Security Policy meta tag found',
        remediation: 'Add CSP meta tag: <meta http-equiv="Content-Security-Policy" content="...">',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class SecSensitiveDataInStorage implements QualityRule {
  getRuleKey() { return 'FE-SEC-013'; }
  getName() { return 'Sensitive data in localStorage/sessionStorage'; }
  getCategory(): IssueCategory { return 'SECURITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // localStorage.setItem with sensitive keys
    const sensitiveStorage = /localStorage|sessionStorage/g;
    const storageMatches = sourceCode.match(sensitiveStorage) || [];
    const hasSensitiveKeys = /password|token|secret|apiKey|api_key|creditCard|ssn/i.test(sourceCode);
    if (storageMatches.length > 0 && hasSensitiveKeys) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.CRITICAL,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Sensitive data stored in localStorage/sessionStorage (accessible by XSS)',
        remediation: 'Use httpOnly cookies or secure memory storage for sensitive data',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class SecInsecureCommunication implements QualityRule {
  getRuleKey() { return 'FE-SEC-014'; }
  getName() { return 'Insecure communication (HTTP/Mixed Content)'; }
  getCategory(): IssueCategory { return 'SECURITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // HTTP URLs (not localhost)
    const httpUrls = /https?:\/\/(?!localhost|127\.0\.0\.1)[^"'`\s]+/gi;
    const matches = sourceCode.match(httpUrls) || [];
    const httpOnly = matches.filter(url => url.startsWith('http://'));
    if (httpOnly.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${httpOnly.length} resource(s) loaded over insecure HTTP`,
        remediation: 'Use HTTPS for all network requests to prevent MITM attacks',
        confidence: 0.9,
      });
    }
    return issues;
  }
}

class SecDOMBasedXSS implements QualityRule {
  getRuleKey() { return 'FE-SEC-015'; }
  getName() { return 'Potential DOM-based XSS vulnerability'; }
  getCategory(): IssueCategory { return 'SECURITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // innerHTML, outerHTML with user input
    const domXSS = [
      /\.innerHTML\s*=/,
      /\.outerHTML\s*=/,
      /document\.write\(/,
      /element\.insertAdjacentHTML\(/,
    ];
    const hasDOMWrite = domXSS.some(pattern => pattern.test(sourceCode));
    const hasUserInput = /location\.|params|query|props\.|data\.|response/i.test(sourceCode);
    if (hasDOMWrite && hasUserInput) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.CRITICAL,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'DOM-based XSS: user input written to DOM without sanitization',
        remediation: 'Use DOMPurify or framework built-in escaping before innerHTML',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class SecEvalUsage implements QualityRule {
  getRuleKey() { return 'FE-SEC-016'; }
  getName() { return 'Use of eval() or equivalent'; }
  getCategory(): IssueCategory { return 'SECURITY'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // eval, new Function, setTimeout with string
    const evalPatterns = [
      /\beval\s*\(/,
      /new\s+Function\s*\(/,
      /setTimeout\s*\(\s*['"`]/,
      /setInterval\s*\(\s*['"`]/,
    ];
    const matches = evalPatterns.filter(pattern => pattern.test(sourceCode));
    if (matches.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.CRITICAL,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Use of eval or equivalent code execution (code injection risk)',
        remediation: 'Avoid eval(). Use JSON.parse(), dynamic imports, or function references',
        confidence: 0.95,
      });
    }
    return issues;
  }
}
