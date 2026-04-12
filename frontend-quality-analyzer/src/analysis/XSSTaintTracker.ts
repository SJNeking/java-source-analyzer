/**
 * XSS Data Flow Tracker — Taint Analysis Engine
 *
 * Tracks user input (sources) through the code to dangerous DOM sinks (innerHTML, dangerouslySetInnerHTML, etc.)
 * Reports potential XSS vulnerabilities with the full data flow path.
 *
 * Mirrors the Java analyzer's TaintEngine architecture.
 */

import { Severity } from '../types';
import { IssueCategory } from '../types';

// ==================== Type Definitions ====================

export interface TaintSource {
  type: string;
  variable: string;
  line: number;
  confidence: number;
}

export interface TaintSink {
  type: string;
  line: number;
  evidence: string;
  isSanitized: boolean;
  sanitizer?: string;
}

export interface TaintFlow {
  source: TaintSource;
  sink: TaintSink;
  path: TaintFlowStep[];
  severity: Severity;
}

export interface TaintFlowStep {
  line: number;
  description: string;
  code: string;
}

// ==================== Known Sources (user-controlled input) ====================

const TAINT_SOURCES = [
  // URL parameters
  { pattern: /window\.location\.search/, type: 'URL parameter', confidence: 0.9 },
  { pattern: /window\.location\.hash/, type: 'URL hash', confidence: 0.9 },
  { pattern: /URLSearchParams/, type: 'URL parameter', confidence: 0.85 },
  { pattern: /new URL\(/, type: 'URL parameter', confidence: 0.85 },

  // DOM events
  { pattern: /event\.target\.value/, type: 'DOM event value', confidence: 0.8 },
  { pattern: /event\.target\.textContent/, type: 'DOM event textContent', confidence: 0.7 },
  { pattern: /document\.cookie/, type: 'document.cookie', confidence: 0.8 },

  // Web APIs
  { pattern: /fetch\(/, type: 'fetch response', confidence: 0.7 },
  { pattern: /axios\./, type: 'axios response', confidence: 0.7 },
  { pattern: /\.json\(\)/, type: 'JSON response', confidence: 0.6 },
  { pattern: /\.text\(\)/, type: 'text response', confidence: 0.6 },

  // Storage
  { pattern: /localStorage\.getItem/, type: 'localStorage', confidence: 0.75 },
  { pattern: /sessionStorage\.getItem/, type: 'sessionStorage', confidence: 0.75 },

  // Message events
  { pattern: /postMessage/, type: 'postMessage', confidence: 0.8 },
  { pattern: /message\.data/, type: 'message data', confidence: 0.85 },

  // Form inputs
  { pattern: /querySelector.*\.value/, type: 'input value', confidence: 0.7 },
  { pattern: /getElementById.*\.value/, type: 'input value', confidence: 0.7 },
  { pattern: /getElementsByName/, type: 'form data', confidence: 0.7 },

  // Props/State in React (less confident)
  { pattern: /props\.\w+/, type: 'React prop', confidence: 0.5 },
  { pattern: /state\.\w+/, type: 'React state', confidence: 0.5 },
];

// ==================== Known Sinks (dangerous DOM operations) ====================

const TAINT_SINKS = [
  { pattern: /\.innerHTML\s*=/, type: 'innerHTML assignment', severity: Severity.CRITICAL },
  { pattern: /\.outerHTML\s*=/, type: 'outerHTML assignment', severity: Severity.CRITICAL },
  { pattern: /document\.write\(/, type: 'document.write()', severity: Severity.CRITICAL },
  { pattern: /document\.writeln\(/, type: 'document.writeln()', severity: Severity.CRITICAL },
  { pattern: /dangerouslySetInnerHTML/, type: 'dangerouslySetInnerHTML', severity: Severity.CRITICAL },
  { pattern: /insertAdjacentHTML\(/, type: 'insertAdjacentHTML()', severity: Severity.MAJOR },
  { pattern: /\.replaceWith\(/, type: 'replaceWith() with HTML', severity: Severity.MAJOR },
  { pattern: /DOMPurify\.sanitize/, type: 'DOMPurify.sanitize', severity: Severity.INFO },
  { pattern: /eval\(/, type: 'eval()', severity: Severity.CRITICAL },
  { pattern: /new Function\(/, type: 'new Function()', severity: Severity.CRITICAL },
  { pattern: /setTimeout\(.*string/, type: 'setTimeout with string', severity: Severity.MAJOR },
  { pattern: /setInterval\(.*string/, type: 'setInterval with string', severity: Severity.MAJOR },
];

// ==================== Known Sanitizers ====================

const SANITIZERS = [
  'DOMPurify.sanitize',
  'sanitizeHtml',
  'escapeHtml',
  'he.encode',
  'escape',
  'encodeURIComponent',
  'String.raw',
  'xss',
  'sanitize',
];

// ==================== XSS Taint Tracker ====================

export class XSSTaintTracker {
  /**
   * Analyze source code for potential XSS data flows
   */
  analyze(sourceCode: string, filePath: string): TaintFlow[] {
    const flows: TaintFlow[] = [];
    const lines = sourceCode.split('\n');

    // Step 1: Find all taint sources
    const sources = this.findSources(lines, filePath);

    // Step 2: Find all taint sinks
    const sinks = this.findSinks(lines, filePath);

    // Step 3: For each sink, check if there's a reachable source
    for (const sink of sinks) {
      // Skip if already sanitized
      if (sink.isSanitized) continue;

      const reachableSources = sources.filter(source => {
        // Source must be before the sink (line-wise)
        return source.line < sink.line;
      });

      for (const source of reachableSources) {
        // Check if the variable from source is used near the sink
        const sinkContext = lines.slice(
          Math.max(0, sink.line - 10),
          Math.min(lines.length, sink.line + 2)
        ).join('\n');

        if (this.isVariableRelated(source, sinkContext, lines)) {
          const path = this.buildTrace(source, sink, lines);
          flows.push({
            source,
            sink,
            path,
            severity: sink.severity,
          });
          break; // One flow per sink is enough
        }
      }
    }

    return flows;
  }

  /**
   * Find all taint sources in the code
   */
  private findSources(lines: string[], filePath: string): TaintSource[] {
    const sources: TaintSource[] = [];

    lines.forEach((line, index) => {
      for (const sourceDef of TAINT_SOURCES) {
        if (sourceDef.pattern.test(line)) {
          const varName = this.extractVariableName(line, sourceDef.pattern);
          sources.push({
            type: sourceDef.type,
            variable: varName || 'unknown',
            line: index + 1,
            confidence: sourceDef.confidence,
          });
        }
      }
    });

    return sources;
  }

  /**
   * Find all taint sinks in the code
   */
  private findSinks(lines: string[], filePath: string): TaintSink[] {
    const sinks: TaintSink[] = [];

    lines.forEach((line, index) => {
      for (const sinkDef of TAINT_SINKS) {
        if (sinkDef.pattern.test(line)) {
          // Check if sanitized
          const isSanitized = this.isSanitized(line, lines, index);
          const sanitizer = isSanitized ? this.findSanitizer(line) : undefined;

          sinks.push({
            type: sinkDef.type,
            line: index + 1,
            evidence: line.trim(),
            isSanitized,
            sanitizer,
          });
        }
      }
    });

    return sinks;
  }

  /**
   * Check if a sink is sanitized
   */
  private isSanitized(line: string, lines: string[], index: number): boolean {
    // Check same line
    for (const sanitizer of SANITIZERS) {
      if (line.includes(sanitizer)) return true;
    }

    // Check surrounding context (10 lines before/after)
    const context = lines.slice(
      Math.max(0, index - 10),
      Math.min(lines.length, index + 10)
    ).join('\n');

    for (const sanitizer of SANITIZERS) {
      if (context.includes(sanitizer)) return true;
    }

    return false;
  }

  /**
   * Find which sanitizer is used
   */
  private findSanitizer(line: string): string | undefined {
    for (const sanitizer of SANITIZERS) {
      if (line.includes(sanitizer)) return sanitizer;
    }
    return undefined;
  }

  /**
   * Extract variable name from a source line
   */
  private extractVariableName(line: string, pattern: RegExp): string | null {
    // Try to find assignment: const x = ... or let x = ...
    const assignMatch = line.match(/(?:const|let|var)\s+(\w+)\s*=/);
    if (assignMatch) return assignMatch[1];

    // Try to find property access: const x = event.target.value
    const propMatch = line.match(/(\w+)\s*=\s*\w+\.\w+/);
    if (propMatch) return propMatch[1];

    return null;
  }

  /**
   * Check if source variable is related to sink usage
   */
  private isVariableRelated(source: TaintSource, sinkContext: string, lines: string[]): boolean {
    // If source variable name is in sink context, likely related
    if (source.variable !== 'unknown' && sinkContext.includes(source.variable)) {
      return true;
    }

    // For URL-based sources, check if any URL parameter is used in sink context
    if (source.type.includes('URL') && sinkContext.includes('url') || sinkContext.includes('URL')) {
      return true;
    }

    // For fetch/axios, check if response is assigned to something
    if (source.type.includes('fetch') || source.type.includes('axios')) {
      const responsePattern = /\b(data|response|result|res|json)\b/;
      if (responsePattern.test(sinkContext)) return true;
    }

    // For props/state, check if the specific prop/state is used
    if (source.type.includes('prop') || source.type.includes('state')) {
      return sinkContext.includes(source.variable) || sinkContext.includes(source.type);
    }

    // Conservative: don't flag unless there's strong evidence
    if (source.confidence < 0.7) return false;

    return true;
  }

  /**
   * Build the trace path from source to sink
   */
  private buildTrace(source: TaintSource, sink: TaintSink, lines: string[]): TaintFlowStep[] {
    const steps: TaintFlowStep[] = [];

    // Source step
    steps.push({
      line: source.line,
      description: `User input: ${source.type}`,
      code: lines[source.line - 1]?.trim() || '',
    });

    // Intermediate steps (assignments/transformations)
    if (source.variable !== 'unknown') {
      for (let i = source.line; i < sink.line; i++) {
        const line = lines[i];
        if (line && line.includes(source.variable) && !line.trim().startsWith('//')) {
          // Check if this is an assignment or transformation
          if (/(=|\.map\(|\.filter\(|\.reduce\(|\.concat\(|\.split\()/i.test(line)) {
            steps.push({
              line: i + 1,
              description: `Variable transformation`,
              code: line.trim(),
            });
          }
        }
      }
    }

    // Sink step
    steps.push({
      line: sink.line,
      description: `Dangerous sink: ${sink.type}`,
      code: sink.evidence,
    });

    return steps;
  }
}
