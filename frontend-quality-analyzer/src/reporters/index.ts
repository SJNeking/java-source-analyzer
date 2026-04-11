import { ProjectAnalysis, Reporter, Severity, IssueCategory, QualityIssue } from '../types';

// ==================== JSON Reporter ====================

export class JSONReporter implements Reporter {
  generate(analysis: ProjectAnalysis): string {
    return JSON.stringify(analysis, null, 2);
  }

  getFormat(): string {
    return 'json';
  }
}

// ==================== HTML Reporter ====================

export class HTMLReporter implements Reporter {
  generate(analysis: ProjectAnalysis): string {
    const { by_severity, by_category, total_issues, total_files, total_lines } = analysis;
    const files = analysis.files || [];
    const allIssues = files.flatMap(f => f.issues || []);

    return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Frontend Quality Report - ${analysis.project_name}</title>
<style>
  :root {
    --critical: #dc3545; --major: #fd7e14; --minor: #ffc107; --info: #17a2b8;
    --bg: #f8f9fa; --card-bg: #fff; --text: #333; --border: #dee2e6;
    --success: #28a745; --dark: #343a40;
  }
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: var(--bg); color: var(--text); line-height: 1.6; }
  .container { max-width: 1400px; margin: 0 auto; padding: 20px; }
  header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; padding: 30px 0; margin-bottom: 30px; }
  header .container { display: flex; justify-content: space-between; align-items: center; }
  h1 { font-size: 2rem; }
  .subtitle { opacity: 0.9; font-size: 0.95rem; }
  .badge { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 0.75rem; font-weight: 600; color: #fff; }
  .badge-critical { background: var(--critical); }
  .badge-major { background: var(--major); }
  .badge-minor { background: var(--minor); color: #333; }
  .badge-info { background: var(--info); }
  .badge-success { background: var(--success); }
  .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px; }
  .card { background: var(--card-bg); border-radius: 10px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); text-align: center; }
  .card h3 { font-size: 2.5rem; margin-bottom: 5px; }
  .card p { color: #666; font-size: 0.9rem; }
  .card.critical h3 { color: var(--critical); }
  .card.major h3 { color: var(--major); }
  .card.minor h3 { color: var(--minor); }
  .card.info h3 { color: var(--info); }
  .section { background: var(--card-bg); border-radius: 10px; padding: 25px; margin-bottom: 30px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
  .section h2 { margin-bottom: 20px; padding-bottom: 10px; border-bottom: 2px solid var(--border); }
  table { width: 100%; border-collapse: collapse; }
  th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid var(--border); }
  th { background: #f8f9fa; font-weight: 600; position: sticky; top: 0; }
  tr:hover { background: #f1f3f5; }
  .filter-bar { display: flex; gap: 10px; margin-bottom: 15px; flex-wrap: wrap; }
  .filter-btn { padding: 6px 14px; border: 1px solid var(--border); border-radius: 6px; background: #fff; cursor: pointer; font-size: 0.85rem; transition: all 0.2s; }
  .filter-btn:hover, .filter-btn.active { background: #667eea; color: #fff; border-color: #667eea; }
  .file-tree { max-height: 400px; overflow-y: auto; }
  .file-item { padding: 10px 15px; border-left: 3px solid var(--border); margin-bottom: 5px; cursor: pointer; transition: all 0.2s; }
  .file-item:hover { border-left-color: #667eea; background: #f8f9fa; }
  .file-item .file-name { font-weight: 500; }
  .file-item .issue-count { font-size: 0.8rem; color: #666; }
  .progress-bar { height: 8px; background: #e9ecef; border-radius: 4px; overflow: hidden; margin-top: 8px; }
  .progress-fill { height: 100%; background: linear-gradient(90deg, #28a745, #20c997); }
  .code-snippet { background: #f8f9fa; padding: 10px; border-radius: 5px; font-family: 'Courier New', monospace; font-size: 0.85rem; margin-top: 5px; white-space: pre-wrap; overflow-x: auto; }
  footer { text-align: center; padding: 20px; color: #666; font-size: 0.85rem; }
  .gate-pass { color: var(--success); font-size: 1.2rem; font-weight: 600; }
  .gate-fail { color: var(--critical); font-size: 1.2rem; font-weight: 600; }
</style>
</head>
<body>
<header>
  <div class="container">
    <div>
      <h1>🔬 Frontend Quality Report</h1>
      <p class="subtitle">${analysis.project_name} | Scanned: ${analysis.scan_date}</p>
    </div>
    <div>
      ${analysis.quality_gate
        ? analysis.quality_gate.passed
          ? '<span class="gate-pass">✅ Quality Gate PASSED</span>'
          : '<span class="gate-fail">❌ Quality Gate FAILED</span>'
        : ''}
    </div>
  </div>
</header>

<div class="container">
  <!-- Summary Cards -->
  <div class="cards">
    <div class="card">
      <h3>${total_files}</h3>
      <p>Files Analyzed</p>
    </div>
    <div class="card">
      <h3>${total_lines.toLocaleString()}</h3>
      <p>Total Lines</p>
    </div>
    <div class="card critical">
      <h3>${total_issues}</h3>
      <p>Total Issues</p>
    </div>
    <div class="card critical">
      <h3>${by_severity.CRITICAL}</h3>
      <p><span class="badge badge-critical">CRITICAL</span></p>
    </div>
    <div class="card major">
      <h3>${by_severity.MAJOR}</h3>
      <p><span class="badge badge-major">MAJOR</span></p>
    </div>
    <div class="card minor">
      <h3>${by_severity.MINOR}</h3>
      <p><span class="badge badge-minor">MINOR</span></p>
    </div>
    <div class="card info">
      <h3>${by_severity.INFO}</h3>
      <p><span class="badge badge-info">INFO</span></p>
    </div>
  </div>

  <!-- Category Breakdown -->
  <div class="section">
    <h2>📊 Issues by Category</h2>
    <table>
      <thead><tr><th>Category</th><th>Count</th><th>Percentage</th></tr></thead>
      <tbody>
        ${Object.entries(by_category).map(([cat, count]) => `
          <tr>
            <td><strong>${cat}</strong></td>
            <td>${count}</td>
            <td>
              <div class="progress-bar">
                <div class="progress-fill" style="width: ${(count / total_issues * 100).toFixed(1)}%"></div>
              </div>
              ${(count / total_issues * 100).toFixed(1)}%
            </td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  </div>

  <!-- File Tree -->
  <div class="section">
    <h2>📁 Files with Issues</h2>
    <div class="file-tree">
      ${files.filter(f => f.issues.length > 0).map(f => `
        <div class="file-item" onclick="document.getElementById('file-${f.file_path.replace(/[^a-zA-Z0-9]/g, '-')}').scrollIntoView({behavior: 'smooth'})">
          <span class="file-name">${f.file_path}</span>
          <span class="issue-count">${f.issues.length} issue(s)</span>
        </div>
      `).join('')}
    </div>
  </div>

  <!-- Detailed Issues -->
  <div class="section">
    <h2>⚠️ All Issues</h2>
    <div class="filter-bar">
      <button class="filter-btn active" onclick="filterIssues('all')">All</button>
      <button class="filter-btn" onclick="filterIssues('CRITICAL')">🔴 Critical</button>
      <button class="filter-btn" onclick="filterIssues('MAJOR')">🟠 Major</button>
      <button class="filter-btn" onclick="filterIssues('MINOR')">🟡 Minor</button>
      <button class="filter-btn" onclick="filterIssues('INFO')">🔵 Info</button>
    </div>
    <table>
      <thead>
        <tr><th>Severity</th><th>Rule</th><th>File</th><th>Line</th><th>Message</th><th>Remediation</th></tr>
      </thead>
      <tbody id="issues-table">
        ${allIssues.map(issue => `
          <tr data-severity="${issue.severity}">
            <td><span class="badge badge-${issue.severity.toLowerCase()}">${issue.severity}</span></td>
            <td><code>${issue.rule_key}</code></td>
            <td>${issue.file_path}</td>
            <td>${issue.line}</td>
            <td>${issue.message}</td>
            <td>${issue.remediation || '-'}</td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  </div>

  <!-- Technical Debt -->
  ${analysis.technical_debt ? `
  <div class="section">
    <h2>⏱️ Technical Debt Estimate</h2>
    <div class="cards">
      <div class="card critical">
        <h3>${analysis.technical_debt.total_remediation_hours}h</h3>
        <p>Total Remediation Time</p>
      </div>
      <div class="card">
        <h3>${analysis.technical_debt.debt_ratio_percentage.toFixed(1)}%</h3>
        <p>Debt Ratio</p>
      </div>
    </div>
  </div>
  ` : ''}
</div>

<footer>
  <p>Generated by Frontend Quality Analyzer | ${new Date().toISOString()}</p>
</footer>

<script>
function filterIssues(severity) {
  const rows = document.querySelectorAll('#issues-table tr');
  rows.forEach(row => {
    if (severity === 'all' || row.dataset.severity === severity) {
      row.style.display = '';
    } else {
      row.style.display = 'none';
    }
  });
  document.querySelectorAll('.filter-btn').forEach(btn => btn.classList.remove('active'));
  event.target.classList.add('active');
}
</script>
</body>
</html>`;
  }

  getFormat(): string {
    return 'html';
  }
}

// ==================== Markdown Reporter ====================

export class MarkdownReporter implements Reporter {
  generate(analysis: ProjectAnalysis): string {
    const { project_name, scan_date, total_files, total_lines, total_issues, by_severity, by_category, files } = analysis;
    const allIssues = files.flatMap(f => f.issues || []);

    let md = `# 🔬 Frontend Quality Report: ${project_name}\n\n`;
    md += `**Scan Date:** ${scan_date}  \n`;
    md += `**Files Analyzed:** ${total_files}  \n`;
    md += `**Total Lines:** ${total_lines.toLocaleString()}  \n`;
    md += `**Total Issues:** ${total_issues}\n\n`;

    // Quality Gate
    if (analysis.quality_gate) {
      const gate = analysis.quality_gate;
      md += `## Quality Gate: ${gate.passed ? '✅ PASSED' : '❌ FAILED'}\n\n`;
      if (!gate.passed) {
        md += `**Reasons:**\n${gate.reasons.map(r => `- ${r}`).join('\n')}\n\n`;
      }
    }

    // Summary by Severity
    md += `## 📊 Issues by Severity\n\n`;
    md += `| Severity | Count |\n|----------|-------|\n`;
    (['CRITICAL', 'MAJOR', 'MINOR', 'INFO'] as Severity[]).forEach(s => {
      md += `| ${s} | ${by_severity[s]} |\n`;
    });
    md += '\n';

    // Summary by Category
    md += `## 📊 Issues by Category\n\n`;
    md += `| Category | Count |\n|----------|-------|\n`;
    Object.entries(by_category).forEach(([cat, count]) => {
      md += `| ${cat} | ${count} |\n`;
    });
    md += '\n';

    // Technical Debt
    if (analysis.technical_debt) {
      const debt = analysis.technical_debt;
      md += `## ⏱️ Technical Debt Estimate\n\n`;
      md += `- **Total Remediation Time:** ${debt.total_remediation_hours} hours\n`;
      md += `- **Debt Ratio:** ${debt.debt_ratio_percentage.toFixed(1)}%\n\n`;
    }

    // Top Issues (Critical + Major)
    const topIssues = allIssues.filter(i => i.severity === 'CRITICAL' || i.severity === 'MAJOR').slice(0, 20);
    if (topIssues.length > 0) {
      md += `## 🚨 Top Issues (Critical + Major)\n\n`;
      md += `| # | Severity | Rule | File | Line | Message |\n|---|----------|------|------|------|--------|\n`;
      topIssues.forEach((issue, idx) => {
        md += `| ${idx + 1} | ${issue.severity} | \`${issue.rule_key}\` | ${issue.file_path} | ${issue.line} | ${issue.message} |\n`;
      });
      md += '\n';
    }

    // All Issues by File
    md += `## 📁 Issues by File\n\n`;
    files.filter(f => f.issues.length > 0).forEach(f => {
      md += `### ${f.file_path} (${f.issues.length} issues)\n\n`;
      f.issues.forEach(issue => {
        md += `- **[${issue.severity}]** \`${issue.rule_key}\` (Line ${issue.line}): ${issue.message}`;
        if (issue.remediation) md += ` — *Fix: ${issue.remediation}*`;
        md += '\n';
      });
      md += '\n';
    });

    md += `\n---\n*Generated by Frontend Quality Analyzer | ${new Date().toISOString()}*\n`;

    return md;
  }

  getFormat(): string {
    return 'markdown';
  }
}
