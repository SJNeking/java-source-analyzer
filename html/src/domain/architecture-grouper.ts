/**
 * Architecture Grouper
 * 
 * Groups assets by architecture layers or package structure.
 * Extracted from CodeExplorerView for reusability.
 */

import type { Asset } from '../types';
import { ArchitectureDetector } from '../utils/architecture-detector';

export class ArchitectureGrouper {
  /**
   * Group assets by detected architecture layers
   */
  static groupByArchitecture(assets: Asset[]): Map<string, Asset[]> {
    return ArchitectureDetector.groupByArchitecture(assets);
  }

  /**
   * Group assets by package (first-level package)
   */
  static groupByFirstLevelPackage(assets: Asset[]): Map<string, Asset[]> {
    const groups = new Map<string, Asset[]>();
    
    assets.forEach(asset => {
      const parts = asset.address.split('.');
      const firstLevel = parts.length > 2 ? parts.slice(0, 3).join('.') : parts[0];
      
      if (!groups.has(firstLevel)) {
        groups.set(firstLevel, []);
      }
      groups.get(firstLevel)!.push(asset);
    });
    
    return groups;
  }

  /**
   * Group assets by full package path
   */
  static groupByFullPackage(assets: Asset[]): Map<string, Asset[]> {
    const groups = new Map<string, Asset[]>();
    
    assets.forEach(asset => {
      const parts = asset.address.split('.');
      const packageName = parts.length > 1 ? parts.slice(0, -1).join('.') : 'default';
      
      if (!groups.has(packageName)) {
        groups.set(packageName, []);
      }
      groups.get(packageName)!.push(asset);
    });
    
    return groups;
  }

  /**
   * Sort groups by name
   */
  static sortGroupsByName(groups: Map<string, Asset[]>): Array<[string, Asset[]]> {
    return Array.from(groups.entries()).sort((a, b) => a[0].localeCompare(b[0]));
  }

  /**
   * Get statistics for grouped assets
   */
  static getGroupStats(groups: Map<string, Asset[]>): {
    totalGroups: number;
    avgAssetsPerGroup: number;
    maxGroupSize: number;
    minGroupSize: number;
  } {
    const sizes = Array.from(groups.values()).map(g => g.length);
    
    return {
      totalGroups: groups.size,
      avgAssetsPerGroup: sizes.reduce((sum, size) => sum + size, 0) / groups.size,
      maxGroupSize: Math.max(...sizes),
      minGroupSize: Math.min(...sizes),
    };
  }
}
