/**
 * Frontend Quality Analyzer — Public API
 *
 * Usage:
 *   import { analyze, RuleEngine, getAllDefaultRules } from 'frontend-quality-analyzer';
 *
 *   const engine = new RuleEngine();
 *   const result = engine.run([{ path: 'src/App.tsx', content: '...' }]);
 */

export { RuleEngine } from './engine/RuleEngine';
export { getAllDefaultRules } from './rules';
export { TypeScriptParser } from './parser/TypeScriptParser';
export { BabelParser } from './parser/BabelParser';
export { JSONReporter, HTMLReporter, MarkdownReporter } from './reporters';
export * from './types';
