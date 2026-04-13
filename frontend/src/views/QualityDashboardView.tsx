/**
 * QualityDashboardView - React Component
 * Displays quality issues with filtering and statistics
 */

import React, { useMemo } from 'react';
import type { UnifiedIssue, Severity, IssueCategory } from '@/types/unified-issue';
import { useAppStore, selectActiveIssues } from '@store/app-store';

const SEVERITY_COLORS: Record<Severity, string> = {
  CRITICAL: '#ef4444',
  MAJOR: '#f59e0b',
  MINOR: '#3b82f6',
  INFO: '#64748b',
};

const CATEGORY_ICONS: Record<IssueCategory, string> = {
  BUG: '🐛',
  CODE_SMELL: '👃',
  SECURITY: '🔒',
  DESIGN: '🎨',
  PERFORMANCE: '⚡',
};

const QualityDashboardView: React.FC = () => {
  const { unifiedReport } = useAppStore();
  const [severityFilter, setSeverityFilter] = React.useState<Severity | 'ALL'>('ALL');
  const [showFiltered, setShowFiltered] = React.useState(false);

  const issues = useMemo(() => {
    if (!unifiedReport?.issues) return [];
    
    let filtered = showFiltered 
      ? unifiedReport.issues 
      : selectActiveIssues(unifiedReport.issues);

    if (severityFilter !== 'ALL') {
      filtered = filtered.filter(issue => issue.severity === severityFilter);
    }

    return filtered.sort((a, b) => {
      const severityOrder = { CRITICAL: 0, MAJOR: 1, MINOR: 2, INFO: 3 };
      return severityOrder[a.severity] - severityOrder[b.severity];
    });
  }, [unifiedReport, severityFilter, showFiltered]);

  const stats = useMemo(() => {
    if (!unifiedReport?.summary) return null;

    const s = unifiedReport.summary;
    return {
      total: s.totalIssues,
      critical: s.critical,
      major: s.major,
      minor: s.minor,
      info: s.info,
      active: s.activeIssues,
      filtered: s.autoFiltered,
      avgConfidence: s.aiAvgConfidence,
      highConfidenceRate: s.aiHighConfidenceRate,
    };
  }, [unifiedReport]);

  if (!unifiedReport) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
        <h3>暂无质量分析数据</h3>
        <p style={{ marginTop: '12px', fontSize: '13px' }}>
          请先运行静态分析或AI审查
        </p>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', height: '100%', overflow: 'auto' }}>
      {/* Statistics Cards */}
      {stats && (
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
          gap: '16px',
          marginBottom: '24px'
        }}>
          <StatCard 
            label="总问题" 
            value={stats.total} 
            color="#f8fafc"
            icon="📊"
          />
          <StatCard 
            label="活跃问题" 
            value={stats.active} 
            color="#3b82f6"
            icon="🔍"
          />
          <StatCard 
            label="已过滤" 
            value={stats.filtered} 
            color="#64748b"
            icon="🚫"
          />
          <StatCard 
            label="严重" 
            value={stats.critical} 
            color="#ef4444"
            icon="🔴"
          />
          <StatCard 
            label="主要" 
            value={stats.major} 
            color="#f59e0b"
            icon="🟠"
          />
          <StatCard 
            label="次要" 
            value={stats.minor} 
            color="#3b82f6"
            icon="🔵"
          />
          {stats.avgConfidence > 0 && (
            <StatCard 
              label="AI平均置信度" 
              value={`${(stats.avgConfidence * 100).toFixed(1)}%`} 
              color="#10b981"
              icon="🎯"
            />
          )}
        </div>
      )}

      {/* Filter Bar */}
      <div style={{ 
        display: 'flex', 
        gap: '12px', 
        marginBottom: '20px',
        alignItems: 'center',
        padding: '12px',
        backgroundColor: '#0f172a',
        borderRadius: '6px',
        border: '1px solid #334155'
      }}>
        <span style={{ color: '#94a3b8', fontSize: '13px' }}>严重程度:</span>
        {(['ALL', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'] as const).map(sev => (
          <button
            key={sev}
            onClick={() => setSeverityFilter(sev)}
            style={{
              padding: '6px 12px',
              backgroundColor: severityFilter === sev ? '#3b82f6' : 'transparent',
              color: severityFilter === sev ? '#f8fafc' : '#94a3b8',
              border: '1px solid #334155',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '12px',
              transition: 'all 0.2s',
            }}
          >
            {sev === 'ALL' ? '全部' : sev}
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
            style={{ cursor: 'pointer' }}
          />
          显示已过滤
        </label>
      </div>

      {/* Issues List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {issues.length === 0 ? (
          <div style={{ 
            padding: '40px', 
            textAlign: 'center',
            color: '#64748b'
          }}>
            没有符合条件的问题
          </div>
        ) : (
          issues.map(issue => (
            <IssueCard key={issue.id} issue={issue} />
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
    padding: '16px',
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
  }}>
    <div style={{ fontSize: '24px' }}>{icon}</div>
    <div>
      <div style={{ fontSize: '24px', fontWeight: 600, color }}>{value}</div>
      <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>{label}</div>
    </div>
  </div>
);

// Issue Card Component
const IssueCard: React.FC<{ issue: UnifiedIssue }> = ({ issue }) => {
  const [expanded, setExpanded] = React.useState(false);

  return (
    <div style={{
      backgroundColor: '#0f172a',
      border: '1px solid #334155',
      borderRadius: '8px',
      padding: '16px',
      transition: 'all 0.2s',
      opacity: issue.autoFiltered ? 0.5 : 1,
    }}>
      {/* Header */}
      <div 
        onClick={() => setExpanded(!expanded)}
        style={{ 
          display: 'flex',
          alignItems: 'flex-start',
          gap: '12px',
          cursor: 'pointer',
        }}
      >
        {/* Severity Badge */}
        <div style={{
          padding: '4px 8px',
          backgroundColor: SEVERITY_COLORS[issue.severity],
          color: '#fff',
          borderRadius: '4px',
          fontSize: '11px',
          fontWeight: 600,
          whiteSpace: 'nowrap',
        }}>
          {issue.severity}
        </div>

        {/* Content */}
        <div style={{ flex: 1 }}>
          <div style={{ 
            fontSize: '14px', 
            fontWeight: 500, 
            color: '#f8fafc',
            marginBottom: '4px'
          }}>
            {CATEGORY_ICONS[issue.category]} {issue.message}
          </div>
          
          <div style={{ fontSize: '12px', color: '#64748b' }}>
            {issue.filePath}:{issue.line}
            {issue.methodName ? ` (${issue.methodName})` : ''}
          </div>

          {/* AI Confidence Badge */}
          {issue.confidence && (
            <div style={{ 
              display: 'inline-block',
              marginTop: '8px',
              padding: '4px 8px',
              backgroundColor: issue.confidence >= 0.9 ? '#10b981' : '#f59e0b',
              color: '#fff',
              borderRadius: '4px',
              fontSize: '11px',
            }}>
              AI置信度: {(issue.confidence * 100).toFixed(0)}%
            </div>
          )}
        </div>

        {/* Expand Icon */}
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
        <div style={{ 
          marginTop: '16px',
          paddingTop: '16px',
          borderTop: '1px solid #334155',
        }}>
          {/* Rule Info */}
          <div style={{ fontSize: '12px', color: '#94a3b8', marginBottom: '8px' }}>
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
              <div style={{ fontSize: '13px', color: '#e2e8f0', whiteSpace: 'pre-wrap' }}>
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
              }}>
                {issue.aiFixedCode}
              </pre>
            </div>
          )}

          {/* Validation Action */}
          {issue.validationAction && (
            <div style={{
              marginTop: '12px',
              padding: '8px 12px',
              backgroundColor: getValidationActionColor(issue.validationAction),
              borderRadius: '6px',
              fontSize: '12px',
              color: '#fff',
              fontWeight: 500,
            }}>
              验证动作: {getValidationActionLabel(issue.validationAction)}
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

export default QualityDashboardView;
