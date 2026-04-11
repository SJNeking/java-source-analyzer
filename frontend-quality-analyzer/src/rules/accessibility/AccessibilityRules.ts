/**
 * Frontend Accessibility (a11y) Rules
 * 
 * Detects accessibility violations based on WCAG 2.1 guidelines.
 * Mirrors backend robustness and compliance rules architecture.
 */

import { QualityIssue, QualityRule, IssueCategory, Severity } from '../../types';

// ==================== Base Class ====================

abstract class AbstractAccessibilityRule implements QualityRule {
  abstract getRuleKey(): string;
  abstract getName(): string;
  
  getCategory(): IssueCategory {
    return 'ACCESSIBILITY';
  }

  check(sourceCode: string, filePath: string): QualityIssue[] {
    // Only analyze files with markup
    if (!filePath.match(/\.(tsx|jsx|vue|html)$/)) {
      return [];
    }

    return this.checkAccessibility(sourceCode, filePath);
  }

  protected abstract checkAccessibility(sourceCode: string, filePath: string): QualityIssue[];
}

// ==================== Rule Implementations ====================

/**
 * FE-A11Y-001: Missing ARIA labels
 * 
 * Detects interactive elements without proper ARIA labels.
 */
export class ARIAComplianceRule extends AbstractAccessibilityRule {
  getRuleKey(): string { return 'FE-A11Y-001'; }
  getName(): string { return 'Interactive element missing ARIA label'; }

  protected checkAccessibility(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect buttons without accessible text
      if (line.includes('<button') && !line.includes('aria-label') && !line.includes('children')) {
        // Check if button has text content
        const hasTextContent = line.match(/>\s*\w+\s*</);
        if (!hasTextContent) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MAJOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'Button element missing accessible text',
            evidence: line.trim(),
            remediation: 'Add aria-label or visible text content'
          });
        }
      }

      // Detect inputs without labels
      if (line.includes('<input') && !line.includes('aria-label') && !line.includes('id=')) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.MAJOR,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'Input element missing label association',
          evidence: line.trim(),
          remediation: 'Add <label htmlFor="..."> or aria-label attribute'
        });
      }

      // Detect images without alt text
      if (line.includes('<img') && !line.includes('alt=')) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.CRITICAL,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'Image missing alt text',
          evidence: line.trim(),
          remediation: 'Add alt attribute: <img src="..." alt="Description" />'
        });
      }

      // Detect decorative images with non-empty alt
      if (line.includes('<img') && line.includes('alt=""') && !line.includes('role="presentation"')) {
        // This is actually correct for decorative images
        // But we can suggest adding role for clarity
      }
    });

    return issues;
  }
}

/**
 * FE-A11Y-002: Keyboard navigation support
 * 
 * Detects interactive elements that may not be keyboard accessible.
 */
export class KeyboardNavigationRule extends AbstractAccessibilityRule {
  getRuleKey(): string { return 'FE-A11Y-002'; }
  getName(): string { return 'Element may not be keyboard accessible'; }

  protected checkAccessibility(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect div/span with click handlers (should be button)
      if ((line.includes('<div') || line.includes('<span')) && 
          (line.includes('onClick') || line.includes('onclick'))) {
        
        const hasKeyboardHandler = line.includes('onKeyDown') || 
                                   line.includes('onKeyPress') ||
                                   line.includes('tabIndex');
        
        if (!hasKeyboardHandler) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MAJOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'Clickable div/span without keyboard support',
            evidence: line.trim(),
            remediation: 'Use <button> element or add onKeyDown handler and tabIndex={0}'
          });
        }
      }

      // Detect elements with tabIndex but negative value
      if (line.includes('tabIndex') && line.includes('tabIndex={-1}') || line.includes('tabindex="-1"')) {
        // This removes from tab order - flag if it's an interactive element
        if (line.includes('onClick') || line.includes('onFocus')) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MINOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'Interactive element removed from tab order',
            evidence: line.trim(),
            remediation: 'Use tabIndex={0} for interactive elements'
          });
        }
      }
    });

    return issues;
  }
}

/**
 * FE-A11Y-003: Color contrast issues
 * 
 * Detects potential color contrast violations (requires heuristic analysis).
 */
export class ColorContrastRule extends AbstractAccessibilityRule {
  getRuleKey(): string { return 'FE-A11Y-003'; }
  getName(): string { return 'Potential color contrast issue'; }

  protected checkAccessibility(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    // Known low-contrast color combinations
    const lowContrastPairs = [
      { fg: '#ffffff', bg: '#f0f0f0', ratio: '1.14:1' },
      { fg: '#cccccc', bg: '#ffffff', ratio: '1.56:1' },
      { fg: '#999999', bg: '#ffffff', ratio: '2.85:1' },
      { fg: '#777777', bg: '#ffffff', ratio: '4.6:1' }  // Just below AA for normal text
    ];

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect inline styles with color definitions
      if (line.includes('style=') && line.includes('color:')) {
        const colorMatch = line.match(/color:\s*(#[0-9a-fA-F]{3,8})/i);
        const bgColorMatch = line.match(/backgroundColor:\s*(#[0-9a-fA-F]{3,8})/i) ||
                            line.match(/background-color:\s*(#[0-9a-fA-F]{3,8})/i);
        
        if (colorMatch && bgColorMatch) {
          const fgColor = colorMatch[1].toLowerCase();
          const bgColor = bgColorMatch[1].toLowerCase();
          
          // Check against known bad combinations
          const badPair = lowContrastPairs.find(pair => 
            pair.fg === fgColor && pair.bg === bgColor
          );
          
          if (badPair) {
            issues.push({
              rule_key: this.getRuleKey(),
              rule_name: this.getName(),
              severity: Severity.MAJOR,
              category: this.getCategory(),
              file_path: filePath,
              line: lineNum,
              message: `Low color contrast ratio: ${badPair.ratio} (minimum 4.5:1 for AA)`,
              evidence: line.trim(),
              remediation: 'Increase contrast between text and background colors'
            });
          }
        }
      }

      // Detect use of light gray text
      if (line.match(/color:\s*#(999|aaa|bbb|ccc)/i)) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.INFO,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'Light gray text may have contrast issues',
          evidence: line.trim(),
          remediation: 'Verify contrast ratio meets WCAG AA standards (4.5:1)'
        });
      }
    });

    return issues;
  }
}

/**
 * FE-A11Y-004: Form accessibility
 * 
 * Detects form elements with accessibility issues.
 */
export class FormAccessibilityRule extends AbstractAccessibilityRule {
  getRuleKey(): string { return 'FE-A11Y-004'; }
  getName(): string { return 'Form element accessibility issue'; }

  protected checkAccessibility(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect forms without submit button
      if (line.includes('<form')) {
        // Look ahead for submit button
        let hasSubmitButton = false;
        for (let i = index; i < Math.min(index + 50, lines.length); i++) {
          if (lines[i].includes('type="submit"') || lines[i].includes("type='submit'")) {
            hasSubmitButton = true;
            break;
          }
        }
        
        if (!hasSubmitButton) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MINOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'Form missing submit button',
            evidence: line.trim(),
            remediation: 'Add <button type="submit"> for keyboard accessibility'
          });
        }
      }

      // Detect required fields without indication
      if (line.includes('required') && !line.includes('aria-required')) {
        issues.push({
          rule_key: this.getRuleKey(),
          rule_name: this.getName(),
          severity: Severity.INFO,
          category: this.getCategory(),
          file_path: filePath,
          line: lineNum,
          message: 'Required field missing aria-required attribute',
          evidence: line.trim(),
          remediation: 'Add aria-required="true" for screen readers'
        });
      }

      // Detect error messages without aria-describedby
      if (line.includes('error') && line.includes('<span') || line.includes('<div')) {
        if (!line.includes('aria-describedby') && !line.includes('role="alert"')) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.INFO,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'Error message may not be announced by screen readers',
            evidence: line.trim(),
            remediation: 'Add role="alert" or aria-describedby to link error to input'
          });
        }
      }
    });

    return issues;
  }
}

/**
 * FE-A11Y-005: Heading hierarchy
 * 
 * Detects improper heading level usage.
 */
export class HeadingHierarchyRule extends AbstractAccessibilityRule {
  getRuleKey(): string { return 'FE-A11Y-005'; }
  getName(): string { return 'Heading hierarchy violation'; }

  protected checkAccessibility(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    let lastHeadingLevel = 0;

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect heading elements
      const headingMatch = line.match(/<h([1-6])/i);
      if (headingMatch) {
        const currentLevel = parseInt(headingMatch[1]);
        
        // Check for skipped levels (e.g., h1 -> h3)
        if (lastHeadingLevel > 0 && currentLevel > lastHeadingLevel + 1) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MINOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: `Heading level skipped: h${lastHeadingLevel} → h${currentLevel}`,
            evidence: line.trim(),
            remediation: `Use h${lastHeadingLevel + 1} instead of h${currentLevel}`
          });
        }
        
        lastHeadingLevel = currentLevel;
      }
    });

    // Check if page starts with h1
    const firstHeading = sourceCode.match(/<h([1-6])/i);
    if (firstHeading && firstHeading[1] !== '1') {
      issues.push({
        rule_key: this.getRuleKey(),
        rule_name: this.getName(),
        severity: Severity.MAJOR,
        category: this.getCategory(),
        file_path: filePath,
        line: 1,
        message: 'Page should start with h1 heading',
        evidence: `First heading is h${firstHeading[1]}`,
        remediation: 'Add <h1> as the main page heading'
      });
    }

    return issues;
  }
}

/**
 * FE-A11Y-006: Focus management
 * 
 * Detects focus management issues in dynamic content.
 */
export class FocusManagementRule extends AbstractAccessibilityRule {
  getRuleKey(): string { return 'FE-A11Y-006'; }
  getName(): string { return 'Focus management issue'; }

  protected checkAccessibility(sourceCode: string, filePath: string): QualityIssue[] {
    const issues: QualityIssue[] = [];
    const lines = sourceCode.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;
      
      // Detect modals/dialogs without focus trap
      if (line.includes('role="dialog"') || line.includes('role="modal"')) {
        if (!sourceCode.includes('focus-trap') && !sourceCode.includes('FocusTrap')) {
          issues.push({
            rule_key: this.getRuleKey(),
            rule_name: this.getName(),
            severity: Severity.MAJOR,
            category: this.getCategory(),
            file_path: filePath,
            line: lineNum,
            message: 'Modal/dialog without focus trap',
            evidence: line.trim(),
            remediation: 'Implement focus trap to keep focus within modal'
          });
        }
      }

      // Detect dynamic content updates without aria-live
      if (line.includes('setState') || line.includes('useState')) {
        // Heuristic: if updating content that users need to know about
        if (line.includes('notification') || line.includes('message') || line.includes('alert')) {
          if (!sourceCode.includes('aria-live')) {
            issues.push({
              rule_key: this.getRuleKey(),
              rule_name: this.getName(),
              severity: Severity.INFO,
              category: this.getCategory(),
              file_path: filePath,
              line: lineNum,
              message: 'Dynamic content update may not be announced',
              evidence: line.trim(),
              remediation: 'Add aria-live="polite" to container for dynamic content'
            });
          }
        }
      }
    });

    return issues;
  }
}

// Export all rules for easy registration
export const AccessibilityRules = {
  ARIAComplianceRule,
  KeyboardNavigationRule,
  ColorContrastRule,
  FormAccessibilityRule,
  HeadingHierarchyRule,
  FocusManagementRule
};
