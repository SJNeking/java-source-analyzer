/**
 * ForceGraphView - React Component
 * Force-directed graph visualization using ECharts
 */

import React, { useEffect, useRef, useCallback } from 'react';
import ReactECharts from 'echarts-for-react';
import type { GraphData, GraphNode, GraphLink } from '@types/graph';
import { useAppStore } from '@store/app-store';

interface ForceGraphViewProps {
  data?: GraphData;
}

const GRAPH_CONFIG = {
  FORCE: {
    REPULSION: 300,
    EDGE_LENGTH: 90,
    GRAVITY: 0.08,
    FRICTION: 0.6,
  },
  EMPHASIS: {
    LINE_WIDTH: 4,
    SHADOW_BLUR: 15,
  },
  LINK: {
    WIDTH_MULTIPLIER: 0.1,
    MAX_WIDTH: 4,
    CURVENESS: 0.2,
  },
};

const ForceGraphView: React.FC<ForceGraphViewProps> = ({ data }) => {
  const chartRef = useRef<ReactECharts>(null);
  const { nodeTypeFilters, setNodeTypeFilter } = useAppStore();

  // Filter nodes based on active filters
  const filterData = useCallback((graphData: GraphData): GraphData => {
    const activeFilters = new Set(Object.entries(nodeTypeFilters)
      .filter(([_, enabled]) => enabled)
      .map(([type]) => type));

    if (activeFilters.size === 0) return graphData;

    const filteredNodes = graphData.nodes.filter(node => 
      activeFilters.has(node.type.toUpperCase())
    );

    const nodeIds = new Set(filteredNodes.map(n => n.id));
    const filteredLinks = graphData.links.filter(link =>
      nodeIds.has(link.source.toString()) && nodeIds.has(link.target.toString())
    );

    return { nodes: filteredNodes, links: filteredLinks };
  }, [nodeTypeFilters]);

  // Build ECharts option
  const buildChartOption = useCallback((graphData: GraphData) => {
    const filteredData = filterData(graphData);

    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item' as const,
        backgroundColor: 'rgba(15, 23, 42, 0.95)',
        borderColor: '#334155',
        borderWidth: 1,
        textStyle: { color: '#94a3b8' },
        formatter: (params: any) => {
          if (params.dataType === 'node') {
            const node = params.data as GraphNode;
            return `
              <div style="padding: 12px;">
                <div style="font-size: 14px; font-weight: 600; color: #f8fafc; margin-bottom: 8px;">
                  ${node.name}
                </div>
                <div style="font-size: 12px; color: #64748b;">
                  Type: ${node.type}<br/>
                  Address: ${node.address || 'N/A'}
                </div>
                ${node.metrics ? `
                  <div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid #334155;">
                    Complexity: ${node.metrics.complexity || 'N/A'}<br/>
                    LOC: ${node.metrics.loc || 'N/A'}
                  </div>
                ` : ''}
              </div>
            `;
          } else {
            const link = params.data as GraphLink;
            return `
              <div style="padding: 12px;">
                <div style="font-size: 12px; color: #94a3b8;">
                  ${link.relationType || 'Dependency'}<br/>
                  Source → Target
                </div>
              </div>
            `;
          }
        },
      },
      series: [{
        type: 'graph',
        layout: 'force' as const,
        roam: true,
        draggable: true,
        data: filteredData.nodes.map(node => ({
          ...node,
          symbolSize: node.symbolSize || 30,
          itemStyle: {
            color: node.itemStyle?.color || getColorByType(node.type),
            borderColor: node.itemStyle?.borderColor || '#475569',
            borderWidth: 2,
          },
          label: {
            show: true,
            position: 'right' as const,
            fontSize: 12,
            color: '#f8fafc',
          },
        })),
        links: filteredData.links.map(link => ({
          ...link,
          lineStyle: {
            width: Math.min(1 + (link.value || 0) * GRAPH_CONFIG.LINK.WIDTH_MULTIPLIER, GRAPH_CONFIG.LINK.MAX_WIDTH),
            curveness: GRAPH_CONFIG.LINK.CURVENESS,
            color: getEdgeColor(link),
            opacity: getEdgeOpacity(link),
          },
          symbol: ['none', 'arrow'],
          symbolSize: [0, 6],
        })),
        force: {
          repulsion: GRAPH_CONFIG.FORCE.REPULSION,
          edgeLength: GRAPH_CONFIG.FORCE.EDGE_LENGTH,
          gravity: GRAPH_CONFIG.FORCE.GRAVITY,
          friction: GRAPH_CONFIG.FORCE.FRICTION,
        },
        emphasis: {
          focus: 'adjacency' as const,
          lineStyle: {
            width: GRAPH_CONFIG.EMPHASIS.LINE_WIDTH,
            shadowBlur: GRAPH_CONFIG.EMPHASIS.SHADOW_BLUR,
          },
        },
        animation: true,
      }],
    };
  }, [filterData]);

  // Get color by node type
  const getColorByType = (type: string): string => {
    const colors: Record<string, string> = {
      CLASS: '#3b82f6',
      INTERFACE: '#8b5cf6',
      ENUM: '#f59e0b',
      ABSTRACT_CLASS: '#ec4899',
      UTILITY: '#10b981',
      EXTERNAL: '#64748b',
    };
    return colors[type.toUpperCase()] || '#3b82f6';
  };

  // Get edge color based on relation type
  const getEdgeColor = (link: GraphLink): string => {
    if (link.isArchViolation) return '#ef4444';
    return '#475569';
  };

  // Get edge opacity
  const getEdgeOpacity = (link: GraphLink): number => {
    return link.isArchViolation ? 0.8 : 0.3;
  };

  // Handle node click
  const handleNodeClick = useCallback((params: any) => {
    if (params.dataType === 'node') {
      console.log('Node clicked:', params.data);
      // TODO: Emit event to show class inspector
    }
  }, []);

  if (!data) {
    return (
      <div style={{ 
        padding: '40px', 
        textAlign: 'center',
        color: '#94a3b8'
      }}>
        暂无图谱数据，请先加载项目
      </div>
    );
  }

  return (
    <div style={{ width: '100%', height: '100%' }}>
      <ReactECharts
        ref={chartRef}
        option={buildChartOption(data)}
        style={{ width: '100%', height: '100%' }}
        onEvents={{
          click: handleNodeClick,
        }}
        opts={{
          renderer: 'canvas',
          useDirtyRect: false,
        }}
      />
    </div>
  );
};

export default ForceGraphView;
