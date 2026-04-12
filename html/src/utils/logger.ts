/**
 * Java Source Analyzer - Logger Utility
 * Formatted console logging with emoji indicators
 */

/**
 * Logger singleton for formatted console output
 */
export class Logger {
  /**
   * Information message
   */
  static info(message: string, ...args: unknown[]): void {
    console.log(`ℹ️  ${message}`, ...args);
  }

  /**
   * Success message
   */
  static success(message: string, ...args: unknown[]): void {
    console.log(`✅ ${message}`, ...args);
  }

  /**
   * Warning message
   */
  static warning(message: string, ...args: unknown[]): void {
    console.warn(`⚠️  ${message}`, ...args);
  }

  /**
   * Error message
   */
  static error(message: string, ...args: unknown[]): void {
    console.error(`❌ ${message}`, ...args);
  }

  /**
   * Debug message
   */
  static debug(message: string, ...args: unknown[]): void {
    console.debug(`🔍 ${message}`, ...args);
  }

  /**
   * Group start
   */
  static group(label: string): void {
    console.group(label);
  }

  /**
   * Group end
   */
  static groupEnd(): void {
    console.groupEnd();
  }

  /**
   * Time tracker
   */
  static time(label: string): void {
    console.time(label);
  }

  /**
   * Time end
   */
  static timeEnd(label: string): void {
    console.timeEnd(label);
  }
}
