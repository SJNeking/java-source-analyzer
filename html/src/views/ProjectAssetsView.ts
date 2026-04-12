/**
 * Java Source Analyzer - Project Assets View (Refactored)
 *
 * Uses Component base class for DOM creation.
 * Uses event delegation instead of inline onclick.
 * Uses centralized constants for icons.
 */

import type { AnalysisResult } from '../types';
import { Component, type Child } from '../framework/component';
import { EventDelegator } from '../framework/events';
import { Logger } from '../utils/logger';
import { ICON } from '../constants';

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
      this.el('div', { className: 'assets-tree' }, [
        this.renderAssetTypesTree(projectAssets, assetTypes),
      ]),
    ]);
  }

  private renderEmptyState(): HTMLElement {
    return this.el('div', { className: 'empty-state' }, [
      this.el('div', { className: 'empty-state-icon' }, [this.text('📁')]),
      this.el('div', { className: 'empty-state-title' }, [this.text('暂无项目资产数据')]),
      this.el('div', { className: 'empty-state-desc' }, [
        this.text('当前项目未包含非 Java 文件资产信息。请使用 Java 分析工具的完整分析模式重新扫描，以获取 XML、YAML、SQL、Dockerfile 等配置文件详情。'),
      ]),
    ]);
  }

  private renderHeader(totalAssets: number, typeCount: number): HTMLElement {
    return this.el('div', { className: 'card-grid' }, [
      this.el('div', { className: 'stat-card' }, [
        this.el('div', { className: 'stat-card-icon' }, [this.text('📁')]),
        this.el('div', { className: 'stat-card-value' }, [this.text(totalAssets)]),
        this.el('div', { className: 'stat-card-label' }, [this.text('总资产数')]),
      ]),
      this.el('div', { className: 'stat-card' }, [
        this.el('div', { className: 'stat-card-icon' }, [this.text('📊')]),
        this.el('div', { className: 'stat-card-value' }, [this.text(typeCount)]),
        this.el('div', { className: 'stat-card-label' }, [this.text('资产类型数')]),
      ]),
    ]);
  }

  private renderAssetTypesTree(projectAssets: Record<string, any[]>, assetTypes: string[]): HTMLElement {
    const nodes: Child[] = [];

    for (const type of assetTypes) {
      const assets = projectAssets[type];
      if (!Array.isArray(assets) || assets.length === 0) continue;

      const icon = ICON.ASSET_TYPE[type as keyof typeof ICON.ASSET_TYPE] || '📄';
      const isExpanded = this.expandedNodes.has(type);

      const nodeHeader = this.el('div', {
        className: 'assets-tree-node',
        'data-type': type,
      }, [
        this.el('span', { className: 'assets-tree-node-icon' }, [this.text(icon)]),
        this.text(this.formatTypeName(type)),
        this.el('span', { style: { color: '#718096', marginLeft: 'auto', fontSize: '12px' } as Partial<CSSStyleDeclaration> },
          [this.text(`${assets.length} 项`)]),
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

    const fileName = asset.file_name || asset.name || asset.path || 'Unknown';
    const path = asset.path || '';

    if (type === 'maven_pom') {
      contentChildren = [
        this.el('div', { style: { fontWeight: '600', color: '#48bb78', marginBottom: '6px' } as Partial<CSSStyleDeclaration> },
          [this.text(asset.artifactId || 'Unknown')]),
        this.el('div', { style: { fontSize: '12px', color: '#a0aec0' } as Partial<CSSStyleDeclaration> }, [
          asset.groupId ? this.text(`Group: ${asset.groupId}`) : this.text(''),
          asset.version ? this.text(` · Version: ${asset.version}`) : this.text(''),
        ]),
        this.el('div', { style: { fontSize: '11px', color: '#718096', marginTop: '4px', fontFamily: "'Courier New', monospace" } as Partial<CSSStyleDeclaration> },
          [this.text(path)]),
        ...(asset.dependencies ? [
          this.el('div', { style: { fontSize: '11px', color: '#63b3ed', marginTop: '4px' } as Partial<CSSStyleDeclaration> },
            [this.text(`依赖数: ${asset.dependencies.length}`)]),
        ] : []),
      ];
    } else if (type.includes('config') || type === 'log_config') {
      contentChildren = [
        this.el('div', { style: { fontWeight: '600', color: '#4299e1', marginBottom: '6px' } as Partial<CSSStyleDeclaration> },
          [this.text(fileName)]),
        this.el('div', { style: { fontSize: '11px', color: '#718096', fontFamily: "'Courier New', monospace" } as Partial<CSSStyleDeclaration> },
          [this.text(path)]),
        ...(asset.middleware ? [
          this.el('div', { style: { fontSize: '11px', color: '#ed8936', marginTop: '4px' } as Partial<CSSStyleDeclaration> },
            [this.text(`中间件: ${asset.middleware}`)]),
        ] : []),
      ];
    } else if (type === 'sql_script' || type === 'mybatis_mapper') {
      contentChildren = [
        this.el('div', { style: { fontWeight: '600', color: '#ed8936', marginBottom: '6px' } as Partial<CSSStyleDeclaration> },
          [this.text(fileName)]),
        this.el('div', { style: { fontSize: '11px', color: '#718096', fontFamily: "'Courier New', monospace" } as Partial<CSSStyleDeclaration> },
          [this.text(path)]),
        ...(asset.namespace ? [
          this.el('div', { style: { fontSize: '11px', color: '#63b3ed', marginTop: '4px' } as Partial<CSSStyleDeclaration> },
            [this.text(`Namespace: ${asset.namespace}`)]),
        ] : []),
        ...(asset.tables ? [
          this.el('div', { style: { fontSize: '11px', color: '#48bb78', marginTop: '4px' } as Partial<CSSStyleDeclaration> },
            [this.text(`表数: ${asset.tables.length}`)]),
        ] : []),
      ];
    } else if (type === 'dockerfile' || type === 'docker_compose') {
      contentChildren = [
        this.el('div', { style: { fontWeight: '600', color: '#4299e1', marginBottom: '6px' } as Partial<CSSStyleDeclaration> },
          [this.text(fileName)]),
        this.el('div', { style: { fontSize: '11px', color: '#718096', fontFamily: "'Courier New', monospace" } as Partial<CSSStyleDeclaration> },
          [this.text(path)]),
        ...(asset.jar_path ? [
          this.el('div', { style: { fontSize: '11px', color: '#a0aec0', marginTop: '4px' } as Partial<CSSStyleDeclaration> },
            [this.text(`JAR: ${asset.jar_path}`)]),
        ] : []),
      ];
    } else {
      contentChildren = [
        this.el('div', { style: { fontWeight: '600', color: '#e2e8f0', marginBottom: '6px' } as Partial<CSSStyleDeclaration> },
          [this.text(fileName)]),
        this.el('div', { style: { fontSize: '11px', color: '#718096', fontFamily: "'Courier New', monospace" } as Partial<CSSStyleDeclaration> },
          [this.text(path)]),
      ];
    }

    return this.el('div', {
      style: { marginLeft: '24px', padding: '12px', background: 'rgba(26, 32, 44, 0.5)', borderRadius: '8px', margin: '8px 0' } as Partial<CSSStyleDeclaration>,
    }, contentChildren);
  }

  private formatTypeName(type: string): string {
    const nameMap: Record<string, string> = {
      'maven_pom': 'Maven POM',
      'yaml_config': 'YAML 配置',
      'properties_config': 'Properties 配置',
      'sql_script': 'SQL 脚本',
      'mybatis_mapper': 'MyBatis Mapper',
      'dockerfile': 'Dockerfile',
      'docker_compose': 'Docker Compose',
      'shell_script': 'Shell 脚本',
      'log_config': '日志配置',
      'markdown_doc': 'Markdown 文档',
      'modules': '模块',
      'scan_summary': '扫描摘要',
      'errors': '错误',
    };
    return nameMap[type] || type;
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
