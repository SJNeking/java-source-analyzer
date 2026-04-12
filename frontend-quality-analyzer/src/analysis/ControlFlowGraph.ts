/**
 * Control Flow Graph (CFG) Analyzer
 *
 * Builds control flow graphs for functions/methods to detect:
 * - Unreachable code
 * - Complex branching (high cyclomatic complexity)
 * - Missing return paths
 * - Deeply nested conditionals
 *
 * Mirrors the Java analyzer's CFGBuilder architecture.
 */

import { QualityIssue, Severity } from '../types';
import { IssueCategory } from '../types';

// ==================== Type Definitions ====================

export interface CFGNode {
  id: number;
  type: 'entry' | 'exit' | 'statement' | 'condition' | 'loop' | 'return' | 'branch';
  line: number;
  code: string;
  successors: number[];
  predecessors: number[];
}

export interface CFGEdge {
  from: number;
  to: number;
  type: 'true' | 'false' | 'next' | 'break' | 'continue' | 'return';
}

export interface ControlFlowGraph {
  nodes: CFGNode[];
  edges: CFGEdge[];
  entryNode: number;
  exitNode: number;
  cyclomaticComplexity: number;
  maxNestingDepth: number;
  unreachableNodes: number[];
}

// ==================== CFG Builder ====================

export class CFGBuilder {
  private nodeIdCounter = 0;
  private nodes: CFGNode[] = [];
  private edges: CFGEdge[] = [];

  /**
   * Build CFG for a function's body
   */
  build(functionBody: string, startLine: number): ControlFlowGraph {
    this.nodeIdCounter = 0;
    this.nodes = [];
    this.edges = [];

    const lines = functionBody.split('\n');

    // Entry node
    const entryNode = this.createNode('entry', startLine, 'function entry');

    // Build nodes and edges from lines
    let currentNode: number = entryNode;
    let nestingDepth = 0;
    let maxNesting = 0;

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i].trim();
      if (!line || line.startsWith('//')) continue;

      const actualLine = startLine + i;

      if (this.isConditional(line)) {
        const conditionNode = this.createNode('condition', actualLine, line);
        this.addEdge(currentNode, conditionNode, 'next');

        const trueBranchNode = this.createNode('branch', actualLine + 1, 'if branch');
        const falseBranchNode = this.createNode('branch', actualLine + 1, 'else branch');

        this.addEdge(conditionNode, trueBranchNode, 'true');
        this.addEdge(conditionNode, falseBranchNode, 'false');

        nestingDepth++;
        maxNesting = Math.max(maxNesting, nestingDepth);
        currentNode = trueBranchNode;

      } else if (this.isLoop(line)) {
        const loopNode = this.createNode('loop', actualLine, line);
        this.addEdge(currentNode, loopNode, 'next');
        nestingDepth++;
        maxNesting = Math.max(maxNesting, nestingDepth);
        currentNode = loopNode;

      } else if (this.isReturn(line)) {
        const returnNode = this.createNode('return', actualLine, line);
        this.addEdge(currentNode, returnNode, 'return');
        currentNode = returnNode;
        nestingDepth = Math.max(0, nestingDepth - 1);

      } else {
        const stmtNode = this.createNode('statement', actualLine, line);
        this.addEdge(currentNode, stmtNode, 'next');
        currentNode = stmtNode;
      }
    }

    // Exit node
    const exitNode = this.createNode('exit', startLine + lines.length, 'function exit');
    this.addEdge(currentNode, exitNode, 'next');

    // Calculate cyclomatic complexity
    const cyclomaticComplexity = this.calculateCyclomaticComplexity();

    // Find unreachable nodes
    const unreachableNodes = this.findUnreachableNodes(entryNode);

    return {
      nodes: this.nodes,
      edges: this.edges,
      entryNode: entryNode,
      exitNode: exitNode,
      cyclomaticComplexity,
      maxNestingDepth: maxNesting,
      unreachableNodes,
    };
  }

  /**
   * Create a new CFG node
   */
  private createNode(type: CFGNode['type'], line: number, code: string): number {
    const id = this.nodeIdCounter++;
    this.nodes.push({
      id,
      type,
      line,
      code: code.substring(0, 80),
      successors: [],
      predecessors: [],
    });
    return id;
  }

  /**
   * Add an edge between two nodes
   */
  private addEdge(from: number, to: number, type: CFGEdge['type']): void {
    this.edges.push({ from, to, type });
    this.nodes[from].successors.push(to);
    this.nodes[to].predecessors.push(from);
  }

  /**
   * Check if a line is a conditional
   */
  private isConditional(line: string): boolean {
    return /\bif\s*\(/.test(line) || /\bswitch\s*\(/.test(line) || /\?.*:/.test(line);
  }

  /**
   * Check if a line is a loop
   */
  private isLoop(line: string): boolean {
    return /\bfor\s*\(/.test(line) || /\bwhile\s*\(/.test(line) || /\.forEach\s*\(/.test(line);
  }

  /**
   * Check if a line is a return
   */
  private isReturn(line: string): boolean {
    return /\breturn\s/.test(line);
  }

  /**
   * Calculate cyclomatic complexity: E - N + 2P
   */
  private calculateCyclomaticComplexity(): number {
    // M = E - N + 2P
    // Where E = edges, N = nodes, P = connected components (1 for single function)
    return this.edges.length - this.nodes.length + 2;
  }

  /**
   * Find unreachable nodes (nodes not reachable from entry)
   */
  private findUnreachableNodes(entryNode: number): number[] {
    const reachable = new Set<number>();
    const queue: number[] = [entryNode];

    while (queue.length > 0) {
      const current = queue.shift()!;
      if (reachable.has(current)) continue;
      reachable.add(current);

      const node = this.nodes[current];
      if (node) {
        for (const succ of node.successors) {
          if (!reachable.has(succ)) {
            queue.push(succ);
          }
        }
      }
    }

    return this.nodes
      .filter(n => !reachable.has(n.id) && n.type !== 'exit')
      .map(n => n.id);
  }
}

// ==================== CFG Quality Issues ====================

/**
 * Analyze CFG and report quality issues
 */
export function analyzeCFGIssues(
  cfg: ControlFlowGraph,
  filePath: string,
  functionName: string
): QualityIssue[] {
  const issues: QualityIssue[] = [];

  // High cyclomatic complexity
  if (cfg.cyclomaticComplexity > 10) {
    const entryNode = cfg.nodes.find(n => n.id === cfg.entryNode);
    issues.push({
      rule_key: 'FE-CFG-001',
      rule_name: 'Function has high cyclomatic complexity',
      severity: Severity.MAJOR,
      category: IssueCategory.ARCHITECTURE,
      file_path: filePath,
      line: entryNode?.line || 1,
      message: `Cyclomatic complexity: ${cfg.cyclomaticComplexity} (max recommended: 10)`,
      remediation: 'Extract complex branches into separate functions',
      confidence: 0.95,
      function_name: functionName,
    });
  }

  // Unreachable code
  for (const nodeId of cfg.unreachableNodes) {
    const node = cfg.nodes[nodeId];
    if (node) {
      issues.push({
        rule_key: 'FE-CFG-002',
        rule_name: 'Unreachable code detected',
        severity: Severity.MAJOR,
        category: IssueCategory.ARCHITECTURE,
        file_path: filePath,
        line: node.line,
        message: `This code is unreachable: ${node.code}`,
        remediation: 'Remove unreachable code or fix control flow',
        confidence: 0.9,
        function_name: functionName,
      });
    }
  }

  // Deep nesting
  if (cfg.maxNestingDepth > 4) {
    issues.push({
      rule_key: 'FE-CFG-003',
      rule_name: 'Code is too deeply nested',
      severity: Severity.MINOR,
      category: IssueCategory.ARCHITECTURE,
      file_path: filePath,
      line: 1,
      message: `Maximum nesting depth: ${cfg.maxNestingDepth} (max recommended: 4)`,
      remediation: 'Use early returns or extract nested logic into functions',
      confidence: 0.85,
      function_name: functionName,
    });
  }

  return issues;
}
