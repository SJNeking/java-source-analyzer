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
        
        const option = {
            backgroundColor: 'transparent',
            tooltip: {
                trigger: 'item',
                backgroundColor: 'rgba(26, 32, 44, 0.95)',
                borderColor: 'rgba(255, 255, 255, 0.1)',
                borderWidth: 1,
                textStyle: { color: '#e2e8f0' },
                formatter: (params) => this.tooltipFormatter(params)
            },
            series: [{
                type: 'graph',
                layout: 'force',
                roam: true,
                draggable: true,
                data: data.nodes.map(n => ({
                    ...n,
                    itemStyle: { color: getNodeColor(n) }
                })),
                links: data.links.map(l => ({
                    ...l,
                    lineStyle: {
                        color: 'source',
                        opacity: 0.08,
                        width: 1,
                        curveness: 0.1
                    }
                })),
                label: {
                    show: !large,
                    position: 'right',
                    color: '#a0aec0',
                    fontSize: 10
                },
                force: {
                    repulsion: 180,
                    edgeLength: 50,
                    gravity: 0.05,
                    friction: 0.75
                },
                emphasis: {
                    focus: 'adjacency',
                    lineStyle: { opacity: 0.8, width: 2 }
                },
                animation: !large,
                animationDuration: 600
            }]
        };
        
        this.chart.setOption(option, true);
        Logger.success('力导向图渲染完成');
    }
    
    /**
     * Tooltip格式化器
     */
    tooltipFormatter(params) {
        if (params.dataType === 'node') {
            const d = params.data;
            const desc = cleanDescription(d.description, 30);
            
            return `
                <div style="max-width: 220px; padding: 4px">
                    <strong style="color:${getNodeColor(d)}; font-size:13px">${d.name}</strong>
                    <div style="margin-top:3px; font-size:11px; line-height:1.4; color:#cbd5e0; white-space:nowrap; overflow:hidden; text-overflow:ellipsis">
                        ${desc || '<span style="color:#718096">No description</span>'}
                    </div>
                    <div style="margin-top:4px; font-size:10px; color:#63b3ed; font-weight:500">
                        M:${d.methodCount} · F:${d.fieldCount}
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
