/**
 * Java Source Analyzer - Metrics Dashboard View
 */

import type { AnalysisResult } from '../types';
import { CONFIG } from '../config';
import { Logger } from '../utils/logger';

declare const echarts: any;

export class MetricsDashboardView {
  private containerId: string;
  private analysisData: AnalysisResult | null = null;
  private charts: echarts.ECharts[] = [];

  constructor(containerId: string = 'metrics-content') {
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
      container.innerHTML = this.renderEmptyState();
      return;
    }

    container.innerHTML = `
      ${this.renderOverviewCards(codeMetrics)}
      ${this.renderComplexityChart()}
      ${this.renderDistributionChart()}
    `;

    setTimeout(() => {
      this.initializeCharts(codeMetrics);
    }, 100);
  }

  private renderEmptyState(): string {
    return `
      <div class="empty-state">
        <div class="empty-state-icon">📊</div>
        <div class="empty-state-title">暂无代码指标数据</div>
        <div class="empty-state-desc">
          当前数据文件未包含代码指标分析结果。<br><br>
          💡 <strong>建议:</strong> 使用最新版本的 Java 分析工具重新分析项目，
          即可获取 LOC、复杂度、耦合度、内聚度等详细指标。
        </div>
      </div>
    `;
  }

  private renderOverviewCards(metrics: any): string {
    return `
      <div class="metrics-card-grid">
        <div class="metrics-stat-card">
          <div class="metrics-stat-icon">📝</div>
          <div class="metrics-stat-value">${metrics.total_classes || 0}</div>
          <div class="metrics-stat-label">总类数</div>
        </div>
        <div class="metrics-stat-card">
          <div class="metrics-stat-icon">⚙️</div>
          <div class="metrics-stat-value">${metrics.total_methods || 0}</div>
          <div class="metrics-stat-label">总方法数</div>
        </div>
        <div class="metrics-stat-card">
          <div class="metrics-stat-icon">📄</div>
          <div class="metrics-stat-value">${metrics.total_loc || 0}</div>
          <div class="metrics-stat-label">代码行数</div>
        </div>
        <div class="metrics-stat-card">
          <div class="metrics-stat-icon">💬</div>
          <div class="metrics-stat-value">${((metrics.comment_ratio || 0) * 100).toFixed(1)}%</div>
          <div class="metrics-stat-label">注释率</div>
        </div>
        <div class="metrics-stat-card">
          <div class="metrics-stat-icon">🔀</div>
          <div class="metrics-stat-value">${(metrics.avg_complexity || 0).toFixed(1)}</div>
          <div class="metrics-stat-label">平均复杂度</div>
        </div>
        <div class="metrics-stat-card">
          <div class="metrics-stat-icon">🎯</div>
          <div class="metrics-stat-value">${(metrics.cohesion_index || 0).toFixed(2)}</div>
          <div class="metrics-stat-label">内聚指数</div>
        </div>
      </div>
    `;
  }

  private renderComplexityChart(): string {
    return `
      <div class="metrics-chart">
        <div class="metrics-chart-title">📈 复杂度分布</div>
        <div id="complexity-chart" class="metrics-chart-container"></div>
      </div>
    `;
  }

  private renderDistributionChart(): string {
    return `
      <div class="metrics-chart">
        <div class="metrics-chart-title">📊 类类型分布</div>
        <div id="distribution-chart" class="metrics-chart-container"></div>
      </div>
    `;
  }

  private initializeCharts(metrics: any): void {
    this.initComplexityChart(metrics);
    this.initDistributionChart();
  }

  private initComplexityChart(metrics: any): void {
    const chartDom = document.getElementById('complexity-chart');
    if (!chartDom) return;

    const chart = echarts.init(chartDom);
    this.charts.push(chart);

    // Use our color system (Blue gradient)
    chart.setOption({
      backgroundColor: 'transparent',
      tooltip: { trigger: 'axis' as const, axisPointer: { type: 'shadow' as const } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category' as const,
        data: ['平均复杂度', '最大复杂度', '平均继承深度', '最大继承深度', '平均方法长度'],
        axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.15)' } },
        axisLabel: { color: '#94a3b8', rotate: 30, fontSize: 10 }
      },
      yAxis: {
        type: 'value' as const,
        axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.15)' } },
        axisLabel: { color: '#94a3b8', fontSize: 10 },
        splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.08)' } }
      },
      series: [{
        data: [
          metrics.avg_complexity || 0,
          metrics.max_complexity || 0,
          metrics.avg_inheritance_depth || 0,
          metrics.max_inheritance_depth || 0,
          metrics.avg_method_length || 0
        ],
        type: 'bar' as const,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: CONFIG.colorMap.INTERFACE },  // Blue #60a5fa
            { offset: 1, color: CONFIG.colorMap.ABSTRACT_CLASS } // Purple #a78bfa
          ])
        },
        barWidth: '50%'
      }]
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

    // Map kinds to our color rules
    const data = Object.entries(typeCounts).map(([name, value]) => ({ 
      name, 
      value,
      itemStyle: { color: CONFIG.colorMap[name as keyof typeof CONFIG.colorMap] || '#94a3b8' }
    }));

    const kindLabels: Record<string, string> = {
      INTERFACE: '🔵 接口',
      ABSTRACT_CLASS: '🟣 抽象类',
      CLASS: '🟢 实现类',
      ENUM: '🟠 枚举',
      UTILITY: '⚪️ 普通类'
    };

    chart.setOption({
      backgroundColor: 'transparent',
      tooltip: { 
        trigger: 'item' as const, 
        formatter: '{b}: {c} ({d}%)' 
      },
      legend: { 
        orient: 'vertical' as const, 
        left: 'left', 
        textStyle: { color: '#cbd5e1', fontSize: 11 },
        formatter: (name: string) => {
          const kindKey = name.replace('🔵 ', '').replace('🟣 ', '').replace('🟢 ', '').replace('🟠 ', '').replace('⚪️ ', '');
          const emoji = kindLabels[name] || name;
          return emoji;
        }
      },
      series: [{
        name: '类类型',
        type: 'pie' as const,
        radius: ['40%', '70%'],
        center: ['55%', '50%'],
        label: {
          color: '#cbd5e1',
          fontSize: 11,
          formatter: '{b}: {d}%'
        },
        labelLine: { lineStyle: { color: '#94a3b8' } },
        data: data.map(d => ({
          ...d,
          name: kindLabels[d.name] || d.name
        }))
      }]
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
}
