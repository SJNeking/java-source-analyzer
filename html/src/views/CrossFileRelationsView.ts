/**
 * Java Source Analyzer - Cross-File Relations View (Refactored)
 *
 * Uses Component base class for DOM creation.
 * Uses event delegation instead of inline onclick.
 * Uses centralized constants.
 */

import type { AnalysisResult } from '../types';
import { Component, type Child } from '../framework/component';
import { EventDelegator } from '../framework/events';
import { Logger } from '../utils/logger';
import { COLORS, TEXT, LAYOUT } from '../constants';
import { Style } from '../utils/style-helpers';

interface CrossFileRelation {
  source_path?: string;
  source_asset?: string;
  target_asset?: string;
  relation_type?: string;
  confidence?: number;
  evidence?: string;
}

export class CrossFileRelationsView extends Component {
  private containerId: string;
  private analysisData: AnalysisResult | null = null;
  private activeFilter: string | null = null;
  private allRelations: CrossFileRelation[] = [];
  private delegator: EventDelegator | null = null;

  constructor(containerId: string = 'relations-content') {
    super();
    this.containerId = containerId;
  }

  public render(data: AnalysisResult): void {
    this.analysisData = data;
    const container = document.getElementById(this.containerId);
    if (!container) {
      Logger.error(`Container not found: ${this.containerId}`);
      return;
    }

    const crossFileRelations = (data as any).cross_file_relations;
    if (!crossFileRelations || !crossFileRelations.total_relations || crossFileRelations.total_relations === 0) {
      this.mount(container);
      return;
    }

    // Flatten all relations
    const relationsByType = crossFileRelations.relations_by_type || {};
    const relationTypes = Object.keys(relationsByType);
    this.allRelations = [];
    Object.entries(relationsByType).forEach(([type, relations]) => {
      if (Array.isArray(relations)) {
        relations.forEach((rel: CrossFileRelation) => {
          this.allRelations.push({ ...rel, relation_type: rel.relation_type || type });
        });
      }
    });

    this.mount(container);

    // Set up event delegation after mount
    this.setupFilterListeners(relationTypes);
  }

  public buildRoot(): HTMLElement {
    const crossFileRelations = (this.analysisData as any)?.cross_file_relations;

    if (!crossFileRelations || !crossFileRelations.total_relations) {
      return this.renderEmptyState();
    }

    const relationTypes = Object.keys(crossFileRelations.relations_by_type || {});
    const totalRelations = crossFileRelations.total_relations;

    const filteredRelations = this.activeFilter
      ? this.allRelations.filter(r => r.relation_type === this.activeFilter)
      : this.allRelations;

    return this.el('div', null, [
      this.renderHeader(totalRelations, relationTypes),
      this.renderFilters(relationTypes),
      this.renderRelationsListContent(filteredRelations),
    ]);
  }

  private renderEmptyState(): HTMLElement {
    return this.el('div', { className: 'empty-state' }, [
      this.el('div', { className: 'empty-state-icon' }, [this.text('🔀')]),
      this.el('div', { className: 'empty-state-title' }, [this.text(TEXT.RELATIONS_EMPTY_TITLE)]),
      this.el('div', { className: 'empty-state-desc' }, [this.text(TEXT.RELATIONS_EMPTY_DESC)]),
    ]);
  }

  private renderHeader(totalRelations: number, relationTypes: string[]): HTMLElement {
    const cards: Child[] = [
      this.el('div', { className: 'stat-card' }, [
        this.el('div', { className: 'stat-card-icon' }, [this.text('🔀')]),
        this.el('div', { className: 'stat-card-value' }, [this.text(totalRelations)]),
        this.el('div', { className: 'stat-card-label' }, [this.text(TEXT.RELATIONS_TOTAL)]),
      ]),
      this.el('div', { className: 'stat-card' }, [
        this.el('div', { className: 'stat-card-icon' }, [this.text('📊')]),
        this.el('div', { className: 'stat-card-value' }, [this.text(relationTypes.length)]),
        this.el('div', { className: 'stat-card-label' }, [this.text(TEXT.RELATIONS_TYPE_COUNT)]),
      ]),
    ];

    // Add type cards
    relationTypes.slice(0, LAYOUT.MAX_IMPORT_DEPS).forEach(type => {
      cards.push(
        this.el('div', { className: 'stat-card' }, [
          this.el('div', { className: 'stat-card-icon' }, [this.text('🔗')]),
          this.el('div', { className: 'stat-card-value', style: { fontSize: LAYOUT.FONT_SIZE_14 } as Partial<CSSStyleDeclaration> }, [this.text(type)]),
          this.el('div', { className: 'stat-card-label' }, [this.text(TEXT.RELATIONS_TYPE_LABEL)]),
        ])
      );
    });

    return this.el('div', { className: 'card-grid' }, cards);
  }

  private renderFilters(relationTypes: string[]): HTMLElement {
    const buttons: Child[] = [
      this.el('button', {
        className: `quality-filter-btn${!this.activeFilter ? ' active' : ''}`,
        'data-filter': 'all',
      }, [this.text(TEXT.RELATIONS_FILTER_ALL.replace('{count}', String(relationTypes.length)))]),
    ];

    relationTypes.forEach(type => {
      buttons.push(
        this.el('button', {
          className: 'quality-filter-btn',
          'data-filter': type,
          'data-type': type,
        }, [this.text(type)])
      );
    });

    return this.el('div', { className: 'quality-filters' }, buttons);
  }

  private renderRelationsListContent(relations: CrossFileRelation[]): HTMLElement {
    if (relations.length === 0) {
      return this.el('div', { className: 'empty-state', style: { padding: '40px 20px' } as Partial<CSSStyleDeclaration> }, [
        this.el('div', { className: 'empty-state-icon' }, [this.text('🎉')]),
        this.el('div', { className: 'empty-state-title' }, [this.text(TEXT.RELATIONS_FILTERED_EMPTY_TITLE)]),
        this.el('div', { className: 'empty-state-desc' }, [this.text(TEXT.RELATIONS_FILTERED_EMPTY_DESC)]),
      ]);
    }

    const headerChildren: Child[] = [
      this.el('span', null, [this.text('📋')]),
      this.el('span', null, [this.text(TEXT.RELATIONS_LIST_HEADER.replace('{count}', String(relations.length)))]),
    ];

    const rows = relations.slice(0, LAYOUT.MAX_RELATIONS_DISPLAY).map(rel => this.renderRelationRow(rel));

    const tableBody = this.el('tbody', null, rows);
    const tableHead = this.el('thead', null, [
      this.el('tr', null, [
        this.el('th', null, [this.text(TEXT.RELATIONS_COL_SOURCE_FILE)]),
        this.el('th', null, [this.text(TEXT.RELATIONS_COL_SOURCE_ASSET)]),
        this.el('th', null, [this.text(TEXT.RELATIONS_COL_TARGET_ASSET)]),
        this.el('th', null, [this.text(TEXT.RELATIONS_COL_TYPE)]),
        this.el('th', null, [this.text(TEXT.RELATIONS_COL_CONFIDENCE)]),
        this.el('th', null, [this.text(TEXT.RELATIONS_COL_EVIDENCE)]),
      ]),
    ]);

    const table = this.el('table', { className: 'relations-table' }, [tableHead, tableBody]);

    const children: Child[] = [
      this.el('div', { className: 'section-header' }, headerChildren),
      table,
    ];

    if (relations.length > LAYOUT.MAX_RELATIONS_DISPLAY) {
      children.push(
        this.el('div', {
          style: { textAlign: 'center', padding: '20px', color: COLORS.SLATE_400, fontSize: LAYOUT.FONT_SIZE_13 } as Partial<CSSStyleDeclaration>,
        }, [this.text(TEXT.RELATIONS_SHOWING.replace('{total}', String(relations.length)))])
      );
    }

    return this.el('div', null, children);
  }

  private renderRelationRow(rel: CrossFileRelation): Child {
    const confidence = rel.confidence || 0;
    const confidencePercent = confidence * 100;

    const confidenceBar = this.el('div', { className: 'confidence-bar' }, [
      this.el('div', {
        className: 'confidence-bar-fill',
        style: { width: `${confidencePercent}%` } as Partial<CSSStyleDeclaration>,
      }),
    ]);

    return this.el('tr', null, [
      this.el('td', { style: { fontFamily: "'Courier New', monospace", fontSize: '12px' } as Partial<CSSStyleDeclaration> },
        [this.text(this.truncatePath(rel.source_path || '', 30))]),
      this.el('td', { style: { fontFamily: "'Courier New', monospace", fontSize: '12px' } as Partial<CSSStyleDeclaration> },
        [this.text(this.truncateText(rel.source_asset || '', 25))]),
      this.el('td', { style: { fontFamily: "'Courier New', monospace", fontSize: '12px' } as Partial<CSSStyleDeclaration> },
        [this.text(this.truncateText(rel.target_asset || '', 25))]),
      this.el('td', null, [
        this.el('span', { className: 'relation-type-badge' }, [this.text(rel.relation_type || 'Unknown')]),
      ]),
      this.el('td', null, [
        this.el('div', { style: { display: 'flex', alignItems: 'center', gap: '8px' } as Partial<CSSStyleDeclaration> }, [
          confidenceBar,
          this.text(`${confidencePercent.toFixed(0)}%`),
        ]),
      ]),
      this.el('td', { style: { fontSize: "12px", color: Style.grayLt, maxWidth: '200px' } as Partial<CSSStyleDeclaration> },
        [this.text(this.truncateText(rel.evidence || '', 40))]),
    ]);
  }

  private setupFilterListeners(relationTypes: string[]): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = new EventDelegator(this.root);

    this.delegator.on('click', '.quality-filter-btn', (_e, el) => {
      const filter = el.getAttribute('data-filter');
      const type = el.getAttribute('data-type');
      this.activeFilter = filter === 'all' ? null : (type || filter);

      // Update active state on buttons
      this.findAll('.quality-filter-btn').forEach(btn => btn.classList.remove('active'));
      el.classList.add('active');

      this.update();
    });
  }

  private truncateText(text: string, maxLength: number): string {
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  }

  private truncatePath(path: string, maxLength: number): string {
    if (path.length <= maxLength) return path;
    const parts = path.split('/');
    if (parts.length > 2) {
      return '.../' + parts.slice(-2).join('/');
    }
    return '...' + path.substring(path.length - maxLength + 3);
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
