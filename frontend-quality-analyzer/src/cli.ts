#!/usr/bin/env node

import { Command } from 'commander';
import * as fs from 'fs';
import * as path from 'path';
import { glob } from 'glob';
import chalk from 'chalk';
import { WebSocketServer } from 'ws';

import { RuleEngine } from './engine/RuleEngine';
import { ProjectAnalysis, FileAnalysis, RulesConfig, QualityIssue, Reporter, IssueCategory, Severity, FrontendMetrics } from './types';
import { JSONReporter, HTMLReporter, MarkdownReporter } from './reporters';

const program = new Command();

program
  .name('frontend-analyze')
  .description('Frontend Code Quality Analyzer — AST-based static analysis')
  .version('1.0.0')
  .requiredOption('--sourceRoot <path>', 'Project root directory to analyze')
  .option('--outputDir <path>', 'Output directory for analysis results', './analysis-output')
  .option('--format <format>', 'Output format: json, html, markdown, or all', 'all')
  .option('--websocket-port <port>', 'WebSocket port for real-time progress', '0')
  .option('--config <path>', 'Rules configuration file')
  .option('--exclude <patterns>', 'Comma-separated glob patterns to exclude', 'node_modules,dist,.git,coverage,*.test.*,*.spec.*')
  .option('--verbose', 'Enable debug logging')
  .parse(process.argv);

const options = program.opts();

// ==================== Utility Functions ====================

function log(message: string, color: string = 'white') {
  console.log(chalk[color](message));
}

function logProgress(message: string) {
  process.stdout.write(chalk.cyan(`\r  ${message}`));
}

function logDone() {
  console.log();
}

async function sendWsEvent(event: string, data: any, port: number) {
  if (port === 0) return;
  try {
    const msg = JSON.stringify({ event, data, timestamp: new Date().toISOString() });
    const net = await import('net');
    const socket = new net.Socket();
    socket.connect(port, '127.0.0.1', () => {
      socket.write(msg + '\n');
      socket.end();
    });
    socket.on('error', () => {});
  } catch {
    // WebSocket server may not be running
  }
}

// ==================== File Discovery ====================

function discoverFiles(
  sourceRoot: string,
  excludePatterns: string
): string[] {
  const extensions = ['*.ts', '*.tsx', '*.js', '*.jsx', '*.vue', '*.css', '*.scss', '*.html'];
  const excludes = excludePatterns.split(',').map(p => p.trim());

  const ignore = [
    ...excludes,
    '**/node_modules/**',
    '**/dist/**',
    '**/.git/**',
    '**/coverage/**',
    '**/build/**',
    '**/*.min.js',
    '**/*.bundle.js',
  ];

  const files: string[] = [];
  for (const ext of extensions) {
    const pattern = path.join(sourceRoot, '**', ext);
    const matches = glob.sync(pattern, { ignore, nodir: true, absolute: true });
    files.push(...matches);
  }

  return [...new Set(files)]; // Deduplicate
}

function detectLanguage(filePath: string): FileAnalysis['language'] {
  const ext = path.extname(filePath);
  const base = path.basename(filePath);

  if (base.endsWith('.vue')) return 'vue';
  if (ext === '.tsx') return 'tsx';
  if (ext === '.ts') return 'typescript';
  if (ext === '.jsx') return 'jsx';
  if (ext === '.js') return 'javascript';
  if (ext === '.scss') return 'scss';
  if (ext === '.css') return 'css';
  if (ext === '.html') return 'html';
  return 'javascript';
}

function detectFramework(filePath: string, content: string): FileAnalysis['framework'] {
  const ext = path.extname(filePath);
  const base = path.basename(filePath).toLowerCase();

  if (ext === '.vue') return 'vue';
  if (ext === '.tsx' || ext === '.jsx') {
    if (content.includes('React') || content.includes('jsx') || content.includes('useState')) return 'react';
  }
  if (ext === '.ts' || ext === '.js') {
    if (content.includes('@angular')) return 'angular';
    if (content.includes('from "svelte"')) return 'svelte';
  }

  // Check for Next.js/Nuxt indicators in file path
  if (filePath.includes('pages/') || filePath.includes('app/')) return 'nextjs';
  if (filePath.includes('pages/') && content.includes('asyncData')) return 'nuxt';

  return undefined;
}

// ==================== Metrics Calculation ====================

function calculateFileMetrics(content: string): FileAnalysis['metrics'] {
  const lines = content.split('\n');
  const lineCount = lines.length;

  // Cyclomatic complexity estimation (regex-based for speed)
  let complexity = 1;
  const complexityPatterns = [
    /if\s*\(/g,
    /\?\s*/g,
    /else\s+if\s*\(/g,
    /while\s*\(/g,
    /for\s*\(/g,
    /case\s+/g,
    /catch\s*\(/g,
    /&&/g,
    /\|\|/g,
  ];
  complexityPatterns.forEach(pat => {
    const matches = content.match(pat);
    if (matches) complexity += matches.length;
  });

  // Function count
  const functionMatches = content.match(/(?:function\s+\w+|const\s+\w+\s*=\s*(?:async\s+)?\(|=>\s*{)/g);
  const functionCount = functionMatches ? functionMatches.length : 0;

  // Component count (React/Vue)
  const componentMatches = content.match(/(?:export\s+(?:default\s+)?function|const\s+\w+\s*=\s*\([^)]*\)\s*(?::\s*\w+)?\s*=>|defineComponent|setup\s*\()/g);
  const componentCount = componentMatches ? componentMatches.length : 0;

  // Import count
  const importMatches = content.match(/^(?:import|export\s+.*from)/gm);
  const importCount = importMatches ? importMatches.length : 0;

  // `any` type usage
  const anyMatches = content.match(/\bany\b/g);
  const anyTypeUsage = anyMatches ? anyMatches.length : 0;

  return {
    cyclomatic_complexity: complexity,
    function_count: functionCount,
    component_count: componentCount,
    import_count: importCount,
    any_type_usage: anyTypeUsage,
  };
}

// ==================== Main Analysis Pipeline ====================

async function runAnalysis(): Promise<void> {
  const startTime = Date.now();
  const sourceRoot = path.resolve(options.sourceRoot);

  log(`\n🔬 Frontend Quality Analyzer v1.0.0`, 'bold');
  log(`━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n`, 'dim');

  if (!fs.existsSync(sourceRoot)) {
    log(`❌ Error: Source directory not found: ${sourceRoot}`, 'red');
    process.exit(1);
  }

  // Load config
  let config: RulesConfig | undefined;
  if (options.config) {
    const configPath = path.resolve(options.config);
    if (fs.existsSync(configPath)) {
      config = JSON.parse(fs.readFileSync(configPath, 'utf-8'));
      log(`📋 Loaded config: ${configPath}`, 'green');
    } else {
      log(`⚠️  Config not found, using defaults`, 'yellow');
    }
  }

  // Discover files
  log('📂 Discovering files...', 'cyan');
  const files = discoverFiles(sourceRoot, options.exclude);
  log(`  Found ${files.length} analyzable files`, 'green');

  if (files.length === 0) {
    log('❌ No files found to analyze', 'red');
    process.exit(1);
  }

  // Initialize Rule Engine (rules auto-register in constructor)
  log('⚙️  Initializing rule engine...', 'cyan');
  const engine = new RuleEngine();
  log(`  ${engine.getRules().length} rules registered`, 'green');

  // Setup WebSocket server for real-time progress
  const wsPort = parseInt(options.websocketPort, 10);
  let wss: WebSocketServer | null = null;
  if (wsPort > 0) {
    wss = new WebSocketServer({ port: wsPort });
    wss.on('connection', (ws) => {
      log(`  WebSocket server started on port ${wsPort}`, 'green');
      ws.on('close', () => wss?.close());
    });
  }

  // Analyze each file
  log('\n🔍 Analyzing files...\n', 'cyan');

  const fileAnalyses: FileAnalysis[] = [];
  const allIssues: QualityIssue[] = [];

  for (let i = 0; i < files.length; i++) {
    const filePath = files[i];
    const relativePath = path.relative(sourceRoot, filePath);

    logProgress(`${i + 1}/${files.length} ${relativePath}`);

    try {
      const content = fs.readFileSync(filePath, 'utf-8');
      const language = detectLanguage(filePath);
      const framework = detectFramework(filePath, content);
      const metrics = calculateFileMetrics(content);

      // Run rule engine
      const fileOptions = {
        framework,
        language,
        config,
      };

      const fileIssues = engine.runSingle(content, relativePath, fileOptions);

      const fileAnalysis: FileAnalysis = {
        file_path: relativePath,
        file_size: Buffer.byteLength(content),
        line_count: content.split('\n').length,
        language,
        framework,
        issues: fileIssues,
        metrics,
      };

      fileAnalyses.push(fileAnalysis);
      allIssues.push(...fileIssues);

      // Send progress via WebSocket
      if (wsPort > 0) {
        await sendWsEvent('progress', {
          current: i + 1,
          total: files.length,
          file: relativePath,
          issues: fileIssues.length,
        }, wsPort);
      }
    } catch (err) {
      if (options.verbose) {
        log(`\n  ⚠️  Error analyzing ${relativePath}: ${err}`, 'yellow');
      }
    }
  }

  logDone();

  // Build project analysis result
  const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);
  const projectAnalysis = buildProjectAnalysis(
    sourceRoot,
    fileAnalyses,
    allIssues,
    engine,
    elapsed,
    config
  );

  // Ensure output directory exists
  const outputDir = path.resolve(options.outputDir);
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }

  // Generate reports
  log('\n📝 Generating reports...', 'cyan');
  generateReports(projectAnalysis, outputDir);

  // Print summary
  printSummary(projectAnalysis, elapsed);

  // Close WebSocket
  if (wss) {
    await sendWsEvent('complete', { total_issues: allIssues.length }, wsPort);
    wss.close();
  }
}

function buildProjectAnalysis(
  sourceRoot: string,
  fileAnalyses: FileAnalysis[],
  allIssues: QualityIssue[],
  engine: RuleEngine,
  elapsed: string,
  config?: RulesConfig
): ProjectAnalysis {
  const projectName = path.basename(sourceRoot);

  // Count by severity
  const by_severity = {
    CRITICAL: allIssues.filter(i => i.severity === 'CRITICAL').length,
    MAJOR: allIssues.filter(i => i.severity === 'MAJOR').length,
    MINOR: allIssues.filter(i => i.severity === 'MINOR').length,
    INFO: allIssues.filter(i => i.severity === 'INFO').length,
  };

  // Count by category
  const by_category: Partial<Record<IssueCategory, number>> = {};
  allIssues.forEach(issue => {
    by_category[issue.category] = (by_category[issue.category] || 0) + 1;
  });

  // Calculate aggregated metrics
  const totalLines = fileAnalyses.reduce((sum, f) => sum + f.line_count, 0);
  const tsFiles = fileAnalyses.filter(f => ['typescript', 'tsx'].includes(f.language));
  const anyUsage = tsFiles.reduce((sum, f) => sum + (f.metrics?.any_type_usage || 0), 0);
  const componentSizes = fileAnalyses
    .filter(f => f.metrics && f.metrics.component_count > 0)
    .map(f => f.line_count);

  const metrics = {
    typescript: {
      coverage_percentage: tsFiles.length > 0
        ? (tsFiles.reduce((sum, f) => sum + f.line_count, 0) / totalLines * 100)
        : 0,
      any_usage_count: anyUsage,
      implicit_any_count: 0, // Filled by AST parser
      explicit_any_count: anyUsage,
      type_safety_score: Math.max(0, 100 - anyUsage * 5),
    },
    performance: {
      estimated_bundle_size_kb: Math.round(totalLines * 0.05), // Rough estimate
      lazy_loading_coverage: 0,
      memoization_ratio: 0,
      image_optimization_score: 0,
    },
    memory: {
      potential_leak_sites: allIssues.filter(i =>
        i.message.toLowerCase().includes('leak') ||
        i.message.toLowerCase().includes('cleanup')
      ).length,
      event_listener_cleanup_ratio: 0,
      timer_cleanup_ratio: 0,
      observer_cleanup_ratio: 0,
    },
    accessibility: {
      wcag_aa_compliance_score: 0,
      aria_attribute_coverage: 0,
      keyboard_navigation_score: 0,
      color_contrast_violations: 0,
      missing_alt_texts: 0,
    },
    architecture: {
      component_count: fileAnalyses.reduce((sum, f) => sum + (f.metrics?.component_count || 0), 0),
      average_component_size_lines: componentSizes.length > 0
        ? Math.round(componentSizes.reduce((a, b) => a + b, 0) / componentSizes.length)
        : 0,
      max_component_size_lines: componentSizes.length > 0 ? Math.max(...componentSizes) : 0,
      circular_dependencies: 0,
      coupling_score: 0,
      cohesion_score: 0,
    },
    testing: {
      test_file_count: fileAnalyses.filter(f =>
        f.file_path.includes('.test.') || f.file_path.includes('.spec.')
      ).length,
      test_coverage_estimate: 0,
      assertion_density: 0,
    },
    maintainability: {
      average_function_length: 0,
      max_function_length: 0,
      comment_ratio: calculateCommentRatio(fileAnalyses),
      duplicate_code_blocks: 0,
      code_smell_count: allIssues.filter(i => i.category === 'TYPESCRIPT').length,
    },
  };

  // Quality Gate evaluation
  const metricsForGate: FrontendMetrics = metrics;
  const quality_gate = engine.evaluateQualityGate(allIssues, metricsForGate, config);

  // Technical debt estimation
  const technical_debt = engine.estimateTechnicalDebt(allIssues);

  return {
    project_name: projectName,
    scan_date: new Date().toISOString().replace('T', ' ').substring(0, 19),
    framework_detected: detectProjectFramework(fileAnalyses),
    total_files: fileAnalyses.length,
    total_lines: totalLines,
    total_issues: allIssues.length,
    by_severity,
    by_category,
    files: fileAnalyses,
    metrics,
    quality_gate,
    technical_debt,
  };
}

function calculateCommentRatio(fileAnalyses: FileAnalysis[]): number {
  let totalLines = 0;
  let commentLines = 0;

  fileAnalyses.forEach(f => {
    if (f.file_path.endsWith('.js') || f.file_path.endsWith('.jsx') ||
        f.file_path.endsWith('.ts') || f.file_path.endsWith('.tsx')) {
      totalLines += f.line_count;
      // Rough estimate from issues — would need actual file content for precision
    }
  });

  return totalLines > 0 ? Math.round((commentLines / totalLines) * 1000) / 10 : 0;
}

function detectProjectFramework(fileAnalyses: FileAnalysis[]): string | undefined {
  const frameworks = fileAnalyses.map(f => f.framework).filter(Boolean);
  const counts: Record<string, number> = {};
  frameworks.forEach(f => { counts[f!] = (counts[f!] || 0) + 1; });

  const sorted = Object.entries(counts).sort((a, b) => b[1] - a[1]);
  return sorted.length > 0 ? sorted[0][0] : undefined;
}

// ==================== Report Generation ====================

function generateReports(analysis: ProjectAnalysis, outputDir: string): void {
  const format = options.format.toLowerCase();
  const projectName = analysis.project_name;
  const timestamp = new Date().toISOString().replace(/[-:T]/g, '').substring(0, 14);

  const reporters: Record<string, Reporter> = {
    json: new JSONReporter(),
    html: new HTMLReporter(),
    markdown: new MarkdownReporter(),
  };

  const formatsToGenerate = format === 'all'
    ? ['json', 'html', 'markdown']
    : [format];

  formatsToGenerate.forEach(fmt => {
    const reporter = reporters[fmt];
    if (!reporter) {
      log(`  ⚠️  Unknown format: ${fmt}`, 'yellow');
      return;
    }

    const content = reporter.generate(analysis);
    const ext = fmt === 'html' ? 'html' : fmt === 'markdown' ? 'md' : 'json';

    // Full report
    const fullFile = path.join(outputDir, `${projectName}_v1.0_full_${timestamp}.${ext}`);
    fs.writeFileSync(fullFile, content, 'utf-8');
    log(`  ✅ ${fmt.toUpperCase()}: ${fullFile}`, 'green');

    // Summary (JSON only)
    if (fmt === 'json') {
      const summary = {
        project_name: analysis.project_name,
        scan_date: analysis.scan_date,
        total_files: analysis.total_files,
        total_lines: analysis.total_lines,
        total_issues: analysis.total_issues,
        by_severity: analysis.by_severity,
        by_category: analysis.by_category,
        quality_gate: analysis.quality_gate,
        technical_debt: analysis.technical_debt,
      };
      const summaryFile = path.join(outputDir, `${projectName}_v1.0_summary_${timestamp}.json`);
      fs.writeFileSync(summaryFile, JSON.stringify(summary, null, 2), 'utf-8');
      log(`  ✅ SUMMARY JSON: ${summaryFile}`, 'green');
    }
  });
}

// ==================== Console Summary ====================

function printSummary(analysis: ProjectAnalysis, elapsed: string): void {
  const { total_files, total_lines, total_issues, by_severity } = analysis;

  log(`\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`, 'dim');
  log(`\n✅ Analysis Complete! (${elapsed}s)\n`, 'bold');

  log(`  Files:    ${total_files}`, 'white');
  log(`  Lines:    ${total_lines.toLocaleString()}`, 'white');
  log(`  Issues:   ${total_issues}`, total_issues > 0 ? 'red' : 'green');
  log(``, 'white');
  log(`  🔴 Critical: ${by_severity.CRITICAL}`, by_severity.CRITICAL > 0 ? 'red' : 'dim');
  log(`  🟠 Major:    ${by_severity.MAJOR}`, by_severity.MAJOR > 0 ? 'yellow' : 'dim');
  log(`  🟡 Minor:    ${by_severity.MINOR}`, 'dim');
  log(`  🔵 Info:     ${by_severity.INFO}`, 'dim');

  if (analysis.quality_gate) {
    log(``, 'white');
    if (analysis.quality_gate.passed) {
      log(`  ✅ Quality Gate: PASSED`, 'green');
    } else {
      log(`  ❌ Quality Gate: FAILED`, 'red');
      analysis.quality_gate.reasons.forEach(r => {
        log(`     - ${r}`, 'red');
      });
    }
  }

  if (analysis.technical_debt) {
    log(``, 'white');
    log(`  ⏱️  Technical Debt: ${analysis.technical_debt.total_remediation_hours}h`, 'yellow');
  }

  log(`\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n`, 'dim');
}

// ==================== Run ====================

runAnalysis().catch(err => {
  log(`\n❌ Fatal error: ${err.message}`, 'red');
  if (options.verbose) {
    console.error(err.stack);
  }
  process.exit(1);
});
