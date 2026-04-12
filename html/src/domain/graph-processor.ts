/**
 * Graph Data Processor
 * 
 * Pure functions for graph data transformation and analysis.
 * Separated from ForceGraphView for testability and reusability.
 */

import type { GraphData, GraphNode, GraphLink } from '../types';

export interface ProcessedGraphData extends GraphData {
  degreeMap: Record<string, number>;
}

export class GraphProcessor {
  /**
   * Calculate degree (number of connections) for each node
   */
  static calculateDegree(nodes: GraphNode[], links: GraphLink[]): Record<string, number> {
    const degreeMap: Record<string, number> = {};
    
    links.forEach(link => {
      // source and target are strings in GraphLink type
      degreeMap[link.source] = (degreeMap[link.source] || 0) + 1;
      degreeMap[link.target] = (degreeMap[link.target] || 0) + 1;
    });
    
    return degreeMap;
  }

  /**
   * Filter nodes by category
   */
  static filterByCategory(data: GraphData, activeCategories: Set<string>): GraphData {
    const filteredNodes = data.nodes.filter(node => activeCategories.has(node.category));
    const validIds = new Set(filteredNodes.map(n => n.id));
    
    const filteredLinks = data.links.filter(link => 
      validIds.has(link.source) && validIds.has(link.target)
    );
    
    return {
      ...data,
      nodes: filteredNodes,
      links: filteredLinks,
    };
  }

  /**
   * Calculate node size based on category and degree
   */
  static calculateNodeSize(node: GraphNode, degree: number): number {
    const isExternal = node.category === 'EXTERNAL';
    const base = isExternal ? 15 : 20;
    return base + Math.min(degree, 50) / 50 * (isExternal ? 10 : 20);
  }

  /**
   * Build node series data for ECharts
   */
  static buildNodeSeries(
    nodes: GraphNode[],
    degreeMap: Record<string, number>,
    options: {
      showLabels?: boolean;
    } = {}
  ): any[] {
    const { showLabels = false } = options;
    
    return nodes.map(node => {
      const degree = degreeMap[node.id] || 0;
      const isExternal = node.category === 'EXTERNAL';
      const symbolSize = this.calculateNodeSize(node, degree);
      
      const baseItemStyle = node.itemStyle || { borderColor: undefined, borderWidth: undefined };
      const defaultBorderColor = isExternal ? 'rgba(255,255,255,0.4)' : 'rgba(255,255,255,0.2)';
      const defaultBorderWidth = isExternal ? 1 : 1.5;
      
      return {
        ...node,
        symbolSize,
        itemStyle: {
          color: node.color,
          shadowBlur: degree > 10 ? 20 : 10,
          shadowColor: node.color,
          borderColor: baseItemStyle.borderColor || defaultBorderColor,
          borderWidth: baseItemStyle.borderWidth || defaultBorderWidth,
        },
        label: {
          show: showLabels,
          position: 'bottom',
          formatter: '{b}',
          color: '#e2e8f0',
          fontSize: 11,
        },
        degree, // Add degree for tooltip
      };
    });
  }

  /**
   * Build link series data for ECharts
   */
  static buildLinkSeries(links: GraphLink[]): any[] {
    return links.map(link => ({
      ...link,
      lineStyle: {
        color: '#94a3b8',
        opacity: 0.3,
        width: Math.min(1 + (link.value || 0) * 0.1, 4),
        curveness: 0.2,
      },
      symbol: ['none', 'arrow'],
      symbolSize: [0, 6],
    }));
  }

  /**
   * Find connected components in the graph
   */
  static findConnectedComponents(nodes: GraphNode[], links: GraphLink[]): GraphNode[][] {
    const adjacencyList: Record<string, Set<string>> = {};
    const visited = new Set<string>();
    const components: GraphNode[][] = [];
    
    // Build adjacency list
    nodes.forEach(node => {
      adjacencyList[node.id] = new Set();
    });
    
    links.forEach(link => {
      // source and target are strings
      adjacencyList[link.source]?.add(link.target);
      adjacencyList[link.target]?.add(link.source);
    });
    
    // DFS to find components
    const dfs = (nodeId: string, component: GraphNode[]) => {
      visited.add(nodeId);
      const node = nodes.find(n => n.id === nodeId);
      if (node) component.push(node);
      
      adjacencyList[nodeId]?.forEach(neighbor => {
        if (!visited.has(neighbor)) {
          dfs(neighbor, component);
        }
      });
    };
    
    nodes.forEach(node => {
      if (!visited.has(node.id)) {
        const component: GraphNode[] = [];
        dfs(node.id, component);
        components.push(component);
      }
    });
    
    return components;
  }

  /**
   * Get hub nodes (nodes with highest degree)
   */
  static getHubNodes(data: GraphData, topN: number = 10): Array<{ node: GraphNode; degree: number }> {
    const degreeMap = this.calculateDegree(data.nodes, data.links);
    
    const nodesWithDegree = data.nodes.map(node => ({
      node,
      degree: degreeMap[node.id] || 0,
    }));
    
    return nodesWithDegree
      .sort((a, b) => b.degree - a.degree)
      .slice(0, topN);
  }
}
