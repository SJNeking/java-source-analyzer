package cn.dolphinmind.glossary.java.analyze.monitoring;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics Collector
 * 
 * Collects application metrics and exposes them in Prometheus text format.
 * Tracks:
 * - Total analyses run
 * - RAG pipeline latency
 * - Errors count
 */
public class MetricsCollector {

    // Counters
    private final AtomicLong totalAnalyses = new AtomicLong(0);
    private final AtomicLong totalRagReviews = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);

    // Latencies (sum + count for average calculation)
    private volatile double totalRagLatencyMs = 0;
    private volatile long totalRagCount = 0;

    // Singleton
    private static final MetricsCollector instance = new MetricsCollector();
    public static MetricsCollector getInstance() { return instance; }
    private MetricsCollector() {}

    public void incrementAnalyses() { totalAnalyses.incrementAndGet(); }
    public void incrementRagReviews() { totalRagReviews.incrementAndGet(); }
    public void incrementErrors() { totalErrors.incrementAndGet(); }

    public void recordRagLatency(double ms) {
        synchronized (this) {
            totalRagLatencyMs += ms;
            totalRagCount++;
        }
    }

    /**
     * Export metrics in Prometheus text format.
     */
    public String exportPrometheus() {
        StringBuilder sb = new StringBuilder();
        sb.append("# HELP codeguardian_analyses_total Total number of static analyses run.\n");
        sb.append("# TYPE codeguardian_analyses_total counter\n");
        sb.append("codeguardian_analyses_total ").append(totalAnalyses.get()).append("\n\n");

        sb.append("# HELP codeguardian_rag_reviews_total Total number of RAG reviews run.\n");
        sb.append("# TYPE codeguardian_rag_reviews_total counter\n");
        sb.append("codeguardian_rag_reviews_total ").append(totalRagReviews.get()).append("\n\n");

        sb.append("# HELP codeguardian_errors_total Total number of errors encountered.\n");
        sb.append("# TYPE codeguardian_errors_total counter\n");
        sb.append("codeguardian_errors_total ").append(totalErrors.get()).append("\n\n");

        double avgLatency = 0;
        synchronized (this) {
            if (totalRagCount > 0) avgLatency = totalRagLatencyMs / totalRagCount;
        }
        
        sb.append("# HELP codeguardian_rag_latency_seconds Average RAG review latency.\n");
        sb.append("# TYPE codeguardian_rag_latency_seconds gauge\n");
        sb.append("codeguardian_rag_latency_seconds ").append(String.format("%.3f", avgLatency / 1000.0)).append("\n");

        return sb.toString();
    }
}
