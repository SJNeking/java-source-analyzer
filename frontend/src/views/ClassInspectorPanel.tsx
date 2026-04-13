/**
 * ClassInspectorPanel - React Component  
 * 类检查器面板
 */

import React, { useMemo } from 'react';
import type { Asset } from '@types/project';
import { useAppStore } from '@store/app-store';

const ClassInspectorPanel: React.FC = () => {
  const { fullAnalysisData } = useAppStore();
  const [selectedClass, setSelectedClass] = React.useState<string | null>(null);

  const classes = useMemo(() => {
    if (!fullAnalysisData?.assets) return [];
    return fullAnalysisData.assets.filter(a => a.kind === 'CLASS');
  }, [fullAnalysisData]);

  const selectedAsset = useMemo(() => {
    if (!selectedClass || !fullAnalysisData?.assets) return null;
    return fullAnalysisData.assets.find(a => a.address === selectedClass);
  }, [selectedClass, fullAnalysisData]);

  if (!fullAnalysisData) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
        <h3>🔍 暂无类数据</h3>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', height: '100%', overflow: 'auto' }}>
      <h2 style={{ marginBottom: '24px', fontSize: '20px', color: '#f8fafc' }}>
        🔍 类检查器
      </h2>

      <div style={{ display: 'grid', gridTemplateColumns: '300px 1fr', gap: '20px', height: 'calc(100vh - 150px)' }}>
        {/* Class List */}
        <div style={{
          backgroundColor: '#0f172a',
          border: '1px solid #334155',
          borderRadius: '8px',
          overflow: 'auto',
        }}>
          <div style={{ padding: '12px', borderBottom: '1px solid #334155' }}>
            <input
              type="text"
              placeholder="🔍 搜索类..."
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
          <div>
            {classes.map(asset => (
              <div
                key={asset.address}
                onClick={() => setSelectedClass(asset.address)}
                style={{
                  padding: '10px 12px',
                  backgroundColor: selectedClass === asset.address ? '#1e293b' : 'transparent',
                  borderLeft: selectedClass === asset.address ? '3px solid #3b82f6' : '3px solid transparent',
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                }}
              >
                <div style={{ fontSize: '12px', color: '#f8fafc' }}>
                  {asset.address.split('.').pop()}
                </div>
                <div style={{ fontSize: '10px', color: '#64748b', marginTop: '2px' }}>
                  {asset.address}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Class Details */}
        <div style={{
          backgroundColor: '#0f172a',
          border: '1px solid #334155',
          borderRadius: '8px',
          padding: '20px',
          overflow: 'auto',
        }}>
          {selectedAsset ? (
            <div>
              <h3 style={{ margin: '0 0 16px 0', fontSize: '16px', color: '#f8fafc' }}>
                {selectedAsset.address}
              </h3>

              {/* Methods */}
              {(selectedAsset.methods_full || selectedAsset.methods || []).length > 0 && (
                <div style={{ marginTop: '20px' }}>
                  <h4 style={{ fontSize: '14px', color: '#94a3b8', marginBottom: '12px' }}>
                    方法 ({(selectedAsset.methods_full || selectedAsset.methods || []).length})
                  </h4>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {(selectedAsset.methods_full || selectedAsset.methods || []).map((method: any, idx: number) => (
                      <div key={idx} style={{
                        padding: '10px',
                        backgroundColor: '#1e293b',
                        borderRadius: '6px',
                        fontSize: '12px',
                      }}>
                        <div style={{ color: '#3b82f6', fontFamily: 'monospace', marginBottom: '4px' }}>
                          {method.modifiers?.join(' ')} {method.returnType} {method.name}()
                        </div>
                        {method.description && (
                          <div style={{ color: '#64748b', fontSize: '11px' }}>
                            {method.description}
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
              <div style={{ fontSize: '48px', marginBottom: '16px' }}>📋</div>
              <div>选择一个类查看详情</div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ClassInspectorPanel;
