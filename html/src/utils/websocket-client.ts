/**
 * Java Source Analyzer - WebSocket Client
 * Connects to the Java analyzer's WebSocket server for real-time progress updates
 */

import { Logger } from './logger';
import { getWebSocketUrl } from '../config';

/**
 * WebSocket connection states
 */
export enum WsState {
  DISCONNECTED = 'disconnected',
  CONNECTING = 'connecting',
  CONNECTED = 'connected',
  ERROR = 'error'
}

/**
 * Analysis progress event from WebSocket
 */
export interface AnalysisProgressEvent {
  type: string;
  message: string;
  progress: number;
  data: Record<string, unknown>;
  timestamp: number;
}

/**
 * WebSocket client callback interface
 */
export interface WsClientCallbacks {
  onConnected?: () => void;
  onDisconnected?: () => void;
  onProgress?: (event: AnalysisProgressEvent) => void;
  onError?: (error: string) => void;
  onAnalysisComplete?: (data: Record<string, unknown>) => void;
}

/**
 * WebSocket client for real-time analysis progress
 */
export class AnalysisWebSocketClient {
  /** WebSocket connection */
  private ws: WebSocket | null = null;

  /** Connection state */
  private state: WsState = WsState.DISCONNECTED;

  /** Server URL */
  private url: string;

  /** Callbacks */
  private callbacks: WsClientCallbacks;

  /** Reconnect timer */
  private reconnectTimer: number | null = null;

  /** Reconnect attempts */
  private reconnectAttempts = 0;

  /** Max reconnect attempts */
  private readonly MAX_RECONNECT_ATTEMPTS = 5;

  /**
   * Create a new WebSocket client
   * @param url - WebSocket server URL (e.g., 'ws://localhost:8887'). If not provided, uses config.
   * @param callbacks - Event callbacks
   */
  constructor(url?: string, callbacks: WsClientCallbacks = {}) {
    this.url = url || getWebSocketUrl();
    this.callbacks = callbacks;
  }

  /**
   * Connect to the WebSocket server
   */
  public connect(): void {
    if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
      Logger.warning('WebSocket already connected or connecting');
      return;
    }

    this.state = WsState.CONNECTING;
    Logger.info(`Connecting to WebSocket: ${this.url}`);

    try {
      this.ws = new WebSocket(this.url);

      this.ws.onopen = () => {
        this.state = WsState.CONNECTED;
        this.reconnectAttempts = 0;
        Logger.success('WebSocket connected');
        this.callbacks.onConnected?.();
      };

      this.ws.onmessage = (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data) as AnalysisProgressEvent | Record<string, unknown>;
          this.handleMessage(data);
        } catch (error) {
          Logger.error('Failed to parse WebSocket message:', error);
        }
      };

      this.ws.onclose = () => {
        this.state = WsState.DISCONNECTED;
        Logger.warning('WebSocket disconnected');
        this.callbacks.onDisconnected?.();
        this.scheduleReconnect();
      };

      this.ws.onerror = (error) => {
        this.state = WsState.ERROR;
        // Silent: WebSocket server is optional, only log on first attempt
        if (this.reconnectAttempts === 0) {
          Logger.debug('WebSocket server not available');
        }
        this.callbacks.onError?.('WebSocket connection failed');
      };
    } catch (error) {
      this.state = WsState.ERROR;
      Logger.error('Failed to create WebSocket connection:', error);
      this.callbacks.onError?.('Failed to create WebSocket connection');
    }
  }

  /**
   * Disconnect from the WebSocket server
   */
  public disconnect(): void {
    this.clearReconnectTimer();
    this.reconnectAttempts = this.MAX_RECONNECT_ATTEMPTS; // Prevent reconnect

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }

    this.state = WsState.DISCONNECTED;
    Logger.info('WebSocket disconnected');
  }

  /**
   * Get current connection state
   */
  public getState(): WsState {
    return this.state;
  }

  /**
   * Check if connected
   */
  public isConnected(): boolean {
    return this.state === WsState.CONNECTED;
  }

  /**
   * Handle incoming WebSocket message
   */
  private handleMessage(data: AnalysisProgressEvent | Record<string, unknown>): void {
    const event = data as AnalysisProgressEvent;

    if (!event.type) return;

    switch (event.type) {
      case 'CONNECTION_ESTABLISHED':
        Logger.success(event.message);
        break;

      case 'SCAN_START':
      case 'FILE_SCANNED':
      case 'FILE_SKIPPED':
      case 'MODULE_COMPLETE':
        // Scanning progress
        this.callbacks.onProgress?.(event);
        break;

      case 'ASSETS_START':
      case 'ASSETS_COMPLETE':
      case 'RELATIONS_START':
      case 'RELATIONS_COMPLETE':
      case 'QUALITY_START':
      case 'QUALITY_COMPLETE':
        // Analysis phase progress
        this.callbacks.onProgress?.(event);
        break;

      case 'ANALYSIS_COMPLETE':
        Logger.success(event.message);
        this.callbacks.onAnalysisComplete?.(event.data);
        this.callbacks.onProgress?.(event);
        break;

      case 'ERROR':
        Logger.error(event.message);
        this.callbacks.onError?.(event.message);
        break;

      default:
        Logger.debug('Unknown WebSocket message type:', event.type);
    }
  }

  /**
   * Schedule reconnect attempt
   */
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.MAX_RECONNECT_ATTEMPTS) {
      Logger.warning('Max reconnect attempts reached, giving up');
      return;
    }

    this.clearReconnectTimer();

    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
    this.reconnectAttempts++;

    Logger.info(`Scheduling reconnect attempt ${this.reconnectAttempts}/${this.MAX_RECONNECT_ATTEMPTS} in ${delay}ms`);

    this.reconnectTimer = window.setTimeout(() => {
      Logger.info('Reconnecting...');
      this.connect();
    }, delay);
  }

  /**
   * Clear reconnect timer
   */
  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  /**
   * Get reconnect attempts count
   */
  public getReconnectAttempts(): number {
    return this.reconnectAttempts;
  }
}
