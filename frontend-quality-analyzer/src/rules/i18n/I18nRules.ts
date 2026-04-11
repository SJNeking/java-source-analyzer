/**
 * Internationalization (i18n) quality rules
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class I18nRules {
  static all(): QualityRule[] {
    return [
      new I18nHardcodedText(),
      new I18nMissingTranslationKey(),
      new I18nDateNotLocalized(),
      new I18nNumberNotLocalized(),
      new I18nPluralNotHandled(),
    ];
  }
}

class I18nHardcodedText implements QualityRule {
  getRuleKey() { return 'FE-I18N-001'; }
  getName() { return 'Hardcoded text should use i18n keys'; }
  getCategory(): IssueCategory { return 'I18N'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Skip test files and config files
    if (/\.(test|spec|config)\.(ts|tsx|js|jsx)$/.test(filePath)) return issues;

    // Check JSX text content
    const jsxTexts = sourceCode.match(/>\s*([A-Z][a-z]+[\w\s]+)\s*</g) || [];
    const hardcodedCount = jsxTexts.filter(t => {
      const text = t.replace(/[><\s]/g, '').trim();
      // Skip if it's likely using t() or intl.formatMessage
      return text.length > 3 && !/t\(|useTranslation|i18n|intl\.format/.test(sourceCode.substring(0, 100));
    }).length;

    if (hardcodedCount > 5 && !/useTranslation|i18next|react-intl|@lingui/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${hardcodedCount} hardcoded text strings detected`,
        remediation: 'Use i18n library (i18next, react-intl, @lingui)',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class I18nMissingTranslationKey implements QualityRule {
  getRuleKey() { return 'FE-I18N-002'; }
  getName() { return 'Translation key format should follow convention'; }
  getCategory(): IssueCategory { return 'I18N'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];

    // Only apply this rule if the project actually uses an i18n library
    // Otherwise every t('...') call is likely a false positive
    if (!/useTranslation|i18next|react-intl|@lingui|t\(['"]/.test(sourceCode)) {
      return issues;
    }

    // Check for t('...') pattern and validate key format
    const tCalls = sourceCode.match(/t\s*\(\s*['"]([^'"]+)['"]/g) || [];
    tCalls.forEach(call => {
      const keyMatch = call.match(/t\s*\(\s*['"]([^'"]+)['"]/);
      if (keyMatch) {
        const key = keyMatch[1];
        // Skip short keys and hardcoded strings
        if (key.length < 3 || /[\s\u4e00-\u9fff]/.test(key)) {
          return;
        }
        // Keys should be namespaced: namespace.category.key
        if (!/\.\w+\.\w+/.test(key) && key.split('.').length < 2) {
          issues.push({
            rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
            category: this.getCategory(), file_path: filePath, line: 1,
            message: `Translation key '${key}' not following convention`,
            remediation: 'Use namespaced keys: common.button.submit',
            confidence: 0.6,
          });
        }
      }
    });
    return issues;
  }
}

class I18nDateNotLocalized implements QualityRule {
  getRuleKey() { return 'FE-I18N-003'; }
  getName() { return 'Dates should be formatted using locale-aware methods'; }
  getCategory(): IssueCategory { return 'I18N'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Check for date formatting without locale
    if (/new Date.*\.toLocaleDateString|\.toISOString\(\)|\.toDateString\(\)/.test(sourceCode)) {
      if (/\.toISOString\(\)|\.toDateString\(\)/.test(sourceCode) &&
          !/Intl\.DateTimeFormat|date-fns\/locale|moment\.locale|dayjs.*locale/.test(sourceCode)) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: 'Date formatted without locale awareness',
          remediation: 'Use Intl.DateTimeFormat or date-fns with locale',
          confidence: 0.7,
        });
      }
    }
    return issues;
  }
}

class I18nNumberNotLocalized implements QualityRule {
  getRuleKey() { return 'FE-I18N-004'; }
  getName() { return 'Numbers and currencies should use locale formatting'; }
  getCategory(): IssueCategory { return 'I18N'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];

    // Only flag files that display numbers/currencies in user-facing contexts
    // Skip files that don't look like UI components
    if (!/<\w+/.test(sourceCode) && !/render|template|jsx/.test(sourceCode.toLowerCase())) {
      return issues;
    }

    // Check for currency or number display without formatting
    const hasCurrencies = /\$|€|¥|£/.test(sourceCode);
    const hasNumberDisplay = /\{.*\d+\}/.test(sourceCode) && !/Intl\.NumberFormat|toLocaleString/.test(sourceCode);

    if ((hasCurrencies || hasNumberDisplay) &&
        !/Intl\.NumberFormat|toLocaleString|currency/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Currency/number displayed without locale formatting',
        remediation: 'Use Intl.NumberFormat for locale-aware formatting',
        confidence: 0.75,
      });
    }
    return issues;
  }
}

class I18nPluralNotHandled implements QualityRule {
  getRuleKey() { return 'FE-I18N-005'; }
  getName() { return 'Pluralization should use i18n plural rules'; }
  getCategory(): IssueCategory { return 'I18N'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Check for manual plural handling
    if (/\w+\s*===?\s*1\s*\?\s*['"]\w+s?['"]\s*:\s*['"]\w+s?['"]/.test(sourceCode)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Manual pluralization detected',
        remediation: 'Use i18next pluralization or Intl.PluralRules',
        confidence: 0.8,
      });
    }
    return issues;
  }
}
