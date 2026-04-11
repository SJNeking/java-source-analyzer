/**
 * Java Source Analyzer - Project Assets View
 * Displays non-Java file assets (XML, YAML, SQL, Docker, etc.)
 */

import type { AnalysisResult } from '../types';
import { Logger } from '../utils/logger';

/**
 * Project Assets View Controller
 */
export class ProjectAssetsView {
  /** Container element ID */
  private containerId: string;

  /** Current analysis data */
  private analysisData: AnalysisResult | null = null;

  /** Expanded node paths */
  private expandedNodes: Set<string> = new Set();

  /**
   * Create a new ProjectAssetsView
   * @param containerId - ID of the container element
   */
  constructor(containerId: string = 'assets-content') {
    this.containerId = containerId;
  }

  /**
   * Render project assets view
   * @param data - Analysis result data
   */
  public render(data: AnalysisResult): void {
    this.analysisData = data;
    const container = document.getElementById(this.containerId);

    if (!container) {
      Logger.error(`Container not found: ${this.containerId}`);
      return;
    }

    // Check if project assets data exists
    const projectAssets = (data as any).project_assets;

    if (!projectAssets) {
      container.innerHTML = this.renderEmptyState();
      return;
    }

    // Count assets by type
    const assetTypes = Object.keys(projectAssets).filter(key => Array.isArray(projectAssets[key]));
    const totalAssets = assetTypes.reduce((sum, type) => sum + projectAssets[type].length, 0);

    // Render view
    container.innerHTML = `
      ${this.renderHeader(totalAssets, assetTypes.length)}
      <div class="assets-tree">
        ${this.renderAssetTypes(projectAssets, assetTypes)}
      </div>
    `;
  }

  /**
   * Render empty state
   */
  private renderEmptyState(): string {
    return `
      <div class="empty-state">
        <div class="empty-state-icon">📁</div>
        <div class="empty-state-title">暂无项目资产数据</div>
        <div class="empty-state-desc">
          当前项目未包含非 Java 文件资产信息。请使用 Java 分析工具的完整分析模式重新扫描，
          以获取 XML、YAML、SQL、Dockerfile 等配置文件详情。
        </div>
      </div>
    `;
  }

  /**
   * Render header with stats
   */
  private renderHeader(totalAssets: number, typeCount: number): string {
    return `
      <div class="card-grid">
        <div class="stat-card">
          <div class="stat-card-icon">📁</div>
          <div class="stat-card-value">${totalAssets}</div>
          <div class="stat-card-label">总资产数</div>
        </div>
        <div class="stat-card">
          <div class="stat-card-icon">📊</div>
          <div class="stat-card-value">${typeCount}</div>
          <div class="stat-card-label">资产类型数</div>
        </div>
      </div>
    `;
  }

  /**
   * Render asset types tree
   */
  private renderAssetTypes(projectAssets: any, assetTypes: string[]): string {
    const iconMap: Record<string, string> = {
      'maven_pom': '📦',
      'yaml_config': '⚙️',
      'properties_config': '⚙️',
      'sql_script': '🗄️',
      'mybatis_mapper': '🔗',
      'dockerfile': '🐳',
      'docker_compose': '🐳',
      'shell_script': '📜',
      'log_config': '📝',
      'markdown_doc': '📖',
      'modules': '📦',
      'scan_summary': '📊',
      'errors': '❌'
    };

    return assetTypes.map(type => {
      const assets = projectAssets[type];
      if (!Array.isArray(assets) || assets.length === 0) return '';

      const icon = iconMap[type] || '📄';
      const isExpanded = this.expandedNodes.has(type);

      return `
        <div class="assets-tree-node" onclick="assetsView.toggleNode('${type}', this)">
          <span class="assets-tree-node-icon">${icon}</span>
          <span>${this.formatTypeName(type)}</span>
          <span style="color: #718096; margin-left: auto; font-size: 12px;">${assets.length} 项</span>
          <span style="margin-left: 8px;">${isExpanded ? '▼' : '▶'}</span>
        </div>
        ${isExpanded ? `
          <div class="assets-tree-children">
            ${assets.map((asset: any, index: number) => this.renderAssetItem(type, asset, index)).join('')}
          </div>
        ` : ''}
      `;
    }).join('');
  }

  /**
   * Render individual asset item
   */
  private renderAssetItem(type: string, asset: any, index: number): string {
    // Different asset types have different structures
    let content = '';

    if (type === 'maven_pom') {
      content = `
        <div style="margin-left: 24px; padding: 12px; background: rgba(26, 32, 44, 0.5); border-radius: 8px; margin: 8px 0;">
          <div style="font-weight: 600; color: #48bb78; margin-bottom: 6px;">
            ${asset.artifactId || 'Unknown'}
          </div>
          <div style="font-size: 12px; color: #a0aec0;">
            ${asset.groupId ? `<span>Group: ${asset.groupId}</span>` : ''}
            ${asset.version ? ` · Version: ${asset.version}` : ''}
          </div>
          <div style="font-size: 11px; color: #718096; margin-top: 4px; font-family: 'Courier New', monospace;">
            ${asset.path || asset.file_name || ''}
          </div>
          ${asset.dependencies ? `
            <div style="font-size: 11px; color: #63b3ed; margin-top: 4px;">
              依赖数: ${asset.dependencies.length}
            </div>
          ` : ''}
        </div>
      `;
    } else if (type.includes('config') || type === 'log_config') {
      content = `
        <div style="margin-left: 24px; padding: 12px; background: rgba(26, 32, 44, 0.5); border-radius: 8px; margin: 8px 0;">
          <div style="font-weight: 600; color: #4299e1; margin-bottom: 6px;">
            ${asset.file_name || 'Unknown'}
          </div>
          <div style="font-size: 11px; color: #718096; font-family: 'Courier New', monospace;">
            ${asset.path || ''}
          </div>
          ${asset.middleware ? `
            <div style="font-size: 11px; color: #ed8936; margin-top: 4px;">
              中间件: ${asset.middleware}
            </div>
          ` : ''}
        </div>
      `;
    } else if (type === 'sql_script' || type === 'mybatis_mapper') {
      content = `
        <div style="margin-left: 24px; padding: 12px; background: rgba(26, 32, 44, 0.5); border-radius: 8px; margin: 8px 0;">
          <div style="font-weight: 600; color: #ed8936; margin-bottom: 6px;">
            ${asset.file_name || 'Unknown'}
          </div>
          <div style="font-size: 11px; color: #718096; font-family: 'Courier New', monospace;">
            ${asset.path || ''}
          </div>
          ${asset.namespace ? `
            <div style="font-size: 11px; color: #63b3ed; margin-top: 4px;">
              Namespace: ${asset.namespace}
            </div>
          ` : ''}
          ${asset.tables ? `
            <div style="font-size: 11px; color: #48bb78; margin-top: 4px;">
              表数: ${asset.tables.length}
            </div>
          ` : ''}
        </div>
      `;
    } else if (type === 'dockerfile' || type === 'docker_compose') {
      content = `
        <div style="margin-left: 24px; padding: 12px; background: rgba(26, 32, 44, 0.5); border-radius: 8px; margin: 8px 0;">
          <div style="font-weight: 600; color: #4299e1; margin-bottom: 6px;">
            ${asset.file_name || 'Unknown'}
          </div>
          <div style="font-size: 11px; color: #718096; font-family: 'Courier New', monospace;">
            ${asset.path || ''}
          </div>
          ${asset.jar_path ? `
            <div style="font-size: 11px; color: #a0aec0; margin-top: 4px;">
              JAR: ${asset.jar_path}
            </div>
          ` : ''}
        </div>
      `;
    } else if (type === 'shell_script') {
      content = `
        <div style="margin-left: 24px; padding: 12px; background: rgba(26, 32, 44, 0.5); border-radius: 8px; margin: 8px 0;">
          <div style="font-weight: 600; color: #48bb78; margin-bottom: 6px;">
            ${asset.file_name || 'Unknown'}
          </div>
          <div style="font-size: 11px; color: #718096; font-family: 'Courier New', monospace;">
            ${asset.path || ''}
          </div>
        </div>
      `;
    } else if (type === 'markdown_doc') {
      content = `
        <div style="margin-left: 24px; padding: 12px; background: rgba(26, 32, 44, 0.5); border-radius: 8px; margin: 8px 0;">
          <div style="font-weight: 600; color: #a0aec0; margin-bottom: 6px;">
            ${asset.file_name || 'Unknown'}
          </div>
          <div style="font-size: 11px; color: #718096; font-family: 'Courier New', monospace;">
            ${asset.path || ''}
          </div>
        </div>
      `;
    } else {
      // Generic asset rendering
      content = `
        <div style="margin-left: 24px; padding: 12px; background: rgba(26, 32, 44, 0.5); border-radius: 8px; margin: 8px 0;">
          <div style="font-weight: 600; color: #e2e8f0; margin-bottom: 6px;">
            ${asset.file_name || asset.name || asset.path || 'Unknown'}
          </div>
          <div style="font-size: 11px; color: #718096; font-family: 'Courier New', monospace;">
            ${asset.path || ''}
          </div>
        </div>
      `;
    }

    return content;
  }

  /**
   * Format asset type name for display
   */
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
      'errors': '错误'
    };

    return nameMap[type] || type;
  }

  /**
   * Toggle tree node expansion
   */
  public toggleNode(type: string, element: HTMLElement): void {
    if (this.expandedNodes.has(type)) {
      this.expandedNodes.delete(type);
    } else {
      this.expandedNodes.add(type);
    }

    // Re-render view
    if (this.analysisData) {
      this.render(this.analysisData);
    }
  }

  /**
   * Get container element
   */
  public getContainer(): HTMLElement | null {
    return document.getElementById(this.containerId);
  }
}
