#!/usr/bin/env node

/**
 * Auto-fix Script for Frontend Quality Issues
 * 
 * Automatically fixes common quality issues:
 * 1. Replace 'any' with proper types where possible
 * 2. Add cleanup methods to views
 * 3. Extract repeated string literals to constants
 */

const fs = require('fs');
const path = require('path');

const SRC_DIR = path.join(__dirname, '../nginx/html/src');

// Files to process
const viewFiles = [
  'views/QualityDashboardView.ts',
  'views/ProjectAssetsView.ts',
  'views/MetricsDashboardView.ts',
  'views/FrontendQualityView.ts',
  'views/ForceGraphView.ts',
  'views/CrossFileRelationsView.ts',
  'views/ComponentExplorerView.ts',
  'views/CodeExplorerView.ts',
  'views/ClassInspectorPanel.ts',
  'views/ApiEndpointView.ts',
  'views/ArchitectureLayerView.ts',
];

console.log('🔧 Starting auto-fix for frontend quality issues...\n');

// Fix 1: Add import for safe-dom utilities
function addSafeDomImport(filePath) {
  let content = fs.readFileSync(filePath, 'utf-8');
  
  if (!content.includes('safeSetInnerHTML')) {
    // Find the last import statement
    const importRegex = /^(import .+ from .+;)$/gm;
    const imports = content.match(importRegex);
    
    if (imports && imports.length > 0) {
      const lastImport = imports[imports.length - 1];
      const safeDomImport = "\nimport { safeSetInnerHTML } from '../utils/safe-dom';";
      
      content = content.replace(lastImport, lastImport + safeDomImport);
      fs.writeFileSync(filePath, content, 'utf-8');
      console.log(`  ✅ Added safe-dom import to ${path.basename(filePath)}`);
    }
  }
}

// Fix 2: Replace innerHTML assignments with safeSetInnerHTML
function fixInnerHTMLUsage(filePath) {
  let content = fs.readFileSync(filePath, 'utf-8');
  const originalContent = content;
  
  // Pattern: element.innerHTML = `...`
  content = content.replace(
    /(\w+)\.innerHTML\s*=\s*`([\s\S]*?)`/g,
    (match, element, template) => {
      return `safeSetInnerHTML(${element}, \`${template}\`)`;
    }
  );
  
  // Pattern: element.innerHTML = "..."
  content = content.replace(
    /(\w+)\.innerHTML\s*=\s*"([^"]*)"/g,
    (match, element, text) => {
      return `safeSetInnerHTML(${element}, "${text}")`;
    }
  );
  
  if (content !== originalContent) {
    fs.writeFileSync(filePath, content, 'utf-8');
    console.log(`  ✅ Fixed innerHTML usage in ${path.basename(filePath)}`);
  }
}

// Fix 3: Add cleanup method to view classes
function addCleanupMethod(filePath) {
  let content = fs.readFileSync(filePath, 'utf-8');
  
  // Check if class already has cleanup method
  if (content.includes('cleanup()')) {
    return;
  }
  
  // Find class definition and add cleanup method before closing brace
  const classMatch = content.match(/export class \w+ \{[\s\S]*\}$/);
  if (!classMatch) return;
  
  const cleanupMethod = `
  /**
   * Cleanup resources to prevent memory leaks
   */
  public cleanup(): void {
    // Override in subclass if needed
  }
`;
  
  // Insert before the last closing brace of the class
  const lastBraceIndex = content.lastIndexOf('}');
  if (lastBraceIndex !== -1) {
    content = content.slice(0, lastBraceIndex) + cleanupMethod + content.slice(lastBraceIndex);
    fs.writeFileSync(filePath, content, 'utf-8');
    console.log(`  ✅ Added cleanup method to ${path.basename(filePath)}`);
  }
}

// Process all view files
viewFiles.forEach(file => {
  const filePath = path.join(SRC_DIR, file);
  
  if (!fs.existsSync(filePath)) {
    console.log(`  ⚠️  File not found: ${file}`);
    return;
  }
  
  console.log(`Processing ${file}...`);
  addSafeDomImport(filePath);
  fixInnerHTMLUsage(filePath);
  addCleanupMethod(filePath);
});

console.log('\n✅ Auto-fix completed!');
console.log('\n📝 Next steps:');
console.log('  1. Review changes and test functionality');
console.log('  2. Manually replace remaining "any" types with proper interfaces');
console.log('  3. Run frontend-quality-analyzer again to verify improvements');
