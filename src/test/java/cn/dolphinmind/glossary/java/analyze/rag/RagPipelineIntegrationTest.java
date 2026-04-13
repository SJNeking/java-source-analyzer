package cn.dolphinmind.glossary.java.analyze.rag;

import cn.dolphinmind.glossary.java.analyze.slicing.CodeSlicer;
import cn.dolphinmind.glossary.java.analyze.unified.UnifiedIssue;
import com.google.gson.Gson;
import org.junit.jupiter.api.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAG Pipeline Integration Test
 * 
 * 验证完整流程: 静态分析 → CodeSlicer切片 → RAG审查 → 结果合并
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RagPipelineIntegrationTest {
    
    private static final String TEST_DATA_PATH = "src/test/resources/examples/static-analysis-sample.json";
    private static final String SOURCE_ROOT = "src/main/java";
    
    private Gson gson;
    
    @BeforeEach
    void setUp() {
        gson = new Gson();
    }
    
    /**
     * 测试1: 验证测试数据格式正确
     */
    @Test
    @Order(1)
    void testStaticAnalysisDataFormat() throws Exception {
        Path testDataPath = Paths.get(TEST_DATA_PATH);
        assertTrue(Files.exists(testDataPath), "测试数据文件不存在");
        
        String jsonContent = new String(Files.readAllBytes(testDataPath));
        Map<String, Object> data = gson.fromJson(jsonContent, Map.class);
        
        // 验证必需字段
        assertTrue(data.containsKey("version"), "缺少version字段");
        assertTrue(data.containsKey("project"), "缺少project字段");
        assertTrue(data.containsKey("quality_issues"), "缺少quality_issues字段");
        
        // 验证quality_issues数组
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> issues = (List<Map<String, Object>>) data.get("quality_issues");
        assertFalse(issues.isEmpty(), "quality_issues不能为空");
        
        // 验证第一个问题的字段
        Map<String, Object> firstIssue = issues.get(0);
        assertTrue(firstIssue.containsKey("rule_key"), "缺少rule_key字段");
        assertTrue(firstIssue.containsKey("severity"), "缺少severity字段");
        assertTrue(firstIssue.containsKey("file"), "缺少file字段");
        assertTrue(firstIssue.containsKey("line"), "缺少line字段");
        assertTrue(firstIssue.containsKey("message"), "缺少message字段");
        
        System.out.println("✓ Static analysis data format validated: " + issues.size() + " issues");
    }
    
    /**
     * 测试2: CodeSlicer能从静态分析结果生成切片
     */
    @Test
    @Order(2)
    void testCodeSlicerGeneratesSlices() throws Exception {
        Path sourceRoot = Paths.get(SOURCE_ROOT);
        assertTrue(Files.exists(sourceRoot), "源码目录不存在: " + sourceRoot);
        
        String jsonContent = new String(Files.readAllBytes(Paths.get(TEST_DATA_PATH)));
        @SuppressWarnings("unchecked")
        Map<String, Object> analysisResult = gson.fromJson(jsonContent, Map.class);
        
        CodeSlicer slicer = new CodeSlicer();
        Map<String, Object> ragContext = slicer.buildRagContext(sourceRoot, analysisResult);
        
        // 验证切片结果
        assertNotNull(ragContext, "RAG Context不应为null");
        assertTrue(ragContext.containsKey("totalSlices"), "缺少totalSlices字段");
        assertTrue(ragContext.containsKey("totalEstimatedTokens"), "缺少totalEstimatedTokens字段");
        assertTrue(ragContext.containsKey("slices"), "缺少slices字段");
        
        int totalSlices = (int) ragContext.get("totalSlices");
        long totalTokens = ((Number) ragContext.get("totalEstimatedTokens")).longValue();
        
        System.out.println("✓ CodeSlicer generated " + totalSlices + " slices, " + totalTokens + " tokens");
        
        // 验证Token优化效果
        assertTrue(totalTokens < 50000, "Token数应小于50000，实际: " + totalTokens);
    }
    
    /**
     * 测试3: ValidationLoop能正确验证问题列表
     */
    @Test
    @Order(3)
    void testValidationLoop() {
        cn.dolphinmind.glossary.java.analyze.unified.ValidationLoop validation = 
            new cn.dolphinmind.glossary.java.analyze.unified.ValidationLoop();
        
        // 创建测试问题
        List<UnifiedIssue> testIssues = new ArrayList<>();
        testIssues.add(UnifiedIssue.builder()
            .source(cn.dolphinmind.glossary.java.analyze.unified.IssueSource.STATIC)
            .ruleKey("NULL_POINTER")
            .severity("CRITICAL")
            .filePath("src/main/java/Test.java")
            .line(10)
            .message("Null pointer risk")
            .confidence(1.0)
            .build());
        
        testIssues.add(UnifiedIssue.builder()
            .source(cn.dolphinmind.glossary.java.analyze.unified.IssueSource.AI)
            .ruleKey("DESIGN_FLAW")
            .severity("MAJOR")
            .filePath("src/main/java/Test.java")
            .line(20)
            .message("Poor design")
            .confidence(0.85)
            .build());
        
        testIssues.add(UnifiedIssue.builder()
            .source(cn.dolphinmind.glossary.java.analyze.unified.IssueSource.AI)
            .ruleKey("STYLE_ISSUE")
            .severity("MINOR")
            .filePath("src/main/java/Test.java")
            .line(30)
            .message("Style issue")
            .confidence(0.4)
            .build());
        
        // 执行验证
        List<UnifiedIssue> validated = validation.validateAndCorrect(testIssues);
        Map<String, Object> stats = validation.getValidationStats(testIssues, validated);
        
        // 验证统计结果
        assertEquals(3, stats.get("originalCount"));
        assertEquals(3, stats.get("correctedCount"));
        
        System.out.println("✓ Validation stats: " + stats);
        
        // 验证低置信度问题被标记
        long filteredCount = validated.stream()
            .filter(UnifiedIssue::isAutoFiltered)
            .count();
        assertTrue(filteredCount >= 1, "应该有至少1个问题被过滤");
    }
    
    /**
     * 测试4: RagPerformanceSLA常量定义合理
     */
    @Test
    @Order(4)
    void testPerformanceSLAConstants() {
        // 验证超时阈值合理性
        assertTrue(RagPerformanceSLA.EMBEDDING_TIMEOUT_MS > 0, "Embedding超时应>0");
        assertTrue(RagPerformanceSLA.LLM_INFERENCE_TIMEOUT_MS > RagPerformanceSLA.EMBEDDING_TIMEOUT_MS, 
            "LLM超时应>Embedding超时");
        assertTrue(RagPerformanceSLA.FULL_PIPELINE_TIMEOUT_MS > RagPerformanceSLA.LLM_INFERENCE_TIMEOUT_MS,
            "完整流程超时应>LLM超时");
        
        // 验证置信度阈值合理性
        assertTrue(RagPerformanceSLA.HIGH_CONFIDENCE > RagPerformanceSLA.LOW_CONFIDENCE,
            "高置信度阈值应>低置信度阈值");
        assertTrue(RagPerformanceSLA.LOW_CONFIDENCE > RagPerformanceSLA.AUTO_FILTER_THRESHOLD,
            "低置信度阈值应>自动过滤阈值");
        
        System.out.println("✓ Performance SLA constants validated");
    }
}
