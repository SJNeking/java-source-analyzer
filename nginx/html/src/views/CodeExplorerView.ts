/**
 * Java Source Analyzer - Code Explorer View
 * - Default: Package-level UML (all classes + import deps)
 * - Click class name: Class-level UML
 * - Click method name: Source code
 * - Audit panel: Comments + Tags (localStorage)
 */

import type { AnalysisResult, Asset } from '../types';
import { CONFIG } from '../config';
import { Logger } from '../utils/logger';
import { LRUCache } from '../utils/lru-cache';

export class CodeExplorerView {
  private containerId: string;
  private data: AnalysisResult | null = null;
  private selectedCode: { content: string; language: string; title: string; kind: string } | null = null;
  private _comments: Array<{author: string; text: string; time: string}> = [];
  private _tags: Array<{name: string; type: string}> = [];
  private _currentMethod: string = '';
  private _currentClass: string = '';
  private sourceCodeCache = new LRUCache<string, string>(50);

  constructor(containerId: string = 'code-explorer-content') {
    this.containerId = containerId;
  }

  public render(data: AnalysisResult): void {
    this.data = data;
    this._currentClass = '';
    this._currentMethod = '';
    this.selectedCode = null;
    this.draw();
  }

  private draw(): void {
    const container = document.getElementById(this.containerId);
    if (!container || !this.data) return;
    container.innerHTML = `
      <div class="explorer-layout">
        <div class="explorer-tree">
          <div class="explorer-tree-header">
            <input type="text" id="code-explorer-search" class="explorer-search" placeholder="🔍 搜索类或方法...">
          </div>
          <div id="tree-content" class="explorer-tree-content"></div>
        </div>
        <div id="code-panel" class="explorer-code">
          ${this.renderDefaultUmlView()}
        </div>
      </div>
    `;
    const searchInput = document.getElementById('code-explorer-search');
    searchInput?.addEventListener('input', (e) => this.filterTree((e.target as HTMLInputElement).value));
    this.renderTree(this.data.assets || []);
    this.bindMethodClicks();
    setTimeout(() => this.renderPackageUml(), 300);
  }

  private renderDefaultUmlView(): string {
    return `
      <div class="explorer-code-layout">
        <div class="explorer-code-main">
          <div class="explorer-code-header">
            <div class="explorer-breadcrumb">
              <span class="breadcrumb-text">📐 项目架构总览 — 点击类名查看类图，点击方法查看源码</span>
            </div>
          </div>
          <div class="uml-viewer" id="uml-container">
            <pre id="uml-pre" class="mermaid" style="display:none;"></pre>
          </div>
        </div>
        <div class="explorer-audit-panel">
          <div class="audit-tabs" role="tablist">
            <div class="audit-tab active" data-tab="comments" onclick="window.__switchAuditTab('comments')">💬 评论</div>
            <div class="audit-tab" data-tab="tags" onclick="window.__switchAuditTab('tags')">🏷️ 标签</div>
          </div>
          <div class="audit-content">
            <div class="audit-tab-panel active" id="audit-comments"><div class="audit-empty">暂无评论</div></div>
            <div class="audit-tab-panel" id="audit-tags" style="display:none;"><div class="audit-empty">暂无标签</div></div>
          </div>
        </div>
      </div>
    `;
  }

  private async renderPackageUml(): Promise<void> {
    if (!this.data?.assets) return;
    const assets = this.data.assets || [];
    let mermaidCode = 'classDiagram\n';
    assets.forEach((a: any) => { mermaidCode += `  class ${a.address.split('.').pop()}\n`; });
    assets.forEach((a: any) => {
      const cn = a.address.split('.').pop();
      (a.import_dependencies || []).slice(0, 6).forEach((dep: string) => {
        const dn = dep.split('.').pop();
        if (dn && dn !== cn && assets.some((o: any) => o.address.split('.').pop() === dn)) mermaidCode += `  ${cn} ..> ${dn}\n`;
      });
    });
    const preEl = document.getElementById('uml-pre');
    if (!preEl) return;
    preEl.textContent = mermaidCode;
    preEl.style.display = 'block';
    const mmd = (window as any).mermaid;
    if (!mmd) return;
    try {
      await mmd.run({ nodes: [preEl] });
      const svg = document.querySelector('#uml-container svg');
      if (svg) { svg.style.width = '100%'; svg.style.height = '100%'; svg.style.maxHeight = 'none'; svg.setAttribute('preserveAspectRatio', 'xMidYMid meet'); }
    } catch (err: any) {
      console.error('UML error:', err);
      const c = document.getElementById('uml-container');
      if (c) c.innerHTML = `<pre style="color:var(--warning);font-size:10px;white-space:pre-wrap;padding:16px;">${mermaidCode}</pre>`;
    }
  }

  private renderUmlView(asset: any): string {
    const kc = this.getKindColor(asset.kind || 'CLASS');
    const cn = asset.address.split('.').pop();
    const methods = asset.methods_full || asset.methods || [];
    const fields = asset.fields_matrix || asset.fields || [];
    let mc = `classDiagram\n  class ${cn} {\n`;
    fields.slice(0, 25).forEach((f: any) => { mc += `    ${f.type_path ? f.type_path.split('.').pop() : 'var'} ${f.name}\n`; });
    methods.slice(0, 25).forEach((m: any) => { mc += `    ${m.modifiers?.includes('public') ? '+' : '-'}${m.name}()\n`; });
    mc += `  }\n`;
    (asset.import_dependencies || []).slice(0, 10).forEach((dep: string) => { const d = dep.split('.').pop(); if (d && d !== cn) mc += `  ${cn} ..> ${d}\n`; });

    return `
      <div class="explorer-code-layout">
        <div class="explorer-code-main">
          <div class="explorer-code-header">
            <div class="explorer-breadcrumb">
              <span class="kind-badge" style="background:${kc}20;color:${kc};border:1px solid ${kc}40">${asset.kind}</span>
              <span class="breadcrumb-text">📐 ${cn} - 类图</span>
            </div>
          </div>
          <div class="uml-viewer"><div id="uml-container"><pre id="uml-pre" class="mermaid" style="display:none;"></pre></div></div>
        </div>
        <div class="explorer-audit-panel">
          <div class="audit-tabs" role="tablist">
            <div class="audit-tab active" data-tab="comments" onclick="window.__switchAuditTab('comments')">💬 评论</div>
            <div class="audit-tab" data-tab="tags" onclick="window.__switchAuditTab('tags')">🏷️ 标签</div>
          </div>
          <div class="audit-content">
            <div class="audit-tab-panel active" id="audit-comments">
              <div class="audit-comment-form">
                <textarea id="audit-comment-input" class="audit-textarea" placeholder="添加评论或思考..."></textarea>
                <button class="audit-submit-btn" onclick="window.__addComment()">提交</button>
              </div>
              <div class="audit-comment-list">
                ${this._comments.length === 0 ? '<div class="audit-empty">暂无评论</div>' :
                  this._comments.map((c) => `<div class="audit-comment-item"><div class="audit-comment-meta"><span class="audit-comment-author">${c.author||'匿名'}</span><span class="audit-comment-time">${c.time}</span></div><div class="audit-comment-text">${c.text}</div></div>`).join('')}
              </div>
            </div>
            <div class="audit-tab-panel" id="audit-tags" style="display:none;">
              <div class="audit-tag-input-row">
                <input type="text" id="audit-tag-input" class="audit-tag-input" placeholder="输入标签名称...">
                <button class="audit-tag-add-btn" onclick="window.__addTag()">添加</button>
              </div>
              <div class="audit-tag-cloud">
                ${this._tags.length === 0 ? '<div class="audit-empty">暂无标签</div>' :
                  this._tags.map((t) => `<span class="audit-tag-badge" style="background:${this.getKindColor(t.type)}15;color:${this.getKindColor(t.type)};border:1px solid ${this.getKindColor(t.type)}40">${t.name}<span class="audit-tag-remove" onclick="window.__removeTag('${t.name}')">✕</span></span>`).join('')}
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  private async renderClassUml(asset: any): Promise<void> {
    const cn = asset.address.split('.').pop();
    let mc = `classDiagram\n  class ${cn} {\n`;
    (asset.fields_matrix || asset.fields || []).slice(0, 25).forEach((f: any) => { mc += `    ${f.type_path ? f.type_path.split('.').pop() : 'var'} ${f.name}\n`; });
    (asset.methods_full || asset.methods || []).slice(0, 25).forEach((m: any) => { mc += `    ${m.modifiers?.includes('public') ? '+' : '-'}${m.name}()\n`; });
    mc += `  }\n`;
    (asset.import_dependencies || []).slice(0, 10).forEach((dep: string) => { const d = dep.split('.').pop(); if (d && d !== cn) mc += `  ${cn} ..> ${d}\n`; });

    const preEl = document.getElementById('uml-pre');
    if (!preEl) return;
    preEl.textContent = mc;
    preEl.style.display = 'block';
    const mmd = (window as any).mermaid;
    if (!mmd) return;
    try {
      await mmd.run({ nodes: [preEl] });
      const svg = document.querySelector('#uml-container svg');
      if (svg) { svg.style.width = '100%'; svg.style.height = '100%'; svg.style.maxHeight = 'none'; svg.setAttribute('preserveAspectRatio', 'xMidYMid meet'); }
    } catch (err: any) {
      console.error('UML error:', err);
      const c = document.getElementById('uml-container');
      if (c) c.innerHTML = `<pre style="color:var(--warning);font-size:10px;white-space:pre-wrap;padding:16px;">${mc}</pre>`;
    }
  }

  private renderCodePanel(code: string, title: string, kind: string): string {
    const lines = code.split('\n');
    const lineNums = lines.map((_, i) => `<div class="code-line-num">${i + 1}</div>`).join('');
    const content = lines.map(line => {
      let e = line.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
      e = e.replace(/\b(public|private|protected|static|final|abstract|synchronized|volatile)\b/g, '<span class="code-keyword">$1</span>');
      e = e.replace(/\b(class|interface|enum|extends|implements|new|this|super|return|if|else|for|while|try|catch|throw)\b/g, '<span class="code-keyword">$1</span>');
      e = e.replace(/\b(String|int|boolean|long|void|List|Map|Set|Collection)\b/g, '<span class="code-type">$1</span>');
      e = e.replace(/(\/\/.*)/g, '<span class="code-comment">$1</span>');
      e = e.replace(/(&quot;[^&]*?&quot;)/g, '<span class="code-string">$1</span>');
      return `<div class="code-line-content">${e}</div>`;
    }).join('');
    const kc = CONFIG.colorMap[kind as keyof typeof CONFIG.colorMap] || '#94a3b8';

    return `
      <div class="explorer-code-layout">
        <div class="explorer-code-main">
          <div class="explorer-code-header">
            <div class="explorer-breadcrumb">
              <span class="kind-badge" style="background:${kc}20;color:${kc};border:1px solid ${kc}40">${kind}</span>
              <span class="breadcrumb-text">📄 ${title}</span>
            </div>
            <div class="explorer-code-actions"><button class="code-action-btn" onclick="window.__copyCode()">📋 复制</button></div>
          </div>
          <div class="explorer-code-body"><div class="code-line-numbers">${lineNums}</div><div class="code-lines">${content}</div></div>
        </div>
        <div class="explorer-audit-panel">
          <div class="audit-tabs" role="tablist">
            <div class="audit-tab active" data-tab="comments" onclick="window.__switchAuditTab('comments')">💬 评论</div>
            <div class="audit-tab" data-tab="tags" onclick="window.__switchAuditTab('tags')">🏷️ 标签</div>
          </div>
          <div class="audit-content">
            <div class="audit-tab-panel active" id="audit-comments">
              <div class="audit-comment-form">
                <textarea id="audit-comment-input" class="audit-textarea" placeholder="添加评论或思考..."></textarea>
                <button class="audit-submit-btn" onclick="window.__addComment()">提交</button>
              </div>
              <div class="audit-comment-list">
                ${this._comments.length === 0 ? '<div class="audit-empty">暂无评论，成为第一个评论的人</div>' :
                  this._comments.map((c) => `<div class="audit-comment-item"><div class="audit-comment-meta"><span class="audit-comment-author">${c.author||'匿名'}</span><span class="audit-comment-time">${c.time}</span></div><div class="audit-comment-text">${c.text}</div></div>`).join('')}
              </div>
            </div>
            <div class="audit-tab-panel" id="audit-tags" style="display:none;">
              <div class="audit-tag-input-row">
                <input type="text" id="audit-tag-input" class="audit-tag-input" placeholder="输入标签名称...">
                <button class="audit-tag-add-btn" onclick="window.__addTag()">添加</button>
              </div>
              <div class="audit-tag-cloud">
                ${this._tags.length === 0 ? '<div class="audit-empty">暂无标签</div>' :
                  this._tags.map((t) => `<span class="audit-tag-badge" style="background:${this.getKindColor(t.type)}15;color:${this.getKindColor(t.type)};border:1px solid ${this.getKindColor(t.type)}40">${t.name}<span class="audit-tag-remove" onclick="window.__removeTag('${t.name}')">✕</span></span>`).join('')}
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  private getKindColor(kind: string): string {
    return { INTERFACE: '#60a5fa', ABSTRACT_CLASS: '#a78bfa', CLASS: '#4ade80', ENUM: '#fb923c', UTILITY: '#9ca3af' }[kind] || '#94a3b8';
  }

  private renderTree(assets: Asset[]): void {
    const content = document.getElementById('tree-content');
    if (!content) return;
    const packages: Record<string, Asset[]> = {};
    assets.forEach(a => { const p = a.address.split('.'); p.pop(); const pkg = p.join('.') || 'default'; if (!packages[pkg]) packages[pkg] = []; packages[pkg].push(a); });
    const kindOrder: Record<string, number> = { INTERFACE: 0, ABSTRACT_CLASS: 1, CLASS: 2, ENUM: 3, UTILITY: 4 };
    let html = '';
    Object.entries(packages).sort((a, b) => a[0].localeCompare(b[0])).forEach(([pkg, items]) => {
      items.sort((a, b) => (kindOrder[a.kind || 'CLASS'] ?? 99) - (kindOrder[b.kind || 'CLASS'] ?? 99));
      html += `<details class="pkg-group"><summary class="pkg-summary"><span class="pkg-icon">📂</span><span class="pkg-name">${pkg.split('.').pop()}</span><span class="pkg-count">${items.length} 类</span></summary><div class="pkg-content">${items.map(a => this.renderClassNode(a)).join('')}</div></details>`;
    });
    content.innerHTML = html;
    content.querySelectorAll('.pkg-group').forEach((g: any) => { g.addEventListener('toggle', () => { if (g.open) content.querySelectorAll('.pkg-group').forEach((o: any) => { if (o !== g) o.open = false; }); }); });
  }

  private renderClassNode(asset: Asset): string {
    const name = asset.address.split('.').pop();
    const methods = asset.methods_full || asset.methods || [];
    const kind = asset.kind || 'CLASS';
    const color = CONFIG.colorMap[kind as keyof typeof CONFIG.colorMap] || '#94a3b8';
    const icon = kind === 'INTERFACE' ? '🔵' : kind === 'ABSTRACT_CLASS' ? '🟣' : kind === 'ENUM' ? '🟠' : kind === 'CLASS' ? '🟢' : '⚪️';
    return `<details class="class-group" data-address="${asset.address}">
      <summary class="class-summary" title="点击查看 UML 类图">
        <span class="class-icon">${icon}</span><span class="class-name">${name}</span>
        <span class="class-tags"><span class="tag-kind" style="background:${color}15;color:${color}">${kind}</span></span>
      </summary>
      <div class="class-content">${methods.length > 0 ? methods.map(m => this.renderMethodNode(m, asset.address)).join('') : '<div class="no-methods">No methods</div>'}</div>
    </details>`;
  }

  private renderMethodNode(method: any, classAddress: string): string {
    let badges = '';
    if (method.modifiers?.includes('public')) badges += '<span class="mod-badge mod-public">pub</span>';
    if (method.modifiers?.includes('static')) badges += '<span class="mod-badge mod-static">st</span>';
    return `<div class="method-item" data-class="${classAddress}" data-method="${method.name}"><span class="method-name">${method.name}</span>${badges}</div>`;
  }

  public filterTree(term: string): void {
    const items = document.querySelectorAll('.method-item');
    const groups = document.querySelectorAll('.pkg-group, .class-group');
    const lower = term.toLowerCase();
    if (!term) { items.forEach(el => (el as HTMLElement).style.display = 'flex'); groups.forEach(el => (el as HTMLDetailsElement).open = true); return; }
    items.forEach(el => { (el as HTMLElement).style.display = (el.textContent?.toLowerCase() || '').includes(lower) ? 'flex' : 'none'; });
    groups.forEach(el => (el as HTMLDetailsElement).open = true);
  }

  public bindMethodClicks(): void {
    const container = document.getElementById(this.containerId);
    if (!container) return;
    container.onclick = (e: Event) => {
      const cs = (e.target as HTMLElement).closest('.class-summary');
      if (cs) {
        e.preventDefault();
        const cg = cs.closest('.class-group') as HTMLElement;
        if (cg) { const addr = cg.getAttribute('data-address'); if (addr) this.openClass(addr); }
        return;
      }
      const mi = (e.target as HTMLElement).closest('.method-item');
      if (mi) { const ca = mi.getAttribute('data-class'); const mn = mi.getAttribute('data-method'); if (ca && mn) this.openMethod(ca, mn); }
    };
  }

  private openClass(classAddr: string): void {
    if (!this.data) return;
    const asset = this.data.assets?.find((a: any) => a.address === classAddr);
    if (!asset) return;
    this._currentClass = classAddr; this._currentMethod = '';
    this.selectedCode = { content: '', language: 'java', title: asset.address.split('.').pop(), kind: asset.kind || 'CLASS' };
    this._loadFromStorage(classAddr, '__class__');
    const cp = document.getElementById('code-panel');
    if (cp) { cp.innerHTML = this.renderUmlView(asset); cp.scrollTo(0, 0); setTimeout(() => this.renderClassUml(asset), 100); }
  }

  private openMethod(classAddr: string, methodName: string): void {
    if (!this.data) return;
    const mk = `${classAddr}#${methodName}`;
    let code = this.sourceCodeCache.get(mk);
    if (code) { this.renderCode(code, classAddr, methodName); return; }
    const asset = this.data.assets?.find((a: any) => a.address === classAddr);
    if (!asset) return;
    const method = (asset.methods_full || asset.methods || []).find((m: any) => m.name === methodName);
    if (!method) return;
    code = method.source_code || method.body_code || `// No source code for ${methodName}`;
    this.sourceCodeCache.set(mk, code);
    this.renderCode(code, classAddr, methodName);
  }

  private renderCode(code: string, classAddr: string, methodName: string): void {
    const asset = this.data?.assets?.find((a: any) => a.address === classAddr);
    const kind = asset?.kind || 'CLASS';
    const title = `${classAddr.split('.').pop()}.${methodName}()`;
    this._currentMethod = methodName; this._currentClass = classAddr;
    this.selectedCode = { content: code, language: 'java', title, kind };
    this._loadFromStorage(classAddr, methodName);
    const cp = document.getElementById('code-panel');
    if (cp) { cp.innerHTML = this.renderCodePanel(code, title, kind); cp.scrollTo(0, 0); }
  }

  private _loadFromStorage(classAddr: string, methodName: string): void {
    const key = `audit_${classAddr}#${methodName}`;
    try { const s = localStorage.getItem(key); if (s) { const d = JSON.parse(s); this._comments = d.comments || []; this._tags = d.tags || []; } else { this._comments = []; this._tags = []; } } catch { this._comments = []; this._tags = []; }
  }

  private _saveToStorage(): void {
    if (!this._currentClass || !this._currentMethod) return;
    try { localStorage.setItem(`audit_${this._currentClass}#${this._currentMethod}`, JSON.stringify({ comments: this._comments, tags: this._tags })); } catch {}
  }

  private _rerenderAuditPanel(): void {
    if (!this.selectedCode) return;
    const cp = document.getElementById('code-panel');
    if (!cp) return;
    const isUml = cp.querySelector('.uml-viewer') !== null;
    if (isUml) {
      const asset = this.data?.assets?.find((a: any) => a.address === this._currentClass);
      if (asset) { cp.innerHTML = this.renderUmlView(asset); setTimeout(() => this.renderClassUml(asset), 100); }
    } else { cp.innerHTML = this.renderCodePanel(this.selectedCode.content, this.selectedCode.title, this.selectedCode.kind); }
  }
}

(window as any).__copyCode = () => { if (navigator.clipboard && document.querySelector('.code-lines')) navigator.clipboard.writeText(document.querySelector('.code-lines')!.textContent || ''); };
(window as any).__switchAuditTab = (tab: string) => {
  document.querySelectorAll('.audit-tab').forEach(t => t.classList.toggle('active', (t as HTMLElement).dataset.tab === tab));
  document.querySelectorAll('.audit-tab-panel').forEach(p => { (p as HTMLElement).style.display = p.id === `audit-${tab}` ? 'block' : 'none'; });
};
(window as any).__addComment = () => {
  const i = document.getElementById('audit-comment-input') as HTMLTextAreaElement;
  if (!i || !i.value.trim()) return;
  const v = window.codeExplorerView as any;
  if (v) { v._comments.unshift({ author: '当前用户', text: i.value.trim(), time: new Date().toLocaleTimeString() }); v._saveToStorage(); v._rerenderAuditPanel(); }
};
(window as any).__addTag = () => {
  const i = document.getElementById('audit-tag-input') as HTMLInputElement;
  if (!i || !i.value.trim()) return;
  const v = window.codeExplorerView as any;
  if (v) { v._tags.push({ name: i.value.trim(), type: v.selectedCode?.kind || 'CLASS' }); i.value = ''; v._saveToStorage(); v._rerenderAuditPanel(); }
};
(window as any).__removeTag = (name: string) => {
  const v = window.codeExplorerView as any;
  if (v) { v._tags = (v._tags || []).filter((t: any) => t.name !== name); v._saveToStorage(); v._rerenderAuditPanel(); }
};
