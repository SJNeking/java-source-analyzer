/**
 * Data fetching service with caching
 */

import { apiService } from './api.service';

interface CacheEntry<T> {
  data: T;
  timestamp: number;
  ttl: number; // Time to live in milliseconds
}

class DataFetcherService {
  private cache = new Map<string, CacheEntry<any>>();
  private DEFAULT_TTL = 5 * 60 * 1000; // 5 minutes

  /**
   * Load projects with caching
   */
  async loadProjects() {
    const cacheKey = 'projects';
    const cached = this.getFromCache(cacheKey);
    
    if (cached) {
      return cached;
    }

    const projects = await apiService.getProjects();
    this.setCache(cacheKey, projects);
    
    return projects;
  }

  /**
   * Load project data with caching
   */
  async loadProjectData(filename: string) {
    const cacheKey = `project:${filename}`;
    const cached = this.getFromCache(cacheKey);
    
    if (cached) {
      return cached;
    }

    const data = await apiService.loadProjectData(filename);
    this.setCache(cacheKey, data);
    
    return data;
  }

  /**
   * Load unified report (prioritized over legacy format)
   */
  async loadUnifiedReport(projectFile: string) {
    const cacheKey = `unified:${projectFile}`;
    const cached = this.getFromCache(cacheKey);
    
    if (cached) {
      return cached;
    }

    const report = await apiService.loadUnifiedReport(projectFile);
    
    if (report) {
      this.setCache(cacheKey, report);
    }
    
    return report;
  }

  /**
   * Get from cache if valid
   */
  private getFromCache<T>(key: string): T | null {
    const entry = this.cache.get(key);
    
    if (!entry) {
      return null;
    }

    const now = Date.now();
    if (now - entry.timestamp > entry.ttl) {
      // Cache expired
      this.cache.delete(key);
      return null;
    }

    return entry.data as T;
  }

  /**
   * Set cache entry
   */
  private setCache<T>(key: string, data: T, ttl?: number) {
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      ttl: ttl || this.DEFAULT_TTL,
    });
  }

  /**
   * Clear all cache
   */
  clearCache() {
    this.cache.clear();
  }

  /**
   * Clear specific cache entry
   */
  clearCacheEntry(key: string) {
    this.cache.delete(key);
  }
}

// Singleton instance
export const dataFetcher = new DataFetcherService();
