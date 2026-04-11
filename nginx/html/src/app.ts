/**
 * Java Source Analyzer - Main Application
 * 
 * Architecture:
 * 1. Unified Data Store (rawData, graphData)
 * 2. Declarative View Switching (via CSS Classes)
 * 3. Performance & Stability (LRU Cache, Auto-Retry)
 * 4. Accessibility (Keyboard Nav, ARIA roles)
 */

import type { GraphData, NodeTypeFilters, Asset, AnalysisResult } from './types';
import { DEFAULT_NODE_TYPE_FILTERS } from './config';
import {
  loadProjectsIndex,
  loadProjectData,
  populateProjectSelector,
  updateStatsDisplay,
  toggleLoadingOverlay,
  showError,
  Logger
} from './utils';
import {
  applyFilters,
  toggleNodeTypeFilter
} from './utils/filter-utils';

import { ForceGraphView } from './views/ForceGraphView';
import { QualityDashboardView } from './views/QualityDashboardView';
import { MetricsDashboardView } from './views/MetricsDashboardView';
import { CodeExplorerView } from './views/CodeExplorerView';
import { CrossFileRelationsView } from './views/CrossFileRelationsView';
import { ProjectAssetsView } from './views/ProjectAssetsView';
import { ClassInspectorPanel } from './views/ClassInspectorPanel';
import { LRUCache } from './utils/lru-cache';

class App {
  // Views
  public forceView: ForceGraphView | null = null;
  public qualityView: QualityDashboardView | null = null;
  public metricsView: MetricsDashboardView | null = null;
  public codeExplorerView: CodeExplorerView | null = null;
  public relationsView: CrossFileRelationsView | null = null;
  public assetsView: ProjectAssetsView | null = null;
  public classInspector: ClassInspectorPanel | null = null;

  // State
  public fullAnalysisData: AnalysisResult | null = null;
  public originalData: GraphData | null = null;
  public nodeTypeFilters: NodeTypeFilters = { ...DEFAULT_NODE_TYPE_FILTERS };
  public currentView: string = 'forcegraph';

  // LRU Cache for raw JSON data (Limit: 3 large projects to save memory)
  private rawDataCache = new LRUCache<string, AnalysisResult>(3);

  async init(): Promise<void> {
    Logger.info('Initializing Application...');

    // 1. Init Views
    this.forceView = new ForceGraphView('main-graph');
    this.qualityView = new QualityDashboardView('quality-content');
    this.metricsView = new MetricsDashboardView('metrics-content');
    this.codeExplorerView = new CodeExplorerView('code-explorer-content');
    this.relationsView = new CrossFileRelationsView('relations-content');
    this.assetsView = new ProjectAssetsView('assets-content');
    this.classInspector = new ClassInspectorPanel();

    // 2. Expose Globals
    window.app = this;
    window.forceView = this.forceView;
    window.qualityView = this.qualityView;
    window.codeExplorerView = this.codeExplorerView;
    window.classInspector = this.classInspector;

    // 3. Load Projects
    const projects = await loadProjectsIndex();
    Logger.success(`Loaded ${projects.length} projects`);

    populateProjectSelector('projectSelector', projects, (filename: string) => {
      this.loadProject(filename);
    });

    if (projects.length > 0) {
      await this.loadProject(projects[0].file);
    }

    Logger.success('Application Ready');
  }

  /**
   * Unified View Switching (Declarative via CSS Classes)
   */
  public switchView(viewName: string): void {
    this.currentView = viewName;
    
    // Update Tab UI
    document.querySelectorAll('.nav-tab').forEach(t => {
      const isActive = (t as HTMLElement).dataset.view === viewName;
      t.classList.toggle('active', isActive);
    });

    // Update Container Visibility (CSS class based, no inline styles)
    document.querySelectorAll('.view-container').forEach(c => {
      const isActive = (c as HTMLElement).id === `view-${viewName}`;
      c.classList.toggle('active', isActive);
    });

    // Trigger Render for the specific view if data is available
    this.renderCurrentView();
  }

  /**
   * Load Project with LRU Caching and Auto-Retry
   */
  private async loadProject(filename: string): Promise<void> {
    if (!filename) return;
    toggleLoadingOverlay(true, '正在解析项目资产...');

    try {
      // Check Cache first
      let analysisResult = this.rawDataCache.get(filename);
      if (!analysisResult) {
        Logger.info(`Fetching data for ${filename}...`);
        // Fetch with 3 retries and exponential backoff
        analysisResult = await this.fetchWithRetry(`/data/${filename}`);
        this.rawDataCache.set(filename, analysisResult);
      }
      
      this.fullAnalysisData = analysisResult;

      // Transform to Graph Data
      const graphData = await loadProjectData(filename);
      this.originalData = graphData;

      updateStatsDisplay(graphData);
      
      // Render current view
      this.renderCurrentView();

      toggleLoadingOverlay(false);
      Logger.success(`Project loaded: ${graphData.nodes.length} nodes`);
    } catch (error) {
      showError(`加载失败: ${(error as Error).message}`);
      toggleLoadingOverlay(false);
      Logger.error('Load failed:', error);
    }
  }

  /**
   * Auto-Retry Fetch Logic (3 attempts with exponential backoff)
   */
  private async fetchWithRetry(url: string, retries: number = 3, delay: number = 1000): Promise<AnalysisResult> {
    try {
      const response = await fetch(`${url}?t=${Date.now()}`);
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      return await response.json() as AnalysisResult;
    } catch (error) {
      if (retries > 0) {
        Logger.warning(`Fetch failed (${url}), retrying in ${delay}ms...`);
        await new Promise(resolve => setTimeout(resolve, delay));
        return this.fetchWithRetry(url, retries - 1, delay * 2);
      }
      throw error;
    }
  }

  /**
   * Route to the correct View's render method
   */
  private renderCurrentView(): void {
    if (!this.fullAnalysisData) return;

    switch (this.currentView) {
      case 'forcegraph':
        if (this.originalData) this.renderForceGraph(this.originalData);
        break;
      case 'quality':
        this.qualityView?.render(this.fullAnalysisData);
        break;
      case 'metrics':
        this.metricsView?.render(this.fullAnalysisData);
        break;
      case 'explorer':
        this.codeExplorerView?.render(this.fullAnalysisData);
        this.codeExplorerView?.bindMethodClicks();
        break;
      case 'relations':
        this.relationsView?.render(this.fullAnalysisData);
        break;
      case 'assets':
        this.assetsView?.render(this.fullAnalysisData);
        break;
    }
  }

  private renderForceGraph(data: GraphData): void {
    if (!this.forceView) return;
    const filtered = applyFilters(data, this.nodeTypeFilters);
    this.forceView.render(filtered);
    updateStatsDisplay(filtered);
    this.setupNodeClick();
  }

  private setupNodeClick(): void {
    if (!this.forceView || !this.fullAnalysisData) return;
    const chart = this.forceView.getChart();
    if (!chart) return;

    chart.on('click', (params: any) => {
      if (params.dataType === 'node' && this.classInspector) {
        const name = params.name;
        const asset = this.fullAnalysisData.assets?.find((a: Asset) => a.address.split('.').pop() === name);
        if (asset) this.classInspector.show(asset, this.fullAnalysisData);
      }
    });
  }

  public toggleNodeTypeFilter(type: string, element: HTMLElement): void {
    if (this.forceView) {
      const isDisabled = element.classList.contains('disabled');
      this.forceView.setCategoryFilter(type, isDisabled);
      element.classList.toggle('disabled');
    }
  }

  // Public Helpers
  public searchNodes(keyword: string) { this.forceView?.searchNodes(keyword); }
  public resetView() { this.forceView?.resetView(); }
  public zoomIn() { this.forceView?.zoomIn(); }
  public zoomOut() { this.forceView?.zoomOut(); }
  public toggleLabels() { this.forceView?.showLabels(false); }
  public centerView() { this.forceView?.centerView(); }
  public exportImage() { this.forceView?.downloadImage(); }
}

// ==========================================
// Global Initialization
// ==========================================

const app = new App();

// Attach to window for HTML access
(window as any).app = app;
(window as any).forceView = null as any;
(window as any).qualityView = null as any;
(window as any).codeExplorerView = null as any;
(window as any).classInspector = null as any;
(window as any).switchView = (viewName: string) => app.switchView(viewName);

(window as any).searchNode = (k: string) => app.searchNodes(k);
(window as any).resetView = () => app.resetView();
(window as any).zoomIn = () => app.zoomIn();
(window as any).zoomOut = () => app.zoomOut();
(window as any).toggleLabels = () => app.toggleLabels();
(window as any).centerView = () => app.centerView();
(window as any).exportImage = () => app.exportImage();
(window as any).toggleFilter = (type: string, el: HTMLElement) => app.toggleNodeTypeFilter(type, el);

// Start App
if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', () => app.init());
else app.init();

export default app;
