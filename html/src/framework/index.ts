/**
 * Framework Module Exports
 *
 * Lightweight component rendering framework that replaces:
 * - Manual innerHTML / template string rendering
 * - Inline onclick handlers
 * - Scattered event binding patterns
 */

export { Component, type Attrs, type Child, type EventHandler, LifecycleState } from './component';
export { html, htmlFragment, htmlToElement, htmlToFragment, setSafeHTML, htmlElements } from './html';
export { EventDelegator, once, stopEvent, clickHandler, type DelegatedHandler } from './events';
