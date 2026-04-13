/**
 * ComponentExplorerView - React Component
 * 组件浏览器
 */

import React, { useMemo } from 'react';
import type { Asset } from '@/types/project';
import { useAppStore } from '@store/app-store';

const ComponentExplorerView: React.FC = () => {
  const { fullAnalysisData } = useAppStore();
  const [filter, setFilter] = React.useState('');

  const components = useMemo(() => {
    if (!fullAnalysisData?.assets) return [];
    
    let filtered = fullAnalysisData.assets;
    
    if (filter) {
      const lower = filter.toLowerCase();
      filtered = filtered.filter(a => 
        a.address.toLowerCase().includes(lower) ||
        (a.kind === 'CLASS' && a.address.split('.').pop()?.toLowerCase().includes(lower))
      );
    }

    return filtered.sort((a, b) => a.address.localeCompare(b.address));
  }, [fullAnalysisData, filter]);

  if (!fullAnalysisData) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
        <h3>📦 暂无组件数据</h3>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', height: '100%', overflow: 'auto' }}>
      <h2 style={{ marginBottom: '24px', fontSize: '20px', color: '#f8fafc' }}>
        📦 组件浏览器
      </h2>

      <input
        type="text"
        placeholder="🔍 搜索组件..."
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        style={{
          width: '100%',
          padding: '10px 14px',
          backgroundColor: '#0f172a',
          border: '1px solid #334155',
          borderRadius: '6px',
          color: '#f8fafc',
          fontSize: '13px',
          marginBottom: '20px',
        }}
      />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '12px' }}>
        {components.map(asset => {
          const methods = asset.methods_full || asset.methods || [];
          return (
            <div key={asset.address} style={{
              padding: '14px',
              backgroundColor: '#0f172a',
              border: '1px solid #334155',
              borderRadius: '6px',
            }}>
              <div style={{ 
                fontSize: '13px', 
                fontWeight: 600, 
                color: '#f8fafc',
                marginBottom: '4px',
              }}>
                {getKindIcon(asset.kind)} {asset.address.split('.').pop()}
              </div>
              <div style={{ 
                fontSize: '11px', 
                color: '#64748b',
                fontFamily: 'monospace',
                marginBottom: '8px',
              }}>
                {asset.address}
              </div>
              <div style={{ fontSize: '11px', color: '#94a3b8' }}>
                类型: {asset.kind} | 方法: {methods.length}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

const getKindIcon = (kind: string): string => {
  const icons: Record<string, string> = {
    CLASS: '🏗️',
    INTERFACE: '🔌',
    ENUM: '📋',
  };
  return icons[kind] || '📄';
};

export default ComponentExplorerView;
