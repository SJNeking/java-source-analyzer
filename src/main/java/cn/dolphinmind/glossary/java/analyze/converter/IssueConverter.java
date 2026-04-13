package cn.dolphinmind.glossary.java.analyze.converter;

import cn.dolphinmind.glossary.java.analyze.dto.IssueDTO;
import cn.dolphinmind.glossary.java.analyze.unified.UnifiedIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Issue 转换器
 * 
 * 负责在 UnifiedIssue (领域模型) 和 IssueDTO (传输对象) 之间转换。
 */
public class IssueConverter {

    /**
     * 领域模型 -> DTO
     */
    public static IssueDTO toDTO(UnifiedIssue issue) {
        if (issue == null) return null;
        
        IssueDTO dto = new IssueDTO();
        dto.setId(issue.getId() != null ? issue.getId() : UUID.randomUUID().toString().substring(0, 8));
        dto.setSource(issue.getSource() != null ? issue.getSource().name() : "unknown");
        dto.setSeverity(issue.getSeverity() != null ? issue.getSeverity().name() : "INFO");
        dto.setCategory(issue.getCategory() != null ? issue.getCategory().name() : "CODE_SMELL");
        dto.setFilePath(issue.getFilePath());
        dto.setClassName(issue.getClassName());
        dto.setMethodName(issue.getMethodName());
        dto.setLine(issue.getLine());
        dto.setMessage(issue.getMessage());
        dto.setConfidence(issue.getConfidence());
        dto.setAiSuggestion(issue.getAiSuggestion());
        dto.setAiFixedCode(issue.getAiFixedCode());
        
        return dto;
    }

    /**
     * 领域模型列表 -> DTO 列表
     */
    public static List<IssueDTO> toDTOList(List<UnifiedIssue> issues) {
        List<IssueDTO> result = new ArrayList<>();
        if (issues != null) {
            for (UnifiedIssue issue : issues) {
                result.add(toDTO(issue));
            }
        }
        return result;
    }
}
