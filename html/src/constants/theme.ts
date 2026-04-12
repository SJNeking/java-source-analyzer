/**
 * Theme constants - all hardcoded colors extracted here
 */

export const COLORS = {
  // HTTP methods
  HTTP_GET: '#48bb78',
  HTTP_POST: '#4299e1',
  HTTP_PUT: '#ed8936',
  HTTP_DELETE: '#f56565',
  HTTP_PATCH: '#9f7aea',
  HTTP_ANY: '#a0aec0',

  // Node kinds (mirrors CONFIG.colorMap)
  INTERFACE: '#60a5fa',
  ABSTRACT_CLASS: '#a78bfa',
  CLASS: '#4ade80',
  ENUM: '#fb923c',
  UTILITY: '#94a3b8',
  EXTERNAL: '#94a3b8',

  // Slate palette (used across all views)
  SLATE_50: '#f8fafc',
  SLATE_100: '#f1f5f9',
  SLATE_200: '#e2e8f0',
  SLATE_300: '#cbd5e1',
  SLATE_400: '#94a3b8',
  SLATE_500: '#718096',
  SLATE_600: '#64748b',
  SLATE_700: '#334155',
  SLATE_800: '#1e293b',
  SLATE_900: '#0f172a',
  SLATE_950: '#020617',

  // Semantic
  GREEN: '#48bb78',
  GREEN_LIGHT: '#34d399',
  RED: '#f56565',
  RED_LIGHT: '#fb7185',
  ORANGE: '#ed8936',
  ORANGE_LIGHT: '#fbbf24',
  BLUE: '#4299e1',
  BLUE_LIGHT: '#38bdf8',
  BLUE_200: '#63b3ed',
  PURPLE: '#9f7aea',
  PURPLE_LIGHT: '#a78bfa',
  TEAL: '#22c55e',

  // Backgrounds (rgba)
  BG_CARD_DARK: 'rgba(20, 25, 40, 0.85)',
  BG_CARD_DARKER: 'rgba(20, 25, 40, 0.6)',
  BG_ASSET_CARD: 'rgba(26, 32, 44, 0.5)',
  BG_BADGE_BLUE: 'rgba(56, 189, 248, 0.1)',

  // Borders (rgba)
  BORDER_WHITE_10: 'rgba(255, 255, 255, 0.1)',
  BORDER_WHITE_08: 'rgba(255, 255, 255, 0.08)',
  BORDER_WHITE_40: 'rgba(255, 255, 255, 0.4)',
  BORDER_WHITE_20: 'rgba(255, 255, 255, 0.2)',
  BORDER_RED: 'rgba(245, 101, 101, 0.4)',
  BORDER_GREEN: 'rgba(72, 187, 120, 0.4)',
  BORDER_GREEN_SOFT: 'rgba(72, 187, 120, 0.3)',

  // Severity backgrounds
  SEVERITY_CRITICAL_BG: 'rgba(251, 113, 133, 0.1)',
  SEVERITY_MAJOR_BG: 'rgba(251, 191, 36, 0.1)',
  SEVERITY_MINOR_BG: 'rgba(56, 189, 248, 0.1)',
  SEVERITY_INFO_BG: 'rgba(74, 222, 128, 0.1)',

  // Chart / graph colors
  CHART_AXIS_LINE: 'rgba(255, 255, 255, 0.15)',
  CHART_GRID_LINE: 'rgba(255, 255, 255, 0.08)',
  CHART_TOOLTIP_BG: 'rgba(15, 23, 42, 0.95)',
  CHART_EXPORT_BG: '#0b0f19',

  // Force Graph
  LINK_COLOR: '#94a3b8',
  LABEL_COLOR: '#e2e8f0',

  // Layer / architecture colors
  LAYER_CONTROLLER: '#3b82f6',
  LAYER_SERVICE: '#10b981',
  LAYER_REPOSITORY: '#8b5cf6',
  LAYER_EXTERNAL: '#6b7280',

  // Level / depth border colors (Sankey, call chain)
  LEVEL_0_BORDER: '#1e40af',
  LEVEL_1_BORDER: '#059669',
  LEVEL_2_BORDER: '#7c3aed',
  LEVEL_3_BORDER: '#6b7280',

  // Violation / risk colors
  VIOLATION_RED: '#ef4444',
  VIOLATION_RED_LIGHT: '#fb7185',
  RISK_AMBER: '#f59e0b',
  CALL_BLUE: '#3b82f6',

  // Modifier colors
  MODIFIER_PUBLIC: '#34d399',
  MODIFIER_STATIC: '#a78bfa',

  // Export image
  PIXEL_RATIO: 2,
} as const;

// Re-export commonly used subsets for direct access
export const C = COLORS;

export const HTTP_METHOD_ICONS: Record<string, string> = {
  GET: '🟢',
  POST: '🔵',
  PUT: '🟡',
  DELETE: '🔴',
  PATCH: '🟠',
  ANY: '⚪',
} as const;

export const LAYER_ICONS: Record<string, string> = {
  CONTROLLER: '🌐',
  SERVICE: '⚙️',
  REPOSITORY: '🗄️',
  ENTITY: '📦',
  CONFIG: '⚙️',
  UTIL: '🔧',
} as const;

export const ASSET_TYPE_ICONS: Record<string, string> = {
  maven_pom: '📦',
  yaml_config: '⚙️',
  properties_config: '⚙️',
  sql_script: '🗄️',
  mybatis_mapper: '🔗',
  dockerfile: '🐳',
  docker_compose: '🐳',
  shell_script: '📜',
  log_config: '📝',
  markdown_doc: '📖',
  modules: '📦',
  scan_summary: '📊',
  errors: '❌',
} as const;

export const KIND_ICONS: Record<string, string> = {
  INTERFACE: '🔵',
  ABSTRACT_CLASS: '🟣',
  CLASS: '🟢',
  ENUM: '🔶',
  UTILITY: '⚪',
  EXTERNAL: '📄',
} as const;
