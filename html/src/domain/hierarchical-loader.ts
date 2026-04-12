/**
 * Hierarchical Data Loader
 * 
 * Implements progressive loading strategy to prevent graph explosion:
 * Level 1: Module level (only module nodes)
 * Level 2: Package level (expand modules to see packages)
 * Level 3: Component level (expand packages to see classes)
 * Level 4: Method level (select component to see methods)
 */

import type { AnalysisResult, Asset, GraphData, GraphNode, GraphLink, AssetKind } from '../types';

export interface HierarchyLevel {
  level: 'module' | 'package' | 'component' | 'method';
  nodes: GraphNode[];
  links: GraphLink[];
  metadata: {
    totalModules?: number;
    totalPackages?: number;
    totalComponents?: number;
    totalMethods?: number;
  };
}

export class HierarchicalLoader {
  /**
   * Load data at specified hierarchy level
   */
  static loadAtLevel(data: AnalysisResult, level: HierarchyLevel['level']): HierarchyLevel {
    const assets = data.assets || [];

    switch (level) {
      case 'module':
        return this.loadModuleLevel(assets);
      case 'package':
        return this.loadPackageLevel(assets);
      case 'component':
        return this.loadComponentLevel(assets);
      case 'method':
        return this.loadMethodLevel(assets);
      default:
        return this.loadComponentLevel(assets);
    }
  }

  /**
   * Level 1: Module level - aggregate by top-level package
   */
  private static loadModuleLevel(assets: Asset[]): HierarchyLevel {
    const moduleMap = new Map<string, Asset[]>();

    // Group by top-level package
    assets.forEach(asset => {
      const parts = asset.address.split('.');
      const moduleName = parts.length > 2 ? parts.slice(0, 2).join('.') : parts[0];
      
      if (!moduleMap.has(moduleName)) {
        moduleMap.set(moduleName, []);
      }
      moduleMap.get(moduleName)!.push(asset);
    });

    // Create module nodes
    const nodes: GraphNode[] = Array.from(moduleMap.entries()).map(([moduleName, moduleAssets], index) => {
      const totalMethods = moduleAssets.reduce((sum, a) => sum + (a.methods_full?.length || a.methods?.length || 0), 0);
      const totalFields = moduleAssets.reduce((sum, a) => sum + (a.fields_matrix?.length || a.fields?.length || 0), 0);

      return {
        id: `module:${moduleName}`,
        name: moduleName,
        category: 'MODULE' as AssetKind,
        description: `Module ${moduleName}`,
        color: this.getModuleColor(index),
        methodCount: totalMethods,
        fieldCount: totalFields,
        symbolSize: 30 + Math.min(moduleAssets.length * 2, 50),
        itemStyle: {
          color: this.getModuleColor(index),
          shadowBlur: 10,
          shadowColor: this.getModuleColor(index),
          borderColor: 'rgba(255,255,255,0.3)',
          borderWidth: 2,
        },
      };
    });

    // Create inter-module links
    const links: GraphLink[] = [];
    const moduleNames = Array.from(moduleMap.keys());
    
    moduleNames.forEach((sourceModule, i) => {
      moduleNames.forEach((targetModule, j) => {
        if (i === j) return;

        // Check if any class in source depends on class in target
        const sourceAssets = moduleMap.get(sourceModule)!;
        const targetAssets = moduleMap.get(targetModule)!;
        const targetAddresses = new Set(targetAssets.map(a => a.address));

        const hasDependency = sourceAssets.some(asset => 
          asset.import_dependencies?.some(dep => targetAddresses.has(dep))
        );

        if (hasDependency) {
          links.push({
            source: `module:${sourceModule}`,
            target: `module:${targetModule}`,
            type: 'DEPENDS_ON',
            value: 1,
          });
        }
      });
    });

    return {
      level: 'module',
      nodes,
      links,
      metadata: {
        totalModules: moduleMap.size,
      },
    };
  }

  /**
   * Level 2: Package level - show second-level packages
   */
  private static loadPackageLevel(assets: Asset[]): HierarchyLevel {
    const packageMap = new Map<string, Asset[]>();

    // Group by second-level package
    assets.forEach(asset => {
      const parts = asset.address.split('.');
      const packageName = parts.length > 3 ? parts.slice(0, 3).join('.') : parts.slice(0, 2).join('.');
      
      if (!packageMap.has(packageName)) {
        packageMap.set(packageName, []);
      }
      packageMap.get(packageName)!.push(asset);
    });

    // Create package nodes
    const nodes: GraphNode[] = Array.from(packageMap.entries()).map(([packageName, packageAssets], index) => {
      const totalMethods = packageAssets.reduce((sum, a) => sum + (a.methods_full?.length || a.methods?.length || 0), 0);
      const totalFields = packageAssets.reduce((sum, a) => sum + (a.fields_matrix?.length || a.fields?.length || 0), 0);

      return {
        id: `package:${packageName}`,
        name: packageName.split('.').pop() || packageName,
        fullName: packageName,
        category: 'PACKAGE' as AssetKind,
        description: `Package ${packageName}`,
        color: this.getPackageColor(index),
        methodCount: totalMethods,
        fieldCount: totalFields,
        symbolSize: 25 + Math.min(packageAssets.length, 40),
        itemStyle: {
          color: this.getPackageColor(index),
          shadowBlur: 8,
          shadowColor: this.getPackageColor(index),
          borderColor: 'rgba(255,255,255,0.3)',
          borderWidth: 2,
        },
      };
    });

    // Create inter-package links
    const links: GraphLink[] = [];
    const packageNames = Array.from(packageMap.keys());
    
    packageNames.forEach((sourcePkg, i) => {
      packageNames.forEach((targetPkg, j) => {
        if (i === j) return;

        const sourceAssets = packageMap.get(sourcePkg)!;
        const targetAssets = packageMap.get(targetPkg)!;
        const targetAddresses = new Set(targetAssets.map(a => a.address));

        const depCount = sourceAssets.filter(asset => 
          asset.import_dependencies?.some(dep => targetAddresses.has(dep))
        ).length;

        if (depCount > 0) {
          links.push({
            source: `package:${sourcePkg}`,
            target: `package:${targetPkg}`,
            type: 'DEPENDS_ON',
            value: depCount,
          });
        }
      });
    });

    return {
      level: 'package',
      nodes,
      links,
      metadata: {
        totalPackages: packageMap.size,
      },
    };
  }

  /**
   * Level 3: Component level - show individual classes
   */
  private static loadComponentLevel(assets: Asset[]): HierarchyLevel {
    const nodes: GraphNode[] = assets.map((asset, index) => ({
      id: asset.address,
      name: asset.address.split('.').pop() || asset.address,
      fullName: asset.address,
      category: asset.kind,
      description: asset.description || '',
      color: this.getComponentColor(asset.kind),
      methodCount: asset.methods_full?.length || asset.methods?.length || 0,
      fieldCount: asset.fields_matrix?.length || asset.fields?.length || 0,
      symbolSize: 20,
      itemStyle: {
        color: this.getComponentColor(asset.kind),
        shadowBlur: 10,
        shadowColor: this.getComponentColor(asset.kind),
        borderColor: 'rgba(255,255,255,0.3)',
        borderWidth: 2,
      },
    }));

    // Create dependency links
    const links: GraphLink[] = [];
    const addressSet = new Set(assets.map(a => a.address));

    assets.forEach(asset => {
      asset.import_dependencies?.forEach(dep => {
        if (addressSet.has(dep)) {
          links.push({
            source: asset.address,
            target: dep,
            type: 'IMPORT',
            value: 1,
          });
        }
      });
    });

    return {
      level: 'component',
      nodes,
      links,
      metadata: {
        totalComponents: assets.length,
      },
    };
  }

  /**
   * Level 4: Method level - show methods within a specific class
   */
  private static loadMethodLevel(assets: Asset[]): HierarchyLevel {
    // This level requires selecting a specific class first
    // For now, return empty - should be called with context
    return {
      level: 'method',
      nodes: [],
      links: [],
      metadata: {
        totalMethods: 0,
      },
    };
  }

  /**
   * Get methods for a specific class
   */
  static getMethodsForClass(data: AnalysisResult, classAddress: string): {
    nodes: GraphNode[];
    links: GraphLink[];
  } {
    const asset = data.assets?.find(a => a.address === classAddress);
    if (!asset) return { nodes: [], links: [] };

    const methods = asset.methods_full || asset.methods || [];
    
    const nodes: GraphNode[] = methods.map((method, index) => ({
      id: method.address,
      name: method.name,
      fullName: method.address,
      category: 'METHOD' as AssetKind,
      description: method.description || '',
      color: '#10b981',
      methodCount: 0,
      fieldCount: 0,
      symbolSize: 15,
      itemStyle: {
        color: '#10b981',
        shadowBlur: 5,
        shadowColor: '#10b981',
        borderColor: 'rgba(255,255,255,0.3)',
        borderWidth: 2,
      },
    }));

    // Extract method-to-method calls
    const links: GraphLink[] = [];
    const methodAddresses = new Set(methods.map(m => m.address));

    methods.forEach(method => {
      const keyStmts = (method as any).key_statements || [];
      const extCalls = keyStmts.filter((s: any) => s.type === 'EXTERNAL_CALL');

      extCalls.forEach((call: any) => {
        const targetAddr = call.target_method || call.target || '';
        if (methodAddresses.has(targetAddr)) {
          links.push({
            source: method.address,
            target: targetAddr,
            type: 'CALLS',
            value: 1,
          });
        }
      });
    });

    return { nodes, links };
  }

  /**
   * Color helpers
   */
  private static getModuleColor(index: number): string {
    const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];
    return colors[index % colors.length];
  }

  private static getPackageColor(index: number): string {
    const colors = ['#06b6d4', '#84cc16', '#f97316', '#dc2626', '#7c3aed', '#db2777'];
    return colors[index % colors.length];
  }

  private static getComponentColor(kind: string): string {
    switch (kind) {
      case 'INTERFACE': return '#60a5fa';
      case 'ABSTRACT_CLASS': return '#a78bfa';
      case 'CLASS': return '#4ade80';
      case 'ENUM': return '#fb923c';
      default: return '#94a3b8';
    }
  }
}
