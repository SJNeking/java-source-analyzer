import React from 'react';
import { Routes, Route } from 'react-router-dom';
import type { ViewType } from '../types';
import ForceGraphView from '@views/ForceGraphView';
import QualityDashboardView from '@views/QualityDashboardView';
import { useAppStore } from '@store/app-store';

// Placeholder view component
const PlaceholderView: React.FC<{ name: string }> = ({ name }) => (
  <div style={{ padding: '40px', textAlign: 'center' }}>
    <h2>{name} 视图待迁移</h2>
    <p style={{ color: '#94a3b8', marginTop: '16px' }}>
      此视图将在后续迁移任务中从旧前端迁移过来
    </p>
  </div>
);

const ViewContainer: React.FC = () => {
  const { graphData, unifiedReport } = useAppStore();

  return (
    <div className="content-viewport">
      <Routes>
        <Route path="/" element={<PlaceholderView name="源码浏览器" />} />
        <Route path="/explorer" element={<PlaceholderView name="源码浏览器" />} />
        
        <Route 
          path="/graph" 
          element={graphData ? <ForceGraphView data={graphData} /> : <PlaceholderView name="依赖图" />} 
        />
        
        <Route path="/relations" element={<PlaceholderView name="跨文件关系" />} />
        
        <Route 
          path="/quality" 
          element={unifiedReport ? <QualityDashboardView /> : <PlaceholderView name="质量分析" />} 
        />
        
        <Route path="/frontend-quality" element={<PlaceholderView name="前端质量" />} />
        <Route path="/metrics" element={<PlaceholderView name="代码指标" />} />
        <Route path="/assets" element={<PlaceholderView name="项目资产" />} />
        <Route path="/ai-review" element={<PlaceholderView name="AI 审查" />} />
        <Route path="/pipeline" element={<PlaceholderView name="RAG管道" />} />
        <Route path="/performance" element={<PlaceholderView name="性能监控" />} />
      </Routes>
    </div>
  );
};

export default ViewContainer;
