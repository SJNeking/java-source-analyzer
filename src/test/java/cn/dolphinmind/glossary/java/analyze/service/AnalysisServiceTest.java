package cn.dolphinmind.glossary.java.analyze.service;

import cn.dolphinmind.glossary.java.analyze.dto.AnalysisRequestDTO;
import cn.dolphinmind.glossary.java.analyze.event.AnalysisEvent;
import cn.dolphinmind.glossary.java.analyze.event.EventBus;
import cn.dolphinmind.glossary.java.analyze.exception.ApiException;
import cn.dolphinmind.glossary.java.analyze.pipeline.AnalysisOrchestrator;
import cn.dolphinmind.glossary.java.analyze.repository.AnalysisRepository;
import cn.dolphinmind.glossary.java.analyze.repository.IssueRepository;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * AnalysisService 单元测试
 * 
 * 验证参数校验、事件发布、以及异常处理逻辑。
 */
public class AnalysisServiceTest {

    @Test(expected = ApiException.class)
    public void testStartAnalysisWithNullRequest() {
        AnalysisService service = createService();
        service.startAnalysis(null);
    }

    @Test(expected = ApiException.class)
    public void testStartAnalysisWithEmptySourceRoot() {
        AnalysisService service = createService();
        AnalysisRequestDTO req = new AnalysisRequestDTO();
        service.startAnalysis(req);
    }

    @Test
    public void testStartAnalysisValid() {
        AnalysisService service = createService();
        
        List<AnalysisEvent> capturedEvents = new ArrayList<>();
        EventBus bus = EventBus.getInstance();
        bus.register(capturedEvents::add);

        AnalysisRequestDTO req = new AnalysisRequestDTO();
        req.setSourceRoot("/valid/path");
        req.setProjectName("test-project");
        
        String taskId = service.startAnalysis(req);
        
        assertNotNull("Task ID should be generated", taskId);
        assertEquals("Should publish STARTED event", 1, capturedEvents.size());
        assertEquals("Event type should be STARTED", AnalysisEvent.Type.STARTED, capturedEvents.get(0).getType());
        
        bus.unregister(capturedEvents::add);
    }

    private AnalysisService createService() {
        // 使用 Mock 依赖
        AnalysisRepository mockRepo = new AnalysisRepository() {
            public void save(cn.dolphinmind.glossary.java.analyze.dto.AnalysisResultDTO dto) {}
            public java.util.Optional<cn.dolphinmind.glossary.java.analyze.dto.AnalysisResultDTO> findById(String id) { return java.util.Optional.empty(); }
            public List<cn.dolphinmind.glossary.java.analyze.dto.AnalysisResultDTO> findByProjectName(String p, int l) { return new ArrayList<>(); }
            public void deleteOlderThan(long t) {}
        };
        
        IssueRepository mockIssueRepo = new IssueRepository() {
            public void batchSave(String id, List<cn.dolphinmind.glossary.java.analyze.dto.IssueDTO> issues) {}
            public List<cn.dolphinmind.glossary.java.analyze.dto.IssueDTO> findByAnalysisId(String id) { return new ArrayList<>(); }
            public List<cn.dolphinmind.glossary.java.analyze.dto.IssueDTO> findByAnalysisIdAndSeverity(String id, String s) { return new ArrayList<>(); }
            public int countByAnalysisId(String id) { return 0; }
        };
        
        return new AnalysisService(
            mockRepo,
            mockIssueRepo,
            new AnalysisOrchestrator(),
            EventBus.getInstance()
        );
    }
}
