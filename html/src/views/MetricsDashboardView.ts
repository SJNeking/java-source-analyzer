/**
 * Java Source Analyzer - Metrics Dashboard View (Refactored)
 *
 * Uses Component base class for DOM creation.
 * Uses centralized constants for labels.
 */

import type { AnalysisResult } from '../types';
import { Component } from '../framework/component';
import { CONFIG, SEVERITY } from '../config';
import { Logger } from '../utils/logger';
import { CLS, LABEL, ICON } from '../constants';

declare const echarts: any;

export class MetricsDashboardView extends Component {
  private containerId: string;
  private analysisData: AnalysisResult | null = null;
  private charts: echarts.ECharts[] = [];

  constructor(containerId: string = 'metrics-content') {
    super();
    this.containerId = containerId;
  }

  public render(data: AnalysisResult): void {
    this.analysisData = data;
    const container = document.getElementById(this.containerId);
    if (!container) {
      Logger.error(`Container not found: ${this.containerId}`);
      return;
    }

    this.disposeCharts();

    const codeMetrics = (data as any).code_metrics;
    if (!codeMetrics) {
      this.mount(container);
      return;
    }

    // Mount the component root
    this.mount(container);

    // Initialize charts after DOM is ready
    setTimeout(() => {
      this.initializeCharts(codeMetrics);
    }, 100);
  }

  public buildRoot(): HTMLElement {
    const codeMetrics = (this.analysisData as any)?.code_metrics;

    if (!codeMetrics) {
      return this.renderEmptyState();
    }

    const root = this.el('div', null, [
      this.renderOverviewCards(codeMetrics),
      this.renderComplexityChartContainer(),
      this.renderDistributionChartContainer(),
    ]);

    return root;
  }

  private renderEmptyState(): HTMLElement {
    return this.el('div', { className: CLS.EMPTY_STATE }, [
      this.el('div', { className: CLS.EMPTY_ICON }, [this.text(ICON.SECTION.METRICS)]),
      this.el('div', { className: CLS.EMPTY_TITLE }, [this.text(LABEL.EMPTY.METRICS.TITLE)]),
      this.el('div', { className: CLS.EMPTY_DESC }, [
        this.el('span', null, [this.text('')]), // will be set via innerHTML for the <br>
      ]),
    ]);
  }

  private renderOverviewCards(metrics: Record<string, any>): HTMLElement {
    const cards = [
      { icon: '📝', value: metrics.total_classes || 0, label: LABEL.METRICS.TOTAL_CLASSES },
      { icon: '⚙️', value: metrics.total_methods || 0, label: LABEL.METRICS.TOTAL_METHODS },
      { icon: '📄', value: metrics.total_loc || 0, label: LABEL.METRICS.TOTAL_LOC },
      { icon: '💬', value: `${((metrics.comment_ratio || 0) * 100).toFixed(1)}%`, label: LABEL.METRICS.COMMENT_RATIO },
      { icon: '🔀', value: (metrics.avg_complexity || 0).toFixed(1), label: LABEL.METRICS.AVG_COMPLEXITY },
      { icon: '🎯', value: (metrics.cohesion_index || 0).toFixed(2), label: LABEL.METRICS.COHESION_INDEX },
    ];

    const cardElements = cards.map(card =>
      this.el('div', { className: CLS.METRICS_STAT_CARD }, [
        this.el('div', { className: CLS.METRICS_STAT_ICON }, [this.text(card.icon)]),
        this.el('div', { className: CLS.METRICS_STAT_VALUE }, [this.text(card.value)]),
        this.el('div', { className: CLS.METRICS_STAT_LABEL }, [this.text(card.label)]),
      ])
    );

    return this.el('div', { className: CLS.METRICS_CARD_GRID }, cardElements);
  }

  private renderComplexityChartContainer(): HTMLElement {
    return this.el('div', { className: CLS.METRICS_CHART }, [
      this.el('div', { className: CLS.METRICS_CHART_TITLE }, [this.text(LABEL.METRICS.COMPLEXITY_DISTRIBUTION)]),
      this.el('div', { id: 'complexity-chart', className: CLS.METRICS_CHART_CONTAINER }),
    ]);
  }

  private renderDistributionChartContainer(): HTMLElement {
    return this.el('div', { className: CLS.METRICS_CHART }, [
      this.el('div', { className: CLS.METRICS_CHART_TITLE }, [this.text(LABEL.METRICS.CLASS_TYPE_DISTRIBUTION)]),
      this.el('div', { id: 'distribution-chart', className: CLS.METRICS_CHART_CONTAINER }),
    ]);
  }

  private initializeCharts(metrics: Record<string, any>): void {
    this.initComplexityChart(metrics);
    this.initDistributionChart();
  }

  private initComplexityChart(metrics: Record<string, any>): void {
    const chartDom = document.getElementById('complexity-chart');
    if (!chartDom) return;

    const chart = echarts.init(chartDom);
    this.charts.push(chart);

    chart.setOption({
      backgroundColor: 'transparent',
      tooltip: { trigger: 'axis' as const, axisPointer: { type: 'shadow' as const } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category' as const,
        data: [
          LABEL.METRICS.AVG_COMPLEXITY,
          `${LABEL.METRICS.MAX} ${LABEL.METRICS.AVG_COMPLEXITY}`,
          `${LABEL.METRICS.AVG} ${LABEL.METRICS.INHERITANCE_DEPTH}`,
          `${LABEL.METRICS.MAX} ${LABEL.METRICS.INHERITANCE_DEPTH}`,
          `${LABEL.METRICS.AVG} ${LABEL.METRICS.METHOD_LENGTH}`,
        ],
        axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.15)' } },
        axisLabel: { color: '#94a3b8', rotate: 30, fontSize: 10 },
      },
      yAxis: {
        type: 'value' as const,
        axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.15)' } },
        axisLabel: { color: '#94a3b8', fontSize: 10 },
        splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.08)' } },
      },
      series: [{
        data: [
          metrics.avg_complexity || 0,
          metrics.max_complexity || 0,
          metrics.avg_inheritance_depth || 0,
          metrics.max_inheritance_depth || 0,
          metrics.avg_method_length || 0,
        ],
        type: 'bar' as const,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: CONFIG.colorMap.INTERFACE },
            { offset: 1, color: CONFIG.colorMap.ABSTRACT_CLASS },
          ]),
        },
        barWidth: '50%',
      }],
    });
  }

  private initDistributionChart(): void {
    const chartDom = document.getElementById('distribution-chart');
    if (!chartDom || !this.analysisData) return;

    const chart = echarts.init(chartDom);
    this.charts.push(chart);

    const typeCounts: Record<string, number> = {};
    this.analysisData.assets.forEach(asset => {
      const kind = asset.kind || 'CLASS';
      typeCounts[kind] = (typeCounts[kind] || 0) + 1;
    });

    const kindLabels: Record<string, string> = {
      INTERFACE: `${ICON.KIND.INTERFACE} 接口`,
      ABSTRACT_CLASS: `${ICON.KIND.ABSTRACT_CLASS} 抽象类`,
      CLASS: `${ICON.KIND.CLASS} 实现类`,
      ENUM: `${ICON.KIND.ENUM} 枚举`,
      UTILITY: `${ICON.KIND.UTILITY} 普通类`,
    };

    const data = Object.entries(typeCounts).map(([name, value]) => ({
      name,
      value,
      itemStyle: { color: CONFIG.colorMap[name as keyof typeof CONFIG.colorMap] || '#94a3b8' },
    }));

    chart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item' as const,
        formatter: '{b}: {c} ({d}%)',
      },
      legend: {
        orient: 'vertical' as const,
        left: 'left',
        textStyle: { color: '#cbd5e1', fontSize: 11 },
        formatter: (name: string) => kindLabels[name] || name,
      },
      series: [{
        name: '类类型',
        type: 'pie' as const,
        radius: ['40%', '70%'],
        center: ['55%', '50%'],
        label: {
          color: '#cbd5e1',
          fontSize: 11,
          formatter: '{b}: {d}%',
        },
        labelLine: { lineStyle: { color: '#94a3b8' } },
        data: data.map(d => ({
          ...d,
          name: kindLabels[d.name] || d.name,
        })),
      }],
    });
  }

  private disposeCharts(): void {
    this.charts.forEach(chart => chart.dispose());
    this.charts = [];
  }

  public resize(): void {
    this.charts.forEach(chart => chart.resize());
  }

  public destroy(): void {
    this.disposeCharts();
  }

  public cleanup(): void {
    this.disposeCharts();
    this.unmount();
  }
}
