/**
 * Bundle Optimization quality rules
 * Detects bundle size and loading optimization issues
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class BundleOptimizationRules {
  static all(): QualityRule[] {
    return [
      new BundleMissingDynamicImportPrefetch(),
      new BundleCSSInJSRuntime(),
      new BundleLargeVendorChunk(),
      new BundleMissingTreeShaking(),
      new BundleDuplicateImports(),
      new BundleMissingFontOptimization(),
    ];
  }
}

class BundleMissingDynamicImportPrefetch implements QualityRule {
  getRuleKey() { return 'FE-BUNDLE-001'; }
  getName() { return 'Dynamic import without prefetch/preload hint'; }
  getCategory(): IssueCategory { return 'BUNDLE_OPTIMIZATION'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // import() without webpackChunkName or preload
    const dynamicImports = /import\s*\(\s*['"][^'"]+['"]\s*\)/g;
    const imports = sourceCode.match(dynamicImports) || [];
    const hasChunkName = /webpackChunkName|webpackPreload|webpackPrefetch/i.test(sourceCode);
    if (imports.length > 0 && !hasChunkName) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${imports.length} dynamic import(s) without prefetch hints`,
        remediation: 'Add /* webpackChunkName: "name" */ or use <link rel="prefetch">',
        confidence: 0.75,
      });
    }
    return issues;
  }
}

class BundleCSSInJSRuntime implements QualityRule {
  getRuleKey() { return 'FE-BUNDLE-002'; }
  getName() { return 'CSS-in-JS runtime overhead'; }
  getCategory(): IssueCategory { return 'BUNDLE_OPTIMIZATION'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Heavy use of styled-components/emotion without extraction
    const hasCSSInJS = /styled\.\w+|css`|@emotion|styled-components/g.test(sourceCode);
    const hasExtractCSS = /babel-plugin-styled-components|extractStyles/i.test(sourceCode);
    const styledCount = (sourceCode.match(/styled\.\w+|css`/g) || []).length;
    if (hasCSSInJS && !hasExtractCSS && styledCount > 10) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${styledCount} CSS-in-JS declarations without extraction`,
        remediation: 'Configure CSS-in-JS extraction plugin or use static CSS',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class BundleLargeVendorChunk implements QualityRule {
  getRuleKey() { return 'FE-BUNDLE-003'; }
  getName() { return 'Large library fully imported instead of modular'; }
  getCategory(): IssueCategory { return 'BUNDLE_OPTIMIZATION'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Full library imports instead of modular
    const fullImports = [
      /from\s+['"]lodash['"]/,
      /from\s+['"]moment['"]/,
      /from\s+['"]antd['"]/,
      /from\s+['"]@mui\/material['"]/,
    ];
    const matches = fullImports.filter(pattern => pattern.test(sourceCode));
    const modularImports = [
      /from\s+['"]lodash\/\w+['"]/,
      /from\s+['"]date-fns['"]/,
      /from\s+['"]@mui\/material\/\w+['"]/,
    ];
    const hasModular = modularImports.some(pattern => pattern.test(sourceCode));
    if (matches.length > 0 && !hasModular) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Full library import increases bundle size',
        remediation: 'Use modular imports: import map from "lodash/map" instead of full lodash',
        confidence: 0.9,
      });
    }
    return issues;
  }
}

class BundleMissingTreeShaking implements QualityRule {
  getRuleKey() { return 'FE-BUNDLE-004'; }
  getName() { return 'Import prevents tree shaking'; }
  getCategory(): IssueCategory { return 'BUNDLE_OPTIMIZATION'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Namespace imports that prevent tree shaking
    const namespaceImports = /import\s+\*\s+as\s+\w+\s+from\s+['"]/g;
    const imports = sourceCode.match(namespaceImports) || [];
    // Default imports from libraries
    const defaultLibImports = /import\s+\w+\s+from\s+['"](?:lodash|rxjs|ramda)/g;
    const defaultImports = sourceCode.match(defaultLibImports) || [];
    if (imports.length > 0 || defaultImports.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Namespace/default imports may prevent tree shaking',
        remediation: 'Use named imports: import { specific } from "library"',
        confidence: 0.75,
      });
    }
    return issues;
  }
}

class BundleDuplicateImports implements QualityRule {
  getRuleKey() { return 'FE-BUNDLE-005'; }
  getName() { return 'Duplicate or overlapping imports'; }
  getCategory(): IssueCategory { return 'BUNDLE_OPTIMIZATION'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    // Multiple imports from same library
    const importLines = sourceCode.match(/import\s+.*from\s+['"][^'"]+['"]/g) || [];
    const libraryCounts: Record<string, number> = {};
    importLines.forEach(imp => {
      const match = imp.match(/from\s+['"](@?\w+(?:\/\w+)?)/);
      if (match) {
        const lib = match[1];
        libraryCounts[lib] = (libraryCounts[lib] || 0) + 1;
      }
    });
    const duplicates = Object.entries(libraryCounts).filter(([_, count]) => count > 3);
    if (duplicates.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${duplicates.length} library/library imported ${duplicates[0][1]} times`,
        remediation: 'Consolidate imports into single import statement',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class BundleMissingFontOptimization implements QualityRule {
  getRuleKey() { return 'FE-BUNDLE-006'; }
  getName() { return 'Font loading not optimized'; }
  getCategory(): IssueCategory { return 'BUNDLE_OPTIMIZATION'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isHTML = /\.html?$/.test(filePath);
    const hasCSS = /\.css$|\.scss$|\.styled\.tsx?$/.test(filePath);
    if (!isHTML && !hasCSS) return issues;
    // Font without display: swap or preload
    const hasFontFace = /@font-face/i.test(sourceCode);
    const hasDisplaySwap = /display\s*:\s*swap/i.test(sourceCode);
    const hasPreload = /<link[^>]+rel=["']preload["'][^>]+as=["']font["']/i.test(sourceCode);
    if (hasFontFace && !hasDisplaySwap && !hasPreload) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Custom font without display: swap causes FOIT',
        remediation: 'Add font-display: swap and preload critical fonts',
        confidence: 0.85,
      });
    }
    return issues;
  }
}
