/**
 * MethodCallView - React Component
 * 方法调用桑基图可视化
 */

import React, { useRef, useEffect, useMemo, useCallback } from 'react';
import ReactECharts from 'echarts-for-react';
import type { EChartsOption } from 'echarts';
import type { Asset, MethodAsset } from '@/types/project';
import { useAppStore } from '@store/app-store';

interface MethodCallNode {
  id: string;
  name: string;
  className: string;
  category: 'internal' | 'external';
  value?: number;
}

interface MethodCallLink {
  source: string;
  target: string;
  value: number;
  calls?: string[];
}

const MethodCallView: React.FC = () => {
  const { fullAnalysisData } = useAppStore();
  const [selectedClass, setSelectedClass] = React.useState<string | null>(null);
  const chartRef = useRef<ReactECharts>(null);

  // Get all classes with methods
  const classes = useMemo(() => {
    if (!fullAnalysisData?.assets) return [];
    return fullAnalysisData.assets.filter(asset => 
      asset.kind === 'CLASS' && (asset.methods_full || asset.methods || []).length > 0
    );
  }, [fullAnalysisData]);

  // Build Sankey diagram data for selected class
  const sankeyData = useMemo(() => {
    if (!selectedClass || !fullAnalysisData?.assets) return null;

    const asset = fullAnalysisData.assets.find(a => a.address === selectedClass);
    if (!asset) return null;

    const methods = asset.methods_full || asset.methods || [];
    if (methods.length === 0) return null;

    // Build nodes
    const nodes: MethodCallNode[] = methods.map(method => ({
      id: method.address,
      name: method.name,
      className: asset.address.split('.').pop() || '',
      category: 'internal',
      value: 0,
    }));

    // Build links
    const links: MethodCallLink[] = [];

    methods.forEach(method => {
      const keyStmts = (method as any).key_statements || [];
      const extCalls = keyStmts.filter((s: any) => s.type === 'EXTERNAL_CALL');

      extCalls.forEach((call: any) => {
        const targetMethod = call.target_method || call.target || '';
        const isInternal = methods.some(m => m.address === targetMethod);

        if (isInternal) {
          const existingLink = links.find(l => l.source === method.address && l.target === targetMethod);
          if (existingLink) {
            existingLink.value += 1;
            if (existingLink.calls) {
              existingLink.calls.push(call.description || `${method.name}() → ${targetMethod.split('.').pop()}`);
            }
          } else {
            links.push({
              source: method.address,
              target: targetMethod,
              value: 1,
              calls: [call.description || `${method.name}() → ${targetMethod.split('.').pop()}`],
            });
          }
        }
      });
    });

    // Update node values based on link counts
    nodes.forEach(node => {
      const incoming = links.filter(l => l.target === node.id).reduce((sum, l) => sum + l.value, 0);
      const outgoing = links.filter(l => l.source === node.id).reduce((sum, l) => sum + l.value, 0);
      node.value = incoming + outgoing;
    });

    return { nodes, links, asset };
  }, [selectedClass, fullAnalysisData]);

  // Build ECharts option
  const buildChartOption = useCallback((data: { nodes: MethodCallNode[]; links: MethodCallLink[]; asset: Asset }): EChartsOption => {
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(15, 23, 42, 0.95)',
        borderColor: '#334155',
        textStyle: { color: '#f8fafc' },
        formatter: (params: any) => {
          if (params.dataType === 'node') {
            const node = params.data as MethodCallNode;
            const incoming = data.links.filter(l => l.target === node.id).length;
            const outgoing = data.links.filter(l => l.source === node.id).length;
            return `
              <div style="padding: 8px;">
                <div style="font-weight: 600; margin-bottom: 4px;">${node.className}.${node.name}()</div>
                <div style="font-size: 12px; color: #94a3b8;">入度: ${incoming} | 出度: ${outgoing}</div>
              </div>
            `;
          } else if (params.dataType === 'edge') {
            const link = params.data as MethodCallLink;
            const callDetails = link.calls ? link.calls.join('<br/>') : '';
            return `
              <div style="padding: 8px;">
                <div style="font-weight: 600; margin-bottom: 4px;">调用关系</div>
                <div style="font-size: 12px; color: #94a3b8;">调用次数: ${link.value}</div>
                ${callDetails ? `<div style="margin-top: 8px; font-size: 11px; color: #e2e8f0;">${callDetails}</div>` : ''}
              </div>
            `;
          }
          return '';
        },
      },
      series: [
        {
          type: 'sankey',
          layout: 'none',
          emphasis: {
            focus: 'adjacency',
          },
          data: data.nodes.map(node => ({
            id: node.id,
            name: node.name,
            value: node.value,
            itemStyle: {
              color: node.category === 'internal' ? '#3b82f6' : '#8b5cf6',
              borderColor: '#475569',
              borderWidth: 2,
            },
            label: {
              color: '#f8fafc',
              fontSize: 11,
              formatter: `{b}`,
            },
          })),
          links: data.links.map(link => ({
            source: link.source,
            target: link.target,
            value: link.value,
            lineStyle: {
              color: 'gradient',
              curveness: 0.5,
              opacity: 0.3 + (link.value / Math.max(...data.links.map(l => l.value))) * 0.5,
            },
          })),
          lineStyle: {
            color: 'source',
            curveness: 0.5,
          },
          label: {
            position: 'right',
            distance: 8,
          },
          levels: [
            {
              depth: 0,
              itemStyle: {
                color: '#3b82f6',
              },
              lineStyle: {
                color: 'source',
                opacity: 0.6,
              },
            },
            {
              depth: 1,
              itemStyle: {
                color: '#10b981',
              },
              lineStyle: {
                color: 'source',
                opacity: 0.6,
              },
            },
          ],
        },
      ],
    };
  }, []);

  if (!fullAnalysisData) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
        <h3>🔗 暂无方法调用数据</h3>
        <p style={{ marginTop: '12px', fontSize: '13px' }}>
          请先加载项目进行分析
        </p>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', height: '100%', overflow: 'auto' }}>
      <h2 style={{ marginBottom: '24px', fontSize: '20px', color: '#f8fafc' }}>
        🔗 方法调用关系
      </h2>

      {!selectedClass ? (
        /* Class Selection Grid */
        <div>
          <div style={{ 
            display: 'grid', 
            gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
            gap: '16px',
          }}>
            {classes.slice(0, 50).map(asset => {
              const methods = asset.methods_full || asset.methods || [];
              return (
                <div
                  key={asset.address}
                  onClick={() => setSelectedClass(asset.address)}
                  style={{
                    padding: '16px',
                    backgroundColor: '#0f172a',
                    border: '1px solid #334155',
                    borderRadius: '8px',
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.borderColor = '#3b82f6';
                    e.currentTarget.style.transform = 'translateY(-2px)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.borderColor = '#334155';
                    e.currentTarget.style.transform = 'translateY(0)';
                  }}
                >
                  <div style={{ 
                    fontSize: '14px', 
                    fontWeight: 600, 
                    color: '#f8fafc', 
                    marginBottom: '8px' 
                  }}>
                    {asset.address.split('.').pop()}
                  </div>
                  <div style={{ 
                    fontSize: '11px', 
                    color: '#64748b', 
                    fontFamily: 'monospace',
                    marginBottom: '8px',
                  }}>
                    {asset.address}
                  </div>
                  <div style={{ 
                    display: 'flex', 
                    gap: '8px', 
                    fontSize: '10px',
                  }}>
                    <span style={{ color: '#3b82f6' }}>
                      📦 {methods.length} 个方法
                    </span>
                  </div>
                </div>
              );
            })}
          </div>

          {classes.length > 50 && (
            <div style={{ 
              textAlign: 'center', 
              marginTop: '20px', 
              color: '#64748b',
              fontSize: '12px',
            }}>
              显示前 50 个类，共 {classes.length} 个
            </div>
          )}
        </div>
      ) : (
        /* Sankey Diagram */
        <div>
          {/* Back Button */}
          <button
            onClick={() => setSelectedClass(null)}
            style={{
              padding: '8px 16px',
              backgroundColor: '#1e293b',
              color: '#f8fafc',
              border: '1px solid #334155',
              borderRadius: '6px',
              cursor: 'pointer',
              fontSize: '13px',
              marginBottom: '16px',
            }}
          >
            ← 返回类列表
          </button>

          {/* Selected Class Info */}
          {sankeyData && (
            <div style={{
              padding: '12px 16px',
              backgroundColor: '#0f172a',
              border: '1px solid #334155',
              borderRadius: '6px',
              marginBottom: '16px',
            }}>
              <div style={{ fontSize: '14px', fontWeight: 600, color: '#f8fafc' }}>
                {sankeyData.asset.address}
              </div>
              <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>
                方法数: {sankeyData.nodes.length} | 调用关系: {sankeyData.links.length}
              </div>
            </div>
          )}

          {/* Sankey Chart */}
          {sankeyData ? (
            <div style={{ 
              height: '600px',
              backgroundColor: '#0f172a',
              border: '1px solid #334155',
              borderRadius: '8px',
              padding: '16px',
            }}>
              <ReactECharts
                ref={chartRef}
                option={buildChartOption(sankeyData)}
                style={{ width: '100%', height: '100%' }}
                opts={{ renderer: 'canvas' }}
              />
            </div>
          ) : (
            <div style={{ 
              padding: '40px', 
              textAlign: 'center', 
              color: '#64748b',
              backgroundColor: '#0f172a',
              border: '1px solid #334155',
              borderRadius: '8px',
            }}>
              <div style={{ fontSize: '48px', marginBottom: '16px' }}>📭</div>
              <div>该类没有方法或方法间没有调用关系</div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default MethodCallView;
