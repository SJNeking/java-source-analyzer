/**
 * Data Fetcher Service
 * 
 * Handles all data loading operations with caching and retry logic.
 * Separated from App class for better testability and maintainability.
 */

import type { AnalysisResult } from '../types';
import { Logger } from '../utils/logger';
import { LRUCache } from '../utils/lru-cache';
import { DataValidator } from '../utils/data-validator';

export class DataFetcherService {
  private rawDataCache: LRUCache<string, AnalysisResult>;

  constructor(cacheSize: number = 3) {
    this.rawDataCache = new LRUCache(cacheSize);
  }

  /**
   * Load project data with caching and auto-retry
   */
  async loadProject(filename: string): Promise<AnalysisResult> {
    if (!filename) {
      throw new Error('Filename is required');
    }

    // Check cache first
    const cached = this.rawDataCache.get(filename);
    if (cached) {
      Logger.info(`Cache hit for ${filename}`);
      return cached;
    }

    // Fetch with retry
    Logger.info(`Fetching data for ${filename}...`);
    const result = await this.fetchWithRetry(`/data/${filename}`);
    
    // Cache the result
    this.rawDataCache.set(filename, result);
    
    return result;
  }

  /**
   * Clear the data cache
   */
  clearCache(): void {
    this.rawDataCache.clear();
  }

  /**
   * Get cache size
   */
  get cacheSize(): number {
    return this.rawDataCache.size;
  }

  /**
   * Fetch data with exponential backoff retry
   */
  private async fetchWithRetry(
    url: string,
    retries: number = 3,
    delay: number = 1000
  ): Promise<AnalysisResult> {
    try {
      const response = await fetch(`${url}?t=${Date.now()}`);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      const rawData = await response.json();
      
      // Validate data structure
      DataValidator.validateAnalysisResult(rawData);
      
      return rawData;
    } catch (error) {
      if (retries > 0) {
        Logger.warning(`Fetch failed (${url}), retrying in ${delay}ms... (${retries} attempts left)`);
        await new Promise(resolve => setTimeout(resolve, delay));
        return this.fetchWithRetry(url, retries - 1, delay * 2);
      }
      
      Logger.error(`Fetch failed after all retries: ${url}`, error);
      throw error;
    }
  }
}

// Export singleton instance
export const dataFetcher = new DataFetcherService(3);
