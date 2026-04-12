/**
 * Cross-file Relation Engine
 *
 * Discovers and maps relationships between:
 * - Component ↔ Test file
 * - Component ↔ CSS/SCSS module
 * - Component ↔ API service
 * - Component ↔ Store/State module
 * - Page → Component tree
 *
 * Mirrors the Java analyzer's RelationEngine architecture.
 */

import { Severity } from '../types';
import { IssueCategory } from '../types';

// ==================== Type Definitions ====================

export interface FileRelation {
  sourceFile: string;
  targetFile: string;
  relationType: RelationType;
  confidence: number;
  evidence: string;
}

export type RelationType =
  | 'COMPONENT_TEST'
  | 'COMPONENT_STYLE'
  | 'COMPONENT_API'
  | 'COMPONENT_STORE'
  | 'PAGE_COMPONENT'
  | 'COMPONENT_CHILD'
  | 'HOOK_COMPONENT';

export interface RelationSummary {
  totalRelations: number;
  byType: Record<RelationType, number>;
  missingRelations: MissingRelation[];
  orphanFiles: string[];
}

export interface MissingRelation {
  file: string;
  expectedRelation: RelationType;
  severity: Severity;
  message: string;
}

// ==================== Cross-file Relation Analyzer ====================

export class CrossFileRelationAnalyzer {
  /**
   * Analyze a set of files and discover cross-file relationships
   */
  analyze(files: Array<{ path: string; content: string }>): {
    relations: FileRelation[];
    summary: RelationSummary;
  } {
    const relations: FileRelation[] = [];
    const filePaths = files.map(f => f.path);
    const fileMap = new Map(files.map(f => [f.path, f.content]));

    // Discover relationships
    for (const file of files) {
      const componentRelations = this.findComponentRelations(file, filePaths, fileMap);
      relations.push(...componentRelations);
    }

    // Build summary
    const summary = this.buildSummary(relations, filePaths);

    return { relations, summary };
  }

  /**
   * Find relations for a component file
   */
  private findComponentRelations(
    file: { path: string; content: string },
    filePaths: string[],
    fileMap: Map<string, string>
  ): FileRelation[] {
    const relations: FileRelation[] = [];
    const { path, content } = file;

    // Only analyze component files
    if (!this.isComponentFile(path)) return relations;

    // Component ↔ Test
    const testFile = this.findTestFile(path, filePaths);
    if (testFile) {
      relations.push({
        sourceFile: path,
        targetFile: testFile,
        relationType: 'COMPONENT_TEST',
        confidence: 0.95,
        evidence: `Test file found: ${testFile}`,
      });
    }

    // Component ↔ CSS/SCSS
    const styleFile = this.findStyleFile(path, filePaths, content);
    if (styleFile) {
      relations.push({
        sourceFile: path,
        targetFile: styleFile,
        relationType: 'COMPONENT_STYLE',
        confidence: 0.9,
        evidence: `Style import: ${styleFile}`,
      });
    }

    // Component ↔ API Service
    const apiService = this.findAPIService(content, filePaths);
    if (apiService) {
      relations.push({
        sourceFile: path,
        targetFile: apiService,
        relationType: 'COMPONENT_API',
        confidence: 0.8,
        evidence: `API import detected`,
      });
    }

    // Component ↔ Store
    const storeModule = this.findStoreModule(content, filePaths);
    if (storeModule) {
      relations.push({
        sourceFile: path,
        targetFile: storeModule,
        relationType: 'COMPONENT_STORE',
        confidence: 0.8,
        evidence: `Store import detected`,
      });
    }

    // Page → Component tree
    if (this.isPageFile(path)) {
      const childComponents = this.findChildComponents(content, filePaths);
      for (const child of childComponents) {
        relations.push({
          sourceFile: path,
          targetFile: child,
          relationType: 'PAGE_COMPONENT',
          confidence: 0.7,
          evidence: `Child component import`,
        });
      }
    }

    // Component → Child Component
    const childComponents = this.findChildComponents(content, filePaths);
    for (const child of childComponents) {
      relations.push({
        sourceFile: path,
        targetFile: child,
        relationType: 'COMPONENT_CHILD',
        confidence: 0.75,
        evidence: `Nested component import`,
      });
    }

    return relations;
  }

  /**
   * Check if a file is a component file
   */
  private isComponentFile(path: string): boolean {
    return /\.(tsx|jsx|vue|svelte)$/.test(path) ||
           /\/components\//i.test(path);
  }

  /**
   * Check if a file is a page file
   */
  private isPageFile(path: string): boolean {
    return /\/pages\//i.test(path) || /\/views\//i.test(path) || /\/app\//i.test(path);
  }

  /**
   * Find corresponding test file
   */
  private findTestFile(componentPath: string, filePaths: string[]): string | null {
    const baseName = componentPath.replace(/\.(tsx|jsx|vue|svelte|ts|js)$/, '');

    const testPatterns = [
      `${baseName}.test.tsx`,
      `${baseName}.test.ts`,
      `${baseName}.test.jsx`,
      `${baseName}.test.js`,
      `${baseName}.spec.tsx`,
      `${baseName}.spec.ts`,
      `${baseName}Spec.tsx`,
      `${baseName}Spec.ts`,
      `${baseName}.test/__snapshots__/`,
    ];

    for (const pattern of testPatterns) {
      if (filePaths.includes(pattern)) return pattern;
    }

    // Also check __tests__ directory
    const testsDir = componentPath.replace(/\/([^/]+\.\w+)$/, '/__tests__/$1');
    for (const fp of filePaths) {
      if (fp.startsWith(testsDir.replace(/(__tests__\/)/, ''))) return fp;
    }

    return null;
  }

  /**
   * Find corresponding style file
   */
  private findStyleFile(componentPath: string, filePaths: string[], content: string): string | null {
    const baseName = componentPath.replace(/\.(tsx|jsx|vue|svelte|ts|js)$/, '');

    // Check for CSS module import
    const stylePatterns = [
      `${baseName}.module.css`,
      `${baseName}.module.scss`,
      `${baseName}.module.sass`,
      `${baseName}.module.less`,
      `${baseName}.css`,
      `${baseName}.scss`,
    ];

    for (const pattern of stylePatterns) {
      if (filePaths.includes(pattern)) return pattern;
    }

    // Check content for style import
    const importMatch = content.match(/import\s+['"].*\.css['"]|import\s+['"].*\.scss['"]/);
    if (importMatch) {
      const imported = importMatch[0].match(/['"]([^'"]+)['"]/);
      if (imported) {
        const fullPath = filePaths.find(f => f.includes(imported[1]));
        if (fullPath) return fullPath;
      }
    }

    return null;
  }

  /**
   * Find API service file
   */
  private findAPIService(content: string, filePaths: string[]): string | null {
    const apiPatterns = [
      /from\s+['"].*api['"]/,
      /from\s+['"].*service['"]/,
      /from\s+['"].*fetch['"]/,
      /from\s+['"].*axios['"]/,
      /from\s+['"].*request['"]/,
      /from\s+['"].*client['"]/,
    ];

    for (const pattern of apiPatterns) {
      const match = content.match(pattern);
      if (match) {
        const importPath = match[0].match(/['"]([^'"]+)['"]/);
        if (importPath) {
          const fullPath = filePaths.find(f =>
            f.includes(importPath[1]) || f.includes('api') || f.includes('service')
          );
          if (fullPath) return fullPath;
        }
      }
    }

    return null;
  }

  /**
   * Find store/state module
   */
  private findStoreModule(content: string, filePaths: string[]): string | null {
    const storePatterns = [
      /from\s+['"].*store['"]/,
      /from\s+['"].*reducer['"]/,
      /from\s+['"].*state['"]/,
      /from\s+['"].*vuex['"]/,
      /from\s+['"].*pinia['"]/,
      /from\s+['"].*redux['"]/,
      /from\s+['"].*zustand['"]/,
      /useSelector|useDispatch|connect\(/,
    ];

    for (const pattern of storePatterns) {
      if (pattern.test(content)) {
        const fullPath = filePaths.find(f =>
          /store|reducer|state|vuex|pinia|redux|zustand/i.test(f)
        );
        if (fullPath) return fullPath;
      }
    }

    return null;
  }

  /**
   * Find child component imports
   */
  private findChildComponents(content: string, filePaths: string[]): string[] {
    const imports = content.match(/from\s+['"]([^'"]+)['"]/g) || [];
    const children: string[] = [];

    for (const imp of imports) {
      const module = imp.replace(/from\s+['"]|['"]/g, '');
      // Skip node_modules and relative config
      if (module.startsWith('.') || module.startsWith('/')) {
        const fullPath = filePaths.find(f => {
          const normalizedModule = module.replace(/^\.\//, '');
          return f.includes(normalizedModule) && this.isComponentFile(f);
        });
        if (fullPath) children.push(fullPath);
      }
    }

    return children;
  }

  /**
   * Build relationship summary
   */
  private buildSummary(relations: FileRelation[], filePaths: string[]): RelationSummary {
    const byType: Record<RelationType, number> = {
      COMPONENT_TEST: 0,
      COMPONENT_STYLE: 0,
      COMPONENT_API: 0,
      COMPONENT_STORE: 0,
      PAGE_COMPONENT: 0,
      COMPONENT_CHILD: 0,
      HOOK_COMPONENT: 0,
    };

    for (const rel of relations) {
      byType[rel.relationType]++;
    }

    // Find missing relations (components without tests, etc.)
    const missingRelations: MissingRelation[] = [];
    const componentFiles = filePaths.filter(p => this.isComponentFile(p));

    for (const comp of componentFiles) {
      const hasTest = relations.some(r => r.sourceFile === comp && r.relationType === 'COMPONENT_TEST');
      if (!hasTest) {
        missingRelations.push({
          file: comp,
          expectedRelation: 'COMPONENT_TEST',
          severity: Severity.INFO,
          message: `Component has no test file`,
        });
      }
    }

    // Find orphan files (not imported by anything)
    const importedFiles = new Set(relations.map(r => r.targetFile));
    const orphanFiles = filePaths.filter(f =>
      this.isComponentFile(f) && !importedFiles.has(f) && !relations.some(r => r.sourceFile === f)
    );

    return {
      totalRelations: relations.length,
      byType,
      missingRelations: missingRelations,
      orphanFiles: orphanFiles.slice(0, 10), // Limit to 10
    };
  }
}
