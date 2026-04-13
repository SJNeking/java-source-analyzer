import React, { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';
import TopBar from './TopBar';
import ViewContainer from './ViewContainer';
import LoadingOverlay from './LoadingOverlay';
import { useAppStore } from '../store';
import { dataFetcher } from '../services';
import type { ProjectInfo } from '../types/project';

const Layout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  
  const { 
    projects, 
    currentProject, 
    setProjects, 
    setCurrentProject,
    setLoading,
    setError 
  } = useAppStore();

  // Load projects on mount
  useEffect(() => {
    const loadProjects = async () => {
      try {
        setLoading(true);
        const projectsData = await dataFetcher.loadProjects();
        setProjects(projectsData as ProjectInfo[]);
        
        if (projectsData.length > 0 && !currentProject) {
          handleProjectSelect(projectsData[0]);
        }
      } catch (error) {
        setError('加载项目失败');
        console.error(error);
      } finally {
        setLoading(false);
      }
    };

    loadProjects();
  }, []);

  const handleProjectSelect = async (project: ProjectInfo) => {
    try {
      setLoading(true);
      setCurrentProject(project);
      
      // Load project data
      const graphData = await dataFetcher.loadProjectData(project.file);
      const unifiedReport = await dataFetcher.loadUnifiedReport(project.file);
      
      // Navigate to default view
      const currentView = location.pathname.split('/')[1] || 'explorer';
      navigate(`/${currentView}`);
    } catch (error) {
      setError('加载项目数据失败');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app-layout">
      <Sidebar 
        projects={projects}
        currentProject={currentProject}
        onProjectSelect={handleProjectSelect}
      />
      
      <main className="main-area" role="main">
        <TopBar />
        <ViewContainer />
      </main>
      
      <LoadingOverlay />
    </div>
  );
};

export default Layout;
