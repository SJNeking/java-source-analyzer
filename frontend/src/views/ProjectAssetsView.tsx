/**
 * ProjectAssetsView - React Component
 * 项目资产列表视图
 */

import React, { useMemo } from 'react';
import type { Asset } from '@types/project';
import { useAppStore } from '@store/app-store';

const ProjectAssetsView: React.FC = () => {
  const { fullAnalysisData } = useAppStore();
  const [filter, setFilter] = React.useState('');
  const [kindFilter, setKindFilter] = React.useState<string>('ALL');

  const assets = useMemo(() => {
    if (!fullAnalysisData?.assets) return [];
    
    let filtered = fullAnalysisData.assets as Asset[];

    if (kindFilter !== 'ALL') {
      filtered = filtered.filter(asset => asset.kind === kindFilter);
    }

    if (filter) {
      const lowerFilter = filter.toLowerCase();
      filtered = filtered.filter(asset => 
        asset.name.toLowerCase().includes(lowerFilter) ||
        asset.path.toLowerCase().includes(lowerFilter)
      );
    }

    return filtered.sort((a, b) => a.name.localeCompare(b.name));
  }, [fullAnalysisData, filter, kindFilter]);

  const kinds = useMemo(() => {
    if (!fullAnalysisData?.assets) return [];
    const uniqueKinds = new Set((fullAnalysisData.assets as Asset[]).map(a => a.kind));
    return Array.from(uniqueKinds);
  }, [fullAnalysisData]);

  if (!fullAnalysisData) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
        <h3>📂 暂无项目资产数据</h3>
        <p style={{ marginTop: '12px', fontSize: '13px' }}>
          请先加载项目进行分析
        </p>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', height: '100%', overflow: 'auto' }}>
      <h2 style={{ marginBottom: '24px', fontSize: '20px', color: '#f8fafc' }}>
        📂 项目资产
      </h2>

      {/* Stats */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
        gap: '12px',
        marginBottom: '24px'
      }}>
        <StatCard label="总资产" value={assets.length} color="#3b82f6" icon="📦" />
        {kinds.map(kind => (
          <StatCard 
            key={kind}
            label={kind} 
            value={assets.filter(a => a.kind === kind).length} 
            color="#8b5cf6"
            icon={getKindIcon(kind)}
          />
        ))}
      </div>

      {/* Filters */}
      <div style={{ 
        display: 'flex', 
        gap: '12px', 
        marginBottom: '20px',
        alignItems: 'center',
      }}>
        <input
          type="text"
          placeholder="🔍 搜索资产..."
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          style={{
            flex: 1,
            padding: '8px 12px',
            backgroundColor: '#0f172a',
            border: '1px solid #334155',
            borderRadius: '6px',
            color: '#f8fafc',
            fontSize: '13px',
            outline: 'none',
          }}
        />
        
        <select
          value={kindFilter}
          onChange={(e) => setKindFilter(e.target.value)}
          style={{
            padding: '8px 12px',
            backgroundColor: '#0f172a',
            border: '1px solid #334155',
            borderRadius: '6px',
            color: '#f8fafc',
            fontSize: '13px',
            outline: 'none',
            cursor: 'pointer',
          }}
        >
          <option value="ALL">全部类型</option>
          {kinds.map(kind => (
            <option key={kind} value={kind}>{kind}</option>
          ))}
        </select>
      </div>

      {/* Assets Table */}
      <div style={{
        backgroundColor: '#0f172a',
        border: '1px solid #334155',
        borderRadius: '8px',
        overflow: 'hidden',
      }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid #334155' }}>
              <th style={{ padding: '12px', textAlign: 'left', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>名称</th>
              <th style={{ padding: '12px', textAlign: 'left', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>类型</th>
              <th style={{ padding: '12px', textAlign: 'left', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>路径</th>
              <th style={{ padding: '12px', textAlign: 'right', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>行数</th>
              <th style={{ padding: '12px', textAlign: 'right', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>复杂度</th>
            </tr>
          </thead>
          <tbody>
            {assets.length === 0 ? (
              <tr>
                <td colSpan={5} style={{ padding: '40px', textAlign: 'center', color: '#64748b' }}>
                  没有找到匹配的资产
                </td>
              </tr>
            ) : (
              assets.map((asset, index) => (
                <tr 
                  key={index}
                  style={{ 
                    borderBottom: '1px solid #1e293b',
                    transition: 'background-color 0.2s',
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#1e293b'}
                  onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                >
                  <td style={{ padding: '12px', color: '#f8fafc', fontSize: '13px' }}>
                    {asset.name}
                  </td>
                  <td style={{ padding: '12px' }}>
                    <span style={{
                      padding: '4px 8px',
                      backgroundColor: getKindColor(asset.kind),
                      color: '#fff',
                      borderRadius: '4px',
                      fontSize: '11px',
                      fontWeight: 500,
                    }}>
                      {asset.kind}
                    </span>
                  </td>
                  <td style={{ padding: '12px', color: '#64748b', fontSize: '12px' }}>
                    {asset.path}
                  </td>
                  <td style={{ padding: '12px', color: '#f8fafc', fontSize: '13px', textAlign: 'right' }}>
                    {asset.lines || '-'}
                  </td>
                  <td style={{ padding: '12px', color: '#f8fafc', fontSize: '13px', textAlign: 'right' }}>
                    {asset.complexity || '-'}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

const StatCard: React.FC<{ label: string; value: number; color: string; icon: string }> = ({
  label, value, color, icon
}) => (
  <div style={{
    backgroundColor: '#0f172a',
    border: '1px solid #334155',
    borderRadius: '8px',
    padding: '16px',
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
  }}>
    <div style={{ fontSize: '24px' }}>{icon}</div>
    <div>
      <div style={{ fontSize: '24px', fontWeight: 600, color }}>{value}</div>
      <div style={{ fontSize: '12px', color: '#64748b', marginTop: '2px' }}>{label}</div>
    </div>
  </div>
);

const getKindIcon = (kind: string): string => {
  const icons: Record<string, string> = {
    CLASS: '🏗️',
    INTERFACE: '🔌',
    ENUM: '📋',
    METHOD: '⚙️',
    FIELD: '📦',
  };
  return icons[kind] || '📄';
};

const getKindColor = (kind: string): string => {
  const colors: Record<string, string> = {
    CLASS: '#3b82f6',
    INTERFACE: '#8b5cf6',
    ENUM: '#f59e0b',
    METHOD: '#10b981',
    FIELD: '#ec4899',
  };
  return colors[kind] || '#64748b';
};

export default ProjectAssetsView;
