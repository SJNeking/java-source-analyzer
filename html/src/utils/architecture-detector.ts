/**
 * Architecture Detector Utility
 * 
 * Automatically detects project architecture type by analyzing package patterns.
 * Supports DDD, MVC, Microservices, Traditional Layered, and Generic structures.
 */

import type { Asset } from '../types';
import type { ArchitectureType } from '../types';
import { ARCHITECTURE_PATTERNS, ARCHITECTURE_DETECTION_RULES } from '../config/architecture-patterns';

/**
 * Detected layer information
 */
export interface DetectedLayer {
  key: string;
  label: string;
  icon: string;
  count: number;
  assets: Asset[];
}

/**
 * Architecture detection result
 */
export interface ArchitectureInfo {
  type: ArchitectureType;
  typeName: string;
  layers: DetectedLayer[];
  confidence: number; // 0-1 confidence score
}

/**
 * Detect project architecture by analyzing asset package patterns
 */
export class ArchitectureDetector {
  
  /**
   * Main detection method
   */
  static detect(assets: Asset[]): ArchitectureInfo {
    if (!assets || assets.length === 0) {
      return this.createGenericResult();
    }

    // Step 1: Match assets to architecture layers
    const layerMap = this.matchAssetsToLayers(assets);
    
    // Step 2: Determine architecture type
    const archType = this.determineArchitectureType(layerMap);
    
    // Step 3: Build detected layers list
    const layers = this.buildDetectedLayers(layerMap, assets);
    
    // Step 4: Calculate confidence
    const confidence = this.calculateConfidence(archType, layerMap);
    
    return {
      type: archType,
      typeName: this.getArchitectureTypeName(archType),
      layers: layers.sort((a, b) => a.count - b.count),
      confidence
    };
  }

  /**
   * Match assets to architecture layer patterns
   */
  private static matchAssetsToLayers(assets: Asset[]): Map<string, Asset[]> {
    const layerMap = new Map<string, Asset[]>();
    
    assets.forEach(asset => {
      const pkg = asset.address.toLowerCase();
      
      for (const pattern of ARCHITECTURE_PATTERNS) {
        const matched = pattern.patterns.some(p => pkg.includes(p));
        if (matched) {
          if (!layerMap.has(pattern.key)) {
            layerMap.set(pattern.key, []);
          }
          layerMap.get(pattern.key)!.push(asset);
          break; // First match wins (priority order)
        }
      }
    });
    
    return layerMap;
  }

  /**
   * Determine architecture type based on detected layers
   */
  private static determineArchitectureType(layerMap: Map<string, Asset[]>): ArchitectureType {
    const detectedKeys = Array.from(layerMap.keys());
    
    // Check DDD
    const dddRules = ARCHITECTURE_DETECTION_RULES.DDD;
    const hasDDDRequired = dddRules.requiredLayers.every(layer => detectedKeys.includes(layer));
    if (hasDDDRequired) {
      return 'DDD';
    }
    
    // Check MVC
    const mvcRules = ARCHITECTURE_DETECTION_RULES.MVC;
    const hasMVCRequired = mvcRules.requiredLayers.every(layer => detectedKeys.includes(layer));
    if (hasMVCRequired) {
      return 'MVC';
    }
    
    // Check Microservice
    const microserviceRules = ARCHITECTURE_DETECTION_RULES.MICROSERVICE;
    const hasMicroserviceLayers = microserviceRules.optionalLayers.some(layer => detectedKeys.includes(layer));
    if (hasMicroserviceLayers && detectedKeys.length >= 2) {
      return 'MICROSERVICE';
    }
    
    // Check Layered
    const layeredRules = ARCHITECTURE_DETECTION_RULES.LAYERED;
    const hasLayeredLayers = layeredRules.optionalLayers.some(layer => detectedKeys.includes(layer));
    if (hasLayeredLayers) {
      return 'LAYERED';
    }
    
    // Default to Generic
    return 'GENERIC';
  }

  /**
   * Build detected layers with metadata
   */
  private static buildDetectedLayers(
    layerMap: Map<string, Asset[]>,
    allAssets: Asset[]
  ): DetectedLayer[] {
    const layers: DetectedLayer[] = [];
    
    for (const [key, assets] of layerMap.entries()) {
      const pattern = ARCHITECTURE_PATTERNS.find(p => p.key === key);
      if (pattern) {
        layers.push({
          key: pattern.key,
          label: pattern.label,
          icon: pattern.icon,
          count: assets.length,
          assets: assets
        });
      }
    }
    
    // Add ungrouped assets
    const groupedCount = Array.from(layerMap.values()).reduce((sum, arr) => sum + arr.length, 0);
    const ungroupedCount = allAssets.length - groupedCount;
    
    if (ungroupedCount > 0) {
      layers.push({
        key: 'other',
        label: '其他',
        icon: '📦',
        count: ungroupedCount,
        assets: []
      });
    }
    
    return layers;
  }

  /**
   * Calculate detection confidence (0-1)
   */
  private static calculateConfidence(
    archType: ArchitectureType,
    layerMap: Map<string, Asset[]>
  ): number {
    const totalAssets = Array.from(layerMap.values()).reduce((sum, arr) => sum + arr.length, 0);
    if (totalAssets === 0) return 0;
    
    const rules = ARCHITECTURE_DETECTION_RULES[archType];
    if (!rules) return 0.3; // Low confidence for unknown types
    
    const requiredMatched = rules.requiredLayers.filter(layer => layerMap.has(layer)).length;
    const requiredTotal = rules.requiredLayers.length;
    
    if (requiredTotal === 0) {
      // No required layers, check optional
      const optionalMatched = rules.optionalLayers.filter(layer => layerMap.has(layer)).length;
      return Math.min(optionalMatched / 3, 1); // Normalize to max 1
    }
    
    return requiredMatched / requiredTotal;
  }

  /**
   * Get human-readable architecture type name
   */
  private static getArchitectureTypeName(type: ArchitectureType): string {
    const names: Record<ArchitectureType, string> = {
      'DDD': 'DDD 架构',
      'MVC': 'MVC 架构',
      'MICROSERVICE': '微服务架构',
      'LAYERED': '分层架构',
      'GENERIC': '通用架构'
    };
    return names[type];
  }

  /**
   * Create generic result for empty or unrecognized projects
   */
  private static createGenericResult(): ArchitectureInfo {
    return {
      type: 'GENERIC',
      typeName: '通用架构',
      layers: [],
      confidence: 0
    };
  }

  /**
   * Group assets by detected architecture layers
   */
  static groupByArchitecture(assets: Asset[]): Map<string, Asset[]> {
    const info = this.detect(assets);
    const groups = new Map<string, Asset[]>();
    
    // Group by detected layers
    info.layers.forEach(layer => {
      if (layer.assets.length > 0) {
        groups.set(layer.key, layer.assets);
      }
    });
    
    // If no layers detected, fall back to package grouping
    if (groups.size === 0) {
      return this.groupByPackage(assets);
    }
    
    return groups;
  }

  /**
   * Fallback: group assets by package structure
   */
  private static groupByPackage(assets: Asset[]): Map<string, Asset[]> {
    const groups = new Map<string, Asset[]>();
    
    assets.forEach(asset => {
      const parts = asset.address.split('.');
      // Group by 3rd level package (e.g., com.example.**module**.*)
      const groupKey = parts.length >= 4 ? parts.slice(0, 3).join('.') : parts.slice(0, 2).join('.');
      
      if (!groups.has(groupKey)) {
        groups.set(groupKey, []);
      }
      groups.get(groupKey)!.push(asset);
    });
    
    return groups;
  }
}
