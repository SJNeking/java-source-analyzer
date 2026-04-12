/**
 * Unified Report Loader
 *
 * Loads unified-report.json (from ResultMerger) and adapts it
 * for both existing AnalysisResult views and the new AiReviewView.
 *
 * This bridges the gap between:
 *   - Legacy format: analysis_result.json (quality_issues, assets, etc.)
 *   - Unified format:  unified-report.json (issues, summary, ragContext, etc.)
 */

import type { AnalysisResult, GraphData, Asset, QualityIssue } from '../types';
import type { UnifiedReport, UnifiedIssue, EngineInfo, ReviewSummary } from '../types/unified-issue';
import { Logger } from './logger';

/**
 * Load unified report if available, otherwise fall back to legacy analysis result
 */
export async function loadUnifiedReport(dataPath: string, filename: string): Promise<{
  unifiedReport: UnifiedReport | null;
  analysisResult: AnalysisResult;
}> {
  // Try loading unified-report.json first
  const unifiedFilename = 'unified-report.json';
  try {
    const response = await fetch(`${dataPath}${unifiedFilename}?t=${Date.now()}`);
    if (response.ok) {
      const unifiedReport: UnifiedReport = await response.json();
      Logger.success(`Loaded unified report: ${unifiedReport.issues?.length || 0} issues`);

      // Convert unified report to AnalysisResult for backward compatibility
      const analysisResult = convertUnifiedToAnalysisResult(unifiedReport);
      return { unifiedReport, analysisResult };
    }
  } catch {
    // Fall through to legacy loading
  }

  // Fall back to loading the original analysis result
  const response = await fetch(`${dataPath}${filename}?t=${Date.now()}`);
  if (!response.ok) throw new Error(`HTTP ${response.status}: File not found - ${filename}`);
  const analysisResult: AnalysisResult = await response.json();

  // Check if it contains unified issues embedded (from ResultMerger)
  const raw = analysisResult as any;
  if (raw.unified_issues && Array.isArray(raw.unified_issues)) {
    const unifiedReport: UnifiedReport = {
      projectName: raw.projectName || analysisResult.framework || 'unknown',
      projectVersion: raw.projectVersion || analysisResult.version || '0.0.0',
      commitSha: raw.commitSha || '',
      branch: raw.branch || '',
      timestamp: raw.timestamp || Date.now(),
      analysisDurationMs: raw.analysisDurationMs || 0,
      staticEngine: raw.staticEngine || undefined,
      aiEngine: raw.aiEngine || undefined,
      summary: raw.summary || undefined,
      issues: raw.unified_issues as UnifiedIssue[],
    };
    Logger.success(`Loaded embedded unified issues: ${unifiedReport.issues.length}`);
    return { unifiedReport, analysisResult };
  }

  return { unifiedReport: null, analysisResult };
}

/**
 * Convert UnifiedReport to AnalysisResult (for legacy view compatibility)
 */
function convertUnifiedToAnalysisResult(report: UnifiedReport): AnalysisResult {
  // Convert unified issues back to legacy QualityIssue format
  const qualityIssues: QualityIssue[] = report.issues
    .filter(i => !i.autoFiltered)
    .map(i => ({
      rule_key: i.ruleKey,
      rule_name: i.ruleName,
      severity: i.severity as QualityIssue['severity'],
      category: i.category,
      file: i.filePath,
      class: i.className,
      method: i.methodName || '',
      line: i.line,
      message: i.message,
      description: i.aiSuggestion || '',
    }));

  // Build quality summary from unified summary
  const summary = report.summary;
  const qualitySummary = summary ? {
    total_issues: summary.activeIssues || summary.totalIssues,
    by_severity: {
      CRITICAL: summary.critical,
      MAJOR: summary.major,
      MINOR: summary.minor,
      INFO: summary.info,
    },
    by_category: summary.byCategory || {},
  } : { total_issues: 0, by_severity: { CRITICAL: 0, MAJOR: 0, MINOR: 0, INFO: 0 }, by_category: {} };

  return {
    framework: report.projectName,
    version: report.projectVersion,
    scan_date: new Date(report.timestamp).toISOString(),
    project_type: { primary_type: 'GENERIC', all_types: [], evidence: [] } as any,
    comment_coverage: { class_comment_coverage_pct: 0, method_comment_coverage_pct: 0, field_comment_coverage_pct: 0 },
    quality_summary: qualitySummary,
    quality_issues: qualityIssues,
    cross_file_relations: { total_relations: 0, relations_by_type: {} },
    assets: [],
    dependencies: [],
    // Attach unified data for AiReviewView
    unified_issues: report.issues,
    summary: report.summary,
    aiEngine: report.aiEngine,
    staticEngine: report.staticEngine,
    ragContext: (report as any).ragContext || null,
  } as unknown as AnalysisResult;
}
