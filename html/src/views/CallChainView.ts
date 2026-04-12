/**
 * Java Source Analyzer - Call Chain View (Refactored)
 *
 * Visualizes method call chains using layered swimlane diagram.
 * Uses Component base class for selector UI.
 * Uses event delegation instead of inline handlers.
 */

import * as echarts from 'echarts';
import type { AnalysisResult, Asset, MethodAsset } from '../types';
import { Component, type Child } from '../framework/component';
import { EventDelegator } from '../framework/events';
import { Logger } from '../utils/logger';

interface CallChainNode {
  id: string;
  name: string;
  layer: 'controller' | 'service' | 'repository' | 'external';
  className: string;
  methodName: string;
  qualityIssues?: { critical: number; major: number; minor: number };
}

interface CallChainLink {
  source: string;
  target: string;
  type: 'normal' | 'violation' | 'risk';
  label?: string;
}

export class CallChainView extends Component {
  private containerId: string;
  private chart: echarts.ECharts | null = null;
  private currentData: AnalysisResult | null = null;
  private selectedEntryMethod: string | null = null;
  private selectedClassForMethod: string | null = null;
  private delegator: EventDelegator | null = null;

  constructor(containerId: string = 'call-chain-content') {
    super();
    this.containerId = containerId;
  }

  public loadData(data: AnalysisResult): void {
    this.currentData = data;
    const container = document.getElementById(this.containerId);
    if (!container) {
      Logger.error(`Container not found: ${this.containerId}`);
      return;
    }

    if (!this.chart) {
      this.chart = echarts.init(container, 'dark');
      window.addEventListener('resize', () => this.chart?.resize());
    }

    if (!this.selectedEntryMethod) {
      this.mount(container);
      this.setupEntryPointListeners();
    } else if (this.selectedClassForMethod) {
      this.mount(container);
      this.setupMethodSelectorListeners();
    } else {
      this.renderCallChain(data, this.selectedEntryMethod);
    }
  }

  public buildRoot(): HTMLElement {
    if (!this.currentData) return this.el('div', null, []);

    if (this.selectedClassForMethod) {
      return this.renderMethodSelector();
    }

    return this.renderEntryPointSelector();
  }

  private renderEntryPointSelector(): HTMLElement {
    const controllers = this.currentData!.assets?.filter(asset =>
      asset.kind === 'CLASS' &&
      (asset.address.includes('.controller.') ||
       asset.address.includes('.api.') ||
       (asset as any).annotation_params?.some((a: any) => a.name?.includes('Controller')))
    ) || [];

    if (controllers.length === 0) {
      return this.el('div', { style: { padding: '40px' } as Partial<CSSStyleDeclaration> }, [
        this.el('div', { className: 'section-header' }, [
          this.el('span', null, [this.text('🔍')]),
          this.el('span', null, [this.text('选择入口方法以追踪调用链路')]),
        ]),
        this.el('div', { style: { textAlign: 'center', padding: '60px', color: 'var(--text-muted)' } as Partial<CSSStyleDeclaration> }, [
          this.el('div', { style: { fontSize: '48px', marginBottom: '16px' } as Partial<CSSStyleDeclaration> }, [this.text('🔍')]),
          this.el('div', null, [this.text('未检测到 Controller 类')]),
          this.el('div', { style: { marginTop: '8px', fontSize: '12px' } as Partial<CSSStyleDeclaration> },
            [this.text('提示: 确保后端分析包含了 Spring MVC 或 REST API 控制器')]),
        ]),
      ]);
    }

    const cards = controllers.slice(0, 20).map(controller =>
      this.el('div', {
        className: 'entry-point-card',
        'data-class': controller.address,
        style: {
          padding: '16px',
          background: 'var(--bg-secondary)',
          border: '1px solid var(--border)',
          borderRadius: '8px',
          cursor: 'pointer',
          transition: 'all 0.2s',
        } as Partial<CSSStyleDeclaration>,
      }, [
        this.el('div', { style: { fontSize: '13px', fontWeight: '600', color: 'var(--text-primary)', marginBottom: '8px' } as Partial<CSSStyleDeclaration> },
          [this.text(`🌐 ${controller.address.split('.').pop()}`)]),
        this.el('div', { style: { fontSize: '10px', color: 'var(--text-muted)', fontFamily: 'monospace', marginBottom: '8px' } as Partial<CSSStyleDeclaration> },
          [this.text(controller.address)]),
        this.el('div', { style: { fontSize: '11px', color: 'var(--blue-primary)' } as Partial<CSSStyleDeclaration> },
          [this.text(`📝 ${(controller.methods_full || controller.methods || []).length} 个方法`)]),
      ])
    );

    const children: Child[] = [
      this.el('div', { className: 'section-header' }, [
        this.el('span', null, [this.text('🔍')]),
        this.el('span', null, [this.text('选择入口方法以追踪调用链路')]),
      ]),
      this.el('div', { style: { marginBottom: '16px', fontSize: '12px', color: 'var(--text-muted)' } as Partial<CSSStyleDeclaration> },
        [this.text(`检测到 ${controllers.length} 个 Controller 类`)]),
      this.el('div', {
        style: { marginTop: '20px', display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))', gap: '16px' } as Partial<CSSStyleDeclaration>,
      }, cards),
    ];

    if (controllers.length > 20) {
      children.push(
        this.el('div', { style: { textAlign: 'center', marginTop: '20px', color: 'var(--text-muted)', fontSize: '12px' } as Partial<CSSStyleDeclaration> },
          [this.text('显示前 20 个 Controller，使用搜索框查找特定类')])
      );
    }

    return this.el('div', { style: { padding: '40px' } as Partial<CSSStyleDeclaration> }, children);
  }

  private renderMethodSelector(): HTMLElement {
    if (!this.currentData || !this.selectedClassForMethod) return this.el('div', null, []);

    const asset = this.currentData.assets?.find(a => a.address === this.selectedClassForMethod);
    if (!asset) return this.el('div', null, []);

    const methods = asset.methods_full || asset.methods || [];
    const publicMethods = methods.filter((m: MethodAsset) => m.modifiers?.includes('public')).slice(0, 30);

    const entries = publicMethods.map((method: MethodAsset) =>
      this.el('div', {
        className: 'method-entry',
        'data-method': method.address,
        style: {
          padding: '14px',
          background: 'var(--bg-secondary)',
          border: '1px solid var(--border)',
          borderRadius: '6px',
          cursor: 'pointer',
          transition: 'all 0.2s',
        } as Partial<CSSStyleDeclaration>,
      }, [
        this.el('div', { style: { fontFamily: 'monospace', fontSize: '12px', color: 'var(--accent)' } as Partial<CSSStyleDeclaration> },
          [this.text(`${method.name}()`)]),
        method.description ? this.el('div', { style: { fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px' } as Partial<CSSStyleDeclaration> },
          [this.text(method.description.substring(0, 80) + (method.description.length > 80 ? '...' : ''))]) : this.el('div', null, []),
      ])
    );

    return this.el('div', { style: { padding: '40px' } as Partial<CSSStyleDeclaration> }, [
      this.el('div', { className: 'section-header' }, [
        this.el('span', { id: 'call-chain-back', style: { cursor: 'pointer' } as Partial<CSSStyleDeclaration> }, [this.text('←')]),
        this.el('span', null, [this.text('返回')]),
        this.el('span', { style: { marginLeft: 'auto' } as Partial<CSSStyleDeclaration> }, [this.text(`${asset.address.split('.').pop()} - 选择入口方法`)]),
      ]),
      this.el('div', {
        style: { marginTop: '20px', display: 'grid', gap: '12px' } as Partial<CSSStyleDeclaration>,
      }, entries),
    ]);
  }

  private setupEntryPointListeners(): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = new EventDelegator(this.root);

    this.delegator.on('click', '.entry-point-card', (_e, el) => {
      const classAddr = el.getAttribute('data-class');
      if (classAddr) this.showMethodSelector(classAddr);
    });

    this.delegator.on('mouseenter', '.entry-point-card', (_e, el) => {
      el.style.borderColor = 'var(--accent)';
    });
    this.delegator.on('mouseleave', '.entry-point-card', (_e, el) => {
      el.style.borderColor = 'var(--border)';
    });
  }

  private setupMethodSelectorListeners(): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = new EventDelegator(this.root);

    this.delegator.on('click', '.method-entry', (_e, el) => {
      const methodAddr = el.getAttribute('data-method');
      if (methodAddr) this.setSelectedEntryPoint(methodAddr);
    });

    this.delegator.on('mouseenter', '.method-entry', (_e, el) => {
      el.style.borderColor = 'var(--accent)';
    });
    this.delegator.on('mouseleave', '.method-entry', (_e, el) => {
      el.style.borderColor = 'var(--border)';
    });

    this.delegator.on('click', '#call-chain-back', () => {
      this.selectedClassForMethod = null;
      this.update();
      this.setupEntryPointListeners();
    });
  }

  public showMethodSelector(classAddress: string): void {
    this.selectedClassForMethod = classAddress;
    if (this.currentData) {
      this.update();
      this.setupMethodSelectorListeners();
    }
  }

  public setSelectedEntryPoint(methodAddress: string): void {
    this.selectedEntryMethod = methodAddress;
    this.selectedClassForMethod = null;
    if (this.currentData) {
      this.renderCallChain(this.currentData, methodAddress);
    }
  }

  public reset(): void {
    this.selectedEntryMethod = null;
    this.selectedClassForMethod = null;
    if (this.chart) {
      this.chart.dispose();
      this.chart = null;
    }
  }

  private renderCallChain(data: AnalysisResult, entryMethodAddress: string): void {
    const chain = this.buildCallChain(data, entryMethodAddress);

    if (chain.nodes.length === 0) {
      const container = document.getElementById(this.containerId);
      if (container) {
        container.innerHTML = `
          <div style="padding: 40px; text-align: center; color: var(--text-muted);">
            <div style="font-size: 48px; margin-bottom: 16px;">🔗</div>
            <div>未检测到调用链路</div>
            <div style="margin-top: 8px; font-size: 12px;">该方法可能没有调用其他内部方法</div>
          </div>
        ` as unknown as string;
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
            const node = params.data as CallChainNode;
            return `
              <div style="padding: 6px;">
                <div style="font-size: 12px; font-weight: 600; margin-bottom: 4px;">${node.methodName}</div>
                <div style="font-size: 10px; color: #94a3b8; font-family: monospace; margin-bottom: 6px;">${node.className}</div>
                <div style="font-size: 10px; color: #64748b; margin-bottom: 4px;">层级: ${this.getLayerLabel(node.layer)}</div>
                ${node.qualityIssues ? `
                  <div style="display: flex; gap: 8px; font-size: 10px; margin-top: 6px; padding-top: 6px; border-top: 1px solid #334155;">
                    ${node.qualityIssues.critical > 0 ? `<span style="color: #fb7185;">🔴 ${node.qualityIssues.critical} 严重</span>` : ''}
                    ${node.qualityIssues.major > 0 ? `<span style="color: #fbbf24;">🟠 ${node.qualityIssues.major} 重要</span>` : ''}
                    ${node.qualityIssues.minor > 0 ? `<span style="color: #38bdf8;">🔵 ${node.qualityIssues.minor} 提示</span>` : ''}
                    ${!node.qualityIssues.critical && !node.qualityIssues.major && !node.qualityIssues.minor ? '<span style="color: #22c55e;">✅ 无问题</span>' : ''}
                  </div>
                ` : ''}
              </div>
            `;
          }
          if (params.dataType === 'edge') {
            const link = params.data as CallChainLink;
            return `
              <div style="padding: 4px;">
                <div style="font-size: 11px; margin-bottom: 4px;">
                  ${link.type === 'violation' ? '⚠️ 架构违规' : link.type === 'risk' ? '🔥 高风险调用' : '📞 正常调用'}
                </div>
                ${link.label ? `<div style="font-size: 10px; color: #94a3b8;">${link.label}</div>` : ''}
              </div>
            `;
          }
          return '';
        },
      },
      series: [{
        type: 'sankey',
        emphasis: { focus: 'adjacency' },
        data: chain.nodes.map(node => ({
          id: node.id,
          name: node.methodName,
          value: 1,
          layer: node.layer,
          itemStyle: {
            color: this.getLayerColor(node.layer),
            borderWidth: node.qualityIssues?.critical ? 2 : 1,
            borderColor: node.qualityIssues?.critical ? '#ef4444' : undefined,
          },
          label: {
            color: '#e2e8f0',
            fontSize: 10,
            formatter: (p: any) => p.name.length > 15 ? p.name.substring(0, 15) + '...' : p.name,
          },
        })),
        links: chain.links.map(link => ({
          source: link.source,
          target: link.target,
          lineStyle: {
            color: link.type === 'violation' ? '#ef4444' : link.type === 'risk' ? '#f59e0b' : '#3b82f6',
            curveness: 0.5,
            opacity: link.type === 'violation' ? 0.8 : 0.4,
            width: link.type === 'violation' ? 3 : 1.5,
            type: link.type === 'violation' ? 'dashed' : 'solid',
          },
          label: link.label,
        })),
        lineStyle: { curveness: 0.5 },
        label: { position: 'right', distance: 5 },
        levels: [
          { depth: 0, itemStyle: { borderWidth: 2, borderColor: '#1e40af' } },
          { depth: 1, itemStyle: { borderWidth: 2, borderColor: '#059669' } },
          { depth: 2, itemStyle: { borderWidth: 2, borderColor: '#7c3aed' } },
          { depth: 3, itemStyle: { borderWidth: 2, borderColor: '#6b7280' } },
        ],
      }],
    };

    this.chart?.setOption(option, true);
    setTimeout(() => this.chart?.resize(), 50);
  }

  private buildCallChain(data: AnalysisResult, entryMethodAddress: string): { nodes: CallChainNode[]; links: CallChainLink[] } {
    const nodes: CallChainNode[] = [];
    const links: CallChainLink[] = [];
    const visited = new Set<string>();

    const traverse = (methodAddress: string, depth: number = 0) => {
      if (visited.has(methodAddress) || depth > 5) return;
      visited.add(methodAddress);

      let method: MethodAsset | null = null;
      let parentClass: Asset | null = null;

      for (const asset of data.assets || []) {
        const found = (asset.methods_full || asset.methods || []).find((m: MethodAsset) => m.address === methodAddress);
        if (found) {
          method = found;
          parentClass = asset;
          break;
        }
      }

      if (!method || !parentClass) return;

      const layer = this.detectLayer(parentClass);

      nodes.push({
        id: method.address,
        name: `${parentClass.address.split('.').pop()}.${method.name}`,
        layer,
        className: parentClass.address,
        methodName: method.name,
        qualityIssues: (method as any).qualityIssues,
      });

      const keyStmts = (method as any).key_statements || [];
      const extCalls = keyStmts.filter((s: any) => s.type === 'EXTERNAL_CALL');

      extCalls.forEach((call: any) => {
        const targetMethodAddr = call.target_method || call.target || '';

        let targetClass: Asset | null = null;
        for (const asset of data.assets || []) {
          if ((asset.methods_full || asset.methods || []).some((m: MethodAsset) => m.address === targetMethodAddr)) {
            targetClass = asset;
            break;
          }
        }

        if (targetClass) {
          const targetLayer = this.detectLayer(targetClass);
          const isViolation = this.isArchitectureViolation(layer, targetLayer);

          links.push({
            source: method!.address,
            target: targetMethodAddr,
            type: isViolation ? 'violation' : 'normal',
            label: call.description || `${method!.name}() → ${targetMethodAddr.split('.').pop()}`,
          });

          traverse(targetMethodAddr, depth + 1);
        }
      });
    };

    traverse(entryMethodAddress);
    return { nodes, links };
  }

  private detectLayer(asset: Asset): CallChainNode['layer'] {
    const address = asset.address.toLowerCase();
    if (address.includes('.controller.') || address.includes('.api.') ||
        (asset as any).annotation_params?.some((a: any) => a.name?.includes('Controller'))) {
      return 'controller';
    }
    if (address.includes('.service.') || (asset as any).annotation_params?.some((a: any) => a.name?.includes('Service'))) {
      return 'service';
    }
    if (address.includes('.repository.') || address.includes('.dao.') ||
        (asset as any).annotation_params?.some((a: any) => a.name?.includes('Repository'))) {
      return 'repository';
    }
    return 'external';
  }

  private isArchitectureViolation(fromLayer: string, toLayer: string): boolean {
    if (fromLayer === 'controller' && toLayer === 'repository') return true;
    if (fromLayer === 'repository' && toLayer === 'controller') return true;
    return false;
  }

  private getLayerColor(layer: string): string {
    switch (layer) {
      case 'controller': return '#3b82f6';
      case 'service': return '#10b981';
      case 'repository': return '#8b5cf6';
      case 'external': return '#6b7280';
      default: return '#9ca3af';
    }
  }

  private getLayerLabel(layer: string): string {
    switch (layer) {
      case 'controller': return '🌐 Controller 层';
      case 'service': return '⚙️ Service 层';
      case 'repository': return '💾 Repository 层';
      case 'external': return '🔌 外部调用';
      default: return layer;
    }
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
