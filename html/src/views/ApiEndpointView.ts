/**
 * Java Source Analyzer - API Endpoint View (Refactored)
 *
 * Uses Component base class for DOM creation.
 * Uses event delegation instead of inline onclick.
 * All colors, labels, icons centralized in constants/style-helpers.
 */

import type { AnalysisResult } from '../types';
import { Component, type Child } from '../framework/component';
import { EventDelegator } from '../framework/events';
import { Logger } from '../utils/logger';
import { Style } from '../utils/style-helpers';
import { ICON, LABEL, CLS } from '../constants';

export class ApiEndpointView extends Component {
  private containerId: string;
  private analysisData: any = null;
  private activeMethodFilter: string | null = null;
  private allEndpoints: any[] = [];
  private delegator: EventDelegator | null = null;

  constructor(containerId: string = 'api-endpoints-content') {
    super();
    this.containerId = containerId;
  }

  public loadData(data: AnalysisResult): void {
    this.analysisData = data;
    const container = document.getElementById(this.containerId);
    if (!container) {
      Logger.error(`Container not found: ${this.containerId}`);
      return;
    }

    const springData = (data as any).spring_analysis;
    if (!springData || !springData.endpoints || springData.endpoints.length === 0) {
      this.mount(container);
      return;
    }

    this.allEndpoints = springData.endpoints;
    this.mount(container);
    this.setupFilterListeners();
  }

  public buildRoot(): HTMLElement {
    const springData = (this.analysisData as any)?.spring_analysis;
    if (!springData || !springData.endpoints?.length) {
      return this.renderEmptyState();
    }

    const endpoints = this.allEndpoints;
    const summary = springData.summary || {};
    const beanDeps = springData.beanDependencies || [];
    const springBeans = springData.springBeans || [];

    const filtered = this.activeMethodFilter
      ? endpoints.filter((ep: any) => ep.httpMethod === this.activeMethodFilter)
      : endpoints;

    return this.el('div', null, [
      this.renderHeader(summary, springBeans.length, beanDeps.length),
      this.renderMethodFilters(endpoints),
      this.renderEndpointsListContent(filtered),
    ]);
  }

  private renderEmptyState(): HTMLElement {
    return this.el('div', { className: CLS.EMPTY_STATE }, [
      this.el('div', { className: 'empty-state-icon' }, [this.text(ICON.HTTP.ANY)]),
      this.el('div', { className: 'empty-state-title' }, [this.text(LABEL.API.NO_ENDPOINTS)]),
      this.el('div', { className: CLS.EMPTY_DESC }, [
        this.text(LABEL.API.NO_ENDPOINTS_DESC),
        this.el('br', {}), this.el('br', {}),
        this.text('可能原因:'),
        this.el('br', {}),
        this.text('1. ' + LABEL.API.REASON_1),
        this.el('br', {}),
        this.text('2. ' + LABEL.API.REASON_2),
        this.el('br', {}),
        this.text('3. ' + LABEL.API.REASON_3),
        this.el('br', {}), this.el('br', {}),
        this.text(ICON.UI.INFO + ' ' + LABEL.API.SUGGEST),
      ]),
    ]);
  }

  private renderHeader(summary: any, beanCount: number, depCount: number): HTMLElement {
    const totalEndpoints = summary.totalEndpoints || 0;
    const httpBreakdown = summary.httpMethodBreakdown || {};

    const cards: Child[] = [
      this.el('div', { className: CLS.STAT_CARD }, [
        this.el('div', { className: 'stat-card-icon' }, [this.text(ICON.HTTP.ANY)]),
        this.el('div', { className: 'stat-card-value' }, [this.text(totalEndpoints)]),
        this.el('div', { className: 'stat-card-label' }, [this.text(LABEL.API.LABEL)]),
      ]),
      this.el('div', { className: CLS.STAT_CARD }, [
        this.el('div', { className: 'stat-card-icon' }, [this.text('📦')]),
        this.el('div', { className: 'stat-card-value' }, [this.text(beanCount)]),
        this.el('div', { className: 'stat-card-label' }, [this.text(LABEL.API.BEANS)]),
      ]),
      this.el('div', { className: CLS.STAT_CARD }, [
        this.el('div', { className: 'stat-card-icon' }, [this.text('🔀')]),
        this.el('div', { className: 'stat-card-value' }, [this.text(depCount)]),
        this.el('div', { className: 'stat-card-label' }, [this.text(LABEL.API.BEAN_DEPS)]),
      ]),
    ];

    for (const [method, count] of Object.entries(httpBreakdown)) {
      const icon = Style.httpIcon(method);
      const color = Style.httpMethod(method);
      cards.push(
        this.el('div', { className: CLS.STAT_CARD }, [
          this.el('div', { className: 'stat-card-icon', style: { color } as Partial<CSSStyleDeclaration> }, [this.text(icon)]),
          this.el('div', { className: 'stat-card-value' }, [this.text(String(count))]),
          this.el('div', { className: 'stat-card-label' }, [this.text(method)]),
        ])
      );
    }

    return this.el('div', { className: CLS.CARD_GRID }, cards);
  }

  private renderMethodFilters(endpoints: any[]): HTMLElement {
    const methodCounts: Record<string, number> = {};
    endpoints.forEach(ep => {
      const method = ep.httpMethod || 'ANY';
      methodCounts[method] = (methodCounts[method] || 0) + 1;
    });

    const buttons: Child[] = [
      this.el('button', {
        className: `quality-filter-btn${!this.activeMethodFilter ? ' active' : ''}`,
        'data-filter': 'all',
      }, [this.text(LABEL.API.FILTER_ALL(endpoints.length))]),
    ];

    for (const [method, count] of Object.entries(methodCounts)) {
      buttons.push(
        this.el('button', {
          className: 'quality-filter-btn',
          'data-filter': method,
          'data-method': method,
        }, [this.text(`${Style.httpIcon(method)} ${method} (${count})`)])
      );
    }

    return this.el('div', { className: 'quality-filters' }, buttons);
  }

  private renderEndpointsListContent(endpoints: any[]): HTMLElement {
    if (endpoints.length === 0) {
      return this.el('div', { className: CLS.EMPTY_STATE, style: { padding: '40px' } as Partial<CSSStyleDeclaration> }, [
        this.el('div', { className: 'empty-state-icon' }, [this.text('🎉')]),
        this.el('div', { className: 'empty-state-title' }, [this.text(LABEL.API.NO_MATCH)]),
      ]);
    }

    const groupedByClass: Record<string, any[]> = {};
    endpoints.forEach(ep => {
      const className = ep.class || 'Unknown';
      if (!groupedByClass[className]) groupedByClass[className] = [];
      groupedByClass[className].push(ep);
    });

    const groups = Object.entries(groupedByClass).map(([className, eps]) =>
      this.renderControllerGroup(className, eps)
    );

    return this.el('div', null, [
      this.el('div', { className: 'section-header' }, [
        this.el('span', null, [this.text('📋')]),
        this.el('span', null, [this.text(LABEL.API.HEADER(endpoints.length))]),
      ]),
      ...groups,
    ]);
  }

  private renderControllerGroup(className: string, endpoints: any[]): HTMLElement {
    const cards = endpoints.map(ep => this.renderEndpointCard(ep));

    return this.el('div', { style: { marginBottom: '24px' } as Partial<CSSStyleDeclaration> }, [
      this.el('div', {
        style: { background: Style.bg.cardDark, borderRadius: '12px', padding: '16px', marginBottom: '12px', border: `1px solid ${Style.border.white10}` } as Partial<CSSStyleDeclaration>,
      }, [
        this.el('div', { style: { fontSize: '16px', fontWeight: '700', color: 'var(--green-primary)', fontFamily: "'Courier New', monospace" } as Partial<CSSStyleDeclaration> },
          [this.text(className)]),
        this.el('div', { style: { fontSize: '12px', color: 'var(--gray-500)', marginTop: '4px' } as Partial<CSSStyleDeclaration> },
          [this.text(LABEL.API.ENDPOINT_COUNT(endpoints.length))]),
      ]),
      this.el('div', { style: { display: 'flex', flexDirection: 'column', gap: '8px' } as Partial<CSSStyleDeclaration> }, cards),
    ]);
  }

  private renderEndpointCard(endpoint: any): HTMLElement {
    const httpMethod = endpoint.httpMethod || 'ANY';
    const path = endpoint.path || '/';
    const description = endpoint.description || '';
    const params = endpoint.parameters || [];
    const color = Style.httpMethod(httpMethod);

    const headerChildren: Child[] = [
      this.el('span', {
        style: { background: color, color: 'white', padding: '3px 10px', borderRadius: '6px', fontSize: '12px', fontWeight: '700' } as Partial<CSSStyleDeclaration>,
      }, [this.text(httpMethod)]),
      this.el('code', { style: { fontFamily: "'Courier New', monospace", fontSize: '14px', color: 'var(--blue-primary)' } as Partial<CSSStyleDeclaration> },
        [this.text(path)]),
    ];

    const children: Child[] = [
      this.el('div', { style: { display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' } as Partial<CSSStyleDeclaration> }, headerChildren),
    ];

    if (description) {
      children.push(this.el('div', { style: { fontSize: '13px', color: 'var(--gray-500)', marginBottom: '8px' } as Partial<CSSStyleDeclaration> },
        [this.text(description)]));
    }

    if (params.length > 0) {
      children.push(this.el('div', { style: { fontSize: '12px', color: 'var(--gray-600)' } as Partial<CSSStyleDeclaration> }, [
        this.el('strong', null, [this.text(LABEL.API.PARAMS)]),
        this.text(params.join(', ')),
      ]));
    }

    children.push(
      this.el('div', { style: { fontSize: '11px', color: 'var(--gray-700)', marginTop: '6px', fontFamily: "'Courier New', monospace" } as Partial<CSSStyleDeclaration> },
        [this.text(`→ ${endpoint.method || 'unknown'} (L${endpoint.line || '?'})`)])
    );

    return this.el('div', {
      style: { background: Style.bg.cardDarker, border: `1px solid ${Style.border.white08}`, borderRadius: '10px', padding: '14px', borderLeft: `4px solid ${color}` } as Partial<CSSStyleDeclaration>,
    }, children);
  }

  private setupFilterListeners(): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = new EventDelegator(this.root);

    this.delegator.on('click', '.quality-filter-btn', (_e, el) => {
      const filter = el.getAttribute('data-filter');
      this.activeMethodFilter = filter === 'all' ? null : filter;

      this.findAll('.quality-filter-btn').forEach(btn => btn.classList.remove('active'));
      el.classList.add('active');

      this.update();
    });
  }

  public cleanup(): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = null;
    this.unmount();
  }

  public getContainer(): HTMLElement | null {
    return document.getElementById(this.containerId);
  }
}
