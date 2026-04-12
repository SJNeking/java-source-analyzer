/**
 * Theme Configuration
 * 
 * Centralized color, spacing, and style constants.
 * Eliminates hard-coded values across all views.
 */

export const THEME = {
  // Color palette for asset types
  colors: {
    INTERFACE: '#60a5fa',
    ABSTRACT_CLASS: '#a78bfa',
    CLASS: '#4ade80',
    ENUM: '#fb923c',
    UTILITY: '#9ca3af',
    EXTERNAL: '#78716c',
    ANNOTATION: '#f6ad55',
    
    // Highlight colors
    HIGHLIGHT_KEYWORD: '#c084fc',
    HIGHLIGHT_TYPE: 'var(--code-type)',
    HIGHLIGHT_COMMENT: 'var(--code-comment)',
    HIGHLIGHT_STRING: 'var(--danger)',
    HIGHLIGHT_ANNOTATION: 'var(--warning)',
    
    // Border colors (fallbacks)
    BORDER_LIGHT: 'rgba(255,255,255,0.05)',
    BORDER_NORMAL: 'rgba(255,255,255,0.1)',
    BORDER_FOCUS: 'var(--accent)',
    
    // Background colors (fallbacks)
    BG_PRIMARY: '#0f172a',
    BG_SECONDARY: 'var(--bg-secondary)',
    BG_TERTIARY: 'var(--bg-tertiary)',
    CODE_BG: '#020617',
    BACKDROP: 'rgba(0,0,0,0.5)',
    
    // Text colors
    TEXT_PRIMARY: 'var(--text-primary)',
    TEXT_SECONDARY: 'var(--text-secondary)',
    TEXT_MUTED: 'var(--text-muted)',
    TEXT_DIM: 'var(--text-dim)',
  },
  
  // Spacing scale
  spacing: {
    XS: '4px',
    SM: '6px',
    MD: '8px',
    LG: '10px',
    XL: '12px',
    '2XL': '14px',
    '3XL': '16px',
    '4XL': '20px',
    '5XL': '24px',
    '6XL': '28px',
  },
  
  // Font sizes
  fontSize: {
    XS: '10px',
    SM: '11px',
    MD: '12px',
    LG: '13px',
    XL: '14px',
    '2XL': '18px',
  },
  
  // Font weights
  fontWeight: {
    NORMAL: '400',
    MEDIUM: '500',
    SEMIBOLD: '600',
    BOLD: '700',
  },
  
  // Border radius
  borderRadius: {
    SM: '4px',
    MD: '6px',
    LG: '8px',
    FULL: '50%',
  },
  
  // Shadows
  shadows: {
    PANEL: '-10px 0 40px rgba(0,0,0,0.8)',
    TOOLTIP: '0 8px 24px rgba(0,0,0,0.5)',
    CARD: '0 2px 8px rgba(0,0,0,0.3)',
  },
  
  // Transitions
  transitions: {
    FAST: '0.15s',
    NORMAL: '0.25s',
    SLOW: '0.35s',
    EASING: 'cubic-bezier(0.4, 0, 0.2, 1)',
  },
  
  // Z-index layers
  zIndex: {
    DROPDOWN: 1000,
    STICKY: 1020,
    FIXED: 1030,
    MODAL_BACKDROP: 9998,
    MODAL: 9999,
    POPOVER: 1060,
    TOOLTIP: 1070,
  },
} as const;

// Export commonly used subsets for convenience
export const COLORS = THEME.colors;
export const SPACING = THEME.spacing;
export const FONT = {
  size: THEME.fontSize,
  weight: THEME.fontWeight,
};
export const RADIUS = THEME.borderRadius;
