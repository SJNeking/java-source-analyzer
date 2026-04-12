/**
 * Java Source Analyzer - DOM Helpers
 * Utility functions for DOM manipulation and UI updates
 */

import type { GraphData, GraphNode, ProjectEntry } from '../types';
import { Logger } from './logger';

/**
 * Populate a <select> element with project options
 * @param selectorId - ID of the <select> element
 * @param projects - Array of project entries
 * @param onChange - Callback when selection changes
 */
export function populateProjectSelector(
  selectorId: string,
  projects: ProjectEntry[],
  onChange: (filename: string) => void
): void {
  const select = document.getElementById(selectorId);

  if (!select) {
    Logger.warning(`Selector element not found: ${selectorId}`);
    return;
  }

  if (!(select instanceof HTMLSelectElement)) {
    Logger.error(`Element is not a <select>: ${selectorId}`);
    return;
  }

  // Clear existing options
  select.innerHTML = '';

  // Add project options
  projects.forEach(project => {
    const option = document.createElement('option');
    option.value = project.file;
    option.textContent = project.name;
    select.appendChild(option);
  });

  // Attach change event listener
  select.addEventListener('change', (event: Event) => {
    const target = event.target as HTMLSelectElement;
    if (target && target.value) {
      onChange(target.value);
    }
  });

  Logger.success(`Populated project selector with ${projects.length} projects`);
}

/**
 * Update statistics display
 * @param data - Graph data
 */
export function updateStatsDisplay(data: GraphData): void {
  // Map to current HTML stat card IDs
  const statAssets = document.getElementById('stat-assets');
  const statIssues = document.getElementById('stat-issues');
  const statLoc = document.getElementById('stat-loc');
  const statComplexity = document.getElementById('stat-complexity');

  if (statAssets && statAssets instanceof HTMLElement) {
    statAssets.textContent = data.nodes.length.toString();
  }

  if (statIssues && statIssues instanceof HTMLElement) {
    // Count nodes with issues (categories that indicate problems)
    const issueCount = data.nodes.filter(n =>
      n.category === 'ISSUE' || n.category === 'ERROR' || n.category === 'WARNING'
    ).length;
    statIssues.textContent = issueCount > 0 ? issueCount.toString() : '0';
  }

  if (statLoc && statLoc instanceof HTMLElement) {
    // Sum lines of code from node data if available
    const totalLoc = data.nodes.reduce((sum, n) => sum + (n.loc || 0), 0);
    statLoc.textContent = totalLoc > 0 ? totalLoc.toLocaleString() : '-';
  }

  if (statComplexity && statComplexity instanceof HTMLElement) {
    // Average cyclomatic complexity if available
    const nodesWithComplexity = data.nodes.filter(n => n.complexity != null);
    if (nodesWithComplexity.length > 0) {
      const avgComplexity = nodesWithComplexity.reduce((sum, n) => sum + (n.complexity || 0), 0) / nodesWithComplexity.length;
      statComplexity.textContent = avgComplexity.toFixed(1);
    } else {
      statComplexity.textContent = '-';
    }
  }
}

/**
 * Update zoom display
 * @param zoomPercentage - Zoom level as percentage
 */
export function updateZoomDisplay(zoomPercentage: number): void {
  const statsZoom = document.getElementById('statsZoom');
  if (statsZoom && statsZoom instanceof HTMLElement) {
    statsZoom.textContent = `${zoomPercentage}%`;
  }
}

/**
 * Show or hide loading overlay
 * @param show - Whether to show or hide
 * @param text - Loading text to display
 */
export function toggleLoadingOverlay(show: boolean, text: string = 'Loading...'): void {
  const overlay = document.getElementById('loading');

  if (!overlay || !(overlay instanceof HTMLElement)) return;

  if (show) {
    overlay.classList.remove('hidden');
    // Update the text content (second child div in the loading overlay)
    const textEl = overlay.querySelector('div[style*="font-size"], div:not(.spinner)');
    if (textEl && textEl instanceof HTMLElement) {
      textEl.textContent = text;
    }
  } else {
    overlay.classList.add('hidden');
  }
}

/**
 * Show error message in loading overlay
 * @param message - Error message
 */
export function showError(message: string): void {
  const overlay = document.getElementById('loading');

  if (overlay && overlay instanceof HTMLElement) {
    // Update text to error message
    const textEl = overlay.querySelector('div[style*="font-size"], div:not(.spinner)');
    if (textEl && textEl instanceof HTMLElement) {
      textEl.textContent = `Error: ${message}`;
      textEl.style.color = 'var(--danger)';
    }
    // Keep overlay visible briefly so user sees the error
    setTimeout(() => {
      overlay.classList.add('hidden');
    }, 3000);
  }

  // Also show as toast
  showToast(message, 'error');
}

/**
 * Show a toast notification
 */
export function showToast(message: string, type: 'success' | 'warning' | 'error' | 'info' = 'info', durationMs: number = 5000): void {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const icons: Record<string, string> = { success: '✅', warning: '⚠️', error: '❌', info: 'ℹ️' };
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<span>${icons[type] || 'ℹ️'}</span><span>${message}</span>`;
  container.appendChild(toast);

  // Auto remove
  setTimeout(() => {
    toast.classList.add('toast-out');
    setTimeout(() => toast.remove(), 200);
  }, durationMs);
}

/**
 * Search and highlight nodes by keyword
 * @param keyword - Search keyword
 * @param nodes - Array of graph nodes
 * @param chart - ECharts instance (passed from view)
 */
export function searchNodes(
  keyword: string,
  nodes: GraphNode[],
  chart: { dispatchAction: (action: unknown) => void }
): void {
  if (!chart || !keyword || keyword.trim() === '') {
    chart?.dispatchAction({
      type: 'downplay',
      seriesIndex: 0
    });
    return;
  }

  // Clear previous highlights
  chart.dispatchAction({
    type: 'downplay',
    seriesIndex: 0
  });

  // Find matching nodes
  const searchTerm = keyword.toLowerCase();
  const matchedIndices: number[] = [];

  nodes.forEach((node, index) => {
    if (node.name.toLowerCase().includes(searchTerm) ||
        node.id.toLowerCase().includes(searchTerm)) {
      matchedIndices.push(index);
    }
  });

  // Highlight matches
  matchedIndices.forEach(index => {
    chart.dispatchAction({
      type: 'highlight',
      seriesIndex: 0,
      dataIndex: index
    });
  });

  Logger.info(`Found ${matchedIndices.length} matching nodes`);
}

/**
 * Check if dataset is large
 * @param nodes - Array of nodes
 * @param threshold - Threshold for large dataset (default: 1000)
 * @returns True if dataset is large
 */
export function isLargeDataset(nodes: unknown[], threshold: number = 1000): boolean {
  return nodes.length > threshold;
}

/**
 * Clean description text by removing Javadoc markers
 * @param description - Raw description
 * @param maxLength - Maximum length (default: 30)
 * @returns Cleaned description
 */
export function cleanDescription(description: string, maxLength: number = 30): string {
  if (!description) return '';

  let cleaned = description
    .replace(/@\w+/g, '')           // Remove @tags
    .replace(/\{.*?\}/g, '')        // Remove {...}
    .replace(/<.*?>/g, '')          // Remove <...>
    .replace(/\s+/g, ' ')           // Normalize whitespace
    .trim();

  if (cleaned.length > maxLength) {
    cleaned = cleaned.substring(0, maxLength) + '...';
  }

  return cleaned;
}

/**
 * Get node color based on category
 * @param node - Graph node
 * @returns Color string
 */
export function getNodeColor(node: GraphNode): string {
  return node.color || '#c9d1d9';
}
