/**
 * CrossFileRelationsView - React Component
 * 跨文件关系可视化
 */

import React, { useMemo } from 'react';
import { useAppStore } from '@store/app-store';

interface CrossFileRelation {
  source: string;
  target: string;
  type: 'INHERITANCE' | 'IMPLEMENTATION' | 'DEPENDENCY' | 'COMPOSITION';
  strength: number;
}

const CrossFileRelationsView: React.FC = () => {
  const { fullAnalysisData } = useAppStore();
  const [relationType, setRelationType] = React.useState<string>('ALL');
  const [minStrength, setMinStrength] = React.useState(0);

  // Extract cross-file relations
  const relations = useMemo(() => {
    if (!fullAnalysisData?.assets) return [];

    const allRelations: CrossFileRelation[] = [];

    fullAnalysisData.assets.forEach(asset => {
      // Check for inheritance
      if (asset.extends_class) {
        allRelations.push({
          source: asset.address,
          target: asset.extends_class,
          type: 'INHERITANCE',
          strength: 1.0,
        });
      }

      // Check for implementations
      if (asset.implements_interfaces) {
        asset.implements_interfaces.forEach((iface: any) => {
          allRelations.push({
            source: asset.address,
            target: iface,
            type: 'IMPLEMENTATION',
            strength: 0.9,
          });
        });
      }

      // Check for dependencies (imports/usage)
      if ((asset as any).dependencies) {
        (asset as any).dependencies.forEach((dep: any) => {
          allRelations.push({
            source: asset.address,
            target: dep.target || dep.class || '',
            type: dep.type === 'COMPOSITION' ? 'COMPOSITION' : 'DEPENDENCY',
            strength: dep.strength || 0.5,
          });
        });
      }
    });

    // Filter by type
    let filtered = relationType === 'ALL' 
      ? allRelations 
      : allRelations.filter(r => r.type === relationType);

    // Filter by strength
    filtered = filtered.filter(r => r.strength >= minStrength);

    return filtered;
  }, [fullAnalysisData, relationType, minStrength]);

  // Group relations by type
  const relationsByType = useMemo(() => {
    const grouped: Record<string, CrossFileRelation[]> = {};
    relations.forEach(rel => {
      if (!grouped[rel.type]) grouped[rel.type] = [];
      grouped[rel.type].push(rel);
    });
    return grouped;
  }, [relations]);

  // Get unique classes involved in relations
  const involvedClasses = useMemo(() => {
    const classes = new Set<string>();
    relations.forEach(r => {
      classes.add(r.source);
      classes.add(r.target);
    });
    return Array.from(classes).sort();
  }, [relations]);

  if (!fullAnalysisData) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
        <h3>🔗 暂无跨文件关系数据</h3>
        <p style={{ marginTop: '12px', fontSize: '13px' }}>
          请先加载项目进行分析
        </p>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', height: '100%', overflow: 'auto' }}>
      <h2 style={{ marginBottom: '24px', fontSize: '20px', color: '#f8fafc' }}>
        🔗 跨文件关系
      </h2>

      {/* Stats Cards */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
        gap: '12px',
        marginBottom: '24px'
      }}>
        <StatCard 
          label="总关系数" 
          value={relations.length} 
          color="#3b82f6"
          icon="🔗"
        />
        <StatCard 
          label="涉及类数" 
          value={involvedClasses.length} 
          color="#8b5cf6"
          icon="📦"
        />
        {Object.entries(relationsByType).map(([type, rels]) => (
          <StatCard 
            key={type}
            label={getRelationTypeLabel(type)} 
            value={rels.length} 
            color={getRelationTypeColor(type)}
            icon={getRelationTypeIcon(type)}
          />
        ))}
      </div>

      {/* Filters */}
      <div style={{ 
        display: 'flex', 
        gap: '12px', 
        marginBottom: '20px',
        alignItems: 'center',
        flexWrap: 'wrap',
      }}>
        <span style={{ color: '#94a3b8', fontSize: '13px' }}>关系类型:</span>
        {(['ALL', 'INHERITANCE', 'IMPLEMENTATION', 'DEPENDENCY', 'COMPOSITION'] as const).map(type => (
          <button
            key={type}
            onClick={() => setRelationType(type)}
            style={{
              padding: '6px 12px',
              backgroundColor: relationType === type ? '#3b82f6' : 'transparent',
              color: relationType === type ? '#f8fafc' : '#94a3b8',
              border: '1px solid #334155',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '12px',
            }}
          >
            {type === 'ALL' ? `全部 (${relations.length})` : 
             `${getRelationTypeLabel(type)} (${relationsByType[type]?.length || 0})`}
          </button>
        ))}

        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ color: '#94a3b8', fontSize: '13px' }}>最小强度:</span>
          <input
            type="range"
            min="0"
            max="1"
            step="0.1"
            value={minStrength}
            onChange={(e) => setMinStrength(parseFloat(e.target.value))}
            style={{ width: '120px' }}
          />
          <span style={{ color: '#f8fafc', fontSize: '13px', minWidth: '40px' }}>
            {(minStrength * 100).toFixed(0)}%
          </span>
        </div>
      </div>

      {/* Relations Table */}
      <div style={{
        backgroundColor: '#0f172a',
        border: '1px solid #334155',
        borderRadius: '8px',
        overflow: 'hidden',
      }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid #334155' }}>
              <th style={{ padding: '12px', textAlign: 'left', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>源类</th>
              <th style={{ padding: '12px', textAlign: 'center', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>关系类型</th>
              <th style={{ padding: '12px', textAlign: 'left', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>目标类</th>
              <th style={{ padding: '12px', textAlign: 'right', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>强度</th>
            </tr>
          </thead>
          <tbody>
            {relations.length === 0 ? (
              <tr>
                <td colSpan={4} style={{ padding: '40px', textAlign: 'center', color: '#64748b' }}>
                  没有找到符合条件的关系
                </td>
              </tr>
            ) : (
              relations.map((rel, index) => (
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
                    {rel.source.split('.').pop()}
                    <div style={{ fontSize: '11px', color: '#64748b', marginTop: '2px' }}>{rel.source}</div>
                  </td>
                  <td style={{ padding: '12px', textAlign: 'center' }}>
                    <span style={{
                      padding: '4px 8px',
                      backgroundColor: getRelationTypeColor(rel.type),
                      color: '#fff',
                      borderRadius: '4px',
                      fontSize: '11px',
                      fontWeight: 500,
                    }}>
                      {getRelationTypeIcon(rel.type)} {getRelationTypeLabel(rel.type)}
                    </span>
                  </td>
                  <td style={{ padding: '12px', color: '#f8fafc', fontSize: '13px' }}>
                    {rel.target.split('.').pop()}
                    <div style={{ fontSize: '11px', color: '#64748b', marginTop: '2px' }}>{rel.target}</div>
                  </td>
                  <td style={{ padding: '12px', textAlign: 'right' }}>
                    <div style={{
                      display: 'inline-block',
                      padding: '4px 8px',
                      backgroundColor: getStrengthColor(rel.strength),
                      color: '#fff',
                      borderRadius: '4px',
                      fontSize: '12px',
                      fontWeight: 600,
                    }}>
                      {(rel.strength * 100).toFixed(0)}%
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {relations.length > 0 && (
        <div style={{ 
          marginTop: '12px', 
          textAlign: 'right',
          color: '#64748b',
          fontSize: '13px',
        }}>
          共 {relations.length} 条关系
        </div>
      )}
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

const getRelationTypeLabel = (type: string): string => {
  const labels: Record<string, string> = {
    INHERITANCE: '继承',
    IMPLEMENTATION: '实现',
    DEPENDENCY: '依赖',
    COMPOSITION: '组合',
  };
  return labels[type] || type;
};

const getRelationTypeIcon = (type: string): string => {
  const icons: Record<string, string> = {
    INHERITANCE: '🔺',
    IMPLEMENTATION: '🔌',
    DEPENDENCY: '➡️',
    COMPOSITION: '🧩',
  };
  return icons[type] || '🔗';
};

const getRelationTypeColor = (type: string): string => {
  const colors: Record<string, string> = {
    INHERITANCE: '#ef4444',
    IMPLEMENTATION: '#8b5cf6',
    DEPENDENCY: '#3b82f6',
    COMPOSITION: '#10b981',
  };
  return colors[type] || '#64748b';
};

const getStrengthColor = (strength: number): string => {
  if (strength >= 0.8) return '#10b981';
  if (strength >= 0.5) return '#3b82f6';
  if (strength >= 0.3) return '#f59e0b';
  return '#ef4444';
};

export default CrossFileRelationsView;
