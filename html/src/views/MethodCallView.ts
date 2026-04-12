/**
 * Java Source Analyzer - Method Call View (Refactored)
 *
 * Visualizes method-to-method call relationships using Sankey diagram.
 * Uses Component base class for selector UI.
 * Uses event delegation instead of inline onmouseover/onmouseout.
 */

import * as echarts from 'echarts';
import type { AnalysisResult, Asset, MethodAsset } from '../types';
import { Component, type Child } from '../framework/component';
import { EventDelegator } from '../framework/events';
import { Logger } from '../utils/logger';

interface MethodCallNode {
  id: string;
  name: string;
  className: string;
  category: 'internal' | 'external';
  value?: number;
}

interface MethodCallLink {
  source: string;
  target: string;
  value: number;
  calls?: string[];
}

export class MethodCallView extends Component {
  private containerId: string;
  private chart: echarts.ECharts | null = null;
  private currentData: AnalysisResult | null = null;
  private selectedClass: string | null = null;
  private delegator: EventDelegator | null = null;

  constructor(containerId: string = 'method-call-content') {
    super();
    this.containerId = containerId;
  }

  public render(data: AnalysisResult): void {
    this.currentData = data;
    const container = document.getElementById(this.containerId);
    if (!container) {
      Logger.error(`Container not found: ${this.containerId}`);
      return;
    }

    // Initialize chart if not exists
    if (!this.chart) {
      this.chart = echarts.init(container, 'dark');
      window.addEventListener('resize', () => this.chart?.resize());
    }

    if (!this.selectedClass) {
      this.mount(container);
      this.setupClassCardListeners();
    } else {
      this.renderMethodCallGraph(data, this.selectedClass);
    }
  }

  public buildRoot(): HTMLElement {
    if (!this.currentData) return this.el('div', null, []);

    const classes = this.currentData.assets || [];
    const displayed = classes.slice(0, 50);

    const cards = displayed.map(asset =>
      this.el('div', {
        className: 'class-card',
        'data-class': asset.address,
        style: {
          padding: '16px',
          background: 'var(--bg-secondary)',
          border: '1px solid var(--border)',
          borderRadius: '8px',
          cursor: 'pointer',
          transition: 'all 0.2s',
        } as Partial<CSSStyleDeclaration>,
      }, [
        this.el('div', { style: { fontSize: '14px', fontWeight: '600', color: 'var(--text-primary)', marginBottom: '8px' } as Partial<CSSStyleDeclaration> },
          [this.text(asset.address.split('.').pop() || '')]),
        this.el('div', { style: { fontSize: '11px', color: 'var(--text-muted)', fontFamily: 'monospace' } as Partial<CSSStyleDeclaration> },
          [this.text(asset.address)]),
        this.el('div', { style: { marginTop: '8px', display: 'flex', gap: '8px', fontSize: '10px' } as Partial<CSSStyleDeclaration> }, [
          this.el('span', { style: { color: 'var(--blue-primary)' } as Partial<CSSStyleDeclaration> },
            [this.text(`📝 ${(asset.methods_full || asset.methods || []).length} 方法`)]),
        ]),
      ])
    );

    const children: Child[] = [
      this.el('div', { className: 'section-header' }, [
        this.el('span', null, [this.text('📊')]),
        this.el('span', null, [this.text('选择类以查看方法调用关系')]),
      ]),
      this.el('div', {
        style: { marginTop: '20px', display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '16px' } as Partial<CSSStyleDeclaration>,
      }, cards),
    ];

    if (classes.length > 50) {
      children.push(
        this.el('div', { style: { textAlign: 'center', marginTop: '20px', color: 'var(--text-muted)', fontSize: '12px' } as Partial<CSSStyleDeclaration> },
          [this.text('显示前 50 个类，使用搜索框查找特定类')])
      );
    }

    return this.el('div', { style: { padding: '40px' } as Partial<CSSStyleDeclaration> }, children);
  }

  private setupClassCardListeners(): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = new EventDelegator(this.root);

    this.delegator.on('click', '.class-card', (_e, el) => {
      const classAddr = el.getAttribute('data-class');
      if (classAddr) this.setSelectedClass(classAddr);
    });

    // Hover effects via CSS instead of inline JS
    this.delegator.on('mouseenter', '.class-card', (_e, el) => {
      el.style.borderColor = 'var(--accent)';
    });
    this.delegator.on('mouseleave', '.class-card', (_e, el) => {
      el.style.borderColor = 'var(--border)';
    });
  }

  public setSelectedClass(classAddress: string): void {
    this.selectedClass = classAddress;
    if (this.currentData) {
      const container = document.getElementById(this.containerId);
      if (container) this.renderMethodCallGraph(this.currentData, classAddress);
    }
  }

  public reset(): void {
    this.selectedClass = null;
    if (this.chart) {
      this.chart.dispose();
      this.chart = null;
    }
  }

  private renderMethodCallGraph(data: AnalysisResult, classAddress: string): void {
    const asset = data.assets?.find(a => a.address === classAddress);
    if (!asset) return;

    const methods = asset.methods_full || asset.methods || [];

    if (methods.length === 0) {
      const container = document.getElementById(this.containerId);
      if (container) {
        container.innerHTML = `
          <div style="padding: 40px; text-align: center; color: var(--text-muted);">
            <div style="font-size: 48px; margin-bottom: 16px;">📭</div>
            <div>该类没有方法</div>
          </div>
        `;
      }
      return;
    }

    // Build nodes and links for Sankey
    const nodes: MethodCallNode[] = methods.map(method => ({
      id: method.address,
      name: method.name,
      className: asset.address.split('.').pop() || '',
      category: 'internal',
      value: 0,
    }));

    const links: MethodCallLink[] = [];

    methods.forEach(method => {
      const keyStmts = (method as any).key_statements || [];
      const extCalls = keyStmts.filter((s: any) => s.type === 'EXTERNAL_CALL');

      extCalls.forEach((call: any) => {
        const targetMethod = call.target_method || call.target || '';
        const isInternal = methods.some(m => m.address === targetMethod);

        if (isInternal) {
          const existingLink = links.find(l => l.source === method.address && l.target === targetMethod);
          if (existingLink) {
            existingLink.value += 1;
            if (existingLink.calls) {
              existingLink.calls.push(call.description || `${method.name}() → ${targetMethod.split('.').pop()}`);
            }
          } else {
            links.push({
              source: method.address,
              target: targetMethod,
              value: 1,
              calls: [call.description || `${method.name}() → ${targetMethod.split('.').pop()}`],
            });
          }
        }
      });
    });

    nodes.forEach(node => {
      const incoming = links.filter(l => l.target === node.id).reduce((sum, l) => sum + l.value, 0);
      const outgoing = links.filter(l => l.source === node.id).reduce((sum, l) => sum + l.value, 0);
      node.value = incoming + outgoing;
    });

    const connectedNodes = nodes.filter(n => n.value! > 0);

    if (connectedNodes.length === 0) {
      const container = document.getElementById(this.containerId);
      if (container) {
        container.innerHTML = `
          <div style="padding: 40px; text-align: center; color: var(--text-muted);">
            <div style="font-size: 48px; margin-bottom: 16px;">🔗</div>
            <div>该类方法之间没有内部调用关系</div>
            <div style="margin-top: 8px; font-size: 12px;">提示: 方法可能只调用了外部类的方法</div>
          </div>
        `;
      }
      return;
    }

    const option: echarts.EChartsOption = {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        triggerOn: 'mousemove',
        backgroundColor: 'rgba(15, 23, 42, 0.95)',
        borderColor: '#334155',
        borderWidth: 1,
        textStyle: { color: '#e2e8f0' },
        formatter: (params: any) => {
          if (params.dataType === 'node') {
            return `
              <div style="padding: 4px;">
                <div style="font-size: 13px; font-weight: 600; margin-bottom: 4px;">${params.name}</div>
                <div style="font-size: 11px; color: #94a3b8;">调用次数: ${params.value}</div>
              </div>
            `;
          }
          if (params.dataType === 'edge') {
            const calls = params.data.calls || [];
            return `
              <div style="padding: 4px;">
                <div style="font-size: 12px; margin-bottom: 6px; font-family: monospace;">
                  ${params.data.source.split('.').pop()} → ${params.data.target.split('.').pop()}
                </div>
                <div style="font-size: 11px; color: #94a3b8; margin-bottom: 4px;">调用 ${params.value} 次</div>
                ${calls.length > 0 ? `
                  <div style="font-size: 10px; color: #64748b; max-width: 300px;">
                    ${calls.slice(0, 3).map((c: string) => `<div>• ${c}</div>`).join('')}
                    ${calls.length > 3 ? `<div>... 还有 ${calls.length - 3} 次调用</div>` : ''}
                  </div>
                ` : ''}
              </div>
            `;
          }
          return '';
        },
      },
      series: [{
        type: 'sankey',
        emphasis: { focus: 'adjacency' },
        data: connectedNodes.map(node => ({
          id: node.id,
          name: node.name,
          value: node.value,
          itemStyle: { color: node.category === 'internal' ? '#3b82f6' : '#6b7280' },
          label: { color: '#e2e8f0', fontSize: 11 },
        })),
        links: links.map(link => ({
          source: link.source,
          target: link.target,
          value: link.value,
          calls: link.calls,
          lineStyle: { color: 'gradient', curveness: 0.5, opacity: 0.4 },
        })),
        lineStyle: { color: 'source', curveness: 0.5 },
        label: { position: 'right', distance: 5 },
        levels: [{ depth: 0, itemStyle: { borderWidth: 2, borderColor: '#1e40af' } }],
      }],
    };

    this.chart?.setOption(option, true);
    setTimeout(() => this.chart?.resize(), 50);
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
