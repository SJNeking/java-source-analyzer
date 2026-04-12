/**
 * Java Source Analyzer - Force Graph View
 * Clean, stable visualization with correct colors and accessibility.
 */

import * as echarts from 'echarts';
import type { GraphData, GraphNode, GraphLink } from '../types';
import { Logger } from '../utils/logger';
import { cleanDescription, isLargeDataset } from '../utils/dom-helpers';
import { safeSetInnerHTML } from '../utils/safe-dom';
import { GraphProcessor } from '../domain/graph-processor';
import { appState } from '../state';
import { COLORS } from '../constants/theme';
import { ICON } from '../constants/icons';
import { Style } from '../utils/style-helpers';
import { C, ICON as ICON_C, LABEL } from '../constants';

// Graph visualization configuration
const GRAPH_CONFIG = {
  // Force layout parameters
  FORCE: {
    REPULSION: 300,
    EDGE_LENGTH: 90,
    GRAVITY: 0.08,
    FRICTION: 0.6,
  },
  // Emphasis effects
  EMPHASIS: {
    LINE_WIDTH: 4,
    SHADOW_BLUR: 15,
  },
  // Link styling
  LINK: {
    WIDTH_MULTIPLIER: 0.1,
    MAX_WIDTH: 4,
    CURVENESS: 0.2,
    SYMBOL_SIZE: [0, 6],
  },
  // Opacity levels
  OPACITY: {
    VIOLATION: 0.8,
    NORMAL: 0.3,
  },
  // Tooltip styling
  TOOLTIP: {
    MAX_WIDTH: 350,
    PADDING: 12,
    BORDER_RADIUS: 8,
    BOX_SHADOW: '0 8px 24px rgba(0,0,0,0.5)',
    NODE_ICON_SIZE: 10,
    NODE_NAME_SIZE: 14,
    FULLNAME_SIZE: 10,
    DESC_SIZE: 11,
    DESC_MAX_LENGTH: 40,
    META_SIZE: 10,
    META_GAP: 10,
    META_GAP_COMPACT: 8,
    META_GAP_WIDE: 12,
    BORDER_WIDTH: 2,
    QUALITY_SIZE: 10,
    EDGE_NAME_SIZE: 12,
    EDGE_META_SIZE: 10,
    EDGE_DESC_SIZE: 11,
    LINE_HEIGHT: 1.4,
    BORDER_TOP_WIDTH: 1,
  },
  // Animation
  ANIMATION: {
    ZOOM_DURATION: 300,
    ZOOM_IN_FACTOR: 1.2,
    ZOOM_OUT_FACTOR: 0.8,
  },
} as const;

// HTML structure helpers for tooltip
const HTML = {
  DIV: (content: string, style: string) => `<div style="${style}">${content}</div>`,
  SPAN: (content: string, style?: string) => style ? `<span style="${style}">${content}</span>` : `<span>${content}</span>`,
  STRONG: (content: string, style?: string) => style ? `<strong style="${style}">${content}</strong>` : `<strong>${content}</strong>`,
  FLEX_CONTAINER: (children: string, style: string) =>
    `<div style="display:flex; ${style}">${children}</div>`,
} as const;

export class ForceGraphView {
  public chart: echarts.ECharts | null = null;
  public currentData: GraphData | null = null;
  private containerId: string;
  private labelsVisible: boolean = false;

  // Filter State
  private activeFilters: Set<string> = new Set([
    'INTERFACE', 'ABSTRACT_CLASS', 'CLASS', 'ENUM', 'UTILITY', 'EXTERNAL'
  ]); // Show all node types by default

  constructor(containerId: string = 'main-graph') {
    this.containerId = containerId;
    this.chart = this.initChart();
    Logger.success('ForceGraphView initialized');
  }

  private initChart(): echarts.ECharts {
    const container = document.getElementById(this.containerId);
    if (!container) throw new Error(`Container not found: ${this.containerId}`);
    const chart = echarts.init(container, 'dark');
    window.addEventListener('resize', () => chart.resize());
    return chart;
  }

  public render(data: GraphData): void {
    this.currentData = data;
    const large = isLargeDataset(data.nodes);
    Logger.time('renderGraph');

    // 1. Filter using domain processor
    const filteredData = GraphProcessor.filterByCategory(data, this.activeFilters);

    // 2. Calculate Degree using domain processor
    const degreeMap = GraphProcessor.calculateDegree(filteredData.nodes, filteredData.links);

    // 3. Build series using domain processor
    const nodeSeries = GraphProcessor.buildNodeSeries(filteredData.nodes, degreeMap, { showLabels: false });
    const linkSeries = this.buildLinkSeriesWithQuality(filteredData.links);

    // 4. Render
    const option = this.buildChartOption({ ...filteredData, nodes: nodeSeries, links: linkSeries }, degreeMap, large);
    this.chart?.setOption(option, true);

    // Force resize to ensure chart fits container
    setTimeout(() => this.chart?.resize(), 50);

    Logger.timeEnd('renderGraph');
    Logger.success(`Graph rendered: ${filteredData.nodes.length} nodes, ${filteredData.links.length} links`);
  }

  /**
   * Update filter state (e.g., hide Interfaces)
   */
  public setCategoryFilter(category: string, enabled: boolean): void {
    if (enabled) this.activeFilters.add(category);
    else this.activeFilters.delete(category);
    if (this.currentData) this.render(this.currentData);
  }

  private buildChartOption(data: GraphData, degreeMap: Record<string, number>, large: boolean): echarts.EChartsOption {
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        enterable: true,
        triggerOn: 'mousemove|click',
        backgroundColor: COLORS.CHART_TOOLTIP_BG,
        borderColor: Style.slate[700],
        borderWidth: 1,
        textStyle: { color: Style.slate[400] },
        formatter: (p: any) => this.tooltipFormatter(p),
        extraCssText: `box-shadow: ${GRAPH_CONFIG.TOOLTIP.BOX_SHADOW}; border-radius: ${GRAPH_CONFIG.TOOLTIP.BORDER_RADIUS}px; padding: ${GRAPH_CONFIG.TOOLTIP.PADDING}px; max-width: ${GRAPH_CONFIG.TOOLTIP.MAX_WIDTH}px;`,
      },
      series: [{
        type: 'graph',
        layout: 'force',
        roam: true,
        draggable: true,
        data: data.nodes as any[],
        links: data.links as any[],
        force: {
          repulsion: GRAPH_CONFIG.FORCE.REPULSION,
          edgeLength: GRAPH_CONFIG.FORCE.EDGE_LENGTH,
          gravity: GRAPH_CONFIG.FORCE.GRAVITY,
          friction: GRAPH_CONFIG.FORCE.FRICTION
        },
        emphasis: { focus: 'adjacency', lineStyle: { width: GRAPH_CONFIG.EMPHASIS.LINE_WIDTH, shadowBlur: GRAPH_CONFIG.EMPHASIS.SHADOW_BLUR } },
        animation: !large
      }]
    };
  }

  /**
   * Build link series with quality-aware styling
   */
  private buildLinkSeriesWithQuality(links: GraphLink[]): any[] {
    return links.map(link => ({
      ...link,
      lineStyle: {
        color: this.getEdgeColor(link),
        opacity: this.getEdgeOpacity(link),
        width: Math.min(1 + (link.value || 0) * GRAPH_CONFIG.LINK.WIDTH_MULTIPLIER, GRAPH_CONFIG.LINK.MAX_WIDTH),
        curveness: GRAPH_CONFIG.LINK.CURVENESS,
      },
      symbol: ['none', 'arrow'],
      symbolSize: GRAPH_CONFIG.LINK.SYMBOL_SIZE,
    }));
  }

  /**
   * Check if a link represents an architecture violation
   * Architecture violations occur when:
   * - Controller directly calls Repository (skipping Service layer)
   * - Repository calls Controller (reverse dependency)
   * These violate the standard layered architecture pattern.
   */
  private isArchitectureViolation(link: GraphLink): boolean {
    const analysisData = appState.fullAnalysisData;
    if (!analysisData) return false;

    const layerData = (analysisData as any).architecture_layers;
    if (!layerData || !layerData.violations) return false;

    // Check if this link matches any violation
    return layerData.violations.some((v: any) =>
      v.sourceClass === link.source && v.targetClass === link.target
    );
  }

  /**
   * Get edge color based on quality and architecture violations
   */
  private getEdgeColor(link: GraphLink): string {
    if (this.isArchitectureViolation(link)) {
      return Style.red;
    }
    return Style.slate[400];
  }

  /**
   * Get edge opacity based on quality
   */
  private getEdgeOpacity(link: GraphLink): number {
    if (this.isArchitectureViolation(link)) {
      return GRAPH_CONFIG.OPACITY.VIOLATION;
    }
    return GRAPH_CONFIG.OPACITY.NORMAL;
  }

  private tooltipFormatter(params: any): string {
    if (params.dataType === 'node') {
      const d = params.data as GraphNode;

      return HTML.DIV(
        HTML.FLEX_CONTAINER(
          HTML.DIV('', `width:${GRAPH_CONFIG.TOOLTIP.NODE_ICON_SIZE}px; height:${GRAPH_CONFIG.TOOLTIP.NODE_ICON_SIZE}px; border-radius:50%; background:${d.color}`) +
          HTML.STRONG(d.name, `font-size:${GRAPH_CONFIG.TOOLTIP.NODE_NAME_SIZE}px; color:${Style.slate[50]}`),
          `align-items:center; gap:${GRAPH_CONFIG.TOOLTIP.META_GAP_WIDE}px; margin-bottom:${GRAPH_CONFIG.TOOLTIP.PADDING / 2}px`
        ) +
        (d.fullName ? HTML.DIV(
          d.fullName,
          `font-size:${GRAPH_CONFIG.TOOLTIP.FULLNAME_SIZE}px; color:${Style.slate[600]}; font-family:monospace; margin-bottom:${GRAPH_CONFIG.TOOLTIP.PADDING / 2}px; word-break:break-all`
        ) : '') +
        HTML.DIV(
          cleanDescription(d.description, GRAPH_CONFIG.TOOLTIP.DESC_MAX_LENGTH) ||
          HTML.SPAN(LABEL.GRAPH.NO_DESC, `color:${Style.slate[600]}`),
          `font-size:${GRAPH_CONFIG.TOOLTIP.DESC_SIZE}px; color:${Style.slate[300]}; line-height:${GRAPH_CONFIG.TOOLTIP.LINE_HEIGHT}; margin-bottom:${GRAPH_CONFIG.TOOLTIP.PADDING * 2 / 3}px; border-left:${GRAPH_CONFIG.TOOLTIP.BORDER_WIDTH}px solid ${Style.slate[700]}; padding-left:${GRAPH_CONFIG.TOOLTIP.PADDING * 2 / 3}px`
        ) +
        HTML.FLEX_CONTAINER(
          HTML.SPAN(`${ICON_C.UI.INFO} ${LABEL.GRAPH.STAT_METHODS(d.methodCount || 0)}`) +
          HTML.SPAN(`${ICON_C.SECTION.ASSETS} ${LABEL.GRAPH.STAT_FIELDS(d.fieldCount || 0)}`) +
          HTML.SPAN(`${ICON_C.SECTION.RELATIONS} ${LABEL.GRAPH.STAT_DEPS(params.data.degree || 0)}`),
          `gap:${GRAPH_CONFIG.TOOLTIP.META_GAP}px; font-size:${GRAPH_CONFIG.TOOLTIP.META_SIZE}px; border-top:${GRAPH_CONFIG.TOOLTIP.BORDER_TOP_WIDTH}px solid ${Style.slate[700]}; padding-top:${GRAPH_CONFIG.TOOLTIP.PADDING / 2}px; color:${Style.slate[400]}`
        ) +
        (d.qualityIssues ? HTML.FLEX_CONTAINER(
          (d.qualityIssues.critical > 0 ? HTML.SPAN(`${ICON_C.SEVERITY.CRITICAL} ${LABEL.GRAPH.SEV_CRITICAL(d.qualityIssues.critical)}`, `color:${Style.redLt}`) : '') +
          (d.qualityIssues.major > 0 ? HTML.SPAN(`${ICON_C.SEVERITY.MAJOR} ${LABEL.GRAPH.SEV_MAJOR(d.qualityIssues.major)}`, `color:${Style.amber}`) : '') +
          (d.qualityIssues.minor > 0 ? HTML.SPAN(`${ICON_C.SEVERITY.MINOR} ${LABEL.GRAPH.SEV_MINOR(d.qualityIssues.minor)}`, `color:${Style.blueLt}`) : '') +
          (!d.qualityIssues.critical && !d.qualityIssues.major && !d.qualityIssues.minor ? HTML.SPAN(`${ICON_C.UI.CHECK} ${LABEL.GRAPH.SEV_OK}`, `color:${Style.teal}`) : ''),
          `gap:${GRAPH_CONFIG.TOOLTIP.META_GAP_COMPACT}px; margin-top:${GRAPH_CONFIG.TOOLTIP.PADDING / 2}px; padding-top:${GRAPH_CONFIG.TOOLTIP.PADDING / 2}px; border-top:${GRAPH_CONFIG.TOOLTIP.BORDER_TOP_WIDTH}px solid ${Style.slate[700]}; font-size:${GRAPH_CONFIG.TOOLTIP.QUALITY_SIZE}px`
        ) : ''),
        `padding: ${GRAPH_CONFIG.TOOLTIP.PADDING / 3}px`
      );
    }

    if (params.dataType === 'edge' || params.dataType === 'link') {
      const d = params.data as any;
      const src = d.source ? d.source.split('.').pop() : '';
      const tgt = d.target ? d.target.split('.').pop() : '';
      const strength = d.value || 0;
      const isViolation = this.isArchitectureViolation(d);

      return HTML.DIV(
        HTML.DIV(
          `${src} ${HTML.SPAN('\u2794', `color:${Style.slate[600]}`)} ${tgt}`,
          `font-size:${GRAPH_CONFIG.TOOLTIP.EDGE_NAME_SIZE}px; color:${Style.slate[50]}; font-family:monospace; margin-bottom:${GRAPH_CONFIG.TOOLTIP.PADDING / 3}px`
        ) +
        (isViolation ?
          HTML.DIV(
            `${ICON_C.UI.WARNING} ${LABEL.GRAPH.EDGE_VIOLATION}`,
            `font-size:${GRAPH_CONFIG.TOOLTIP.EDGE_META_SIZE}px; color:${Style.redLt}; margin-bottom:${GRAPH_CONFIG.TOOLTIP.PADDING / 3}px`
          ) :
          HTML.DIV(
            `${ICON_C.SECTION.ASSETS} ${LABEL.GRAPH.EDGE_IMPORT}`,
            `font-size:${GRAPH_CONFIG.TOOLTIP.EDGE_META_SIZE}px; color:${Style.slate[400]}; margin-bottom:${GRAPH_CONFIG.TOOLTIP.PADDING / 3}px`
          )
        ) +
        HTML.DIV(
          `${LABEL.GRAPH.EDGE_REF}: ${HTML.STRONG(String(strength), `color:${Style.blueLt}`)}`,
          `font-size:${GRAPH_CONFIG.TOOLTIP.EDGE_DESC_SIZE}px; color:${Style.slate[300]}`
        ),
        `padding: ${GRAPH_CONFIG.TOOLTIP.PADDING / 2}px`
      );
    }
    return '';
  }

  // Public APIs for UI controls
  public showLabels(show: boolean) { this.labelsVisible = show; this.chart?.setOption({ series: [{ label: { show } }] }); }
  public zoomIn() { this.chart?.dispatchAction({ type: 'geoRoam', animation: { duration: GRAPH_CONFIG.ANIMATION.ZOOM_DURATION }, zoom: GRAPH_CONFIG.ANIMATION.ZOOM_IN_FACTOR }); }
  public zoomOut() { this.chart?.dispatchAction({ type: 'geoRoam', animation: { duration: GRAPH_CONFIG.ANIMATION.ZOOM_DURATION }, zoom: GRAPH_CONFIG.ANIMATION.ZOOM_OUT_FACTOR }); }
  public resetView() { this.chart?.dispatchAction({ type: 'restore' }); }
  public centerView() { this.chart?.dispatchAction({ type: 'geoRoam', animation: { duration: GRAPH_CONFIG.ANIMATION.ZOOM_DURATION }, position: { x: 0, y: 0 } }); }
  public downloadImage() {
    if (!this.chart) return;
    const url = this.chart.getDataURL({ type: 'png', pixelRatio: C.PIXEL_RATIO, backgroundColor: C.CHART_EXPORT_BG });
    const link = document.createElement('a');
    link.download = `graph-${Date.now()}.png`;
    link.href = url;
    link.click();
  }
  public searchNodes(keyword: string) {
    if (!this.chart || !this.currentData) return;
    const chart = this.chart;
    chart.dispatchAction({ type: 'downplay', seriesIndex: 0 });
    if (!keyword) return;
    const term = keyword.toLowerCase();
    this.currentData.nodes.forEach((n, i) => {
      if (n.name.toLowerCase().includes(term) || n.id.toLowerCase().includes(term)) {
        chart.dispatchAction({ type: 'highlight', seriesIndex: 0, dataIndex: i });
      }
    });
  }
  public getChart() { return this.chart; }

  /**
   * Cleanup resources
   */
  public cleanup(): void {
    if (this.chart) {
      this.chart.dispose();
      this.chart = null;
    }
  }
}
