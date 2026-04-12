/**
 * Java Source Analyzer - Quality Dashboard View (Refactored)
 *
 * Uses Component base class for DOM creation instead of innerHTML.
 * Uses event delegation instead of inline onclick.
 * Uses centralized constants for icons, labels, and CSS classes.
 */

import type { AnalysisResult, Asset } from '../types';
import { Component, type Child } from '../framework/component';
import { EventDelegator } from '../framework/events';
import { Logger } from '../utils/logger';
import { LRUCache } from '../utils/lru-cache';
import { setSafeHTML } from '../framework/html';
import { SEVERITY, SEVERITY_ORDER } from '../config';
import { CLS, ICON, LABEL } from '../constants';

interface QualityIssue {
  class: string;
  method?: string;
  line: number;
  severity: 'CRITICAL' | 'MAJOR' | 'MINOR' | 'INFO';
  message: string;
  description?: string;
  rule_name?: string;
}

interface MethodData {
  name: string;
  address?: string;
  source_code?: string;
  body_code?: string;
}

export class QualityDashboardView extends Component {
  private containerId: string;
  private analysisData: AnalysisResult | null = null;
  private activeFilter: string = 'ALL';
  private expandedKey: string | null = null;
  private codeCache = new LRUCache<string, string>(50);
  private delegator: EventDelegator | null = null;

  constructor(containerId: string = 'quality-content') {
    super();
    this.containerId = containerId;
  }

  public render(data: AnalysisResult): void {
    this.analysisData = data;
    const container = document.getElementById(this.containerId);
    if (!container) return;
    
    // Call parent mount to render into container
    this.mount(container);
  }

  public buildRoot(): HTMLElement {
    const qualityIssues: QualityIssue[] = (this.analysisData as any)?.quality_issues || [];
    const qualitySummary = (this.analysisData as any)?.quality_summary;

    if (!qualitySummary && qualityIssues.length === 0) {
      return this.renderEmptyState();
    }

    const counts: Record<string, number> = { CRITICAL: 0, MAJOR: 0, MINOR: 0, INFO: 0 };
    for (const issue of qualityIssues) {
      const sev = issue.severity || 'MINOR';
      if (counts[sev] !== undefined) counts[sev]++;
    }

    // Stat cards
    const statCards: HTMLElement[] = [];

    // Total card
    statCards.push(
      this.el('div', { className: CLS.STAT_CARD }, [
        this.el('div', { className: CLS.STAT_VALUE, style: { color: 'var(--text)' } as Partial<CSSStyleDeclaration> },
          [this.text(qualitySummary?.total_issues || qualityIssues.length)]),
        this.el('div', { className: CLS.STAT_LABEL }, [this.text(LABEL.QUALITY.TOTAL_ISSUES)]),
      ])
    );

    // Severity cards
    for (const sev of SEVERITY_ORDER) {
      if (sev === 'ALL') continue;
      const cfg = SEVERITY.get(sev);
      const isActive = sev === this.activeFilter;
      const card = this.el('div', {
        className: CLS.STAT_CARD,
        style: { cursor: 'pointer', borderColor: isActive ? cfg.color : 'var(--border)' } as Partial<CSSStyleDeclaration>,
        'data-severity': sev,
      }, [
        this.el('div', { className: CLS.STAT_VALUE, style: { color: cfg.color } as Partial<CSSStyleDeclaration> },
          [this.text(counts[sev] || 0)]),
        this.el('div', { className: CLS.STAT_LABEL }, [this.text(cfg.label)]),
      ]);
      statCards.push(card);
    }

    const cardGrid = this.el('div', { className: CLS.CARD_GRID }, statCards);
    const listContainer = this.el('div', { className: CLS.LIST, id: 'qi-list-container' });

    const root = this.el('div', null, [cardGrid, listContainer]);

    // Set up event delegation after root is created
    this.setupEventDelegation(root, qualityIssues);

    return root;
  }

  private setupEventDelegation(root: HTMLElement, issues: QualityIssue[]): void {
    // Clean up previous delegator
    if (this.delegator) this.delegator.destroy();
    this.delegator = new EventDelegator(root);

    // Click on severity stat cards
    this.delegator.on('click', '[data-severity]', (_e, el) => {
      const severity = el.getAttribute('data-severity');
      if (severity) this.setFilter(severity);
    });

    // Click on issue items (expand/collapse)
    this.delegator.on('click', `.${CLS.LIST_ITEM}`, (_e, el) => {
      const key = el.getAttribute('data-issue-key');
      if (key) this.toggleCode(key);
    });

    // Keyboard support
    this.delegator.on('keydown', `.${CLS.LIST_ITEM}`, (e, el) => {
      const key = el.getAttribute('data-issue-key');
      if (key && ((e as KeyboardEvent).key === 'Enter' || (e as KeyboardEvent).key === ' ')) {
        this.toggleCode(key);
      }
    });
  }

  private renderEmptyState(): HTMLElement {
    return this.el('div', { className: CLS.EMPTY_STATE }, [
      this.el('div', { className: CLS.EMPTY_ICON }, [this.text(ICON.UI.CHECK)]),
      this.el('div', { className: CLS.EMPTY_TITLE }, [this.text(LABEL.EMPTY.QUALITY.TITLE)]),
      this.el('div', { className: CLS.EMPTY_DESC }, [
        this.el('span', null, [this.text(LABEL.EMPTY.QUALITY.DESC)]),
      ]),
    ]);
  }

  private renderIssueList(issues: QualityIssue[]): HTMLElement {
    const filtered = this.activeFilter === 'ALL'
      ? issues
      : issues.filter(i => (i.severity || 'MINOR') === this.activeFilter);

    if (filtered.length === 0) {
      return this.el('div', { style: { textAlign: 'center', padding: '40px', color: 'var(--text-dim)' } as Partial<CSSStyleDeclaration> },
        [this.text(LABEL.QUALITY.NO_ISSUES_IN_FILTER)]);
    }

    const items = filtered.map((issue, index) => this.renderIssueItem(issue, index));
    return this.el('div', null, items);
  }

  private renderIssueItem(issue: QualityIssue, index: number): HTMLElement {
    const className = issue.class || 'Unknown';
    const method = issue.method || '';
    const line = issue.line || 0;
    const methodKey = `${className}#${method}#${line}`;
    const isExpanded = this.expandedKey === methodKey;
    const cfg = SEVERITY.get(issue.severity || 'MINOR');
    const shortName = className.split('.').pop() || className;

    const headerChildren: Child[] = [
      this.el('div', { style: { display: 'flex', gap: '8px', alignItems: 'center' } as Partial<CSSStyleDeclaration> }, [
        this.el('span', {
          className: CLS.BADGE,
          style: { background: cfg.bg, color: cfg.color, border: `1px solid ${cfg.color}40` } as Partial<CSSStyleDeclaration>,
        }, [this.text(issue.severity)]),
        this.el('span', { className: CLS.CLASS_NAME, title: className }, [this.text(shortName)]),
      ]),
      this.el('span', { className: CLS.EXPAND_ICON, style: { color: 'var(--text-dim)' } as Partial<CSSStyleDeclaration> },
        [this.text(isExpanded ? ICON.EXPAND.OPEN : ICON.EXPAND.CLOSED)]),
    ];

    const children: Child[] = [
      this.el('div', { className: CLS.ITEM_HEADER }, headerChildren),
      this.el('div', { className: CLS.MSG }, [this.text(issue.message || issue.description || '')]),
    ];

    // Meta info
    const metaChildren: Child[] = [];
    if (issue.rule_name) metaChildren.push(this.el('span', { className: CLS.RULE }, [this.text(issue.rule_name)]));
    if (method) metaChildren.push(this.el('span', { className: CLS.METHOD }, [this.text(`${method}()`)]));
    metaChildren.push(this.el('span', { className: CLS.LINE }, [this.text(`L${line}`)]));
    children.push(this.el('div', { className: CLS.META }, metaChildren));

    // Expanded code block
    if (isExpanded) {
      const codeContent = this.getCodeForIssue(issue);
      const highlighted = this.highlightCode(codeContent);
      const codeBlock = this.el('div', { className: CLS.CODE_BLOCK }, [
        this.el('div', { className: CLS.CODE_HEADER }, [
          this.el('span', null, [this.text(`📄 ${method || className} ()`)]),
          this.el('span', {
            style: { background: 'var(--accent-bg)', color: 'var(--accent)', padding: '2px 6px', borderRadius: '4px' } as Partial<CSSStyleDeclaration>,
          }, [this.text(LABEL.EXPLORER.JAVA_LABEL)]),
        ]),
        this.el('pre', { className: CLS.CODE_PRE }, []),
      ]);
      // Set highlighted HTML safely
      const preEl = codeBlock.querySelector(`.${CLS.CODE_PRE}`);
      if (preEl) setSafeHTML(preEl as HTMLElement, highlighted);
      children.push(codeBlock);
    }

    const item = this.el('div', {
      className: `${CLS.LIST_ITEM}${isExpanded ? ` ${CLS.LIST_ITEM_EXPANDED}` : ''}`,
      style: { cursor: 'pointer', borderLeft: `3px solid ${cfg.color}` } as Partial<CSSStyleDeclaration>,
      role: 'button',
      tabIndex: 0,
      'data-issue-key': methodKey,
    }, children);

    return item;
  }

  private getCodeForIssue(issue: QualityIssue): string {
    const className = issue.class;
    const method = issue.method;
    const key = `${className}#${method}`;

    const cached = this.codeCache.get(key);
    if (cached) return cached;

    const assets: Asset[] = (this.analysisData as any)?.assets || [];
    const asset = assets.find((a: Asset) => a.address === className || a.address.endsWith(`.${className}`));

    let code = `// Source not found for ${method || className}`;
    if (asset) {
      const methods: MethodData[] = (asset as any).methods_full || asset.methods || [];
      const m = methods.find((m: MethodData) => m.name === method || (method && m.address?.includes(method)));
      if (m) {
        code = m.source_code || m.body_code || `// No source code for ${method}`;
      }
    }

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
    this.expandedKey = null;
    if (this.analysisData) {
      const container = document.getElementById(this.containerId);
      if (container) this.mount(container);
    }
  }

  public toggleCode(methodKey: string): void {
    this.expandedKey = this.expandedKey === methodKey ? null : methodKey;
    if (this.analysisData) {
      const issues: QualityIssue[] = (this.analysisData as any).quality_issues || [];
      const listContainer = this.find(`#${this.containerId} .${CLS.LIST}`) ||
        document.getElementById('qi-list-container');
      if (listContainer) {
        const listContent = this.renderIssueList(issues);
        listContainer.innerHTML = '';
        listContainer.appendChild(listContent);
      }
    }
  }

  public getContainer(): HTMLElement | null {
    return document.getElementById(this.containerId);
  }

  public cleanup(): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = null;
    this.unmount();
  }
}
