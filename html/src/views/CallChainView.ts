/**
 * Java Source Analyzer - Call Chain View (Refactored)
 *
 * Visualizes method call chains using layered swimlane diagram.
 * Uses Component base class for selector UI.
 * Uses event delegation instead of inline handlers.
 * All hardcoded strings and colors replaced with constants.
 */

import * as echarts from 'echarts';
import type { AnalysisResult, Asset, MethodAsset } from '../types';
import { Component, type Child } from '../framework/component';
import { EventDelegator } from '../framework/events';
import { Logger } from '../utils/logger';
import { Style } from '../utils/style-helpers';
import { ICON, LABEL, CLS, C } from '../constants';

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
          this.el('span', null, [this.text(ICON.SECTION.CALL_CHAIN)]),
          this.el('span', null, [this.text(LABEL.CALL_CHAIN.SELECT_ENTRY)]),
        ]),
        this.el('div', { style: { textAlign: 'center', padding: '60px', color: 'var(--text-muted)' } as Partial<CSSStyleDeclaration> }, [
          this.el('div', { style: { fontSize: '48px', marginBottom: '16px' } as Partial<CSSStyleDeclaration> }, [this.text(ICON.UI.SEARCH)]),
          this.el('div', null, [this.text(LABEL.CALL_CHAIN.NO_CONTROLLER)]),
          this.el('div', { style: { marginTop: '8px', fontSize: '12px' } as Partial<CSSStyleDeclaration> },
            [this.text(LABEL.CALL_CHAIN.TIP_CONTROLLER)]),
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
          [this.text(`${ICON.LAYER.CONTROLLER} ${controller.address.split('.').pop()}`)]),
        this.el('div', { style: { fontSize: '10px', color: 'var(--text-muted)', fontFamily: 'monospace', marginBottom: '8px' } as Partial<CSSStyleDeclaration> },
          [this.text(controller.address)]),
        this.el('div', { style: { fontSize: '11px', color: Style.blueCall } as Partial<CSSStyleDeclaration> },
          [this.text(LABEL.COMPONENT.METHOD_COUNT((controller.methods_full || controller.methods || []).length).replace('⚙️ ', `${ICON.CODE.JAVA} `))]),
      ])
    );

    const children: Child[] = [
      this.el('div', { className: 'section-header' }, [
        this.el('span', null, [this.text(ICON.SECTION.CALL_CHAIN)]),
        this.el('span', null, [this.text(LABEL.CALL_CHAIN.SELECT_ENTRY)]),
      ]),
      this.el('div', { style: { marginBottom: '16px', fontSize: '12px', color: 'var(--text-muted)' } as Partial<CSSStyleDeclaration> },
        [this.text(LABEL.CALL_CHAIN.DETECTED_CONTROLLERS(controllers.length))]),
      this.el('div', {
        style: { marginTop: '20px', display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))', gap: '16px' } as Partial<CSSStyleDeclaration>,
      }, cards),
    ];

    if (controllers.length > 20) {
      children.push(
        this.el('div', { style: { textAlign: 'center', marginTop: '20px', color: 'var(--text-muted)', fontSize: '12px' } as Partial<CSSStyleDeclaration> },
          [this.text(LABEL.CALL_CHAIN.SHOWING_FIRST)])
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
        this.el('div', { style: { fontFamily: 'monospace', fontSize: '12px', color: Style.blueCall } as Partial<CSSStyleDeclaration> },
          [this.text(`${method.name}()`)]),
        method.description ? this.el('div', { style: { fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px' } as Partial<CSSStyleDeclaration> },
          [this.text(method.description.substring(0, 80) + (method.description.length > 80 ? '...' : ''))]) : this.el('div', null, []),
      ])
    );

    return this.el('div', { style: { padding: '40px' } as Partial<CSSStyleDeclaration> }, [
      this.el('div', { className: 'section-header' }, [
        this.el('span', { id: 'call-chain-back', style: { cursor: 'pointer' } as Partial<CSSStyleDeclaration> }, [this.text(LABEL.COMPONENT.BACK)]),
        this.el('span', { style: { marginLeft: 'auto' } as Partial<CSSStyleDeclaration> }, [this.text(`${asset.address.split('.').pop()} - ${LABEL.CALL_CHAIN.SELECT_ENTRY}`)]),
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
            <div style="font-size: 48px; margin-bottom: 16px;">${ICON.LINK.CHAIN}</div>
            <div>${LABEL.CALL_CHAIN.NO_CHAIN}</div>
            <div style="margin-top: 8px; font-size: 12px;">${LABEL.CALL_CHAIN.TIP_NO_CHAIN}</div>
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
        borderColor: Style.slate[700],
        borderWidth: 1,
        textStyle: { color: Style.slate[200] },
        formatter: (params: any) => {
          if (params.dataType === 'node') {
            const node = params.data as CallChainNode;
            return `
              <div style="padding: 6px;">
                <div style="font-size: 12px; font-weight: 600; margin-bottom: 4px;">${node.methodName}</div>
                <div style="font-size: 10px; color: ${Style.slate[400]}; font-family: monospace; margin-bottom: 6px;">${node.className}</div>
                <div style="font-size: 10px; color: ${Style.slate[600]}; margin-bottom: 4px;">${LABEL.CALL_CHAIN.LAYER_LABEL} ${this.getLayerLabel(node.layer)}</div>
                ${node.qualityIssues ? `
                  <div style="display: flex; gap: 8px; font-size: 10px; margin-top: 6px; padding-top: 6px; border-top: 1px solid ${Style.slate[700]};">
                    ${node.qualityIssues.critical > 0 ? `<span style="color: ${Style.redLt};">${ICON.SEVERITY.CRITICAL} ${node.qualityIssues.critical} ${LABEL.QUALITY.CRITICAL}</span>` : ''}
                    ${node.qualityIssues.major > 0 ? `<span style="color: ${Style.amber};">${ICON.SEVERITY.MAJOR} ${node.qualityIssues.major} ${LABEL.QUALITY.MAJOR}</span>` : ''}
                    ${node.qualityIssues.minor > 0 ? `<span style="color: ${Style.blueLt};">${ICON.SEVERITY.MINOR} ${node.qualityIssues.minor} ${LABEL.QUALITY.MINOR}</span>` : ''}
                    ${!node.qualityIssues.critical && !node.qualityIssues.major && !node.qualityIssues.minor ? `<span style="color: ${Style.teal};">${LABEL.GRAPH.SEV_OK}</span>` : ''}
                  </div>
                ` : ''}
              </div>
            `;
          }
          if (params.dataType === 'edge') {
            const link = params.data as CallChainLink;
            const linkIcon = link.type === 'violation' ? ICON.CALL_LINK.VIOLATION
              : link.type === 'risk' ? ICON.CALL_LINK.RISK
              : ICON.CALL_LINK.NORMAL;
            return `
              <div style="padding: 4px;">
                <div style="font-size: 11px; margin-bottom: 4px;">
                  ${linkIcon}
                </div>
                ${link.label ? `<div style="font-size: 10px; color: ${Style.slate[400]};">${link.label}</div>` : ''}
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
            borderColor: node.qualityIssues?.critical ? C.VIOLATION_RED : undefined,
          },
          label: {
            color: Style.slate[200],
            fontSize: 10,
            formatter: (p: any) => p.name.length > 15 ? p.name.substring(0, 15) + '...' : p.name,
          },
        })),
        links: chain.links.map(link => ({
          source: link.source,
          target: link.target,
          lineStyle: {
            color: link.type === 'violation' ? C.VIOLATION_RED : link.type === 'risk' ? C.RISK_AMBER : Style.blueCall,
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
          { depth: 0, itemStyle: { borderWidth: 2, borderColor: C.LEVEL_0_BORDER } },
          { depth: 1, itemStyle: { borderWidth: 2, borderColor: C.LEVEL_1_BORDER } },
          { depth: 2, itemStyle: { borderWidth: 2, borderColor: C.LEVEL_2_BORDER } },
          { depth: 3, itemStyle: { borderWidth: 2, borderColor: C.LEVEL_3_BORDER } },
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
    return Style.layerColor(layer);
  }

  private getLayerLabel(layer: string): string {
    const names = LABEL.CALL_CHAIN.LAYER_NAMES;
    return (names as Record<string, string>)[layer] || layer;
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
