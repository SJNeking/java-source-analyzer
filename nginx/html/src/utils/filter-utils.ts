/**
 * Java Source Analyzer - Filter Utilities
 * Functions for filtering graph data
 */

import type {
  GraphData,
  GraphNode,
  NodeTypeFilters
} from '../types';
import { Logger } from './logger';

/**
 * Apply node type filters to graph data
 * @param originalData - Original complete graph data
 * @param filters - Node type filter state
 * @returns Filtered graph data
 */
export function applyFilters(
  originalData: GraphData,
  filters: NodeTypeFilters
): GraphData {
  if (!originalData) {
    Logger.error('Cannot apply filters: no data provided');
    return { nodes: [], links: [], framework: '', version: '' } as unknown as GraphData;
  }

  // Filter nodes by enabled types
  const filteredNodes = originalData.nodes.filter(node => {
    const category = node.category as keyof NodeTypeFilters;
    // Default to true if category not in filters
    return filters[category] !== false;
  });

  // Create set of visible node IDs for quick lookup
  const visibleNodeIds = new Set(filteredNodes.map(n => n.id));

  // Filter links (only keep if both endpoints are visible)
  const filteredLinks = originalData.links.filter(link => {
    const sourceId = typeof link.source === 'object'
      ? (link.source as GraphNode).id
      : link.source;
    const targetId = typeof link.target === 'object'
      ? (link.target as GraphNode).id
      : link.target;

    return visibleNodeIds.has(sourceId) && visibleNodeIds.has(targetId);
  });

  Logger.info(
    `Filter applied: ${filteredNodes.length}/${originalData.nodes.length} nodes, ` +
    `${filteredLinks.length}/${originalData.links.length} links`
  );

  return {
    ...originalData,
    nodes: filteredNodes,
    links: filteredLinks
  };
}

/**
 * Toggle a specific node type filter
 * @param filters - Current filter state
 * @param type - Node type to toggle
 * @returns New filter state
 */
export function toggleNodeTypeFilter(
  filters: NodeTypeFilters,
  type: keyof NodeTypeFilters
): NodeTypeFilters {
  return {
    ...filters,
    [type]: !filters[type]
  };
}

/**
 * Reset all filters to enabled
 * @returns Default filter state (all enabled)
 */
export function resetFilters(): NodeTypeFilters {
  return {
    INTERFACE: true,
    ABSTRACT_CLASS: true,
    CLASS: true,
    ENUM: true,
    UTILITY: true,
    EXTERNAL: true
  };
}

/**
 * Enable all filters
 */
export function enableAllFilters(): NodeTypeFilters {
  return resetFilters();
}

/**
 * Disable all filters
 */
export function disableAllFilters(): NodeTypeFilters {
  return {
    INTERFACE: false,
    ABSTRACT_CLASS: false,
    CLASS: false,
    ENUM: false,
    UTILITY: false,
    EXTERNAL: false
  };
}

/**
 * Get count of enabled filters
 */
export function getEnabledFilterCount(filters: NodeTypeFilters): number {
  return Object.values(filters).filter(v => v).length;
}

/**
 * Check if any filter is enabled
 */
export function hasActiveFilters(filters: NodeTypeFilters): boolean {
  return Object.values(filters).some(v => v);
}
