/**
 * Architecture Layer Patterns Configuration
 * 
 * Defines patterns for detecting different architecture types:
 * - DDD (Domain-Driven Design)
 * - MVC (Model-View-Controller)
 * - Microservices
 * - Traditional Layered
 * - Generic structure
 * 
 * This configuration is used by the ArchitectureDetector to automatically
 * identify and categorize project structures.
 */

import type { ArchLayerPattern } from '../types';

/**
 * Architecture layer patterns for automatic detection
 * Ordered by priority (lower order = higher priority)
 */
export const ARCHITECTURE_PATTERNS: ArchLayerPattern[] = [
  // === DDD Architecture ===
  {
    key: 'trigger',
    patterns: ['.trigger.', '.adapter.', '.interface.'],
    icon: '🌐',
    label: '触发层',
    order: 10
  },
  {
    key: 'app',
    patterns: ['.app.', '.application.'],
    icon: '⚙️',
    label: '应用层',
    order: 20
  },
  {
    key: 'domain',
    patterns: ['.domain.'],
    icon: '🏛️',
    label: '领域层',
    order: 30
  },
  {
    key: 'infrastructure',
    patterns: ['.infrastructure.', '.infra.'],
    icon: '🔧',
    label: '基础设施',
    order: 40
  },
  
  // === MVC Architecture ===
  {
    key: 'controller',
    patterns: ['.controller.', '.web.', '.action.'],
    icon: '🌐',
    label: '控制层',
    order: 50
  },
  {
    key: 'service',
    patterns: ['.service.', '.biz.'],
    icon: '⚙️',
    label: '服务层',
    order: 60
  },
  {
    key: 'dao',
    patterns: ['.dao.', '.repository.', '.mapper.'],
    icon: '💾',
    label: '数据访问',
    order: 70
  },
  {
    key: 'model',
    patterns: ['.model.', '.entity.', '.pojo.', '.bean.'],
    icon: '📦',
    label: '模型层',
    order: 80
  },
  
  // === Microservice Patterns ===
  {
    key: 'api',
    patterns: ['.api.', '.client.', '.feign.'],
    icon: '📡',
    label: '接口层',
    order: 90
  },
  {
    key: 'config',
    patterns: ['.config.', '.configuration.'],
    icon: '⚙️',
    label: '配置层',
    order: 100
  },
  
  // === Common Layers ===
  {
    key: 'dto',
    patterns: ['.dto.', '.vo.', '.do.'],
    icon: '📦',
    label: 'DTO层',
    order: 110
  },
  {
    key: 'common',
    patterns: ['.common.', '.shared.', '.core.'],
    icon: '📦',
    label: '公共层',
    order: 120
  },
  {
    key: 'util',
    patterns: ['.util.', '.helper.', '.tools.'],
    icon: '🔨',
    label: '工具层',
    order: 130
  }
];

/**
 * Architecture detection rules
 * Determines architecture type based on detected layers
 */
export const ARCHITECTURE_DETECTION_RULES = {
  DDD: {
    requiredLayers: ['domain', 'infrastructure'],
    optionalLayers: ['trigger', 'app']
  },
  MVC: {
    requiredLayers: ['controller', 'service', 'dao'],
    optionalLayers: ['model']
  },
  MICROSERVICE: {
    requiredLayers: [],
    optionalLayers: ['api', 'client', 'config']
  },
  LAYERED: {
    requiredLayers: [],
    optionalLayers: ['web', 'service', 'dao']
  },
  GENERIC: {
    requiredLayers: [],
    optionalLayers: []
  }
};

/**
 * View metadata configuration
 * Defines all available views in the application
 */
export const VIEW_METADATA: Record<string, {
  id: string;
  label: string;
  icon: string;
  description: string;
}> = {
  graph: {
    id: 'graph',
    label: '全局地图',
    icon: '🗺️',
    description: '组件依赖关系图'
  },
  explorer: {
    id: 'explorer',
    label: '源码浏览器',
    icon: '🔍',
    description: '代码结构浏览'
  },
  quality: {
    id: 'quality',
    label: '质量分析',
    icon: '📊',
    description: '代码质量概览'
  },
  'frontend-quality': {
    id: 'frontend-quality',
    label: '前端质量',
    icon: '🎨',
    description: '前端代码质量'
  },
  metrics: {
    id: 'metrics',
    label: '度量指标',
    icon: '📈',
    description: '代码度量统计'
  },
  relations: {
    id: 'relations',
    label: '跨文件关系',
    icon: '🔗',
    description: '文件间依赖关系'
  },
  assets: {
    id: 'assets',
    label: '项目资产',
    icon: '📁',
    description: '配置文件和资源'
  },
  architecture: {
    id: 'architecture',
    label: '架构分层',
    icon: '🏗️',
    description: '架构层次分析'
  },
  api: {
    id: 'api',
    label: 'API总览',
    icon: '🔌',
    description: 'API端点列表'
  }
};
