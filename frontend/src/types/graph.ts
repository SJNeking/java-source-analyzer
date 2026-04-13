/**
 * Graph Data Types for Force Directed Graph
 */

export interface GraphNode {
  id: string;
  name: string;
  type: string;
  value?: number;
  category?: number;
  symbolSize?: number | number[];
  itemStyle?: {
    color?: string;
    borderColor?: string;
    borderWidth?: number;
  };
  label?: {
    show?: boolean;
    position?: string;
    fontSize?: number;
  };
  draggable?: boolean;
  x?: number;
  y?: number;
  
  // Extended properties
  address?: string;
  fqn?: string;
  description?: string;
  metrics?: {
    complexity?: number;
    loc?: number;
    coupling?: number;
    cohesion?: number;
  };
}

export interface GraphLink {
  source: string;
  target: string;
  value?: number;
  lineStyle?: {
    width?: number;
    curveness?: number;
    color?: string;
    type?: 'solid' | 'dashed' | 'dotted';
  };
  
  // Extended properties
  relationType?: string;
  isArchViolation?: boolean;
}

export interface GraphData {
  nodes: GraphNode[];
  links: GraphLink[];
  categories?: Array<{ name: string }>;
}
