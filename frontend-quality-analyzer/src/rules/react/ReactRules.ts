/**
 * React Best Practices Rules
 * 
 * Detects React anti-patterns and enforces best practices.
 * Mirrors backend SpringBootRules architecture.
 */

import { QualityIssue, QualityRule, IssueCategory, Severity } from '../../types';

// ==================== Base Class ====================

abstract class AbstractReactRule implements QualityRule {
  abstract getRuleKey(): string;
  abstract getName(): string;
  
  getCategory(): IssueCategory {
    return 'REACT';
  }

  check(sourceCode: string, filePath: string): QualityIssue[] {
    // Only analyze React files
    if (!filePath.match(/\.(tsx|jsx)$/)) {
      return [];
    }

    return this.checkReact(sourceCode, filePath);
  }

  protected abstract checkReact(sourceCode: string, filePath: string): QualityIssue[];
}

// ==================== Rule Implementations ====================

/**
 * FE-REACT-001: useEffect missing dependencies
 * 
 * Detects useEffect hooks with missing dependency array items.
 */
export class HooksDependencyRule extends AbstractReactRule {
  getRuleKey(): string { return 'FE-REACT-001'; }
  getName(): string { return 'useEffect has missing dependencies'; }

  protected checkReact(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Match useEffect calls
      if (!line.includes('useEffect(')) {
        return;
      }

      // Look for the dependency array in next few lines
      let effectBlock = line;
      for (let i = 1; i <= 5 && (index + i) < lines.length; i++) {
        effectBlock += '\n' + lines[index + i];
        if (lines[index + i].includes(']);') || lines[index + i].includes('])')) {
          break;
        }
      }

      // Check if dependency array is empty or missing
      if (effectBlock.match(/useEffect\(\s*\(\)\s*=>\s*{[^}]*}\s*,\s*\[\s*\]/s)) {
        // Empty dependency array - check if effect uses any variables
        const varsUsed = effectBlock.match(/\b(useState|useRef|props\.\w+|state\.\w+)\b/g);
        if (varsUsed && varsUsed.length > 0) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MAJOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'useEffect has empty dependency array but uses external variables',
            evidence: 'useEffect(() => { ... }, [])',
            remediation: 'Add missing dependencies to the dependency array'
          });
        }
      }

      // Check for missing dependency array entirely
      if (effectBlock.match(/useEffect\(\s*\(\)\s*=>\s*{[^}]*}\s*\)/s) &&
          !effectBlock.includes('],') && !effectBlock.includes('])')) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.CRITICAL,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'useEffect is missing dependency array',
          evidence: 'useEffect(() => { ... })',
          remediation: 'Add dependency array: useEffect(() => { ... }, [deps])'
        });
      }
    });

    return issues;
  }
}

/**
 * FE-REACT-002: Missing key prop in list rendering
 * 
 * Detects map() calls without key prop.
 */
export class MissingKeyPropRule extends AbstractReactRule {
  getRuleKey(): string { return 'FE-REACT-002'; }
  getName(): string { return 'Missing key prop in list rendering'; }

  protected checkReact(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect .map() calls
      if (!line.includes('.map(')) {
        return;
      }

      // Look for JSX element in next few lines
      let mapBlock = line;
      for (let i = 1; i <= 3 && (index + i) < lines.length; i++) {
        mapBlock += '\n' + lines[index + i];
        if (lines[index + i].includes(')') && !lines[index + i].includes('(')) {
          break;
        }
      }

      // Check if returned element has key prop
      if (mapBlock.includes('<') && !mapBlock.includes('key=')) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.MAJOR,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'List item rendered without key prop',
          evidence: '.map(item => <Component />)',
          remediation: 'Add unique key: .map(item => <Component key={item.id} />)'
        });
      }

      // Check for using index as key (anti-pattern)
      if (mapBlock.includes('key={index}') || mapBlock.includes('key={i}')) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.MINOR,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'Using array index as key (not recommended)',
          evidence: 'key={index}',
          remediation: 'Use stable unique identifier instead of index'
        });
      }
    });

    return issues;
  }
}

/**
 * FE-REACT-003: Direct state mutation
 * 
 * Detects direct mutations of React state.
 */
export class StateImmutabilityRule extends AbstractReactRule {
  getRuleKey(): string { return 'FE-REACT-003'; }
  getName(): string { return 'Direct state mutation detected'; }

  protected checkReact(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Skip comments
      if (line.trim().startsWith('//')) {
        return;
      }

      // Detect common state mutation patterns
      const mutationPatterns = [
        /setState\([^)]*\.[a-zA-Z]+\s*=/,  // setState(obj.prop = value)
        /\bstate\.\w+\s*=\s*/,              // state.prop = value
        /\buseState.*\[\d+\]\s*=/,          // arr[index] = value
        /\.push\(/,                          // array.push()
        /\.pop\(/,                           // array.pop()
        /\.splice\(/,                        // array.splice()
        /\.shift\(/,                         // array.shift()
        /\.unshift\(/,                       // array.unshift()
        /\.sort\(/,                          // array.sort() (mutates)
        /\.reverse\(/                        // array.reverse() (mutates)
      ];

      mutationPatterns.forEach(pattern => {
        if (pattern.test(line)) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MAJOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'Potential direct state mutation',
            evidence: line.trim(),
            remediation: 'Create a new copy before modifying: [...array], {...object}'
          });
        }
      });
    });

    return issues;
  }
}

/**
 * FE-REACT-004: Component size too large
 * 
 * Detects components that exceed recommended size limits.
 */
export class ComponentSizeRule extends AbstractReactRule {
  private readonly MAX_COMPONENT_LINES = 300;

  getRuleKey(): string { return 'FE-REACT-004'; }
  getName(): string { return `Component exceeds ${this.MAX_COMPONENT_LINES} lines`; }

  protected checkReact(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');
    const totalLines = lines.length;

    if (totalLines > this.MAX_COMPONENT_LINES) {
      issues.push({
        rule_key: this.getRuleKey(),
        rule_name: this.getName(),
        severity: Severity.MINOR,
        category: this.getCategory(),
        file_path: filePath,
        line: 1,
        message: `Component is ${totalLines} lines (max recommended: ${this.MAX_COMPONENT_LINES})`,
        evidence: `${totalLines} lines`,
        remediation: 'Consider breaking into smaller components or custom hooks'
      });
    }

    return issues;
  }
}

/**
 * FE-REACT-005: Missing cleanup in useEffect
 * 
 * Detects useEffect hooks that set up listeners/subscriptions without cleanup.
 */
export class EffectCleanupRule extends AbstractReactRule {
  getRuleKey(): string { return 'FE-REACT-005'; }
  getName(): string { return 'useEffect missing cleanup function'; }

  protected checkReact(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      if (!line.includes('useEffect(')) {
        return;
      }

      // Look for the entire useEffect block
      let effectBlock = '';
      let braceCount = 0;
      let startFound = false;
      
      for (let i = index; i < lines.length && i < index + 30; i++) {
        const currentLine = lines[i];
        effectBlock += currentLine + '\n';
        
        if (currentLine.includes('{')) {
          braceCount += (currentLine.match(/{/g) || []).length;
          startFound = true;
        }
        if (currentLine.includes('}')) {
          braceCount -= (currentLine.match(/}/g) || []).length;
        }
        
        if (startFound && braceCount === 0) {
          break;
        }
      }

      // Check for event listeners or subscriptions without cleanup
      const needsCleanup = [
        /addEventListener\s*\(/,
        /setInterval\s*\(/,
        /setTimeout\s*\(/,
        /subscribe\s*\(/,
        /WebSocket\s*\(/,
        /new\s+Observer/
      ];

      const hasSetup = needsCleanup.some(pattern => pattern.test(effectBlock));
      const hasCleanup = effectBlock.includes('return () =>') || 
                        effectBlock.includes('return function');

      if (hasSetup && !hasCleanup) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.MAJOR,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'useEffect sets up listener/subscription without cleanup',
          evidence: 'useEffect(() => { addEventListener(...) })',
          remediation: 'Return cleanup function: return () => { removeEventListener(...) }'
        });
      }
    });

    return issues;
  }
}

/**
 * FE-REACT-006: Prop drilling detection
 * 
 * Detects excessive prop drilling (passing props through many levels).
 */
export class PropDrillingRule extends AbstractReactRule {
  getRuleKey(): string { return 'FE-REACT-006'; }
  getName(): string { return 'Excessive prop drilling detected'; }

  protected checkReact(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    
    // Count how many props are passed down
    const propPassPattern = /<\w+\s+([^>]*?)>/g;
    let match;
    let totalPropsPassed = 0;

    while ((match = propPassPattern.exec(sourceCode)) !== null) {
      const props = match[1].match(/\w+=/g);
      if (props) {
        totalPropsPassed += props.length;
      }
    }

    // Heuristic: if component passes more than 10 props, likely prop drilling
    if (totalPropsPassed > 10) {
      issues.push({
        rule_key: this.getRuleKey(),
        rule_name: this.getName(),
        severity: Severity.INFO,
        category: this.getCategory(),
        file_path: filePath,
        line: 1,
        message: `Component passes ${totalPropsPassed} props (possible prop drilling)`,
        evidence: `${totalPropsPassed} props passed to children`,
        remediation: 'Consider using Context API or state management library'
      });
    }

    return issues;
  }
}

// Export all rules for easy registration
export const ReactRules = {
  HooksDependencyRule,
  MissingKeyPropRule,
  StateImmutabilityRule,
  ComponentSizeRule,
  EffectCleanupRule,
  PropDrillingRule
};
