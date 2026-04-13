package cn.dolphinmind.glossary.java.analyze.unified;

import java.util.*;
import java.util.logging.Logger;

/**
 * 验证反馈回路 - Harness Engineering核心组件
 * 
 * 作用:
 * 1. 交叉验证: 静态规则和AI结论互相校验
 * 2. 自我校正: 低置信度问题重新审查或降级
 * 3. 规则进化: AI发现的新模式反馈到静态规则库
 */
public class ValidationLoop {
    
    private static final Logger logger = Logger.getLogger(ValidationLoop.class.getName());
    
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;
    private static final double LOW_CONFIDENCE_THRESHOLD = 0.5;
    private static final int MAX_RETRIES = 2;
    
    /**
     * 验证并校正合并后的问题列表
     */
    public List<UnifiedIssue> validateAndCorrect(List<UnifiedIssue> mergedIssues) {
        List<UnifiedIssue> corrected = new ArrayList<>();
        
        for (UnifiedIssue issue : mergedIssues) {
            ValidationResult result = validate(issue);
            
            switch (result.getAction()) {
                case ACCEPT:
                    corrected.add(issue);
                    break;
                    
                case DOWNGRADE:
                    // 降级严重程度
                    issue.setSeverity(downgradeSeverity(issue.getSeverity()));
                    issue.setMessage(issue.getMessage() + " [AI置信度较低，已降级]");
                    corrected.add(issue);
                    logger.info("Downgraded: " + issue.getRuleKey() + " at " + issue.getFilePath() + ":" + issue.getLine());
                    break;
                    
                case FLAG_FOR_REVIEW:
                    // 标记需要人工审查
                    issue.setAutoFiltered(true);
                    issue.setMessage(issue.getMessage() + " [需人工确认]");
                    corrected.add(issue);
                    logger.warning("Flagged for review: " + issue.getRuleKey());
                    break;
                    
                case RETRY_WITH_CONTEXT:
                    // 重试: 增加更多上下文重新审查 (需要外部LLM调用)
                    if (issue.getRetryCount() < MAX_RETRIES) {
                        issue.incrementRetryCount();
                        logger.info("Retrying with more context: " + issue.getRuleKey());
                        // TODO: 调用RagPipeline.reviewWithContext()重新审查
                    } else {
                        issue.setAutoFiltered(true);
                        corrected.add(issue);
                    }
                    break;
            }
        }
        
        return corrected;
    }
    
    /**
     * 单个问题的验证逻辑
     */
    private ValidationResult validate(UnifiedIssue issue) {
        // 规则1: 静态+AI都发现 → 高置信度，直接接受
        if (issue.getSource() == IssueSource.MERGED && issue.getConfidence() >= HIGH_CONFIDENCE_THRESHOLD) {
            return ValidationResult.ACCEPT;
        }
        
        // 规则2: 纯AI问题且置信度低 → 降级或标记
        if (issue.getSource() == IssueSource.AI) {
            if (issue.getConfidence() == null || issue.getConfidence() < LOW_CONFIDENCE_THRESHOLD) {
                return ValidationResult.FLAG_FOR_REVIEW;
            } else if (issue.getConfidence() < 0.7) {
                return ValidationResult.DOWNGRADE;
            }
        }
        
        // 规则3: CRITICAL级别但AI置信度<0.6 → 需要重试
        if ("CRITICAL".equals(issue.getSeverity()) && 
            issue.getConfidence() != null && 
            issue.getConfidence() < 0.6) {
            return ValidationResult.RETRY_WITH_CONTEXT;
        }
        
        // 规则4: 静态问题(置信度=1.0) → 直接接受
        if (issue.getSource() == IssueSource.STATIC) {
            return ValidationResult.ACCEPT;
        }
        
        // 默认: 接受
        return ValidationResult.ACCEPT;
    }
    
    /**
     * 降级严重程度
     */
    private String downgradeSeverity(String severity) {
        switch (severity) {
            case "CRITICAL": return "MAJOR";
            case "MAJOR": return "MINOR";
            case "MINOR": return "INFO";
            default: return severity;
        }
    }
    
    /**
     * 统计验证结果
     */
    public Map<String, Object> getValidationStats(List<UnifiedIssue> original, List<UnifiedIssue> corrected) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("originalCount", original.size());
        stats.put("correctedCount", corrected.size());
        
        long accepted = corrected.stream().filter(i -> !i.isAutoFiltered()).count();
        long filtered = corrected.stream().filter(UnifiedIssue::isAutoFiltered).count();
        long downgraded = corrected.stream()
            .filter(i -> i.getMessage() != null && i.getMessage().contains("已降级"))
            .count();
        
        stats.put("accepted", accepted);
        stats.put("filtered", filtered);
        stats.put("downgraded", downgraded);
        stats.put("filterRate", String.format("%.1f%%", (double) filtered / corrected.size() * 100));
        
        return stats;
    }
    
    enum Action {
        ACCEPT,              // 接受
        DOWNGRADE,           // 降级
        FLAG_FOR_REVIEW,     // 标记待审
        RETRY_WITH_CONTEXT   // 重试
    }
    
    static class ValidationResult {
        static final ValidationResult ACCEPT = new ValidationResult(Action.ACCEPT);
        static final ValidationResult DOWNGRADE = new ValidationResult(Action.DOWNGRADE);
        static final ValidationResult FLAG_FOR_REVIEW = new ValidationResult(Action.FLAG_FOR_REVIEW);
        static final ValidationResult RETRY_WITH_CONTEXT = new ValidationResult(Action.RETRY_WITH_CONTEXT);
        
        private final Action action;
        
        ValidationResult(Action action) {
            this.action = action;
        }
        
        Action getAction() {
            return action;
        }
    }
}
