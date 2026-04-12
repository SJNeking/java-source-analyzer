/**
 * Graph Aggregator
 * 
 * Implements graph clustering and aggregation to reduce visual complexity:
 * - Aggregate low-value classes into package clusters
 * - Fold utility classes
 * - Collapse external dependencies into "External" node
 * - Smart edge pruning based on weight and importance
 */

import type { GraphData, GraphNode, GraphLink, AssetKind } from '../types';

export interface AggregationOptions {
  /** Minimum degree to keep node visible */
  minDegree?: number;
  /** Whether to aggregate utility classes */
  aggregateUtils?: boolean;
  /** Whether to collapse external dependencies */
  collapseExternals?: boolean;
  /** Maximum nodes to display before auto-aggregation */
  maxNodes?: number;
}

export class GraphAggregator {
  /**
   * Apply aggregation strategies to reduce graph complexity
   */
  static aggregate(data: GraphData, options: AggregationOptions = {}): GraphData {
    const {
      minDegree = 0,
      aggregateUtils = true,
      collapseExternals = true,
      maxNodes = 200,
    } = options;

    let result = { ...data };

    // Step 1: Filter by degree threshold
    if (minDegree > 0) {
      result = this.filterByDegree(result, minDegree);
    }

    // Step 2: Aggregate utility classes
    if (aggregateUtils) {
      result = this.aggregateUtilityClasses(result);
    }

    // Step 3: Collapse external dependencies
    if (collapseExternals) {
      result = this.collapseExternalDependencies(result);
    }

    // Step 4: If still too many nodes, apply aggressive aggregation
    if (result.nodes.length > maxNodes) {
      result = this.aggressiveAggregation(result, maxNodes);
    }

    return result;
  }

  /**
   * Filter nodes by minimum degree (connections count)
   */
  private static filterByDegree(data: GraphData, minDegree: number): GraphData {
    // Calculate degrees
    const degreeMap: Record<string, number> = {};
    data.links.forEach(link => {
      const sourceId = typeof link.source === 'string' ? link.source : (link.source as GraphNode).id;
      const targetId = typeof link.target === 'string' ? link.target : (link.target as GraphNode).id;
      degreeMap[sourceId] = (degreeMap[sourceId] || 0) + 1;
      degreeMap[targetId] = (degreeMap[targetId] || 0) + 1;
    });

    // Filter nodes with sufficient degree
    const validNodes = data.nodes.filter(node => {
      const degree = degreeMap[node.id] || 0;
      return degree >= minDegree;
    });

    const validIds = new Set(validNodes.map(n => n.id));

    // Filter links to only include valid nodes
    const validLinks = data.links.filter(link => {
      const sourceId = typeof link.source === 'string' ? link.source : (link.source as GraphNode).id;
      const targetId = typeof link.target === 'string' ? link.target : (link.target as GraphNode).id;
      return validIds.has(sourceId) && validIds.has(targetId);
    });

    return { ...data, nodes: validNodes, links: validLinks };
  }

  /**
   * Aggregate utility/helper classes into a single cluster
   */
  private static aggregateUtilityClasses(data: GraphData): GraphData {
    const utilNodes: GraphNode[] = [];
    const otherNodes: GraphNode[] = [];

    // Separate utility classes
    data.nodes.forEach(node => {
      const isUtil = 
        node.name?.toLowerCase().includes('util') ||
        node.name?.toLowerCase().includes('helper') ||
        node.category === 'UTILITY';

      if (isUtil) {
        utilNodes.push(node);
      } else {
        otherNodes.push(node);
      }
    });

    // If few utility classes, don't aggregate
    if (utilNodes.length <= 3) {
      return data;
    }

    // Create aggregated utility cluster node
    const clusterNode: GraphNode = {
      id: '__util_cluster__',
      name: `📦 Utils (${utilNodes.length} classes)`,
      category: 'UTILITY' as AssetKind,
      color: '#6b7280',
      description: 'Aggregated utility classes',
      methodCount: utilNodes.reduce((sum, n) => sum + (n.methodCount || 0), 0),
      fieldCount: utilNodes.reduce((sum, n) => sum + (n.fieldCount || 0), 0),
      symbolSize: 35,
      itemStyle: {
        color: '#6b7280',
        shadowBlur: 15,
        shadowColor: '#6b7280',
        borderColor: 'rgba(255,255,255,0.3)',
        borderWidth: 2,
      },
    };

    // Aggregate links
    const aggregatedLinks: GraphLink[] = [];
    const utilIds = new Set(utilNodes.map(n => n.id));

    data.links.forEach(link => {
      const sourceId = typeof link.source === 'string' ? link.source : (link.source as GraphNode).id;
      const targetId = typeof link.target === 'string' ? link.target : (link.target as GraphNode).id;

      const sourceIsUtil = utilIds.has(sourceId);
      const targetIsUtil = utilIds.has(targetId);

      // Both ends are utils - skip internal util links
      if (sourceIsUtil && targetIsUtil) return;

      // One end is util - redirect to cluster
      if (sourceIsUtil) {
        aggregatedLinks.push({
          ...link,
          source: clusterNode.id,
        });
      } else if (targetIsUtil) {
        aggregatedLinks.push({
          ...link,
          target: clusterNode.id,
        });
      } else {
        // Neither end is util - keep as is
        aggregatedLinks.push(link);
      }
    });

    return {
      ...data,
      nodes: [...otherNodes, clusterNode],
      links: aggregatedLinks,
    };
  }

  /**
   * Collapse all external dependencies into a single node
   */
  private static collapseExternalDependencies(data: GraphData): GraphData {
    const externalNodes = data.nodes.filter(n => n.category === 'EXTERNAL');
    
    // If few externals, don't collapse
    if (externalNodes.length <= 5) {
      return data;
    }

    // Group by dependency type (JDK vs Third-party)
    const jdkNodes = externalNodes.filter(n => n.dependencyType === 'JDK');
    const thirdPartyNodes = externalNodes.filter(n => n.dependencyType !== 'JDK');

    const collapsedNodes = data.nodes.filter(n => n.category !== 'EXTERNAL');
    const collapsedLinks: GraphLink[] = [];
    const externalIds = new Set(externalNodes.map(n => n.id));

    // Create cluster nodes
    const newClusters: GraphNode[] = [];

    if (jdkNodes.length > 0) {
      const jdkCluster: GraphNode = {
        id: '__jdk_cluster__',
        name: `☕ JDK (${jdkNodes.length} libs)`,
        category: 'EXTERNAL' as AssetKind,
        dependencyType: 'JDK',
        color: '#f59e0b',
        description: 'Aggregated JDK dependencies',
        methodCount: 0,
        fieldCount: 0,
        symbolSize: 30,
        itemStyle: {
          color: '#f59e0b',
          shadowBlur: 12,
          shadowColor: '#f59e0b',
          borderColor: 'rgba(255,255,255,0.3)',
          borderWidth: 2,
        },
      };
      collapsedNodes.push(jdkCluster);
      newClusters.push(jdkCluster);
    }

    if (thirdPartyNodes.length > 0) {
      const tpCluster: GraphNode = {
        id: '__thirdparty_cluster__',
        name: `📚 Third-Party (${thirdPartyNodes.length} libs)`,
        category: 'EXTERNAL' as AssetKind,
        dependencyType: 'THIRD_PARTY',
        color: '#ec4899',
        description: 'Aggregated third-party dependencies',
        methodCount: 0,
        fieldCount: 0,
        symbolSize: 30,
        itemStyle: {
          color: '#ec4899',
          shadowBlur: 12,
          shadowColor: '#ec4899',
          borderColor: 'rgba(255,255,255,0.3)',
          borderWidth: 2,
        },
      };
      collapsedNodes.push(tpCluster);
      newClusters.push(tpCluster);
    }

    // Redirect links
    const jdkIds = new Set(jdkNodes.map(n => n.id));
    const tpIds = new Set(thirdPartyNodes.map(n => n.id));

    data.links.forEach(link => {
      const sourceId = typeof link.source === 'string' ? link.source : (link.source as GraphNode).id;
      const targetId = typeof link.target === 'string' ? link.target : (link.target as GraphNode).id;

      const sourceIsExternal = externalIds.has(sourceId);
      const targetIsExternal = externalIds.has(targetId);

      // Both external - skip
      if (sourceIsExternal && targetIsExternal) return;

      // Source is external - redirect to appropriate cluster
      if (sourceIsExternal) {
        const clusterId = jdkIds.has(sourceId) ? '__jdk_cluster__' : '__thirdparty_cluster__';
        if (clusterId) {
          collapsedLinks.push({ ...link, source: clusterId });
        }
      }
      // Target is external - redirect to appropriate cluster
      else if (targetIsExternal) {
        const clusterId = jdkIds.has(targetId) ? '__jdk_cluster__' : '__thirdparty_cluster__';
        if (clusterId) {
          collapsedLinks.push({ ...link, target: clusterId });
        }
      }
      // Neither external - keep
      else {
        collapsedLinks.push(link);
      }
    });

    return {
      ...data,
      nodes: collapsedNodes,
      links: collapsedLinks,
    };
  }

  /**
   * Aggressive aggregation when graph is still too large
   */
  private static aggressiveAggregation(data: GraphData, maxNodes: number): GraphData {
    // Sort nodes by degree (importance)
    const degreeMap: Record<string, number> = {};
    data.links.forEach(link => {
      const sourceId = typeof link.source === 'string' ? link.source : (link.source as GraphNode).id;
      const targetId = typeof link.target === 'string' ? link.target : (link.target as GraphNode).id;
      degreeMap[sourceId] = (degreeMap[sourceId] || 0) + 1;
      degreeMap[targetId] = (degreeMap[targetId] || 0) + 1;
    });

    const sortedNodes = [...data.nodes].sort((a, b) => {
      return (degreeMap[b.id] || 0) - (degreeMap[a.id] || 0);
    });

    // Keep top nodes
    const keepNodes = sortedNodes.slice(0, maxNodes);
    const keepIds = new Set(keepNodes.map(n => n.id));

    // Filter links
    const keepLinks = data.links.filter(link => {
      const sourceId = typeof link.source === 'string' ? link.source : (link.source as GraphNode).id;
      const targetId = typeof link.target === 'string' ? link.target : (link.target as GraphNode).id;
      return keepIds.has(sourceId) && keepIds.has(targetId);
    });

    return {
      ...data,
      nodes: keepNodes,
      links: keepLinks,
    };
  }

  /**
   * Prune edges based on weight/importance
   */
  static pruneEdges(data: GraphData, options: {
    minWeight?: number;
    maxEdgesPerNode?: number;
  } = {}): GraphData {
    const { minWeight = 0, maxEdgesPerNode = 20 } = options;

    // Filter by minimum weight
    let filteredLinks = data.links.filter(link => 
      (link.value || 0) >= minWeight
    );

    // Limit edges per node
    const edgeCount: Record<string, number> = {};
    filteredLinks = filteredLinks.filter(link => {
      const sourceId = typeof link.source === 'string' ? link.source : (link.source as GraphNode).id;
      const targetId = typeof link.target === 'string' ? link.target : (link.target as GraphNode).id;
      
      edgeCount[sourceId] = (edgeCount[sourceId] || 0) + 1;
      edgeCount[targetId] = (edgeCount[targetId] || 0) + 1;

      return edgeCount[sourceId] <= maxEdgesPerNode && edgeCount[targetId] <= maxEdgesPerNode;
    });

    return { ...data, links: filteredLinks };
  }
}
