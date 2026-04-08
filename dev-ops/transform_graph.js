const fs = require('fs');
const path = require('path');

// 🚩 配置路径
const OUTPUT_DIR = '/Users/mingxilv/WebDevelopment/gitcode/dev-proj/s-pay-mall/s-pay-mall-ddd/source-proj/glossary-redisson/dev-ops/output';
const HTML_DIR = '/Users/mingxilv/WebDevelopment/gitcode/dev-proj/s-pay-mall/s-pay-mall-ddd/source-proj/glossary-redisson/nginx/html';
const DATA_DIR = path.join(HTML_DIR, 'data');

// 确保目录存在
fs.mkdirSync(DATA_DIR, { recursive: true });

// 🎨 颜色定义
const COLOR_MAP = {
    'INTERFACE': '#4299e1', 'ABSTRACT_CLASS': '#9f7aea', 'CLASS': '#48bb78',
    'ENUM': '#ed8936', 'UTILITY': '#a0aec0', 'EXTERNAL': '#4a5568'
};

function transformData() {
    console.log('🔍 开始扫描输出目录...');
    const files = fs.readdirSync(OUTPUT_DIR).filter(f => f.endsWith('.json') && !f.includes('_other_')); // 暂时忽略 other 模块
    
    if (files.length === 0) {
        console.log('⚠️ 未找到任何 JSON 文件');
        return;
    }

    // 读取已有索引
    const indexFile = path.join(DATA_DIR, 'projects.json');
    let index = fs.existsSync(indexFile) ? JSON.parse(fs.readFileSync(indexFile, 'utf8')) : [];
    const existingMap = new Map(index.map(p => [p.file, p]));

    let processed = 0;

    files.forEach(file => {
        const sourcePath = path.join(OUTPUT_DIR, file);
        const baseName = file.replace(/_full_.*\.json$/, '').replace(/[^a-zA-Z0-9]/g, '_').toLowerCase();
        const outName = `graph_data_${baseName}.json`;
        const outPath = path.join(DATA_DIR, outName);

        try {
            console.log(`📂 处理: ${file}`);
            const rawData = JSON.parse(fs.readFileSync(sourcePath, 'utf8'));
            
            // 优化显示名称逻辑
            let displayName = `${rawData.framework || 'Unknown'} ${rawData.version || ''}`;
            if (rawData.module) {
                displayName += ` - ${rawData.module}`;
            }
            
            const nodes = [];
            const links = [];
            const nodeSet = new Set();

            // 提取节点和连线
            function extractAssets(assets, deps) {
                if (!Array.isArray(assets)) return;
                
                // 0. 先计算每个节点的依赖计数
                const depCountMap = new Map();
                if (Array.isArray(deps)) {
                    deps.forEach(dep => {
                        depCountMap.set(dep.source, (depCountMap.get(dep.source) || 0) + 1);
                    });
                }
                
                // 1. 先注册所有节点
                assets.forEach(asset => {
                    const nodeId = asset.address;
                    if (!nodeSet.has(nodeId)) {
                        nodeSet.add(nodeId);
                        nodes.push({
                            id: nodeId,
                            name: nodeId.split('.').pop(),
                            category: asset.kind || 'CLASS',
                            color: COLOR_MAP[asset.kind] || '#c9d1d9',
                            description: asset.description || '',
                            modifiers: asset.modifiers || [],
                            methodCount: (asset.methods || []).length,
                            fieldCount: (asset.fields_matrix || []).length,
                            depCount: depCountMap.get(nodeId) || 0
                        });
                    }
                });

                // 2. 处理显式定义的依赖 (DEPENDS_ON, EXTENDS, IMPLEMENTS, CALLS)
                if (Array.isArray(deps)) {
                    deps.forEach(dep => {
                        // 确保目标节点也存在（如果是外部依赖则创建一个幽灵节点）
                        if (!nodeSet.has(dep.target)) {
                            nodeSet.add(dep.target);
                            nodes.push({
                                id: dep.target,
                                name: dep.target.split('.').pop().split('#').pop(),
                                category: dep.type === 'CALLS' ? 'METHOD' : 'EXTERNAL',
                                color: dep.type === 'CALLS' ? '#f6ad55' : COLOR_MAP['EXTERNAL'],
                                symbolSize: dep.type === 'CALLS' ? 15 : 10,
                                modifiers: [],
                                methodCount: 0,
                                fieldCount: 0,
                                depCount: 0
                            });
                        }
                        
                        // 根据依赖类型设置不同的连线样式
                        let lineStyle = { curveness: 0.1 };
                        if (dep.type === 'EXTENDS') {
                            lineStyle.width = 2;
                            lineStyle.opacity = 0.6;
                        } else if (dep.type === 'IMPLEMENTS') {
                            lineStyle.width = 1.5;
                            lineStyle.opacity = 0.6;
                        } else if (dep.type === 'CALLS') {
                            lineStyle.width = 0.8;
                            lineStyle.opacity = 0.3; // 调用链较淡，避免视觉混乱
                        } else {
                            lineStyle.width = 1;
                            lineStyle.opacity = 0.4;
                        }
                        
                        links.push({ 
                            source: dep.source, 
                            target: dep.target, 
                            type: dep.type || 'DEPENDS_ON',
                            lineStyle: lineStyle
                        });
                    });
                }
            }

            extractAssets(rawData.assets, rawData.dependencies);

            // 保存转换后的数据
            const graphData = { nodes, links };
            fs.writeFileSync(outPath, JSON.stringify(graphData));
            
            // 更新索引
            existingMap.set(outName, {
                name: displayName,
                file: outName,
                date: rawData.scan_date || new Date().toISOString()
            });
            
            processed++;
            console.log(`✅ 完成: ${outName} (节点: ${nodes.length}, 连线: ${links.length})`);
        } catch (e) {
            console.error(`❌ 失败: ${file} - ${e.message}`);
        }
    });

    // 保存索引
    index = Array.from(existingMap.values()).sort((a, b) => (b.date || '').localeCompare(a.date || ''));
    fs.writeFileSync(indexFile, JSON.stringify(index, null, 2));
    console.log(`\n📊 总结: 成功处理 ${processed} 个项目，数据已同步至: ${DATA_DIR}`);
}

transformData();
