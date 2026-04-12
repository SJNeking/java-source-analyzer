/**
 * Data Validator
 * 
 * Runtime validation for backend data to prevent crashes from schema changes.
 * Provides type guards and detailed error messages.
 */

import type { AnalysisResult, Asset, GraphData } from '../types';
import { Logger } from '../utils/logger';

export class DataValidator {
  /**
   * Validate AnalysisResult structure
   * Throws error with details if invalid
   */
  static validateAnalysisResult(data: unknown): asserts data is AnalysisResult {
    if (!data || typeof data !== 'object') {
      throw new Error('Invalid data: expected object');
    }

    const result = data as Record<string, unknown>;

    // Check required fields
    const requiredFields = ['framework', 'version', 'assets', 'quality_summary'];
    for (const field of requiredFields) {
      if (!(field in result)) {
        throw new Error(`Missing required field: "${field}"`);
      }
    }

    // Validate types
    if (typeof result.framework !== 'string') {
      throw new Error(`Invalid framework: expected string, got ${typeof result.framework}`);
    }

    if (typeof result.version !== 'string') {
      throw new Error(`Invalid version: expected string, got ${typeof result.version}`);
    }

    if (!Array.isArray(result.assets)) {
      throw new Error(`Invalid assets: expected array, got ${typeof result.assets}`);
    }

    // Validate assets structure (sample first 5)
    const sampleSize = Math.min(5, result.assets.length);
    for (let i = 0; i < sampleSize; i++) {
      this.validateAsset(result.assets[i], i);
    }

    Logger.success('Data validation passed');
  }

  /**
   * Validate GraphData structure
   */
  static validateGraphData(data: unknown): asserts data is GraphData {
    if (!data || typeof data !== 'object') {
      throw new Error('Invalid graph data: expected object');
    }

    const graph = data as Record<string, unknown>;

    if (!Array.isArray(graph.nodes)) {
      throw new Error(`Invalid nodes: expected array, got ${typeof graph.nodes}`);
    }

    if (!Array.isArray(graph.links)) {
      throw new Error(`Invalid links: expected array, got ${typeof graph.links}`);
    }

    if (typeof graph.framework !== 'string') {
      throw new Error(`Invalid framework: expected string`);
    }

    if (typeof graph.version !== 'string') {
      throw new Error(`Invalid version: expected string`);
    }

    Logger.success('Graph data validation passed');
  }

  /**
   * Safely parse and validate JSON
   */
  static safeParseJSON(json: string, context: string = 'unknown'): unknown {
    try {
      return JSON.parse(json);
    } catch (error) {
      Logger.error(`Invalid JSON in ${context}:`, error);
      throw new Error(`Failed to parse JSON (${context}): ${(error as Error).message}`);
    }
  }

  /**
   * Validate single asset
   */
  private static validateAsset(asset: unknown, index: number): void {
    if (!asset || typeof asset !== 'object') {
      throw new Error(`Invalid asset at index ${index}: expected object`);
    }

    const a = asset as Record<string, unknown>;

    if (typeof a.address !== 'string' || !a.address) {
      throw new Error(`Invalid asset at index ${index}: missing or empty "address"`);
    }

    if (typeof a.kind !== 'string') {
      Logger.warning(`Asset at index ${index} missing "kind", defaulting to CLASS`);
    }
  }

  /**
   * Create fallback data when validation fails
   */
  static createFallbackAnalysisResult(): AnalysisResult {
    return {
      framework: 'Unknown',
      version: '0.0.0',
      scan_date: new Date().toISOString(),
      project_type: { 
        primary_type: 'GENERIC', 
        all_types: [], 
        evidence: [] 
      },
      comment_coverage: { 
        class_comment_coverage_pct: 0,
        method_comment_coverage_pct: 0,
        field_comment_coverage_pct: 0
      },
      quality_summary: { 
        total_issues: 0, 
        by_severity: { CRITICAL: 0, MAJOR: 0, MINOR: 0, INFO: 0 },
        by_category: { BUG: 0, CODE_SMELL: 0, SECURITY: 0 }
      },
      cross_file_relations: { 
        total_relations: 0, 
        relations_by_type: {} 
      },
      assets: [],
      dependencies: [],
    };
  }
}
