/**
 * LRU Cache Implementation
 * Automatically removes least recently used items when capacity is reached.
 * Used for caching source code snippets to prevent memory leaks.
 */
export class LRUCache<K, V> {
    private cache = new Map<K, V>();
    private maxSize: number;

    constructor(maxSize: number = 100) {
        this.maxSize = maxSize;
    }

    public get(key: K): V | undefined {
        const item = this.cache.get(key);
        if (item) {
            // Move to end (most recently used)
            this.cache.delete(key);
            this.cache.set(key, item);
        }
        return item;
    }

    public set(key: K, value: V): void {
        if (this.cache.has(key)) {
            this.cache.delete(key);
        } else if (this.cache.size >= this.maxSize) {
            // Evict the first item (least recently used)
            const firstKey = this.cache.keys().next().value;
            this.cache.delete(firstKey);
        }
        this.cache.set(key, value);
    }

    public has(key: K): boolean {
        return this.cache.has(key);
    }

    public clear(): void {
        this.cache.clear();
    }

    public get size(): number {
        return this.cache.size;
    }
}
