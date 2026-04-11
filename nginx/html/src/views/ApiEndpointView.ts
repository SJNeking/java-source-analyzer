/**
 * Java Source Analyzer - API Endpoint View
 */

import type { AnalysisResult } from '../types';
import { Logger } from '../utils/logger';

export class ApiEndpointView {
  private containerId: string;
  private analysisData: any = null;
  private activeMethodFilter: string | null = null;

  constructor(containerId: string = 'api-endpoints-content') {
    this.containerId = containerId;
  }

  public render(data: AnalysisResult): void {
    this.analysisData = data;
    const container = document.getElementById(this.containerId);

    if (!container) {
      Logger.error(`Container not found: ${this.containerId}`);
      return;
    }

    const springData = (data as any).spring_analysis;

    if (!springData || !springData.endpoints || springData.endpoints.length === 0) {
      container.innerHTML = this.renderEmptyState();
      return;
    }

    const endpoints = springData.endpoints;
    const summary = springData.summary || {};
    const beanDeps = springData.beanDependencies || [];
    const springBeans = springData.springBeans || [];

    container.innerHTML = `
      ${this.renderHeader(summary, springBeans.length, beanDeps.length)}
      ${this.renderMethodFilters(endpoints)}
      <div id="endpoints-list-container"></div>
    `;

    this.renderEndpointsList(endpoints);
  }

  private renderEmptyState(): string {
    return `
      <div class="empty-state">
        <div class="empty-state-icon">🔗</div>
        <div class="empty-state-title">暂无 API 端点数据</div>
        <div class="empty-state-desc">
          当前项目未检测到 Spring API 端点。<br><br>
          <strong>可能原因:</strong><br>
          <strong>1.</strong> 项目未使用 Spring Boot 框架<br>
          <strong>2.</strong> 未使用 @RestController / @Controller 注解<br>
          <strong>3.</strong> 数据文件是早期版本，未包含 Spring 分析<br><br>
          💡 <strong>建议:</strong> 确保项目是 Spring Boot 项目，使用最新版分析工具重新分析。
        </div>
      </div>
    `;
  }

  private renderHeader(summary: any, beanCount: number, depCount: number): string {
    const totalEndpoints = summary.totalEndpoints || 0;
    const httpBreakdown = summary.httpMethodBreakdown || {};

    return `
      <div class="card-grid">
        <div class="stat-card">
          <div class="stat-card-icon">🔗</div>
          <div class="stat-card-value">${totalEndpoints}</div>
          <div class="stat-card-label">API 端点</div>
        </div>
        <div class="stat-card">
          <div class="stat-card-icon">📦</div>
          <div class="stat-card-value">${beanCount}</div>
          <div class="stat-card-label">Spring Beans</div>
        </div>
        <div class="stat-card">
          <div class="stat-card-icon">🔀</div>
          <div class="stat-card-value">${depCount}</div>
          <div class="stat-card-label">Bean 依赖</div>
        </div>
        ${Object.entries(httpBreakdown).map(([method, count]) => `
          <div class="stat-card">
            <div class="stat-card-icon">${this.getHttpMethodIcon(method)}</div>
            <div class="stat-card-value">${count}</div>
            <div class="stat-card-label">${method}</div>
          </div>
        `).join('')}
      </div>
    `;
  }

  private renderMethodFilters(endpoints: any[]): string {
    const methodCounts: Record<string, number> = {};
    endpoints.forEach(ep => {
      const method = ep.httpMethod || 'ANY';
      methodCounts[method] = (methodCounts[method] || 0) + 1;
    });

    return `
      <div class="quality-filters">
        <button class="quality-filter-btn ${!this.activeMethodFilter ? 'active' : ''}"
                onclick="apiEndpointView.setMethodFilter(null, this)">
          全部 (${endpoints.length})
        </button>
        ${Object.entries(methodCounts).map(([method, count]) => `
          <button class="quality-filter-btn" data-method="${method}"
                  onclick="apiEndpointView.setMethodFilter('${method}', this)">
            ${this.getHttpMethodIcon(method)} ${method} (${count})
          </button>
        `).join('')}
      </div>
    `;
  }

  private renderEndpointsList(endpoints: any[]): void {
    const listContainer = document.getElementById('endpoints-list-container');
    if (!listContainer) return;

    const filteredEndpoints = this.activeMethodFilter
      ? endpoints.filter(ep => ep.httpMethod === this.activeMethodFilter)
      : endpoints;

    if (filteredEndpoints.length === 0) {
      listContainer.innerHTML = `<div class="empty-state" style="padding: 40px;"><div class="empty-state-icon">🎉</div><div class="empty-state-title">无匹配的端点</div></div>`;
      return;
    }

    const groupedByClass: Record<string, any[]> = {};
    filteredEndpoints.forEach(ep => {
      const className = ep.class || 'Unknown';
      if (!groupedByClass[className]) groupedByClass[className] = [];
      groupedByClass[className].push(ep);
    });

    listContainer.innerHTML = `
      <div class="section-header"><span>📋</span><span>API 端点 (${filteredEndpoints.length})</span></div>
      ${Object.entries(groupedByClass).map(([className, eps]) => this.renderControllerGroup(className, eps)).join('')}
    `;
  }

  private renderControllerGroup(className: string, endpoints: any[]): string {
    return `
      <div style="margin-bottom: 24px;">
        <div style="background: rgba(20, 25, 40, 0.85); border-radius: 12px; padding: 16px; margin-bottom: 12px; border: 1px solid rgba(255, 255, 255, 0.1);">
          <div style="font-size: 16px; font-weight: 700; color: #48bb78; font-family: 'Courier New', monospace;">${className}</div>
          <div style="font-size: 12px; color: #a0aec0; margin-top: 4px;">${endpoints.length} 个端点</div>
        </div>
        <div style="display: flex; flex-direction: column; gap: 8px;">
          ${endpoints.map(ep => this.renderEndpointCard(ep)).join('')}
        </div>
      </div>
    `;
  }

  private renderEndpointCard(endpoint: any): string {
    const httpMethod = endpoint.httpMethod || 'ANY';
    const path = endpoint.path || '/';
    const description = endpoint.description || '';
    const params = endpoint.parameters || [];

    return `
      <div style="background: rgba(20, 25, 40, 0.6); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 10px; padding: 14px; border-left: 4px solid ${this.getHttpMethodColor(httpMethod)};">
        <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 8px;">
          <span style="background: ${this.getHttpMethodColor(httpMethod)}; color: white; padding: 3px 10px; border-radius: 6px; font-size: 12px; font-weight: 700;">${httpMethod}</span>
          <code style="font-family: 'Courier New', monospace; font-size: 14px; color: #4299e1;">${path}</code>
        </div>
        ${description ? `<div style="font-size: 13px; color: #a0aec0; margin-bottom: 8px;">${description}</div>` : ''}
        ${params.length > 0 ? `<div style="font-size: 12px; color: #718096;"><strong>参数:</strong> ${params.join(', ')}</div>` : ''}
        <div style="font-size: 11px; color: #4a5568; margin-top: 6px; font-family: 'Courier New', monospace;">→ ${endpoint.method || 'unknown'} (L${endpoint.line || '?'})</div>
      </div>
    `;
  }

  public setMethodFilter(method: string | null, element: HTMLElement): void {
    this.activeMethodFilter = method;
    const buttons = element.parentElement?.querySelectorAll('.quality-filter-btn');
    buttons?.forEach(btn => btn.classList.remove('active'));
    element.classList.add('active');

    if (this.analysisData) {
      const springData = (this.analysisData as any).spring_analysis;
      if (springData) this.renderEndpointsList(springData.endpoints || []);
    }
  }

  private getHttpMethodIcon(method: string): string {
    const icons: Record<string, string> = { 'GET': '🟢', 'POST': '🔵', 'PUT': '🟡', 'DELETE': '🔴', 'PATCH': '🟠', 'ANY': '⚪' };
    return icons[method] || '⚪';
  }

  private getHttpMethodColor(method: string): string {
    const colors: Record<string, string> = { 'GET': '#48bb78', 'POST': '#4299e1', 'PUT': '#ed8936', 'DELETE': '#f56565', 'PATCH': '#9f7aea', 'ANY': '#a0aec0' };
    return colors[method] || '#a0aec0';
  }

  public getContainer(): HTMLElement | null {
    return document.getElementById(this.containerId);
  }
}
