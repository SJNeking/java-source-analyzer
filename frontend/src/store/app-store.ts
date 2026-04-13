/**
 * Application State Store (Zustand)
 * 
 * Replaces the legacy ApplicationState with modern state management
 */

import { create } from 'zustand';
import type { 
  UnifiedReport, 
  UnifiedIssue, 
  AnalysisResult,
  ViewType 
} from '../types';
import type { GraphData } from '../types/graph';
import type { ProjectInfo } from '../types/project';

interface AppState {
  // Current project
  currentProject: ProjectInfo | null;
  setCurrentProject: (project: ProjectInfo) => void;
  
  // Projects list
  projects: ProjectInfo[];
  setProjects: (projects: ProjectInfo[]) => void;
  
  // Graph data
  graphData: GraphData | null;
  setGraphData: (data: GraphData) => void;
  
  // Full analysis result (legacy format)
  fullAnalysisData: AnalysisResult | null;
  setFullAnalysisData: (data: AnalysisResult) => void;
  
  // Unified report (new format with Harness Engineering)
  unifiedReport: UnifiedReport | null;
  setUnifiedReport: (report: UnifiedReport) => void;
  
  // Current view
  currentView: ViewType;
  setCurrentView: (view: ViewType) => void;
  
  // Node type filters (for graph)
  nodeTypeFilters: Record<string, boolean>;
  setNodeTypeFilter: (type: string, enabled: boolean) => void;
  
  // Loading state
  isLoading: boolean;
  setLoading: (loading: boolean) => void;
  
  // Error state
  error: string | null;
  setError: (error: string | null) => void;
  
  // Reset store
  reset: () => void;
}

const initialState = {
  currentProject: null,
  projects: [],
  graphData: null,
  fullAnalysisData: null,
  unifiedReport: null,
  currentView: 'explorer' as ViewType,
  nodeTypeFilters: {},
  isLoading: false,
  error: null,
};

export const useAppStore = create<AppState>((set) => ({
  ...initialState,
  
  setCurrentProject: (project) => set({ currentProject: project }),
  
  setProjects: (projects) => set({ projects }),
  
  setGraphData: (data) => set({ graphData: data }),
  
  setFullAnalysisData: (data) => set({ fullAnalysisData: data }),
  
  setUnifiedReport: (report) => set({ unifiedReport: report }),
  
  setCurrentView: (view) => set({ currentView: view }),
  
  setNodeTypeFilter: (type, enabled) => 
    set((state) => ({
      nodeTypeFilters: {
        ...state.nodeTypeFilters,
        [type]: enabled,
      },
    })),
  
  setLoading: (loading) => set({ isLoading: loading }),
  
  setError: (error) => set({ error }),
  
  reset: () => set(initialState),
}));

// Selectors for common queries
export const selectActiveIssues = (issues: UnifiedIssue[]): UnifiedIssue[] => {
  return issues.filter(issue => !issue.autoFiltered);
};

export const selectHighConfidenceIssues = (issues: UnifiedIssue[]): UnifiedIssue[] => {
  return issues.filter(issue => 
    issue.confidenceLevel === 'HIGH' || (issue.confidence && issue.confidence >= 0.9)
  );
};

export const selectValidationStats = (report: UnifiedReport | null) => {
  if (!report?.summary?.validationStats) return null;
  return report.summary.validationStats;
};
