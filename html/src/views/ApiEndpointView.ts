/**
 * Java Source Analyzer - API Endpoint View (Refactored)
 *
 * Uses Component base class for DOM creation.
 * Uses event delegation instead of inline onclick.
 * Uses centralized constants for HTTP method icons.
 */

import type { AnalysisResult } from '../types';
import { Component, type Child } from '../framework/component';
import { EventDelegator } from '../framework/events';
import { Logger } from '../utils/logger';
import { ICON } from '../constants';

const HTTP_METHOD_COLORS: Record<string, string> = {
  GET: '#48bb78', POST: '#4299e1', PUT: '#ed8936', DELETE: '#f56565', PATCH: '#9f7aea', ANY: '#a0aec0',
};

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
    return this.el('div', { className: 'empty-state' }, [
      this.el('div', { className: 'empty-state-icon' }, [this.text(ICON.HTTP.ANY)]),
      this.el('div', { className: 'empty-state-title' }, [this.text('暂无 API 端点数据')]),
      this.el('div', { className: 'empty-state-desc' }, [
        this.text('当前项目未检测到 Spring API 端点。'),
        this.el('br', {}), this.el('br', {}),
        this.text('可能原因:'),
        this.el('br', {}),
        this.text('1. 项目未使用 Spring Boot 框架'),
        this.el('br', {}),
        this.text('2. 未使用 @RestController / @Controller 注解'),
        this.el('br', {}),
        this.text('3. 数据文件是早期版本，未包含 Spring 分析'),
        this.el('br', {}), this.el('br', {}),
        this.text('💡 确保项目是 Spring Boot 项目，使用最新版分析工具重新分析。'),
      ]),
    ]);
  }

  private renderHeader(summary: any, beanCount: number, depCount: number): HTMLElement {
    const totalEndpoints = summary.totalEndpoints || 0;
    const httpBreakdown = summary.httpMethodBreakdown || {};

    const cards: Child[] = [
      this.el('div', { className: 'stat-card' }, [
        this.el('div', { className: 'stat-card-icon' }, [this.text(ICON.HTTP.ANY)]),
        this.el('div', { className: 'stat-card-value' }, [this.text(totalEndpoints)]),
        this.el('div', { className: 'stat-card-label' }, [this.text('API 端点')]),
      ]),
      this.el('div', { className: 'stat-card' }, [
        this.el('div', { className: 'stat-card-icon' }, [this.text('📦')]),
        this.el('div', { className: 'stat-card-value' }, [this.text(beanCount)]),
        this.el('div', { className: 'stat-card-label' }, [this.text('Spring Beans')]),
      ]),
      this.el('div', { className: 'stat-card' }, [
        this.el('div', { className: 'stat-card-icon' }, [this.text('🔀')]),
        this.el('div', { className: 'stat-card-value' }, [this.text(depCount)]),
        this.el('div', { className: 'stat-card-label' }, [this.text('Bean 依赖')]),
      ]),
    ];

    for (const [method, count] of Object.entries(httpBreakdown)) {
      const icon = this.getHttpMethodIcon(method);
      cards.push(
        this.el('div', { className: 'stat-card' }, [
          this.el('div', { className: 'stat-card-icon' }, [this.text(icon)]),
          this.el('div', { className: 'stat-card-value' }, [this.text(String(count))]),
          this.el('div', { className: 'stat-card-label' }, [this.text(method)]),
        ])
      );
    }

    return this.el('div', { className: 'card-grid' }, cards);
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
      }, [this.text(`全部 (${endpoints.length})`)]),
    ];

    for (const [method, count] of Object.entries(methodCounts)) {
      buttons.push(
        this.el('button', {
          className: 'quality-filter-btn',
          'data-filter': method,
          'data-method': method,
        }, [this.text(`${this.getHttpMethodIcon(method)} ${method} (${count})`)])
      );
    }

    return this.el('div', { className: 'quality-filters' }, buttons);
  }

  private renderEndpointsListContent(endpoints: any[]): HTMLElement {
    if (endpoints.length === 0) {
      return this.el('div', { className: 'empty-state', style: { padding: '40px' } as Partial<CSSStyleDeclaration> }, [
        this.el('div', { className: 'empty-state-icon' }, [this.text('🎉')]),
        this.el('div', { className: 'empty-state-title' }, [this.text('无匹配的端点')]),
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
        this.el('span', null, [this.text(`API 端点 (${endpoints.length})`)]),
      ]),
      ...groups,
    ]);
  }

  private renderControllerGroup(className: string, endpoints: any[]): HTMLElement {
    const cards = endpoints.map(ep => this.renderEndpointCard(ep));

    return this.el('div', { style: { marginBottom: '24px' } as Partial<CSSStyleDeclaration> }, [
      this.el('div', {
        style: { background: 'rgba(20, 25, 40, 0.85)', borderRadius: '12px', padding: '16px', marginBottom: '12px', border: '1px solid rgba(255, 255, 255, 0.1)' } as Partial<CSSStyleDeclaration>,
      }, [
        this.el('div', { style: { fontSize: '16px', fontWeight: '700', color: 'var(--green-primary)', fontFamily: "'Courier New', monospace" } as Partial<CSSStyleDeclaration> },
          [this.text(className)]),
        this.el('div', { style: { fontSize: '12px', color: 'var(--gray-500)', marginTop: '4px' } as Partial<CSSStyleDeclaration> },
          [this.text(`${endpoints.length} 个端点`)]),
      ]),
      this.el('div', { style: { display: 'flex', flexDirection: 'column', gap: '8px' } as Partial<CSSStyleDeclaration> }, cards),
    ]);
  }

  private renderEndpointCard(endpoint: any): HTMLElement {
    const httpMethod = endpoint.httpMethod || 'ANY';
    const path = endpoint.path || '/';
    const description = endpoint.description || '';
    const params = endpoint.parameters || [];
    const color = this.getHttpMethodColor(httpMethod);

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
        this.el('strong', null, [this.text('参数: ')]),
        this.text(params.join(', ')),
      ]));
    }

    children.push(
      this.el('div', { style: { fontSize: '11px', color: 'var(--gray-700)', marginTop: '6px', fontFamily: "'Courier New', monospace" } as Partial<CSSStyleDeclaration> },
        [this.text(`→ ${endpoint.method || 'unknown'} (L${endpoint.line || '?'})`)])
    );

    return this.el('div', {
      style: { background: 'rgba(20, 25, 40, 0.6)', border: '1px solid rgba(255, 255, 255, 0.08)', borderRadius: '10px', padding: '14px', borderLeft: `4px solid ${color}` } as Partial<CSSStyleDeclaration>,
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

  private getHttpMethodIcon(method: string): string {
    return ICON.HTTP[method as keyof typeof ICON.HTTP] || '⚪';
  }

  private getHttpMethodColor(method: string): string {
    return HTTP_METHOD_COLORS[method] || '#a0aec0';
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
