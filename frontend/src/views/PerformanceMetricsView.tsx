/**
 * PerformanceMetricsView - React Component
 * 性能监控面板（Harness Engineering）
 */

import React, { useMemo } from 'react';
import type { UnifiedReport, UnifiedIssue } from '@/types/unified-issue';
import { useAppStore } from '@store/app-store';

interface TimeDistribution {
  range: string;
  count: number;
  color: string;
}

const PerformanceMetricsView: React.FC = () => {
  const { unifiedReport } = useAppStore();
  const [timeRange, setTimeRange] = React.useState<'all' | 'fast' | 'medium' | 'slow'>('all');

  // Calculate performance metrics from pipeline data
  const performanceData = useMemo(() => {
    if (!unifiedReport?.issues) return null;

    const issuesWithMetrics = unifiedReport.issues.filter(i => i.pipelineMetrics);
    if (issuesWithMetrics.length === 0) return null;

    // Extract all timing data
    const embeddingTimes = issuesWithMetrics.map(i => i.pipelineMetrics?.embeddingTimeMs || 0);
    const vectorSearchTimes = issuesWithMetrics.map(i => i.pipelineMetrics?.vectorSearchTimeMs || 0);
    const llmTimes = issuesWithMetrics.map(i => i.pipelineMetrics?.llmInferenceTimeMs || 0);
    const totalTimes = issuesWithMetrics.map(i => i.pipelineMetrics?.totalPipelineTimeMs || 0);

    // Calculate statistics
    const calcStats = (times: number[]) => ({
      min: Math.min(...times),
      max: Math.max(...times),
      avg: Math.round(times.reduce((a, b) => a + b, 0) / times.length),
      p50: percentile(times, 50),
      p90: percentile(times, 90),
      p95: percentile(times, 95),
      p99: percentile(times, 99),
    });

    return {
      embedding: calcStats(embeddingTimes),
      vectorSearch: calcStats(vectorSearchTimes),
      llm: calcStats(llmTimes),
      total: calcStats(totalTimes),
      totalIssues: issuesWithMetrics.length,
      tokensUsed: issuesWithMetrics.reduce((sum, i) => sum + (i.pipelineMetrics?.tokensUsed || 0), 0),
    };
  }, [unifiedReport]);

  // Time distribution for total pipeline time
  const timeDistribution = useMemo<TimeDistribution[]>(() => {
    if (!performanceData) return [];

    const issues = unifiedReport!.issues.filter(i => i.pipelineMetrics);
    const totalTimes = issues.map(i => i.pipelineMetrics!.totalPipelineTimeMs);

    const ranges = [
      { label: '< 1s', threshold: 1000, color: '#10b981' },
      { label: '1-2s', threshold: 2000, color: '#3b82f6' },
      { label: '2-5s', threshold: 5000, color: '#f59e0b' },
      { label: '> 5s', threshold: Infinity, color: '#ef4444' },
    ];

    let prevThreshold = 0;
    return ranges.map(range => {
      const count = totalTimes.filter(t => t > prevThreshold && t <= range.threshold).length;
      prevThreshold = range.threshold;
      return {
        range: range.label,
        count,
        color: range.color,
      };
    });
  }, [performanceData, unifiedReport]);

  // Filter by time range
  const filteredIssues = useMemo(() => {
    if (!unifiedReport?.issues) return [];

    let issues = unifiedReport.issues.filter(i => i.pipelineMetrics);

    if (timeRange !== 'all') {
      const thresholds = {
        fast: 1000,
        medium: 3000,
        slow: 5000,
      };

      if (timeRange === 'fast') {
        issues = issues.filter(i => (i.pipelineMetrics?.totalPipelineTimeMs || 0) < thresholds.fast);
      } else if (timeRange === 'medium') {
        issues = issues.filter(i => {
          const t = i.pipelineMetrics?.totalPipelineTimeMs || 0;
          return t >= thresholds.fast && t < thresholds.medium;
        });
      } else if (timeRange === 'slow') {
        issues = issues.filter(i => (i.pipelineMetrics?.totalPipelineTimeMs || 0) >= thresholds.slow);
      }
    }

    return issues.sort((a, b) => 
      (b.pipelineMetrics?.totalPipelineTimeMs || 0) - (a.pipelineMetrics?.totalPipelineTimeMs || 0)
    );
  }, [unifiedReport, timeRange]);

  if (!unifiedReport) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
        <h3>⚡ 暂无性能数据</h3>
        <p style={{ marginTop: '12px', fontSize: '13px' }}>
          性能监控需要先运行 CodeGuardian AI 模块。<br/>
          此视图展示 RAG 管道各阶段的性能指标。
        </p>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', height: '100%', overflow: 'auto' }}>
      <h2 style={{ marginBottom: '24px', fontSize: '20px', color: '#f8fafc' }}>
        ⚡ 性能监控
      </h2>

      {/* Key Metrics Cards */}
      {performanceData && (
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
          gap: '12px',
          marginBottom: '32px'
        }}>
          <MetricCard 
            title="平均总耗时" 
            value={`${performanceData.total.avg}ms`} 
            icon="⏱️"
            color="#3b82f6"
          />
          <MetricCard 
            title="P95 耗时" 
            value={`${performanceData.total.p95}ms`} 
            icon="📊"
            color="#8b5cf6"
          />
          <MetricCard 
            title="最慢耗时" 
            value={`${performanceData.total.max}ms`} 
            icon="🐌"
            color="#ef4444"
          />
          <MetricCard 
            title="最快耗时" 
            value={`${performanceData.total.min}ms`} 
            icon="🚀"
            color="#10b981"
          />
          <MetricCard 
            title="处理问题数" 
            value={performanceData.totalIssues} 
            icon="📝"
            color="#f59e0b"
          />
          <MetricCard 
            title="总 Tokens" 
            value={performanceData.tokensUsed.toLocaleString()} 
            icon="🔢"
            color="#ec4899"
          />
        </div>
      )}

      {/* Stage Breakdown */}
      {performanceData && (
        <div style={{
          backgroundColor: '#0f172a',
          border: '1px solid #334155',
          borderRadius: '8px',
          padding: '20px',
          marginBottom: '24px',
        }}>
          <h3 style={{ margin: '0 0 16px 0', fontSize: '16px', color: '#f8fafc' }}>
            管道阶段耗时分解
          </h3>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <StageBar 
              name="Code Slicer" 
              icon="✂️"
              avg={performanceData.embedding.avg}
              p95={performanceData.embedding.p95}
              max={performanceData.embedding.max}
              color="#3b82f6"
            />
            <StageBar 
              name="Vector Embedding" 
              icon="🔢"
              avg={performanceData.vectorSearch.avg}
              p95={performanceData.vectorSearch.p95}
              max={performanceData.vectorSearch.max}
              color="#8b5cf6"
            />
            <StageBar 
              name="LLM Inference" 
              icon="🤖"
              avg={performanceData.llm.avg}
              p95={performanceData.llm.p95}
              max={performanceData.llm.max}
              color="#f59e0b"
            />
          </div>
        </div>
      )}

      {/* Time Distribution */}
      {timeDistribution.length > 0 && (
        <div style={{
          backgroundColor: '#0f172a',
          border: '1px solid #334155',
          borderRadius: '8px',
          padding: '20px',
          marginBottom: '24px',
        }}>
          <h3 style={{ margin: '0 0 16px 0', fontSize: '16px', color: '#f8fafc' }}>
            耗时分布
          </h3>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {timeDistribution.map(dist => (
              <div key={dist.range} style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <span style={{ fontSize: '13px', color: '#94a3b8', minWidth: '60px' }}>{dist.range}</span>
                <div style={{ 
                  flex: 1, 
                  height: '28px', 
                  backgroundColor: '#1e293b',
                  borderRadius: '4px',
                  overflow: 'hidden',
                  position: 'relative',
                }}>
                  <div style={{
                    width: `${Math.min((dist.count / (performanceData?.totalIssues || 1)) * 100, 100)}%`,
                    height: '100%',
                    backgroundColor: dist.color,
                    transition: 'width 0.3s',
                  }} />
                  <span style={{
                    position: 'absolute',
                    right: '8px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    fontSize: '12px',
                    color: '#f8fafc',
                    fontWeight: 600,
                  }}>
                    {dist.count}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Slow Issues Table */}
      <div style={{
        backgroundColor: '#0f172a',
        border: '1px solid #334155',
        borderRadius: '8px',
        padding: '20px',
      }}>
        <div style={{ 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center',
          marginBottom: '16px',
        }}>
          <h3 style={{ margin: 0, fontSize: '16px', color: '#f8fafc' }}>
            问题性能列表
          </h3>

          <div style={{ display: 'flex', gap: '8px' }}>
            {(['all', 'fast', 'medium', 'slow'] as const).map(range => (
              <button
                key={range}
                onClick={() => setTimeRange(range)}
                style={{
                  padding: '6px 12px',
                  backgroundColor: timeRange === range ? '#3b82f6' : 'transparent',
                  color: timeRange === range ? '#f8fafc' : '#94a3b8',
                  border: '1px solid #334155',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  fontSize: '12px',
                }}
              >
                {range === 'all' ? '全部' :
                 range === 'fast' ? '< 1s' :
                 range === 'medium' ? '1-3s' : '> 5s'}
              </button>
            ))}
          </div>
        </div>

        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid #334155' }}>
              <th style={{ padding: '12px', textAlign: 'left', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>问题</th>
              <th style={{ padding: '12px', textAlign: 'right', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>Embedding</th>
              <th style={{ padding: '12px', textAlign: 'right', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>Vector Search</th>
              <th style={{ padding: '12px', textAlign: 'right', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>LLM</th>
              <th style={{ padding: '12px', textAlign: 'right', color: '#94a3b8', fontSize: '12px', fontWeight: 600 }}>总计</th>
            </tr>
          </thead>
          <tbody>
            {filteredIssues.length === 0 ? (
              <tr>
                <td colSpan={5} style={{ padding: '40px', textAlign: 'center', color: '#64748b' }}>
                  没有符合条件的问题
                </td>
              </tr>
            ) : (
              filteredIssues.slice(0, 20).map(issue => (
                <tr 
                  key={issue.id}
                  style={{ borderBottom: '1px solid #1e293b' }}
                >
                  <td style={{ padding: '12px', color: '#f8fafc', fontSize: '13px' }}>
                    {issue.message.substring(0, 60)}...
                  </td>
                  <td style={{ padding: '12px', color: '#94a3b8', fontSize: '13px', textAlign: 'right' }}>
                    {issue.pipelineMetrics?.embeddingTimeMs}ms
                  </td>
                  <td style={{ padding: '12px', color: '#94a3b8', fontSize: '13px', textAlign: 'right' }}>
                    {issue.pipelineMetrics?.vectorSearchTimeMs}ms
                  </td>
                  <td style={{ padding: '12px', color: '#94a3b8', fontSize: '13px', textAlign: 'right' }}>
                    {issue.pipelineMetrics?.llmInferenceTimeMs}ms
                  </td>
                  <td style={{ padding: '12px', fontWeight: 600, textAlign: 'right' }}>
                    <span style={{
                      padding: '4px 8px',
                      backgroundColor: getLatencyColor(issue.pipelineMetrics?.totalPipelineTimeMs || 0),
                      color: '#fff',
                      borderRadius: '4px',
                      fontSize: '12px',
                    }}>
                      {issue.pipelineMetrics?.totalPipelineTimeMs}ms
                    </span>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {filteredIssues.length > 20 && (
          <div style={{ 
            marginTop: '12px', 
            textAlign: 'center', 
            color: '#64748b',
            fontSize: '13px',
          }}>
            显示前 20 条，共 {filteredIssues.length} 条
          </div>
        )}
      </div>
    </div>
  );
};

const MetricCard: React.FC<{ title: string; value: number | string; icon: string; color: string }> = ({
  title, value, icon, color
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
      <div style={{ fontSize: '12px', color: '#64748b', marginTop: '2px' }}>{title}</div>
    </div>
  </div>
);

const StageBar: React.FC<{ 
  name: string; 
  icon: string;
  avg: number; 
  p95: number; 
  max: number;
  color: string;
}> = ({ name, icon, avg, p95, max, color }) => (
  <div>
    <div style={{ 
      display: 'flex', 
      justifyContent: 'space-between',
      marginBottom: '8px',
      fontSize: '13px',
    }}>
      <span style={{ color: '#f8fafc', fontWeight: 500 }}>
        {icon} {name}
      </span>
      <span style={{ color: '#94a3b8' }}>
        平均: <span style={{ color, fontWeight: 600 }}>{avg}ms</span> | 
        P95: <span style={{ color, fontWeight: 600 }}>{p95}ms</span> | 
        最大: <span style={{ color, fontWeight: 600 }}>{max}ms</span>
      </span>
    </div>
    <div style={{ 
      height: '8px', 
      backgroundColor: '#1e293b',
      borderRadius: '4px',
      overflow: 'hidden',
    }}>
      <div style={{
        width: `${Math.min((avg / Math.max(max, 1)) * 100, 100)}%`,
        height: '100%',
        backgroundColor: color,
        transition: 'width 0.3s',
      }} />
    </div>
  </div>
);

const percentile = (data: number[], p: number): number => {
  const sorted = [...data].sort((a, b) => a - b);
  const index = Math.ceil((p / 100) * sorted.length) - 1;
  return sorted[Math.max(0, index)];
};

const getLatencyColor = (latency: number): string => {
  if (latency < 1000) return '#10b981';
  if (latency < 3000) return '#3b82f6';
  if (latency < 5000) return '#f59e0b';
  return '#ef4444';
};

export default PerformanceMetricsView;
