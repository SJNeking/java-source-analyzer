import React from 'react';
import { useAppStore } from '../store';

const LoadingOverlay: React.FC = () => {
  const { isLoading } = useAppStore();
  
  if (!isLoading) return null;

  return (
    <div className="loading-overlay">
      <div className="spinner"></div>
      <div style={{ marginTop: '16px', fontSize: '13px', color: 'var(--accent)' }}>
        正在解析项目资产...
      </div>
    </div>
  );
};

export default LoadingOverlay;
