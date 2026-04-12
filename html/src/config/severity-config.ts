/**
 * Severity Configuration
 *
 * Centralized configuration for quality issue severity levels.
 * Replaces hardcoded SEVERITY_CONFIG objects scattered in views.
 *
 * Usage:
 *   import { SEVERITY } from '../config';
 *   const cfg = SEVERITY.getConfig('CRITICAL');
 */

export const SEVERITY_ORDER = ['ALL', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'] as const;

export type SeverityLevel = typeof SEVERITY_ORDER[number];

export interface SeverityConfig {
  icon: string;
  label: string;
  color: string;
  bg: string;
  order: number;
}

const SEVERITY_DATA: Record<SeverityLevel, SeverityConfig> = {
  ALL: {
    icon: '📊',
    label: '全部',
    color: 'var(--text)',
    bg: 'var(--bg-tertiary)',
    order: 0,
  },
  CRITICAL: {
    icon: '🔴',
    label: '严重',
    color: 'var(--danger)',
    bg: 'rgba(251, 113, 133, 0.1)',
    order: 1,
  },
  MAJOR: {
    icon: '🟠',
    label: '重要',
    color: 'var(--warning)',
    bg: 'rgba(251, 191, 36, 0.1)',
    order: 2,
  },
  MINOR: {
    icon: '🔵',
    label: '次要',
    color: 'var(--accent)',
    bg: 'rgba(56, 189, 248, 0.1)',
    order: 3,
  },
  INFO: {
    icon: '🟢',
    label: '信息',
    color: 'var(--success)',
    bg: 'rgba(74, 222, 128, 0.1)',
    order: 4,
  },
};

export class SeverityManager {
  /**
   * Get configuration for a severity level
   */
  static get(level: SeverityLevel | string): SeverityConfig {
    return SEVERITY_DATA[level as SeverityLevel] || SEVERITY_DATA.INFO;
  }

  /**
   * Get all severity configurations in order
   */
  static getAll(): ReadonlyArray<{ key: SeverityLevel; config: SeverityConfig }> {
    return SEVERITY_ORDER.map(key => ({ key, config: SEVERITY_DATA[key] }));
  }

  /**
   * Get severity levels excluding ALL
   */
  static getFilterable(): ReadonlyArray<Exclude<SeverityLevel, 'ALL'>> {
    return SEVERITY_ORDER.filter(s => s !== 'ALL') as Exclude<SeverityLevel, 'ALL'>[];
  }

  /**
   * Get color for a severity level
   */
  static getColor(level: SeverityLevel | string): string {
    return this.get(level).color;
  }

  /**
   * Get background for a severity level
   */
  static getBackground(level: SeverityLevel | string): string {
    return this.get(level).bg;
  }

  /**
   * Get icon for a severity level
   */
  static getIcon(level: SeverityLevel | string): string {
    return this.get(level).icon;
  }

  /**
   * Get display label for a severity level
   */
  static getLabel(level: SeverityLevel | string): string {
    return this.get(level).label;
  }
}

// Convenience export
export const SEVERITY = SeverityManager;
