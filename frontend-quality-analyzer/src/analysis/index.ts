/**
 * Advanced Analysis Module
 *
 * Exports all advanced analysis tools:
 * - XSS Data Flow Tracker
 * - Component Dependency Graph
 * - Control Flow Graph
 * - Cross-file Relation Engine
 */

export { XSSTaintTracker } from './XSSTaintTracker';
export { ComponentDependencyAnalyzer } from './ComponentDependencyGraph';
export { CFGBuilder, analyzeCFGIssues } from './ControlFlowGraph';
export { CrossFileRelationAnalyzer } from './CrossFileRelationEngine';
