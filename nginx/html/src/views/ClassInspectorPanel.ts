/**
 * Java Source Analyzer - Class Inspector Panel
 * Clean, stable inspector showing Info, Fields, Methods, and Source Code.
 */

import type { Asset } from '../types';
import { Logger } from '../utils/logger';

export class ClassInspectorPanel {
  private panel: HTMLElement | null = null;
  private analysisData: any = null;
  private currentAsset: Asset | null = null;
  private expandedMethod: string | null = null;
  private sourceCodeCache: Map<string, string> = new Map();
  private backdrop: HTMLElement | null = null;

  constructor() {
    this.createPanel();
  }

  private createPanel(): void {
    if (!this.panel) {
      // Backdrop
      this.backdrop = document.createElement('div');
      this.backdrop.style.cssText = `position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 9998; opacity: 0; pointer-events: none; transition: opacity 0.25s ease; backdrop-filter: blur(4px);`;
      this.backdrop.addEventListener('click', () => this.hide());
      document.body.appendChild(this.backdrop);

      // Panel
      this.panel = document.createElement('div');
      this.panel.id = 'class-inspector';
      this.panel.style.cssText = `position: fixed; top: 56px; right: -550px; bottom: 0; width: 550px; background: #0f172a; border-left: 1px solid rgba(255,255,255,0.1); z-index: 9999; transition: right 0.35s cubic-bezier(0.4, 0, 0.2, 1); display: flex; flex-direction: column; box-shadow: -10px 0 40px rgba(0,0,0,0.8);`;

      // Header
      this.panel.innerHTML = `
        <div style="padding: 20px; border-bottom: 1px solid rgba(255,255,255,0.05); background: #020617;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
            <div style="flex: 1; min-width: 0;">
              <div style="font-size: 18px; font-weight: 700; color: #f8fafc; font-family: monospace;" id="inspector-title">Loading...</div>
              <div style="font-size: 11px; color: #64748b; margin-top: 4px; font-family: monospace; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" id="inspector-fqn"></div>
            </div>
            <button id="inspector-close" style="background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); color: #94a3b8; font-size: 14px; width: 28px; height: 28px; border-radius: 50%; cursor: pointer;">✕</button>
          </div>
          <div id="inspector-tabs" style="display: flex; gap: 16px;">
            <div class="tab-btn" data-tab="info" style="font-size: 12px; font-weight: 600; color: #38bdf8; cursor: pointer; padding-bottom: 8px; border-bottom: 2px solid #38bdf8;">📋 信息</div>
            <div class="tab-btn" data-tab="code" style="font-size: 12px; font-weight: 600; color: #64748b; cursor: pointer; padding-bottom: 8px; border-bottom: 2px solid transparent;">📄 源码</div>
          </div>
        </div>
      `;
      
      // Content
      const content = document.createElement('div');
      content.id = 'inspector-content';
      content.style.cssText = 'flex: 1; overflow: hidden; position: relative; background: #0f172a;';
      this.panel.appendChild(content);
      document.body.appendChild(this.panel);

      // Events
      document.getElementById('inspector-close')!.addEventListener('click', () => this.hide());
      document.getElementById('inspector-tabs')!.addEventListener('click', (e: Event) => {
        const target = (e.target as HTMLElement).closest('.tab-btn');
        if (target) this.switchTab(target.getAttribute('data-tab') || 'info');
      });
    }
  }

  public show(asset: Asset, data: any): void {
    this.currentAsset = asset;
    this.analysisData = data;
    this.expandedMethod = null;

    if (!this.panel) this.createPanel();

    // Header
    const titleEl = document.getElementById('inspector-title');
    const fqnEl = document.getElementById('inspector-fqn');
    if (titleEl) titleEl.innerText = asset.address.split('.').pop() || 'Unknown';
    if (fqnEl) { fqnEl.innerText = asset.address; fqnEl.title = asset.address; }

    // Show
    this.panel!.style.right = '0';
    if (this.backdrop) { this.backdrop.style.opacity = '1'; this.backdrop.style.pointerEvents = 'auto'; }

    this.switchTab('info');
  }

  public hide(): void {
    if (this.panel) this.panel.style.right = '-550px';
    if (this.backdrop) { this.backdrop.style.opacity = '0'; this.backdrop.style.pointerEvents = 'none'; }
  }

  private switchTab(tab: string): void {
    document.querySelectorAll('.tab-btn').forEach(btn => {
      const isActive = btn.getAttribute('data-tab') === tab;
      (btn as HTMLElement).style.color = isActive ? '#38bdf8' : '#64748b';
      (btn as HTMLElement).style.borderBottomColor = isActive ? '#38bdf8' : 'transparent';
    });

    const container = document.getElementById('inspector-content');
    if (!container) return;

    if (tab === 'info') container.innerHTML = `<div style="padding: 20px; overflow-y: auto; height: 100%;">${this.renderInfo()}</div>`;
    else if (tab === 'code') container.innerHTML = `<div style="padding: 20px; overflow-y: auto; height: 100%;">${this.renderCode()}</div>`;
  }

  private renderInfo(): string {
    if (!this.currentAsset) return '';
    const a = this.currentAsset;
    const fields = a.fields_matrix || a.fields || [];
    
    return `
      <div style="font-size: 13px; color: #cbd5e1; line-height: 1.7;">
        <div style="margin-bottom: 24px;">
          <div style="color: #64748b; margin-bottom: 6px; font-weight: 600; font-size: 11px;">类型</div>
          <div style="padding: 10px; background: #1e293b; border-radius: 6px; border: 1px solid #334155; color: #38bdf8;">${a.kind || 'CLASS'}</div>
        </div>
        ${a.description ? `<div style="margin-bottom: 24px;"><div style="color: #64748b; margin-bottom: 6px; font-weight: 600; font-size: 11px;">描述</div><div style="padding: 10px; background: #1e293b; border-radius: 6px; border: 1px solid #334155;">${a.description}</div></div>` : ''}
        ${fields.length > 0 ? `
        <div style="margin-bottom: 24px;">
          <div style="color: #64748b; margin-bottom: 6px; font-weight: 600; font-size: 11px;">字段 (${fields.length})</div>
          <div style="display:flex; flex-direction:column; gap:6px;">
            ${fields.slice(0, 10).map((f: any) => `
              <div style="font-family:monospace; font-size: 12px; display:flex; align-items:center; gap:8px; background: #0f172a; padding: 6px 10px; border-radius: 4px;">
                <span style="color:#a78bfa;">${f.type_path?.split('.').pop() || 'var'}</span>
                <span style="color:#e2e8f0;">${f.name}</span>
              </div>
            `).join('')}
          </div>
        </div>
        ` : ''}
      </div>
    `;
  }

  private renderCode(): string {
    if (!this.currentAsset) return '';
    const methods = this.currentAsset.methods_full || this.currentAsset.methods || [];
    if (!methods.length) return '<div style="text-align:center;color:#64748b;">暂无源码</div>';

    return `
      <div style="display:flex; flex-direction:column; gap:8px;">
        ${methods.map((m: any, i: number) => {
          const isExpanded = this.expandedMethod === m.address;
          const code = isExpanded ? (this.sourceCodeCache.get(m.address) || m.source_code || m.body_code || '// 无代码') : null;
          
          return `
            <div style="background:#1e293b; border:1px solid #334155; border-radius:6px; overflow:hidden;">
              <div style="padding:10px 14px; cursor:pointer; display:flex; justify-content:space-between; align-items:center; background:#0f172a;" 
                   onclick="classInspector.toggleCode(${i})">
                <div style="font-family:monospace; font-size:12px; color:#38bdf8;">${m.name || 'Unknown'}</div>
                <span style="font-size:10px; color:#64748b;">${isExpanded ? '▲' : '▼'}</span>
              </div>
              ${isExpanded && code ? `<pre style="padding:12px; margin:0; font-size:11px; color:#cbd5e1; background:#020617; overflow-x:auto; font-family:monospace;">${this.highlightCode(code)}</pre>` : ''}
            </div>
          `;
        }).join('')}
      </div>
    `;
  }

  public toggleCode(index: number): void {
    const methods = this.currentAsset?.methods_full || this.currentAsset?.methods || [];
    const method = methods[index];
    if (!method) return;

    if (this.expandedMethod === method.address) {
      this.expandedMethod = null;
    } else {
      this.expandedMethod = method.address;
      // Cache code
      const code = method.source_code || method.body_code || '// 无代码';
      this.sourceCodeCache.set(method.address, code);
    }
    this.switchTab('code');
  }

  private highlightCode(code: string): string {
    let escaped = code.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    const rules: [RegExp, string][] = [
      [/(\/\/[^\n]*)/g, 'color:#6a9955;'], [/(\/\*[\s\S]*?\*\/)/g, 'color:#6a9955;'],
      [/\b(public|private|protected|static|final|abstract|synchronized|volatile)\b/g, 'color:#c084fc;font-weight:bold;'],
      [/\b(class|interface|enum|extends|implements|new|this|super)\b/g, 'color:#38bdf8;font-weight:bold;'],
      [/\b(if|else|for|while|return|try|catch|throw)\b/g, 'color:#c084fc;font-weight:bold;'],
      [/\b(String|int|boolean|long|void|List|Map)\b/g, 'color:#34d399;'],
      [/@\w+/g, 'color:#fbbf24;'],
      [/(&quot;[^&]*?&quot;)/g, 'color:#fb923c;'],
    ];
    for (const [p, s] of rules) escaped = escaped.replace(p, `<span style="${s}">$&</span>`);
    return escaped;
  }
}
