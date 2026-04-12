/**
 * Component Dependency Graph Analyzer
 *
 * Builds a directed graph of component imports and relationships.
 * Detects circular dependencies, orphan components, and coupling metrics.
 *
 * Mirrors the Java analyzer's DependencyGraphGenerator architecture.
 */

import { QualityIssue, Severity, IssueCategory } from '../types';

// ==================== Type Definitions ====================

export interface ComponentNode {
  filePath: string;
  componentName: string;
  imports: string[];
  importedBy: string[];
  isOrphan: boolean;
  complexity: number;
}

export interface DependencyEdge {
  from: string;
  to: string;
  type: 'import' | 'dynamic-import' | 'lazy';
}

export interface ComponentGraph {
  nodes: ComponentNode[];
  edges: DependencyEdge[];
  circularDependencies: string[][];
  orphanComponents: string[];
  couplingScore: number;
  maxCouplingPath: string[];
}

// ==================== Component Dependency Analyzer ====================

export class ComponentDependencyAnalyzer {
  private graph: ComponentGraph = {
    nodes: [],
    edges: [],
    circularDependencies: [],
    orphanComponents: [],
    couplingScore: 0,
    maxCouplingPath: [],
  };

  /**
   * Analyze a set of files and build the dependency graph
   */
  analyze(files: Array<{ path: string; content: string }>): ComponentGraph {
    this.graph = {
      nodes: [],
      edges: [],
      circularDependencies: [],
      orphanComponents: [],
      couplingScore: 0,
      maxCouplingPath: [],
    };

    // Step 1: Parse each file to extract imports and component names
    for (const file of files) {
      if (!this.isComponentFile(file.path)) continue;

      const imports = this.extractImports(file.content, file.path);
      const componentName = this.extractComponentName(file.content, file.path);

      this.graph.nodes.push({
        filePath: file.path,
        componentName: componentName || this.fileNameToComponent(file.path),
        imports: imports,
        importedBy: [],
        isOrphan: true,
        complexity: this.estimateComplexity(file.content),
      });
    }

    // Step 2: Build edges
    for (const node of this.graph.nodes) {
      for (const imp of node.imports) {
        const targetNode = this.graph.nodes.find(n =>
          this.pathMatches(n.filePath, imp)
        );
        if (targetNode) {
          this.graph.edges.push({
            from: node.filePath,
            to: targetNode.filePath,
            type: this.isDynamicImport(imp) ? 'dynamic-import' : 'import',
          });
          targetNode.importedBy.push(node.filePath);
          node.isOrphan = false;
        }
      }
    }

    // Step 3: Detect circular dependencies
    this.detectCircularDependencies();

    // Step 4: Find orphan components
    this.graph.orphanComponents = this.graph.nodes
      .filter(n => n.isOrphan)
      .map(n => n.filePath);

    // Step 5: Calculate coupling score
    this.calculateCouplingScore();

    return this.graph;
  }

  /**
   * Check if a file is a component file
   */
  private isComponentFile(filePath: string): boolean {
    return /\.(tsx|jsx|vue|svelte)$/.test(filePath) ||
           /component\.ts$/.test(filePath) ||
           /\/components\//i.test(filePath);
  }

  /**
   * Extract imports from file content
   */
  private extractImports(content: string, filePath: string): string[] {
    const imports: string[] = [];

    // Static imports: import X from '...'
    const staticImports = content.match(/from\s+['"]([^'"]+)['"]/g) || [];
    staticImports.forEach(imp => {
      const module = imp.replace(/from\s+['"]|['"]/g, '');
      imports.push(module);
    });

    // Dynamic imports: import('...')
    const dynamicImports = content.match(/import\(\s*['"]([^'"]+)['"]\s*\)/g) || [];
    dynamicImports.forEach(imp => {
      const module = imp.replace(/import\(\s*['"]|['"]\s*\)/g, '');
      imports.push(module);
    });

    return imports;
  }

  /**
   * Extract component name from file content
   */
  private extractComponentName(content: string, filePath: string): string | null {
    // React: export default function ComponentName
    const reactFunc = content.match(/export\s+default\s+(?:function|class)\s+(\w+)/);
    if (reactFunc) return reactFunc[1];

    // React: export const ComponentName = () =>
    const reactConst = content.match(/export\s+const\s+(\w+)\s*=\s*(?:\(|async)/);
    if (reactConst) return reactConst[1];

    // Vue: export default { name: 'ComponentName' }
    const vueName = content.match(/name\s*:\s*['"]([^'"]+)['"]/);
    if (vueName) return vueName[1];

    return null;
  }

  /**
   * Convert file path to component name (fallback)
   */
  private fileNameToComponent(filePath: string): string {
    const parts = filePath.replace(/\.(tsx|jsx|vue|svelte|ts|js)$/, '').split('/');
    const name = parts[parts.length - 1];
    return name.charAt(0).toUpperCase() + name.slice(1);
  }

  /**
   * Check if a path matches an import
   */
  private pathMatches(filePath: string, importPath: string): boolean {
    // Convert import path to possible file paths
    const extensions = ['.tsx', '.jsx', '.vue', '.svelte', '.ts', '.js'];
    const basePath = importPath.replace(/^\.\//, './');

    for (const ext of extensions) {
      if (filePath.endsWith(basePath + ext) ||
          filePath.endsWith(basePath + '/index' + ext) ||
          filePath.endsWith(basePath.replace(/\/index$/, '') + ext)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Check if import is dynamic
   */
  private isDynamicImport(imp: string): boolean {
    return imp.includes('import(');
  }

  /**
   * Estimate component complexity
   */
  private estimateComplexity(content: string): number {
    let complexity = 1;

    // Hooks/state
    complexity += (content.match(/useState|useReducer|ref\(|reactive\(/g) || []).length;
    // Effects
    complexity += (content.match(/useEffect|watch|onMounted|onUnmounted/g) || []).length;
    // Conditional rendering
    complexity += (content.match(/\?.*:.*|v-if|v-show|&&/g) || []).length;
    // Event handlers
    complexity += (content.match(/onClick|onSubmit|onChange|@click|@submit/g) || []).length;

    return complexity;
  }

  /**
   * Detect circular dependencies using DFS
   */
  private detectCircularDependencies(): void {
    const visited = new Set<string>();
    const path: string[] = [];
    const pathSet = new Set<string>();

    const dfs = (node: string): void => {
      if (pathSet.has(node)) {
        // Found a cycle
        const cycleStart = path.indexOf(node);
        const cycle = [...path.slice(cycleStart), node];
        this.graph.circularDependencies.push(cycle);
        return;
      }

      if (visited.has(node)) return;

      visited.add(node);
      path.push(node);
      pathSet.add(node);

      const edges = this.graph.edges.filter(e => e.from === node);
      for (const edge of edges) {
        dfs(edge.to);
      }

      path.pop();
      pathSet.delete(node);
    };

    for (const node of this.graph.nodes) {
      if (!visited.has(node.filePath)) {
        dfs(node.filePath);
      }
    }
  }

  /**
   * Calculate coupling score (0-100, lower is better)
   */
  private calculateCouplingScore(): void {
    if (this.graph.nodes.length === 0) {
      this.graph.couplingScore = 0;
      return;
    }

    // Coupling = (number of edges) / (max possible edges)
    const maxEdges = this.graph.nodes.length * (this.graph.nodes.length - 1);
    this.graph.couplingScore = maxEdges > 0
      ? Math.round((this.graph.edges.length / maxEdges) * 100)
      : 0;

    // Find max coupling path (longest import chain)
    const longestPath = this.findLongestPath();
    this.graph.maxCouplingPath = longestPath;
  }

  /**
   * Find the longest import chain
   */
  private findLongestPath(): string[] {
    let longest: string[] = [];

    const dfs = (node: string, currentPath: string[], visited: Set<string>): void => {
      if (currentPath.length > longest.length) {
        longest = [...currentPath];
      }

      const edges = this.graph.edges.filter(e => e.from === node);
      for (const edge of edges) {
        if (!visited.has(edge.to)) {
          visited.add(edge.to);
          currentPath.push(edge.to);
          dfs(edge.to, currentPath, visited);
          currentPath.pop();
          visited.delete(edge.to);
        }
      }
    };

    for (const node of this.graph.nodes) {
      dfs(node.filePath, [node.filePath], new Set([node.filePath]));
    }

    return longest;
  }

  /**
   * Get the graph
   */
  getGraph(): ComponentGraph {
    return this.graph;
  }
}
