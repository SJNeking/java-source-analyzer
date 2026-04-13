/**
 * Type exports barrel file
 */

export * from './unified-issue';
export * from './graph';
export * from './project';

// View type for routing
export type ViewType = 
  | 'explorer'
  | 'graph'
  | 'relations'
  | 'quality'
  | 'frontend-quality'
  | 'metrics'
  | 'assets'
  | 'ai-review'
  | 'pipeline'      // New: RAG Pipeline visualization
  | 'performance';  // New: Performance metrics
