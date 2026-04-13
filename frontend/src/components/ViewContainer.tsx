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
        {/* Default route - Welcome page */}
        <Route 
          path="/" 
          element={
            <div style={{ padding: '60px', textAlign: 'center' }}>
              <h1 style={{ fontSize: '32px', marginBottom: '20px', color: '#f8fafc' }}>
                🚀 Java Source Analyzer
              </h1>
              <p style={{ fontSize: '16px', color: '#94a3b8', marginBottom: '40px' }}>
                React前端已就绪，请选择左侧菜单查看功能
              </p>
              
              <div style={{ 
                display: 'grid', 
                gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
                gap: '20px',
                maxWidth: '1000px',
                margin: '0 auto'
              }}>
                <div style={{ padding: '20px', backgroundColor: '#0f172a', borderRadius: '8px', border: '1px solid #334155' }}>
                  <div style={{ fontSize: '24px', marginBottom: '8px' }}>📊</div>
                  <h3 style={{ margin: '0 0 8px 0', color: '#f8fafc' }}>代码可视化</h3>
                  <p style={{ margin: 0, fontSize: '13px', color: '#94a3b8' }}>力导向图、桑基图、调用链路</p>
                </div>
                
                <div style={{ padding: '20px', backgroundColor: '#0f172a', borderRadius: '8px', border: '1px solid #334155' }}>
                  <div style={{ fontSize: '24px', marginBottom: '8px' }}>🔍</div>
                  <h3 style={{ margin: '0 0 8px 0', color: '#f8fafc' }}>质量分析</h3>
                  <p style={{ margin: 0, fontSize: '13px', color: '#94a3b8' }}>AI审查、指标监控、架构检测</p>
                </div>
                
                <div style={{ padding: '20px', backgroundColor: '#0f172a', borderRadius: '8px', border: '1px solid #334155' }}>
                  <div style={{ fontSize: '24px', marginBottom: '8px' }}>⚙️</div>
                  <h3 style={{ margin: '0 0 8px 0', color: '#f8fafc' }}>Harness Engineering</h3>
                  <p style={{ margin: 0, fontSize: '13px', color: '#94a3b8' }}>RAG管道、性能监控、验证反馈</p>
                </div>
              </div>
              
              <div style={{ marginTop: '40px', padding: '16px', backgroundColor: '#1e293b', borderRadius: '6px', display: 'inline-block' }}>
                <p style={{ margin: 0, fontSize: '13px', color: '#64748b' }}>
                  💡 提示：启动后端服务以加载项目数据
                </p>
              </div>
            </div>
          }
        />
        
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
