/**
 * Architecture quality rules
 * Detects structural and organizational anti-patterns
 * Including: hardcoding, boundary violations, data flow issues, SRP violations
 */
import { QualityRule, QualityIssue, IssueCategory, Severity } from '../../types';

export class ArchitectureRules {
  static all(): QualityRule[] {
    return [
      // Original rules
      new ArchNoCircularImport(),
      new ArchComponentTooDeep(),
      new ArchPageDirectApiCall(),
      new ArchTooManyProps(),
      new ArchMixedResponsibility(),
      new ArchMagicString(),
      new ArchDeepImportPath(),
      new ArchNoBarrelExport(),
      
      // Hardcoding rules (NEW)
      new ArchHardcodedAPIEndpoint(),
      new ArchHardcodedURL(),
      new ArchHardcodedColor(),
      new ArchHardcodedConfigValue(),
      new ArchHardcodedEnvironmentValue(),
      new ArchHardcodedTokenOrSecret(),
      new ArchHardcodedFilePath(),
      new ArchMagicNumber(),
      
      // Architecture boundary and layer separation (NEW)
      new ArchLayerBoundaryViolation(),
      new ArchCrossLayerImport(),
      new ArchNoServiceLayer(),
      new ArchNoDomainBoundary(),
      new ArchBypassStateManagement(),
      new ArchDirectStoreMutation(),
      
      // Data flow clarity rules (NEW)
      new ArchPropDrilling(),
      new ArchNoUnidirectionalDataFlow(),
      new ArchStateScatteredEverywhere(),
      new ArchNoDataSourceAbstraction(),
      new ArchCallbackHell(),
      new ArchImplicitDataType(),
      
      // Single Responsibility Principle violations (NEW)
      new ArchGodComponent(),
      new ArchFunctionTooLong(),
      new ArchFileTooLarge(),
      new ArchTooManyResponsibilities(),
      new ArchNoHookAbstraction(),
      new ArchMixedDataSources(),
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

// ==================== HARDCODING RULES ====================

class ArchHardcodedAPIEndpoint implements QualityRule {
  getRuleKey() { return 'FE-ARCH-009'; }
  getName() { return 'Hardcoded API endpoint detected'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const apiPattern = /['"]https?:\/\/[^'"]*(api|v\d+|graphql|rest)[^'"]*['"]/gi;
    const matches = sourceCode.match(apiPattern) || [];
    if (matches.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${matches.length} hardcoded API endpoint(s) found`,
        remediation: 'Extract API endpoints to a configuration file or environment variables',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class ArchHardcodedURL implements QualityRule {
  getRuleKey() { return 'FE-ARCH-010'; }
  getName() { return 'Hardcoded URL detected'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const urlPattern = /['"]https?:\/\/[^\s'"]{10,}['"]/g;
    const matches = sourceCode.match(urlPattern) || [];
    const suspiciousUrls = matches.filter(url => 
      !/example\.com|placeholder|localhost:3000|localhost:5173/.test(url)
    );
    if (suspiciousUrls.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${suspiciousUrls.length} hardcoded URL(s) found`,
        remediation: 'Use environment variables or configuration for URLs',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class ArchHardcodedColor implements QualityRule {
  getRuleKey() { return 'FE-ARCH-011'; }
  getName() { return 'Hardcoded color value detected'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const hexPattern = /#[0-9a-fA-F]{6}\b|#[0-9a-fA-F]{3}\b/g;
    const rgbPattern = /rgba?\(\s*\d+\s*,\s*\d+\s*,\s*\d+/g;
    const hexMatches = sourceCode.match(hexPattern) || [];
    const rgbMatches = sourceCode.match(rgbPattern) || [];
    const totalMatches = hexMatches.length + rgbMatches.length;
    if (totalMatches > 3) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${totalMatches} hardcoded color value(s) detected`,
        remediation: 'Use CSS variables or a design token system',
        confidence: 0.9,
      });
    }
    return issues;
  }
}

class ArchHardcodedConfigValue implements QualityRule {
  getRuleKey() { return 'FE-ARCH-012'; }
  getName() { return 'Hardcoded configuration value detected'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const configPatterns = [
      /['"]timeout['"]\s*:\s*\d+/i,
      /['"]maxRetries['"]\s*:\s*\d+/i,
      /['"]pageSize['"]\s*:\s*\d+/i,
      /['"]limit['"]\s*:\s*\d+/i,
      /const\s+(TIMEOUT|MAX_SIZE|PAGE_SIZE|LIMIT)\s*=\s*\d+/i,
    ];
    const matches = configPatterns.filter(pattern => pattern.test(sourceCode));
    if (matches.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Hardcoded configuration value detected',
        remediation: 'Extract to configuration file or environment variables',
        confidence: 0.75,
      });
    }
    return issues;
  }
}

class ArchHardcodedEnvironmentValue implements QualityRule {
  getRuleKey() { return 'FE-ARCH-013'; }
  getName() { return 'Hardcoded environment-specific value detected'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const envPatterns = [
      /['"]development['"]|['"]production['"]|['"]staging['"]|['"]test['"]/i,
    ];
    const shouldUseEnvPattern = /process\.env|import\.meta\.env/;
    const hasHardcodedEnv = envPatterns.some(pattern => pattern.test(sourceCode));
    const notUsingEnvVars = !shouldUseEnvPattern.test(sourceCode);
    if (hasHardcodedEnv && notUsingEnvVars) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Environment-specific value hardcoded',
        remediation: 'Use process.env (Node.js) or import.meta.env (Vite) for environment variables',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class ArchHardcodedTokenOrSecret implements QualityRule {
  getRuleKey() { return 'FE-ARCH-014'; }
  getName() { return 'Hardcoded token or secret detected'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const tokenPatterns = [
      /['"][A-Za-z0-9]{20,}['"]/,
      /apiKey|api_key|secret|password|token/i,
      /Bearer\s+[A-Za-z0-9._-]+/,
      /AKIA[0-9A-Z]{16}/,
    ];
    const matches = tokenPatterns.filter(pattern => pattern.test(sourceCode));
    if (matches.length >= 2) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.CRITICAL,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Potential hardcoded token, API key, or secret detected',
        remediation: 'NEVER hardcode secrets. Use environment variables or secret management',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class ArchHardcodedFilePath implements QualityRule {
  getRuleKey() { return 'FE-ARCH-015'; }
  getName() { return 'Hardcoded file path detected'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const pathPatterns = [
      /['"]\/[a-z]+\/[a-z]+\/[a-z]+['"]/i,
      /['"][A-Z]:\\[^'"]+['"]/i,
      /['"]\/tmp\/|['"]\/var\/|['"]\/etc\//i,
    ];
    const matches = pathPatterns.filter(pattern => pattern.test(sourceCode));
    if (matches.length > 0) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Hardcoded file path detected',
        remediation: 'Use path resolution utilities or configuration',
        confidence: 0.65,
      });
    }
    return issues;
  }
}

class ArchMagicNumber implements QualityRule {
  getRuleKey() { return 'FE-ARCH-016'; }
  getName() { return 'Magic number detected'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const numberPattern = /\b\d{2,}\b/g;
    const numbers = sourceCode.match(numberPattern) || [];
    const magicNumbers = numbers.filter(n => {
      const num = parseInt(n);
      return ![10, 100, 1000, 60, 24, 365].includes(num);
    });
    if (magicNumbers.length > 5) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${magicNumbers.length} magic number(s) found`,
        remediation: 'Extract numbers to named constants for better readability',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

// ==================== ARCHITECTURE BOUNDARY RULES ====================

class ArchLayerBoundaryViolation implements QualityRule {
  getRuleKey() { return 'FE-ARCH-017'; }
  getName() { return 'Layer boundary violation detected'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isUIComponent = /components?\/|ui\/|widgets?\//i.test(filePath);
    const hasDataLayerAccess = /store|repository|database|orm|prisma|sequelize/i.test(sourceCode);
    if (isUIComponent && hasDataLayerAccess) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'UI component directly accessing data layer',
        remediation: 'Use service layer or data hooks to maintain layer boundaries',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class ArchCrossLayerImport implements QualityRule {
  getRuleKey() { return 'FE-ARCH-018'; }
  getName() { return 'Cross-layer import detected'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isComponent = /components?\//i.test(filePath);
    const isPage = /pages?|views?|screens?/i.test(filePath);
    const importsDomain = /from\s+['"].*domain\/|from\s+['"].*entities?\//i.test(sourceCode);
    const importsInfrastructure = /from\s+['"].*infrastructure\/|from\s+['"].*data\//i.test(sourceCode);
    if ((isComponent || isPage) && (importsDomain || importsInfrastructure)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Presentation layer importing domain/infrastructure directly',
        remediation: 'Use application layer (services/useCases) to mediate',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class ArchNoServiceLayer implements QualityRule {
  getRuleKey() { return 'FE-ARCH-019'; }
  getName() { return 'Business logic in component (missing service layer)'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isComponent = /\.(tsx|jsx|vue)$/.test(filePath);
    const hasBusinessLogic = /if\s*\(.*(?:calculate|validate|transform|filter|sort|process)/i.test(sourceCode) ||
                            /for\s*\(.*(?:calculate|validate|transform|filter|sort|process)/i.test(sourceCode);
    if (isComponent && hasBusinessLogic) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Business logic detected in component',
        remediation: 'Extract to service layer or custom hook',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class ArchNoDomainBoundary implements QualityRule {
  getRuleKey() { return 'FE-ARCH-020'; }
  getName() { return 'Domain entity manipulation outside domain layer'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const notInDomain = !/domain\/|entities?\//i.test(filePath);
    const hasEntityManipulation = /interface\s+\w*(?:Entity|Model|Domain)|type\s+\w*(?:Entity|Model|Domain)/i.test(sourceCode);
    if (notInDomain && hasEntityManipulation) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Domain entity definition outside domain layer',
        remediation: 'Move entity definitions to domain/entities layer',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class ArchBypassStateManagement implements QualityRule {
  getRuleKey() { return 'FE-ARCH-021'; }
  getName() { return 'Bypassing centralized state management'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const hasGlobalStore = /redux|mobx|zustand|recoil|pinia|ngrx/i.test(sourceCode);
    const bypassesWithLocal = /(useState|useReducer|ref\(|data\s*:\s*\{)/.test(sourceCode);
    const hasComplexState = /interface\s+\w*State|type\s+\w*State/i.test(sourceCode);
    if (hasGlobalStore && bypassesWithLocal && hasComplexState) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Complex state managed locally while global state available',
        remediation: 'Consider using centralized state management for consistency',
        confidence: 0.6,
      });
    }
    return issues;
  }
}

class ArchDirectStoreMutation implements QualityRule {
  getRuleKey() { return 'FE-ARCH-022'; }
  getName() { return 'Direct store mutation detected'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const mutationPatterns = [
      /store\.\w+\s*=/,
      /state\.\w+\s*=/,
    ];
    const hasDirectMutation = mutationPatterns.some(pattern => pattern.test(sourceCode));
    if (hasDirectMutation) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Direct state mutation detected',
        remediation: 'Use actions/reducers/mutations for state changes',
        confidence: 0.75,
      });
    }
    return issues;
  }
}

// ==================== DATA FLOW CLARITY RULES ====================

class ArchPropDrilling implements QualityRule {
  getRuleKey() { return 'FE-ARCH-023'; }
  getName() { return 'Prop drilling detected (passing through 3+ levels)'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const propPatterns = [
      /\{\s*\.\.\.(?:props|data|state)\s*\}/g,
      /(?:data|state|props)\.\w+(?:\.\w+){2,}/g,
    ];
    const matches = propPatterns.reduce((acc, pattern) => {
      return acc + (sourceCode.match(pattern) || []).length;
    }, 0);
    if (matches > 3) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Potential prop drilling detected',
        remediation: 'Use Context API, composition, or state management',
        confidence: 0.65,
      });
    }
    return issues;
  }
}

class ArchNoUnidirectionalDataFlow implements QualityRule {
  getRuleKey() { return 'FE-ARCH-024'; }
  getName() { return 'Bidirectional data flow detected'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const bidirectionalPatterns = [
      /onUpdate\([^)]*\)\s*=>\s*\{[^}]*\.\w+\s*=/,
      /callback.*setState|callback.*mutation/i,
      /emit\(['"]update/i,
    ];
    const hasBidirectional = bidirectionalPatterns.some(pattern => pattern.test(sourceCode));
    if (hasBidirectional) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Bidirectional data flow detected',
        remediation: 'Use unidirectional data flow: parent -> child via props, child -> parent via callbacks',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class ArchStateScatteredEverywhere implements QualityRule {
  getRuleKey() { return 'FE-ARCH-025'; }
  getName() { return 'State scattered across multiple components'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const stateDeclarations = (sourceCode.match(/useState|useReducer|ref\(|reactive\(|state\s*=/g) || []).length;
    if (stateDeclarations > 5) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `${stateDeclarations} state declaration(s) in single file`,
        remediation: 'Consolidate related state using custom hooks or state management',
        confidence: 0.75,
      });
    }
    return issues;
  }
}

class ArchNoDataSourceAbstraction implements QualityRule {
  getRuleKey() { return 'FE-ARCH-026'; }
  getName() { return 'No data source abstraction'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isComponent = /\.(tsx|jsx|vue)$/.test(filePath);
    const hasDataFetching = /fetch\(|axios\.|useQuery|useMutation|\.get\(|\.post\(/.test(sourceCode);
    const hasRepositoryPattern = /repository|datasource|service/i.test(sourceCode);
    if (isComponent && hasDataFetching && !hasRepositoryPattern) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Data fetching without abstraction layer',
        remediation: 'Create repository/service layer for data access',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

class ArchCallbackHell implements QualityRule {
  getRuleKey() { return 'FE-ARCH-027'; }
  getName() { return 'Callback hell or deeply nested promises'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');
    let maxNesting = 0;
    let currentNesting = 0;
    lines.forEach(line => {
      const opens = (line.match(/\.then\(|\.catch\(|=>\s*\{|if\s*\(/g) || []).length;
      const closes = (line.match(/^\s*\}\)|\s*\);/g) || []).length;
      currentNesting += opens - closes;
      maxNesting = Math.max(maxNesting, currentNesting);
    });
    if (maxNesting > 3) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `Deeply nested async code (depth: ${maxNesting})`,
        remediation: 'Use async/await or extract to separate functions',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class ArchImplicitDataType implements QualityRule {
  getRuleKey() { return 'FE-ARCH-028'; }
  getName() { return 'Implicit data type (missing TypeScript types)'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isTypeScript = /\.tsx?$/.test(filePath);
    const hasUntypedState = /useState\(\)/.test(sourceCode) && !/useState<\w+>/.test(sourceCode);
    const hasUntypedProps = /: React\.FC\(\)/.test(sourceCode) || /props\s*:\s*any/.test(sourceCode);
    const hasUntypedFetch = /fetch\([^)]+\)\.then/.test(sourceCode) && !/interface|type/.test(sourceCode);
    if (isTypeScript && (hasUntypedState || hasUntypedProps || hasUntypedFetch)) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Data flow without explicit TypeScript typing',
        remediation: 'Add explicit TypeScript interfaces for all data structures',
        confidence: 0.8,
      });
    }
    return issues;
  }
}

// ==================== SINGLE RESPONSIBILITY PRINCIPLE RULES ====================

class ArchGodComponent implements QualityRule {
  getRuleKey() { return 'FE-ARCH-029'; }
  getName() { return 'God component (too large and too many responsibilities)'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isComponent = /\.(tsx|jsx|vue)$/.test(filePath);
    if (!isComponent) return issues;
    const lines = sourceCode.split('\n').length;
    const hasMultipleResponsibilities = [
      /useState|useReducer/,
      /useEffect|watch/,
      /fetch|axios|useQuery/,
      /router|navigate|history/,
      /form|submit|validation/i,
      /modal|dialog|toast/,
    ].filter(pattern => pattern.test(sourceCode)).length;
    if (lines > 300 && hasMultipleResponsibilities >= 3) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `God component: ${lines} lines with ${hasMultipleResponsibilities} responsibilities`,
        remediation: 'Split into smaller, focused components. Extract logic to custom hooks/services.',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class ArchFunctionTooLong implements QualityRule {
  getRuleKey() { return 'FE-ARCH-030'; }
  getName() { return 'Function too long'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const functionPattern = /(async\s+)?function\s+\w+\s*\([^)]*\)\s*\{([^}]+(?:\{[^}]*\}[^}]*)*)\}/gs;
    const arrowFunctionPattern = /const\s+\w+\s*=\s*(async\s+)?\([^)]*\)\s*=>\s*\{([^}]+(?:\{[^}]*\}[^}]*)*)\}/gs;
    const functions = sourceCode.match(functionPattern) || sourceCode.match(arrowFunctionPattern) || [];
    functions.forEach(func => {
      const lineCount = (func.match(/\n/g) || []).length;
      if (lineCount > 50) {
        issues.push({
          rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
          category: this.getCategory(), file_path: filePath, line: 1,
          message: `Function too long: ${lineCount} lines (max recommended: 50)`,
          remediation: 'Extract sub-functions following single responsibility principle',
          confidence: 0.8,
        });
      }
    });
    return issues;
  }
}

class ArchFileTooLarge implements QualityRule {
  getRuleKey() { return 'FE-ARCH-031'; }
  getName() { return 'File too large'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lineCount = sourceCode.split('\n').length;
    if (lineCount > 400) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `File too large: ${lineCount} lines (max recommended: 400)`,
        remediation: 'Split into multiple files following single responsibility principle',
        confidence: 0.9,
      });
    }
    return issues;
  }
}

class ArchTooManyResponsibilities implements QualityRule {
  getRuleKey() { return 'FE-ARCH-032'; }
  getName() { return 'Component has too many responsibilities'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const responsibilities = [
      { name: 'State Management', pattern: /useState|useReducer|ref\(/ },
      { name: 'Data Fetching', pattern: /fetch\(|axios\.|useQuery/ },
      { name: 'Routing', pattern: /useNavigate|useRouter|router\./ },
      { name: 'Form Handling', pattern: /onSubmit|validation|formData/i },
      { name: 'Animation', pattern: /useAnimation|gsap|framer-motion/ },
      { name: 'Analytics', pattern: /analytics|trackEvent|gtag/ },
      { name: 'Authentication', pattern: /login|logout|auth|token/i },
      { name: 'Internationalization', pattern: /i18n|t\(|translate/ },
    ];
    const presentResponsibilities = responsibilities.filter(r => r.pattern.test(sourceCode));
    if (presentResponsibilities.length >= 4) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MAJOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `Component handles ${presentResponsibilities.length} responsibilities: ${presentResponsibilities.map(r => r.name).join(', ')}`,
        remediation: 'Split responsibilities using SRP. Extract to custom hooks, services, or sub-components.',
        confidence: 0.85,
      });
    }
    return issues;
  }
}

class ArchNoHookAbstraction implements QualityRule {
  getRuleKey() { return 'FE-ARCH-033'; }
  getName() { return 'Reusable logic not extracted to custom hooks'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const isReactComponent = /\.tsx$/.test(filePath) && /function\s+\w+|const\s+\w+\s*=\s*\(\)/.test(sourceCode);
    const hasReusableLogic = /(useEffect.*fetch|useState.*debounce|useEffect.*subscribe)/s.test(sourceCode);
    const noCustomHooks = !/use[A-Z]\w+/.test(sourceCode) || !/function\s+use[A-Z]\w+/.test(sourceCode);
    if (isReactComponent && hasReusableLogic && noCustomHooks) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.INFO,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: 'Reusable logic not extracted to custom hooks',
        remediation: 'Extract reusable logic to custom hooks (useFetch, useDebounce, etc.)',
        confidence: 0.7,
      });
    }
    return issues;
  }
}

class ArchMixedDataSources implements QualityRule {
  getRuleKey() { return 'FE-ARCH-034'; }
  getName() { return 'Component mixing multiple data sources'; }
  getCategory(): IssueCategory { return 'ARCHITECTURE'; }
  check(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const dataSources = [
      /useSelector|useStore/,
      /useContext/,
      /useState/,
      /useQuery/,
    ];
    const mixedSources = dataSources.filter(pattern => pattern.test(sourceCode));
    if (mixedSources.length >= 2) {
      issues.push({
        rule_key: this.getRuleKey(), rule_name: this.getName(), severity: Severity.MINOR,
        category: this.getCategory(), file_path: filePath, line: 1,
        message: `Component uses ${mixedSources.length} different data sources`,
        remediation: 'Unify data source management. Use one state management solution consistently.',
        confidence: 0.75,
      });
    }
    return issues;
  }
}
