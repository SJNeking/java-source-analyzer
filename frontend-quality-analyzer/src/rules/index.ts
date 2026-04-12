/**
 * Default Rules Registry
 *
 * Provides all rules out-of-the-box, mirroring the Java analyzer's AllRules approach.
 * Users can filter via config to enable/disable specific rules.
 */

import { QualityRule } from '../types';

// TypeScript Rules
import { TypeScriptRules } from './typescript/TypeScriptRules';

// React Rules
import { ReactRules } from './react/ReactRules';

// Security Rules
import { SecurityRules } from './security/SecurityRules';

// Vue Rules
import { VueRules } from './vue/VueRules';

// Performance Rules
import { PerformanceRules } from './performance/PerformanceRules';

// Memory Rules
import { MemoryRules } from './memory/MemoryRules';

// Accessibility Rules
import { AccessibilityRules } from './accessibility/AccessibilityRules';

// Architecture Rules
import { ArchitectureRules } from './architecture/ArchitectureRules';

// Styling Rules
import { StylingRules } from './styling/StylingRules';

// Testing Rules
import { TestingRules } from './testing/TestingRules';

// Build Config Rules
import { BuildConfigRules } from './build-config/BuildConfigRules';

// i18n Rules
import { I18nRules } from './i18n/I18nRules';

// Error Handling Rules
import { ErrorHandlingRules } from './error-handling/ErrorHandlingRules';

// State Management Rules
import { StateManagementRules } from './state-management/StateManagementRules';

// Extended Security Rules
import { ExtendedSecurityRules } from './security/ExtendedSecurityRules';

// Event Handling Rules
import { EventHandlingRules } from './event-handling/EventHandlingRules';

// API Design Rules
import { APIDesignRules } from './api-design/APIDesignRules';

// Bundle Optimization Rules
import { BundleOptimizationRules } from './bundle-optimization/BundleOptimizationRules';

// Lifecycle/Timing Rules
import { LifecycleTimingRules } from './lifecycle-timing/LifecycleTimingRules';

// Component Design Rules
import { ComponentDesignRules } from './component-design/ComponentDesignRules';

// Data Validation Rules
import { DataValidationRules } from './data-validation/DataValidationRules';

/**
 * Get all default rules (150+ rules)
 */
export function getAllDefaultRules(): QualityRule[] {
  const rules: QualityRule[] = [];

  // TypeScript: 6 rules
  rules.push(...TypeScriptRules.all());

  // React: 6 rules
  rules.push(...ReactRules.all());

  // Security: 16 rules (6 original + 10 extended)
  rules.push(...SecurityRules.all());
  rules.push(...ExtendedSecurityRules.all());

  // Vue: 10 rules
  rules.push(...VueRules.all());

  // Performance: 10 rules
  rules.push(...PerformanceRules.all());

  // Memory: 8 rules
  rules.push(...MemoryRules.all());

  // Accessibility: 10 rules
  rules.push(...AccessibilityRules.all());

  // Architecture: 34 rules (8 original + 26 new)
  rules.push(...ArchitectureRules.all());

  // Styling: 8 rules
  rules.push(...StylingRules.all());

  // Testing: 6 rules
  rules.push(...TestingRules.all());

  // Build Config: 5 rules
  rules.push(...BuildConfigRules.all());

  // i18n: 5 rules
  rules.push(...I18nRules.all());

  // Error Handling: 8 rules (NEW)
  rules.push(...ErrorHandlingRules.all());

  // State Management: 8 rules (NEW)
  rules.push(...StateManagementRules.all());

  // Event Handling: 6 rules (NEW)
  rules.push(...EventHandlingRules.all());

  // API Design: 8 rules (NEW)
  rules.push(...APIDesignRules.all());

  // Bundle Optimization: 6 rules (NEW)
  rules.push(...BundleOptimizationRules.all());

  // Lifecycle/Timing: 6 rules (NEW)
  rules.push(...LifecycleTimingRules.all());

  // Component Design: 6 rules (NEW)
  rules.push(...ComponentDesignRules.all());

  // Data Validation: 6 rules (NEW)
  rules.push(...DataValidationRules.all());

  return rules;
}
