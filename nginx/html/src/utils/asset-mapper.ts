/**
 * Java Source Analyzer - Asset Mapper
 * Solves the "Identity Crisis" by creating a strict, O(1) lookup table for assets.
 * Prevents "Zhang Guan Li Dai" (matching wrong classes) and "Not Found" errors.
 */

export interface AssetMap {
  byAddress: Map<string, any>;        // Key: "com.example.Class"
  bySimpleName: Map<string, any[]>;   // Key: "Class" -> Array (handles duplicates)
}

export class AssetMapper {
  private map: AssetMap = { byAddress: new Map(), bySimpleName: new Map() };
  private isBuilt: boolean = false;

  /**
   * Build the index from raw analysis data.
   * This is O(N) but only happens once per project load.
   */
  public build(assets: any[]): void {
    this.map = { byAddress: new Map(), bySimpleName: new Map() };
    
    if (!assets) return;

    for (const asset of assets) {
      const fqn = asset.address;
      if (!fqn) continue;

      // Index by Full Qualified Name
      this.map.byAddress.set(fqn, asset);

      // Index by Simple Name (Last part of FQN)
      const parts = fqn.split('.');
      const simpleName = parts[parts.length - 1];
      if (simpleName) {
        if (!this.map.bySimpleName.has(simpleName)) {
          this.map.bySimpleName.set(simpleName, []);
        }
        this.map.bySimpleName.get(simpleName)!.push(asset);
      }
    }
    this.isBuilt = true;
  }

  /**
   * Get asset by strict FQN.
   * Returns null if not found. No fuzzy matching.
   */
  public getByFqn(fqn: string): any | null {
    return this.map.byAddress.get(fqn) || null;
  }

  /**
   * Get assets by Simple Name.
   * Returns array (could be multiple if class names collide in different packages).
   */
  public getBySimpleName(simpleName: string): any[] {
    return this.map.bySimpleName.get(simpleName) || [];
  }

  /**
   * Attempt to resolve a potentially ambiguous class name.
   * Priority: Exact Match -> Unique Simple Name Match.
   */
  public resolve(className: string): any | null {
    if (!className) return null;

    // 1. Try Exact FQN Match
    const exact = this.map.byAddress.get(className);
    if (exact) return exact;

    // 2. Try Simple Name Match
    const candidates = this.map.bySimpleName.get(className);
    if (candidates) {
      if (candidates.length === 1) return candidates[0];
      
      // If multiple, try to find the best match based on context (e.g. if className contains package parts)
      // For now, return the first one but log a warning
      console.warn(`Multiple classes found for simple name "${className}", returning first match.`);
      return candidates[0];
    }

    return null;
  }

  /**
   * Find the specific method source code within an asset.
   */
  public findMethodSource(className: string, methodName: string): string | null {
    const asset = this.resolve(className);
    if (!asset) return null;

    const methods = asset.methods_full || asset.methods || [];
    
    // Strict method match
    const method = methods.find((m: any) => 
      m.name === methodName || 
      m.address.endsWith(`#${methodName}`) ||
      m.address.endsWith(`#${methodName}(`)
    );

    if (method) {
      return method.source_code || method.body_code || null;
    }

    return null;
  }
}
