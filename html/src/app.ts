/**
 * Java Source Analyzer - Main Application (Refactored)
 *
 * Architecture:
 * 1. Unified Data Store (ApplicationState)
 * 2. Data Fetching Service (DataFetcherService)
 * 3. Declarative View Switching via ViewManager
 * 4. Event-Driven Communication via EventBus
 * 5. Lazy-loaded views (code splitting)
 * 6. No global window.* assignments
 */

import type { GraphData, Asset, ViewType } from './types';
import {
  loadProjectsIndex,
  loadProjectData,
  populateProjectSelector,
  updateStatsDisplay,
  toggleLoadingOverlay,
  showError,
  Logger,
  loadUnifiedReport
} from './utils';
import { applyFilters } from './utils/filter-utils';
import { appState, viewManager } from './state';
import { CONFIG } from './config';
import { dataFetcher } from './services';
import { eventBus } from './utils/event-bus';

// View renderer interface (matches ViewManager's expectation)
import type { ViewRenderer } from './state/view-registry';

// Lazy view loader map
type ViewLoader = () => Promise<ViewRenderer>;

const VIEW_LOADERS: Record<string, ViewLoader> = {
  'graph': () => import('./views/ForceGraphView').then(m => {
    const ForceGraphView = m.ForceGraphView;
    const view = new ForceGraphView('main-graph');
    return {
      render: (data) => {
        const originalData = appState.originalData;
        if (originalData) {
          const filters = appState.nodeTypeFilters;
          const filtered = applyFilters(originalData, filters);
          view.render(filtered);
          updateStatsDisplay(filtered);
          setupNodeClick(view);
        }
      },
      cleanup: () => view.cleanup?.()
    };
  }),

  'quality': () => import('./views/QualityDashboardView').then(m => {
    const view = new m.QualityDashboardView('quality-content');
    return { render: (data) => view.render(data), cleanup: () => view.cleanup?.() };
  }),

  'frontend-quality': () => import('./views/FrontendQualityView').then(m => {
    const view = new m.FrontendQualityView('frontend-quality-content');
    return { render: (data) => view.render(data as any), cleanup: () => view.cleanup?.() };
  }),

  'metrics': () => import('./views/MetricsDashboardView').then(m => {
    const view = new m.MetricsDashboardView('metrics-content');
    return { render: (data) => view.render(data), cleanup: () => view.cleanup?.() };
  }),

  'explorer': () => import('./views/CodeExplorerView').then(m => {
    const view = new m.CodeExplorerView('code-explorer-content');
    return { render: (data) => { view.render(data); }, cleanup: () => view.cleanup?.() };
  }),

  'relations': () => import('./views/CrossFileRelationsView').then(m => {
    const view = new m.CrossFileRelationsView('relations-content');
    return { render: (data) => view.render(data), cleanup: () => view.cleanup?.() };
  }),

  'assets': () => import('./views/ProjectAssetsView').then(m => {
    const view = new m.ProjectAssetsView('assets-content');
    return { render: (data) => { view.render(data); }, cleanup: () => view.cleanup?.() };
  }),

  'architecture': () => import('./views/ArchitectureLayerView').then(m => {
    const view = new m.ArchitectureLayerView('architecture-content');
    return { render: (data) => { view.render(data); }, cleanup: () => {} };
  }),

  'method-call': () => import('./views/MethodCallView').then(m => {
    const view = new m.MethodCallView('method-call-content');
    return { render: (data) => view.render(data), cleanup: () => view.cleanup?.() };
  }),

  'call-chain': () => import('./views/CallChainView').then(m => {
    const view = new m.CallChainView('call-chain-content');
    return { render: (data) => { view.render(data); }, cleanup: () => {} };
  }),

  'ai-review': () => import('./views/AiReviewView').then(m => {
    const view = new m.AiReviewView('ai-review-content');
    return { render: (data) => view.render(data), cleanup: () => view.cleanup?.() };
  }),
};

// Cached view instances for lazy-loaded modules
const viewInstances: Record<string, ViewRenderer | null> = {};

class App {
  async init(): Promise<void> {
    Logger.info('Initializing Application...');

    // Register lazy view loaders with ViewManager
    this.registerLazyViews();

    // Load Projects
    await this.loadProjects();

    Logger.success('Application Ready');
  }

  /**
   * Register lazy view loaders - views loaded on first access
   */
  private registerLazyViews(): void {
    for (const [viewId, loader] of Object.entries(VIEW_LOADERS)) {
      viewManager.register(viewId as ViewType, `view-${viewId}`, {
        render: async (data) => {
          // Load view on first access
          if (!viewInstances[viewId]) {
            viewInstances[viewId] = await loader();
          }
          viewInstances[viewId]?.render(data);
        },
        cleanup: async () => {
          const view = viewInstances[viewId];
          if (view?.cleanup) await view.cleanup();
        }
      });
    }
  }

  /**
   * Switch view via EventBus
   */
  public switchView(viewName: string): void {
    viewManager.switchTo(viewName as ViewType);
    appState.setCurrentView(viewName);
  }

  /**
   * Load projects index and initial project
   */
  private async loadProjects(): Promise<void> {
    const projects = await loadProjectsIndex();
    Logger.success(`Loaded ${projects.length} projects`);

    populateProjectSelector('projectSelector', projects, (filename: string) => {
      this.loadProject(filename);
    });

    if (projects.length > 0) {
      await this.loadProject(projects[0].file);
    }
  }

  /**
   * Load Project using DataFetcherService with unified report support
   */
  private async loadProject(filename: string): Promise<void> {
    if (!filename) return;
    toggleLoadingOverlay(true, '正在解析项目资产...');

    try {
      // Try loading unified report first (unified-report.json)
      const { unifiedReport, analysisResult } = await loadUnifiedReport(
        CONFIG.dataPath,
        filename
      );

      appState.setFullAnalysisData(analysisResult);

      const graphData = await loadProjectData(filename);
      appState.setGraphData(graphData);

      updateStatsDisplay(graphData);

      // If unified report has AI data, set it on appState for AiReviewView
      if (unifiedReport) {
        (appState as any).setUnifiedReport?.(unifiedReport);
      }

      // Set data for all registered views via ViewManager
      viewManager.setData(analysisResult);

      toggleLoadingOverlay(false);
      Logger.success(`Project loaded: ${graphData.nodes.length} nodes${unifiedReport ? `, ${unifiedReport.issues?.length || 0} unified issues` : ''}`);
    } catch (error) {
      showError(`加载失败: ${(error as Error).message}`);
      toggleLoadingOverlay(false);
      Logger.error('Load failed:', error);
    }
  }
}

/**
 * Set up node click handler for ForceGraphView
 */
function setupNodeClick(forceView: any): void {
  const chart = forceView.getChart?.();
  if (!chart) return;

  const analysisData = appState.fullAnalysisData;
  if (!analysisData) return;

  chart.on('click', (params: any) => {
    if (params.dataType === 'node') {
      const name = params.name;
      const asset = analysisData.assets?.find((a: Asset) => a.address.split('.').pop() === name);
      if (asset) {
        // Use eventBus instead of direct class inspector call
        eventBus.emit('ASSET_SELECT', { address: asset.address, type: 'class' });
      }
    }
  });
}

// ==========================================
// Application Initialization
// ==========================================

const app = new App();

// Export app instance for module access
export { app };

// Expose switchView on window for HTML onclick handlers
// This is the ONLY window assignment - replaces all previous globals
(window as any).switchView = (viewName: string) => app.switchView(viewName);

// Use ViewManager for view switching - accessible via module imports
export const switchView = (viewName: string) => app.switchView(viewName);

// Start App
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => app.init());
} else {
  app.init();
}

export default app;
