/**
 * Java Source Analyzer - Code Explorer View (Refactored)
 *
 * Uses Component base class for DOM creation.
 * Uses event delegation instead of inline handlers.
 * Uses centralized constants for icons and labels.
 */

import type { AnalysisResult, Asset, MethodAsset } from '../types';
import { Component, type Child } from '../framework/component';
import { EventDelegator } from '../framework/events';
import { CONFIG } from '../config';
import { LRUCache } from '../utils/lru-cache';
import { ArchitectureDetector } from '../utils/architecture-detector';
import { eventBus } from '../utils/event-bus';
import { ArchitectureGrouper, FilterEngine } from '../domain';
import { ICON, LABEL, CLS, C } from '../constants';
import { Style } from '../utils/style-helpers';

export class CodeExplorerView extends Component {
  private containerId: string;
  private data: AnalysisResult | null = null;
  private _currentMethod: string = '';
  private _currentClass: string = '';
  private sourceCodeCache = new LRUCache<string, string>(50);
  private delegator: EventDelegator | null = null;

  constructor(containerId: string = 'code-explorer-content') {
    super();
    this.containerId = containerId;
  }

  public loadData(data: AnalysisResult): void {
    this.data = data;
    this._currentClass = '';
    this._currentMethod = '';
    const container = document.getElementById(this.containerId);
    if (!container) return;
    this.mount(container);
    this.setupEventListeners();
  }

  public buildRoot(): HTMLElement {
    if (!this.data) return this.el('div', null, []);

    return this.el('div', { className: 'explorer-layout' }, [
      this.renderTreePanel(),
      this.renderMainPanel(),
      this.renderContextPanel(),
    ]);
  }

  private renderTreePanel(): HTMLElement {
    const children: Child[] = [
      this.el('div', { className: CLS.TREE_HEADER }, [
        this.el('div', { className: CLS.TREE_VIEW_TOGGLES }, [
          this.el('button', { className: CLS.VIEW_TOGGLE_ACTIVE, 'data-mode': 'packages' }, [this.text(LABEL.EXPLORER.PACKAGE_VIEW)]),
          this.el('button', { className: CLS.VIEW_TOGGLE, 'data-mode': 'flat' }, [this.text(LABEL.EXPLORER.LIST_VIEW)]),
        ]),
        this.el('input', { type: 'text', id: 'tree-search', className: CLS.TREE_SEARCH, placeholder: LABEL.EXPLORER.SEARCH_PLACEHOLDER }),
      ]),
      this.renderQuickAccess(),
      this.el('div', { id: 'tree-content', className: CLS.TREE_CONTENT }),
    ];

    return this.el('div', { className: CLS.EXPLORER_TREE }, children);
  }

  private renderMainPanel(): HTMLElement {
    return this.el('div', { className: CLS.EXPLORER_MAIN }, [
      this.el('div', { className: CLS.VIEWER_TABS }, [
        this.el('button', { className: CLS.VIEWER_TAB_ACTIVE, 'data-tab': 'diagram' }, [this.text(LABEL.EXPLORER.DIAGRAM_TAB)]),
        this.el('button', { className: CLS.VIEWER_TAB, 'data-tab': 'source' }, [this.text(LABEL.EXPLORER.SOURCE_TAB)]),
        this.el('button', { className: CLS.VIEWER_TAB, 'data-tab': 'calls' }, [this.text(LABEL.EXPLORER.CALLS_TAB)]),
      ]),
      this.el('div', { className: CLS.VIEWER_PANELS }, [
        this.el('div', { id: 'panel-diagram', className: CLS.PANEL_ACTIVE }, [this.renderWelcome()]),
        this.el('div', { id: 'panel-source', className: CLS.PANEL }, [
          this.el('div', { className: CLS.EMPTY_CENTER }, [
            this.el('div', { className: CLS.EMPTY_ICON_SIMPLE }, [this.text(ICON.CODE.JAVA)]),
            this.el('div', null, [this.text(LABEL.EXPLORER.SELECT_METHOD)]),
          ]),
        ]),
        this.el('div', { id: 'panel-calls', className: CLS.PANEL }, [
          this.el('div', { className: CLS.EMPTY_CENTER }, [
            this.el('div', { className: CLS.EMPTY_ICON_SIMPLE }, [this.text(ICON.LINK.CHAIN)]),
            this.el('div', null, [this.text(LABEL.EXPLORER.SELECT_CALL)]),
          ]),
        ]),
      ]),
    ]);
  }

  private renderContextPanel(): HTMLElement {
    return this.el('div', { className: CLS.EXPLORER_CONTEXT }, [
      this.el('div', { className: CLS.CONTEXT_HEADER }, [this.text(LABEL.EXPLORER.DETAIL_HEADER)]),
      this.el('div', { id: 'ctx-content', className: CLS.CONTEXT_BODY }, [
        this.el('div', { className: CLS.EMPTY_CENTER }, [
          this.el('div', { className: CLS.EMPTY_ICON_SIMPLE }, [this.text(ICON.UI.COPY)]),
          this.el('div', null, [this.text(LABEL.EXPLORER.SELECT_DETAIL)]),
        ]),
      ]),
    ]);
  }

  private renderWelcome(): HTMLElement {
    if (!this.data) return this.el('div', null, []);

    const assets = this.data.assets || [];
    const totalClasses = assets.length;
    const totalMethods = assets.reduce((sum, a) => sum + ((a.methods_full?.length || a.methods?.length) || 0), 0);
    const totalIssues = this.data.quality_summary?.total_issues || 0;
    const archInfo = ArchitectureDetector.detect(assets);

    const children: Child[] = [
      this.el('div', { className: 'welcome' }, [
        this.el('div', { className: 'welcome-title' }, [
          this.el('h2', null, [this.text(`${this.data.framework} v${this.data.version}`)]),
          this.el('p', { className: 'welcome-sub' }, [this.text(LABEL.EXPLORER.WELCOME_SUB(totalClasses, archInfo.layers.length > 0 ? LABEL.EXPLORER.MULTI_MODULE(archInfo.layers.length) : LABEL.EXPLORER.SINGLE_MODULE))]),
        ]),
        this.el('div', { className: 'stats-row' }, [
          this.el('div', { className: 'stat' }, [this.el('div', { className: 'stat-v' }, [this.text(totalClasses)]), this.el('div', { className: 'stat-l' }, [this.text(LABEL.METRICS.TOTAL_CLASSES)])]),
          this.el('div', { className: 'stat' }, [this.el('div', { className: 'stat-v' }, [this.text(totalMethods)]), this.el('div', { className: 'stat-l' }, [this.text(LABEL.METRICS.TOTAL_METHODS)])]),
          this.el('div', { className: 'stat' }, [this.el('div', { className: 'stat-v' }, [this.text(totalIssues)]), this.el('div', { className: 'stat-l' }, [this.text(LABEL.COMMON.DETAIL)])]),
        ]),
      ]),
    ];

    // Architecture layers section
    if (archInfo.layers.length > 0) {
      const layerCards = archInfo.layers.map(l =>
        this.el('div', { className: 'layer-card' }, [
          this.el('div', { className: 'layer-ico' }, [this.text(l.icon)]),
          this.el('div', { className: 'layer-nm' }, [this.text(l.label)]),
          this.el('div', { className: 'layer-ct' }, [this.text(LABEL.EXPLORER.LAYER_CLASS_COUNT(l.count))]),
        ])
      );

      children.push(
        this.el('div', { className: 'layers-section' }, [
          this.el('h3', null, [this.text(LABEL.EXPLORER.PROJECT_STRUCTURE(archInfo.typeName))]),
          this.el('div', { className: 'layers-grid' }, layerCards),
        ])
      );
    }

    // Quality gate
    if (this.data.quality_gate) {
      children.push(
        this.el('div', { className: `qgate ${this.data.quality_gate.passed ? 'pass' : 'fail'}` }, [
          this.text(this.data.quality_gate.passed ? LABEL.QUALITY_GATE.PASSED : LABEL.QUALITY_GATE.FAILED),
        ])
      );
    }

    return this.el('div', null, children);
  }

  private renderQuickAccess(): Child | null {
    if (!this.data) return null;

    const assets = this.data.assets || [];
    const qualityHotspots = assets.filter((a: any) => a.qualityIssues?.critical > 0).slice(0, 5);
    const apiEndpoints = assets.filter(a =>
      a.kind === 'CLASS' &&
      (a.address.includes('.controller.') || a.address.includes('.api.') ||
       (a as any).annotations?.some((an: string) => an.includes('Controller')))
    ).slice(0, 5);

    const complexMethods: Array<{ asset: Asset; method: MethodAsset }> = [];
    assets.forEach(asset => {
      (asset.methods_full || asset.methods || []).forEach(method => {
        const complexity = (method as any).cyclomaticComplexity || 0;
        if (complexity > 10) complexMethods.push({ asset, method });
      });
    });
    complexMethods.sort((a, b) => ((b.method as any).cyclomaticComplexity || 0) - ((a.method as any).cyclomaticComplexity || 0));

    const hasShortcuts = qualityHotspots.length > 0 || apiEndpoints.length > 0 || complexMethods.length > 0;
    if (!hasShortcuts) return null;

    const sections: Child[] = [];

    if (qualityHotspots.length > 0) {
      const items = qualityHotspots.map(asset =>
        this.el('div', { className: CLS.QA_ITEM, 'data-class': asset.address }, [this.text(asset.address.split('.').pop() || '')])
      );
      sections.push(
        this.el('div', { className: CLS.QA_SECTION }, [
          this.el('div', { style: { fontSize: '10px', color: Style.redLt, marginBottom: '4px' } as Partial<CSSStyleDeclaration> },
            [this.text(LABEL.EXPLORER.QUALITY_HOTSPOTS(qualityHotspots.length))]),
          ...items,
        ])
      );
    }

    if (apiEndpoints.length > 0) {
      const items = apiEndpoints.map(asset =>
        this.el('div', { className: CLS.QA_ITEM, 'data-class': asset.address }, [this.text(asset.address.split('.').pop() || '')])
      );
      sections.push(
        this.el('div', { className: CLS.QA_SECTION }, [
          this.el('div', { style: { fontSize: '10px', color: Style.blueCall, marginBottom: '4px' } as Partial<CSSStyleDeclaration> },
            [this.text(LABEL.EXPLORER.API_ENDPOINTS(apiEndpoints.length))]),
          ...items,
        ])
      );
    }

    if (complexMethods.length > 0) {
      const items = complexMethods.slice(0, 5).map(({ method }) =>
        this.el('div', { className: CLS.QA_ITEM, 'data-method': method.address }, [this.text(`${method.name}()`)])
      );
      sections.push(
        this.el('div', { className: CLS.QA_SECTION }, [
          this.el('div', { style: { fontSize: '10px', color: Style.orange, marginBottom: '4px' } as Partial<CSSStyleDeclaration> },
            [this.text(LABEL.EXPLORER.HIGH_COMPLEXITY(complexMethods.length))]),
          ...items,
        ])
      );
    }

    return this.el('div', { className: CLS.QUICK_ACCESS }, [
      this.el('div', { style: { fontSize: '11px', fontWeight: '600', color: 'var(--text-muted)', marginBottom: '8px' } as Partial<CSSStyleDeclaration> },
        [this.text(LABEL.EXPLORER.QUICK_ACCESS)]),
      ...sections,
    ]);
  }

  public mount(container: HTMLElement | null): void {
    super.mount(container);
    // After mounting, render tree content
    if (this.data && this.root) {
      const treeContent = this.find('#tree-content');
      if (treeContent) this.renderTree(this.data.assets || [], 'grouped', treeContent);
    }
  }

  private renderTree(assets: Asset[], mode: 'grouped' | 'flat', target?: HTMLElement): void {
    const container = target || this.find('#tree-content');
    if (!container) return;

    container.innerHTML = '';

    if (mode === 'flat') {
      const sorted = [...assets].sort((a, b) => a.address.localeCompare(b.address));
      sorted.forEach(a => container.appendChild(this.renderClassNode(a)));
    } else {
      const groups = ArchitectureGrouper.groupByArchitecture(assets);
      Array.from(groups.entries())
        .sort((a, b) => a[0].localeCompare(b[0]))
        .forEach(([pkg, items]) => {
          const shortName = pkg.split('.').pop() || pkg;
          const details = this.el('details', { className: CLS.PKG_GROUP, open: true }, [
            this.el('summary', { className: CLS.PKG_SUMMARY }, [
              this.el('span', { className: CLS.PKG_ICON }, [this.text(ICON.SECTION.ASSETS)]),
              this.el('span', { className: CLS.PKG_NAME }, [this.text(shortName)]),
              this.el('span', { className: CLS.PKG_COUNT }, [this.text(String(items.length))]),
            ]),
            this.el('div', { className: CLS.PKG_BODY }, items.map(a => this.renderClassNode(a))),
          ]);
          container.appendChild(details);
        });
    }
  }

  private renderClassNode(asset: Asset): HTMLElement {
    const name = asset.address.split('.').pop() || '';
    const methods = asset.methods_full || asset.methods || [];
    const kind = asset.kind || 'CLASS';
    const color = C[kind as keyof typeof C] || Style.slate[400];
    const icon = ICON.KIND[kind as keyof typeof ICON.KIND] || ICON.KIND.CLASS;

    const methodItems = methods.map(m =>
      this.el('div', { className: CLS.METHOD_ITEM, 'data-cls': asset.address, 'data-mtd': m.name }, [
        this.el('span', { className: CLS.METHOD_NAME }, [this.text(m.name)]),
        m.modifiers?.includes('public') ? this.el('span', { className: CLS.METHOD_BADGE_PUB }, [this.text('pub')]) : this.el('span', null, []),
        m.modifiers?.includes('static') ? this.el('span', { className: CLS.METHOD_BADGE_ST }, [this.text('st')]) : this.el('span', null, []),
      ])
    );

    return this.el('details', { className: CLS.CLASS_GROUP, 'data-addr': asset.address }, [
      this.el('summary', { className: CLS.CLASS_SUMMARY }, [
        this.el('span', { className: CLS.CLASS_ICON }, [this.text(icon)]),
        this.el('span', { className: CLS.CLASS_NAME_TAG }, [this.text(name)]),
        this.el('span', { className: CLS.CLASS_KIND, style: { color } as Partial<CSSStyleDeclaration> }, [this.text(kind)]),
      ]),
      this.el('div', { className: CLS.CLASS_BODY }, methodItems),
    ]);
  }

  private setupEventListeners(): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = new EventDelegator(this.root);
    if (!this.delegator) return;

    // Search input
    const searchInput = this.find('#tree-search');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        eventBus.emit('SEARCH', { keyword: (e.target as HTMLInputElement).value });
      });
    }

    // View toggle buttons
    this.delegator.on('click', '.view-toggle', (_e, el) => {
      const mode = el.getAttribute('data-mode') as 'grouped' | 'flat';
      this.findAll('.view-toggle').forEach(b => b.classList.toggle('active', b === el));
      eventBus.emit('TREE_MODE_CHANGE', { mode });
      if (this.data) this.renderTree(this.data.assets || [], mode);
    });

    // Tab buttons
    this.delegator.on('click', '.vtab', (_e, el) => {
      const tabId = el.getAttribute('data-tab');
      if (tabId) eventBus.emit('TAB_SWITCH', { tabId });
    });

    // Tree interactions (class/method clicks)
    this.delegator.on('click', '.qa-item', (_e, el) => {
      const classAddr = el.getAttribute('data-class');
      const methodAddr = el.getAttribute('data-method');
      if (classAddr) this.openClass(classAddr);
      else if (methodAddr) {
        const parts = methodAddr.split('#');
        if (parts.length > 1) {
          this.openClass(parts[0]);
          setTimeout(() => this.openMethod(parts[0], parts[1]), 100);
        }
      }
    });

    this.delegator.on('click', '.cls-summary', (e, el) => {
      e.preventDefault();
      const clsGroup = el.closest('.cls-group') as HTMLElement;
      const addr = clsGroup?.getAttribute('data-addr');
      if (addr) {
        eventBus.emit('CLASS_SELECT', { address: addr });
        this.openClass(addr);
      }
    });

    this.delegator.on('click', '.mtd-item', (_e, el) => {
      const clsAddr = el.getAttribute('data-cls');
      const mtdName = el.getAttribute('data-mtd');
      if (clsAddr && mtdName) {
        eventBus.emit('METHOD_SELECT', { classAddress: clsAddr, methodName: mtdName });
        this.openMethod(clsAddr, mtdName);
      }
    });

    // Copy code button
    this.delegator.on('click', '#copy-code-btn', () => {
      const el = this.find('.code-content');
      if (el && navigator.clipboard) {
        eventBus.emit('CODE_COPY', { code: el.textContent || '' });
      }
    });

    // Subscribe to events
    eventBus.on('TAB_SWITCH', ({ tabId }) => {
      this.findAll('.vtab').forEach(t => t.classList.toggle('active', t.getAttribute('data-tab') === tabId));
      this.findAll('.vpanel').forEach(p => {
        (p as HTMLElement).style.display = p.id === `panel-${tabId}` ? 'block' : 'none';
      });
    });

    eventBus.on('SEARCH', ({ keyword }) => {
      this.filterTree(keyword);
    });
  }

  private filterTree(term: string): void {
    if (!this.data) return;
    const assets = this.data.assets || [];
    const filtered = FilterEngine.filterAssets(assets, { keyword: term });
    const content = this.find('#tree-content');
    if (!content) return;

    if (filtered.length === 0 && term) {
      content.innerHTML = '';
      content.appendChild(this.el('div', { className: CLS.EMPTY_CENTER }, [
        this.el('div', { className: CLS.EMPTY_ICON_SIMPLE }, [this.text(ICON.UI.SEARCH)]),
        this.el('div', null, [this.text(LABEL.EXPLORER.NO_MATCH)]),
      ]));
      return;
    }

    this.renderTree(filtered, 'grouped', content);
  }

  private openClass(classAddr: string): void {
    if (!this.data) return;
    const asset = this.data.assets?.find(a => a.address === classAddr);
    if (!asset) return;

    this._currentClass = classAddr;
    this._currentMethod = '';
    eventBus.emit('TAB_SWITCH', { tabId: 'diagram' });

    const panel = this.find('#panel-diagram');
    if (panel) {
      const cn = asset.address.split('.').pop() || '';
      panel.innerHTML = '';
      const umlWrap = this.el('div', { className: CLS.UML_WRAP }, [
        this.el('div', { className: CLS.UML_HEADER }, [
          this.el('span', { className: CLS.UML_TITLE }, [this.text(cn)]),
          this.el('span', { className: CLS.UML_KIND, style: { color: this.getKindColor(asset.kind) } as Partial<CSSStyleDeclaration> }, [this.text(asset.kind)]),
        ]),
        this.el('div', { className: CLS.UML_BODY, id: 'uml-container' }, [
          this.el('pre', { id: 'uml-pre', className: 'mermaid', style: { display: 'none' } as Partial<CSSStyleDeclaration> }),
        ]),
      ]);
      panel.appendChild(umlWrap);
      setTimeout(() => this.renderClassUml(asset), 100);
    }

    this.renderContext(asset);
  }

  private openMethod(classAddr: string, methodName: string): void {
    if (!this.data) return;

    const cacheKey = `${classAddr}#${methodName}`;
    let code = this.sourceCodeCache.get(cacheKey);

    if (!code) {
      const asset = this.data.assets?.find(a => a.address === classAddr);
      if (!asset) return;
      const method = (asset.methods_full || asset.methods || []).find((m: MethodAsset) => m.name === methodName);
      if (!method) return;
      code = method.source_code || method.body_code || `// No source for ${methodName}`;
      this.sourceCodeCache.set(cacheKey, code);
    }

    this.renderCode(code, classAddr, methodName);
  }

  private renderCode(code: string, classAddr: string, methodName: string): void {
    const asset = this.data?.assets?.find(a => a.address === classAddr);
    const kind = asset?.kind || 'CLASS';
    const title = `${classAddr.split('.').pop()}.${methodName}()`;

    this._currentMethod = methodName;
    this._currentClass = classAddr;
    eventBus.emit('TAB_SWITCH', { tabId: 'source' });

    const panel = this.find('#panel-source');
    if (panel) {
      panel.innerHTML = '';
      panel.appendChild(this.renderCodeView(code, title, kind));
    }

    this.renderMethodContext(asset, methodName);
  }

  private renderCodeView(code: string, title: string, _kind: string): HTMLElement {
    const lines = code.split('\n');
    const nums = lines.map((_, i) => this.el('div', { className: CLS.CODE_LINE_NUMBER }, [this.text(String(i + 1))]));

    const contentLines = lines.map(line => {
      let h = line.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
      h = h.replace(/\b(public|private|protected|static|final|abstract)\b/g, `<span class="${CLS.CODE_KEYWORD}">$1</span>`);
      h = h.replace(/\b(class|interface|enum|extends|implements|return|if|else|for|while)\b/g, `<span class="${CLS.CODE_KEYWORD}">$1</span>`);
      h = h.replace(/\b(String|int|boolean|long|void|List|Map)\b/g, `<span class="${CLS.CODE_TYPE}">$1</span>`);
      h = h.replace(/(\/\/.*)/g, `<span class="${CLS.CODE_COMMENT}">$1</span>`);
      return this.el('div', { className: CLS.CODE_LINE }, []); // set via innerHTML
    });

    const body = this.el('div', { className: CLS.CODE_BODY }, [
      this.el('div', { className: CLS.CODE_NUMBERS }, nums),
      this.el('div', { className: CLS.CODE_CONTENT }),
    ]);

    // Set code content via innerHTML for syntax highlighting
    const codeContent = this.el('div', { className: CLS.CODE_CONTENT });
    lines.forEach(line => {
      let h = line.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
      h = h.replace(/\b(public|private|protected|static|final|abstract)\b/g, `<span class="${CLS.CODE_KEYWORD}">$1</span>`);
      h = h.replace(/\b(class|interface|enum|extends|implements|return|if|else|for|while)\b/g, `<span class="${CLS.CODE_KEYWORD}">$1</span>`);
      h = h.replace(/\b(String|int|boolean|long|void|List|Map)\b/g, `<span class="${CLS.CODE_TYPE}">$1</span>`);
      h = h.replace(/(\/\/.*)/g, `<span class="${CLS.CODE_COMMENT}">$1</span>`);
      const div = document.createElement('div');
      div.className = CLS.CODE_LINE;
      div.innerHTML = h;
      codeContent.appendChild(div);
    });
    body.innerHTML = '';
    body.appendChild(this.el('div', { className: CLS.CODE_NUMBERS }, nums));
    body.appendChild(codeContent);

    return this.el('div', { className: CLS.CODE_VIEW }, [
      this.el('div', { className: CLS.CODE_HEADER_BAR }, [
        this.el('span', null, [this.text(`${ICON.CODE.JAVA} ${title}`)]),
        this.el('button', { className: CLS.CODE_BUTTON, id: 'copy-code-btn' }, [this.text(LABEL.EXPLORER.COPY_CODE)]),
      ]),
      body,
    ]);
  }

  private async renderClassUml(asset: Asset): Promise<void> {
    const cn = asset.address.split('.').pop();
    let mc = `classDiagram\n  class ${cn} {\n`;

    (asset.fields_matrix || asset.fields || []).slice(0, 20).forEach((f) => {
      const fieldType = (f as any).type_path || f.type || 'var';
      mc += `    ${fieldType.split('.').pop()} ${f.name}\n`;
    });

    (asset.methods_full || asset.methods || []).slice(0, 20).forEach((m: MethodAsset) => {
      mc += `    ${m.modifiers?.includes('public') ? '+' : '-'}${m.name}()\n`;
    });

    mc += `  }\n`;

    (asset.import_dependencies || []).slice(0, 8).forEach((dep: string) => {
      const d = dep.split('.').pop();
      if (d && d !== cn) mc += `  ${cn} ..> ${d}\n`;
    });

    const preEl = document.getElementById('uml-pre');
    if (!preEl) return;

    preEl.textContent = mc;
    preEl.style.display = 'block';

    const mmd = (window as any).mermaid;
    if (!mmd) return;

    setTimeout(async () => {
      if (!mmd || !preEl) return;
      try {
        await mmd.run({ nodes: [preEl] });
        const svg = document.querySelector('#uml-container svg') as SVGElement;
        if (svg) {
          svg.removeAttribute('style');
          svg.style.width = '100%';
          svg.style.height = '100%';
        }
      } catch {
        const c = document.getElementById('uml-container');
        if (c) c.innerHTML = `<pre style="padding:16px;color:var(--warning);font-size:11px;">${mc}</pre>`;
      }
    }, 100);
  }

  private renderContext(asset: Asset): void {
    const el = this.find('#ctx-content');
    if (!el) return;

    const issues = this.data?.quality_issues || [];
    const assetShortName = asset.address.split('.').pop() || '';
    const clsIssues = issues.filter(i => i.class === asset.address || (i.class && i.class.includes(assetShortName)));
    const methods = asset.methods_full || asset.methods || [];
    const fields = asset.fields_matrix || asset.fields || [];

    el.innerHTML = '';

    const overview = this.el('div', { className: CLS.CONTEXT_SECTION }, [
      this.el('h4', null, [this.text(LABEL.EXPLORER.OVERVIEW)]),
      this.el('div', { className: CLS.CONTEXT_STATS }, [
        this.el('div', { className: CLS.CONTEXT_STAT }, [this.el('span', null, [this.text(LABEL.EXPLORER.METHODS)]), this.el('strong', null, [this.text(methods.length)])]),
        this.el('div', { className: CLS.CONTEXT_STAT }, [this.el('span', null, [this.text(LABEL.EXPLORER.FIELDS)]), this.el('strong', null, [this.text(fields.length)])]),
      ]),
    ]);
    el.appendChild(overview);

    if (clsIssues.length > 0) {
      const issueItems = clsIssues.slice(0, 10).map(i =>
        this.el('div', { className: `q-item sev-${(i.severity || 'info').toLowerCase()}` }, [
          this.el('span', { className: CLS.BADGE }, [this.text(i.severity)]),
          this.el('span', { className: CLS.MSG }, [this.text(i.message)]),
          this.el('span', { className: CLS.LINE }, [this.text(`L${i.line}`)]),
        ])
      );
      el.appendChild(
        this.el('div', { className: CLS.CONTEXT_SECTION }, [
          this.el('h4', null, [this.text(LABEL.EXPLORER.ISSUES(clsIssues.length))]),
          this.el('div', { className: CLS.QUALITY_LIST }, issueItems),
        ])
      );
    } else {
      el.appendChild(this.el('div', { className: CLS.CONTEXT_SECTION }, [
        this.el('h4', null, [this.text(LABEL.QUALITY.NO_QUALITY_ISSUES)]),
      ]));
    }
  }

  private renderMethodContext(asset: Asset | undefined, methodName: string): void {
    const el = this.find('#ctx-content');
    if (!el || !asset) return;

    const method = (asset.methods_full || asset.methods || []).find((m: MethodAsset) => m.name === methodName);
    if (!method) return;

    const issues = this.data?.quality_issues || [];
    const mtdIssues = issues.filter(i => i.method === methodName);
    const keyStmts = method.key_statements || [];
    const extCalls = keyStmts.filter(s => s.type === 'EXTERNAL_CALL');

    el.innerHTML = '';

    el.appendChild(this.el('div', { className: CLS.CONTEXT_SECTION }, [
      this.el('h4', null, [this.text(LABEL.EXPLORER.OVERVIEW)]),
      this.el('div', { className: CLS.CONTEXT_STATS }, [
        this.el('div', { className: CLS.CONTEXT_STAT }, [this.el('span', null, [this.text(LABEL.EXPLORER.METHODS)]), this.el('strong', null, [this.text(methods.length)])]),
        this.el('div', { className: CLS.CONTEXT_STAT }, [this.el('span', null, [this.text(LABEL.EXPLORER.FIELDS)]), this.el('strong', null, [this.text(fields.length)])]),
      ]),
      this.el('div', { className: CLS.METHOD_SIGNATURE }, [this.text(method.signature || `${methodName}()`)]),
      method.modifiers?.length ? this.el('div', { className: CLS.METHOD_MODIFIERS }, [this.text(method.modifiers.join(' '))]) : this.el('div', null, []),
    ]));

    if (extCalls.length > 0) {
      const callItems = extCalls.map(c => {
        if (!c.target) return this.el('div', null, []);
        const parts = c.target.split('.');
        const m = parts.pop();
        const cls = parts.join('.').split('.').pop();
        return this.el('div', { className: CLS.CALL_ITEM }, [
          this.el('span', { className: CLS.CALL_CLASS }, [this.text(cls)]),
          this.el('span', null, [this.text('.')]),
          this.el('span', { className: CLS.CALL_METHOD }, [this.text(`${m}()`)]),
          this.el('span', { className: CLS.CALL_LINE }, [this.text(`L${c.line}`)]),
        ]);
      });

      el.appendChild(
        this.el('div', { className: CLS.CONTEXT_SECTION }, [
          this.el('h4', null, [this.text(LABEL.EXPLORER.CALLS(extCalls.length))]),
          this.el('div', { className: CLS.CALL_LIST }, callItems),
        ])
      );
    }

    if (mtdIssues.length > 0) {
      const issueItems = mtdIssues.map(i =>
        this.el('div', { className: `q-item sev-${(i.severity || 'info').toLowerCase()}` }, [
          this.el('span', { className: CLS.BADGE }, [this.text(i.severity)]),
          this.el('span', { className: CLS.MSG }, [this.text(i.message)]),
          this.el('span', { className: CLS.LINE }, [this.text(`L${i.line}`)]),
        ])
      );
      el.appendChild(
        this.el('div', { className: CLS.CONTEXT_SECTION }, [
          this.el('h4', null, [this.text(LABEL.EXPLORER.ISSUES(mtdIssues.length))]),
          this.el('div', { className: CLS.QUALITY_LIST }, issueItems),
        ])
      );
    }
  }

  private getKindColor(kind: string): string {
    return C[kind as keyof typeof C] || Style.slate[400];
  }

  public cleanup(): void {
    if (this.delegator) this.delegator.destroy();
    this.delegator = null;
    this.unmount();
  }
}
