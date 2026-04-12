/**
 * CSS Class Names
 *
 * Centralized CSS class names used across all views.
 * Eliminates hardcoded class string literals scattered in templates.
 *
 * Usage:
 *   import { CLS } from '../constants';
 *   el('div', { className: CLS.CARD_GRID })
 */
export const CLS = {
  // Layout
  CARD_GRID: 'qi-card-grid',
  STAT_CARD: 'qi-stat-card',
  STAT_VALUE: 'qi-stat-value',
  STAT_LABEL: 'qi-stat-label',
  LIST: 'qi-list',
  LIST_ITEM: 'qi-item',
  LIST_ITEM_EXPANDED: 'qi-item expanded',
  ITEM_HEADER: 'qi-item-header',
  MSG: 'qi-msg',
  META: 'qi-meta',
  CODE_BLOCK: 'qi-code-block',
  CODE_HEADER: 'qi-code-header',
  CODE_CONTENT: 'qi-code-content',
  CODE_PRE: 'qi-code-pre',

  // Badges
  BADGE: 'qi-badge',
  CLASS_NAME: 'qi-class-name',
  EXPAND_ICON: 'qi-expand-icon',
  RULE: 'qi-rule',
  METHOD: 'qi-method',
  LINE: 'qi-line',

  // Explorer
  EXPLORER_LAYOUT: 'explorer-layout',
  EXPLORER_TREE: 'explorer-tree',
  TREE_HEADER: 'tree-header',
  TREE_VIEW_TOGGLES: 'tree-view-toggles',
  VIEW_TOGGLE: 'view-toggle',
  VIEW_TOGGLE_ACTIVE: 'view-toggle active',
  TREE_SEARCH: 'tree-search',
  TREE_CONTENT: 'tree-content',
  EXPLORER_MAIN: 'explorer-main',
  VIEWER_TABS: 'viewer-tabs',
  VIEWER_TAB: 'vtab',
  VIEWER_TAB_ACTIVE: 'vtab active',
  VIEWER_PANELS: 'viewer-panels',
  PANEL: 'vpanel',
  PANEL_ACTIVE: 'vpanel active',
  EXPLORER_CONTEXT: 'explorer-context',
  CONTEXT_HEADER: 'ctx-header',
  CONTEXT_BODY: 'ctx-body',

  // Tree / Package
  PKG_GROUP: 'pkg-group',
  PKG_SUMMARY: 'pkg-summary',
  PKG_ICON: 'pkg-ico',
  PKG_NAME: 'pkg-nm',
  PKG_COUNT: 'pkg-ct',
  PKG_BODY: 'pkg-body',
  CLASS_GROUP: 'cls-group',
  CLASS_SUMMARY: 'cls-summary',
  CLASS_ICON: 'cls-ico',
  CLASS_NAME_TAG: 'cls-nm',
  CLASS_KIND: 'cls-kind',
  CLASS_BODY: 'cls-body',
  METHOD_ITEM: 'mtd-item',
  METHOD_NAME: 'mtd-nm',
  METHOD_BADGE: 'mtd-badge',
  METHOD_BADGE_PUB: 'mtd-badge pub',
  METHOD_BADGE_ST: 'mtd-badge st',

  // Welcome / Dashboard
  WELCOME: 'welcome',
  WELCOME_TITLE: 'welcome-title',
  WELCOME_SUB: 'welcome-sub',
  STATS_ROW: 'stats-row',
  STAT: 'stat',
  STAT_VALUE_SHORT: 'stat-v',
  STAT_LABEL_SHORT: 'stat-l',
  LAYERS_SECTION: 'layers-section',
  LAYERS_GRID: 'layers-grid',
  LAYER_CARD: 'layer-card',
  LAYER_ICON: 'layer-ico',
  LAYER_NAME: 'layer-nm',
  LAYER_COUNT: 'layer-ct',
  QUALITY_GATE: 'qgate',
  QUALITY_GATE_PASS: 'qgate pass',
  QUALITY_GATE_FAIL: 'qgate fail',

  // Quick Access
  QUICK_ACCESS: 'quick-access',
  QA_SECTION: 'qa-section',
  QA_ITEM: 'qa-item',

  // UML
  UML_WRAP: 'uml-wrap',
  UML_HEADER: 'uml-hdr',
  UML_TITLE: 'uml-title',
  UML_KIND: 'uml-kind',
  UML_BODY: 'uml-body',
  UML_PRE: 'uml-pre',

  // Code View
  CODE_VIEW: 'code-view',
  CODE_HEADER_BAR: 'code-hdr',
  CODE_BUTTON: 'code-btn',
  CODE_BODY: 'code-body',
  CODE_NUMBERS: 'code-nums',
  CODE_LINE_NUMBER: 'lnum',
  CODE_LINE: 'cline',
  CODE_KEYWORD: 'code-keyword',
  CODE_TYPE: 'code-type',
  CODE_COMMENT: 'code-comment',
  CODE_STRING: 'code-string',

  // Context Panel
  CONTEXT_SECTION: 'ctx-section',
  CONTEXT_STATS: 'ctx-stats',
  CONTEXT_STAT: 'ctx-st',
  QUALITY_LIST: 'q-list',
  QUALITY_ITEM: 'q-item',
  METHOD_SIGNATURE: 'mtd-sig',
  METHOD_MODIFIERS: 'mtd-mods',
  CALL_LIST: 'call-list',
  CALL_ITEM: 'call-item',
  CALL_CLASS: 'call-cls',
  CALL_METHOD: 'call-mtd',
  CALL_LINE: 'call-line',

  // Empty State
  EMPTY_STATE: 'empty-state',
  EMPTY_ICON: 'empty-state-icon',
  EMPTY_TITLE: 'empty-state-title',
  EMPTY_DESC: 'empty-state-desc',
  EMPTY_CENTER: 'empty',
  EMPTY_ICON_SIMPLE: 'empty-icon',

  // Metrics
  METRICS_CARD_GRID: 'metrics-card-grid',
  METRICS_STAT_CARD: 'metrics-stat-card',
  METRICS_STAT_ICON: 'metrics-stat-icon',
  METRICS_STAT_VALUE: 'metrics-stat-value',
  METRICS_STAT_LABEL: 'metrics-stat-label',
  METRICS_CHART: 'metrics-chart',
  METRICS_CHART_TITLE: 'metrics-chart-title',
  METRICS_CHART_CONTAINER: 'metrics-chart-container',

  // Common
  HIDDEN: 'hidden',
  ACTIVE: 'active',
} as const;
