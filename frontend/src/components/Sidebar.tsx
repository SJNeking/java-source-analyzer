import React from 'react';
import { useNavigate } from 'react-router-dom';
import type { ProjectInfo } from '../types/project';

interface SidebarProps {
  projects: ProjectInfo[];
  currentProject: ProjectInfo | null;
  onProjectSelect: (project: ProjectInfo) => void;
}

const Sidebar: React.FC<SidebarProps> = ({ projects, currentProject, onProjectSelect }) => {
  const navigate = useNavigate();

  const menuItems = [
    { path: '/', icon: '🏠', label: '首页' },
    { path: '/explorer', icon: '📂', label: '代码浏览器' },
    { path: '/graph', icon: '🕸️', label: '依赖图谱' },
    { path: '/quality', icon: '📊', label: '质量分析' },
    { path: '/metrics', icon: '📈', label: '代码指标' },
    { path: '/assets', icon: '📦', label: '项目资产' },
    { path: '/relations', icon: '🔗', label: '跨文件关系' },
    { path: '/architecture', icon: '🏛️', label: '架构分层' },
    { path: '/method-calls', icon: '🔄', label: '方法调用' },
    { path: '/call-chains', icon: '⛓️', label: '调用链路' },
    { path: '/ai-review', icon: '🤖', label: 'AI审查' },
    { path: '/pipeline', icon: '🚀', label: 'RAG管道' },
    { path: '/performance', icon: '⚡', label: '性能监控' },
  ];

  return (
    <nav className="sidebar" aria-label="项目导航">
      <div className="sidebar-header">
        <h2 style={{ margin: 0, fontSize: '16px', color: '#f8fafc' }}>
          🎯 Java Analyzer
        </h2>
      </div>
      
      <div style={{ padding: '12px', borderBottom: '1px solid #334155' }}>
        <div style={{ fontSize: '11px', color: '#64748b', marginBottom: '4px' }}>
          当前项目
        </div>
        <div style={{ fontSize: '13px', color: '#f8fafc', fontWeight: 600 }}>
          {currentProject?.name || '未选择'}
        </div>
      </div>
      
      <div style={{ flex: 1, overflow: 'auto', padding: '8px' }}>
        {menuItems.map(item => (
          <div
            key={item.path}
            onClick={() => navigate(item.path)}
            style={{
              padding: '10px 12px',
              marginBottom: '4px',
              borderRadius: '6px',
              cursor: 'pointer',
              fontSize: '13px',
              color: '#e2e8f0',
              transition: 'all 0.2s',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.backgroundColor = '#1e293b';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = 'transparent';
            }}
          >
            <span style={{ fontSize: '16px' }}>{item.icon}</span>
            <span>{item.label}</span>
          </div>
        ))}
      </div>
    </nav>
  );
};

export default Sidebar;
