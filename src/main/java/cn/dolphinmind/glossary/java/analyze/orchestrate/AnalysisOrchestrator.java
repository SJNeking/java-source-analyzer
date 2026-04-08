package cn.dolphinmind.glossary.java.analyze.orchestrate;

import cn.dolphinmind.glossary.java.analyze.SourceUniversePro;
import cn.dolphinmind.glossary.java.analyze.translate.SemanticTranslator;

/**
 * Orchestrates the full analysis pipeline.
 *
 * Extracted from SourceUniversePro.main() to:
 * - Eliminate hardcoded paths and magic values
 * - Separate configuration from execution
 * - Enable testing and reuse
 */
public class AnalysisOrchestrator {

    private final AnalysisConfig config;
    private final SemanticTranslator translator;

    public AnalysisOrchestrator(AnalysisConfig config, SemanticTranslator translator) {
        this.config = config;
        this.translator = translator;
    }

    /**
     * Execute the full analysis pipeline via SourceUniversePro.
     */
    public void execute() throws Exception {
        config.applyDefaults();
        SourceUniversePro.runAnalysis(config, translator);
    }

    public AnalysisConfig getConfig() { return config; }
    public SemanticTranslator getTranslator() { return translator; }
}
