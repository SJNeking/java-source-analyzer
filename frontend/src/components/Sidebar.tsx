import React from 'react';
import type { ProjectInfo } from '../types/project';

interface SidebarProps {
  projects: ProjectInfo[];
  currentProject: ProjectInfo | null;
  onProjectSelect: (project: ProjectInfo) => void;
}

const Sidebar: React.FC<SidebarProps> = ({ projects, currentProject, onProjectSelect }) => {
  return (
    <nav className="sidebar" aria-label="项目导航">
      <div className="sidebar-header">
        <select 
          className="project-select"
          value={currentProject?.file || ''}
          onChange={(e) => {
            const project = projects.find(p => p.file === e.target.value);
            if (project) onProjectSelect(project);
          }}
        >
          {projects.map(p => (
            <option key={p.file} value={p.file}>{p.name}</option>
          ))}
        </select>
      </div>
      
      <div className="search-box">
        <input 
          type="text" 
          className="search-input" 
          placeholder="🔍 搜索类或方法..." 
        />
      </div>
      
      <div id="tree-container" className="tree-container" role="tree">
        {/* Tree content will be added in future migration */}
        <div style={{ padding: '20px', color: '#94a3b8' }}>
          项目树待迁移...
        </div>
      </div>
    </nav>
  );
};

export default Sidebar;
