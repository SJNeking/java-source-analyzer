/**
 * Java Source Analyzer - AI Review View
 *
 * Displays unified review results (static + AI merged).
 * Shows AI suggestions, confidence scores, fixed code diffs.
 * Supports filtering by confidence level and issue source.
 */

import type { AnalysisResult } from '../types';
import type { UnifiedIssue, ReviewSummary, EngineInfo, IssueSource, ConfidenceLevel } from '../types/unified-issue';
import { Component, type Child } from '../framework/component';
import { EventDelegator } from '../framework/events';
import { Logger } from '../utils/logger';
import { CLS, ICON, LABEL } from '../constants';
import { Style } from '../utils/style-helpers';

type SourceFilter = 'all' | 'static' | 'ai' | 'merged';
type ConfidenceFilter = 'all' | 'high' | 'medium' | 'low';

export class AiReviewView extends Component {
  private containerId: string;
  private analysisData: AnalysisResult | null = null;
  private unifiedIssues: UnifiedIssue[] = [];
  private summary: ReviewSummary | null = null;
  private aiEngine: EngineInfo | null = null;

  private sourceFilter: SourceFilter = 'all';
  private confidenceFilter: ConfidenceFilter = 'all';
  private showFiltered = false;

  private expandedIssueId: string | null = null;
  private delegator: EventDelegator | null = null;

  constructor(containerId: string = 'ai-review-content') {
    super();
    this.containerId = containerId;
  }

  public render(data: AnalysisResult): void {
    this.analysisData = data;

    // Extract unified report data (may be from merged output or legacy format)
    const raw = data as any;
    this.unifiedIssues = raw.unified_issues || this.convertLegacyIssues(raw);
    this.summary = raw.summary || this.buildSummary(this.unifiedIssues);
    this.aiEngine = raw.aiEngine || null;

    const container = document.getElementById(this.containerId);
    if (!container) return;
    this.mount(container);
    this.setupEventListeners();
  }

  public buildRoot(): HTMLElement {
    if (this.unifiedIssues.length === 0 && !this.aiEngine) {
      return this.renderEmptyState();
    }

    const children: Child[] = [
      this.renderHeader(),
      this.renderSummaryCards(),
      this.renderFilterBar(),
      this.renderIssueList(),
    ];

    return this.el('div', { className: 'ai-review-container' }, children);
  }

  // ========== Empty State ==========

  private renderEmptyState(): HTMLElement {
    return this.el('div', { className: CLS.EMPTY_STATE }, [
      this.el('div', { className: CLS.EMPTY_ICON }, [this.text('🤖')]),
      this.el('div', { className: CLS.EMPTY_TITLE }, [this.text('暂无 AI 审查数据')]),
      this.el('div', { className: CLS.EMPTY_DESC }, [
        this.text('AI 审查功能需要先运行 CodeGuardian AI 模块。'),
        this.el('br', {}),
        this.text('运行静态分析后，使用 ResultMerger 合并结果即可在此视图查看 AI 建议。'),
      ]),
    ]);
  }

  // ========== Header ==========

  private renderHeader(): HTMLElement {
    const children: Child[] = [
      this.el('h2', { className: 'view-title' }, [
        this.el('span', null, [this.text(ICON.SECTION.QUALITY)]),
        this.text(' AI 审查'),
      ]),
    ];

    if (this.aiEngine) {
      children.push(
        this.el('div', { className: 'ai-engine-badge' }, [
          this.el('span', { className: 'ai-model-tag' }, [
            this.text(`🤖 ${this.aiEngine.modelUsed || this.aiEngine.name}`),
          ]),
        ])
      );
    }

    return this.el('div', { className: 'view-header' }, children);
  }

  // ========== Summary Cards ==========

  private renderSummaryCards(): HTMLElement {
    if (!this.summary) return this.el('div', null, []);

    const s = this.summary;
    const cards = [
      { label: '总问题', value: s.totalIssues, color: Style.slate[300], icon: '📊' },
      { label: '活跃问题', value: s.activeIssues, color: Style.blue, icon: '🔍' },
      { label: '已过滤', value: s.autoFiltered, color: Style.slate[500], icon: '🚫' },
      { label: '静态发现', value: s.staticOnly, color: Style.green, icon: '🔧' },
      { label: 'AI 发现', value: s.aiOnly, color: Style.purple, icon: '🤖' },
      { label: '双重确认', value: s.merged, color: Style.orange, icon: '✅' },
    ];

    const cardEls = cards.map(c =>
      this.el('div', { className: 'ai-stat-card' }, [
        this.el('div', { className: 'ai-stat-icon' }, [this.text(c.icon)]),
        this.el('div', { className: 'ai-stat-value', style: { color: c.color } as Partial<CSSStyleDeclaration> },
          [this.text(c.value)]),
        this.el('div', { className: 'ai-stat-label' }, [this.text(c.label)]),
      ])
    );

    if (s.aiHighConfidenceRate > 0) {
      cardEls.push(
        this.el('div', { className: 'ai-stat-card' }, [
          this.el('div', { className: 'ai-stat-icon' }, [this.text('🎯')]),
          this.el('div', { className: 'ai-stat-value', style: { color: Style.teal } as Partial<CSSStyleDeclaration> },
            [this.text(`${s.aiHighConfidenceRate}%`)]),
          this.el('div', { className: 'ai-stat-label' }, [this.text('AI 高信率')]),
        ])
      );
    }

    return this.el('div', { className: 'ai-stats-grid' }, cardEls);
  }

  // ========== Filter Bar ==========

  private renderFilterBar(): HTMLElement {
    const sourceBtns: Child[] = [
      this.el('button', {
        className: `ai-filter-btn${this.sourceFilter === 'all' ? ' active' : ''}`,
        'data-filter-type': 'source',
        'data-filter-value': 'all',
      }, [this.text(`全部 (${this.summary?.totalIssues || 0})`)]),
      this.el('button', {
        className: `ai-filter-btn${this.sourceFilter === 'static' ? ' active' : ''}`,
        'data-filter-type': 'source',
        'data-filter-value': 'static',
      }, [this.text(`🔧 静态 (${this.summary?.staticOnly || 0})`)]),
      this.el('button', {
        className: `ai-filter-btn${this.sourceFilter === 'ai' ? ' active' : ''}`,
        'data-filter-type': 'source',
        'data-filter-value': 'ai',
      }, [this.text(`🤖 AI (${(this.summary?.aiOnly || 0) + (this.summary?.merged || 0)})`)]),
      this.el('button', {
        className: `ai-filter-btn${this.sourceFilter === 'merged' ? ' active' : ''}`,
        'data-filter-type': 'source',
        'data-filter-value': 'merged',
      }, [this.text(`✅ 双重 (${this.summary?.merged || 0})`)]),
    ];

    const confBtns: Child[] = [
      this.el('button', {
        className: `ai-filter-btn ai-conf-btn${this.confidenceFilter === 'all' ? ' active' : ''}`,
        'data-filter-type': 'confidence',
        'data-filter-value': 'all',
      }, [this.text('全部置信度')]),
      this.el('button', {
        className: `ai-filter-btn ai-conf-btn${this.confidenceFilter === 'high' ? ' active' : ''}`,
        'data-filter-type': 'confidence',
        'data-filter-value': 'high',
      }, [this.text('🎯 高 (≥0.9)')]),
      this.el('button', {
        className: `ai-filter-btn ai-conf-btn${this.confidenceFilter === 'medium' ? ' active' : ''}`,
        'data-filter-type': 'confidence',
        'data-filter-value': 'medium',
      }, [this.text('🔶 中 (≥0.7)')]),
      this.el('button', {
        className: `ai-filter-btn ai-conf-btn${this.confidenceFilter === 'low' ? ' active' : ''}`,
        'data-filter-type': 'confidence',
        'data-filter-value': 'low',
      }, [this.text('🔻 低 (<0.7)')]),
    ];

    const toggleFiltered = this.el('label', { className: 'ai-toggle-filtered' }, [
      this.el('input', { type: 'checkbox', id: 'show-filtered', checked: this.showFiltered }),
      this.text('显示已过滤'),
    ]);

    return this.el('div', { className: 'ai-filter-bar' }, [
      this.el('div', { className: 'ai-filter-group' }, sourceBtns),
      this.el('div', { className: 'ai-filter-group' }, confBtns),
      toggleFiltered,
    ]);
  }

  // ========== Issue List ==========

  private renderIssueList(): HTMLElement {
    const filtered = this.getFilteredIssues();

    if (filtered.length === 0) {
      return this.el('div', { className: CLS.EMPTY_CENTER }, [
        this.el('div', { className: CLS.EMPTY_ICON }, [this.text('✨')]),
        this.el('div', { className: CLS.EMPTY_TITLE }, [this.text('当前筛选条件下无问题')]),
      ]);
    }

    const items = filtered.map(issue => this.renderIssueItem(issue));
    return this.el('div', { className: 'ai-issue-list' }, items);
  }

  private renderIssueItem(issue: UnifiedIssue): HTMLElement {
    const isExpanded = this.expandedIssueId === issue.id;
    const isFiltered = issue.autoFiltered === true;

    // Severity color
    const sevColors: Record<string, string> = {
      CRITICAL: Style.red, MAJOR: Style.orange, MINOR: Style.blue, INFO: Style.teal,
    };
    const sevColor = sevColors[issue.severity] || Style.slate[400];

    // Source badge
    const sourceLabels: Record<string, string> = {
      static: '🔧 静态', ai: '🤖 AI', merged: '✅ 合并',
    };
    const sourceLabel = sourceLabels[issue.source] || issue.source;

    const children: Child[] = [
      // Header row
      this.el('div', { className: 'ai-issue-header' }, [
        this.el('div', { className: 'ai-issue-meta' }, [
          this.el('span', { className: 'ai-severity-dot', style: { background: sevColor } as Partial<CSSStyleDeclaration> }),
          this.el('span', { className: 'ai-source-badge' }, [this.text(sourceLabel)]),
          this.el('span', { className: 'ai-severity-text', style: { color: sevColor } as Partial<CSSStyleDeclaration> },
            [this.text(issue.severity)]),
        ]),
        this.el('span', { className: 'ai-expand-icon' }, [
          this.text(isExpanded ? ICON.EXPAND.OPEN : ICON.EXPAND.CLOSED),
        ]),
      ]),

      // Message
      this.el('div', { className: 'ai-issue-message' }, [this.text(issue.message)]),

      // File info
      this.el('div', { className: 'ai-issue-file' }, [
        this.el('span', { className: 'ai-file-path' }, [this.text(this.shortenPath(issue.filePath))]),
        this.el('span', { className: 'ai-line-num' }, [this.text(`L${issue.line}`)]),
      ]),
    ];

    // Expanded content
    if (isExpanded) {
      const expandedContent: Child[] = [];

      // AI suggestion
      if (issue.aiSuggestion) {
        expandedContent.push(
          this.el('div', { className: 'ai-suggestion-block' }, [
            this.el('div', { className: 'ai-suggestion-header' }, [
              this.text('🤖 AI 修复建议'),
              issue.confidence != null
                ? this.el('span', { className: 'ai-confidence-badge', style: this.confidenceBadgeStyle(issue.confidence) } as Partial<CSSStyleDeclaration>,
                    [this.text(`${(issue.confidence * 100).toFixed(0)}%`)] )
                : null,
            ]),
            this.el('div', { className: 'ai-suggestion-text' }, [this.text(issue.aiSuggestion)]),
          ])
        );
      }

      // AI reasoning
      if (issue.aiReasoning) {
        expandedContent.push(
          this.el('div', { className: 'ai-reasoning-block' }, [
            this.el('div', { className: 'ai-reasoning-header' }, [this.text('💡 推理过程')]),
            this.el('div', { className: 'ai-reasoning-text' }, [this.text(issue.aiReasoning)]),
          ])
        );
      }

      // AI fixed code
      if (issue.aiFixedCode) {
        expandedContent.push(
          this.el('div', { className: 'ai-code-block' }, [
            this.el('div', { className: 'ai-code-header' }, [
              this.text('📝 修复代码'),
              this.el('button', { className: 'ai-copy-code-btn', 'data-copy-target': `code-${issue.id}` },
                [this.text('复制')]),
            ]),
            this.el('pre', { id: `code-${issue.id}`, className: 'ai-code-content' },
              [this.text(issue.aiFixedCode)]),
          ])
        );
      }

      // Confidence bar
      if (issue.confidence != null) {
        expandedContent.push(
          this.el('div', { className: 'ai-confidence-bar-wrap' }, [
            this.el('div', { className: 'ai-confidence-label' }, [
              this.text(`置信度: ${(issue.confidence * 100).toFixed(0)}% (${issue.confidenceLevel || 'N/A'})`),
            ]),
            this.el('div', { className: 'ai-confidence-bar-track' }, [
              this.el('div', {
                className: 'ai-confidence-bar-fill',
                style: {
                  width: `${issue.confidence * 100}%`,
                  background: this.confidenceBarColor(issue.confidence),
                } as Partial<CSSStyleDeclaration>,
              }),
            ]),
          ])
        );
      }

      // Static analysis metadata
      if (issue.cyclomaticComplexity != null || issue.loc != null) {
        const metaItems: Child[] = [];
        if (issue.cyclomaticComplexity != null) metaItems.push(this.text(`圈复杂度: ${issue.cyclomaticComplexity}`));
        if (issue.loc != null) metaItems.push(this.text(`行数: ${issue.loc}`));
        if (issue.relatedAssets?.length) metaItems.push(this.text(`关联: ${issue.relatedAssets.length} 个`));
        expandedContent.push(
          this.el('div', { className: 'ai-static-meta' }, metaItems)
        );
      }

      children.push(this.el('div', { className: 'ai-issue-expanded' }, expandedContent));
    }

    return this.el('div', {
      className: `ai-issue-item${isExpanded ? ' expanded' : ''}${isFiltered ? ' filtered' : ''}`,
      'data-issue-id': issue.id,
    }, children);
  }

  // ========== Helpers ==========

  private getFilteredIssues(): UnifiedIssue[] {
    return this.unifiedIssues.filter(issue => {
      if (!this.showFiltered && issue.autoFiltered) return false;
      if (this.sourceFilter !== 'all' && issue.source !== this.sourceFilter) return false;
      if (this.confidenceFilter !== 'all' && issue.confidence != null) {
        if (this.confidenceFilter === 'high' && issue.confidence < 0.9) return false;
        if (this.confidenceFilter === 'medium' && (issue.confidence < 0.7 || issue.confidence >= 0.9)) return false;
        if (this.confidenceFilter === 'low' && issue.confidence >= 0.7) return false;
      }
      return true;
    });
  }

  private shortenPath(path: string): string {
    const parts = path.split('/');
    if (parts.length > 3) return '.../' + parts.slice(-3).join('/');
    return path;
  }

  private confidenceBadgeStyle(score: number): Partial<CSSStyleDeclaration> {
    const c = this.confidenceBarColor(score);
    return { background: c + '22', color: c, border: `1px solid ${c}44` };
  }

  private confidenceBarColor(score: number): string {
    if (score >= 0.9) return Style.teal;
    if (score >= 0.7) return Style.orange;
    return Style.red;
  }

  // ========== Legacy conversion ==========

  private convertLegacyIssues(raw: any): UnifiedIssue[] {
    const issues: UnifiedIssue[] = [];
    const qualityIssues = raw.quality_issues || [];
    for (const qi of qualityIssues) {
      issues.push({
        id: qi.rule_key + ':' + (qi.file || '') + ':' + qi.line,
        source: 'static',
        sourceLabel: '静态分析',
        ruleKey: qi.rule_key || '',
        ruleName: qi.rule_name || '',
        severity: qi.severity || 'MINOR',
        category: qi.category || 'CODE_SMELL',
        filePath: qi.file || '',
        className: qi.class || '',
        methodName: qi.method || undefined,
        line: qi.line || 0,
        message: qi.message || '',
        confidence: 1.0,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      });
    }
    return issues;
  }

  private buildSummary(issues: UnifiedIssue[]): ReviewSummary {
    const active = issues.filter(i => !i.autoFiltered);
    return {
      totalIssues: issues.length,
      staticOnly: issues.filter(i => i.source === 'static').length,
      aiOnly: issues.filter(i => i.source === 'ai').length,
      merged: issues.filter(i => i.source === 'merged').length,
      autoFiltered: issues.filter(i => i.autoFiltered).length,
      activeIssues: active.length,
      critical: active.filter(i => i.severity === 'CRITICAL').length,
      major: active.filter(i => i.severity === 'MAJOR').length,
      minor: active.filter(i => i.severity === 'MINOR').length,
      info: active.filter(i => i.severity === 'INFO').length,
      aiAvgConfidence: 0,
      aiHighConfidenceRate: 0,
      byCategory: {},
    };
  }

  // ========== Event Listeners ==========

  private setupEventListeners(): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = new EventDelegator(this.root);
    if (!this.delegator) return;

    // Source filter buttons
    this.delegator.on('click', '[data-filter-type="source"]', (_e, el) => {
      this.sourceFilter = el.getAttribute('data-filter-value') as SourceFilter;
      this.update();
      this.setupEventListeners();
    });

    // Confidence filter buttons
    this.delegator.on('click', '[data-filter-type="confidence"]', (_e, el) => {
      this.confidenceFilter = el.getAttribute('data-filter-value') as ConfidenceFilter;
      this.update();
      this.setupEventListeners();
    });

    // Show filtered checkbox
    this.delegator.on('change', '#show-filtered', (_e, el) => {
      this.showFiltered = (el as HTMLInputElement).checked;
      this.update();
      this.setupEventListeners();
    });

    // Issue expand/collapse
    this.delegator.on('click', '.ai-issue-item', (_e, el) => {
      const issueId = el.getAttribute('data-issue-id');
      if (issueId) {
        this.expandedIssueId = this.expandedIssueId === issueId ? null : issueId;
        this.update();
        this.setupEventListeners();
      }
    });

    // Copy code button
    this.delegator.on('click', '.ai-copy-code-btn', (_e, el) => {
      const targetId = el.getAttribute('data-copy-target');
      if (targetId) {
        const codeEl = document.getElementById(targetId);
        if (codeEl && navigator.clipboard) {
          navigator.clipboard.writeText(codeEl.textContent || '');
          el.textContent = '已复制';
          setTimeout(() => { el.textContent = '复制'; }, 2000);
        }
      }
    });
  }

  public cleanup(): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = null;
    this.unmount();
  }
}
