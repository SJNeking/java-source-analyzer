/**
 * Java Source Analyzer - Code Explorer View
 * 
 * Inspired by 'claude-code-analysis' project design.
 * Features:
 * 1. Left: Call Chain Tree (Package -> Class -> Method)
 * 2. Right: Source Code Viewer with Line Numbers and Syntax Highlighting.
 */

import type { AnalysisResult, Asset } from '../types';
import { Logger } from '../utils/logger';
import { LRUCache } from '../utils/lru-cache';

export class CodeExplorerView {
  private containerId: string;
  private data: AnalysisResult | null = null;
  
  // State
  private selectedCode: { content: string; language: string; title: string } | null = null;

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

    // Create Layout
    container.innerHTML = `
      <div style="display: flex; height: 100%; background: var(--bg, #0F172A);">
        <!-- Left: Tree Panel -->
        <div id="tree-panel" style="width: 320px; border-right: 1px solid var(--border, #334155); overflow-y: auto; background: var(--card, #1E293B); flex-shrink: 0;">
          <div style="padding: 12px; border-bottom: 1px solid var(--border, #334155);">
            <input type="text" id="code-explorer-search" placeholder="🔍 搜索类或方法..." 
                   style="width: 100%; background: var(--bg, #0F172A); border: 1px solid var(--border, #334155); color: var(--text, #F8FAFC); padding: 6px 10px; border-radius: 4px; font-size: 12px; outline: none;">
          </div>
          <div id="tree-content" style="padding: 10px;"></div>
        </div>
        
        <!-- Right: Code Panel -->
        <div id="code-panel" style="flex: 1; overflow: auto; position: relative;">
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
      <div style="height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; color: var(--text-dim, #94A3B8);">
        <div style="font-size: 48px; opacity: 0.3; margin-bottom: 16px;">📄</div>
        <div style="font-size: 14px;">点击左侧方法查看源码</div>
        <div style="font-size: 12px; margin-top: 8px; opacity: 0.6;">Click a method in the tree to view source code</div>
      </div>
    `;
  }

  /**
   * Render the Source Code Viewer
   */
  private renderCodePanel(code: string, title: string): string {
    const lines = code.split('\n');
    const lineNumbers = lines.map((_, i) => `<div style="color: var(--text-dim, #64748b); text-align: right; padding-right: 12px; user-select: none; font-size: 11px;">${i + 1}</div>`).join('');
    
    const codeContent = lines.map(line => {
      let escaped = line.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
      // Simple Syntax Highlighting
      escaped = escaped
        .replace(/\b(public|private|protected|static|final|abstract|synchronized|volatile)\b/g, '<span style="color:#c084fc;font-weight:bold;">$1</span>')
        .replace(/\b(class|interface|enum|extends|implements|new|this|super|return|if|else|for|while|try|catch|throw)\b/g, '<span style="color:#c084fc;font-weight:bold;">$1</span>')
        .replace(/\b(String|int|boolean|long|void|List|Map|Set|Collection)\b/g, '<span style="color:#34d399;">$1</span>')
        .replace(/(\/\/.*)/g, '<span style="color:#6a9955;">$1</span>')
        .replace(/(&quot;[^&]*?&quot;)/g, '<span style="color:#fb923c;">$1</span>');
      return `<div style="white-space: pre;">${escaped}</div>`;
    }).join('');

    return `
      <div style="display: flex; flex-direction: column; height: 100%;">
        <div style="padding: 10px 16px; background: var(--card, #1E293B); border-bottom: 1px solid var(--border, #334155); display: flex; justify-content: space-between; align-items: center;">
          <span style="font-family: monospace; font-size: 12px; color: var(--text, #F8FAFC); font-weight: 500;">📄 ${title}</span>
          <span style="font-size: 10px; color: var(--text-dim, #94A3B8); background: var(--bg, #0F172A); padding: 2px 6px; border-radius: 4px;">Java</span>
        </div>
        <div style="flex: 1; overflow: auto; padding: 12px; display: flex; font-family: 'Menlo', 'Monaco', monospace; font-size: 11px; line-height: 1.5;">
          <div style="border-right: 1px solid var(--border, #334155); padding-right: 0; margin-right: 12px;">${lineNumbers}</div>
          <div style="color: var(--text, #cbd5e1);">${codeContent}</div>
        </div>
      </div>
    `;
  }

  /**
   * Render the Call Chain Tree
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

    let html = '';
    Object.entries(packages).sort((a, b) => a[0].localeCompare(b[0])).forEach(([pkg, items]) => {
      html += `
        <details style="margin-bottom: 4px; border-radius: 4px; overflow: hidden;" open>
          <summary style="padding: 6px 8px; cursor: pointer; font-size: 11px; color: var(--text-dim, #94A3B8); background: var(--bg, #0F172A); font-family: monospace;">
            📂 ${pkg.split('.').pop()}
          </summary>
          <div style="padding-left: 8px;">
            ${items.map(asset => this.renderClassNode(asset)).join('')}
          </div>
        </details>
      `;
    });

    content.innerHTML = html;
  }

  /**
   * Render Class Node and its Methods
   */
  private renderClassNode(asset: Asset): string {
    const name = asset.address.split('.').pop();
    const methods = asset.methods_full || asset.methods || [];

    return `
      <details style="margin-bottom: 2px;" open>
        <summary style="padding: 4px 6px; cursor: pointer; font-size: 12px; color: var(--text, #F8FAFC); display: flex; align-items: center; gap: 6px;">
          <span style="color: ${asset.kind === 'INTERFACE' ? 'var(--blue, #38bdf8)' : 'var(--green, #4ade80)'};">
            ${asset.kind === 'INTERFACE' ? '🔷' : '📄'}
          </span>
          <span>${name}</span>
        </summary>
        <div style="padding-left: 16px; border-left: 1px solid var(--border, #334155); margin-left: 10px;">
          ${methods.length > 0 ? methods.map(m => this.renderMethodNode(m, asset.address)).join('') : '<div style="padding: 4px 6px; font-size: 10px; color: var(--text-dim, #64748b);">No methods</div>'}
        </div>
      </details>
    `;
  }

  /**
   * Render Method Node (Leaf of the tree, triggers code view)
   */
  private renderMethodNode(method: any, classAddress: string): string {
    const methodKey = `${classAddress}#${method.name}`;
    // Use data attributes to store info, event listener will be added in draw()
    return `
      <div class="code-explorer-method" 
           data-key="${methodKey}" 
           data-class="${classAddress}" 
           data-method="${method.name}"
           style="padding: 3px 6px; cursor: pointer; font-size: 11px; color: var(--text-dim, #94A3B8); border-radius: 3px; display: flex; align-items: center; gap: 4px; font-family: monospace;">
        <span>⚙️</span> ${method.name}
      </div>
    `;
  }

  /**
   * Filter Tree based on search term
   */
  public filterTree(term: string): void {
    const items = document.querySelectorAll('.code-explorer-method');
    const details = document.querySelectorAll('#tree-content details');
    
    const lowerTerm = term.toLowerCase();

    if (!term) {
      // Show all
      items.forEach(el => (el.parentElement as HTMLElement).style.display = 'block');
      details.forEach(el => el.open = true);
      return;
    }

    items.forEach(el => {
      const text = el.textContent?.toLowerCase() || '';
      const match = text.includes(lowerTerm);
      (el.parentElement as HTMLElement).style.display = match ? 'block' : 'none';
    });

    // Open all details to show matches
    details.forEach(el => el.open = true);
  }

  /**
   * Initialize Event Listeners for Method Clicks
   * This is called once by App.ts after render
   */
  public bindMethodClicks(): void {
    const container = document.getElementById(this.containerId);
    if (!container) return;

    container.onclick = (e: Event) => {
      const target = (e.target as HTMLElement).closest('.code-explorer-method');
      if (target) {
        const key = target.getAttribute('data-key');
        const classAddr = target.getAttribute('data-class');
        const methodName = target.getAttribute('data-method');
        if (key && classAddr && methodName) {
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

    code = method.source_code || method.body_code || `// No source code available for ${method.name}`;
    
    // 3. Save to Cache
    this.sourceCodeCache.set(methodKey, code);

    this.renderCode(code, classAddr, methodName);
  }

  private renderCode(code: string, classAddr: string, methodName: string): void {
    this.selectedCode = {
      content: code,
      language: 'java',
      title: `${classAddr.split('.').pop()}.${methodName}()`
    };
    const codePanel = document.getElementById('code-panel');
    if (codePanel) {
      codePanel.innerHTML = this.renderCodePanel(code, this.selectedCode.title);
      codePanel.scrollTo(0, 0);
    }
  }
}
