/**
 * Java Source Analyzer - Frontend Configuration
 * Centralized configuration for API endpoints and server URLs
 */

/**
 * Server configuration
 * These values can be overridden by environment-specific build processes
 */
export const SERVER_CONFIG = {
  /** WebSocket server URL for real-time updates */
  WS_URL: getEnvVar('WS_URL', 'ws://localhost:8887'),
  
  /** HTTP API base URL */
  API_BASE_URL: getEnvVar('API_BASE_URL', 'http://localhost:8887'),
  
  /** Development server port */
  DEV_PORT: parseInt(getEnvVar('DEV_PORT', '8080'), 10),
  
  /** CDN base URL for external libraries */
  CDN_BASE_URL: getEnvVar('CDN_BASE_URL', 'https://cdn.jsdelivr.net/npm'),
} as const;

/**
 * Get environment variable with fallback
 * Works with build-time environment variables
 */
function getEnvVar(key: string, fallback: string): string {
  // Check for build-time environment variables (injected by esbuild define plugin)
  const envVar = (globalThis as any)[`__ENV_${key}__`];
  return envVar !== undefined ? envVar : fallback;
}

/**
 * Get WebSocket URL with smart defaults
 * Tries to detect the current host if possible
 */
export function getWebSocketUrl(): string {
  // If running from the same host, try to detect automatically
  if (typeof window !== 'undefined') {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.hostname;
    // Default WebSocket port is typically the same as the API server
    const wsPort = SERVER_CONFIG.WS_URL.split(':').pop()?.split('/')[0] || '8887';
    return `${protocol}//${host}:${wsPort}`;
  }
  return SERVER_CONFIG.WS_URL;
}

/**
 * Get API URL with smart defaults
 */
export function getApiUrl(path: string): string {
  if (typeof window !== 'undefined' && !path.startsWith('http')) {
    // Relative URL - use current host
    return path;
  }
  return `${SERVER_CONFIG.API_BASE_URL}${path}`;
}

export default SERVER_CONFIG;
