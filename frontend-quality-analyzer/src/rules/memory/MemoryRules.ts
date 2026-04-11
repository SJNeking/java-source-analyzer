/**
 * Frontend Memory Leak Detection Rules
 * 
 * Detects potential memory leaks in frontend code.
 * Mirrors backend ResourceLeakPath architecture with CFG-based analysis concept.
 */

import { QualityIssue, QualityRule, IssueCategory, Severity } from '../../types';

// ==================== Base Class ====================

abstract class AbstractMemoryRule implements QualityRule {
  abstract getRuleKey(): string;
  abstract getName(): string;
  
  getCategory(): IssueCategory {
    return 'MEMORY';
  }

  check(sourceCode: string, filePath: string): QualityIssue[] {
    return this.checkMemory(sourceCode, filePath);
  }

  protected abstract checkMemory(sourceCode: string, filePath: string): QualityIssue[];
}

// ==================== Rule Implementations ====================

/**
 * FE-MEM-001: Event listener without cleanup
 * 
 * Detects addEventListener calls without corresponding removeEventListener.
 */
export class EventListenerCleanupRule extends AbstractMemoryRule {
  getRuleKey(): string { return 'FE-MEM-001'; }
  getName(): string { return 'Event listener added without cleanup'; }

  protected checkMemory(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect addEventListener calls
      if (line.includes('addEventListener(')) {
        // Check if there's a corresponding removeEventListener
        const hasCleanup = sourceCode.includes('removeEventListener(');
        
        // For React useEffect, check for cleanup function
        const isInEffect = this.isInsideUseEffect(lines, index);
        const hasEffectCleanup = isInEffect && this.hasEffectCleanup(lines, index);

        if (!hasCleanup && (!isInEffect || !hasEffectCleanup)) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MAJOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'Event listener added without cleanup',
            evidence: line.trim(),
            remediation: 'Add removeEventListener in cleanup function or useEffect return'
          });
        }
      }
    });

    return issues;
  }

  private isInsideUseEffect(lines: string[], currentIndex: number): boolean {
    // Look backwards for useEffect
    for (let i = Math.max(0, currentIndex - 20); i < currentIndex; i++) {
      if (lines[i].includes('useEffect(')) {
        return true;
      }
    }
    return false;
  }

  private hasEffectCleanup(lines: string[], effectStartIndex: number): boolean {
    // Look forward for return () => cleanup pattern
    let braceCount = 0;
    let inEffect = false;
    
    for (let i = effectStartIndex; i < Math.min(effectStartIndex + 50, lines.length); i++) {
      const line = lines[i];
      
      if (line.includes('useEffect(')) {
        inEffect = true;
      }
      
      if (inEffect) {
        braceCount += (line.match(/{/g) || []).length;
        braceCount -= (line.match(/}/g) || []).length;
        
        if (line.includes('return () =>') || line.includes('return function')) {
          return true;
        }
        
        if (braceCount === 0 && inEffect) {
          break;
        }
      }
    }
    
    return false;
  }
}

/**
 * FE-MEM-002: Timer without cleanup
 * 
 * Detects setInterval/setTimeout without clearInterval/clearTimeout.
 */
export class TimerCleanupRule extends AbstractMemoryRule {
  getRuleKey(): string { return 'FE-MEM-002'; }
  getName(): string { return 'Timer created without cleanup'; }

  protected checkMemory(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect setInterval (more critical than setTimeout)
      if (line.includes('setInterval(')) {
        const hasCleanup = sourceCode.includes('clearInterval(');
        const isInEffect = this.isInsideUseEffect(lines, index);
        const hasEffectCleanup = isInEffect && this.hasEffectCleanup(lines, index);

        if (!hasCleanup && (!isInEffect || !hasEffectCleanup)) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MAJOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'setInterval without clearInterval cleanup',
            evidence: line.trim(),
            remediation: 'Store interval ID and clear in cleanup: clearInterval(id)'
          });
        }
      }

      // Detect setTimeout (less critical but still important)
      if (line.includes('setTimeout(') && !line.includes('clearTimeout')) {
        // Only flag if it's a long-running timeout or in a loop
        const isLongRunning = line.match(/setTimeout\s*\([^,]+,\s*(\d+)/);
        if (isLongRunning && parseInt(isLongRunning[1]) > 5000) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MINOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'Long-running setTimeout without clearTimeout',
            evidence: line.trim(),
            remediation: 'Store timeout ID and clear when component unmounts'
          });
        }
      }
    });

    return issues;
  }

  private isInsideUseEffect(lines: string[], currentIndex: number): boolean {
    for (let i = Math.max(0, currentIndex - 20); i < currentIndex; i++) {
      if (lines[i].includes('useEffect(')) {
        return true;
      }
    }
    return false;
  }

  private hasEffectCleanup(lines: string[], effectStartIndex: number): boolean {
    let braceCount = 0;
    let inEffect = false;
    
    for (let i = effectStartIndex; i < Math.min(effectStartIndex + 50, lines.length); i++) {
      const line = lines[i];
      
      if (line.includes('useEffect(')) {
        inEffect = true;
      }
      
      if (inEffect) {
        braceCount += (line.match(/{/g) || []).length;
        braceCount -= (line.match(/}/g) || []).length;
        
        if (line.includes('return () =>') || line.includes('return function')) {
          return true;
        }
        
        if (braceCount === 0 && inEffect) {
          break;
        }
      }
    }
    
    return false;
  }
}

/**
 * FE-MEM-003: Observer without disconnect
 * 
 * Detects IntersectionObserver/MutationObserver without disconnect.
 */
export class ObserverCleanupRule extends AbstractMemoryRule {
  getRuleKey(): string { return 'FE-MEM-003'; }
  getName(): string { return 'Observer created without disconnect'; }

  protected checkMemory(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    const observerTypes = [
      { type: 'IntersectionObserver', cleanup: 'disconnect' },
      { type: 'MutationObserver', cleanup: 'disconnect' },
      { type: 'ResizeObserver', cleanup: 'disconnect' },
      { type: 'PerformanceObserver', cleanup: 'disconnect' }
    ];

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      observerTypes.forEach(({ type, cleanup }) => {
        const pattern = new RegExp(`new\\s+${type}\\s*\\(`);
        if (pattern.test(line)) {
          // Check for disconnect call
          const hasCleanup = sourceCode.includes(`.${cleanup}(`);
          const isInEffect = this.isInsideUseEffect(lines, index);
          const hasEffectCleanup = isInEffect && this.hasEffectCleanup(lines, index);

          if (!hasCleanup && (!isInEffect || !hasEffectCleanup)) {
            issues.push({
              rule_key: this.getRuleKey(),
              rule_name: this.getName(),
              severity: Severity.MAJOR,
              category: this.getCategory(),
              file_path: filePath,
              line: lineNum,
              message: `${type} created without ${cleanup} cleanup`,
              evidence: line.trim(),
              remediation: `Call observer.${cleanup}() in cleanup function`
            });
          }
        }
      });
    });

    return issues;
  }

  private isInsideUseEffect(lines: string[], currentIndex: number): boolean {
    for (let i = Math.max(0, currentIndex - 20); i < currentIndex; i++) {
      if (lines[i].includes('useEffect(')) {
        return true;
      }
    }
    return false;
  }

  private hasEffectCleanup(lines: string[], effectStartIndex: number): boolean {
    let braceCount = 0;
    let inEffect = false;
    
    for (let i = effectStartIndex; i < Math.min(effectStartIndex + 50, lines.length); i++) {
      const line = lines[i];
      
      if (line.includes('useEffect(')) {
        inEffect = true;
      }
      
      if (inEffect) {
        braceCount += (line.match(/{/g) || []).length;
        braceCount -= (line.match(/}/g) || []).length;
        
        if (line.includes('return () =>') || line.includes('return function')) {
          return true;
        }
        
        if (braceCount === 0 && inEffect) {
          break;
        }
      }
    }
    
    return false;
  }
}

/**
 * FE-MEM-004: WebSocket without close
 * 
 * Detects WebSocket connections without proper closure.
 */
export class WebSocketCleanupRule extends AbstractMemoryRule {
  getRuleKey(): string { return 'FE-MEM-004'; }
  getName(): string { return 'WebSocket connection without close'; }

  protected checkMemory(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect WebSocket creation
      if (line.match(/new\s+WebSocket\s*\(/)) {
        // Check for close() call
        const hasClose = sourceCode.includes('.close(');
        const isInEffect = this.isInsideUseEffect(lines, index);
        const hasEffectCleanup = isInEffect && this.hasEffectCleanup(lines, index);

        if (!hasClose && (!isInEffect || !hasEffectCleanup)) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MAJOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'WebSocket created without close() cleanup',
            evidence: line.trim(),
            remediation: 'Call ws.close() in cleanup function or componentWillUnmount'
          });
        }
      }
    });

    return issues;
  }

  private isInsideUseEffect(lines: string[], currentIndex: number): boolean {
    for (let i = Math.max(0, currentIndex - 20); i < currentIndex; i++) {
      if (lines[i].includes('useEffect(')) {
        return true;
      }
    }
    return false;
  }

  private hasEffectCleanup(lines: string[], effectStartIndex: number): boolean {
    let braceCount = 0;
    let inEffect = false;
    
    for (let i = effectStartIndex; i < Math.min(effectStartIndex + 50, lines.length); i++) {
      const line = lines[i];
      
      if (line.includes('useEffect(')) {
        inEffect = true;
      }
      
      if (inEffect) {
        braceCount += (line.match(/{/g) || []).length;
        braceCount -= (line.match(/}/g) || []).length;
        
        if (line.includes('return () =>') || line.includes('return function')) {
          return true;
        }
        
        if (braceCount === 0 && inEffect) {
          break;
        }
      }
    }
    
    return false;
  }
}

/**
 * FE-MEM-005: Closure memory leak
 * 
 * Detects closures that may cause memory leaks by capturing large objects.
 */
export class ClosureMemoryLeakRule extends AbstractMemoryRule {
  getRuleKey(): string { return 'FE-MEM-005'; }
  getName(): string { return 'Closure may cause memory leak'; }

  protected checkMemory(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect patterns that commonly cause closure leaks
      
      // 1. Large object captured in closure
      if (line.match(/useState\s*\(\s*{[^}]*}/)) {
        // Check if state setter is used in useEffect without dependency
        const varName = line.match(/const\s+\[(\w+),\s*set\w+\]/);
        if (varName) {
          const stateVar = varName[1];
          // Look for useEffect that uses this state
          for (let i = index + 1; i < Math.min(index + 100, lines.length); i++) {
            if (lines[i].includes('useEffect(')) {
              // Check next 20 lines for usage of stateVar
              let usesState = false;
              let hasDependency = false;
              
              for (let j = i; j < Math.min(i + 20, lines.length); j++) {
                if (lines[j].includes(stateVar)) {
                  usesState = true;
                }
                if (lines[j].match(/\]\s*\)/) && lines[j].includes(stateVar)) {
                  hasDependency = true;
                }
              }
              
              if (usesState && !hasDependency) {
                issues.push({
                  rule_key: this.getRuleKey(),
                  rule_name: this.getName(),
                  severity: Severity.MINOR,
                  category: this.getCategory(),
                  file_path: filePath,
                  line: lineNum,
                  message: `State '${stateVar}' captured in closure without proper dependency`,
                  evidence: line.trim(),
                  remediation: `Add '${stateVar}' to useEffect dependency array`
                });
              }
              break;
            }
          }
        }
      }

      // 2. Event handler capturing stale closures
      if (line.match(/const\s+\w+\s*=\s*useCallback\s*\(/)) {
        // Check if useCallback has empty dependency array but uses external vars
        const handlerBlock = this.extractFunctionBlock(lines, index);
        if (handlerBlock) {
          const externalVars = handlerBlock.match(/\b(props\.\w+|state\.\w+)\b/g);
          if (externalVars && externalVars.length > 0) {
            // Check dependency array
            const depArrayMatch = line.match(/\[\s*\]/);
            if (depArrayMatch) {
              issues.push({
                rule_key: this.getRuleKey(),
                rule_name: this.getName(),
                severity: Severity.MINOR,
                category: this.getCategory(),
                file_path: filePath,
                line: lineNum,
                message: 'useCallback with empty deps captures stale values',
                evidence: line.trim(),
                remediation: 'Add dependencies to useCallback or remove memoization'
              });
            }
          }
        }
      }
    });

    return issues;
  }

  private extractFunctionBlock(lines: string[], startIndex: number): string | null {
    let block = '';
    let braceCount = 0;
    let started = false;
    
    for (let i = startIndex; i < Math.min(startIndex + 30, lines.length); i++) {
      const line = lines[i];
      block += line + '\n';
      
      if (line.includes('{')) {
        braceCount += (line.match(/{/g) || []).length;
        started = true;
      }
      if (line.includes('}')) {
        braceCount -= (line.match(/}/g) || []).length;
      }
      
      if (started && braceCount === 0) {
        return block;
      }
    }
    
    return null;
  }
}

// Export all rules for easy registration
export const MemoryRules = {
  EventListenerCleanupRule,
  TimerCleanupRule,
  ObserverCleanupRule,
  WebSocketCleanupRule,
  ClosureMemoryLeakRule
};
