/**
 * AiReviewView - React Component
 * AI审查视图（含Harness Engineering验证反馈）
 */

import React, { useMemo } from 'react';
import type { UnifiedIssue, IssueSource } from '@types/unified-issue';
import { useAppStore, selectActiveIssues, selectValidationStats } from '@store/app-store';

const AiReviewView: React.FC = () => {
  const { unifiedReport } = useAppStore();
  const [sourceFilter, setSourceFilter] = React.useState<IssueSource | 'all'>('all');
  const [showFiltered, setShowFiltered] = React.useState(false);

  const issues = useMemo(() => {
    if (!unifiedReport?.issues) return [];
    
    let filtered = showFiltered 
      ? unifiedReport.issues 
      : selectActiveIssues(unifiedReport.issues);

    if (sourceFilter !== 'all') {
      filtered = filtered.filter(issue => issue.source === sourceFilter);
    }

    return filtered.sort((a, b) => {
      const confidenceA = a.confidence || 0;
      const confidenceB = b.confidence || 0;
      return confidenceB - confidenceA;
    });
  }, [unifiedReport, sourceFilter, showFiltered]);

  const stats = useMemo(() => {
    if (!unifiedReport?.summary) return null;
    const s = unifiedReport.summary;
    return {
      total: s.totalIssues,
      active: s.activeIssues,
      filtered: s.autoFiltered,
      staticOnly: s.staticOnly,
      aiOnly: s.aiOnly,
      merged: s.merged,
      highConfidenceRate: s.aiHighConfidenceRate,
      validationStats: selectValidationStats(unifiedReport),
    };
  }, [unifiedReport]);

  if (!unifiedReport) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
        <h3>🤖 暂无 AI 审查数据</h3>
        <p style={{ marginTop: '12px', fontSize: '13px' }}>
          AI 审查功能需要先运行 CodeGuardian AI 模块。<br/>
          运行静态分析后，使用 ResultMerger 合并结果即可在此查看。
        </p>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', height: '100%', overflow: 'auto' }}>
      {/* AI Engine Badge */}
      {unifiedReport.aiEngine && (
        <div style={{ 
          display: 'inline-block',
          padding: '6px 12px',
          backgroundColor: '#1e293b',
          borderRadius: '6px',
          border: '1px solid #3b82f6',
          marginBottom: '20px',
          fontSize: '13px',
          color: '#3b82f6',
          fontWeight: 500,
        }}>
          🤖 {unifiedReport.aiEngine.modelUsed || unifiedReport.aiEngine.name}
        </div>
      )}

      {/* Statistics Cards */}
      {stats && (
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
          gap: '12px',
          marginBottom: '24px'
        }}>
          <StatCard label="总问题" value={stats.total} color="#f8fafc" icon="📊" />
          <StatCard label="活跃问题" value={stats.active} color="#3b82f6" icon="🔍" />
          <StatCard label="已过滤" value={stats.filtered} color="#64748b" icon="🚫" />
          <StatCard label="静态发现" value={stats.staticOnly} color="#10b981" icon="🔧" />
          <StatCard label="AI 发现" value={stats.aiOnly} color="#8b5cf6" icon="🤖" />
          <StatCard label="双重确认" value={stats.merged} color="#f59e0b" icon="✅" />
          {stats.highConfidenceRate > 0 && (
            <StatCard 
              label="AI 高信率" 
              value={`${stats.highConfidenceRate}%`} 
              color="#14b8a6" 
              icon="🎯" 
            />
          )}
        </div>
      )}

      {/* Validation Stats */}
      {stats?.validationStats && (
        <div style={{
          padding: '16px',
          backgroundColor: '#0f172a',
          borderRadius: '8px',
          border: '1px solid #334155',
          marginBottom: '20px',
        }}>
          <h4 style={{ margin: '0 0 12px 0', fontSize: '14px', color: '#f8fafc' }}>
            ⚙️ Harness Engineering 验证统计
          </h4>
          <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
            <div style={{ fontSize: '12px', color: '#94a3b8' }}>
              ✅ 接受: <span style={{ color: '#10b981', fontWeight: 600 }}>{stats.validationStats.accepted}</span>
            </div>
            <div style={{ fontSize: '12px', color: '#94a3b8' }}>
              ⬇️ 降级: <span style={{ color: '#f59e0b', fontWeight: 600 }}>{stats.validationStats.downgraded}</span>
            </div>
            <div style={{ fontSize: '12px', color: '#94a3b8' }}>
              🚩 标记待审: <span style={{ color: '#3b82f6', fontWeight: 600 }}>{stats.validationStats.flaggedForReview}</span>
            </div>
            <div style={{ fontSize: '12px', color: '#94a3b8' }}>
              🔄 重试: <span style={{ color: '#8b5cf6', fontWeight: 600 }}>{stats.validationStats.retried}</span>
            </div>
          </div>
        </div>
      )}

      {/* Filter Bar */}
      <div style={{ 
        display: 'flex', 
        gap: '8px', 
        marginBottom: '20px',
        alignItems: 'center',
        flexWrap: 'wrap',
      }}>
        <span style={{ color: '#94a3b8', fontSize: '13px' }}>来源:</span>
        {(['all', 'static', 'ai', 'merged'] as const).map(source => (
          <button
            key={source}
            onClick={() => setSourceFilter(source)}
            style={{
              padding: '6px 12px',
              backgroundColor: sourceFilter === source ? '#3b82f6' : 'transparent',
              color: sourceFilter === source ? '#f8fafc' : '#94a3b8',
              border: '1px solid #334155',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '12px',
            }}
          >
            {source === 'all' ? `全部 (${stats?.total || 0})` :
             source === 'static' ? `🔧 静态 (${stats?.staticOnly || 0})` :
             source === 'ai' ? `🤖 AI (${(stats?.aiOnly || 0) + (stats?.merged || 0)})` :
             `✅ 双重 (${stats?.merged || 0})`}
          </button>
        ))}
        
        <label style={{ 
          marginLeft: 'auto',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          color: '#94a3b8',
          fontSize: '13px',
          cursor: 'pointer'
        }}>
          <input
            type="checkbox"
            checked={showFiltered}
            onChange={(e) => setShowFiltered(e.target.checked)}
          />
          显示已过滤
        </label>
      </div>

      {/* Issues List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {issues.length === 0 ? (
          <div style={{ padding: '40px', textAlign: 'center', color: '#64748b' }}>
            没有符合条件的问题
          </div>
        ) : (
          issues.map(issue => (
            <AiIssueCard key={issue.id} issue={issue} />
          ))
        )}
      </div>
    </div>
  );
};

// Stat Card Component
const StatCard: React.FC<{ label: string; value: number | string; color: string; icon: string }> = ({
  label, value, color, icon
}) => (
  <div style={{
    backgroundColor: '#0f172a',
    border: '1px solid #334155',
    borderRadius: '8px',
    padding: '12px',
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
  }}>
    <div style={{ fontSize: '20px' }}>{icon}</div>
    <div>
      <div style={{ fontSize: '20px', fontWeight: 600, color }}>{value}</div>
      <div style={{ fontSize: '11px', color: '#64748b', marginTop: '2px' }}>{label}</div>
    </div>
  </div>
);

// AI Issue Card Component
const AiIssueCard: React.FC<{ issue: UnifiedIssue }> = ({ issue }) => {
  const [expanded, setExpanded] = React.useState(false);

  return (
    <div style={{
      backgroundColor: '#0f172a',
      border: '1px solid #334155',
      borderRadius: '8px',
      padding: '16px',
      opacity: issue.autoFiltered ? 0.5 : 1,
    }}>
      {/* Header */}
      <div 
        onClick={() => setExpanded(!expanded)}
        style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', cursor: 'pointer' }}
      >
        {/* Source Badge */}
        <div style={{
          padding: '4px 8px',
          backgroundColor: issue.source === 'merged' ? '#f59e0b' : 
                          issue.source === 'ai' ? '#8b5cf6' : '#10b981',
          color: '#fff',
          borderRadius: '4px',
          fontSize: '11px',
          fontWeight: 600,
          whiteSpace: 'nowrap',
        }}>
          {issue.source === 'merged' ? '✅ 双重' : 
           issue.source === 'ai' ? '🤖 AI' : '🔧 静态'}
        </div>

        <div style={{ flex: 1 }}>
          <div style={{ fontSize: '14px', fontWeight: 500, color: '#f8fafc', marginBottom: '4px' }}>
            {issue.message}
          </div>
          
          <div style={{ fontSize: '12px', color: '#64748b' }}>
            {issue.filePath}:{issue.line}
            {issue.methodName ? ` (${issue.methodName})` : ''}
          </div>

          {/* Confidence Badge */}
          {issue.confidence && (
            <div style={{ 
              display: 'inline-block',
              marginTop: '8px',
              padding: '4px 8px',
              backgroundColor: issue.confidence >= 0.9 ? '#10b981' : 
                              issue.confidence >= 0.7 ? '#f59e0b' : '#ef4444',
              color: '#fff',
              borderRadius: '4px',
              fontSize: '11px',
              fontWeight: 500,
            }}>
              置信度: {(issue.confidence * 100).toFixed(0)}%
            </div>
          )}

          {/* Validation Action Badge */}
          {issue.validationAction && (
            <div style={{ 
              display: 'inline-block',
              marginTop: '8px',
              marginLeft: '8px',
              padding: '4px 8px',
              backgroundColor: getValidationActionColor(issue.validationAction),
              color: '#fff',
              borderRadius: '4px',
              fontSize: '11px',
              fontWeight: 500,
            }}>
              {getValidationActionLabel(issue.validationAction)}
            </div>
          )}
        </div>

        <div style={{ 
          fontSize: '16px', 
          color: '#64748b',
          transform: expanded ? 'rotate(180deg)' : 'rotate(0deg)',
          transition: 'transform 0.2s',
        }}>
          ▼
        </div>
      </div>

      {/* Expanded Content */}
      {expanded && (
        <div style={{ marginTop: '16px', paddingTop: '16px', borderTop: '1px solid #334155' }}>
          {/* Rule Info */}
          <div style={{ fontSize: '12px', color: '#94a3b8', marginBottom: '12px' }}>
            <strong>规则:</strong> {issue.ruleKey} - {issue.ruleName}
          </div>

          {/* AI Suggestion */}
          {issue.aiSuggestion && (
            <div style={{ 
              marginTop: '12px',
              padding: '12px',
              backgroundColor: '#1e293b',
              borderRadius: '6px',
              borderLeft: '3px solid #3b82f6',
            }}>
              <div style={{ fontSize: '12px', color: '#3b82f6', marginBottom: '8px', fontWeight: 600 }}>
                🤖 AI建议
              </div>
              <div style={{ fontSize: '13px', color: '#e2e8f0', whiteSpace: 'pre-wrap', lineHeight: '1.6' }}>
                {issue.aiSuggestion}
              </div>
            </div>
          )}

          {/* Fixed Code */}
          {issue.aiFixedCode && (
            <div style={{ marginTop: '12px' }}>
              <div style={{ fontSize: '12px', color: '#10b981', marginBottom: '8px', fontWeight: 600 }}>
                ✅ 修复代码
              </div>
              <pre style={{
                padding: '12px',
                backgroundColor: '#020617',
                borderRadius: '6px',
                fontSize: '12px',
                color: '#e2e8f0',
                overflow: 'auto',
                maxHeight: '300px',
                lineHeight: '1.5',
              }}>
                {issue.aiFixedCode}
              </pre>
            </div>
          )}

          {/* AI Reasoning */}
          {issue.aiReasoning && (
            <div style={{ 
              marginTop: '12px',
              padding: '12px',
              backgroundColor: '#1e293b',
              borderRadius: '6px',
              borderLeft: '3px solid #8b5cf6',
            }}>
              <div style={{ fontSize: '12px', color: '#8b5cf6', marginBottom: '8px', fontWeight: 600 }}>
                🧠 推理过程
              </div>
              <div style={{ fontSize: '13px', color: '#e2e8f0', whiteSpace: 'pre-wrap', lineHeight: '1.6' }}>
                {issue.aiReasoning}
              </div>
            </div>
          )}

          {/* Pipeline Metrics */}
          {issue.pipelineMetrics && (
            <div style={{
              marginTop: '12px',
              padding: '12px',
              backgroundColor: '#0f172a',
              borderRadius: '6px',
              border: '1px solid #334155',
            }}>
              <div style={{ fontSize: '12px', color: '#94a3b8', marginBottom: '8px', fontWeight: 600 }}>
                ⚙️ 管道性能指标
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '8px', fontSize: '12px' }}>
                <div>
                  <span style={{ color: '#64748b' }}>Embedding:</span>
                  <span style={{ color: '#f8fafc', marginLeft: '8px' }}>{issue.pipelineMetrics.embeddingTimeMs}ms</span>
                </div>
                <div>
                  <span style={{ color: '#64748b' }}>Vector Search:</span>
                  <span style={{ color: '#f8fafc', marginLeft: '8px' }}>{issue.pipelineMetrics.vectorSearchTimeMs}ms</span>
                </div>
                <div>
                  <span style={{ color: '#64748b' }}>LLM Inference:</span>
                  <span style={{ color: '#f8fafc', marginLeft: '8px' }}>{issue.pipelineMetrics.llmInferenceTimeMs}ms</span>
                </div>
                <div>
                  <span style={{ color: '#64748b' }}>总计:</span>
                  <span style={{ color: '#f8fafc', marginLeft: '8px' }}>{issue.pipelineMetrics.totalPipelineTimeMs}ms</span>
                </div>
                {issue.pipelineMetrics.tokensUsed && (
                  <div>
                    <span style={{ color: '#64748b' }}>Tokens:</span>
                    <span style={{ color: '#f8fafc', marginLeft: '8px' }}>{issue.pipelineMetrics.tokensUsed}</span>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Validation Stats */}
          {issue.validationStats && (
            <div style={{
              marginTop: '12px',
              padding: '12px',
              backgroundColor: '#0f172a',
              borderRadius: '6px',
              border: '1px solid #334155',
            }}>
              <div style={{ fontSize: '12px', color: '#94a3b8', marginBottom: '8px', fontWeight: 600 }}>
                📊 验证统计
              </div>
              <div style={{ fontSize: '12px', color: '#e2e8f0', lineHeight: '1.8' }}>
                <div>原始置信度: <span style={{ color: '#f8fafc' }}>{(issue.validationStats.originalConfidence * 100).toFixed(1)}%</span></div>
                <div>调整后置信度: <span style={{ color: '#f8fafc' }}>{(issue.validationStats.adjustedConfidence * 100).toFixed(1)}%</span></div>
                <div>验证时间: <span style={{ color: '#f8fafc' }}>{new Date(issue.validationStats.validationTimestamp).toLocaleString()}</span></div>
                <div>验证器版本: <span style={{ color: '#f8fafc' }}>{issue.validationStats.validatorVersion}</span></div>
              </div>
            </div>
          )}

          {/* Retry Count */}
          {issue.retryCount !== undefined && issue.retryCount > 0 && (
            <div style={{
              marginTop: '12px',
              padding: '8px 12px',
              backgroundColor: '#1e293b',
              borderRadius: '6px',
              fontSize: '12px',
              color: '#f59e0b',
            }}>
              🔄 重试次数: {issue.retryCount} / {issue.maxRetries || 3}
            </div>
          )}

          {/* Degradation Strategy */}
          {issue.degradationStrategy && (
            <div style={{
              marginTop: '12px',
              padding: '8px 12px',
              backgroundColor: '#7f1d1d',
              borderRadius: '6px',
              fontSize: '12px',
              color: '#fecaca',
            }}>
              ⚠️ 降级策略: {issue.degradationStrategy}
              {issue.degradationReason && <div style={{ marginTop: '4px', opacity: 0.8 }}>{issue.degradationReason}</div>}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

const getValidationActionColor = (action: string): string => {
  const colors: Record<string, string> = {
    ACCEPT: '#10b981',
    DOWNGRADE: '#f59e0b',
    FLAG_FOR_REVIEW: '#3b82f6',
    RETRY_WITH_CONTEXT: '#8b5cf6',
  };
  return colors[action] || '#64748b';
};

const getValidationActionLabel = (action: string): string => {
  const labels: Record<string, string> = {
    ACCEPT: '✅ 接受',
    DOWNGRADE: '⬇️ 降级',
    FLAG_FOR_REVIEW: '🚩 标记待审',
    RETRY_WITH_CONTEXT: '🔄 重试',
  };
  return labels[action] || action;
};

export default AiReviewView;
