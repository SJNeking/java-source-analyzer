/**
 * Java Source Analyzer - Project Assets View (Refactored)
 *
 * Uses Component base class for DOM creation.
 * Uses event delegation instead of inline onclick.
 * Uses centralized constants for icons, labels, and styles.
 */

import type { AnalysisResult } from '../types';
import { Component, type Child } from '../framework/component';
import { EventDelegator } from '../framework/events';
import { Logger } from '../utils/logger';
import { Style } from '../utils/style-helpers';
import { ICON, LABEL, CLS, C } from '../constants';

export class ProjectAssetsView extends Component {
  private containerId: string;
  private analysisData: AnalysisResult | null = null;
  private expandedNodes: Set<string> = new Set();
  private delegator: EventDelegator | null = null;

  constructor(containerId: string = 'assets-content') {
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

    const projectAssets = (data as any).project_assets;
    if (!projectAssets) {
      this.mount(container);
      return;
    }

    this.mount(container);
    this.setupEventListeners();
  }

  public buildRoot(): HTMLElement {
    const projectAssets = (this.analysisData as any)?.project_assets;

    if (!projectAssets) {
      return this.renderEmptyState();
    }

    const assetTypes = Object.keys(projectAssets).filter(key => Array.isArray(projectAssets[key]));
    const totalAssets = assetTypes.reduce((sum: number, type: string) => sum + projectAssets[type].length, 0);

    return this.el('div', null, [
      this.renderHeader(totalAssets, assetTypes.length),
      this.el('div', { className: CLS.CARD_GRID }, [
        this.renderAssetTypesTree(projectAssets, assetTypes),
      ]),
    ]);
  }

  private renderEmptyState(): HTMLElement {
    return this.el('div', { className: CLS.EMPTY_STATE }, [
      this.el('div', { className: CLS.EMPTY_ICON }, [this.text('📁')]),
      this.el('div', { className: CLS.EMPTY_TITLE }, [this.text(LABEL.ASSETS.NO_ASSETS)]),
      this.el('div', { className: CLS.EMPTY_DESC }, [
        this.text(LABEL.ASSETS.NO_ASSETS_DESC),
      ]),
    ]);
  }

  private renderHeader(totalAssets: number, typeCount: number): HTMLElement {
    return this.el('div', { className: CLS.CARD_GRID }, [
      this.el('div', { className: CLS.STAT_CARD }, [
        this.el('div', { className: CLS.STAT_VALUE }, [this.text('📁')]),
        this.el('div', { className: CLS.STAT_VALUE }, [this.text(totalAssets)]),
        this.el('div', { className: CLS.STAT_LABEL }, [this.text(LABEL.ASSETS.TOTAL_LABEL)]),
      ]),
      this.el('div', { className: CLS.STAT_CARD }, [
        this.el('div', { className: CLS.STAT_VALUE }, [this.text('📊')]),
        this.el('div', { className: CLS.STAT_VALUE }, [this.text(typeCount)]),
        this.el('div', { className: CLS.STAT_LABEL }, [this.text(LABEL.ASSETS.TYPE_COUNT_LABEL)]),
      ]),
    ]);
  }

  private renderAssetTypesTree(projectAssets: Record<string, any[]>, assetTypes: string[]): HTMLElement {
    const nodes: Child[] = [];

    for (const type of assetTypes) {
      const assets = projectAssets[type];
      if (!Array.isArray(assets) || assets.length === 0) continue;

      const icon = ICON.ASSET_TYPE[type.toUpperCase().replace(/-/g, '_') as keyof typeof ICON.ASSET_TYPE] || '📄';
      const isExpanded = this.expandedNodes.has(type);

      const nodeHeader = this.el('div', {
        className: 'assets-tree-node',
        'data-type': type,
      }, [
        this.el('span', { className: 'assets-tree-node-icon' }, [this.text(icon)]),
        this.text(LABEL.ASSETS.TYPE_NAMES[type as keyof typeof LABEL.ASSETS.TYPE_NAMES] || type),
        this.el('span', { style: { color: Style.slate[500], marginLeft: 'auto', fontSize: '12px' } as Partial<CSSStyleDeclaration> },
          [this.text(LABEL.ASSETS.COUNT_ITEMS(assets.length))]),
        this.el('span', { style: { marginLeft: '8px' } as Partial<CSSStyleDeclaration> },
          [this.text(isExpanded ? '▼' : '▶')]),
      ]);

      const children: Child[] = [nodeHeader];

      if (isExpanded) {
        const childItems = assets.map((asset, index) => this.renderAssetItem(type, asset, index));
        children.push(this.el('div', { className: 'assets-tree-children' }, childItems));
      }

      nodes.push(this.el('div', null, children));
    }

    return this.el('div', null, nodes);
  }

  private renderAssetItem(type: string, asset: Record<string, any>, _index: number): HTMLElement {
    let contentChildren: Child[] = [];

    const fileName = asset.file_name || asset.name || asset.path || LABEL.COMMON.UNKNOWN;
    const path = asset.path || '';

    if (type === 'maven_pom') {
      contentChildren = [
        this.el('div', { style: { fontWeight: '600', color: Style.green, marginBottom: '6px' } as Partial<CSSStyleDeclaration> },
          [this.text(asset.artifactId || LABEL.COMMON.UNKNOWN)]),
        this.el('div', { style: { fontSize: '12px', color: Style.grayLt } as Partial<CSSStyleDeclaration> }, [
          asset.groupId ? this.text(`${LABEL.ASSETS.GROUP_LABEL}${asset.groupId}`) : this.text(''),
          asset.version ? this.text(` · ${LABEL.ASSETS.VERSION_LABEL}${asset.version}`) : this.text(''),
        ]),
        this.el('div', { style: { fontSize: '11px', color: Style.slate[500], marginTop: '4px', fontFamily: "'Courier New', monospace" } as Partial<CSSStyleDeclaration> },
          [this.text(path)]),
        ...(asset.dependencies ? [
          this.el('div', { style: { fontSize: '11px', color: Style.blue200, marginTop: '4px' } as Partial<CSSStyleDeclaration> },
            [this.text(`${LABEL.ASSETS.DEPS_LABEL} ${asset.dependencies.length}`)]),
        ] : []),
      ];
    } else if (type.includes('config') || type === 'log_config') {
      contentChildren = [
        this.el('div', { style: { fontWeight: '600', color: Style.blue, marginBottom: '6px' } as Partial<CSSStyleDeclaration> },
          [this.text(fileName)]),
        this.el('div', { style: { fontSize: '11px', color: Style.slate[500], fontFamily: "'Courier New', monospace" } as Partial<CSSStyleDeclaration> },
          [this.text(path)]),
        ...(asset.middleware ? [
          this.el('div', { style: { fontSize: '11px', color: Style.orange, marginTop: '4px' } as Partial<CSSStyleDeclaration> },
            [this.text(`${LABEL.ASSETS.MIDDLEWARE_LABEL} ${asset.middleware}`)]),
        ] : []),
      ];
    } else if (type === 'sql_script' || type === 'mybatis_mapper') {
      contentChildren = [
        this.el('div', { style: { fontWeight: '600', color: Style.orange, marginBottom: '6px' } as Partial<CSSStyleDeclaration> },
          [this.text(fileName)]),
        this.el('div', { style: { fontSize: '11px', color: Style.slate[500], fontFamily: "'Courier New', monospace" } as Partial<CSSStyleDeclaration> },
          [this.text(path)]),
        ...(asset.namespace ? [
          this.el('div', { style: { fontSize: '11px', color: Style.blue200, marginTop: '4px' } as Partial<CSSStyleDeclaration> },
            [this.text(`${LABEL.ASSETS.NAMESPACE_LABEL}${asset.namespace}`)]),
        ] : []),
        ...(asset.tables ? [
          this.el('div', { style: { fontSize: '11px', color: Style.green, marginTop: '4px' } as Partial<CSSStyleDeclaration> },
            [this.text(`${LABEL.ASSETS.TABLES_LABEL} ${asset.tables.length}`)]),
        ] : []),
      ];
    } else if (type === 'dockerfile' || type === 'docker_compose') {
      contentChildren = [
        this.el('div', { style: { fontWeight: '600', color: Style.blue, marginBottom: '6px' } as Partial<CSSStyleDeclaration> },
          [this.text(fileName)]),
        this.el('div', { style: { fontSize: '11px', color: Style.slate[500], fontFamily: "'Courier New', monospace" } as Partial<CSSStyleDeclaration> },
          [this.text(path)]),
        ...(asset.jar_path ? [
          this.el('div', { style: { fontSize: '11px', color: Style.grayLt, marginTop: '4px' } as Partial<CSSStyleDeclaration> },
            [this.text(`${LABEL.ASSETS.JAR_LABEL} ${asset.jar_path}`)]),
        ] : []),
      ];
    } else {
      contentChildren = [
        this.el('div', { style: { fontWeight: '600', color: Style.slate[200], marginBottom: '6px' } as Partial<CSSStyleDeclaration> },
          [this.text(fileName)]),
        this.el('div', { style: { fontSize: '11px', color: Style.slate[500], fontFamily: "'Courier New', monospace" } as Partial<CSSStyleDeclaration> },
          [this.text(path)]),
      ];
    }

    return this.el('div', {
      style: { marginLeft: '24px', padding: '12px', background: Style.bg.assetCard, borderRadius: '8px', margin: '8px 0' } as Partial<CSSStyleDeclaration>,
    }, contentChildren);
  }

  private setupEventListeners(): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = new EventDelegator(this.root);

    this.delegator.on('click', '.assets-tree-node', (_e, el) => {
      const type = el.getAttribute('data-type');
      if (type) this.toggleNode(type);
    });
  }

  private toggleNode(type: string): void {
    if (this.expandedNodes.has(type)) {
      this.expandedNodes.delete(type);
    } else {
      this.expandedNodes.add(type);
    }

    if (this.analysisData) {
      this.update();
      this.setupEventListeners();
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
