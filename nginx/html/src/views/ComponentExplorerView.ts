/**
 * Java Source Analyzer - Component Explorer View
 * Hierarchical view: Packages -> Classes -> Methods/Fields.
 */

import type { AnalysisResult, Asset } from '../types';
import { Logger } from '../utils/logger';

export class ComponentExplorerView {
  private containerId: string;
  private data: AnalysisResult | null = null;
  private history: { level: string; address: string | null }[] = [{ level: 'packages', address: null }];

  constructor(containerId: string = 'explorer-content') {
    this.containerId = containerId;
  }

  public render(data: AnalysisResult): void {
    this.data = data;
    this.history = [{ level: 'packages', address: null }];
    this.draw();
  }

  private draw(): void {
    const container = document.getElementById(this.containerId);
    if (!container || !this.data) return;

    const current = this.history[this.history.length - 1];
    if (current.level === 'packages') this.renderPackages(container);
    else if (current.level === 'class') this.renderClassDetail(container, current.address!);
  }

  private renderPackages(container: HTMLElement): void {
    const assets = this.data!.assets || [];
    const packages: Record<string, Asset[]> = {};

    assets.forEach(a => {
      const parts = a.address.split('.');
      parts.pop(); 
      const pkg = parts.join('.') || 'default';
      if (!packages[pkg]) packages[pkg] = [];
      packages[pkg].push(a);
    });

    const sorted = Object.entries(packages).sort((a, b) => a[0].localeCompare(b[0]));

    container.innerHTML = `
      <div class="explorer-header" style="padding:16px; border-bottom:1px solid #334155; background:#020617;">
        <h2 class="explorer-title">📦 项目组件 (${assets.length})</h2>
      </div>
      <div style="padding: 20px; overflow-y: auto; height: calc(100% - 56px);">
        ${sorted.map(([pkg, items]) => `
          <div style="margin-bottom: 24px;">
            <div style="font-size: 11px; color: #64748b; margin-bottom: 8px; font-family: monospace; border-bottom: 1px solid #1e293b; padding-bottom: 4px;">📂 ${pkg}</div>
            <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 10px;">
              ${items.map(a => this.createCard(a)).join('')}
            </div>
          </div>
        `).join('')}
      </div>
    `;
  }

  private createCard(asset: Asset): string {
    const kind = asset.kind || 'CLASS';
    const name = asset.address.split('.').pop();
    const icon = kind === 'INTERFACE' ? '🔷' : kind === 'ENUM' ? '🔶' : '📄';
    const mCount = asset.methods_full?.length || 0;
    const fCount = asset.fields_matrix?.length || 0;

    return `
      <div onclick="window.__explorerNavigate('${asset.address}')" 
           class="asset-card" style="background: #1e293b; border: 1px solid #334155; border-radius: 6px; padding: 12px; cursor: pointer; transition: all 0.2s;"
           onmouseover="this.style.borderColor='#38bdf8'; this.style.transform='translateY(-2px)'"
           onmouseout="this.style.borderColor='#334155'; this.style.transform='none'">
        <div style="display:flex; align-items:center; gap:6px; margin-bottom:6px;">
          <span style="font-size:16px;">${icon}</span>
          <span style="font-weight:600; color:#f8fafc; font-size:13px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${name}</span>
        </div>
        <div style="font-size:10px; color:#64748b; display:flex; gap:6px;">
          <span style="background:rgba(56,189,248,0.1); color:#38bdf8; padding:2px 4px; border-radius:3px;">${kind}</span>
          <span>⚙️ ${mCount} 方法</span>
        </div>
      </div>
    `;
  }

  public navigateTo(address: string): void {
    this.history.push({ level: 'class', address });
    this.draw();
  }

  public goBack(): void {
    if (this.history.length > 1) {
      this.history.pop();
      this.draw();
    }
  }

  private renderClassDetail(container: HTMLElement, address: string): void {
    const asset = this.data!.assets?.find((a: any) => a.address === address);
    if (!asset) {
      container.innerHTML = `<div style="padding:40px;text-align:center;">⚠️ 未找到组件</div>`;
      return;
    }

    const methods = asset.methods_full || asset.methods || [];
    const fields = asset.fields_matrix || asset.fields || [];

    container.innerHTML = `
      <div class="explorer-header" style="padding:12px 16px; border-bottom:1px solid #334155; background:#020617; display:flex; align-items:center;">
        <button onclick="window.__explorerBack()" style="background:transparent; border:1px solid #334155; color:#94a3b8; cursor:pointer; padding:4px 10px; border-radius:4px; margin-right:12px;">← 返回</button>
        <div style="flex:1; min-width:0;">
          <div style="font-size:16px; font-weight:bold; color:#f8fafc; font-family:monospace;">${asset.address.split('.').pop()}</div>
          <div style="font-size:10px; color:#64748b; font-family:monospace; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${asset.address}</div>
        </div>
      </div>
      <div style="padding: 20px; overflow-y: auto; height: calc(100% - 56px); display:flex; flex-direction:column; gap:20px;">
        
        ${asset.description ? `
          <div>
            <div style="font-size:11px; color:#64748b; margin-bottom:6px; font-weight:600;">📝 描述</div>
            <div style="color:#cbd5e1; font-size:12px; line-height:1.5; background:#1e293b; padding:10px; border-radius:6px; border:1px solid #334155;">${asset.description}</div>
          </div>
        ` : ''}

        ${fields.length > 0 ? `
          <div>
            <div style="font-size:11px; color:#64748b; margin-bottom:6px; font-weight:600;">📊 字段 (${fields.length})</div>
            <div style="display:grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap:6px;">
              ${fields.map((f: any) => `
                <div style="background:#1e293b; border:1px solid #334155; border-radius:4px; padding:6px 10px; font-family:monospace; font-size:11px; display:flex; gap:6px;">
                  <span style="color:#a78bfa;">${f.type_path?.split('.').pop() || 'var'}</span>
                  <span style="color:#e2e8f0;">${f.name}</span>
                </div>
              `).join('')}
            </div>
          </div>
        ` : ''}

        <div>
          <div style="font-size:11px; color:#64748b; margin-bottom:6px; font-weight:600;">⚙️ 方法 (${methods.length})</div>
          <div style="display:flex; flex-direction:column; gap:6px;">
            ${methods.map((m: any) => `
              <div style="background:#1e293b; border:1px solid #334155; border-radius:4px; padding:8px 12px; font-family:monospace; font-size:11px;">
                <div style="display:flex; align-items:center; gap:6px; color:#38bdf8; font-weight:500;">
                  ${m.modifiers?.includes('public') ? '<span style="color:#34d399;">public</span>' : ''}
                  ${m.modifiers?.includes('static') ? '<span style="color:#a78bfa;">static</span>' : ''}
                  <span>${m.name}(...)</span>
                </div>
                ${m.description ? `<div style="color:#64748b; margin-top:2px; font-family:sans-serif;">${m.description}</div>` : ''}
              </div>
            `).join('')}
          </div>
        </div>

      </div>
    `;
  }
}
