/**
 * Application State Manager
 * 
 * Centralized state management for the application.
 * Replaces scattered state variables in App class.
 */

import type { AnalysisResult, GraphData, NodeTypeFilters } from '../types';
import { DEFAULT_NODE_TYPE_FILTERS } from '../config';

export class ApplicationState {
  private _fullAnalysisData: AnalysisResult | null = null;
  private _originalData: GraphData | null = null;
  private _nodeTypeFilters: NodeTypeFilters = { ...DEFAULT_NODE_TYPE_FILTERS };
  private _currentView: string = 'explorer';
  
  // Subscribers for reactive updates
  private dataSubscribers: Array<(data: AnalysisResult) => void> = [];
  private graphDataSubscribers: Array<(data: GraphData) => void> = [];
  private viewSubscribers: Array<(viewId: string) => void> = [];

  /**
   * Get full analysis data (read-only)
   */
  get fullAnalysisData(): AnalysisResult | null {
    return this._fullAnalysisData;
  }

  /**
   * Get graph data (read-only)
   */
  get originalData(): GraphData | null {
    return this._originalData;
  }

  /**
   * Get node type filters (read-only copy)
   */
  get nodeTypeFilters(): NodeTypeFilters {
    return { ...this._nodeTypeFilters };
  }

  /**
   * Get current view ID
   */
  get currentView(): string {
    return this._currentView;
  }

  /**
   * Set full analysis data and notify subscribers
   */
  setFullAnalysisData(data: AnalysisResult): void {
    this._fullAnalysisData = data;
    this.notifyDataSubscribers(data);
  }

  /**
   * Set graph data and notify subscribers
   */
  setGraphData(data: GraphData): void {
    this._originalData = data;
    this.notifyGraphDataSubscribers(data);
  }

  /**
   * Update node type filters
   */
  updateNodeTypeFilter(type: keyof NodeTypeFilters, enabled: boolean): void {
    this._nodeTypeFilters[type] = enabled;
  }

  /**
   * Set current view
   */
  setCurrentView(viewId: string): void {
    this._currentView = viewId;
    this.notifyViewSubscribers(viewId);
  }

  /**
   * Reset all state
   */
  reset(): void {
    this._fullAnalysisData = null;
    this._originalData = null;
    this._nodeTypeFilters = { ...DEFAULT_NODE_TYPE_FILTERS };
    this._currentView = 'explorer';
  }

  /**
   * Subscribe to analysis data changes
   * Returns unsubscribe function
   */
  onDataChange(listener: (data: AnalysisResult) => void): () => void {
    this.dataSubscribers.push(listener);
    return () => {
      const index = this.dataSubscribers.indexOf(listener);
      if (index > -1) this.dataSubscribers.splice(index, 1);
    };
  }

  /**
   * Subscribe to graph data changes
   * Returns unsubscribe function
   */
  onGraphDataChange(listener: (data: GraphData) => void): () => void {
    this.graphDataSubscribers.push(listener);
    return () => {
      const index = this.graphDataSubscribers.indexOf(listener);
      if (index > -1) this.graphDataSubscribers.splice(index, 1);
    };
  }

  /**
   * Subscribe to view changes
   * Returns unsubscribe function
   */
  onViewChange(listener: (viewId: string) => void): () => void {
    this.viewSubscribers.push(listener);
    return () => {
      const index = this.viewSubscribers.indexOf(listener);
      if (index > -1) this.viewSubscribers.splice(index, 1);
    };
  }

  private notifyDataSubscribers(data: AnalysisResult): void {
    this.dataSubscribers.forEach(listener => {
      try {
        listener(data);
      } catch (error) {
        console.error('Error in data subscriber:', error);
      }
    });
  }

  private notifyGraphDataSubscribers(data: GraphData): void {
    this.graphDataSubscribers.forEach(listener => {
      try {
        listener(data);
      } catch (error) {
        console.error('Error in graph data subscriber:', error);
      }
    });
  }

  private notifyViewSubscribers(viewId: string): void {
    this.viewSubscribers.forEach(listener => {
      try {
        listener(viewId);
      } catch (error) {
        console.error('Error in view subscriber:', error);
      }
    });
  }
}

// Export singleton instance
export const appState = new ApplicationState();
