/**
 * Java Source Analyzer - Cross-File Relations View
 * Displays cross-file relations with filtering and visualization
 */

import type { AnalysisResult } from '../types';
import { Logger } from '../utils/logger';

/**
 * Cross-File Relations View Controller
 */
export class CrossFileRelationsView {
  /** Container element ID */
  private containerId: string;

  /** Current analysis data */
  private analysisData: AnalysisResult | null = null;

  /** Active relation type filter */
  private activeFilter: string | null = null;

  /**
   * Create a new CrossFileRelationsView
   * @param containerId - ID of the container element
   */
  constructor(containerId: string = 'relations-content') {
    this.containerId = containerId;
  }

  /**
   * Render cross-file relations view
   * @param data - Analysis result data
   */
  public render(data: AnalysisResult): void {
    this.analysisData = data;
    const container = document.getElementById(this.containerId);

    if (!container) {
      Logger.error(`Container not found: ${this.containerId}`);
      return;
    }

    // Check if relations data exists
    const crossFileRelations = (data as any).cross_file_relations;

    if (!crossFileRelations || !crossFileRelations.total_relations || crossFileRelations.total_relations === 0) {
      container.innerHTML = this.renderEmptyState();
      return;
    }

    // Extract relations by type
    const relationsByType = crossFileRelations.relations_by_type || {};
    const relationTypes = Object.keys(relationsByType);

    // Flatten all relations
    const allRelations: any[] = [];
    Object.entries(relationsByType).forEach(([type, relations]: [string, any]) => {
      if (Array.isArray(relations)) {
        relations.forEach((rel: any) => {
          allRelations.push({ ...rel, relation_type: rel.relation_type || type });
        });
      }
    });

    // Render view
    container.innerHTML = `
      ${this.renderHeader(crossFileRelations.total_relations, relationTypes)}
      ${this.renderFilters(relationTypes)}
      <div id="relations-list-container"></div>
    `;

    // Render relations list
    this.renderRelationsList(allRelations);
  }

  /**
   * Render empty state
   */
  private renderEmptyState(): string {
    return `
      <div class="empty-state">
        <div class="empty-state-icon">🔀</div>
        <div class="empty-state-title">暂无跨文件关系数据</div>
        <div class="empty-state-desc">
          当前项目未包含跨文件关系分析结果。请使用 Java 分析工具的完整分析模式重新扫描，
          以获取 Java 文件与 XML、SQL、配置文件等的关联关系。
        </div>
      </div>
    `;
  }

  /**
   * Render header with stats
   */
  private renderHeader(totalRelations: number, relationTypes: string[]): string {
    return `
      <div class="card-grid">
        <div class="stat-card">
          <div class="stat-card-icon">🔀</div>
          <div class="stat-card-value">${totalRelations}</div>
          <div class="stat-card-label">总关系数</div>
        </div>
        <div class="stat-card">
          <div class="stat-card-icon">📊</div>
          <div class="stat-card-value">${relationTypes.length}</div>
          <div class="stat-card-label">关系类型数</div>
        </div>
        ${relationTypes.slice(0, 6).map(type => `
          <div class="stat-card">
            <div class="stat-card-icon">🔗</div>
            <div class="stat-card-value" style="font-size: 14px">${type}</div>
            <div class="stat-card-label">关系类型</div>
          </div>
        `).join('')}
      </div>
    `;
  }

  /**
   * Render relation type filter buttons
   */
  private renderFilters(relationTypes: string[]): string {
    return `
      <div class="quality-filters">
        <button class="quality-filter-btn ${!this.activeFilter ? 'active' : ''}" 
                onclick="relationsView.setFilter(null, this)">
          全部 (${relationTypes.length} 种类型)
        </button>
        ${relationTypes.map(type => `
          <button class="quality-filter-btn" data-type="${type}" 
                  onclick="relationsView.setFilter('${type}', this)">
            ${type}
          </button>
        `).join('')}
      </div>
    `;
  }

  /**
   * Render relations list
   */
  private renderRelationsList(relations: any[]): void {
    const listContainer = document.getElementById('relations-list-container');
    if (!listContainer) return;

    // Filter relations
    const filteredRelations = this.activeFilter
      ? relations.filter(r => r.relation_type === this.activeFilter)
      : relations;

    if (filteredRelations.length === 0) {
      listContainer.innerHTML = `
        <div class="empty-state" style="padding: 40px 20px;">
          <div class="empty-state-icon">🎉</div>
          <div class="empty-state-title">所有关系已过滤</div>
          <div class="empty-state-desc">当前没有符合所选过滤器的关系。</div>
        </div>
      `;
      return;
    }

    listContainer.innerHTML = `
      <div class="section-header">
        <span>📋</span>
        <span>关系列表 (${filteredRelations.length})</span>
      </div>
      <table class="relations-table">
        <thead>
          <tr>
            <th>源文件</th>
            <th>源资产</th>
            <th>目标资产</th>
            <th>关系类型</th>
            <th>置信度</th>
            <th>证据</th>
          </tr>
        </thead>
        <tbody>
          ${filteredRelations.slice(0, 100).map(rel => this.renderRelationRow(rel)).join('')}
        </tbody>
      </table>
      ${filteredRelations.length > 100 ? `
        <div style="text-align: center; padding: 20px; color: #a0aec0; font-size: 13px;">
          显示前 100 条，共 ${filteredRelations.length} 条关系
        </div>
      ` : ''}
    `;
  }

  /**
   * Render individual relation row
   */
  private renderRelationRow(rel: any): string {
    const confidence = rel.confidence || 0;
    const confidencePercent = confidence * 100;

    return `
      <tr>
        <td style="font-family: 'Courier New', monospace; font-size: 12px;">
          ${this.truncatePath(rel.source_path || '', 30)}
        </td>
        <td style="font-family: 'Courier New', monospace; font-size: 12px;">
          ${this.truncateText(rel.source_asset || '', 25)}
        </td>
        <td style="font-family: 'Courier New', monospace; font-size: 12px;">
          ${this.truncateText(rel.target_asset || '', 25)}
        </td>
        <td>
          <span class="relation-type-badge">${rel.relation_type || 'Unknown'}</span>
        </td>
        <td>
          <div style="display: flex; align-items: center; gap: 8px;">
            <div class="confidence-bar">
              <div class="confidence-bar-fill" style="width: ${confidencePercent}%"></div>
            </div>
            <span style="font-size: 11px; color: #a0aec0;">${(confidencePercent).toFixed(0)}%</span>
          </div>
        </td>
        <td style="font-size: 12px; color: #a0aec0; max-width: 200px;">
          ${this.truncateText(rel.evidence || '', 40)}
        </td>
      </tr>
    `;
  }

  /**
   * Truncate text with ellipsis
   */
  private truncateText(text: string, maxLength: number): string {
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  }

  /**
   * Truncate file path, keeping the filename visible
   */
  private truncatePath(path: string, maxLength: number): string {
    if (path.length <= maxLength) return path;
    const parts = path.split('/');
    if (parts.length > 2) {
      return '.../' + parts.slice(-2).join('/');
    }
    return '...' + path.substring(path.length - maxLength + 3);
  }

  /**
   * Set active filter
   */
  public setFilter(type: string | null, element: HTMLElement): void {
    this.activeFilter = type;

    // Update button states
    const buttons = element.parentElement?.querySelectorAll('.quality-filter-btn');
    buttons?.forEach(btn => btn.classList.remove('active'));
    element.classList.add('active');

    // Re-render relations list
    if (this.analysisData) {
      const crossFileRelations = (this.analysisData as any).cross_file_relations;
      if (crossFileRelations) {
        const relationsByType = crossFileRelations.relations_by_type || {};
        const allRelations: any[] = [];
        Object.entries(relationsByType).forEach(([relType, relations]: [string, any]) => {
          if (Array.isArray(relations)) {
            relations.forEach((rel: any) => {
              allRelations.push({ ...rel, relation_type: rel.relation_type || relType });
            });
          }
        });
        this.renderRelationsList(allRelations);
      }
    }
  }

  /**
   * Get container element
   */
  public getContainer(): HTMLElement | null {
    return document.getElementById(this.containerId);
  }
}
