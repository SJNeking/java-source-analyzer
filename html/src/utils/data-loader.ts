/**
 * Java Source Analyzer - Data Loader
 * 
 * CORE LOGIC:
 * 1. Extracts dependencies from 'import_dependencies' (Static Import Analysis).
 * 2. NO FILTERING: JDK and Third-party libs are treated as valid External Dependencies.
 * 3. Dynamic Node Creation: Imported classes not in project assets are created as External nodes.
 */

import type {
  AnalysisResult,
  GraphData,
  GraphNode,
  GraphLink,
  ProjectEntry,
  ProjectIndex,
  ProjectEntryArray,
  AssetKind, Asset,
  QualityIssue
} from '../types';
import { CONFIG } from '../config';
import { Logger } from './logger';

export async function loadProjectsIndex(): Promise<ProjectEntry[]> {
  try {
    const response = await fetch(`${CONFIG.projectsIndexUrl}?t=${Date.now()}`);
    if (!response.ok) throw new Error(`HTTP ${response.status}: Unable to load project index`);
    const data: ProjectIndex | ProjectEntryArray = await response.json();
    if (Array.isArray(data)) return data;
    if ('frameworks' in data && Array.isArray(data.frameworks)) return data.frameworks;
    if ('projects' in data && Array.isArray(data.projects)) return data.projects;
    return [];
  } catch (error) {
    console.error('❌ Failed to load project index:', error);
    throw error;
  }
}

export async function loadAnalysisResult(filename: string): Promise<AnalysisResult> {
  const response = await fetch(`${CONFIG.dataPath}${filename}?t=${Date.now()}`);
  if (!response.ok) throw new Error(`HTTP ${response.status}: File not found - ${filename}`);
  const data = await response.json();

  // Schema version compatibility check
  const schemaVersion = (data as any).schema_version;
  if (schemaVersion) {
    const [major] = schemaVersion.split('.').map(Number);
    if (major > 1) {
      Logger.warning(`Schema version ${schemaVersion} may be incompatible with this frontend. Expected 1.x.x`);
    }
  } else {
    Logger.warning('No schema_version found in analysis result. Data format may be from an older analyzer.');
  }

  return data as AnalysisResult;
}

export function extractShortName(address: string): string {
  if (!address) return '';
  return address.split('.').pop() || address;
}

function categorizeAddress(address: string, projectPrefix: string): 'INTERNAL' | 'JDK' | 'THIRD_PARTY' {
  if (address.startsWith(projectPrefix)) return 'INTERNAL';
  if (/^(java\.|javax\.|sun\.|org\.xml\.|org\.w3c\.|jdk\.|com\.sun\.)/.test(address)) return 'JDK';
  return 'THIRD_PARTY';
}

function normalizeAssetKind(kind: string): AssetKind {
  const u = kind?.toUpperCase();
  if (u === 'INTERFACE') return 'INTERFACE';
  if (u === 'ABSTRACT_CLASS') return 'ABSTRACT_CLASS';
  if (u === 'CLASS') return 'CLASS';
  if (u === 'ENUM') return 'ENUM';
  return 'CLASS';
}

function calculateMethodCount(a: Asset) { return Array.isArray(a.methods_full) ? a.methods_full.length : (Array.isArray(a.methods) ? a.methods.length : 0); }
function calculateFieldCount(a: Asset) { return Array.isArray(a.fields_matrix) ? a.fields_matrix.length : (Array.isArray(a.fields) ? a.fields.length : 0); }

/**
 * Calculate quality grade based on issue counts
 */
function calculateQualityGrade(critical: number, major: number, minor: number): string {
  if (critical > 0) return 'D';
  if (major > 2) return 'C';
  if (major > 0) return 'B';
  if (minor > 5) return 'B';
  return 'A';
}

/**
 * Transform analysis result into graph data.
 * - Nodes: Internal Assets + Discovered External Dependencies.
 * - Links: Import relationships.
 */
export function transformToGraph(result: AnalysisResult): GraphData {
  Logger.time('transformToGraph');

  const assets = result.assets || [];
  if (assets.length === 0) return { framework: result.framework, version: result.version, nodes: [], links: [] };

  // 1. Detect Project's Own Package Prefix
  const projectPrefix = assets[0].address.split('.').slice(0, 2).join('.');

  // 2. Build Quality Index (fast lookup)
  const qualityByClass = new Map<string, {critical: number; major: number; minor: number}>();
  const qualityIssues: QualityIssue[] = (result as any).quality_issues || [];
  qualityIssues.forEach((issue: QualityIssue) => {
    const cls = issue.class || '';
    if (!qualityByClass.has(cls)) {
      qualityByClass.set(cls, {critical: 0, major: 0, minor: 0});
    }
    const stats = qualityByClass.get(cls)!;
    if (issue.severity === 'CRITICAL') stats.critical++;
    else if (issue.severity === 'MAJOR') stats.major++;
    else if (issue.severity === 'MINOR') stats.minor++;
  });

  // 3. Build Internal Nodes with Quality Info
  const nodes: GraphNode[] = assets.map(asset => {
    const kind = normalizeAssetKind(asset.kind);
    const classAddr = asset.address;
    const quality = qualityByClass.get(classAddr) || {critical: 0, major: 0, minor: 0};
    
    // Calculate quality border color based on severity
    let borderColor = 'rgba(255,255,255,0.2)'; // Default
    let borderWidth = 1.5;
    let qualityGrade: 'A' | 'B' | 'C' | 'D' | 'E' = 'A';
    
    if (quality.critical > 0) {
      borderColor = '#fb7185'; // Red for Critical
      borderWidth = 2.5;
      qualityGrade = 'D';
    } else if (quality.major > 0) {
      borderColor = '#fbbf24'; // Orange for Major
      borderWidth = 2;
      qualityGrade = 'C';
    } else if (quality.minor > 0) {
      borderColor = '#38bdf8'; // Blue for Minor
      borderWidth = 1.5;
      qualityGrade = 'B';
    }
    
    return {
      id: asset.address,
      name: extractShortName(asset.address),
      fullName: asset.address,
      category: kind,
      dependencyType: 'INTERNAL',
      description: asset.description || '',
      color: CONFIG.colorMap[kind] || '#94a3b8',
      methodCount: calculateMethodCount(asset),
      fieldCount: calculateFieldCount(asset),
      loc: (asset as any).loc || 0,
      complexity: (asset as any).complexity || 0,
      qualityGrade,
      qualityIssues: quality,
      itemStyle: {
        color: CONFIG.colorMap[kind] || '#94a3b8',
        shadowBlur: 10,
        shadowColor: CONFIG.colorMap[kind] || '#94a3b8',
        borderColor,
        borderWidth
      }
    };
  });

  const internalNodeIds = new Set(nodes.map(n => n.id));
  
  // 3. Discover External Dependencies (JDK + Third Party)
  // Scan all import_dependencies. If target is not in assets, create an external node.
  const externalNodesMap = new Map<string, GraphNode>();
  const rawLinks: GraphLink[] = [];

  assets.forEach(asset => {
    const sourceClass = asset.address;
    const imports = asset.import_dependencies || [];
    
    imports.forEach((targetClass: string) => {
      if (!targetClass || targetClass === sourceClass) return;
      
      // Skip imports from the same package (noise reduction)
      // Check if target is in same package as source
      const sourcePkg = sourceClass.substring(0, sourceClass.lastIndexOf('.'));
      if (targetClass.startsWith(sourcePkg + '.')) return;

      // Create External Node if missing
      if (!internalNodeIds.has(targetClass) && !externalNodesMap.has(targetClass)) {
        const cat = categorizeAddress(targetClass, projectPrefix);
        externalNodesMap.set(targetClass, {
          id: targetClass,
          name: extractShortName(targetClass),
          fullName: targetClass,
          category: 'EXTERNAL',
          dependencyType: cat,
          description: cat === 'JDK' ? 'JDK Standard Library' : 'External Library',
          color: cat === 'JDK' ? '#94a3b8' : '#f59e0b', // Grey for JDK, Orange for 3rd Party
          methodCount: 0,
          fieldCount: 0,
          symbolSize: 12
        });
      }

      // Add Link
      rawLinks.push({ source: sourceClass, target: targetClass, type: 'IMPORTS', value: 1 });
    });
  });

  // 4. Aggregate Links
  const linkMap = new Map<string, GraphLink>();
  rawLinks.forEach(l => {
    const key = `${l.source}|${l.target}`;
    if (!linkMap.has(key)) linkMap.set(key, { ...l });
    else linkMap.get(key)!.value!++;
  });

  // Only keep links where both nodes exist (Internal or created External)
  const allNodeIds = new Set([...internalNodeIds, ...externalNodesMap.keys()]);
  const finalLinks = Array.from(linkMap.values()).filter(l => allNodeIds.has(l.source) && allNodeIds.has(l.target));
  const finalNodes = [...nodes, ...Array.from(externalNodesMap.values())];

  Logger.timeEnd('transformToGraph');
  Logger.success(`Graph: ${finalNodes.length} nodes, ${finalLinks.length} links`);

  return { framework: result.framework, version: result.version, nodes: finalNodes, links: finalLinks };
}

export async function loadProjectData(filename: string): Promise<GraphData> {
  const result = await loadAnalysisResult(filename);
  return transformToGraph(result);
}
