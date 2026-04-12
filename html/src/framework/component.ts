/**
 * Component - Lightweight base class for UI components
 *
 * Provides:
 * - Type-safe DOM element creation
 * - Event delegation system
 * - Lifecycle management (mount/unmount/update)
 * - Automatic cleanup of event listeners
 */

/**
 * DOM element attributes type
 */
export interface Attrs {
  id?: string;
  className?: string;
  class?: string;
  style?: Partial<CSSStyleDeclaration> | string;
  role?: string;
  tabIndex?: number;
  tabindex?: number | string;
  title?: string;
  placeholder?: string;
  type?: string;
  value?: string;
  disabled?: boolean;
  checked?: boolean;
  open?: boolean;
  htmlFor?: string;
  for?: string;
  src?: string;
  href?: string;
  alt?: string;
  [key: string]: unknown;
}

/**
 * A child can be a DOM element, string, number, null, undefined, Text node, or array of children
 */
export type Child = Element | Text | string | number | null | undefined | Child[];

/**
 * Event handler type
 */
export type EventHandler<E extends Event = Event> = (e: E) => void;

/**
 * Component lifecycle state
 */
export enum LifecycleState {
  CREATED = 'created',
  MOUNTED = 'mounted',
  UNMOUNTED = 'unmounted',
}

/**
 * Base Component class
 *
 * Usage:
 *   class MyView extends Component {
 *     render() {
 *       return this.el('div', { className: 'my-view' }, [
 *         this.el('h1', null, 'Hello'),
 *         this.el('p', null, 'World')
 *       ]);
 *     }
 *   }
 */
export abstract class Component {
  /** Root element of this component */
  protected root: HTMLElement | null = null;

  /** Current lifecycle state */
  protected state: LifecycleState = LifecycleState.CREATED;

  /** Registered event cleanup functions */
  private cleanupFns: Array<() => void> = [];

  /** Child component references for cascade unmount */
  private children: Component[] = [];

  // =========================================================================
  // DOM Element Creation
  // =========================================================================

  /**
   * Create a DOM element with attributes and children
   *
   * @param tag - HTML tag name
   * @param attrs - Element attributes (or null)
   * @param children - Child elements, strings, or arrays
   * @returns The created HTMLElement
   */
  protected el<K extends keyof HTMLElementTagNameMap>(
    tag: K,
    attrs: Attrs | null,
    children?: Child[]
  ): HTMLElementTagNameMap[K] {
    const element = document.createElement(tag);
    this.applyAttributes(element, attrs);
    if (children) this.appendChildren(element, children);
    return element;
  }

  /**
   * Create an SVG element
   */
  protected svg(tag: string, attrs: Record<string, string> | null, children?: SVGElement[]): SVGElement {
    const element = document.createElementNS('http://www.w3.org/2000/svg', tag);
    if (attrs) {
      Object.entries(attrs).forEach(([key, value]) => {
        element.setAttribute(key, value);
      });
    }
    if (children) {
      children.forEach(child => element.appendChild(child));
    }
    return element;
  }

  /**
   * Create a text node (safe - handles null/undefined)
   */
  protected text(content: string | number | null | undefined): Text {
    return document.createTextNode(content != null ? String(content) : '');
  }

  /**
   * Create a document fragment from children
   */
  protected fragment(children: Child[]): DocumentFragment {
    const frag = document.createDocumentFragment();
    this.appendChildren(frag, children);
    return frag;
  }

  // =========================================================================
  // Attribute Application
  // =========================================================================

  /**
   * Apply attributes to a DOM element
   */
  private applyAttributes(element: HTMLElement, attrs: Attrs | null): void {
    if (!attrs) return;

    const skip = new Set(['style', 'className', 'class', 'tabIndex', 'tabindex', 'htmlFor', 'for']);

    for (const [key, value] of Object.entries(attrs)) {
      if (skip.has(key)) continue;
      if (value !== undefined && value !== null) {
        element.setAttribute(key, String(value));
      }
    }

    // Handle special attributes
    if (attrs.className || attrs.class) {
      element.className = String(attrs.className || attrs.class);
    }

    if (attrs.style) {
      if (typeof attrs.style === 'string') {
        element.style.cssText = attrs.style;
      } else {
        Object.assign(element.style, attrs.style);
      }
    }

    if (attrs.tabIndex !== undefined) element.tabIndex = attrs.tabIndex;
    else if (attrs.tabindex !== undefined) element.tabIndex = Number(attrs.tabindex);

    if (attrs.htmlFor || attrs.for) {
      const htmlForValue = String(attrs.htmlFor || attrs.for);
      if ('htmlFor' in element) {
        (element as HTMLLabelElement).htmlFor = htmlForValue;
      }
    }

    if (attrs.role) element.setAttribute('role', attrs.role);
  }

  // =========================================================================
  // Child Appending
  // =========================================================================

  /**
   * Append children to a parent node (element or fragment)
   */
  private appendChildren(parent: HTMLElement | DocumentFragment, children: Child[]): void {
    for (const child of children) {
      if (child == null) continue;

      if (Array.isArray(child)) {
        this.appendChildren(parent, child);
      } else if (child instanceof Element) {
        parent.appendChild(child);
      } else if (typeof child === 'string' || typeof child === 'number') {
        parent.appendChild(document.createTextNode(String(child)));
      } else {
        // Handle other cases (Text nodes, etc.)
        try {
          parent.appendChild(child as Node);
        } catch (e) {
          console.warn('Failed to append child:', child, e);
        }
      }
    }
  }

  // =========================================================================
  // Event Delegation
  // =========================================================================

  /**
   * Register a delegated event listener on the root element
   * Automatically cleaned up on unmount
   *
   * @param eventType - DOM event type (click, mouseover, etc.)
   * @param selector - CSS selector for target elements
   * @param handler - Event handler function
   */
  protected on<E extends Event>(
    eventType: string,
    selector: string,
    handler: EventHandler<E>
  ): void {
    const wrappedHandler = (e: Event) => {
      const target = (e.target as HTMLElement).closest(selector);
      if (target && this.root?.contains(target)) {
        handler(e as E);
      }
    };

    this.root?.addEventListener(eventType, wrappedHandler);
    this.cleanupFns.push(() => {
      this.root?.removeEventListener(eventType, wrappedHandler);
    });
  }

  /**
   * Register a direct event listener on a specific element
   * Automatically cleaned up on unmount
   */
  protected onDirect<E extends Event>(
    element: HTMLElement | null,
    eventType: string,
    handler: EventHandler<E>
  ): void {
    if (!element) return;
    element.addEventListener(eventType, handler as EventHandler);
    this.cleanupFns.push(() => {
      element.removeEventListener(eventType, handler as EventHandler);
    });
  }

  /**
   * Register an arbitrary cleanup function
   */
  protected addCleanup(fn: () => void): void {
    this.cleanupFns.push(fn);
  }

  // =========================================================================
  // Lifecycle
  // =========================================================================

  /**
   * Mount this component into a container
   * Replaces container content with this component's root element
   */
  public mount(container: HTMLElement | null): void {
    if (!container) return;

    // Unmount previous instance if already mounted
    if (this.state === LifecycleState.MOUNTED) {
      this.unmount();
    }

    // Create root element
    this.root = this.buildRoot();
    if (!this.root) return;

    // Clear container and mount
    container.innerHTML = '';
    container.appendChild(this.root);

    this.state = LifecycleState.MOUNTED;
    this.onMount();
  }

  /**
   * Override: called after mounting
   */
  protected onMount(): void {
    // Subclasses can override
  }

  /**
   * Unmount and cleanup
   */
  public unmount(): void {
    // Unmount children
    for (const child of this.children) {
      child.unmount();
    }
    this.children = [];

    // Run all cleanup functions
    for (const cleanup of this.cleanupFns) {
      try {
        cleanup();
      } catch (error) {
        console.error('Error during component cleanup:', error);
      }
    }
    this.cleanupFns = [];

    // Remove root element
    if (this.root?.parentNode) {
      this.root.parentNode.removeChild(this.root);
    }
    this.root = null;
    this.state = LifecycleState.UNMOUNTED;

    this.onUnmount();
  }

  /**
   * Override: called after unmounting
   */
  protected onUnmount(): void {
    // Subclasses can override
  }

  /**
   * Re-render the component (replaces content)
   */
  public update(): void {
    if (this.state !== LifecycleState.MOUNTED) return;
    const parent = this.root?.parentNode as HTMLElement | null;
    if (!parent || !this.root) return;

    const newRoot = this.buildRoot();
    if (!newRoot) return;

    parent.replaceChild(newRoot, this.root);
    this.root = newRoot;
  }

  // =========================================================================
  // Abstract Methods
  // =========================================================================

  /**
   * Build and return the root element.
   * Must be implemented by subclasses.
   * Named "buildRoot" to avoid conflict with ViewRenderer.render(data).
   */
  public abstract buildRoot(): HTMLElement;

  // =========================================================================
  // Utilities
  // =========================================================================

  /**
   * Get the root element
   */
  public getRoot(): HTMLElement | null {
    return this.root;
  }

  /**
   * Get current lifecycle state
   */
  public getState(): LifecycleState {
    return this.state;
  }

  /**
   * Register a child component for cascade unmount
   */
  protected registerChild(child: Component): void {
    this.children.push(child);
  }

  /**
   * Find an element within this component
   */
  protected find(selector: string): HTMLElement | null {
    return this.root?.querySelector(selector) || null;
  }

  /**
   * Find all elements within this component
   */
  protected findAll(selector: string): NodeListOf<HTMLElement> {
    return this.root?.querySelectorAll(selector) || ([] as unknown as NodeListOf<HTMLElement>);
  }
}
