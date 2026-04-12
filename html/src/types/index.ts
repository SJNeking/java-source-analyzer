/**
 * Java Source Analyzer - Type Definitions
 * Based on the actual JSON output structure from the Java backend
 */

// ==================== Core Analysis Result Types ====================

/**
 * Complete analysis result (full JSON output)
 */
export interface AnalysisResult {
  framework: string;
  version: string;
  scan_date: string;
  project_type: ProjectTypeDetection;
  comment_coverage: CommentCoverage;
  quality_gate?: QualityGate;
  quality_summary: QualitySummary;
  quality_issues?: QualityIssue[];
  technical_debt?: TechnicalDebt;
  cross_file_relations: CrossFileRelations;
  assets: Asset[];
  dependencies: Dependency[];
  project_assets?: Record<string, unknown>;
  code_metrics?: CodeMetrics;
  dependency_graph?: DependencyGraph;
  core_analysis?: CoreAnalysis;
}

/**
 * Project type detection result
 */
export interface ProjectTypeDetection {
  primary_type: string;
  all_types: string[];
  evidence: string[];
}

/**
 * Comment coverage statistics
 */
export interface CommentCoverage {
  class_comment_coverage_pct: number;
  method_comment_coverage_pct: number;
  field_comment_coverage_pct: number;
  total_classes?: number;
  classes_with_comments?: number;
}

/**
 * Quality gate result
 */
export interface QualityGate {
  passed: boolean;
  conditions?: Array<{
    metric: string;
    threshold: number;
    actual: number;
    status: 'PASS' | 'FAIL';
  }>;
}

/**
 * Quality summary by severity and category
 */
export interface QualitySummary {
  total_issues?: number;
  by_severity: {
    CRITICAL: number;
    MAJOR: number;
    MINOR: number;
    INFO?: number;
  };
  by_category: {
    BUG: number;
    CODE_SMELL: number;
    SECURITY: number;
    [key: string]: number;
  };
}

/**
 * Technical debt estimation
 */
export interface TechnicalDebt {
  technical_debt_ratio_pct?: number;
  remediation_effort_minutes?: number;
  sqale_rating?: string;
}

/**
 * Cross-file relations
 */
export interface CrossFileRelations {
  total_relations: number;
  relations_by_type: {
    [relationType: string]: Relation[];
  };
}

/**
 * Individual relation between assets
 */
export interface Relation {
  source_path: string;
  source_asset: string;
  target_asset: string;
  relation_type: string;
  evidence: string;
  confidence: number;
}

// ==================== Asset Types ====================

/**
 * Main asset entity (class, interface, enum, etc.)
 */
export interface Asset {
  address: string;
  kind: AssetKind;
  description: string;
  comment_details?: CommentDetails;
  source_file: string;
  modifiers: string[];
  import_dependencies?: string[];
  annotation_params?: AnnotationParam[];
  methods_full?: MethodAsset[];
  methods_intent?: MethodIntent[];
  fields_matrix?: FieldAsset[];
  constructor_matrix?: ConstructorAsset[];
  methods?: MethodAsset[]; // Alternative field name
  fields?: FieldAsset[]; // Alternative field name
}

/**
 * Asset kind/type enumeration
 */
export type AssetKind = 
  | 'CLASS'
  | 'INTERFACE'
  | 'ABSTRACT_CLASS'
  | 'ENUM'
  | 'UTILITY'
  | 'EXTERNAL'
  | 'ANNOTATION'
  | 'ISSUE'
  | 'ERROR'
  | 'WARNING';

/**
 * Comment details extracted from Javadoc
 */
export interface CommentDetails {
  summary: string;
  description: string;
  params: ParamComment[];
  return_description: string;
  throws: ThrowsComment[];
  deprecated?: string;
  since?: string;
  author?: string;
  see?: string[];
  semantic_notes?: string[];
  raw_comment?: string;
}

/**
 * Parameter comment
 */
export interface ParamComment {
  name: string;
  description: string;
}

/**
 * Throws clause comment
 */
export interface ThrowsComment {
  exception: string;
  description: string;
}

/**
 * Annotation parameters
 */
export interface AnnotationParam {
  name: string;
  parameters: Array<{
    key: string;
    value: string;
  }>;
}

// ==================== Method Types ====================

/**
 * Full method asset with complete information
 */
export interface MethodAsset {
  address: string;
  name: string;
  description: string;
  comment_details?: CommentDetails;
  modifiers: string[];
  line_start: number;
  line_end: number;
  signature: string;
  source_code: string;
  body_code: string;
  code_summary: string;
  key_statements: Statement[];
  line_count: number;
  tags?: string[];
}

/**
 * Method intent (simplified version)
 */
export interface MethodIntent {
  address: string;
  name: string;
  description: string;
  intent?: string;
  tags?: string[];
}

/**
 * Key statement in method body
 */
export interface Statement {
  type: 'CONDITION' | 'EXTERNAL_CALL' | 'RETURN' | 'LOOP' | 'THROW' | string;
  condition?: string;
  target?: string;
  line: string;
}

// ==================== Field Types ====================

/**
 * Field/property asset
 */
export interface FieldAsset {
  name: string;
  type: string;
  modifiers: string[];
  description?: string;
  annotations?: string[];
  line?: number;
}

/**
 * Constructor asset
 */
export interface ConstructorAsset {
  address: string;
  modifiers: string[];
  line_start: number;
  line_end: number;
  signature: string;
  parameters?: ParamComment[];
}

// ==================== Dependency Types ====================

/**
 * Dependency between assets
 */
export interface Dependency {
  source: string;
  target: string;
  type: string;
  weight?: number;
}

// ==================== Metrics Types ====================

/**
 * Code metrics
 */
export interface CodeMetrics {
  total_lines?: number;
  total_classes?: number;
  total_methods?: number;
  total_fields?: number;
  average_complexity?: number;
  cyclomatic_complexity?: number;
  halstead_metrics?: {
    vocabulary: number;
    length: number;
    volume: number;
    difficulty: number;
    effort: number;
  };
}

/**
 * Dependency graph structure
 */
export interface DependencyGraph {
  nodes: Array<{
    id: string;
    name: string;
    type: string;
  }>;
  edges: Array<{
    source: string;
    target: string;
    type: string;
  }>;
}

/**
 * Core analysis results
 */
export interface CoreAnalysis {
  entry_points?: string[];
  call_chains?: CallChain[];
  taint_flows?: TaintFlow[];
}

/**
 * Call chain trace
 */
export interface CallChain {
  from: string;
  to: string;
  path: string[];
  depth: number;
}

/**
 * Taint flow analysis result
 */
export interface TaintFlow {
  source: string;
  sink: string;
  path: string[];
  vulnerability_type: string;
}

// ==================== Graph Visualization Types ====================

/**
 * Node for force graph visualization
 */
export interface GraphNode {
  id: string;
  name: string;
  fullName?: string; // Fully qualified name
  category: AssetKind | 'EXTERNAL';
  dependencyType?: 'INTERNAL' | 'JDK' | 'THIRD_PARTY'; // Dependency origin
  description: string;
  color: string;
  methodCount: number;
  fieldCount: number;
  symbolSize?: number;
  degree?: number;
  loc?: number; // Lines of code
  complexity?: number; // Cyclomatic complexity
  qualityGrade?: 'A' | 'B' | 'C' | 'D' | 'E'; // Quality grade
  qualityIssues?: {critical: number; major: number; minor: number}; // Issue counts
  itemStyle?: {
    color: string;
    shadowBlur: number;
    shadowColor: string;
    borderColor: string;
    borderWidth: number;
  };
  label?: {
    show: boolean;
    position: string;
    formatter: string;
    [key: string]: unknown;
  };
}

/**
 * Edge/Link for force graph visualization
 */
export interface GraphLink {
  source: string;
  target: string;
  type: string;
  
  // The aggregated count of method calls
  value?: number;

  // The full path trace: e.g. ["MethodA() -> MethodB()", "MethodA() -> MethodC()"]
  calls?: string[];

  lineStyle?: {
    color: string;
    opacity: number;
    width: number;
    curveness: number;
  };
}

/**
 * Graph data structure for visualization
 */
export interface GraphData {
  framework: string;
  version: string;
  nodes: GraphNode[];
  links: GraphLink[];
}

// ==================== Project Index Types ====================

/**
 * Project entry in projects.json index
 */
export interface ProjectIndex {
  frameworks?: ProjectEntry[];
  projects?: ProjectEntry[];
  [key: string]: unknown;
}

/**
 * Individual project entry
 */
export interface ProjectEntry {
  file: string;
  name: string;
  path?: string;
  description?: string;
}

/**
 * Alternative projects.json format (array)
 */
export type ProjectEntryArray = ProjectEntry[];

// ==================== Summary Report Types ====================

/**
 * Summary analysis result (compressed JSON)
 */
export interface SummaryResult {
  framework: string;
  version: string;
  scan_date: string;
  total_classes: number;
  total_methods: number;
  total_fields: number;
  quality_issues: number;
  technical_debt?: TechnicalDebt;
  quality_gate?: QualityGate;
  comment_coverage: CommentCoverage;
  project_type: ProjectTypeDetection;
  modules: string[];
  dependencies_count: number;
}

// ==================== Glossary Types ====================

/**
 * Raw glossary entry
 */
export interface GlossaryEntry {
  term: string;
  kind: AssetKind;
  description: string;
  modifiers: string;
}

/**
 * Glossary array
 */
export type Glossary = GlossaryEntry[];

// ==================== Quality Issue Types ====================

/**
 * Quality issue detected by rule engine
 */
export interface QualityIssue {
  severity: 'CRITICAL' | 'MAJOR' | 'MINOR' | 'INFO';
  category: 'BUG' | 'CODE_SMELL' | 'SECURITY' | string;
  rule_key: string;
  rule_name: string;
  class: string;
  method?: string;
  line: number;
  message: string;
  description?: string;
  remediation_effort_minutes?: number;
}

// ==================== Configuration Types ====================

/**
 * View type enumeration
 */
export type ViewType = 
  | 'graph'
  | 'quality'
  | 'frontend-quality'
  | 'metrics'
  | 'explorer'
  | 'relations'
  | 'assets'
  | 'architecture'
  | 'api'
  | 'method-call'
  | 'call-chain';

/**
 * Architecture layer pattern configuration
 */
export interface ArchLayerPattern {
  key: string;
  patterns: string[]; // Multiple patterns to match
  icon: string;
  label: string;
  order: number; // Display order
}

/**
 * Detected architecture type
 */
export type ArchitectureType = 
  | 'DDD'           // Domain-Driven Design
  | 'MVC'           // Model-View-Controller
  | 'MICROSERVICE'  // Microservices
  | 'LAYERED'       // Traditional layered
  | 'GENERIC';      // Generic structure

/**
 * Application configuration
 */
export interface AppConfig {
  dataPath: string;
  projectsIndexUrl: string;
  largeDatasetThreshold: number;
  colorMap: Record<AssetKind, string>;
}

// ==================== Filter/State Types ====================

/**
 * Node type filter state
 */
export interface NodeTypeFilters {
  INTERFACE: boolean;
  ABSTRACT_CLASS: boolean;
  CLASS: boolean;
  ENUM: boolean;
  UTILITY: boolean;
  EXTERNAL: boolean;
}

/**
 * Application state
 */
export interface AppState {
  currentProject: string | null;
  originalData: GraphData | null;
  filteredData: GraphData | null;
  filters: NodeTypeFilters;
  labelsVisible: boolean;
  currentZoom: number;
  isLoading: boolean;
  error: string | null;
}
