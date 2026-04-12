/**
 * Java Source Analyzer - Configuration
 * Centralized configuration constants
 */

import type { AppConfig, AssetKind } from '../types';

// Export server configuration
export { SERVER_CONFIG, getWebSocketUrl, getApiUrl } from './server-config';

// Export theme configuration
export { THEME, COLORS, SPACING, FONT, RADIUS } from './theme';

// Export severity configuration
export { SEVERITY, SEVERITY_ORDER, SeverityManager, type SeverityConfig, type SeverityLevel } from './severity-config';

/**
 * Application configuration
 */
export const CONFIG: AppConfig = {
  // API paths (relative to website root)
  dataPath: '/data/',
  projectsIndexUrl: '/data/projects.json',

  // Performance thresholds
  largeDatasetThreshold: 1000,

  // Color mapping for asset kinds
  colorMap: {
    'INTERFACE': '#60a5fa',
    'ABSTRACT_CLASS': '#a78bfa',
    'CLASS': '#4ade80',
    'ENUM': '#fb923c',
    'UTILITY': '#9ca3af',
    'EXTERNAL': '#78716c',
    'ANNOTATION': '#f6ad55'
  } as Record<AssetKind, string>
};

/**
 * Default node type filter state
 */
export const DEFAULT_NODE_TYPE_FILTERS = {
  INTERFACE: true,
  ABSTRACT_CLASS: true,
  CLASS: true,
  ENUM: true,
  UTILITY: true,
  EXTERNAL: false
};
