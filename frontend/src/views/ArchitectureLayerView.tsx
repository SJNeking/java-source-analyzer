/**
 * ArchitectureLayerView - React Component
 * 架构分层视图
 */

import React, { useMemo } from 'react';
import type { Asset } from '@types/project';
import { useAppStore } from '@store/app-store';

interface ArchitectureLayer {
  name: string;
  icon: string;
  color: string;
  classes: Asset[];
  description: string;
}

const ArchitectureLayerView: React.FC = () => {
  const { fullAnalysisData } = useAppStore();
  const [selectedLayer, setSelectedLayer] = React.useState<string | null>(null);

  // Detect architecture layers
  const layers = useMemo(() => {
    if (!fullAnalysisData?.assets) return [];

    const assets = fullAnalysisData.assets;
    const detectedLayers: ArchitectureLayer[] = [];

    // Detect Controller/API layer
    const controllers = assets.filter(a => 
      a.address.includes('.controller.') || 
      a.address.includes('.api.') ||
      (a as any).annotations?.some((ann: string) => ann.includes('Controller'))
    );
    if (controllers.length > 0) {
      detectedLayers.push({
        name: 'Controller / API',
        icon: '🌐',
        color: '#3b82f6',
        classes: controllers,
        description: '处理HTTP请求和API端点',
      });
    }

    // Detect Service layer
    const services = assets.filter(a => 
      a.address.includes('.service.') ||
      a.address.includes('.biz.') ||
      (a as any).annotations?.some((ann: string) => ann.includes('Service'))
    );
    if (services.length > 0) {
      detectedLayers.push({
        name: 'Service / Business',
        icon: '⚙️',
        color: '#8b5cf6',
        classes: services,
        description: '业务逻辑和服务实现',
      });
    }

    // Detect Repository/DAO layer
    const repositories = assets.filter(a => 
      a.address.includes('.repository.') ||
      a.address.includes('.dao.') ||
      a.address.includes('.mapper.') ||
      (a as any).annotations?.some((ann: string) => ann.includes('Repository') || ann.includes('Mapper'))
    );
    if (repositories.length > 0) {
      detectedLayers.push({
        name: 'Repository / DAO',
        icon: '💾',
        color: '#10b981',
        classes: repositories,
        description: '数据访问和持久化',
      });
    }

    // Detect Model/Entity layer
    const models = assets.filter(a => 
      a.address.includes('.model.') ||
      a.address.includes('.entity.') ||
      a.address.includes('.dto.') ||
      (a as any).annotations?.some((ann: string) => ann.includes('Entity') || ann.includes('Model'))
    );
    if (models.length > 0) {
      detectedLayers.push({
        name: 'Model / Entity',
        icon: '📦',
        color: '#f59e0b',
        classes: models,
        description: '数据模型和实体定义',
      });
    }

    // Detect Utility/Helper layer
    const utils = assets.filter(a => 
      a.address.includes('.util.') ||
      a.address.includes('.helper.') ||
      a.address.includes('.common.')
    );
    if (utils.length > 0) {
      detectedLayers.push({
        name: 'Utility / Common',
        icon: '🔧',
        color: '#64748b',
        classes: utils,
        description: '工具类和通用组件',
      });
    }

    // Remaining classes go to "Other" layer
    const classifiedAddresses = new Set(
      detectedLayers.flatMap(l => l.classes.map(c => c.address))
    );
    const others = assets.filter(a => !classifiedAddresses.has(a.address));
    if (others.length > 0) {
      detectedLayers.push({
        name: 'Other',
        icon: '📄',
        color: '#475569',
        classes: others,
        description: '其他未分类的类',
      });
    }

    return detectedLayers;
  }, [fullAnalysisData]);

  // Filter by selected layer
  const displayedClasses = useMemo(() => {
    if (!selectedLayer) return [];
    const layer = layers.find(l => l.name === selectedLayer);
    return layer?.classes || [];
  }, [layers, selectedLayer]);

  if (!fullAnalysisData) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
        <h3>🏗️ 暂无架构分层数据</h3>
        <p style={{ marginTop: '12px', fontSize: '13px' }}>
          请先加载项目进行分析
        </p>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', height: '100%', overflow: 'auto' }}>
      <h2 style={{ marginBottom: '24px', fontSize: '20px', color: '#f8fafc' }}>
        🏗️ 架构分层
      </h2>

      {/* Layer Cards */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '16px',
        marginBottom: '32px'
      }}>
        {layers.map(layer => (
          <div
            key={layer.name}
            onClick={() => setSelectedLayer(selectedLayer === layer.name ? null : layer.name)}
            style={{
              padding: '20px',
              backgroundColor: selectedLayer === layer.name ? '#1e293b' : '#0f172a',
              border: `2px solid ${layer.color}`,
              borderRadius: '8px',
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
          >
            <div style={{ fontSize: '32px', marginBottom: '8px', textAlign: 'center' }}>
              {layer.icon}
            </div>
            <div style={{ 
              fontSize: '14px', 
              fontWeight: 600, 
              color: '#f8fafc',
              textAlign: 'center',
              marginBottom: '4px',
            }}>
              {layer.name}
            </div>
            <div style={{ 
              fontSize: '12px', 
              color: '#64748b',
              textAlign: 'center',
              marginBottom: '8px',
            }}>
              {layer.classes.length} 个类
            </div>
            <div style={{ 
              fontSize: '11px', 
              color: '#94a3b8',
              textAlign: 'center',
              lineHeight: '1.4',
            }}>
              {layer.description}
            </div>
          </div>
        ))}
      </div>

      {/* Classes in Selected Layer */}
      {selectedLayer && (
        <div>
          <h3 style={{ 
            marginBottom: '16px', 
            fontSize: '16px', 
            color: '#f8fafc',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}>
            {layers.find(l => l.name === selectedLayer)?.icon} 
            {selectedLayer} - 类列表
          </h3>

          <div style={{
            backgroundColor: '#0f172a',
            border: '1px solid #334155',
            borderRadius: '8px',
            overflow: 'hidden',
          }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid #334155' }}>
                  <th style={{ padding: '12px', textAlign: 'left', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>类名</th>
                  <th style={{ padding: '12px', textAlign: 'left', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>完整路径</th>
                  <th style={{ padding: '12px', textAlign: 'right', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>方法数</th>
                </tr>
              </thead>
              <tbody>
                {displayedClasses.map(asset => {
                  const methods = asset.methods_full || asset.methods || [];
                  return (
                    <tr 
                      key={asset.address}
                      style={{ borderBottom: '1px solid #1e293b' }}
                    >
                      <td style={{ padding: '12px', color: '#f8fafc', fontSize: '13px' }}>
                        {asset.address.split('.').pop()}
                      </td>
                      <td style={{ padding: '12px', color: '#64748b', fontSize: '12px', fontFamily: 'monospace' }}>
                        {asset.address}
                      </td>
                      <td style={{ padding: '12px', color: '#f8fafc', fontSize: '13px', textAlign: 'right' }}>
                        {methods.length}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div style={{ 
            marginTop: '12px', 
            textAlign: 'right',
            color: '#64748b',
            fontSize: '13px',
          }}>
            共 {displayedClasses.length} 个类
          </div>
        </div>
      )}

      {/* Architecture Summary */}
      {!selectedLayer && (
        <div style={{
          marginTop: '24px',
          padding: '20px',
          backgroundColor: '#0f172a',
          border: '1px solid #334155',
          borderRadius: '8px',
        }}>
          <h3 style={{ margin: '0 0 16px 0', fontSize: '16px', color: '#f8fafc' }}>
            📊 架构统计
          </h3>
          
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '12px' }}>
            <div>
              <div style={{ fontSize: '12px', color: '#94a3b8', marginBottom: '4px' }}>总层数</div>
              <div style={{ fontSize: '24px', fontWeight: 600, color: '#3b82f6' }}>{layers.length}</div>
            </div>
            <div>
              <div style={{ fontSize: '12px', color: '#94a3b8', marginBottom: '4px' }}>总类数</div>
              <div style={{ fontSize: '24px', fontWeight: 600, color: '#8b5cf6' }}>
                {layers.reduce((sum, l) => sum + l.classes.length, 0)}
              </div>
            </div>
            <div>
              <div style={{ fontSize: '12px', color: '#94a3b8', marginBottom: '4px' }}>最大层</div>
              <div style={{ fontSize: '14px', fontWeight: 600, color: '#10b981' }}>
                {layers.reduce((max, l) => l.classes.length > max.classes.length ? l : max, layers[0])?.name || '-'}
              </div>
            </div>
            <div>
              <div style={{ fontSize: '12px', color: '#94a3b8', marginBottom: '4px' }}>最小层</div>
              <div style={{ fontSize: '14px', fontWeight: 600, color: '#f59e0b' }}>
                {layers.reduce((min, l) => l.classes.length < min.classes.length ? l : min, layers[0])?.name || '-'}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ArchitectureLayerView;
