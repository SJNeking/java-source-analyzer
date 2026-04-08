/**
 * Force Graph View - 力导向图视图控制器
 */

class ForceGraphView {
    constructor(containerId = 'main') {
        this.chart = initChart(containerId);
        this.currentData = null;
    }
    
    /**
     * 渲染力导向图
     * @param {Object} data - {nodes, links}
     */
    render(data) {
        this.currentData = data;
        const large = isLargeDataset(data.nodes);
        
        // 计算每个节点的连接度
        const degreeMap = this.calculateDegree(data);
        
        const option = {
            backgroundColor: 'transparent',
            tooltip: {
                trigger: 'item',
                backgroundColor: 'rgba(26, 32, 44, 0.95)',
                borderColor: 'rgba(66, 153, 225, 0.3)',
                borderWidth: 1,
                textStyle: { color: '#e2e8f0' },
                formatter: (params) => this.tooltipFormatter(params),
                extraCssText: 'box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4); border-radius: 8px; padding: 12px;'
            },
            series: [{
                type: 'graph',
                layout: 'force',
                roam: true,
                draggable: true,
                data: data.nodes.map(n => {
                    const degree = degreeMap[n.name] || 0;
                    return {
                        ...n,
                        symbolSize: this.calculateNodeSize(n, degree),
                        itemStyle: {
                            color: getNodeColor(n),
                            shadowBlur: degree > 15 ? 25 : 15,
                            shadowColor: getNodeColor(n),
                            borderColor: degree > 15 ? 'rgba(255, 255, 255, 0.6)' : 'rgba(255, 255, 255, 0.3)',
                            borderWidth: degree > 15 ? 3 : 1.5
                        },
                        label: {
                            show: false, // 默认隐藏，避免重叠
                            position: 'top',
                            formatter: '{b}',
                            backgroundColor: 'rgba(26, 32, 44, 0.95)',
                            borderColor: 'rgba(66, 153, 225, 0.4)',
                            borderWidth: 1,
                            borderRadius: 6,
                            padding: [6, 10],
                            color: '#f7fafc',
                            fontSize: 12,
                            fontWeight: 600,
                            textShadowColor: 'rgba(0, 0, 0, 0.8)',
                            textShadowBlur: 4
                        }
                    };
                }),
                links: data.links.map(l => ({
                    ...l,
                    lineStyle: {
                        color: 'source',
                        opacity: 0.25,
                        width: 2,
                        curveness: 0.2,
                        shadowBlur: 6,
                        shadowColor: 'rgba(66, 153, 225, 0.3)'
                    },
                    symbol: ['none', 'arrow'],
                    symbolSize: [0, 6]
                })),
                force: {
                    repulsion: 250,
                    edgeLength: 80,
                    gravity: 0.1,
                    friction: 0.65
                },
                emphasis: {
                    focus: 'adjacency',
                    lineStyle: { 
                        opacity: 0.9, 
                        width: 3.5,
                        shadowBlur: 12,
                        shadowColor: 'rgba(66, 153, 225, 0.6)'
                    },
                    itemStyle: {
                        shadowBlur: 30,
                        borderWidth: 3
                    },
                    label: {
                        show: true // 悬停时显示标签
                    }
                },
                animation: !large,
                animationDuration: 800,
                animationEasing: 'cubicOut'
            }]
        };
        
        this.chart.setOption(option, true);
        Logger.success('力导向图渲染完成');
    }
    
    /**
     * 计算每个节点的连接度
     */
    calculateDegree(data) {
        const degreeMap = {};
        data.links.forEach(link => {
            const source = typeof link.source === 'object' ? link.source.name : link.source;
            const target = typeof link.target === 'object' ? link.target.name : link.target;
            degreeMap[source] = (degreeMap[source] || 0) + 1;
            degreeMap[target] = (degreeMap[target] || 0) + 1;
        });
        return degreeMap;
    }
    
    /**
     * 根据节点属性和连接度计算大小
     */
    calculateNodeSize(node, degree) {
        const baseSize = 15;
        const degreeFactor = Math.min(degree || 0, 30) / 30 * 20;
        const methodFactor = Math.min(node.methodCount || 0, 50) / 50 * 10;
        return baseSize + degreeFactor + methodFactor;
    }
    
    /**
     * Tooltip格式化器
     */
    tooltipFormatter(params) {
        if (params.dataType === 'node') {
            const d = params.data;
            const desc = cleanDescription(d.description, 30);
            const nodeColor = getNodeColor(d);
            
            return `
                <div style="max-width: 260px; padding: 4px">
                    <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px">
                        <div style="width: 14px; height: 14px; border-radius: 50%; background: ${nodeColor}; box-shadow: 0 0 10px ${nodeColor}"></div>
                        <strong style="color:${nodeColor}; font-size:14px; font-weight: 600">${d.name}</strong>
                    </div>
                    <div style="margin-top:6px; font-size:12px; line-height:1.6; color:#cbd5e0; white-space:normal; word-wrap:break-word">
                        ${desc || '<span style="color:#718096; font-style:italic">No description available</span>'}
                    </div>
                    <div style="margin-top:8px; padding-top:8px; border-top: 1px solid rgba(255,255,255,0.1); display: flex; gap: 12px; font-size:11px">
                        <span style="color:#63b3ed; font-weight:600">📊 Methods: ${d.methodCount}</span>
                        <span style="color:#68d391; font-weight:600">📝 Fields: ${d.fieldCount}</span>
                    </div>
                </div>
            `;
        }
        return params.name;
    }
    
    /**
     * 销毁图表实例
     */
    destroy() {
        if (this.chart) {
            this.chart.dispose();
        }
    }
}
