/**
 * Java Source Analyzer - Architecture Layer View
 */

import type { AnalysisResult } from '../types';
import { Logger } from '../utils/logger';

export class ArchitectureLayerView {
  private containerId: string;
  private analysisData: any = null;

  constructor(containerId: string = 'architecture-content') {
    this.containerId = containerId;
  }

  public render(data: AnalysisResult): void {
    this.analysisData = data;
    const container = document.getElementById(this.containerId);

    if (!container) {
      Logger.error(`Container not found: ${this.containerId}`);
      return;
    }

    const layerData = (data as any).architecture_layers;

    if (!layerData) {
      container.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">🏗️</div>
          <div class="empty-state-title">暂无架构分层数据</div>
          <div class="empty-state-desc">
            当前数据文件未包含架构分层分析结果。<br><br>
            <strong>架构分层分析可以:</strong><br>
            • 自动识别 Controller / Service / Repository / Entity 层<br>
            • 显示层间依赖关系矩阵<br>
            • 检测架构违规（如 Controller 直接调用 Repository）<br><br>
            💡 <strong>建议:</strong> 使用最新版本的 Java 分析工具重新分析项目。
          </div>
        </div>
      `;
      return;
    }

    const layerCounts = layerData.layerCounts || {};
    const violations = layerData.violations || [];
    const violationSummary = layerData.violationSummary || {};
    const layerGraph = layerData.layerGraph || {};

    container.innerHTML = `
      ${this.renderHeader(layerCounts, violationSummary)}
      ${this.renderLayerGraph(layerGraph, layerCounts)}
      ${this.renderViolations(violations)}
    `;
  }

  private renderHeader(layerCounts: Record<string, number>, violationSummary: any): string {
    const totalViolations = violationSummary.total || 0;
    const layerIcons: Record<string, string> = { 'CONTROLLER': '🌐', 'SERVICE': '⚙️', 'REPOSITORY': '🗄️', 'ENTITY': '📦', 'CONFIG': '⚙️', 'UTIL': '🔧' };

    return `
      <div class="card-grid">
        ${Object.entries(layerCounts).map(([layer, count]) => `
          <div class="stat-card">
            <div class="stat-card-icon">${layerIcons[layer] || '📁'}</div>
            <div class="stat-card-value">${count}</div>
            <div class="stat-card-label">${layer}</div>
          </div>
        `).join('')}
        <div class="stat-card" style="border: 1px solid ${totalViolations > 0 ? 'rgba(245, 101, 101, 0.4)' : 'rgba(72, 187, 120, 0.4)'};">
          <div class="stat-card-icon">${totalViolations > 0 ? '⚠️' : '✅'}</div>
          <div class="stat-card-value" style="color: ${totalViolations > 0 ? '#f56565' : '#48bb78'}">${totalViolations}</div>
          <div class="stat-card-label">架构违规</div>
        </div>
      </div>
    `;
  }

  private renderLayerGraph(layerGraph: Record<string, Record<string, number>>, layerCounts: Record<string, number>): string {
    const layers = Object.keys(layerCounts);
    if (layers.length === 0) return '';

    return `
      <div class="metrics-chart">
        <div class="metrics-chart-title">🔀 层间依赖关系</div>
        <table class="relations-table">
          <thead>
            <tr><th>源层 → 目标层</th>${layers.map(l => `<th>${l}</th>`).join('')}</tr>
          </thead>
          <tbody>
            ${layers.map(sourceLayer => `
              <tr>
                <td style="font-weight: 600; color: #4299e1;">${sourceLayer}</td>
                ${layers.map(targetLayer => {
                  const count = layerGraph[sourceLayer]?.[targetLayer] || 0;
                  return `<td style="text-align: center; color: ${count > 0 ? '#e2e8f0' : '#4a5568'};">${count > 0 ? count : '-'}</td>`;
                }).join('')}
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    `;
  }

  private renderViolations(violations: any[]): string {
    if (violations.length === 0) {
      return `
        <div class="metrics-chart" style="border-color: rgba(72, 187, 120, 0.3);">
          <div class="metrics-chart-title" style="color: #48bb78;">✅ 无架构违规</div>
          <div style="text-align: center; padding: 40px; color: #a0aec0;">恭喜！未检测到架构分层违规。</div>
        </div>
      `;
    }

    return `
      <div class="section-header"><span>⚠️</span><span>架构违规 (${violations.length})</span></div>
      <div class="quality-list">
        ${violations.slice(0, 50).map(v => this.renderViolation(v)).join('')}
      </div>
    `;
  }

  private renderViolation(v: any): string {
    const severity = v.violationType?.includes('CONTROLLER_TO_REPOSITORY') ? 'MAJOR' : 'MINOR';
    return `
      <div class="quality-item ${severity}">
        <div class="quality-item-header">
          <span class="quality-severity ${severity}">${severity}</span>
          <span class="quality-class">${v.sourceClass || ''} → ${v.targetClass || ''}</span>
        </div>
        <div class="quality-message">${v.description || ''}</div>
        <div class="quality-meta"><strong>类型:</strong> ${v.violationType || ''}</div>
      </div>
    `;
  }

  public getContainer(): HTMLElement | null {
    return document.getElementById(this.containerId);
  }
}
