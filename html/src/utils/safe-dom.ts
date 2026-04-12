/**
 * Safe DOM Utilities
 * 
 * Provides XSS-safe HTML rendering and type-safe DOM manipulation.
 * Replaces direct innerHTML usage with sanitized alternatives.
 */

import DOMPurify from 'dompurify';

/**
 * Safely set innerHTML with DOMPurify sanitization
 */
export function safeSetInnerHTML(element: HTMLElement, html: string): void {
  if (!element) return;
  element.innerHTML = DOMPurify.sanitize(html);
}

/**
 * Create element with sanitized content
 */
export function createSafeElement(tag: string, options?: {
  className?: string;
  innerHTML?: string;
  textContent?: string;
  attributes?: Record<string, string>;
}): HTMLElement {
  const el = document.createElement(tag);
  
  if (options?.className) el.className = options.className;
  if (options?.textContent) el.textContent = options.textContent;
  if (options?.innerHTML) safeSetInnerHTML(el, options.innerHTML);
  
  if (options?.attributes) {
    Object.entries(options.attributes).forEach(([key, value]) => {
      el.setAttribute(key, value);
    });
  }
  
  return el;
}

/**
 * Render template safely (sanitizes before insertion)
 */
export function renderTemplate(container: HTMLElement, template: string): void {
  safeSetInnerHTML(container, template);
}

/**
 * Append safe HTML to container
 */
export function appendSafeHTML(container: HTMLElement, html: string): void {
  const temp = document.createElement('div');
  safeSetInnerHTML(temp, html);
  while (temp.firstChild) {
    container.appendChild(temp.firstChild);
  }
}
