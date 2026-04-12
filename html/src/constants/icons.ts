/**
 * UI Icons
 *
 * Centralized icon emoji/unicode mappings.
 * Eliminates scattered icon strings in templates.
 *
 * Usage:
 *   import { ICON } from '../constants';
 *   ICON.SEVERITY.CRITICAL → '🔴'
 */

export const ICON = {
  // Severity icons
  SEVERITY: {
    CRITICAL: '🔴',
    MAJOR: '🟠',
    MINOR: '🔵',
    INFO: '🟢',
    ALL: '📊',
  },

  // Asset kind icons
  KIND: {
    INTERFACE: '🔵',
    ABSTRACT_CLASS: '🟣',
    CLASS: '🟢',
    ENUM: '🔶',
    UTILITY: '⚪',
    EXTERNAL: '📄',
    ANNOTATION: '🏷️',
  },

  // View / Section icons
  SECTION: {
    GRAPH: '🕸️',
    QUALITY: '📊',
    FRONTEND_QUALITY: '🎨',
    METRICS: '📈',
    EXPLORER: '🔍',
    RELATIONS: '🔗',
    ASSETS: '📦',
    ARCHITECTURE: '🏛️',
    METHOD_CALL: '📞',
    CALL_CHAIN: '⛓️',
    API: '🌐',
  },

  // UI icons
  UI: {
    SEARCH: '🔍',
    COPY: '📋',
    DOWNLOAD: '📥',
    ZOOM_IN: '🔍+',
    ZOOM_OUT: '🔍-',
    RESET: '🔄',
    EXPAND: '▼',
    COLLAPSE: '▲',
    CHECK: '✅',
    WARNING: '⚠️',
    ERROR: '❌',
    INFO: 'ℹ️',
    LOADING: '⏳',
  },

  // Code / Language icons
  CODE: {
    JAVA: '☕',
    XML: '📄',
    YAML: '⚙️',
    PROPERTIES: '⚙️',
    SQL: '🗄️',
    SHELL: '📜',
    DOCKER: '🐳',
    MARKDOWN: '📖',
  },

  // HTTP method icons
  HTTP: {
    GET: '🟢',
    POST: '🔵',
    PUT: '🟡',
    DELETE: '🔴',
    PATCH: '🟠',
    ANY: '⚪',
  },

  // Architecture layer icons
  LAYER: {
    CONTROLLER: '🌐',
    SERVICE: '⚙️',
    REPOSITORY: '🗄️',
    ENTITY: '📦',
    CONFIG: '⚙️',
    UTIL: '🔧',
    DOMAIN: '🧱',
    APPLICATION: '📱',
    INFRASTRUCTURE: '🏗️',
    MODEL: '📋',
    VIEW: '🖼️',
  },

  // Asset type icons (for project assets)
  ASSET_TYPE: {
    MAVEN_POM: '📦',
    YAML_CONFIG: '⚙️',
    PROPERTIES_CONFIG: '⚙️',
    SQL_SCRIPT: '🗄️',
    MYBATIS_MAPPER: '🔗',
    DOCKERFILE: '🐳',
    DOCKER_COMPOSE: '🐳',
    SHELL_SCRIPT: '📜',
    LOG_CONFIG: '📝',
    MARKDOWN_DOC: '📖',
    MODULES: '📦',
    SCAN_SUMMARY: '📊',
    ERRORS: '❌',
  },

  // Expand/collapse state
  EXPAND: {
    OPEN: '▲',
    CLOSED: '▼',
  },

  // Link / edge icons
  LINK: {
    ARROW: '➔',
    CHAIN: '⛓️',
  },

  // Tooltip icons
  TOOLTIP: {
    METHODS: '📊',
    FIELDS: '📝',
    DEPS: '🔗',
    NO_DESC: '暂无描述',
    NO_ISSUES: '✅ 无问题',
  },

  // Call chain link type icons
  CALL_LINK: {
    VIOLATION: '⚠️ 架构违规',
    RISK: '🔥 高风险调用',
    NORMAL: '📞 正常调用',
  },

  // Misc
  MISC: {
    BULLET: '•',
    CROSS: '✕',
  },
} as const;
