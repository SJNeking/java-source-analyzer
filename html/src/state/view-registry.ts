/**
 * View Manager (Refactored)
 *
 * Centralized view switching logic with async renderer support.
 * Replaces switch-case statements in app.ts with a declarative,
 * extensible view registration system.
 */

import type { ViewType, AnalysisResult } from '../types';
import { VIEW_METADATA } from '../config/architecture-patterns';
import { eventBus } from '../utils/event-bus';

/**
 * View renderer interface - supports both sync and async
 */
export interface ViewRenderer {
  render(data: AnalysisResult): void | Promise<void>;
  cleanup?(): void | Promise<void>;
}

/**
 * View registry entry
 */
interface ViewRegistryEntry {
  id: ViewType;
  containerId: string;
  renderer: ViewRenderer | null;
  isActive: boolean;
}

/**
 * View Manager - handles view lifecycle and switching
 */
export class ViewManager {
  private registry: Map<ViewType, ViewRegistryEntry> = new Map();
  private currentView: ViewType | null = null;
  private currentData: AnalysisResult | null = null;

  /**
   * Register a view renderer
   */
  register(viewId: ViewType, containerId: string, renderer: ViewRenderer): void {
    this.registry.set(viewId, {
      id: viewId,
      containerId,
      renderer,
      isActive: false,
    });
  }

  /**
   * Unregister a view (with cleanup)
   */
  async unregister(viewId: ViewType): Promise<void> {
    const entry = this.registry.get(viewId);
    if (entry?.renderer?.cleanup) {
      await entry.renderer.cleanup();
    }
    this.registry.delete(viewId);
  }

  /**
   * Switch to a view (with async support)
   */
  async switchTo(viewId: ViewType): Promise<void> {
    if (!this.registry.has(viewId)) {
      console.warn(`View "${viewId}" not registered`);
      return;
    }

    // Hide current view
    if (this.currentView) {
      this.hideView(this.currentView);
    }

    // Show new view
    this.showView(viewId);
    this.currentView = viewId;

    // Emit event
    eventBus.emit('VIEW_SWITCH', { viewId });

    // Render if data is available
    if (this.currentData) {
      await this.renderCurrentView();
    }
  }

  /**
   * Set data and re-render current view
   */
  async setData(data: AnalysisResult): Promise<void> {
    this.currentData = data;

    if (this.currentView) {
      await this.renderCurrentView();
    }
  }

  /**
   * Get current view ID
   */
  getCurrentView(): ViewType | null {
    return this.currentView;
  }

  /**
   * Get all registered view metadata
   */
  getRegisteredViews(): Array<{ id: ViewType; label: string; icon: string }> {
    return Array.from(this.registry.keys()).map(id => ({
      id,
      label: VIEW_METADATA[id]?.label || id,
      icon: VIEW_METADATA[id]?.icon || '📄',
    }));
  }

  /**
   * Show a view (DOM manipulation)
   */
  private showView(viewId: ViewType): void {
    const entry = this.registry.get(viewId);
    if (!entry) return;

    // Update tab UI
    document.querySelectorAll('.nav-tab').forEach(tab => {
      const el = tab as HTMLElement;
      const isActive = el.dataset.view === viewId;
      el.classList.toggle('active', isActive);
    });

    // Update container visibility
    document.querySelectorAll('.view-container, .view-dashboard').forEach(container => {
      const el = container as HTMLElement;
      const isActive = el.id === `view-${viewId}`;
      el.classList.toggle('hidden', !isActive);
      el.classList.toggle('active', isActive);
    });

    entry.isActive = true;
  }

  /**
   * Hide a view (DOM manipulation + cleanup)
   */
  private hideView(viewId: ViewType): void {
    const entry = this.registry.get(viewId);
    if (!entry) return;

    // Cleanup if needed
    if (entry.renderer?.cleanup) {
      try {
        entry.renderer.cleanup();
      } catch (error) {
        console.error(`Error cleaning up view "${viewId}":`, error);
      }
    }

    entry.isActive = false;
  }

  /**
   * Render current view with current data (async-safe)
   */
  private async renderCurrentView(): Promise<void> {
    if (!this.currentView || !this.currentData) return;

    const entry = this.registry.get(this.currentView);
    if (entry && entry.renderer) {
      try {
        await entry.renderer.render(this.currentData);
      } catch (error) {
        console.error(`Error rendering view "${this.currentView}":`, error);
      }
    }
  }
}

// Export singleton instance
export const viewManager = new ViewManager();
