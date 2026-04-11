/**
 * Java Source Analyzer - Quality Dashboard View
 * Stable, accessible, and memory-safe.
 */

import type { AnalysisResult } from '../types';
import { Logger } from '../utils/logger';
import { LRUCache } from '../utils/lru-cache';

const SEVERITY_ORDER = ['ALL', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'] as const;

const SEVERITY_CONFIG: Record<string, { icon: string; label: string; color: string; bg: string }> = {
  ALL: { icon: '📊', label: '全部', color: 'var(--text)', bg: 'var(--bg-tertiary)' },
  CRITICAL: { icon: '🔴', label: '严重', color: 'var(--danger)', bg: 'rgba(251, 113, 133, 0.1)' },
  MAJOR: { icon: '🟠', label: '重要', color: 'var(--warning)', bg: 'rgba(251, 191, 36, 0.1)' },
  MINOR: { icon: '🔵', label: '次要', color: 'var(--accent)', bg: 'rgba(56, 189, 248, 0.1)' },
  INFO: { icon: '🟢', label: '信息', color: 'var(--success)', bg: 'rgba(74, 222, 128, 0.1)' },
};

export class QualityDashboardView {
  private containerId: string;
  private analysisData: AnalysisResult | null = null;
  private activeFilter: string = 'ALL';
  
  // Accordion State
  private expandedKey: string | null = null;
  
  // Cache for source code (LRU to prevent memory leaks - Limit 50 snippets)
  private codeCache = new LRUCache<string, string>(50);

  constructor(containerId: string = 'quality-content') {
    this.containerId = containerId;
  }

  public render(data: AnalysisResult): void {
    this.analysisData = data;
    const container = document.getElementById(this.containerId);
    if (!container) return;

    const qualityIssues = (data as any).quality_issues || [];
    const qualitySummary = (data as any).quality_summary;

    if (!qualitySummary && qualityIssues.length === 0) {
      container.innerHTML = this.renderEmptyState();
      return;
    }

    // Stats
    const counts: Record<string, number> = { CRITICAL: 0, MAJOR: 0, MINOR: 0, INFO: 0 };
    for (const issue of qualityIssues) {
      const sev = issue.severity || 'MINOR';
      if (counts[sev] !== undefined) counts[sev]++;
    }

    container.innerHTML = `
      <div class="qi-card-grid">
        <div class="qi-stat-card">
          <div class="qi-stat-value" style="color:var(--text)">${qualitySummary?.total_issues || qualityIssues.length}</div>
          <div class="qi-stat-label">总问题数</div>
        </div>
        ${SEVERITY_ORDER.filter(s => s !== 'ALL').map(sev => `
          <div class="qi-stat-card" style="cursor:pointer; border-color: ${sev === this.activeFilter ? SEVERITY_CONFIG[sev].color : 'var(--border)'}" onclick="qualityView.setFilter('${sev}')">
            <div class="qi-stat-value" style="color:${SEVERITY_CONFIG[sev].color}">${counts[sev] || 0}</div>
            <div class="qi-stat-label">${SEVERITY_CONFIG[sev].label}</div>
          </div>
        `).join('')}
      </div>
      <div class="qi-list" id="qi-list-container"></div>
    `;

    this.renderList(qualityIssues);
  }

  private renderEmptyState(): string {
    return `<div class="empty-state"><div class="empty-state-icon">✅</div><div class="empty-state-title">暂无质量分析数据</div><div class="empty-state-desc">💡 <strong>建议:</strong> 使用最新版 Java 分析工具重新分析项目。</div></div>`;
  }

  private renderList(issues: any[]): void {
    const container = document.getElementById('qi-list-container');
    if (!container) return;

    const filtered = this.activeFilter === 'ALL' 
      ? issues 
      : issues.filter(i => (i.severity || 'MINOR') === this.activeFilter);

    if (filtered.length === 0) {
      container.innerHTML = `<div style="text-align:center;padding:40px;color:var(--text-dim);">当前筛选条件下无问题</div>`;
      return;
    }

    container.innerHTML = filtered.map((issue, index) => this.renderItem(issue, index)).join('');
  }

  private renderItem(issue: any, index: number): string {
    const className = issue.class || 'Unknown';
    const method = issue.method || '';
    const line = issue.line || 0;
    
    // Unique Key for Accordion Logic
    const methodKey = `${className}#${method}#${line}`;
    const isExpanded = this.expandedKey === methodKey;
    const cfg = SEVERITY_CONFIG[issue.severity || 'MINOR'];

    return `
      <div class="qi-item ${isExpanded ? 'expanded' : ''}" 
           style="cursor: pointer; border-left: 3px solid ${cfg.color}"
           onclick="qualityView.toggleCode('${methodKey.replace(/'/g, "\\'")}')"
           role="button"
           tabindex="0"
           onkeydown="if(event.key==='Enter'||event.key===' ') qualityView.toggleCode('${methodKey.replace(/'/g, "\\'")}')">
        <div class="qi-item-header">
          <div style="display:flex; gap:8px; align-items:center;">
             <span class="qi-badge" style="background:${cfg.bg};color:${cfg.color}; border:1px solid ${cfg.color}40">${issue.severity}</span>
             <span class="qi-class-name" title="${className}">${className.split('.').pop()}</span>
          </div>
          <span class="qi-expand-icon" style="color:var(--text-dim)">${isExpanded ? '▲' : '▼'}</span>
        </div>
        <div class="qi-msg">${issue.message || issue.description || ''}</div>
        <div class="qi-meta">
          ${issue.rule_name ? `<span class="qi-rule">${issue.rule_name}</span>` : ''}
          ${method ? `<span class="qi-method">${method}()</span>` : ''}
          <span class="qi-line">L${line}</span>
        </div>
        
        ${isExpanded ? `
        <div class="qi-code-block" onclick="event.stopPropagation()" role="region" aria-label="Source Code">
          <div class="qi-code-header">
            <span>📄 ${method || className} ()</span>
            <span style="background:var(--accent-bg); color:var(--accent); padding:2px 6px; border-radius:4px;">Java</span>
          </div>
          <pre class="qi-code-content">${this.highlightCode(this.getCodeForIssue(issue))}</pre>
        </div>
        ` : ''}
      </div>
    `;
  }

  private getCodeForIssue(issue: any): string {
    const className = issue.class;
    const method = issue.method;
    const key = `${className}#${method}`;

    // Check LRU Cache
    const cached = this.codeCache.get(key);
    if (cached) return cached;

    // Fetch
    const assets = (this.analysisData as any)?.assets || [];
    const asset = assets.find((a: any) => a.address === className || a.address.endsWith(`.${className}`));

    let code = `// Source not found for ${method || className}`;
    if (asset) {
      const methods = asset.methods_full || asset.methods || [];
      const m = methods.find((m: any) => m.name === method || (method && m.address.includes(method)));
      if (m) {
        code = m.source_code || m.body_code || `// No source code for ${method}`;
      }
    }
    
    // Set LRU Cache
    this.codeCache.set(key, code);
    return code;
  }

  private highlightCode(code: string): string {
    let escaped = code.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    escaped = escaped
      .replace(/\b(public|private|protected|static|final|void|class|return|if|else|for|while|try|catch|throw|new|extends|implements)\b/g, '<span class="code-keyword">$1</span>')
      .replace(/\b(String|int|boolean|long|List|Map|Set|Collection)\b/g, '<span class="code-type">$1</span>')
      .replace(/(\/\/.*)/g, '<span class="code-comment">$1</span>')
      .replace(/(&quot;[^&]*?&quot;)/g, '<span class="code-string">$1</span>');
    return escaped;
  }

  public setFilter(severity: string): void {
    this.activeFilter = severity;
    this.expandedKey = null; // Collapse all
    if (this.analysisData) this.render(this.analysisData);
  }

  public toggleCode(methodKey: string): void {
    this.expandedKey = this.expandedKey === methodKey ? null : methodKey;
    if (this.analysisData) this.renderList((this.analysisData as any).quality_issues || []);
  }

  public getContainer(): HTMLElement | null {
    return document.getElementById(this.containerId);
  }
}
