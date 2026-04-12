/**
 * Java Source Analyzer - Architecture Layer View (Refactored)
 *
 * Uses Component base class for DOM creation.
 * Uses centralized constants for icons and labels.
 */

import type { AnalysisResult } from '../types';
import { Component, type Child } from '../framework/component';
import { Logger } from '../utils/logger';
import { ICON, LABEL } from '../constants';

interface ArchitectureViolation {
  sourceClass?: string;
  targetClass?: string;
  violationType?: string;
  description?: string;
}

interface ViolationSummary {
  total?: number;
}

export class ArchitectureLayerView extends Component {
  private containerId: string;
  private analysisData: AnalysisResult | null = null;

  constructor(containerId: string = 'architecture-content') {
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
    
    // Call parent mount to render into container
    this.mount(container);
  }

  public buildRoot(): HTMLElement {
    const layerData = (this.analysisData as any)?.architecture_layers;

    if (!layerData) {
      return this.renderEmptyState();
    }

    const layerCounts = layerData.layerCounts || {};
    const violations = layerData.violations || [];
    const violationSummary = layerData.violationSummary || {};
    const layerGraph = layerData.layerGraph || {};

    return this.el('div', null, [
      this.renderHeader(layerCounts, violationSummary),
      this.renderLayerGraph(layerGraph, layerCounts),
      this.renderViolations(violations),
    ]);
  }

  private renderEmptyState(): HTMLElement {
    return this.el('div', { className: 'empty-state' }, [
      this.el('div', { className: 'empty-state-icon' }, [this.text(ICON.LAYER.UTIL)]),
      this.el('div', { className: 'empty-state-title' }, [this.text('暂无架构分层数据')]),
      this.el('div', { className: 'empty-state-desc' }, [
        this.el('span', null, [this.text('当前数据文件未包含架构分层分析结果。')]),
        this.el('br', {}),
        this.el('br', {}),
        this.el('span', null, [this.text('架构分层分析可以:')]),
        this.el('br', {}),
        this.el('span', null, [this.text('• 自动识别 Controller / Service / Repository / Entity 层')]),
        this.el('br', {}),
        this.el('span', null, [this.text('• 显示层间依赖关系矩阵')]),
        this.el('br', {}),
        this.el('span', null, [this.text('• 检测架构违规（如 Controller 直接调用 Repository）')]),
        this.el('br', {}),
        this.el('br', {}),
        this.el('span', null, [this.text('💡 使用最新版本的 Java 分析工具重新分析项目。')]),
      ]),
    ]);
  }

  private renderHeader(layerCounts: Record<string, number>, violationSummary: ViolationSummary): HTMLElement {
    const totalViolations = violationSummary.total || 0;
    const cards: HTMLElement[] = [];

    // Layer count cards
    for (const [layer, count] of Object.entries(layerCounts)) {
      const icon = ICON.LAYER[layer as keyof typeof ICON.LAYER] || '📁';
      cards.push(
        this.el('div', { className: 'stat-card' }, [
          this.el('div', { className: 'stat-card-icon' }, [this.text(icon)]),
          this.el('div', { className: 'stat-card-value' }, [this.text(count)]),
          this.el('div', { className: 'stat-card-label' }, [this.text(layer)]),
        ])
      );
    }

    // Violations card
    const violationIcon = totalViolations > 0 ? ICON.UI.WARNING : ICON.UI.CHECK;
    const violationColor = totalViolations > 0 ? '#f56565' : '#48bb78';
    const borderColor = totalViolations > 0 ? 'rgba(245, 101, 101, 0.4)' : 'rgba(72, 187, 120, 0.4)';

    cards.push(
      this.el('div', { className: 'stat-card', style: { border: `1px solid ${borderColor}` } as Partial<CSSStyleDeclaration> }, [
        this.el('div', { className: 'stat-card-icon' }, [this.text(violationIcon)]),
        this.el('div', { className: 'stat-card-value', style: { color: violationColor } as Partial<CSSStyleDeclaration> }, [this.text(totalViolations)]),
        this.el('div', { className: 'stat-card-label' }, [this.text('架构违规')]),
      ])
    );

    return this.el('div', { className: 'card-grid' }, cards);
  }

  private renderLayerGraph(layerGraph: Record<string, Record<string, number>>, layerCounts: Record<string, number>): HTMLElement {
    const layers = Object.keys(layerCounts);
    if (layers.length === 0) return this.el('div', null, []);

    // Build table header
    const headerCells: Child[] = [
      this.el('th', null, [this.text('源层 → 目标层')]),
      ...layers.map(l => this.el('th', null, [this.text(l)])),
    ];

    // Build table rows
    const rows = layers.map(sourceLayer => {
      const cells: Child[] = [
        this.el('td', { style: { fontWeight: '600', color: 'var(--blue-primary)' } as Partial<CSSStyleDeclaration> }, [this.text(sourceLayer)]),
        ...layers.map(targetLayer => {
          const count = layerGraph[sourceLayer]?.[targetLayer] || 0;
          return this.el('td', {
            style: { textAlign: 'center', color: count > 0 ? 'var(--gray-300)' : 'var(--gray-700)' } as Partial<CSSStyleDeclaration>,
          }, [this.text(count > 0 ? count : '-')]);
        }),
      ];
      return this.el('tr', null, cells);
    });

    const thead = this.el('thead', null, [this.el('tr', null, headerCells)]);
    const tbody = this.el('tbody', null, rows);
    const table = this.el('table', { className: 'relations-table' }, [thead, tbody]);

    return this.el('div', { className: 'metrics-chart' }, [
      this.el('div', { className: 'metrics-chart-title' }, [this.text('🔀 层间依赖关系')]),
      table,
    ]);
  }

  private renderViolations(violations: ArchitectureViolation[]): HTMLElement {
    if (violations.length === 0) {
      return this.el('div', {
        className: 'metrics-chart',
        style: { borderColor: 'rgba(72, 187, 120, 0.3)' } as Partial<CSSStyleDeclaration>,
      }, [
        this.el('div', {
          className: 'metrics-chart-title',
          style: { color: 'var(--green-primary)' } as Partial<CSSStyleDeclaration>,
        }, [this.text(LABEL.ARCHITECTURE.NO_VIOLATIONS)]),
        this.el('div', { style: { textAlign: 'center', padding: '40px', color: 'var(--gray-500)' } as Partial<CSSStyleDeclaration> },
          [this.text('恭喜！未检测到架构分层违规。')]),
      ]);
    }

    const items = violations.slice(0, 50).map(v => this.renderViolation(v));
    return this.el('div', null, [
      this.el('div', { className: 'section-header' }, [
        this.el('span', null, [this.text(ICON.UI.WARNING)]),
        this.el('span', null, [this.text(LABEL.ARCHITECTURE.VIOLATIONS(violations.length))]),
      ]),
      this.el('div', { className: 'quality-list' }, items),
    ]);
  }

  private renderViolation(v: ArchitectureViolation): HTMLElement {
    const severity = v.violationType?.includes('CONTROLLER_TO_REPOSITORY') ? 'MAJOR' : 'MINOR';

    const children: Child[] = [
      this.el('div', { className: 'quality-item-header' }, [
        this.el('span', { className: `quality-severity ${severity}` }, [this.text(severity)]),
        this.el('span', { className: 'quality-class' }, [this.text(`${v.sourceClass || ''} → ${v.targetClass || ''}`)]),
      ]),
      this.el('div', { className: 'quality-message' }, [this.text(v.description || '')]),
      this.el('div', { className: 'quality-meta' }, [
        this.el('strong', null, [this.text('类型: ')]),
        this.text(v.violationType || ''),
      ]),
    ];

    return this.el('div', { className: `quality-item ${severity}` }, children);
  }

  public getContainer(): HTMLElement | null {
    return document.getElementById(this.containerId);
  }
}
