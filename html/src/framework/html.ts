/**
 * HTML Tagged Template Helper
 *
 * Provides safe HTML string → DOM conversion using DOMPurify.
 * Returns DOM elements instead of strings, eliminating innerHTML abuse.
 *
 * Usage:
 *   const el = html`<div class="card"><h2>${title}</h2></div>`;
 *   container.appendChild(el);
 */

import DOMPurify from 'dompurify';

/**
 * Convert an HTML string to a DOM element (single root)
 */
export function htmlToElement(htmlString: string): HTMLElement | null {
  const sanitized = DOMPurify.sanitize(htmlString, {
    USE_PROFILES: { html: true },
    ALLOWED_TAGS: [
      'div', 'span', 'p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'button', 'input', 'select', 'textarea', 'form',
      'table', 'thead', 'tbody', 'tr', 'th', 'td',
      'ul', 'ol', 'li', 'dl', 'dt', 'dd',
      'a', 'img', 'pre', 'code', 'blockquote',
      'details', 'summary', 'section', 'article', 'aside',
      'header', 'footer', 'nav', 'main',
      'label', 'fieldset', 'legend',
      'hr', 'br', 'strong', 'em', 'small', 'mark',
      'svg', 'path', 'circle', 'rect', 'line', 'text', 'g',
    ],
    ALLOWED_ATTR: [
      'class', 'id', 'style', 'title', 'role', 'tabindex',
      'type', 'value', 'placeholder', 'disabled', 'checked',
      'open', 'for', 'href', 'src', 'alt',
      'data-*', 'aria-*',
    ],
  });

  if (!sanitized.trim()) return null;

  const template = document.createElement('template');
  template.innerHTML = sanitized.trim();
  const child = template.content.firstElementChild as HTMLElement | null;
  return child;
}

/**
 * Convert an HTML string to a DocumentFragment (multiple roots)
 */
export function htmlToFragment(htmlString: string): DocumentFragment {
  const sanitized = DOMPurify.sanitize(htmlString, {
    USE_PROFILES: { html: true },
  });

  const template = document.createElement('template');
  template.innerHTML = sanitized;
  return document.importNode(template.content, true);
}

/**
 * Tagged template literal that returns a DOM element
 *
 * Usage:
 *   const el = html`<div class="card">Hello ${name}</div>`;
 */
export function html(
  strings: TemplateStringsArray,
  ...values: unknown[]
): HTMLElement | null {
  const htmlString = strings.reduce((result, str, i) => {
    const value = values[i] != null ? String(values[i]) : '';
    return result + str + value;
  }, '');

  return htmlToElement(htmlString);
}

/**
 * Tagged template literal that returns a DocumentFragment
 *
 * Usage:
 *   const frag = htmlFragment`<li>A</li><li>B</li>`;
 */
export function htmlFragment(
  strings: TemplateStringsArray,
  ...values: unknown[]
): DocumentFragment {
  const htmlString = strings.reduce((result, str, i) => {
    const value = values[i] != null ? String(values[i]) : '';
    return result + str + value;
  }, '');

  return htmlToFragment(htmlString);
}

/**
 * Safely set innerHTML on an existing element using DOMPurify
 * Replacement for element.innerHTML = dangerousString
 */
export function setSafeHTML(element: HTMLElement, htmlString: string): void {
  const sanitized = DOMPurify.sanitize(htmlString);
  element.innerHTML = sanitized;
}

/**
 * Create multiple elements from template strings
 * Returns an array of HTMLElement
 */
export function htmlElements(
  strings: TemplateStringsArray,
  ...values: unknown[]
): HTMLElement[] {
  const frag = htmlFragment(strings, ...values);
  return Array.from(frag.children) as HTMLElement[];
}
