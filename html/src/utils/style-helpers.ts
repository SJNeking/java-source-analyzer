/**
 * Style Helpers - Centralized inline style factories
 *
 * Eliminates scattered style objects with hardcoded colors/sizes across views.
 * Each factory returns a typed Partial<CSSStyleDeclaration>.
 */

import { COLORS as C } from '../constants/theme';

// =========================================================================
// Color factories
// =========================================================================

export const Style = {
  // Slate color palette
  slate: {
    50:  '#f8fafc',
    100: '#f1f5f9',
    200: '#e2e8f0',
    300: '#cbd5e1',
    400: '#94a3b8',
    500: '#718096',
    600: '#64748b',
    700: '#334155',
    800: '#1e293b',
    900: '#0f172a',
    950: '#020617',
  },

  // Semantic colors
  red:    '#f56565',
  redLt:  '#fb7185',
  orange: '#ed8936',
  amber:  '#f59e0b',
  green:  '#48bb78',
  greenLt:'#34d399',
  teal:   '#22c55e',
  blue:   '#4299e1',
  blueLt: '#38bdf8',
  blue200:'#63b3ed',
  blueCall:'#3b82f6',
  purple: '#9f7aea',
  purpleLt:'#a78bfa',
  violet: '#8b5cf6',
  gray:   '#6b7280',
  grayLt: '#a0aec0',

  // Background rgba values
  bg: {
    cardDark:   'rgba(20, 25, 40, 0.85)',
    cardDarker: 'rgba(20, 25, 40, 0.6)',
    assetCard:  'rgba(26, 32, 44, 0.5)',
    badgeBlue:  'rgba(56, 189, 248, 0.1)',
    sevCritical:'rgba(251, 113, 133, 0.1)',
    sevMajor:   'rgba(251, 191, 36, 0.1)',
    sevMinor:   'rgba(56, 189, 248, 0.1)',
    sevInfo:    'rgba(74, 222, 128, 0.1)',
  },

  // Border rgba values
  border: {
    white10: 'rgba(255, 255, 255, 0.1)',
    white08: 'rgba(255, 255, 255, 0.08)',
    white40: 'rgba(255, 255, 255, 0.4)',
    white20: 'rgba(255, 255, 255, 0.2)',
    red:     'rgba(245, 101, 101, 0.4)',
    green:   'rgba(72, 187, 120, 0.4)',
    greenSoft:'rgba(72, 187, 120, 0.3)',
  },

  // =========================================================================
  // Style factory functions
  // =========================================================================

  // Card style (used across multiple views)
  card: (opts?: { background?: string; border?: string; borderRadius?: string; padding?: string }): Partial<CSSStyleDeclaration> => ({
    background: opts?.background || Style.bg.cardDarker,
    border: `1px solid ${opts?.border || Style.border.white10}`,
    borderRadius: opts?.borderRadius || '8px',
    padding: opts?.padding || '12px',
    cursor: 'pointer',
    transition: 'all 0.2s',
  }),

  // Stat value style
  statValue: (color?: string): Partial<CSSStyleDeclaration> => ({
    color: color || Style.slate[50],
  }),

  // Label / muted text style
  muted: (size?: string): Partial<CSSStyleDeclaration> => ({
    fontSize: size || '11px',
    color: Style.slate[600],
  }),

  // Monospace code style
  mono: (color?: string, size?: string): Partial<CSSStyleDeclaration> => ({
    fontFamily: "'Courier New', monospace",
    fontSize: size || '12px',
    color: color || Style.slate[300],
  }),

  // Flex row with gap
  flexRow: (gap?: string): Partial<CSSStyleDeclaration> => ({
    display: 'flex',
    alignItems: 'center',
    gap: gap || '8px',
  }),

  // Grid with auto-fill columns
  grid: (minWidth?: string, gap?: string): Partial<CSSStyleDeclaration> => ({
    display: 'grid',
    gridTemplateColumns: `repeat(auto-fill, minmax(${minWidth || '240px'}, 1fr))`,
    gap: gap || '10px',
  }),

  // Section title style
  sectionTitle: (size?: string): Partial<CSSStyleDeclaration> => ({
    fontSize: size || '11px',
    color: Style.slate[600],
    marginBottom: '6px',
    fontWeight: '600',
  }),

  // Value box style (info/description box)
  valueBox: (): Partial<CSSStyleDeclaration> => ({
    padding: '10px',
    background: 'var(--bg-tertiary)',
    borderRadius: '6px',
    border: '1px solid var(--border)',
  }),

  // Severity badge style
  severityBadge: (severity: string): Partial<CSSStyleDeclaration> => {
    const colors: Record<string, { bg: string; color: string }> = {
      CRITICAL: { bg: Style.bg.sevCritical, color: Style.redLt },
      MAJOR:    { bg: Style.bg.sevMajor,    color: Style.amber },
      MINOR:    { bg: Style.bg.sevMinor,    color: Style.blueLt },
      INFO:     { bg: Style.bg.sevInfo,     color: Style.teal },
    };
    const c = colors[severity] || { bg: 'var(--bg-tertiary)', color: Style.slate[400] };
    return {
      background: c.bg,
      color: c.color,
      padding: '2px 4px',
      borderRadius: '3px',
    };
  },

  // HTTP method color
  httpMethod: (method: string): string => {
    const map: Record<string, string> = {
      GET: Style.green, POST: Style.blue, PUT: Style.orange,
      DELETE: Style.red, PATCH: Style.purple, ANY: Style.grayLt,
    };
    return map[method] || Style.grayLt;
  },

  // HTTP method icon
  httpIcon: (method: string): string => {
    const map: Record<string, string> = {
      GET: '🟢', POST: '🔵', PUT: '🟡', DELETE: '🔴', PATCH: '🟠', ANY: '⚪',
    };
    return map[method] || '⚪';
  },

  // Layer color (controller/service/repository/external)
  layerColor: (layer: string): string => {
    const map: Record<string, string> = {
      controller: Style.blueCall,
      service:    Style.greenLt,
      repository: Style.violet,
      external:   Style.gray,
    };
    return map[layer] || Style.gray;
  },

  // Level border color (Sankey depth levels)
  levelBorder: (depth: number): string => {
    const map: Record<number, string> = {
      0: '#1e40af',
      1: '#059669',
      2: '#7c3aed',
      3: Style.gray,
    };
    return map[depth] || Style.gray;
  },
};

export default Style;
