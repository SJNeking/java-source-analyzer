package cn.dolphinmind.glossary.java.analyze;

import com.github.javaparser.StaticJavaParser;

/**
 * MVP v0.4：命令行参数与异常友好增强版
 */
public class SourceUniverseRefactoredDemo {
    public static void main(String[] args) throws Exception {
        CLI cli = CLI.fromArgs(args);
        ScanConfig config = ScanConfig.defaultConfig(cli.sourceRoot, cli.outputDir, cli.artifactName);

        System.out.println("====== SourceUniverse MVP v0.4 启动 ======");
        System.out.println("源码路径: " + config.sourceRoot);
        System.out.println("输出目录: " + config.outputDir);
        System.out.println("产物名: " + config.artifactName);
        System.out.println("======================================");

        StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_8);

        ProjectScanOrchestrator orchestrator = new ProjectScanOrchestrator(
                new JavaSourceScanner(),
                new TypeAssetExtractor(),
                new JsonFileExporter()
        );
        try {
            ScanResult result = orchestrator.execute(config);
            System.out.println("✅ 扫描完成，输出在: " + config.buildVersionedOutputFile());
            System.out.println("🔗 依赖数: " + result.summary.dependencyCount + "  类数: " + result.summary.classCount);
        } catch (Exception e) {
            System.err.println("❌ 扫描失败: " + e.getMessage());
        }
    }

    // ========== CLI 解析 ==========
    static class CLI {
        String sourceRoot;
        String outputDir;
        String artifactName;
        static CLI fromArgs(String[] args) {
            CLI cli = new CLI();
            cli.sourceRoot = getArg(args, "--sourceRoot", "/Users/mingxilv/learn/s-pay-mall-ddd/glossary-redisson/src/main/java");
            cli.outputDir = getArg(args, "--outputDir", "/Users/mingxilv/learn/s-pay-mall-ddd/glossary-redisson/dev-ops/output");
            cli.artifactName = getArg(args, "--artifactName", "source-universe-refactored-demo");
            return cli;
        }
        static String getArg(String[] args, String key, String def) {
            for (int i = 0; i < args.length - 1; i++) if (args[i].equalsIgnoreCase(key)) return args[i + 1];
            return def;
        }
    }

    // 其余类（ScanConfig, ProjectScanOrchestrator, JavaSourceScanner, TypeAssetExtractor, JsonFileExporter, DTOs）
    // 保持不变，略。
    
    // ===============================
    // === 复制 v0.3 的其余实现即可 ===
    // ===============================

    // ...（此处省略，实际代码中请完整包含v0.3所有内容，仅main和CLI部分作如上增强）
}
