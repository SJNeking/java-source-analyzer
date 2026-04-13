package cn.dolphinmind.glossary.java.analyze.web;

import cn.dolphinmind.glossary.java.analyze.domain.AnalysisTask;
import cn.dolphinmind.glossary.java.analyze.mapper.AnalysisTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分析任务 REST Controller
 * 
 * 提供对外的 RESTful API，替代原来的 CLI main 方法调用。
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class AnalysisTaskController {

    @Autowired
    private AnalysisTaskMapper taskMapper;

    /**
     * 1. 获取项目历史任务
     * GET /api/v1/tasks/history?projectName=xxx&limit=10
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(
            @RequestParam(defaultValue = "") String projectName,
            @RequestParam(defaultValue = "20") int limit) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            List<AnalysisTask> tasks = taskMapper.selectByProjectName(projectName, limit);
            result.put("success", true);
            result.put("data", tasks);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 2. 提交新任务
     * POST /api/v1/tasks/submit
     * Body: { "projectName": "...", "sourceRoot": "..." }
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitTask(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String projectName = request.get("projectName");
        
        if (projectName == null || projectName.isEmpty()) {
            result.put("success", false);
            result.put("message", "projectName is required");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            // 1. 初始化 Task 记录
            AnalysisTask task = new AnalysisTask();
            task.setProjectName(projectName);
            task.setSource("unified"); // 默认
            task.setAnalysisMs(0L);
            // TODO: 2. 调用分析引擎 (AnalysisOrchestrator)
            // TODO: 3. 保存报告到 MinIO 并更新 raw_json_url
            
            taskMapper.insert(task);

            result.put("success", true);
            result.put("taskId", task.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Task submission failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
