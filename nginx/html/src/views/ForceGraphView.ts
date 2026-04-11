/**
 * Java Source Analyzer - Force Graph View
 * Clean, stable visualization with correct colors and accessibility.
 */

import * as echarts from 'echarts';
import type { GraphData, GraphNode, GraphLink } from '../types';
import { Logger } from '../utils/logger';
import { cleanDescription, isLargeDataset } from '../utils/dom-helpers';

export class ForceGraphView {
  public chart: echarts.ECharts | null = null;
  public currentData: GraphData | null = null;
  private containerId: string;
  private labelsVisible: boolean = false;
  
  // Filter State
  private activeFilters: Set<string> = new Set(['INTERFACE', 'ABSTRACT_CLASS', 'CLASS', 'ENUM', 'UTILITY', 'EXTERNAL']);

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

    // 1. Filter Nodes
    const filteredNodes = data.nodes.filter(n => this.activeFilters.has(n.category));
    const validIds = new Set(filteredNodes.map(n => n.id));

    // 2. Filter Links
    const filteredLinks = data.links.filter(l => validIds.has(l.source) && validIds.has(l.target));

    // 3. Calculate Degree
    const degreeMap = this.calculateDegree({ nodes: filteredNodes, links: filteredLinks });

    // 4. Render
    const option = this.buildChartOption({ ...data, nodes: filteredNodes, links: filteredLinks }, degreeMap, large);
    this.chart?.setOption(option, true);
    
    Logger.timeEnd('renderGraph');
    Logger.success(`Graph rendered: ${filteredNodes.length} nodes, ${filteredLinks.length} links`);
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
        backgroundColor: 'rgba(15, 23, 42, 0.95)',
        borderColor: '#334155',
        borderWidth: 1,
        textStyle: { color: '#cbd5e1' },
        formatter: (p: any) => this.tooltipFormatter(p),
        extraCssText: 'box-shadow: 0 8px 24px rgba(0,0,0,0.5); border-radius: 8px; padding: 12px; max-width: 350px;'
      },
      series: [{
        type: 'graph',
        layout: 'force',
        roam: true,
        draggable: true,
        data: this.buildNodeSeries(data.nodes, degreeMap),
        links: this.buildLinkSeries(data.links),
        force: { repulsion: 300, edgeLength: 90, gravity: 0.08, friction: 0.6 },
        emphasis: { focus: 'adjacency', lineStyle: { width: 4, shadowBlur: 15 } },
        animation: !large
      }]
    };
  }

  private buildNodeSeries(nodes: GraphNode[], degreeMap: Record<string, number>): any[] {
    return nodes.map(node => {
      const degree = degreeMap[node.id] || 0;
      const isExternal = node.category === 'EXTERNAL';
      const symbolSize = this.calculateNodeSize(node, degree);

      return {
        ...node,
        symbolSize,
        itemStyle: {
          color: node.color, // Uses color from data loader (consistent with CSS variables or hex)
          shadowBlur: degree > 10 ? 20 : 10,
          shadowColor: node.color,
          borderColor: isExternal ? 'rgba(255,255,255,0.4)' : 'rgba(255,255,255,0.2)',
          borderWidth: isExternal ? 1 : 1.5
        },
        label: {
          show: false, // Hidden by default
          position: 'bottom',
          formatter: '{b}',
          color: '#e2e8f0',
          fontSize: 11
        }
      };
    });
  }

  private buildLinkSeries(links: GraphLink[]): any[] {
    return links.map(link => ({
      ...link,
      lineStyle: {
        color: '#94a3b8', // Slate-400: Accessible contrast on dark bg, matches --text-muted
        opacity: 0.3,
        width: Math.min(1 + (link.value || 0) * 0.1, 4),
        curveness: 0.2
      },
      symbol: ['none', 'arrow'],
      symbolSize: [0, 6]
    }));
  }

  private calculateDegree(data: GraphData): Record<string, number> {
    const map: Record<string, number> = {};
    data.links.forEach(l => {
      map[l.source] = (map[l.source] || 0) + 1;
      map[l.target] = (map[l.target] || 0) + 1;
    });
    return map;
  }

  private calculateNodeSize(node: GraphNode, degree: number): number {
    const isExt = node.category === 'EXTERNAL';
    const base = isExt ? 15 : 20;
    return base + Math.min(degree, 50) / 50 * (isExt ? 10 : 20);
  }

  /**
   * Semantic Tooltip Formatter
   */
  private tooltipFormatter(params: any): string {
    if (params.dataType === 'node') {
      const d = params.data as GraphNode;
      const isExt = d.category === 'EXTERNAL';
      const kindName = isExt ? (d.dependencyType === 'JDK' ? 'JDK 库' : '第三方库') :
                       d.category === 'INTERFACE' ? '接口' :
                       d.category === 'ABSTRACT_CLASS' ? '抽象类' :
                       d.category === 'ENUM' ? '枚举' : '类';
      
      return `
        <div style="padding: 4px;">
          <div style="display:flex; align-items:center; gap:8px; margin-bottom:6px;">
            <div style="width:10px; height:10px; border-radius:50%; background:${d.color};"></div>
            <strong style="font-size:14px; color:#f8fafc;">${d.name}</strong>
          </div>
          ${d.fullName ? `<div style="font-size:10px; color:#64748b; font-family:monospace; margin-bottom:6px; word-break:break-all;">${d.fullName}</div>` : ''}
          <div style="font-size:11px; color:#cbd5e1; line-height:1.4; margin-bottom:8px; border-left:2px solid #334155; padding-left:8px;">
            ${cleanDescription(d.description, 40) || '<span style="color:#64748b;">暂无描述</span>'}
          </div>
          <div style="display:flex; gap:10px; font-size:10px; border-top:1px solid #334155; padding-top:6px; color:#94a3b8;">
            <span>📊 ${d.methodCount || 0} 方法</span>
            <span>📝 ${d.fieldCount || 0} 字段</span>
            <span>🔗 ${params.data.degree || 0} 依赖</span>
          </div>
        </div>
      `;
    }

    if (params.dataType === 'edge' || params.dataType === 'link') {
      const d = params.data as any;
      const src = d.source ? d.source.split('.').pop() : '';
      const tgt = d.target ? d.target.split('.').pop() : '';
      const strength = d.value || 0;

      return `
        <div style="padding: 6px;">
          <div style="font-size:12px; color:#f8fafc; font-family:monospace; margin-bottom:4px;">
            ${src} <span style="color:#64748b">➔</span> ${tgt}
          </div>
          <div style="font-size:10px; color:#94a3b8; margin-bottom:4px;">📦 Import 依赖关系</div>
          <div style="font-size:11px; color:#cbd5e1;">
            引用该类的数量: <strong style="color:#38bdf8;">${strength}</strong>
          </div>
        </div>
      `;
    }
    return '';
  }

  // Public APIs for UI controls
  public showLabels(show: boolean) { this.labelsVisible = show; this.chart?.setOption({ series: [{ label: { show } }] }); }
  public zoomIn() { this.chart?.dispatchAction({ type: 'geoRoam', animation: true, zoom: 1.2 }); }
  public zoomOut() { this.chart?.dispatchAction({ type: 'geoRoam', animation: true, zoom: 0.8 }); }
  public resetView() { this.chart?.dispatchAction({ type: 'restore' }); }
  public centerView() { this.chart?.dispatchAction({ type: 'geoRoam', animation: true, position: { x: 0, y: 0 } }); }
  public downloadImage() {
    if (!this.chart) return;
    const url = this.chart.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#0b0f19' });
    const link = document.createElement('a');
    link.download = `graph-${Date.now()}.png`;
    link.href = url;
    link.click();
  }
  public searchNodes(keyword: string) {
    if (!this.chart || !this.currentData) return;
    this.chart.dispatchAction({ type: 'downplay', seriesIndex: 0 });
    if (!keyword) return;
    const term = keyword.toLowerCase();
    this.currentData.nodes.forEach((n, i) => {
      if (n.name.toLowerCase().includes(term) || n.id.toLowerCase().includes(term)) {
        this.chart.dispatchAction({ type: 'highlight', seriesIndex: 0, dataIndex: i });
      }
    });
  }
  public getChart() { return this.chart; }
}
