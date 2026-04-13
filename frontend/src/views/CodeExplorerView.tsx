/**
 * CodeExplorerView - React Component
 * 代码浏览器（最终视图）
 */

import React, { useMemo } from 'react';
import type { Asset } from '@/types/project';
import { useAppStore } from '@store/app-store';

const CodeExplorerView: React.FC = () => {
  const { fullAnalysisData } = useAppStore();
  const [selectedClass, setSelectedClass] = React.useState<string | null>(null);
  const [viewMode, setViewMode] = React.useState<'packages' | 'flat'>('packages');
  const [searchTerm, setSearchTerm] = React.useState('');
  const [activeTab, setActiveTab] = React.useState<'diagram' | 'source' | 'calls'>('diagram');

  const assets = useMemo(() => {
    if (!fullAnalysisData?.assets) return [];
    
    let filtered = fullAnalysisData.assets;
    
    if (searchTerm) {
      const lower = searchTerm.toLowerCase();
      filtered = filtered.filter(a => a.address.toLowerCase().includes(lower));
    }

    if (viewMode === 'packages') {
      // Group by package
      const grouped: Record<string, Asset[]> = {};
      filtered.forEach(asset => {
        const pkg = asset.address.split('.').slice(0, -1).join('.');
        if (!grouped[pkg]) grouped[pkg] = [];
        grouped[pkg].push(asset);
      });
      return grouped;
    }
    
    return filtered.sort((a, b) => a.address.localeCompare(b.address));
  }, [fullAnalysisData, searchTerm, viewMode]);

  const selectedAsset = useMemo(() => {
    if (!selectedClass || !fullAnalysisData?.assets) return null;
    return fullAnalysisData.assets.find(a => a.address === selectedClass);
  }, [selectedClass, fullAnalysisData]);

  if (!fullAnalysisData) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
        <h3>📂 暂无代码数据</h3>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', height: 'calc(100vh - 100px)', gap: '16px', padding: '20px' }}>
      {/* Left Panel - Tree */}
      <div style={{ 
        width: '300px', 
        backgroundColor: '#0f172a',
        border: '1px solid #334155',
        borderRadius: '8px',
        display: 'flex',
        flexDirection: 'column',
      }}>
        {/* Header */}
        <div style={{ padding: '12px', borderBottom: '1px solid #334155' }}>
          <div style={{ display: 'flex', gap: '8px', marginBottom: '8px' }}>
            <button
              onClick={() => setViewMode('packages')}
              style={{
                flex: 1,
                padding: '6px',
                backgroundColor: viewMode === 'packages' ? '#3b82f6' : 'transparent',
                color: viewMode === 'packages' ? '#fff' : '#94a3b8',
                border: '1px solid #334155',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '11px',
              }}
            >
              包视图
            </button>
            <button
              onClick={() => setViewMode('flat')}
              style={{
                flex: 1,
                padding: '6px',
                backgroundColor: viewMode === 'flat' ? '#3b82f6' : 'transparent',
                color: viewMode === 'flat' ? '#fff' : '#94a3b8',
                border: '1px solid #334155',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '11px',
              }}
            >
              列表视图
            </button>
          </div>
          <input
            type="text"
            placeholder="🔍 搜索..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{
              width: '100%',
              padding: '6px 10px',
              backgroundColor: '#1e293b',
              border: '1px solid #334155',
              borderRadius: '4px',
              color: '#f8fafc',
              fontSize: '12px',
            }}
          />
        </div>

        {/* Tree Content */}
        <div style={{ flex: 1, overflow: 'auto', padding: '8px' }}>
          {viewMode === 'packages' && typeof assets === 'object' ? (
            Object.entries(assets as Record<string, Asset[]>).map(([pkg, pkgAssets]) => (
              <div key={pkg} style={{ marginBottom: '12px' }}>
                <div style={{ 
                  fontSize: '11px', 
                  color: '#64748b',
                  fontWeight: 600,
                  marginBottom: '4px',
                  padding: '4px 8px',
                }}>
                  📦 {pkg.split('.').pop()}
                </div>
                {pkgAssets.map(asset => (
                  <div
                    key={asset.address}
                    onClick={() => setSelectedClass(asset.address)}
                    style={{
                      padding: '6px 8px',
                      backgroundColor: selectedClass === asset.address ? '#1e293b' : 'transparent',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontSize: '12px',
                      color: selectedClass === asset.address ? '#3b82f6' : '#f8fafc',
                    }}
                  >
                    {asset.address.split('.').pop()}
                  </div>
                ))}
              </div>
            ))
          ) : (
            <div>
              {(assets as Asset[]).map(asset => (
                <div
                  key={asset.address}
                  onClick={() => setSelectedClass(asset.address)}
                  style={{
                    padding: '6px 8px',
                    backgroundColor: selectedClass === asset.address ? '#1e293b' : 'transparent',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontSize: '12px',
                    color: selectedClass === asset.address ? '#3b82f6' : '#f8fafc',
                  }}
                >
                  {asset.address}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Middle Panel - Main Viewer */}
      <div style={{ 
        flex: 1,
        backgroundColor: '#0f172a',
        border: '1px solid #334155',
        borderRadius: '8px',
        display: 'flex',
        flexDirection: 'column',
      }}>
        {/* Tabs */}
        <div style={{ 
          display: 'flex', 
          borderBottom: '1px solid #334155',
          padding: '0 12px',
        }}>
          {(['diagram', 'source', 'calls'] as const).map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              style={{
                padding: '12px 16px',
                backgroundColor: 'transparent',
                color: activeTab === tab ? '#3b82f6' : '#94a3b8',
                border: 'none',
                borderBottom: activeTab === tab ? '2px solid #3b82f6' : '2px solid transparent',
                cursor: 'pointer',
                fontSize: '13px',
                fontWeight: activeTab === tab ? 600 : 400,
              }}
            >
              {tab === 'diagram' ? '📊 图表' : tab === 'source' ? '📝 源码' : '🔗 调用'}
            </button>
          ))}
        </div>

        {/* Tab Content */}
        <div style={{ flex: 1, padding: '20px', overflow: 'auto' }}>
          {!selectedAsset ? (
            <div style={{ textAlign: 'center', color: '#64748b', marginTop: '100px' }}>
              <div style={{ fontSize: '48px', marginBottom: '16px' }}>👈</div>
              <div>从左侧选择一个类查看</div>
            </div>
          ) : activeTab === 'diagram' ? (
            <div>
              <h3 style={{ color: '#f8fafc', marginBottom: '16px' }}>
                {selectedAsset.address}
              </h3>
              <div style={{ 
                padding: '20px',
                backgroundColor: '#1e293b',
                borderRadius: '8px',
                textAlign: 'center',
              }}>
                <div style={{ fontSize: '64px', marginBottom: '16px' }}>🏗️</div>
                <div style={{ color: '#94a3b8' }}>
                  类型: {selectedAsset.kind}<br/>
                  方法数: {(selectedAsset.methods_full || selectedAsset.methods || []).length}
                </div>
              </div>
            </div>
          ) : activeTab === 'source' ? (
            <div>
              <pre style={{
                padding: '16px',
                backgroundColor: '#020617',
                borderRadius: '6px',
                fontSize: '12px',
                color: '#e2e8f0',
                overflow: 'auto',
                maxHeight: '500px',
              }}>
                {`// ${selectedAsset.address}\n// 源代码查看功能需要后端支持\n\npublic class ${selectedAsset.address.split('.').pop()} {\n  // ...\n}`}
              </pre>
            </div>
          ) : (
            <div style={{ textAlign: 'center', color: '#64748b', marginTop: '100px' }}>
              <div style={{ fontSize: '48px', marginBottom: '16px' }}>🔗</div>
              <div>调用关系图（使用MethodCallView查看）</div>
            </div>
          )}
        </div>
      </div>

      {/* Right Panel - Context */}
      <div style={{ 
        width: '280px',
        backgroundColor: '#0f172a',
        border: '1px solid #334155',
        borderRadius: '8px',
        padding: '16px',
        overflow: 'auto',
      }}>
        <h3 style={{ margin: '0 0 16px 0', fontSize: '14px', color: '#f8fafc' }}>
          📋 详细信息
        </h3>

        {selectedAsset ? (
          <div>
            <div style={{ marginBottom: '16px' }}>
              <div style={{ fontSize: '11px', color: '#64748b', marginBottom: '4px' }}>完整路径</div>
              <div style={{ fontSize: '12px', color: '#f8fafc', fontFamily: 'monospace', wordBreak: 'break-all' }}>
                {selectedAsset.address}
              </div>
            </div>

            <div style={{ marginBottom: '16px' }}>
              <div style={{ fontSize: '11px', color: '#64748b', marginBottom: '4px' }}>类型</div>
              <div style={{ fontSize: '12px', color: '#f8fafc' }}>
                {selectedAsset.kind}
              </div>
            </div>

            {(selectedAsset.methods_full || selectedAsset.methods || []).length > 0 && (
              <div>
                <div style={{ fontSize: '11px', color: '#64748b', marginBottom: '8px' }}>
                  方法列表 ({(selectedAsset.methods_full || selectedAsset.methods || []).length})
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  {(selectedAsset.methods_full || selectedAsset.methods || []).slice(0, 10).map((method: any, idx: number) => (
                    <div key={idx} style={{
                      padding: '8px',
                      backgroundColor: '#1e293b',
                      borderRadius: '4px',
                      fontSize: '11px',
                    }}>
                      <div style={{ color: '#3b82f6', fontFamily: 'monospace' }}>
                        {method.name}()
                      </div>
                      {method.description && (
                        <div style={{ color: '#64748b', fontSize: '10px', marginTop: '2px' }}>
                          {method.description.substring(0, 60)}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        ) : (
          <div style={{ textAlign: 'center', color: '#64748b', marginTop: '100px' }}>
            <div style={{ fontSize: '32px', marginBottom: '8px' }}>📄</div>
            <div style={{ fontSize: '12px' }}>选择类查看详情</div>
          </div>
        )}
      </div>
    </div>
  );
};

export default CodeExplorerView;
