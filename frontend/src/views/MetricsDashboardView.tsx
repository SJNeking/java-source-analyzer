/**
 * MetricsDashboardView - React Component
 * 代码指标仪表板
 */

import React from 'react';
import { useAppStore } from '@store/app-store';

const MetricsDashboardView: React.FC = () => {
  const { fullAnalysisData } = useAppStore();

  if (!fullAnalysisData) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
        <h3>📊 暂无代码指标数据</h3>
        <p style={{ marginTop: '12px', fontSize: '13px' }}>
          请先加载项目进行分析
        </p>
      </div>
    );
  }

  const metrics = (fullAnalysisData as any).code_metrics || {};

  return (
    <div style={{ padding: '20px', height: '100%', overflow: 'auto' }}>
      <h2 style={{ marginBottom: '24px', fontSize: '20px', color: '#f8fafc' }}>
        📊 代码指标
      </h2>

      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '16px',
        marginBottom: '32px'
      }}>
        <MetricCard 
          title="总行数" 
          value={metrics.totalLines || 0} 
          icon="📝"
          color="#3b82f6"
        />
        <MetricCard 
          title="代码行数" 
          value={metrics.codeLines || 0} 
          icon="💻"
          color="#10b981"
        />
        <MetricCard 
          title="注释行数" 
          value={metrics.commentLines || 0} 
          icon="📖"
          color="#8b5cf6"
        />
        <MetricCard 
          title="类数量" 
          value={metrics.classes || 0} 
          icon="🏗️"
          color="#f59e0b"
        />
        <MetricCard 
          title="方法数量" 
          value={metrics.methods || 0} 
          icon="⚙️"
          color="#ec4899"
        />
        <MetricCard 
          title="平均复杂度" 
          value={metrics.avgComplexity?.toFixed(2) || '0.00'} 
          icon="🔀"
          color="#ef4444"
        />
        <MetricCard 
          title="最大复杂度" 
          value={metrics.maxComplexity || 0} 
          icon="⚠️"
          color="#dc2626"
        />
      </div>

      {/* Complexity Distribution */}
      {metrics.complexityDistribution && (
        <div style={{
          backgroundColor: '#0f172a',
          border: '1px solid #334155',
          borderRadius: '8px',
          padding: '20px',
          marginTop: '24px',
        }}>
          <h3 style={{ margin: '0 0 16px 0', fontSize: '16px', color: '#f8fafc' }}>
            复杂度分布
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {Object.entries(metrics.complexityDistribution).map(([range, count]: [string, any]) => (
              <div key={range} style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <span style={{ fontSize: '13px', color: '#94a3b8', minWidth: '80px' }}>{range}</span>
                <div style={{ 
                  flex: 1, 
                  height: '24px', 
                  backgroundColor: '#1e293b',
                  borderRadius: '4px',
                  overflow: 'hidden',
                }}>
                  <div style={{
                    width: `${Math.min(count * 2, 100)}%`,
                    height: '100%',
                    backgroundColor: getComplexityColor(range),
                    transition: 'width 0.3s',
                  }} />
                </div>
                <span style={{ fontSize: '13px', color: '#f8fafc', minWidth: '40px' }}>{count}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

const MetricCard: React.FC<{ title: string; value: number | string; icon: string; color: string }> = ({
  title, value, icon, color
}) => (
  <div style={{
    backgroundColor: '#0f172a',
    border: '1px solid #334155',
    borderRadius: '8px',
    padding: '20px',
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
  }}>
    <div style={{ fontSize: '32px' }}>{icon}</div>
    <div>
      <div style={{ fontSize: '28px', fontWeight: 600, color }}>{value}</div>
      <div style={{ fontSize: '13px', color: '#64748b', marginTop: '4px' }}>{title}</div>
    </div>
  </div>
);

const getComplexityColor = (range: string): string => {
  if (range.includes('1-5')) return '#10b981';
  if (range.includes('6-10')) return '#f59e0b';
  if (range.includes('11-20')) return '#ef4444';
  return '#dc2626';
};

export default MetricsDashboardView;
