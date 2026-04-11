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

/**
 * Get all default rules (100+ rules)
 */
export function getAllDefaultRules(): QualityRule[] {
  const rules: QualityRule[] = [];

  // TypeScript: 12 rules
  rules.push(...TypeScriptRules.all());

  // React: 12 rules
  rules.push(...ReactRules.all());

  // Security: 12 rules
  rules.push(...SecurityRules.all());

  // Vue: 10 rules
  rules.push(...VueRules.all());

  // Performance: 10 rules
  rules.push(...PerformanceRules.all());

  // Memory: 8 rules
  rules.push(...MemoryRules.all());

  // Accessibility: 10 rules
  rules.push(...AccessibilityRules.all());

  // Architecture: 8 rules
  rules.push(...ArchitectureRules.all());

  // Styling: 8 rules
  rules.push(...StylingRules.all());

  // Testing: 6 rules
  rules.push(...TestingRules.all());

  // Build Config: 5 rules
  rules.push(...BuildConfigRules.all());

  // i18n: 5 rules
  rules.push(...I18nRules.all());

  return rules;
}
