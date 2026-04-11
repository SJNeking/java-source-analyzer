/**
 * Java Source Analyzer - Code Explorer View
 * 
 * Redesigned with proper color coding and professional tree layout.
 * Features:
 * 1. Left: Color-coded tree (Package -> Class -> Method) with exact kind colors
 * 2. Right: Code viewer with breadcrumbs, copy button, and syntax highlighting
 */

import type { AnalysisResult, Asset } from '../types';
import { CONFIG } from '../config';
import { Logger } from '../utils/logger';
import { LRUCache } from '../utils/lru-cache';

export class CodeExplorerView {
  private containerId: string;
  private data: AnalysisResult | null = null;
  
  // State
  private selectedCode: { content: string; language: string; title: string; kind: string } | null = null;

  // LRU Cache for source code snippets (Limit: 50 snippets)
  private sourceCodeCache = new LRUCache<string, string>(50);

  constructor(containerId: string = 'code-explorer-content') {
    this.containerId = containerId;
  }

  public render(data: AnalysisResult): void {
    this.data = data;
    this.draw();
  }

  private draw(): void {
    const container = document.getElementById(this.containerId);
    if (!container || !this.data) return;

    container.innerHTML = `
      <div class="explorer-layout">
        <!-- Left: Tree Panel -->
        <div class="explorer-tree">
          <div class="explorer-tree-header">
            <input type="text" id="code-explorer-search" class="explorer-search" placeholder="🔍 搜索类或方法...">
          </div>
          <div id="tree-content" class="explorer-tree-content"></div>
        </div>
        
        <!-- Right: Code Panel -->
        <div id="code-panel" class="explorer-code">
          ${this.renderEmptyState()}
        </div>
      </div>
    `;

    // Bind Search Event
    const searchInput = document.getElementById('code-explorer-search');
    searchInput?.addEventListener('input', (e) => {
      const target = e.target as HTMLInputElement;
      this.filterTree(target.value);
    });

    // Initial Render
    this.renderTree(this.data.assets || []);
  }

  /**
   * Render Empty State
   */
  private renderEmptyState(): string {
    return `
      <div class="explorer-empty">
        <div class="explorer-empty-icon">📄</div>
        <div class="explorer-empty-title">点击左侧方法查看源码</div>
        <div class="explorer-empty-desc">Click a method in the tree to view source code</div>
      </div>
    `;
  }

  /**
   * Render the Source Code Viewer
   */
  private renderCodePanel(code: string, title: string, kind: string): string {
    const lines = code.split('\n');
    const lineNumbers = lines.map((_, i) => `<div class="code-line-num">${i + 1}</div>`).join('');
    
    const codeContent = lines.map(line => {
      let escaped = line.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
      escaped = escaped
        .replace(/\b(public|private|protected|static|final|abstract|synchronized|volatile)\b/g, '<span class="code-keyword">$1</span>')
        .replace(/\b(class|interface|enum|extends|implements|new|this|super|return|if|else|for|while|try|catch|throw)\b/g, '<span class="code-keyword">$1</span>')
        .replace(/\b(String|int|boolean|long|void|List|Map|Set|Collection)\b/g, '<span class="code-type">$1</span>')
        .replace(/(\/\/.*)/g, '<span class="code-comment">$1</span>')
        .replace(/(&quot;[^&]*?&quot;)/g, '<span class="code-string">$1</span>');
      return `<div class="code-line-content">${escaped}</div>`;
    }).join('');

    return `
      <div class="explorer-code-layout">
        <div class="explorer-code-header">
          <div class="explorer-breadcrumb">
            <span class="kind-badge" style="background:${CONFIG.colorMap[kind as keyof typeof CONFIG.colorMap] || '#94a3b8'}20; color:${CONFIG.colorMap[kind as keyof typeof CONFIG.colorMap] || '#94a3b8'}; border:1px solid ${CONFIG.colorMap[kind as keyof typeof CONFIG.colorMap] || '#94a3b8'}40">${kind}</span>
            <span class="breadcrumb-text">📄 ${title}</span>
          </div>
          <div class="explorer-code-actions">
            <button class="code-action-btn" onclick="window.__copyCode()" title="复制代码">📋 复制</button>
          </div>
        </div>
        <div class="explorer-code-body">
          <div class="code-line-numbers">${lineNumbers}</div>
          <div class="code-lines">${codeContent}</div>
        </div>
      </div>
    `;
  }

  /**
   * Render the Color-Coded Tree (sorted by kind: Interface → Abstract → Class → Enum → Utility)
   */
  private renderTree(assets: Asset[]): void {
    const content = document.getElementById('tree-content');
    if (!content) return;

    // Group by Package
    const packages: Record<string, Asset[]> = {};
    assets.forEach(a => {
      const parts = a.address.split('.');
      parts.pop(); 
      const pkg = parts.join('.') || 'default';
      if (!packages[pkg]) packages[pkg] = [];
      packages[pkg].push(a);
    });

    // Kind sort order
    const kindOrder: Record<string, number> = {
      'INTERFACE': 0,
      'ABSTRACT_CLASS': 1,
      'CLASS': 2,
      'ENUM': 3,
      'UTILITY': 4
    };

    let html = '';
    Object.entries(packages).sort((a, b) => a[0].localeCompare(b[0])).forEach(([pkg, items]) => {
      // Sort items by kind order
      items.sort((a, b) => {
        const orderA = kindOrder[a.kind || 'CLASS'] ?? 99;
        const orderB = kindOrder[b.kind || 'CLASS'] ?? 99;
        return orderA - orderB;
      });

      html += `
        <details class="pkg-group" open>
          <summary class="pkg-summary">
            <span class="pkg-icon">📂</span>
            <span class="pkg-name">${pkg.split('.').pop()}</span>
            <span class="pkg-count">${items.length} 类</span>
          </summary>
          <div class="pkg-content">
            ${items.map(asset => this.renderClassNode(asset)).join('')}
          </div>
        </details>
      `;
    });

    content.innerHTML = html;
  }

  /**
   * Render Class Node with proper color coding (no left border)
   */
  private renderClassNode(asset: Asset): string {
    const name = asset.address.split('.').pop();
    const methods = asset.methods_full || asset.methods || [];
    const kind = asset.kind || 'CLASS';
    const color = CONFIG.colorMap[kind as keyof typeof CONFIG.colorMap] || '#94a3b8';

    // Icon mapping per user rules: 🔵接口 🟣抽象类 🟢实现类 🟠枚举 ⚪️普通类
    const kindIcon = kind === 'INTERFACE' ? '🔵' : 
                     kind === 'ABSTRACT_CLASS' ? '🟣' :
                     kind === 'ENUM' ? '🟠' :
                     kind === 'CLASS' ? '🟢' : '⚪️';

    return `
      <details class="class-group">
        <summary class="class-summary">
          <span class="class-icon">${kindIcon}</span>
          <span class="class-name">${name}</span>
          <span class="class-tags">
            <span class="tag-kind" style="background:${color}15; color:${color}">${kind}</span>
          </span>
        </summary>
        <div class="class-content">
          ${methods.length > 0 ? methods.map(m => this.renderMethodNode(m, asset.address)).join('') : '<div class="no-methods">No methods</div>'}
        </div>
      </details>
    `;
  }

  /**
   * Render Method Node (Leaf of the tree, triggers code view)
   */
  private renderMethodNode(method: any, classAddress: string): string {
    const methodKey = `${classAddress}#${method.name}`;
    const isPublic = method.modifiers?.includes('public');
    const isStatic = method.modifiers?.includes('static');
    
    let modBadges = '';
    if (isPublic) modBadges += '<span class="mod-badge mod-public">pub</span>';
    if (isStatic) modBadges += '<span class="mod-badge mod-static">st</span>';

    return `
      <div class="method-item" 
           data-key="${methodKey}" 
           data-class="${classAddress}" 
           data-method="${method.name}">
        <span class="method-name">${method.name}</span>
        ${modBadges}
      </div>
    `;
  }

  /**
   * Filter Tree based on search term
   */
  public filterTree(term: string): void {
    const items = document.querySelectorAll('.method-item');
    const groups = document.querySelectorAll('.pkg-group, .class-group');
    
    const lowerTerm = term.toLowerCase();

    if (!term) {
      items.forEach(el => (el as HTMLElement).style.display = 'flex');
      groups.forEach(el => (el as HTMLDetailsElement).open = true);
      return;
    }

    items.forEach(el => {
      const text = el.textContent?.toLowerCase() || '';
      const match = text.includes(lowerTerm);
      (el as HTMLElement).style.display = match ? 'flex' : 'none';
    });

    groups.forEach(el => (el as HTMLDetailsElement).open = true);
  }

  /**
   * Initialize Event Listeners for Method Clicks
   */
  public bindMethodClicks(): void {
    const container = document.getElementById(this.containerId);
    if (!container) return;

    container.onclick = (e: Event) => {
      const target = (e.target as HTMLElement).closest('.method-item');
      if (target) {
        const classAddr = target.getAttribute('data-class');
        const methodName = target.getAttribute('data-method');
        if (classAddr && methodName) {
          this.openMethod(classAddr, methodName);
        }
      }
    };
  }

  /**
   * View Code for a specific Method
   */
  private openMethod(classAddr: string, methodName: string): void {
    if (!this.data) return;

    const methodKey = `${classAddr}#${methodName}`;

    // 1. Check Cache (LRU)
    let code = this.sourceCodeCache.get(methodKey);

    if (code) {
      this.renderCode(code, classAddr, methodName);
      return;
    }

    // 2. Fetch if missing
    const asset = this.data.assets?.find((a: any) => a.address === classAddr);
    if (!asset) return;

    const methods = asset.methods_full || asset.methods || [];
    const method = methods.find((m: any) => m.name === methodName);
    if (!method) return;

    code = method.source_code || method.body_code || `// No source code available for ${methodName}`;
    
    // 3. Save to Cache
    this.sourceCodeCache.set(methodKey, code);

    this.renderCode(code, classAddr, methodName);
  }

  private renderCode(code: string, classAddr: string, methodName: string): void {
    const asset = this.data?.assets?.find((a: any) => a.address === classAddr);
    const kind = asset?.kind || 'CLASS';
    const title = `${classAddr.split('.').pop()}.${methodName}()`;

    this.selectedCode = {
      content: code,
      language: 'java',
      title,
      kind
    };

    const codePanel = document.getElementById('code-panel');
    if (codePanel) {
      codePanel.innerHTML = this.renderCodePanel(code, title, kind);
      codePanel.scrollTo(0, 0);
    }
  }
}

// Global helper for copy button
(window as any).__copyCode = () => {
  if (navigator.clipboard && document.querySelector('.code-lines')) {
    const text = document.querySelector('.code-lines')!.textContent || '';
    navigator.clipboard.writeText(text);
  }
};
