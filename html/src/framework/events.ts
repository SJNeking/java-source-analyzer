/**
 * Framework Event System
 *
 * Provides DOM-level event delegation utilities that work with the Component class.
 * Complements the application-level EventBus with element-scoped delegation.
 */

/**
 * Event handler type
 */
export type DelegatedHandler<E extends Event = Event> = (e: E, el: HTMLElement) => void;

/**
 * Registered delegation entry
 */
interface DelegationEntry {
  eventType: string;
  selector: string;
  handler: DelegatedHandler;
  wrapper: (e: Event) => void;
}

/**
 * Event delegator - manages delegated event listeners on a root element
 *
 * Usage:
 *   const delegator = new EventDelegator(container);
 *   delegator.on('click', '.btn', (e, el) => { ... });
 *   delegator.destroy(); // cleanup all
 */
export class EventDelegator {
  private entries: DelegationEntry[] = [];
  private root: HTMLElement | null;

  constructor(root: HTMLElement | null) {
    this.root = root;
  }

  /**
   * Register a delegated event listener
   *
   * @param eventType - DOM event type (click, input, keydown, etc.)
   * @param selector - CSS selector for target elements
   * @param handler - Called with (event, matchedElement)
   * @returns Unsubscribe function
   */
  on<E extends Event>(
    eventType: string,
    selector: string,
    handler: DelegatedHandler<E>
  ): () => void {
    if (!this.root) {
      console.warn('EventDelegator: root element is null, listener not registered');
      return () => {};
    }

    const wrapper = (e: Event) => {
      const target = (e.target as HTMLElement).closest(selector);
      if (target && this.root?.contains(target)) {
        handler(e as E, target as HTMLElement);
      }
    };

    this.root.addEventListener(eventType, wrapper);

    const entry: DelegationEntry = { eventType, selector, handler: handler as DelegatedHandler, wrapper };
    this.entries.push(entry);

    // Return unsubscribe
    return () => this.off(entry);
  }

  /**
   * Remove a specific delegated listener
   */
  off(entry: DelegationEntry): void {
    if (!this.root) return;
    this.root.removeEventListener(entry.eventType, entry.wrapper);
    const index = this.entries.indexOf(entry);
    if (index > -1) this.entries.splice(index, 1);
  }

  /**
   * Remove all delegated listeners
   */
  destroy(): void {
    for (const entry of this.entries) {
      this.off(entry);
    }
  }

  /**
   * Get count of active listeners
   */
  get listenerCount(): number {
    return this.entries.length;
  }
}

/**
 * One-time event listener (auto-removes after first trigger)
 */
export function once<E extends Event>(
  element: HTMLElement,
  eventType: string,
  handler: (e: E) => void
): void {
  const wrapped = (e: Event) => {
    element.removeEventListener(eventType, wrapped);
    handler(e as E);
  };
  element.addEventListener(eventType, wrapped);
}

/**
 * Prevent default and stop propagation helper
 */
export function stopEvent(e: Event): void {
  e.preventDefault();
  e.stopPropagation();
}

/**
 * Create a click handler that prevents default
 */
export function clickHandler(handler: (e: MouseEvent) => void): (e: MouseEvent) => void {
  return (e: MouseEvent) => {
    e.preventDefault();
    handler(e);
  };
}
