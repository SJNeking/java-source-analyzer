/**
 * ApiEndpointView - React Component
 * API端点视图
 */

import React, { useMemo } from 'react';
import { useAppStore } from '@store/app-store';

const ApiEndpointView: React.FC = () => {
  const { fullAnalysisData } = useAppStore();

  const endpoints = useMemo(() => {
    if (!fullAnalysisData?.assets) return [];
    
    return fullAnalysisData.assets.filter(asset =>
      asset.kind === 'CLASS' &&
      (asset.address.includes('.controller.') ||
       asset.address.includes('.api.') ||
       (asset as any).annotations?.some((a: string) => a.includes('Controller')))
    );
  }, [fullAnalysisData]);

  if (!fullAnalysisData) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
        <h3>🌐 暂无API端点数据</h3>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', height: '100%', overflow: 'auto' }}>
      <h2 style={{ marginBottom: '24px', fontSize: '20px', color: '#f8fafc' }}>
        🌐 API端点
      </h2>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        {endpoints.map(controller => {
          const methods = controller.methods_full || controller.methods || [];
          return (
            <div key={controller.address} style={{
              backgroundColor: '#0f172a',
              border: '1px solid #334155',
              borderRadius: '8px',
              overflow: 'hidden',
            }}>
              <div style={{
                padding: '14px',
                borderBottom: '1px solid #334155',
                backgroundColor: '#1e293b',
              }}>
                <div style={{ fontSize: '14px', fontWeight: 600, color: '#f8fafc' }}>
                  🌐 {controller.address.split('.').pop()}
                </div>
                <div style={{ fontSize: '11px', color: '#64748b', marginTop: '4px' }}>
                  {controller.address}
                </div>
              </div>

              <div style={{ padding: '12px' }}>
                {methods.length > 0 ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {methods.map((method: any, idx: number) => (
                      <div key={idx} style={{
                        padding: '10px',
                        backgroundColor: '#1e293b',
                        borderRadius: '6px',
                        fontSize: '12px',
                      }}>
                        <div style={{ 
                          fontFamily: 'monospace',
                          color: '#10b981',
                          marginBottom: '4px',
                        }}>
                          GET /api/{method.name.toLowerCase()}
                        </div>
                        <div style={{ color: '#f8fafc' }}>
                          {method.name}()
                        </div>
                        {method.description && (
                          <div style={{ color: '#64748b', fontSize: '11px', marginTop: '4px' }}>
                            {method.description}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <div style={{ textAlign: 'center', color: '#64748b', padding: '20px' }}>
                    没有公开方法
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default ApiEndpointView;
