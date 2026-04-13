/**
 * RagPipelineView - React Component
 * RAG管道流程可视化（Harness Engineering）
 */

import React, { useMemo } from 'react';
import type { UnifiedReport, UnifiedIssue } from '@types/unified-issue';
import { useAppStore } from '@store/app-store';

interface PipelineStage {
  name: string;
  icon: string;
  color: string;
  duration?: number;
  status: 'success' | 'warning' | 'error' | 'pending';
  details?: string[];
}

const RagPipelineView: React.FC = () => {
  const { unifiedReport } = useAppStore();
  const [selectedStage, setSelectedStage] = React.useState<string | null>(null);

  // Extract pipeline metrics from issues
  const pipelineMetrics = useMemo(() => {
    if (!unifiedReport?.issues) return null;

    const issuesWithMetrics = unifiedReport.issues.filter(i => i.pipelineMetrics);
    if (issuesWithMetrics.length === 0) return null;

    const totalEmbedding = issuesWithMetrics.reduce((sum, i) => sum + (i.pipelineMetrics?.embeddingTimeMs || 0), 0);
    const totalVectorSearch = issuesWithMetrics.reduce((sum, i) => sum + (i.pipelineMetrics?.vectorSearchTimeMs || 0), 0);
    const totalLLM = issuesWithMetrics.reduce((sum, i) => sum + (i.pipelineMetrics?.llmInferenceTimeMs || 0), 0);
    const totalPipeline = issuesWithMetrics.reduce((sum, i) => sum + (i.pipelineMetrics?.totalPipelineTimeMs || 0), 0);
    const totalTokens = issuesWithMetrics.reduce((sum, i) => sum + (i.pipelineMetrics?.tokensUsed || 0), 0);

    return {
      avgEmbedding: Math.round(totalEmbedding / issuesWithMetrics.length),
      avgVectorSearch: Math.round(totalVectorSearch / issuesWithMetrics.length),
      avgLLM: Math.round(totalLLM / issuesWithMetrics.length),
      avgTotal: Math.round(totalPipeline / issuesWithMetrics.length),
      totalTokens,
      processedCount: issuesWithMetrics.length,
    };
  }, [unifiedReport]);

  // Build pipeline stages
  const stages = useMemo<PipelineStage[]>(() => {
    if (!pipelineMetrics) {
      return [
        { name: 'Code Slicer', icon: '✂️', color: '#3b82f6', status: 'pending' },
        { name: 'Vector Embedding', icon: '🔢', color: '#8b5cf6', status: 'pending' },
        { name: 'Semantic Search', icon: '🔍', color: '#10b981', status: 'pending' },
        { name: 'LLM Review', icon: '🤖', color: '#f59e0b', status: 'pending' },
        { name: 'Result Merger', icon: '🔗', color: '#ec4899', status: 'pending' },
      ];
    }

    return [
      {
        name: 'Code Slicer',
        icon: '✂️',
        color: '#3b82f6',
        status: 'success',
        details: ['切片大小: 500 tokens', '重叠: 50 tokens'],
      },
      {
        name: 'Vector Embedding',
        icon: '🔢',
        color: '#8b5cf6',
        status: 'success',
        duration: pipelineMetrics.avgEmbedding,
        details: [`耗时: ${pipelineMetrics.avgEmbedding}ms`],
      },
      {
        name: 'Semantic Search',
        icon: '🔍',
        color: '#10b981',
        status: 'success',
        duration: pipelineMetrics.avgVectorSearch,
        details: [`检索时间: ${pipelineMetrics.avgVectorSearch}ms`, `Top-K: 5`],
      },
      {
        name: 'LLM Review',
        icon: '🤖',
        color: '#f59e0b',
        status: 'success',
        duration: pipelineMetrics.avgLLM,
        details: [
          `推理时间: ${pipelineMetrics.avgLLM}ms`,
          `Tokens: ${pipelineMetrics.totalTokens}`,
        ],
      },
      {
        name: 'Result Merger',
        icon: '🔗',
        color: '#ec4899',
        status: 'success',
        details: ['策略: 优先级合并', '去重: enabled'],
      },
    ];
  }, [pipelineMetrics]);

  if (!unifiedReport) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
        <h3>🔧 暂无 RAG 管道数据</h3>
        <p style={{ marginTop: '12px', fontSize: '13px' }}>
          RAG 管道需要先运行 CodeGuardian AI 模块。<br/>
          此视图展示从代码切片到结果合并的完整流程。
        </p>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', height: '100%', overflow: 'auto' }}>
      <h2 style={{ marginBottom: '24px', fontSize: '20px', color: '#f8fafc' }}>
        🔧 RAG 管道流程
      </h2>

      {/* Pipeline Metrics Summary */}
      {pipelineMetrics && (
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: '12px',
          marginBottom: '32px'
        }}>
          <MetricCard 
            label="平均总耗时" 
            value={`${pipelineMetrics.avgTotal}ms`} 
            icon="⏱️"
            color="#3b82f6"
          />
          <MetricCard 
            label="处理问题数" 
            value={pipelineMetrics.processedCount} 
            icon="📊"
            color="#10b981"
          />
          <MetricCard 
            label="总 Tokens" 
            value={pipelineMetrics.totalTokens.toLocaleString()} 
            icon="🔢"
            color="#8b5cf6"
          />
          <MetricCard 
            label="平均 LLM 耗时" 
            value={`${pipelineMetrics.avgLLM}ms`} 
            icon="🤖"
            color="#f59e0b"
          />
        </div>
      )}

      {/* Pipeline Flow Visualization */}
      <div style={{
        backgroundColor: '#0f172a',
        border: '1px solid #334155',
        borderRadius: '12px',
        padding: '32px',
        marginBottom: '24px',
      }}>
        <h3 style={{ margin: '0 0 24px 0', fontSize: '16px', color: '#f8fafc' }}>
          管道流程可视化
        </h3>

        <div style={{ 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'space-between',
          gap: '16px',
          flexWrap: 'wrap',
        }}>
          {stages.map((stage, index) => (
            <React.Fragment key={stage.name}>
              {/* Stage Node */}
              <div
                onClick={() => setSelectedStage(selectedStage === stage.name ? null : stage.name)}
                style={{
                  flex: '1',
                  minWidth: '140px',
                  padding: '16px',
                  backgroundColor: selectedStage === stage.name ? '#1e293b' : '#0f172a',
                  border: `2px solid ${stage.color}`,
                  borderRadius: '8px',
                  cursor: 'pointer',
                  transition: 'all 0.3s',
                  opacity: stage.status === 'pending' ? 0.5 : 1,
                }}
              >
                <div style={{ fontSize: '32px', marginBottom: '8px', textAlign: 'center' }}>
                  {stage.icon}
                </div>
                <div style={{ 
                  fontSize: '13px', 
                  fontWeight: 600, 
                  color: '#f8fafc',
                  textAlign: 'center',
                  marginBottom: '4px',
                }}>
                  {stage.name}
                </div>
                {stage.duration && (
                  <div style={{ 
                    fontSize: '11px', 
                    color: stage.color,
                    textAlign: 'center',
                  }}>
                    {stage.duration}ms
                  </div>
                )}
                <div style={{ 
                  marginTop: '8px',
                  width: '8px',
                  height: '8px',
                  borderRadius: '50%',
                  backgroundColor: getStageStatusColor(stage.status),
                  marginLeft: 'auto',
                  marginRight: 'auto',
                }} />
              </div>

              {/* Arrow (except last) */}
              {index < stages.length - 1 && (
                <div style={{ 
                  fontSize: '24px', 
                  color: '#475569',
                  flexShrink: 0,
                }}>
                  →
                </div>
              )}
            </React.Fragment>
          ))}
        </div>
      </div>

      {/* Stage Details */}
      {selectedStage && (
        <div style={{
          backgroundColor: '#0f172a',
          border: '1px solid #334155',
          borderRadius: '8px',
          padding: '20px',
        }}>
          <h4 style={{ margin: '0 0 16px 0', fontSize: '14px', color: '#f8fafc' }}>
            {stages.find(s => s.name === selectedStage)?.icon} {selectedStage} - 详细信息
          </h4>
          
          {stages.find(s => s.name === selectedStage)?.details && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {stages.find(s => s.name === selectedStage)?.details?.map((detail, idx) => (
                <div key={idx} style={{
                  padding: '8px 12px',
                  backgroundColor: '#1e293b',
                  borderRadius: '6px',
                  fontSize: '13px',
                  color: '#e2e8f0',
                }}>
                  {detail}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Validation Loop */}
      {unifiedReport.summary?.validationStats && (
        <div style={{
          backgroundColor: '#0f172a',
          border: '1px solid #334155',
          borderRadius: '8px',
          padding: '20px',
          marginTop: '24px',
        }}>
          <h3 style={{ margin: '0 0 16px 0', fontSize: '16px', color: '#f8fafc' }}>
            🔄 Harness Engineering 验证循环
          </h3>
          
          <div style={{ 
            display: 'grid', 
            gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))',
            gap: '12px',
          }}>
            <ValidationStat 
              label="接受" 
              value={unifiedReport.summary.validationStats.accepted} 
              color="#10b981"
              icon="✅"
            />
            <ValidationStat 
              label="降级" 
              value={unifiedReport.summary.validationStats.downgraded} 
              color="#f59e0b"
              icon="⬇️"
            />
            <ValidationStat 
              label="标记待审" 
              value={unifiedReport.summary.validationStats.flaggedForReview} 
              color="#3b82f6"
              icon="🚩"
            />
            <ValidationStat 
              label="重试" 
              value={unifiedReport.summary.validationStats.retried} 
              color="#8b5cf6"
              icon="🔄"
            />
          </div>
        </div>
      )}
    </div>
  );
};

const MetricCard: React.FC<{ label: string; value: number | string; icon: string; color: string }> = ({
  label, value, icon, color
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
      <div style={{ fontSize: '20px', fontWeight: 600, color }}>{value}</div>
      <div style={{ fontSize: '12px', color: '#64748b', marginTop: '2px' }}>{label}</div>
    </div>
  </div>
);

const ValidationStat: React.FC<{ label: string; value: number; color: string; icon: string }> = ({
  label, value, color, icon
}) => (
  <div style={{
    padding: '12px',
    backgroundColor: '#1e293b',
    borderRadius: '6px',
    textAlign: 'center',
  }}>
    <div style={{ fontSize: '24px', marginBottom: '4px' }}>{icon}</div>
    <div style={{ fontSize: '20px', fontWeight: 600, color, marginBottom: '4px' }}>{value}</div>
    <div style={{ fontSize: '12px', color: '#94a3b8' }}>{label}</div>
  </div>
);

const getStageStatusColor = (status: string): string => {
  const colors: Record<string, string> = {
    success: '#10b981',
    warning: '#f59e0b',
    error: '#ef4444',
    pending: '#64748b',
  };
  return colors[status] || '#64748b';
};

export default RagPipelineView;
