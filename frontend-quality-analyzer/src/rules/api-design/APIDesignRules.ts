/**
 * API Design quality rules
 * Detects API/client communication anti-patterns
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class APIDesignRules {
  static all(): QualityRule[] {
    return [
      new APIMissingTimeout(),
      new APINoRetryLogic(),
      new APIMissingCancellation(),
      new APIResponseNotTyped(),
      new APINoErrorStandardization(),
      new APIMissingPagination(),
      new APIHardcodedHeaders(),
      new APINoRequestInterceptor(),
    ];
  }
}

class APIMissingTimeout implements QualityRule {
  getRuleKey() { return 'FE-API-001'; }
  getName() { return 'API request without timeout'; }
  getCategory(): IssueCategory { return 'API_DESIGN'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // fetch or axios without timeout
    const hasFetch = /fetch\(|axios\.(get|post|put|delete|patch)/.test(sourceCode);
    const hasTimeout = /timeout|AbortController|signal|cancelToken/i.test(sourceCode);
    if (hasFetch && !hasTimeout) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'API request without timeout may hang indefinitely',
        remediation: 'Add timeout with AbortController (fetch) or timeout config (axios)',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class APINoRetryLogic implements QualityRule {
  getRuleKey() { return 'FE-API-002'; }
  getName() { return 'API request without retry logic'; }
  getCategory(): IssueCategory { return 'API_DESIGN'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const hasFetch = /fetch\(|axios\.(get|post|put|delete)/.test(sourceCode);
    const hasRetry = /retry|exponential.*backoff|retryCount|retryDelay/i.test(sourceCode);
    const isMutation = /\.post\(|\.put\(|\.delete\(/.test(sourceCode);
    // GET requests should retry, mutations should not
    if (hasFetch && !hasRetry && !isMutation) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'GET request without retry logic (network failures not recovered)',
        remediation: 'Implement retry with exponential backoff for resilient network requests',
        confidence: 0.75,
      });
    }
    return issues;
  }
}

class APIMissingCancellation implements QualityRule {
  getRuleKey() { return 'FE-API-003'; }
  getName() { return 'API request without cancellation on unmount'; }
  getCategory(): IssueCategory { return 'API_DESIGN'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isComponent = /\.(tsx|jsx|vue)$/.test(filePath);
    if (!isComponent) return issues;
    const hasFetch = /useEffect.*fetch|useEffect.*axios/s.test(sourceCode);
    const hasCancellation = /AbortController|cancelToken|return\s*\(\)\s*=>.*abort/s.test(sourceCode);
    if (hasFetch && !hasCancellation) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'In-flight request not cancelled on unmount',
        remediation: 'Use AbortController to cancel requests in useEffect cleanup',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class APIResponseNotTyped implements QualityRule {
  getRuleKey() { return 'FE-API-004'; }
  getName() { return 'API response not typed with TypeScript interface'; }
  getCategory(): IssueCategory { return 'API_DESIGN'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isTypeScript = /\.tsx?$/.test(filePath);
    if (!isTypeScript) return issues;
    const hasFetch = /fetch\([^)]+\)\.then\([^)]*\.json\(\)/.test(sourceCode);
    const hasTyping = /interface\s+\w*(?:Response|Data|Payload)|type\s+\w*(?:Response|Data|Payload)/i.test(sourceCode);
    const hasGenericTyping = /\.then\(\s*\(res\)\s*=>\s*res\.json\(\)\s*\)/.test(sourceCode);
    if (hasFetch && !hasTyping && hasGenericTyping) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'API response not typed (using implicit any)',
        remediation: 'Define TypeScript interface for API response: fetch<T>(url)',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class APINoErrorStandardization implements QualityRule {
  getRuleKey() { return 'FE-API-005'; }
  getName() { return 'API error handling not standardized'; }
  getCategory(): IssueCategory { return 'API_DESIGN'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const hasMultipleFetchPatterns = [
      /fetch\(/,
      /axios\./,
      /useQuery|useMutation/,
      /api\.(get|post|put|delete)/,
    ].filter(pattern => pattern.test(sourceCode)).length;
    const hasStandardError = /ApiError|BaseError|HttpError|errorHandler/i.test(sourceCode);
    if (hasMultipleFetchPatterns > 1 && !hasStandardError) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Multiple API fetching methods without standardized error handling',
        remediation: 'Create unified API client with standardized error handling',
        confidence: 0.75,
      });
    }
    return issues;
  }
}

class APIMissingPagination implements QualityRule {
  getRuleKey() { return 'FE-API-006'; }
  getName() { return 'List API request without pagination'; }
  getCategory(): IssueCategory { return 'API_DESIGN'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const hasListFetch = /fetch\([^)]*(?:list|items|users|products|records)/i.test(sourceCode);
    const hasPagination = /page|limit|offset|cursor|pageSize|pageNumber/i.test(sourceCode);
    const hasUseInfiniteQuery = /useInfiniteQuery/.test(sourceCode);
    if (hasListFetch && !hasPagination && !hasUseInfiniteQuery) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'List API without pagination may fetch excessive data',
        remediation: 'Add pagination params or use useInfiniteQuery for large datasets',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class APIHardcodedHeaders implements QualityRule {
  getRuleKey() { return 'FE-API-007'; }
  getName() { return 'Hardcoded API headers'; }
  getCategory(): IssueCategory { return 'API_DESIGN'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const hasHardcodedHeaders = /headers\s*:\s*\{[^}]*(?:Authorization|Content-Type|Accept)[^}]*['"][^'"]+['"]/g;
    const matches = sourceCode.match(hasHardcodedHeaders) || [];
    const hasInterceptor = /interceptor|axios\.defaults|createAxiosWithConfig/i.test(sourceCode);
    if (matches.length > 0 && !hasInterceptor) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${matches.length} API call(s) with hardcoded headers`,
        remediation: 'Use request interceptor or axios defaults for common headers',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class APINoRequestInterceptor implements QualityRule {
  getRuleKey() { return 'FE-API-008'; }
  getName() { return 'Missing request interceptor for common logic'; }
  getCategory(): IssueCategory { return 'API_DESIGN'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Multiple fetch/axios calls without centralized setup
    const fetchCount = (sourceCode.match(/fetch\(|axios\.(get|post|put|delete)/g) || []).length;
    const hasInterceptor = /interceptor|axios\.create|baseURL/i.test(sourceCode);
    const hasCommonLogic = /Authorization|Bearer|token|api[-_]?key/i.test(sourceCode);
    if (fetchCount > 2 && hasCommonLogic && !hasInterceptor) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Multiple API calls with repeated auth/header logic',
        remediation: 'Create API client with request interceptor for auth headers',
        confidence: 0.75,
      });
    }
    return issues;
  }
}
