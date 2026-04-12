/**
 * Java Source Analyzer - Utility Module Exports
 */

export { Logger } from './logger';
export {
  loadProjectsIndex,
  loadAnalysisResult,
  loadProjectData,
  transformToGraph,
  extractShortName
} from './data-loader';
export {
  populateProjectSelector,
  updateStatsDisplay,
  updateZoomDisplay,
  toggleLoadingOverlay,
  showError,
  showToast,
  searchNodes,
  isLargeDataset,
  cleanDescription,
  getNodeColor
} from './dom-helpers';
export {
  applyFilters,
  toggleNodeTypeFilter,
  resetFilters,
  enableAllFilters,
  disableAllFilters,
  getEnabledFilterCount,
  hasActiveFilters
} from './filter-utils';
export { ArchitectureDetector } from './architecture-detector';
export { eventBus, bindEventToDOM } from './event-bus';
export { safeSetInnerHTML, createSafeElement, renderTemplate, appendSafeHTML } from './safe-dom';
export { DataValidator } from './data-validator';
