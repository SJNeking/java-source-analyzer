import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import type { ViewType } from '../types';

const NAV_ITEMS: Array<{ view: ViewType; label: string; icon: string }> = [
  { view: 'explorer', label: '源码浏览器', icon: '📄' },
  { view: 'graph', label: '依赖图', icon: '🔗' },
  { view: 'relations', label: '跨文件关系', icon: '🔀' },
  { view: 'quality', label: '质量分析', icon: '⚠️' },
  { view: 'frontend-quality', label: '前端质量', icon: '🔬' },
  { view: 'metrics', label: '代码指标', icon: '📊' },
  { view: 'assets', label: '项目资产', icon: '📂' },
  { view: 'ai-review', label: 'AI 审查', icon: '🤖' },
  { view: 'pipeline', label: 'RAG管道', icon: '⚙️' },
  { view: 'performance', label: '性能监控', icon: '📈' },
];

const TopBar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const currentPath = location.pathname.split('/')[1] || 'explorer';
  
  const currentView = NAV_ITEMS.find(v => v.view === currentPath);

  return (
    <div className="top-bar">
      <div className="top-bar-title">
        <span>{currentView?.icon || '📄'}</span>
        <span>{currentView?.label || '源码浏览器'}</span>
      </div>
      
      <div className="nav-tabs">
        {NAV_ITEMS.map(item => (
          <button
            key={item.view}
            className={`nav-tab${currentPath === item.view ? ' active' : ''}`}
            onClick={() => navigate(`/${item.view}`)}
          >
            {item.icon} {item.label}
          </button>
        ))}
      </div>
    </div>
  );
};

export default TopBar;
