/**
 * Filter Engine
 * 
 * Centralized filtering logic for assets, methods, and classes.
 * Replaces scattered filter implementations across views.
 */

import type { Asset, MethodAsset } from '../types';

export interface FilterCriteria {
  keyword?: string;
  kind?: Set<string>;
  hasPublicMethods?: boolean;
  hasStaticMethods?: boolean;
  minMethodCount?: number;
  maxMethodCount?: number;
}

export class FilterEngine {
  /**
   * Filter assets by multiple criteria
   */
  static filterAssets(assets: Asset[], criteria: FilterCriteria): Asset[] {
    let result = [...assets];
    
    // Keyword search (address or method names)
    if (criteria.keyword) {
      const keyword = criteria.keyword.toLowerCase();
      result = result.filter(asset => 
        asset.address.toLowerCase().includes(keyword) ||
        this.assetHasMatchingMethod(asset, keyword)
      );
    }
    
    // Filter by kind (CLASS, INTERFACE, etc.)
    if (criteria.kind && criteria.kind.size > 0) {
      result = result.filter(asset => criteria.kind!.has(asset.kind || 'CLASS'));
    }
    
    // Filter by public methods
    if (criteria.hasPublicMethods) {
      result = result.filter(asset => 
        asset.methods_full?.some(m => m.modifiers?.includes('public')) ||
        asset.methods?.some(m => m.modifiers?.includes('public'))
      );
    }
    
    // Filter by static methods
    if (criteria.hasStaticMethods) {
      result = result.filter(asset => 
        asset.methods_full?.some(m => m.modifiers?.includes('static')) ||
        asset.methods?.some(m => m.modifiers?.includes('static'))
      );
    }
    
    // Filter by method count range
    if (criteria.minMethodCount !== undefined) {
      result = result.filter(asset => 
        (asset.methods_full?.length || asset.methods?.length || 0) >= criteria.minMethodCount!
      );
    }
    
    if (criteria.maxMethodCount !== undefined) {
      result = result.filter(asset => 
        (asset.methods_full?.length || asset.methods?.length || 0) <= criteria.maxMethodCount!
      );
    }
    
    return result;
  }

  /**
   * Filter methods within an asset
   */
  static filterMethods(methods: MethodAsset[], criteria: {
    keyword?: string;
    hasPublic?: boolean;
    hasStatic?: boolean;
  }): MethodAsset[] {
    let result = [...methods];
    
    if (criteria.keyword) {
      const keyword = criteria.keyword.toLowerCase();
      result = result.filter(m => 
        m.name.toLowerCase().includes(keyword) ||
        m.signature?.toLowerCase().includes(keyword)
      );
    }
    
    if (criteria.hasPublic) {
      result = result.filter(m => m.modifiers?.includes('public'));
    }
    
    if (criteria.hasStatic) {
      result = result.filter(m => m.modifiers?.includes('static'));
    }
    
    return result;
  }

  /**
   * Search assets with highlighting support
   */
  static searchAssets(assets: Asset[], keyword: string): Array<{
    asset: Asset;
    matchedMethods: string[];
  }> {
    if (!keyword) {
      return assets.map(asset => ({ asset, matchedMethods: [] }));
    }
    
    const lowerKeyword = keyword.toLowerCase();
    
    return assets
      .map(asset => {
        const matchedMethods = this.getMatchedMethods(asset, lowerKeyword);
        const addressMatch = asset.address.toLowerCase().includes(lowerKeyword);
        
        if (addressMatch || matchedMethods.length > 0) {
          return { asset, matchedMethods };
        }
        
        return null;
      })
      .filter((item): item is NonNullable<typeof item> => item !== null);
  }

  /**
   * Get statistics about filtered results
   */
  static getFilterStats(originalCount: number, filteredCount: number): {
    totalOriginal: number;
    totalFiltered: number;
    reductionPercentage: number;
  } {
    return {
      totalOriginal: originalCount,
      totalFiltered: filteredCount,
      reductionPercentage: ((originalCount - filteredCount) / originalCount) * 100,
    };
  }

  /**
   * Check if asset has any method matching keyword
   */
  private static assetHasMatchingMethod(asset: Asset, keyword: string): boolean {
    const allMethods = [...(asset.methods_full || []), ...(asset.methods || [])];
    return allMethods.some(m => 
      m.name.toLowerCase().includes(keyword) ||
      m.signature?.toLowerCase().includes(keyword)
    );
  }

  /**
   * Get list of matched method names
   */
  private static getMatchedMethods(asset: Asset, keyword: string): string[] {
    const allMethods = [...(asset.methods_full || []), ...(asset.methods || [])];
    return allMethods
      .filter(m => 
        m.name.toLowerCase().includes(keyword) ||
        m.signature?.toLowerCase().includes(keyword)
      )
      .map(m => m.name);
  }
}
