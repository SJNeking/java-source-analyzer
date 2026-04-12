/**
 * Java Source Analyzer - Annotation Manager
 * Handles adding and retrieving user annotations on source code.
 * Persists to localStorage.
 */

export interface CodeComment {
  id: string;
  line: number;
  text: string;
  timestamp: number;
}

/**
 * Storage structure:
 * {
 *   "ClassName#MethodName": [
 *     { line: 10, text: "Bug?", timestamp: ... },
 *     ...
 *   ]
 * }
 */
type AnnotationStore = Record<string, CodeComment[]>;

export class AnnotationManager {
  private annotations: AnnotationStore = {};
  private storageKey: string = 'java-analyzer-annotations';

  constructor() {
    this.load();
  }

  /**
   * Load annotations from localStorage
   */
  private load(): void {
    try {
      const data = localStorage.getItem(this.storageKey);
      if (data) {
        this.annotations = JSON.parse(data);
      }
    } catch (e) {
      console.error('Failed to load annotations:', e);
      this.annotations = {};
    }
  }

  /**
   * Save annotations to localStorage
   */
  private save(): void {
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(this.annotations));
    } catch (e) {
      console.error('Failed to save annotations:', e);
    }
  }

  /**
   * Get comments for a specific method
   * @param methodKey Unique identifier: "ClassName#MethodName"
   */
  public getComments(methodKey: string): CodeComment[] {
    return this.annotations[methodKey] || [];
  }

  /**
   * Add a comment to a specific line
   */
  public addComment(methodKey: string, line: number, text: string): void {
    if (!this.annotations[methodKey]) {
      this.annotations[methodKey] = [];
    }

    const comment: CodeComment = {
      id: `c-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      line,
      text,
      timestamp: Date.now()
    };

    this.annotations[methodKey].push(comment);
    this.save();
    console.log(`💬 Added comment to ${methodKey}:${line}`);
  }

  /**
   * Delete a comment
   */
  public deleteComment(methodKey: string, commentId: string): void {
    if (this.annotations[methodKey]) {
      this.annotations[methodKey] = this.annotations[methodKey].filter(c => c.id !== commentId);
      this.save();
    }
  }

  /**
   * Check if a method has any comments
   */
  public hasComments(methodKey: string): boolean {
    return (this.annotations[methodKey]?.length || 0) > 0;
  }

  /**
   * Get comment count for a method
   */
  public getCommentCount(methodKey: string): number {
    return this.annotations[methodKey]?.length || 0;
  }
}
