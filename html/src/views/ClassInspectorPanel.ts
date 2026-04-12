/**
 * Java Source Analyzer - Class Inspector Panel (Refactored)
 *
 * Uses Component base class for DOM creation instead of innerHTML.
 * Uses event delegation instead of inline event handlers.
 * Uses centralized constants for labels and config.
 * Uses Style helpers for all colors and style objects.
 */

import type { Asset, MethodAsset, FieldAsset } from '../types';
import { Component, type Child } from '../framework/component';
import { EventDelegator } from '../framework/events';
import { ICON, LABEL, CLS, C } from '../constants';
import { Style } from '../utils/style-helpers';

const PANEL_CONFIG = {
  WIDTH: 550,
  TOP_OFFSET: 56,
  Z_INDEX: 9999,
  BACKDROP_Z: 9998,
  TRANSITION: '0.35s',
  BOX_SHADOW: '-10px 0 40px rgba(0,0,0,0.8)',
  BLUR: 4,
  MAX_FIELDS: 10,
  CACHE_LIMIT: 50,
} as const;

export class ClassInspectorPanel extends Component {
  private panel: HTMLElement | null = null;
  private analysisData: Record<string, unknown> | null = null;
  private currentAsset: Asset | null = null;
  private expandedMethod: string | null = null;
  private sourceCodeCache: Map<string, string> = new Map();
  private backdrop: HTMLElement | null = null;
  private contentContainer: HTMLElement | null = null;
  private delegator: EventDelegator | null = null;

  constructor() {
    super();
  }

  public buildRoot(): HTMLElement {
    return this.el('div', null, []);
  }

  public show(asset: Asset, data: Record<string, unknown>): void {
    if (!asset || !asset.address) {
      console.warn('ClassInspectorPanel.show: Invalid asset provided');
      return;
    }

    this.currentAsset = asset;
    this.analysisData = data;
    this.expandedMethod = null;

    if (!this.panel) {
      this.createPanel();
    }

    this.updateHeader(asset);
    this.setVisible(true);
    this.switchTab('info');
  }

  public hide(): void {
    this.setVisible(false);
  }

  public toggleCode(index: number): void {
    if (!this.currentAsset) return;

    const methods = this.currentAsset.methods_full || this.currentAsset.methods || [];
    const method = methods[index];

    if (!method) {
      console.warn(`toggleCode: Method at index ${index} not found`);
      return;
    }

    if (this.expandedMethod === method.address) {
      this.expandedMethod = null;
    } else {
      this.expandedMethod = method.address;
      this.cacheMethodCode(method);
    }

    this.switchTab('code');
  }

  // ==================== Panel Creation ====================

  private createPanel(): void {
    this.createBackdrop();
    this.createMainPanel();
    this.setupEventListeners();
  }

  private createBackdrop(): void {
    this.backdrop = document.createElement('div');
    Object.assign(this.backdrop.style, {
      position: 'fixed',
      inset: '0',
      background: 'rgba(0,0,0,0.5)',
      zIndex: String(PANEL_CONFIG.BACKDROP_Z),
      opacity: '0',
      pointerEvents: 'none',
      transition: `opacity ${PANEL_CONFIG.TRANSITION} ease`,
      backdropFilter: `blur(${PANEL_CONFIG.BLUR}px)`,
    } as Partial<CSSStyleDeclaration>);
    this.backdrop.addEventListener('click', () => this.hide());
    document.body.appendChild(this.backdrop);
  }

  private createMainPanel(): void {
    this.panel = document.createElement('div');
    this.panel.id = 'class-inspector';
    Object.assign(this.panel.style, {
      position: 'fixed',
      top: `${PANEL_CONFIG.TOP_OFFSET}px`,
      right: `-${PANEL_CONFIG.WIDTH}px`,
      bottom: '0',
      width: `${PANEL_CONFIG.WIDTH}px`,
      background: `var(--bg-primary, ${Style.slate[900]})`,
      borderLeft: `1px solid var(--border-color, ${Style.border.white10})`,
      zIndex: String(PANEL_CONFIG.Z_INDEX),
      transition: `right ${PANEL_CONFIG.TRANSITION} cubic-bezier(0.4, 0, 0.2, 1)`,
      display: 'flex',
      flexDirection: 'column',
      boxShadow: PANEL_CONFIG.BOX_SHADOW,
    } as Partial<CSSStyleDeclaration>);

    this.renderPanelHeader();
    this.createContentContainer();
    document.body.appendChild(this.panel);
  }

  private renderPanelHeader(): void {
    if (!this.panel) return;

    const header = this.el('div', {
      style: {
        padding: '20px',
        borderBottom: `1px solid var(--border-light, ${Style.border.white20})`,
        background: `var(--code-bg, ${Style.slate[950]})`,
      } as Partial<CSSStyleDeclaration>,
    }, [
      this.el('div', {
        style: {
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          marginBottom: '6px',
        } as Partial<CSSStyleDeclaration>,
      }, [
        this.el('div', { style: { flex: 1, minWidth: 0 } as unknown as Partial<CSSStyleDeclaration> }, [
          this.el('div', {
            id: 'inspector-title',
            style: {
              fontSize: '18px',
              fontWeight: '700',
              color: 'var(--text-primary)',
              fontFamily: 'monospace',
            } as Partial<CSSStyleDeclaration>,
          }, [this.text(LABEL.COMMON.LOADING)]),
          this.el('div', {
            id: 'inspector-fqn',
            style: {
              fontSize: '11px',
              color: 'var(--text-muted)',
              marginTop: '6px',
              fontFamily: 'monospace',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            } as Partial<CSSStyleDeclaration>,
          }),
        ]),
        this.el('button', {
          id: 'inspector-close',
          style: {
            background: `var(--btn-bg, ${Style.border.white20})`,
            border: `1px solid var(--border-color, ${Style.border.white10})`,
            color: 'var(--text-dim)',
            fontSize: '14px',
            width: '28px',
            height: '28px',
            borderRadius: '50%',
            cursor: 'pointer',
          } as Partial<CSSStyleDeclaration>,
        }, [this.text(ICON.MISC.CROSS)]),
      ]),
      this.el('div', { id: 'inspector-tab-bar', style: { display: 'flex', gap: '16px' } as Partial<CSSStyleDeclaration> }, [
        this.el('div', {
          className: 'inspector-tab-btn',
          'data-tab': 'info',
          style: {
            fontSize: '11px',
            fontWeight: '600',
            color: 'var(--accent)',
            cursor: 'pointer',
            paddingBottom: '6px',
            borderBottom: '2px solid var(--accent)',
          } as Partial<CSSStyleDeclaration>,
        }, [this.text(LABEL.INSPECTOR.TAB_INFO)]),
        this.el('div', {
          className: 'inspector-tab-btn',
          'data-tab': 'code',
          style: {
            fontSize: '11px',
            fontWeight: '600',
            color: 'var(--text-muted)',
            cursor: 'pointer',
            paddingBottom: '6px',
            borderBottom: '2px solid transparent',
          } as Partial<CSSStyleDeclaration>,
        }, [this.text(LABEL.INSPECTOR.TAB_SOURCE)]),
      ]),
    ]);

    this.panel.appendChild(header);
  }

  private createContentContainer(): void {
    if (!this.panel) return;

    this.contentContainer = document.createElement('div');
    this.contentContainer.id = 'inspector-content';
    Object.assign(this.contentContainer.style, {
      flex: '1',
      overflow: 'hidden',
      position: 'relative',
      background: 'var(--bg-secondary)',
    } as Partial<CSSStyleDeclaration>);
    this.panel.appendChild(this.contentContainer);
  }

  private setupEventListeners(): void {
    if (this.delegator) this.delegator.destroy();

    const closeBtn = document.getElementById('inspector-close');
    if (closeBtn) {
      closeBtn.addEventListener('click', () => this.hide());
    }

    const tabBar = document.getElementById('inspector-tab-bar');
    if (tabBar) {
      this.delegator = new EventDelegator(tabBar as HTMLElement);
      this.delegator.on('click', '.inspector-tab-btn', (_e, el) => {
        const tabId = el.getAttribute('data-tab');
        if (tabId) this.switchTab(tabId);
      });
    }
  }

  private updateHeader(asset: Asset): void {
    const titleEl = document.getElementById('inspector-title');
    const fqnEl = document.getElementById('inspector-fqn');
    const shortName = asset.address.split('.').pop() || LABEL.INSPECTOR.UNKNOWN;

    if (titleEl) titleEl.textContent = shortName;
    if (fqnEl) {
      fqnEl.textContent = asset.address;
      fqnEl.title = asset.address;
    }
  }

  private setVisible(visible: boolean): void {
    if (!this.panel || !this.backdrop) return;

    if (visible) {
      this.panel.style.right = '0';
      this.backdrop.style.opacity = '1';
      this.backdrop.style.pointerEvents = 'auto';
    } else {
      this.panel.style.right = `-${PANEL_CONFIG.WIDTH}px`;
      this.backdrop.style.opacity = '0';
      this.backdrop.style.pointerEvents = 'none';
    }
  }

  private switchTab(tab: string): void {
    const tabButtons = document.querySelectorAll('.inspector-tab-btn');
    tabButtons.forEach(btn => {
      const isActive = btn.getAttribute('data-tab') === tab;
      const btnEl = btn as HTMLElement;
      btnEl.style.color = isActive ? 'var(--accent)' : 'var(--text-muted)';
      btnEl.style.borderBottomColor = isActive ? 'var(--accent)' : 'transparent';
    });

    if (!this.contentContainer) return;

    if (tab === 'info') {
      this.contentContainer.innerHTML = '';
      this.contentContainer.appendChild(this.renderInfoTab());
    } else if (tab === 'code') {
      this.contentContainer.innerHTML = '';
      this.contentContainer.appendChild(this.renderCodeTab());
    }
  }

  // ==================== Info Tab ====================

  private renderInfoTab(): HTMLElement {
    if (!this.currentAsset) return this.el('div', null, []);

    const asset = this.currentAsset;
    const fields = asset.fields_matrix || asset.fields || [];

    const children: Child[] = [
      this.el('div', {
        style: {
          fontSize: '13px',
          color: 'var(--text-secondary)',
          lineHeight: '1.7',
        } as Partial<CSSStyleDeclaration>,
      }, [
        this.renderInfoSection(LABEL.INSPECTOR.TYPE_LABEL, asset.kind || LABEL.INSPECTOR.UNKNOWN, { color: 'var(--accent)' }),
        asset.description ? this.renderDescriptionSection(asset.description) : this.el('div', null, []),
        fields.length > 0 ? this.renderFieldsSection(fields) : this.el('div', null, []),
      ]),
    ];

    return this.el('div', {
      style: {
        padding: '20px',
        overflowY: 'auto',
        height: '100%',
      } as Partial<CSSStyleDeclaration>,
    }, children);
  }

  private renderInfoSection(label: string, value: string, valueStyle?: Partial<CSSStyleDeclaration>): HTMLElement {
    return this.el('div', { style: { marginBottom: '24px' } as Partial<CSSStyleDeclaration> }, [
      this.el('div', {
        style: {
          ...Style.muted('11px'),
          fontWeight: '600',
        } as Partial<CSSStyleDeclaration>,
      }, [this.text(label)]),
      this.el('div', {
        style: {
          ...Style.valueBox(),
          ...valueStyle,
        } as Partial<CSSStyleDeclaration>,
      }, [this.text(value)]),
    ]);
  }

  private renderDescriptionSection(description: string): HTMLElement {
    return this.el('div', { style: { marginBottom: '24px' } as Partial<CSSStyleDeclaration> }, [
      this.el('div', {
        style: {
          ...Style.muted('11px'),
          fontWeight: '600',
        } as Partial<CSSStyleDeclaration>,
      }, [this.text(LABEL.INSPECTOR.DESC_LABEL)]),
      this.el('div', { style: Style.valueBox() as Partial<CSSStyleDeclaration> },
        [this.text(description)]),
    ]);
  }

  private renderFieldsSection(fields: FieldAsset[]): HTMLElement {
    const displayFields = fields.slice(0, PANEL_CONFIG.MAX_FIELDS);

    const items = displayFields.map(field => {
      const fieldType = (field as any).type_path || field.type || LABEL.EXPLORER.JAVA_LABEL;
      const typeName = fieldType.split('.').pop() || LABEL.EXPLORER.JAVA_LABEL;

      return this.el('div', {
        style: {
          fontFamily: 'monospace',
          fontSize: '12px',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          background: 'var(--bg-secondary)',
          padding: '6px 10px',
          borderRadius: '4px',
        } as Partial<CSSStyleDeclaration>,
      }, [
        this.el('span', { style: { color: 'var(--purple-primary)' } as Partial<CSSStyleDeclaration> }, [this.text(typeName)]),
        this.el('span', { style: { color: Style.slate[300] } as Partial<CSSStyleDeclaration> }, [this.text(field.name)]),
      ]);
    });

    return this.el('div', { style: { marginBottom: '24px' } as Partial<CSSStyleDeclaration> }, [
      this.el('div', {
        style: {
          ...Style.muted('11px'),
          fontWeight: '600',
        } as Partial<CSSStyleDeclaration>,
      }, [this.text(`${LABEL.INSPECTOR.FIELDS_LABEL} (${fields.length})`)]),
      this.el('div', {
        style: {
          display: 'flex',
          flexDirection: 'column',
          gap: '8px',
        } as Partial<CSSStyleDeclaration>,
      }, items),
    ]);
  }

  // ==================== Code Tab ====================

  private renderCodeTab(): HTMLElement {
    if (!this.currentAsset) return this.el('div', null, []);

    const methods = this.currentAsset.methods_full || this.currentAsset.methods || [];

    if (!methods.length) {
      return this.el('div', {
        style: {
          padding: '20px',
          textAlign: 'center',
          color: 'var(--text-muted)',
        } as Partial<CSSStyleDeclaration>,
      }, [this.text(LABEL.INSPECTOR.NO_SOURCE)]);
    }

    const items = methods.map((method: MethodAsset, index: number) => this.renderMethodCard(method, index));

    return this.el('div', {
      style: {
        padding: '20px',
        overflowY: 'auto',
        height: '100%',
      } as Partial<CSSStyleDeclaration>,
    }, [
      this.el('div', {
        style: {
          display: 'flex',
          flexDirection: 'column',
          gap: '8px',
        } as Partial<CSSStyleDeclaration>,
      }, items),
    ]);
  }

  private renderMethodCard(method: MethodAsset, index: number): HTMLElement {
    const isExpanded = this.expandedMethod === method.address;
    const code = isExpanded ? this.getMethodCode(method) : null;

    const children: Child[] = [
      this.el('div', {
        'data-method-index': String(index),
        style: {
          padding: '10px 14px',
          cursor: 'pointer',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          background: 'var(--bg-secondary)',
        } as Partial<CSSStyleDeclaration>,
      }, [
        this.el('div', {
          style: {
            fontFamily: 'monospace',
            fontSize: '12px',
            color: 'var(--accent)',
          } as Partial<CSSStyleDeclaration>,
        }, [this.text(method.name || LABEL.INSPECTOR.UNKNOWN)]),
        this.el('span', { style: Style.muted('10px') as Partial<CSSStyleDeclaration> },
          [this.text(isExpanded ? ICON.EXPAND.OPEN : ICON.EXPAND.CLOSED)]),
      ]),
    ];

    if (isExpanded && code) {
      const codeEl = this.el('pre', {
        style: {
          padding: '12px',
          margin: 0,
          fontSize: '11px',
          color: 'var(--text-secondary)',
          background: `var(--code-bg, ${Style.slate[950]})`,
          overflowX: 'auto',
          fontFamily: 'monospace',
        } as unknown as Partial<CSSStyleDeclaration>,
      }, []);
      codeEl.innerHTML = this.highlightCode(code);
      children.push(codeEl);
    }

    return this.el('div', {
      style: {
        background: 'var(--bg-tertiary)',
        border: '1px solid var(--border)',
        borderRadius: '6px',
        overflow: 'hidden',
      } as Partial<CSSStyleDeclaration>,
    }, children);
  }

  private getMethodCode(method: MethodAsset): string {
    const cached = this.sourceCodeCache.get(method.address);
    if (cached) return cached;
    return method.source_code || method.body_code || '// No code available';
  }

  private cacheMethodCode(method: MethodAsset): void {
    const code = method.source_code || method.body_code || '// No code available';

    if (this.sourceCodeCache.size >= PANEL_CONFIG.CACHE_LIMIT) {
      const firstKey = this.sourceCodeCache.keys().next().value;
      if (firstKey) this.sourceCodeCache.delete(firstKey);
    }

    this.sourceCodeCache.set(method.address, code);
  }

  private highlightCode(code: string): string {
    let escaped = code.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

    const keywordColor = Style.purple;

    const rules: Array<[RegExp, string]> = [
      [/(\/\/[^\n]*)/g, 'color:var(--code-comment);'],
      [/(\/\*[\s\S]*?\*\/)/g, 'color:var(--code-comment);'],
      [/\b(public|private|protected|static|final|abstract|synchronized|volatile)\b/g, `color:${keywordColor};font-weight:bold;`],
      [/\b(class|interface|enum|extends|implements|new|this|super)\b/g, 'color:var(--accent);font-weight:bold;'],
      [/\b(if|else|for|while|return|try|catch|throw)\b/g, `color:${keywordColor};font-weight:bold;`],
      [/\b(String|int|boolean|long|void|List|Map)\b/g, 'color:var(--code-type);'],
      [/@\w+/g, 'color:var(--warning);'],
      [/(&quot;[^&]*?&quot;)/g, 'color:var(--danger);'],
    ];

    for (const [pattern, style] of rules) {
      escaped = escaped.replace(pattern, `<span style="${style}">$&</span>`);
    }

    return escaped;
  }

  public cleanup(): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = null;

    if (this.panel?.parentNode) this.panel.parentNode.removeChild(this.panel);
    if (this.backdrop?.parentNode) this.backdrop.parentNode.removeChild(this.backdrop);
    this.panel = null;
    this.backdrop = null;
  }
}
