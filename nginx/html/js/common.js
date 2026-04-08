/**
 * Java Source Analyzer - Common Utilities
 * 通用工具函数库
 */

// ==================== 配置常量 ====================

const CONFIG = {
    // 颜色映射表
    COLOR_MAP: {
        'INTERFACE': '#4299e1',
        'ABSTRACT_CLASS': '#9f7aea',
        'CLASS': '#48bb78',
        'ENUM': '#ed8936',
        'UTILITY': '#a0aec0',
        'EXTERNAL': '#4a5568'
    },
    
    // 性能阈值
    LARGE_DATASET_THRESHOLD: 1000,
    
    // API路径（相对于网站根目录）
    DATA_PATH: '/data/',
    PROJECTS_INDEX: '/data/projects.json'
};

// ==================== 数据加载器 ====================

/**
 * 加载项目索引
 * @returns {Promise<Array>} 项目列表
 */
async function loadProjectsIndex() {
    try {
        const res = await fetch(`${CONFIG.PROJECTS_INDEX}?t=${Date.now()}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}: 无法加载项目索引`);
        
        const data = await res.json();
        // 兼容多种数据结构
        return data.frameworks || data.projects || (Array.isArray(data) ? data : []);
    } catch (error) {
        console.error('❌ 加载项目索引失败:', error);
        throw error;
    }
}

/**
 * 加载项目数据
 * @param {string} filename - 数据文件名
 * @returns {Promise<Object>} 项目数据 {nodes, links}
 */
async function loadProjectData(filename) {
    try {
        const res = await fetch(`${CONFIG.DATA_PATH}${filename}?t=${Date.now()}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}: 文件不存在`);
        
        const raw = await res.json();
        
        // 实际数据结构: {framework, version, scan_date, assets, dependencies}
        if (!raw.assets || !raw.dependencies) {
            throw new Error('数据格式错误: 缺少 assets 或 dependencies 字段');
        }
        
        // 转换 assets → nodes
        const nodes = raw.assets.map(asset => {
            const kind = asset.kind || 'CLASS';
            return {
                id: asset.address,
                name: asset.address.split('.').pop(),
                category: kind,
                description: asset.description || '',
                color: CONFIG.COLOR_MAP[kind] || '#c9d1d9',
                methodCount: Array.isArray(asset.methods) ? asset.methods.length : 0,
                fieldCount: asset.fields_matrix ? (Array.isArray(asset.fields_matrix) ? asset.fields_matrix.length : 0) : 0
            };
        });
        
        // 转换 dependencies → links
        const links = raw.dependencies.map(dep => ({
            source: dep.source,
            target: dep.target,
            type: dep.type || 'DEPENDS'
        }));
        
        return {
            framework: raw.framework,
            version: raw.version,
            nodes,
            links
        };
    } catch (error) {
        console.error(`❌ 加载项目数据失败 [${filename}]:`, error);
        throw error;
    }
}

// ==================== DOM 操作工具 ====================

/**
 * 填充项目选择器
 * @param {string} selectorId - select元素ID
 * @param {Array} projects - 项目列表
 * @param {Function} onChange - 变更回调
 */
function populateProjectSelector(selectorId, projects, onChange) {
    const select = document.getElementById(selectorId);
    if (!select) {
        console.warn(`⚠️ 未找到选择器元素: ${selectorId}`);
        return;
    }
    
    select.innerHTML = '';
    
    projects.forEach(project => {
        const option = document.createElement('option');
        option.value = project.file;
        option.textContent = project.name;
        select.appendChild(option);
    });
    
    if (onChange) {
        select.addEventListener('change', (e) => onChange(e.target.value));
    }
}

/**
 * 更新统计信息显示
 * @param {Object} data - 项目数据
 */
function updateStatsDisplay(data) {
    const statsNodes = document.getElementById('statsNodes');
    const statsLinks = document.getElementById('statsLinks');
    const statsZoom = document.getElementById('statsZoom');
    
    if (statsNodes) {
        statsNodes.textContent = data.nodes.length;
    }
    if (statsLinks) {
        statsLinks.textContent = data.links.length;
    }
    if (statsZoom) {
        statsZoom.textContent = '100%';
    }
}

/**
 * 显示/隐藏加载遮罩
 * @param {boolean} show - 是否显示
 * @param {string} text - 提示文本
 */
function toggleLoadingOverlay(show, text = '加载中...') {
    const overlay = document.getElementById('loadingOverlay');
    const loadingText = document.getElementById('loadingText');
    
    if (!overlay) return;
    
    if (show) {
        if (loadingText) loadingText.textContent = text;
        overlay.style.display = 'flex';
        overlay.style.opacity = '1';
    } else {
        overlay.style.opacity = '0';
        setTimeout(() => {
            overlay.style.display = 'none';
        }, 300);
    }
}

/**
 * 显示错误信息
 * @param {string} message - 错误消息
 */
function showError(message) {
    const loadingText = document.getElementById('loadingText');
    if (loadingText) {
        loadingText.textContent = `错误: ${message}`;
        loadingText.style.color = '#fc8181';
    }
    console.error('❌', message);
}

// ==================== 数据处理工具 ====================

/**
 * 判断是否为大数据集
 * @param {Array} nodes - 节点数组
 * @returns {boolean}
 */
function isLargeDataset(nodes) {
    return nodes.length > CONFIG.LARGE_DATASET_THRESHOLD;
}

/**
 * 精简大数据集（用于3D视图等性能敏感场景）
 * @param {Object} data - 原始数据
 * @param {number} maxNodes - 最大节点数
 * @returns {Object} 精简后的数据
 */
function trimLargeDataset(data, maxNodes = 800) {
    if (!isLargeDataset(data.nodes)) return data;
    
    const trimmedNodes = data.nodes.slice(0, maxNodes);
    const nodeIds = new Set(trimmedNodes.map(n => n.id));
    
    const trimmedLinks = data.links.filter(link => 
        nodeIds.has(link.source) && nodeIds.has(link.target)
    );
    
    return {
        ...data,
        nodes: trimmedNodes,
        links: trimmedLinks,
        trimmed: true,
        originalCount: data.nodes.length
    };
}

/**
 * 清理描述文本（移除Javadoc标记）
 * @param {string} description - 原始描述
 * @param {number} maxLength - 最大长度
 * @returns {string} 清理后的描述
 */
function cleanDescription(description, maxLength = 30) {
    if (!description) return '';
    
    let cleaned = description
        .replace(/@\w+/g, '')           // 移除 @tag
        .replace(/\{.*?\}/g, '')        // 移除 {...}
        .replace(/<.*?>/g, '')          // 移除 <...>
        .replace(/\s+/g, ' ')           // 合并空白
        .trim();
    
    if (cleaned.length > maxLength) {
        cleaned = cleaned.substring(0, maxLength) + '...';
    }
    
    return cleaned;
}

/**
 * 获取节点颜色
 * @param {Object} node - 节点对象
 * @returns {string} 颜色值
 */
function getNodeColor(node) {
    return node.color || CONFIG.COLOR_MAP[node.category] || '#c9d1d9';
}

// ==================== 初始化工具 ====================

/**
 * 初始化图表容器
 * @param {string} containerId - 容器元素ID
 * @param {string} theme - ECharts主题
 * @returns {Object} ECharts实例
 */
function initChart(containerId, theme = 'dark') {
    const container = document.getElementById(containerId);
    if (!container) {
        throw new Error(`图表容器不存在: ${containerId}`);
    }
    
    const chart = echarts.init(container, theme);
    
    // 自动响应窗口大小变化
    window.addEventListener('resize', () => {
        chart.resize();
    });
    
    return chart;
}

// ==================== 日志工具 ====================

/**
 * 格式化日志输出
 */
const Logger = {
    info: (msg, ...args) => console.log(`ℹ️  ${msg}`, ...args),
    success: (msg, ...args) => console.log(`✅ ${msg}`, ...args),
    warning: (msg, ...args) => console.warn(`⚠️  ${msg}`, ...args),
    error: (msg, ...args) => console.error(`❌ ${msg}`, ...args),
    debug: (msg, ...args) => console.debug(`🔍 ${msg}`, ...args)
};

// ==================== 导出（供模块化使用）====================

if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        CONFIG,
        loadProjectsIndex,
        loadProjectData,
        populateProjectSelector,
        updateStatsDisplay,
        toggleLoadingOverlay,
        showError,
        isLargeDataset,
        trimLargeDataset,
        cleanDescription,
        getNodeColor,
        initChart,
        Logger
    };
}
