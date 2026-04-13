/**
 * CallChainView - React Component
 * 调用链路分层泳道图
 */

import React, { useMemo } from 'react';
import type { Asset } from '@/types/project';
import { useAppStore } from '@store/app-store';

interface CallChainNode {
  id: string;
  name: string;
  layer: 'controller' | 'service' | 'repository' | 'external';
  className: string;
  methodName: string;
  qualityIssues?: { critical: number; major: number; minor: number };
}

interface CallChainLink {
  source: string;
  target: string;
  type: 'normal' | 'violation' | 'risk';
  label?: string;
}

const CallChainView: React.FC = () => {
  const { fullAnalysisData } = useAppStore();
  const [selectedClass, setSelectedClass] = React.useState<string | null>(null);
  const [selectedMethod, setSelectedMethod] = React.useState<string | null>(null);

  // Get all controller classes
  const controllers = useMemo(() => {
    if (!fullAnalysisData?.assets) return [];
    return fullAnalysisData.assets.filter(asset =>
      asset.kind === 'CLASS' &&
      (asset.address.includes('.controller.') ||
       asset.address.includes('.api.') ||
       (asset as any).annotations?.some((a: string) => a.includes('Controller')))
    );
  }, [fullAnalysisData]);

  // Get methods for selected class
  const classMethods = useMemo(() => {
    if (!selectedClass || !fullAnalysisData?.assets) return [];
    const asset = fullAnalysisData.assets.find(a => a.address === selectedClass);
    if (!asset) return [];
    
    const methods = asset.methods_full || asset.methods || [];
    return methods.filter((m: any) => m.modifiers?.includes('public')).slice(0, 30);
  }, [selectedClass, fullAnalysisData]);

  // Build call chain for selected method
  const callChain = useMemo(() => {
    if (!selectedMethod || !fullAnalysisData?.assets) return null;

    const nodes: CallChainNode[] = [];
    const links: CallChainLink[] = [];

    // Find the entry method
    let entryMethod: any = null;
    let entryAsset: Asset | null = null;

    for (const asset of fullAnalysisData.assets) {
      const methods = asset.methods_full || asset.methods || [];
      const found = methods.find((m: any) => m.address === selectedMethod);
      if (found) {
        entryMethod = found;
        entryAsset = asset;
        break;
      }
    }

    if (!entryMethod || !entryAsset) return null;

    // Add entry node (Controller layer)
    nodes.push({
      id: entryMethod.address,
      name: `${entryAsset.address.split('.').pop()}.${entryMethod.name}()`,
      layer: 'controller',
      className: entryAsset.address,
      methodName: entryMethod.name,
    });

    // Traverse method calls to build chain
    const visited = new Set<string>();
    const traverse = (methodAddr: string, currentLayer: number) => {
      if (visited.has(methodAddr)) return;
      visited.add(methodAddr);

      // Find method in assets
      for (const asset of fullAnalysisData.assets || []) {
        const methods = asset.methods_full || asset.methods || [];
        const method = methods.find((m: any) => m.address === methodAddr);
        
        if (method) {
          const keyStmts = (method as any).key_statements || [];
          
          keyStmts.forEach((stmt: any) => {
            if (stmt.type === 'EXTERNAL_CALL') {
              const targetAddr = stmt.target_method || stmt.target || '';
              
              // Determine target layer
              let targetLayer: CallChainNode['layer'] = 'external';
              if (targetAddr.includes('.service.')) targetLayer = 'service';
              else if (targetAddr.includes('.repository.') || targetAddr.includes('.dao.')) targetLayer = 'repository';

              // Add target node
              const targetExists = nodes.find(n => n.id === targetAddr);
              if (!targetExists && targetAddr) {
                nodes.push({
                  id: targetAddr,
                  name: stmt.description || targetAddr.split('.').pop() || '',
                  layer: targetLayer,
                  className: targetAddr.split('.').slice(0, -1).join('.'),
                  methodName: targetAddr.split('.').pop() || '',
                });
              }

              // Add link
              links.push({
                source: methodAddr,
                target: targetAddr,
                type: 'normal',
                label: stmt.description,
              });

              // Recursively traverse
              if (currentLayer < 5) {
                traverse(targetAddr, currentLayer + 1);
              }
            }
          });
        }
      }
    };

    traverse(entryMethod.address, 0);

    return { nodes, links, entryMethod, entryAsset };
  }, [selectedMethod, fullAnalysisData]);

  if (!fullAnalysisData) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
        <h3>🔗 暂无调用链路数据</h3>
        <p style={{ marginTop: '12px', fontSize: '13px' }}>
          请先加载项目进行分析
        </p>
      </div>
    );
  }

  // Step 1: Select Controller
  if (!selectedClass) {
    return (
      <div style={{ padding: '40px' }}>
        <h2 style={{ marginBottom: '24px', fontSize: '20px', color: '#f8fafc' }}>
          🔗 调用链路分析
        </h2>

        <div style={{ marginBottom: '16px', fontSize: '12px', color: '#64748b' }}>
          检测到 {controllers.length} 个 Controller 类
        </div>

        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))',
          gap: '16px',
        }}>
          {controllers.slice(0, 20).map(controller => {
            const methods = controller.methods_full || controller.methods || [];
            return (
              <div
                key={controller.address}
                onClick={() => setSelectedClass(controller.address)}
                style={{
                  padding: '16px',
                  backgroundColor: '#0f172a',
                  border: '1px solid #334155',
                  borderRadius: '8px',
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                }}
                onMouseEnter={(e) => e.currentTarget.style.borderColor = '#3b82f6'}
                onMouseLeave={(e) => e.currentTarget.style.borderColor = '#334155'}
              >
                <div style={{ 
                  fontSize: '13px', 
                  fontWeight: 600, 
                  color: '#f8fafc', 
                  marginBottom: '8px' 
                }}>
                  🌐 {controller.address.split('.').pop()}
                </div>
                <div style={{ 
                  fontSize: '10px', 
                  color: '#64748b', 
                  fontFamily: 'monospace',
                  marginBottom: '8px',
                }}>
                  {controller.address}
                </div>
                <div style={{ fontSize: '11px', color: '#3b82f6' }}>
                  ⚙️ {methods.length} 个方法
                </div>
              </div>
            );
          })}
        </div>

        {controllers.length > 20 && (
          <div style={{ 
            textAlign: 'center', 
            marginTop: '20px', 
            color: '#64748b',
            fontSize: '12px',
          }}>
            显示前 20 个 Controller
          </div>
        )}
      </div>
    );
  }

  // Step 2: Select Method
  if (!selectedMethod) {
    return (
      <div style={{ padding: '40px' }}>
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
          ← 返回 Controller 列表
        </button>

        <h2 style={{ marginBottom: '24px', fontSize: '20px', color: '#f8fafc' }}>
          {selectedClass.split('.').pop()} - 选择入口方法
        </h2>

        <div style={{ display: 'grid', gap: '12px' }}>
          {classMethods.map((method: any) => (
            <div
              key={method.address}
              onClick={() => setSelectedMethod(method.address)}
              style={{
                padding: '14px',
                backgroundColor: '#0f172a',
                border: '1px solid #334155',
                borderRadius: '6px',
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
              onMouseEnter={(e) => e.currentTarget.style.borderColor = '#3b82f6'}
              onMouseLeave={(e) => e.currentTarget.style.borderColor = '#334155'}
            >
              <div style={{ 
                fontFamily: 'monospace', 
                fontSize: '12px', 
                color: '#3b82f6',
                marginBottom: '4px',
              }}>
                {method.name}()
              </div>
              {method.description && (
                <div style={{ 
                  fontSize: '11px', 
                  color: '#64748b',
                }}>
                  {method.description.substring(0, 80)}
                  {method.description.length > 80 ? '...' : ''}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    );
  }

  // Step 3: Display Call Chain
  return (
    <div style={{ padding: '40px' }}>
      <button
        onClick={() => setSelectedMethod(null)}
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
        ← 返回方法列表
      </button>

      <h2 style={{ marginBottom: '24px', fontSize: '20px', color: '#f8fafc' }}>
        🔗 调用链路泳道图
      </h2>

      {callChain ? (
        <div>
          {/* Layer Headers */}
          <div style={{ 
            display: 'grid', 
            gridTemplateColumns: 'repeat(4, 1fr)',
            gap: '16px',
            marginBottom: '16px',
          }}>
            {(['controller', 'service', 'repository', 'external'] as const).map(layer => (
              <div key={layer} style={{
                padding: '12px',
                backgroundColor: getLayerColor(layer),
                borderRadius: '6px',
                textAlign: 'center',
                fontSize: '13px',
                fontWeight: 600,
                color: '#fff',
              }}>
                {getLayerIcon(layer)} {getLayerLabel(layer)}
              </div>
            ))}
          </div>

          {/* Nodes by Layer */}
          <div style={{ 
            display: 'grid', 
            gridTemplateColumns: 'repeat(4, 1fr)',
            gap: '16px',
          }}>
            {(['controller', 'service', 'repository', 'external'] as const).map(layer => {
              const layerNodes = callChain.nodes.filter(n => n.layer === layer);
              return (
                <div key={layer} style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {layerNodes.map(node => (
                    <div
                      key={node.id}
                      style={{
                        padding: '12px',
                        backgroundColor: '#0f172a',
                        border: `2px solid ${getLayerColor(node.layer)}`,
                        borderRadius: '6px',
                        fontSize: '11px',
                      }}
                    >
                      <div style={{ fontWeight: 600, color: '#f8fafc', marginBottom: '4px' }}>
                        {node.methodName}()
                      </div>
                      <div style={{ color: '#64748b', fontSize: '10px' }}>
                        {node.className.split('.').pop()}
                      </div>
                    </div>
                  ))}
                </div>
              );
            })}
          </div>

          {/* Links Summary */}
          <div style={{
            marginTop: '24px',
            padding: '16px',
            backgroundColor: '#0f172a',
            border: '1px solid #334155',
            borderRadius: '8px',
          }}>
            <h3 style={{ margin: '0 0 12px 0', fontSize: '14px', color: '#f8fafc' }}>
              调用关系 ({callChain.links.length} 条)
            </h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {callChain.links.map((link, idx) => (
                <div key={idx} style={{
                  padding: '8px 12px',
                  backgroundColor: '#1e293b',
                  borderRadius: '4px',
                  fontSize: '12px',
                  color: '#e2e8f0',
                }}>
                  {link.source.split('.').pop()} → {link.target.split('.').pop()}
                  {link.label && (
                    <span style={{ color: '#64748b', marginLeft: '8px' }}>
                      ({link.label})
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>
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
          <div>未找到调用链路</div>
        </div>
      )}
    </div>
  );
};

const getLayerLabel = (layer: string): string => {
  const labels: Record<string, string> = {
    controller: 'Controller',
    service: 'Service',
    repository: 'Repository',
    external: 'External',
  };
  return labels[layer] || layer;
};

const getLayerIcon = (layer: string): string => {
  const icons: Record<string, string> = {
    controller: '🌐',
    service: '⚙️',
    repository: '💾',
    external: '🔌',
  };
  return icons[layer] || '📄';
};

const getLayerColor = (layer: string): string => {
  const colors: Record<string, string> = {
    controller: '#3b82f6',
    service: '#8b5cf6',
    repository: '#10b981',
    external: '#64748b',
  };
  return colors[layer] || '#475569';
};

export default CallChainView;
