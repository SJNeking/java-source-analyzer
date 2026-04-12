/**
 * Frontend Quality Analyzer - Quality Dashboard View (Refactored)
 *
 * Uses Component base class for DOM creation.
 * Uses event delegation instead of bound click handlers.
 * Uses centralized constants for icons and labels.
 */

import type {
  FrontendAnalysisResult,
  FrontendFileAnalysis,
  FrontendQualityIssue,
  QualityGate,
  TechnicalDebt,
  AdvancedAnalysis
} from '../types/frontend-quality';
import { Component, type Child } from '../framework/component';
import { EventDelegator } from '../framework/events';
import { ICON } from '../constants';

const SEVERITY_ORDER = ['ALL', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'] as const;

const SEVERITY_CONFIG: Record<string, { icon: string; label: string }> = {
  ALL: { icon: '📊', label: '全部' },
  CRITICAL: { icon: '🔴', label: '严重' },
  MAJOR: { icon: '🟠', label: '重要' },
  MINOR: { icon: '🔵', label: '次要' },
  INFO: { icon: '🟢', label: '信息' },
};

const CATEGORY_CONFIG: Record<string, { icon: string; label: string }> = {
  TYPESCRIPT: { icon: '📘', label: 'TypeScript' },
  REACT: { icon: '⚛️', label: 'React' },
  VUE: { icon: '💚', label: 'Vue' },
  SECURITY: { icon: '🔒', label: '安全' },
  PERFORMANCE: { icon: '⚡', label: '性能' },
  MEMORY: { icon: '🧠', label: '内存' },
  ACCESSIBILITY: { icon: '♿', label: '无障碍' },
  ARCHITECTURE: { icon: '🏗️', label: '架构' },
  STYLING: { icon: '🎨', label: '样式' },
  TESTING: { icon: '🧪', label: '测试' },
  BUILD: { icon: '🔧', label: '构建' },
  I18N: { icon: '🌐', label: '国际化' },
};

export class FrontendQualityView extends Component {
  private containerId: string;
  private analysisData: FrontendAnalysisResult | null = null;
  private activeFilter: string = 'ALL';
  private expandedFile: string | null = null;
  private delegator: EventDelegator | null = null;

  constructor(containerId: string = 'frontend-quality-content') {
    super();
    this.containerId = containerId;
  }

  public render(data: FrontendAnalysisResult): void {
    this.analysisData = data;
    const container = document.getElementById(this.containerId);
    if (!container) return;
    this.mount(container);
    this.setupEventListeners();
  }

  public buildRoot(): HTMLElement {
    if (!this.analysisData) return this.renderEmptyState();

    const files: FrontendFileAnalysis[] = this.analysisData.files || [];
    const allIssues: FrontendQualityIssue[] = [];
    for (const f of files) {
      if (f.issues) allIssues.push(...f.issues);
    }

    if (allIssues.length === 0 && !(this.analysisData as any).advanced_analysis) {
      return this.renderEmptyState();
    }

    const counts: Record<string, number> = { CRITICAL: 0, MAJOR: 0, MINOR: 0, INFO: 0 };
    const categoryCounts: Record<string, number> = {};
    for (const issue of allIssues) {
      counts[issue.severity] = (counts[issue.severity] || 0) + 1;
      categoryCounts[issue.category] = (categoryCounts[issue.category] || 0) + 1;
    }

    const framework = (this.analysisData as any).framework_detected || 'Unknown';
    const totalFiles = (this.analysisData as any).total_files || 0;
    const totalLines = (this.analysisData as any).total_lines || 0;
    const projectName = (this.analysisData as any).project_name || 'Project';
    const qualityGate = (this.analysisData as any).quality_gate as QualityGate | undefined;
    const technicalDebt = (this.analysisData as any).technical_debt as TechnicalDebt | undefined;
    const advancedAnalysis = (this.analysisData as any).advanced_analysis as AdvancedAnalysis | undefined;

    const children: Child[] = [
      this.renderHeader(projectName, framework),
      this.renderStatsGrid(totalFiles, totalLines, counts),
      qualityGate ? this.renderQualityGate(qualityGate) : this.el('div', null, []),
      technicalDebt ? this.renderTechnicalDebt(technicalDebt) : this.el('div', null, []),
      this.renderFilterTabs(counts, allIssues.length),
      this.renderCategoryBreakdown(categoryCounts),
      advancedAnalysis ? this.renderAdvancedAnalysis(advancedAnalysis) : this.el('div', null, []),
      this.renderIssuesByFile(files, allIssues),
    ];

    return this.el('div', { className: 'frontend-quality-dashboard' }, children);
  }

  private renderEmptyState(): HTMLElement {
    return this.el('div', { className: 'empty-state' }, [
      this.el('h3', null, [this.text('🔬 前端质量分析')]),
      this.el('p', null, [this.text('尚未运行前端质量分析。请使用以下命令运行：')]),
      this.el('code', { className: 'empty-command' }, [
        this.text('cd frontend-quality-analyzer && node dist/cli.js --sourceRoot /path/to/project'),
      ]),
    ]);
  }

  private renderHeader(projectName: string, framework: string): HTMLElement {
    return this.el('div', { className: 'view-header' }, [
      this.el('h2', { className: 'view-title' }, [this.text(`🔬 ${projectName}`)]),
      this.el('span', { className: 'framework-badge' }, [this.text(framework)]),
    ]);
  }

  private renderStatsGrid(totalFiles: number, totalLines: number, counts: Record<string, number>): HTMLElement {
    const cards = [
      { value: totalFiles, label: '文件数' },
      { value: totalLines.toLocaleString(), label: '代码行数' },
      { value: counts.CRITICAL, label: '严重', className: 'stat-critical' },
      { value: counts.MAJOR, label: '重要', className: 'stat-major' },
      { value: counts.MINOR, label: '次要', className: 'stat-minor' },
      { value: counts.INFO, label: '信息', className: 'stat-info' },
    ].map(card =>
      this.el('div', { className: `stat-card${card.className ? ` ${card.className}` : ''}` }, [
        this.el('span', { className: 'stat-value' }, [this.text(card.value)]),
        this.el('span', { className: 'stat-label' }, [this.text(card.label)]),
      ])
    );

    return this.el('div', { className: 'stats-grid' }, cards);
  }

  private renderQualityGate(gate: QualityGate): HTMLElement {
    const passed = gate.passed;
    const children: Child[] = [
      this.el('span', { className: 'gate-status' }, [
        this.text(passed ? '✅ 质量门禁通过' : '❌ 质量门禁失败'),
      ]),
    ];

    if (!passed && gate.reasons?.length) {
      const items = gate.reasons.map(r => this.el('li', null, [this.text(r)]));
      children.push(this.el('ul', null, items));
    }

    return this.el('div', { className: `quality-gate ${passed ? 'passed' : 'failed'}` }, children);
  }

  private renderTechnicalDebt(debt: TechnicalDebt): HTMLElement {
    return this.el('div', { className: 'technical-debt' }, [
      this.el('h3', null, [this.text('技术债务')]),
      this.el('div', { className: 'debt-grid' }, [
        this.el('div', { className: 'debt-item' }, [
          this.el('span', { className: 'debt-value' }, [this.text(`${debt.total_remediation_hours}h`)]),
          this.el('span', { className: 'debt-label' }, [this.text('修复工时')]),
        ]),
        this.el('div', { className: 'debt-item' }, [
          this.el('span', { className: 'debt-value' }, [this.text(`${(debt.debt_ratio_percentage?.toFixed(1) || 0)}%`)]),
          this.el('span', { className: 'debt-label' }, [this.text('债务占比')]),
        ]),
      ]),
    ]);
  }

  private renderFilterTabs(counts: Record<string, number>, totalIssues: number): HTMLElement {
    const tabs = SEVERITY_ORDER.map(key => {
      const cfg = SEVERITY_CONFIG[key];
      const count = key === 'ALL' ? totalIssues : (counts[key] || 0);
      const isActive = this.activeFilter === key;

      return this.el('button', {
        className: `filter-tab${isActive ? ' active' : ''}`,
        'data-filter': key,
      }, [this.text(`${cfg.icon} ${cfg.label} ${count}`)]);
    });

    return this.el('div', { className: 'filter-tabs' }, tabs);
  }

  private renderCategoryBreakdown(categoryCounts: Record<string, number>): HTMLElement {
    const sorted = Object.entries(categoryCounts).sort((a, b) => b[1] - a[1]);
    const items = sorted.map(([cat, count]) => {
      const cfg = CATEGORY_CONFIG[cat] || { icon: ICON.SECTION.ASSETS, label: '未知' };
      return this.el('div', { className: 'category-item' }, [
        this.el('span', null, [this.text(`${cfg.icon} ${cfg.label}`)]),
        this.el('span', { className: 'category-count' }, [this.text(count)]),
      ]);
    });

    return this.el('div', { className: 'category-breakdown' }, [
      this.el('h3', null, [this.text('🔬 按类别分布')]),
      this.el('div', { className: 'category-grid' }, items),
    ]);
  }

  private renderAdvancedAnalysis(aa: AdvancedAnalysis): HTMLElement {
    const sections: Child[] = [];

    if ((aa as any).xss_taint_flows?.length > 0) {
      const flows = (aa as any).xss_taint_flows.map((flow: any) =>
        this.el('div', { className: 'taint-flow-item' }, [
          this.el('span', { className: 'taint-source' }, [this.text(`源: ${flow.source?.type || ''}`)]),
          this.el('span', { className: 'taint-path' }, [this.text(`→ ${flow.path_length} 步 →`)]),
          this.el('span', { className: 'taint-sink' }, [this.text(`汇: ${flow.sink?.type || ''}`)]),
        ])
      );
      sections.push(
        this.el('div', { className: 'advanced-section' }, [
          this.el('h3', null, [this.text('XSS 数据流追踪')]),
          this.el('div', { className: 'taint-flows' }, flows),
        ])
      );
    }

    if ((aa as any).component_graph?.total_components > 0) {
      const cg = (aa as any).component_graph;
      sections.push(
        this.el('div', { className: 'advanced-section' }, [
          this.el('h3', null, [this.text('组件依赖图')]),
          this.el('div', { className: 'component-graph-stats' }, [
            this.el('div', { className: 'graph-stat' }, [this.el('span', { className: 'stat-num' }, [this.text(cg.total_components)]), this.text(' 组件')]),
            this.el('div', { className: 'graph-stat' }, [this.el('span', { className: 'stat-num' }, [this.text(cg.total_dependencies)]), this.text(' 依赖')]),
            this.el('div', { className: 'graph-stat' }, [this.el('span', { className: 'stat-num' }, [this.text(cg.circular_dependencies)]), this.text(' 循环依赖')]),
            this.el('div', { className: 'graph-stat' }, [this.text('耦合度: '), this.el('span', { className: 'stat-num' }, [this.text(cg.coupling_score)])]),
          ]),
        ])
      );
    }

    if ((aa as any).cross_file_relations?.total_relations > 0) {
      const cfr = (aa as any).cross_file_relations;
      sections.push(
        this.el('div', { className: 'advanced-section' }, [
          this.el('h3', null, [this.text('跨文件关联')]),
          this.el('p', null, [this.text(`${cfr.total_relations} 关系 | ${cfr.missing_test_files || 0} 缺测试 | ${cfr.orphan_files || 0} 孤立文件`)]),
        ])
      );
    }

    if (sections.length === 0) return this.el('div', null, []);

    return this.el('div', { className: 'advanced-analysis' }, sections);
  }

  private renderIssuesByFile(files: FrontendFileAnalysis[], _allIssues: FrontendQualityIssue[]): HTMLElement {
    const filesWithIssues = files.filter(f => f.issues.length > 0);
    const sections: Child[] = [];

    for (const file of filesWithIssues) {
      const fileIssues = this.activeFilter === 'ALL'
        ? file.issues
        : file.issues.filter(i => i.severity === this.activeFilter);

      if (fileIssues.length === 0) continue;

      const isExpanded = this.expandedFile === file.file_path;

      const fileSectionChildren: Child[] = [
        this.el('div', {
          className: 'file-header',
          'data-file-path': file.file_path,
        }, [
          this.el('span', { className: 'file-icon' }, [this.text(isExpanded ? '▶' : '▼')]),
          this.el('span', { className: 'file-name' }, [this.text(file.file_path)]),
          this.el('span', { className: 'issue-badge' }, [this.text(`${fileIssues.length} 个问题`)]),
        ]),
      ];

      if (isExpanded) {
        const issueItems = fileIssues.map(issue => this.renderIssue(issue));
        fileSectionChildren.push(this.el('div', { className: 'file-issues' }, issueItems));
      }

      sections.push(
        this.el('div', { className: `file-section${isExpanded ? ' expanded' : ''}` }, fileSectionChildren)
      );
    }

    return this.el('div', { className: 'issues-by-file' }, [
      this.el('h3', null, [this.text('🔬 按文件查看问题')]),
      ...sections,
    ]);
  }

  private renderIssue(issue: FrontendQualityIssue): HTMLElement {
    const sevConfig = SEVERITY_CONFIG[issue.severity] || { icon: '•', label: '未知' };
    const catConfig = CATEGORY_CONFIG[issue.category] || { icon: '📁', label: '未知' };

    const children: Child[] = [
      this.el('div', { className: 'issue-header' }, [
        this.el('span', { className: 'issue-severity', 'data-severity': issue.severity },
          [this.text(`${sevConfig.icon} ${sevConfig.label}`)]),
        this.el('span', { className: 'issue-rule-key' }, [this.text(issue.rule_key)]),
        this.el('span', { className: 'issue-line' }, [this.text(`L${issue.line}`)]),
      ]),
      this.el('div', { className: 'issue-message' }, [this.text(issue.message)]),
    ];

    if (issue.remediation) {
      children.push(this.el('div', { className: 'issue-remediation' }, [this.text(`💡 ${issue.remediation}`)]));
    }
    if (issue.evidence) {
      children.push(this.el('pre', { className: 'issue-evidence' }, [this.text(issue.evidence)]));
    }

    return this.el('div', { className: 'issue-item', 'data-severity': issue.severity }, children);
  }

  private setupEventListeners(): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = new EventDelegator(this.root);

    // Filter tab clicked
    this.delegator.on('click', '.filter-tab', (_e, el) => {
      const filter = el.getAttribute('data-filter');
      if (filter && filter !== this.activeFilter) {
        this.activeFilter = filter;
        if (this.analysisData) {
          this.update();
          this.setupEventListeners();
        }
      }
    });

    // File header clicked (expand/collapse)
    this.delegator.on('click', '.file-header', (_e, el) => {
      const filePath = el.getAttribute('data-file-path');
      if (filePath) {
        this.expandedFile = this.expandedFile === filePath ? null : filePath;
        if (this.analysisData) {
          this.update();
          this.setupEventListeners();
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
