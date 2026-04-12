/**
 * Java Source Analyzer - Component Explorer View (Refactored)
 *
 * Hierarchical view: Packages -> Classes -> Methods/Fields.
 * Uses Component base class for DOM creation.
 * Uses event delegation instead of window.__explorerNavigate/__explorerBack.
 * Uses eventBus for navigation.
 */

import type { AnalysisResult, Asset } from '../types';
import { Component, type Child } from '../framework/component';
import { EventDelegator } from '../framework/events';
import { eventBus } from '../utils/event-bus';
import { Logger } from '../utils/logger';
import { Style } from '../utils/style-helpers';
import { ICON, LABEL } from '../constants';

export class ComponentExplorerView extends Component {
  private containerId: string;
  private data: AnalysisResult | null = null;
  private history: { level: string; address: string | null }[] = [{ level: 'packages', address: null }];
  private delegator: EventDelegator | null = null;

  constructor(containerId: string = 'explorer-content') {
    super();
    this.containerId = containerId;
  }

  public loadData(data: AnalysisResult): void {
    this.data = data;
    this.history = [{ level: 'packages', address: null }];
    const container = document.getElementById(this.containerId);
    if (!container) return;
    this.mount(container);
    this.setupEventListeners();
  }

  public buildRoot(): HTMLElement {
    if (!this.data) return this.el('div', null, []);

    const current = this.history[this.history.length - 1];
    if (current.level === 'packages') return this.renderPackagesView();
    if (current.level === 'class' && current.address) return this.renderClassDetailView(current.address);
    return this.renderPackagesView();
  }

  private renderPackagesView(): HTMLElement {
    const assets = this.data!.assets || [];
    const packages: Record<string, Asset[]> = {};

    assets.forEach(a => {
      const parts = a.address.split('.');
      parts.pop();
      const pkg = parts.join('.') || 'default';
      if (!packages[pkg]) packages[pkg] = [];
      packages[pkg].push(a);
    });

    const sorted = Object.entries(packages).sort((a, b) => a[0].localeCompare(b[0]));

    const pkgSections = sorted.map(([pkg, items]) => {
      const cards = items.map(a => this.createCard(a));
      return this.el('div', { style: { marginBottom: '24px' } as Partial<CSSStyleDeclaration> }, [
        this.el('div', {
          style: { fontSize: '11px', color: Style.slate[600], marginBottom: '8px', fontFamily: 'monospace', borderBottom: `1px solid ${Style.slate[800]}`, paddingBottom: '4px' } as Partial<CSSStyleDeclaration>,
        }, [this.text(LABEL.COMPONENT.PACKAGE_PREFIX(pkg))]),
        this.el('div', {
          style: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '10px' } as Partial<CSSStyleDeclaration>,
        }, cards),
      ]);
    });

    return this.el('div', null, [
      this.el('div', {
        className: 'explorer-header',
        style: { padding: '16px', borderBottom: `1px solid ${Style.slate[700]}`, background: Style.slate[950] } as Partial<CSSStyleDeclaration>,
      }, [
        this.el('h2', { className: 'explorer-title' }, [this.text(LABEL.COMPONENT.TITLE(assets.length))]),
      ]),
      this.el('div', { style: { padding: '20px', overflowY: 'auto', height: 'calc(100% - 56px)' } as Partial<CSSStyleDeclaration> }, pkgSections),
    ]);
  }

  private createCard(asset: Asset): HTMLElement {
    const kind = asset.kind || 'CLASS';
    const name = asset.address.split('.').pop() || '';
    const icon = ICON.KIND[kind as keyof typeof ICON.KIND] || '📄';
    const mCount = asset.methods_full?.length || 0;
    const fCount = asset.fields_matrix?.length || 0;

    return this.el('div', {
      className: 'asset-card',
      'data-address': asset.address,
      style: {
        background: Style.slate[800],
        border: `1px solid ${Style.slate[700]}`,
        borderRadius: '6px',
        padding: '12px',
        cursor: 'pointer',
        transition: 'all 0.2s',
      } as Partial<CSSStyleDeclaration>,
    }, [
      this.el('div', { style: { display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '6px' } as Partial<CSSStyleDeclaration> }, [
        this.el('span', { style: { fontSize: '16px' } as Partial<CSSStyleDeclaration> }, [this.text(icon)]),
        this.el('span', {
          style: { fontWeight: '600', color: Style.slate[50], fontSize: '13px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' } as Partial<CSSStyleDeclaration>,
        }, [this.text(name)]),
      ]),
      this.el('div', { style: { fontSize: '10px', color: Style.slate[600], display: 'flex', gap: '6px' } as Partial<CSSStyleDeclaration> }, [
        this.el('span', { style: { background: Style.bg.badgeBlue, color: Style.blueLt, padding: '2px 4px', borderRadius: '3px' } as Partial<CSSStyleDeclaration> }, [this.text(kind)]),
        this.el('span', null, [this.text(LABEL.COMPONENT.METHOD_COUNT(mCount))]),
        fCount > 0 ? this.el('span', null, [this.text(LABEL.COMPONENT.FIELDS(fCount))]) : this.el('span', null, []),
      ]),
    ]);
  }

  private renderClassDetailView(address: string): HTMLElement {
    const asset = this.data!.assets?.find(a => a.address === address);
    if (!asset) {
      return this.el('div', { style: { padding: '40px', textAlign: 'center' } as Partial<CSSStyleDeclaration> },
        [this.text(LABEL.COMPONENT.NOT_FOUND)]);
    }

    const methods = asset.methods_full || asset.methods || [];
    const fields = asset.fields_matrix || asset.fields || [];

    const children: Child[] = [
      this.el('div', {
        className: 'explorer-header',
        style: { padding: '12px 16px', borderBottom: `1px solid ${Style.slate[700]}`, background: Style.slate[950], display: 'flex', alignItems: 'center' } as Partial<CSSStyleDeclaration>,
      }, [
        this.el('button', {
          id: 'explorer-back-btn',
          style: { background: 'transparent', border: `1px solid ${Style.slate[700]}`, color: Style.slate[400], cursor: 'pointer', padding: '4px 10px', borderRadius: '4px', marginRight: '12px' } as Partial<CSSStyleDeclaration>,
        }, [this.text(LABEL.COMPONENT.BACK)]),
        this.el('div', { style: { flex: 1, minWidth: 0 } as unknown as Partial<CSSStyleDeclaration> }, [
          this.el('div', { style: { fontSize: '16px', fontWeight: 'bold', color: Style.slate[50], fontFamily: 'monospace' } as Partial<CSSStyleDeclaration> },
            [this.text(asset.address.split('.').pop() || '')]),
          this.el('div', { style: { fontSize: '10px', color: Style.slate[600], fontFamily: 'monospace', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' } as Partial<CSSStyleDeclaration> },
            [this.text(asset.address)]),
        ]),
      ]),
    ];

    const contentChildren: Child[] = [];

    if (asset.description) {
      contentChildren.push(
        this.el('div', null, [
          this.el('div', { style: { fontSize: '11px', color: Style.slate[600], marginBottom: '6px', fontWeight: '600' } as Partial<CSSStyleDeclaration> },
            [this.text(LABEL.COMPONENT.DESCRIPTION)]),
          this.el('div', {
            style: { color: Style.slate[300], fontSize: '12px', lineHeight: '1.5', background: Style.slate[800], padding: '10px', borderRadius: '6px', border: `1px solid ${Style.slate[700]}` } as Partial<CSSStyleDeclaration>,
          }, [this.text(asset.description)]),
        ])
      );
    }

    if (fields.length > 0) {
      const fieldCards = fields.map((f: any) => {
        const typeName = f.type_path?.split('.').pop() || f.type || 'var';
        return this.el('div', {
          style: { background: Style.slate[800], border: `1px solid ${Style.slate[700]}`, borderRadius: '4px', padding: '6px 10px', fontFamily: 'monospace', fontSize: '11px', display: 'flex', gap: '6px' } as Partial<CSSStyleDeclaration>,
        }, [
          this.el('span', { style: { color: Style.purpleLt } as Partial<CSSStyleDeclaration> }, [this.text(typeName)]),
          this.el('span', { style: { color: Style.slate[200] } as Partial<CSSStyleDeclaration> }, [this.text(f.name)]),
        ]);
      });

      contentChildren.push(
        this.el('div', null, [
          this.el('div', { style: { fontSize: '11px', color: Style.slate[600], marginBottom: '6px', fontWeight: '600' } as Partial<CSSStyleDeclaration> },
            [this.text(LABEL.COMPONENT.FIELDS(fields.length))]),
          this.el('div', {
            style: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '6px' } as Partial<CSSStyleDeclaration>,
          }, fieldCards),
        ])
      );
    }

    const methodCards = methods.map((m: any) => {
      const modSpans: Child[] = [];
      if (m.modifiers?.includes('public')) modSpans.push(this.el('span', { style: { color: Style.greenLt } as Partial<CSSStyleDeclaration> }, [this.text(LABEL.COMPONENT.MOD_PUBLIC)]));
      if (m.modifiers?.includes('static')) modSpans.push(this.el('span', { style: { color: Style.purpleLt } as Partial<CSSStyleDeclaration> }, [this.text(LABEL.COMPONENT.MOD_STATIC)]));
      modSpans.push(this.el('span', null, [this.text(`${m.name}(...)`)]));

      return this.el('div', {
        style: { background: Style.slate[800], border: `1px solid ${Style.slate[700]}`, borderRadius: '4px', padding: '8px 12px', fontFamily: 'monospace', fontSize: '11px' } as Partial<CSSStyleDeclaration>,
      }, [
        this.el('div', { style: { display: 'flex', alignItems: 'center', gap: '6px', color: Style.blueLt, fontWeight: '500' } as Partial<CSSStyleDeclaration> }, modSpans),
        m.description ? this.el('div', { style: { color: Style.slate[600], marginTop: '2px', fontFamily: 'sans-serif' } as Partial<CSSStyleDeclaration> }, [this.text(m.description)]) : this.el('div', null, []),
      ]);
    });

    contentChildren.push(
      this.el('div', null, [
        this.el('div', { style: { fontSize: '11px', color: Style.slate[600], marginBottom: '6px', fontWeight: '600' } as Partial<CSSStyleDeclaration> },
          [this.text(LABEL.COMPONENT.METHODS(methods.length))]),
        this.el('div', { style: { display: 'flex', flexDirection: 'column', gap: '6px' } as Partial<CSSStyleDeclaration> }, methodCards),
      ])
    );

    children.push(
      this.el('div', { style: { padding: '20px', overflowY: 'auto', height: 'calc(100% - 56px)', display: 'flex', flexDirection: 'column', gap: '20px' } as Partial<CSSStyleDeclaration> }, contentChildren)
    );

    return this.el('div', null, children);
  }

  private setupEventListeners(): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = new EventDelegator(this.root);
    if (!this.delegator) return;

    // Asset card click → navigate to class
    this.delegator.on('click', '.asset-card', (_e, el) => {
      const address = el.getAttribute('data-address');
      if (address) this.navigateTo(address);
    });

    // Hover effects on cards
    this.delegator.on('mouseenter', '.asset-card', (_e, el) => {
      el.style.borderColor = Style.blueLt;
      el.style.transform = 'translateY(-2px)';
    });
    this.delegator.on('mouseleave', '.asset-card', (_e, el) => {
      el.style.borderColor = Style.slate[700];
      el.style.transform = 'none';
    });

    // Back button
    this.delegator.on('click', '#explorer-back-btn', () => {
      this.goBack();
    });
  }

  public navigateTo(address: string): void {
    this.history.push({ level: 'class', address });
    this.update();
    this.setupEventListeners();
  }

  public goBack(): void {
    if (this.history.length > 1) {
      this.history.pop();
      this.update();
      this.setupEventListeners();
    }
  }

  public cleanup(): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = null;
    this.unmount();
  }
}
