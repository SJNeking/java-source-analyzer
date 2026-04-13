import React from 'react';
import { Routes, Route } from 'react-router-dom';
import ForceGraphView from '@views/ForceGraphView';
import QualityDashboardView from '@views/QualityDashboardView';
import AiReviewView from '@views/AiReviewView';
import MetricsDashboardView from '@views/MetricsDashboardView';
import ProjectAssetsView from '@views/ProjectAssetsView';
import RagPipelineView from '@views/RagPipelineView';
import PerformanceMetricsView from '@views/PerformanceMetricsView';
import MethodCallView from '@views/MethodCallView';
import CrossFileRelationsView from '@views/CrossFileRelationsView';
import ArchitectureLayerView from '@views/ArchitectureLayerView';
import CallChainView from '@views/CallChainView';
import ClassInspectorPanel from '@views/ClassInspectorPanel';
import ComponentExplorerView from '@views/ComponentExplorerView';
import ApiEndpointView from '@views/ApiEndpointView';
import FrontendQualityView from '@views/FrontendQualityView';
import CodeExplorerView from '@views/CodeExplorerView';
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
  const { graphData, unifiedReport, fullAnalysisData } = useAppStore();

  return (
    <div className="content-viewport">
      <Routes>
        <Route 
          path="/explorer" 
          element={fullAnalysisData ? <CodeExplorerView /> : <PlaceholderView name="代码浏览器" />} 
        />
        
        <Route 
          path="/graph" 
          element={graphData ? <ForceGraphView data={graphData} /> : <PlaceholderView name="依赖图" />} 
        />
        
        <Route path="/relations" element={<PlaceholderView name="跨文件关系" />} />
        
        <Route 
          path="/quality" 
          element={unifiedReport ? <QualityDashboardView /> : <PlaceholderView name="质量分析" />} 
        />
        
        <Route path="/frontend-quality" element={<FrontendQualityView />} />
        <Route path="/metrics" element={fullAnalysisData ? <MetricsDashboardView /> : <PlaceholderView name="代码指标" />} />
        <Route path="/assets" element={fullAnalysisData ? <ProjectAssetsView /> : <PlaceholderView name="项目资产" />} />
        <Route path="/relations" element={<CrossFileRelationsView />} />
        <Route path="/architecture" element={<ArchitectureLayerView />} />
        <Route path="/method-calls" element={<MethodCallView />} />
        <Route path="/call-chains" element={<CallChainView />} />
        <Route path="/inspector" element={<ClassInspectorPanel />} />
        <Route path="/components" element={<ComponentExplorerView />} />
        <Route path="/api-endpoints" element={<ApiEndpointView />} />
        <Route path="/ai-review" element={unifiedReport ? <AiReviewView /> : <PlaceholderView name="AI 审查" />} />
        <Route path="/pipeline" element={<RagPipelineView />} />
        <Route path="/performance" element={<PerformanceMetricsView />} />
      </Routes>
    </div>
  );
};

export default ViewContainer;
