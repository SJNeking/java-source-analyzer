/**
 * Global Event Bus
 * 
 * Replaces window.__functionName global functions with a type-safe event system.
 * Provides decoupled communication between components.
 */

/**
 * Event types for the application
 */
export type EventType = 
  | 'VIEW_SWITCH'           // Switch view tab
  | 'TREE_MODE_CHANGE'      // Change tree view mode (grouped/flat)
  | 'ASSET_SELECT'          // Select an asset (class/method)
  | 'METHOD_SELECT'         // Select a method
  | 'CLASS_SELECT'          // Select a class
  | 'SEARCH'                // Search in tree
  | 'CODE_COPY'             // Copy code to clipboard
  | 'TAB_SWITCH'            // Switch internal tabs (diagram/source/calls)
  | 'FILTER_CHANGE'         // Change filters
  | 'HIERARCHY_LEVEL_CHANGE' // Change hierarchy level (module/package/component)
  | 'GRAPH_AGGREGATE'       // Toggle graph aggregation
  | 'NODE_EXPAND'          // Expand node to show children
  | 'NODE_COLLAPSE';       // Collapse node

/**
 * Event payload types
 */
export interface EventPayloads {
  VIEW_SWITCH: { viewId: string };
  TREE_MODE_CHANGE: { mode: 'grouped' | 'flat' };
  ASSET_SELECT: { address: string; type?: 'class' | 'method' };
  METHOD_SELECT: { classAddress: string; methodName: string };
  CLASS_SELECT: { address: string };
  SEARCH: { keyword: string };
  CODE_COPY: { code: string };
  TAB_SWITCH: { tabId: string };
  FILTER_CHANGE: { filterType: string; enabled: boolean };
  HIERARCHY_LEVEL_CHANGE: { level: 'module' | 'package' | 'component' | 'method' };
  GRAPH_AGGREGATE: { enabled: boolean; options?: any };
  NODE_EXPAND: { nodeId: string; level?: string };
  NODE_COLLAPSE: { nodeId: string };
}

/**
 * Event listener function type
 */
export type EventListener<T extends EventType> = (payload: EventPayloads[T]) => void;

/**
 * Global Event Bus singleton
 */
class EventBus {
  private listeners: Map<EventType, Set<EventListener<any>>> = new Map();

  /**
   * Subscribe to an event
   */
  on<T extends EventType>(eventType: T, listener: EventListener<T>): () => void {
    if (!this.listeners.has(eventType)) {
      this.listeners.set(eventType, new Set());
    }
    this.listeners.get(eventType)!.add(listener);

    // Return unsubscribe function
    return () => {
      this.off(eventType, listener);
    };
  }

  /**
   * Unsubscribe from an event
   */
  off<T extends EventType>(eventType: T, listener: EventListener<T>): void {
    const listeners = this.listeners.get(eventType);
    if (listeners) {
      listeners.delete(listener);
    }
  }

  /**
   * Emit an event
   */
  emit<T extends EventType>(eventType: T, payload: EventPayloads[T]): void {
    const listeners = this.listeners.get(eventType);
    if (listeners) {
      listeners.forEach(listener => {
        try {
          listener(payload);
        } catch (error) {
          console.error(`Error in event listener for ${eventType}:`, error);
        }
      });
    }
  }

  /**
   * Clear all listeners (useful for cleanup)
   */
  clear(): void {
    this.listeners.clear();
  }

  /**
   * Get listener count for debugging
   */
  getListenerCount(eventType?: EventType): number {
    if (eventType) {
      return this.listeners.get(eventType)?.size || 0;
    }
    return Array.from(this.listeners.values()).reduce((sum, set) => sum + set.size, 0);
  }
}

// Export singleton instance
export const eventBus = new EventBus();

/**
 * Helper: Bind event bus to DOM elements
 * Automatically attaches event listeners and cleans up on unmount
 */
export function bindEventToDOM<T extends EventType>(
  eventType: T,
  selector: string,
  elementGetter: () => HTMLElement | null,
  transformEvent: (e: Event) => EventPayloads[T]
): () => void {
  const handler = (e: Event) => {
    const payload = transformEvent(e);
    eventBus.emit(eventType, payload);
  };

  const attach = () => {
    const el = elementGetter();
    if (el) {
      el.addEventListener(getDOMEventType(eventType), handler as any);
    }
  };

  attach();

  // Return cleanup function
  return () => {
    const el = elementGetter();
    if (el) {
      el.removeEventListener(getDOMEventType(eventType), handler as any);
    }
  };
}

/**
 * Map event types to DOM event names
 */
function getDOMEventType(eventType: EventType): string {
  switch (eventType) {
    case 'SEARCH':
      return 'input';
    case 'CODE_COPY':
      return 'click';
    default:
      return 'click';
  }
}
