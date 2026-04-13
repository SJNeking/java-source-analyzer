/**
 * API Service Layer
 * 
 * Handles all backend communication with retry logic and error handling
 */

import axios, { AxiosError, AxiosInstance } from 'axios';

const API_BASE_URL = '/api';

class ApiService {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      async (error: AxiosError) => {
        console.error('API Error:', error.message);
        
        if (error.response) {
          const status = error.response.status;
          
          if (status === 404) {
            throw new Error('请求的资源不存在');
          } else if (status === 500) {
            throw new Error('服务器内部错误');
          } else if (status === 401) {
            throw new Error('未授权访问');
          }
        } else if (error.code === 'ECONNABORTED') {
          throw new Error('请求超时，请检查网络连接');
        }
        
        return Promise.reject(error);
      }
    );
  }

  /**
   * Fetch projects index
   */
  async getProjects(): Promise<Array<{ name: string; file: string }>> {
    try {
      const response = await this.client.get('/projects/index.json');
      return response.data.projects || [];
    } catch (error) {
      // Fallback to mock data for development
      console.warn('Failed to load projects, using mock data');
      return [
        { name: 'Demo Project', file: 'demo-project.json' },
      ];
    }
  }

  /**
   * Load project graph data
   */
  async loadProjectData(filename: string): Promise<any> {
    const response = await this.client.get(`/data/${filename}`);
    return response.data;
  }

  /**
   * Load unified report (with Harness Engineering data)
   */
  async loadUnifiedReport(): Promise<any> {
    try {
      const response = await this.client.get(`/data/unified-report.json`);
      return response.data;
    } catch (error) {
      console.warn('Unified report not found, falling back to legacy format');
      return null;
    }
  }

  /**
   * Load analysis result (legacy format)
   */
  async loadAnalysisResult(projectFile: string): Promise<any> {
    const response = await this.client.get(`/data/${projectFile}`);
    return response.data;
  }

  /**
   * Generic GET request with retry
   */
  async get<T>(url: string, retries = 3, backoff = 1000): Promise<T> {
    try {
      const response = await this.client.get<T>(url);
      return response.data;
    } catch (error) {
      if (retries > 0 && this.isRetryableError(error)) {
        console.warn(`Request failed, retrying in ${backoff}ms... (${retries} attempts left)`);
        await this.delay(backoff);
        return this.get(url, retries - 1, backoff * 2);
      }
      throw error;
    }
  }

  /**
   * Check if error is retryable
   */
  private isRetryableError(error: any): boolean {
    return (
      error.code === 'ECONNABORTED' ||
      error.code === 'ETIMEDOUT' ||
      (error.response && error.response.status >= 500)
    );
  }

  /**
   * Delay utility
   */
  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}

// Singleton instance
export const apiService = new ApiService();
