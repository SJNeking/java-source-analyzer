/**
 * MermaidDiagram - React Component
 * Mermaid UML图表渲染组件
 */

import React, { useEffect, useRef } from 'react';
import mermaid from 'mermaid';

interface MermaidDiagramProps {
  chart: string;
  className?: string;
}

const MermaidDiagram: React.FC<MermaidDiagramProps> = ({ chart, className }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [svg, setSvg] = React.useState<string>('');
  const [error, setError] = React.useState<string | null>(null);

  useEffect(() => {
    mermaid.initialize({
      startOnLoad: false,
      theme: 'dark',
      securityLevel: 'loose',
      fontFamily: 'SF Mono, Monaco, monospace',
      fontSize: 14,
    });
  }, []);

  useEffect(() => {
    const renderDiagram = async () => {
      try {
        const { svg } = await mermaid.render(`mermaid-${Math.random().toString(36).substr(2, 9)}`, chart);
        setSvg(svg);
        setError(null);
      } catch (err) {
        console.error('Mermaid rendering error:', err);
        setError('图表渲染失败');
      }
    };

    if (chart) {
      renderDiagram();
    }
  }, [chart]);

  if (error) {
    return (
      <div style={{ padding: '20px', textAlign: 'center', color: '#ef4444' }}>
        ⚠️ {error}
      </div>
    );
  }

  if (!svg) {
    return (
      <div style={{ padding: '20px', textAlign: 'center', color: '#64748b' }}>
        🔄 渲染中...
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      className={className}
      dangerouslySetInnerHTML={{ __html: svg }}
      style={{
        display: 'flex',
        justifyContent: 'center',
        padding: '20px',
        backgroundColor: '#0f172a',
        borderRadius: '8px',
      }}
    />
  );
};

export default MermaidDiagram;
