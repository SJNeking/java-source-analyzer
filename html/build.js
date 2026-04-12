#!/usr/bin/env node
/**
 * Build script using esbuild with code splitting and ESM format
 *
 * Features:
 * - Multiple entry points (one per view)
 * - Code splitting for shared chunks
 * - ESM output format for dynamic imports
 * - Tree shaking
 */

const { build } = require('esbuild');
const path = require('path');
const fs = require('fs');

const isWatch = process.argv.includes('--watch');

// Define entry points - main app + each view as separate entry
const entryPoints = [
  path.join(__dirname, 'src/app.ts'),
  path.join(__dirname, 'src/views/ForceGraphView.ts'),
  path.join(__dirname, 'src/views/QualityDashboardView.ts'),
  path.join(__dirname, 'src/views/FrontendQualityView.ts'),
  path.join(__dirname, 'src/views/MetricsDashboardView.ts'),
  path.join(__dirname, 'src/views/CodeExplorerView.ts'),
  path.join(__dirname, 'src/views/CrossFileRelationsView.ts'),
  path.join(__dirname, 'src/views/ProjectAssetsView.ts'),
  path.join(__dirname, 'src/views/ArchitectureLayerView.ts'),
  path.join(__dirname, 'src/views/MethodCallView.ts'),
  path.join(__dirname, 'src/views/CallChainView.ts'),
  path.join(__dirname, 'src/views/ClassInspectorPanel.ts'),
  path.join(__dirname, 'src/views/ComponentExplorerView.ts'),
  path.join(__dirname, 'src/views/ApiEndpointView.ts'),
];

const buildConfig = {
  entryPoints,
  bundle: true,
  splitting: true,
  format: 'esm',
  outdir: path.join(__dirname, 'dist'),
  outbase: path.join(__dirname, 'src'),
  sourcemap: true,
  minify: !isWatch,
  target: 'es2018',
  loader: {
    '.ts': 'ts',
  },
  external: ['echarts'], // ECharts loaded separately via <script>
  treeShaking: true,
  // Public path for chunk loading
  publicPath: './',
};

async function runBuild() {
  // Clean output directory
  const distDir = path.join(__dirname, 'dist');
  if (fs.existsSync(distDir)) {
    fs.rmSync(distDir, { recursive: true, force: true });
  }
  fs.mkdirSync(distDir, { recursive: true });

  if (isWatch) {
    console.log('🔍 Watch mode enabled (ESM with code splitting)');
    const ctx = await build({
      ...buildConfig,
      watch: {
        onRebuild: (error, result) => {
          if (error) {
            console.error('❌ Build failed:', error);
          } else {
            console.log('✅ Build succeeded');
          }
        },
      },
    });
    console.log('⏳ Watching for changes...');
  } else {
    console.log('🔨 Building with code splitting (ESM)...');
    await build(buildConfig);
    console.log('✅ Build successful!');
    console.log(`📦 Output: ${distDir}`);
    console.log('📝 Entry point: dist/app.js');
    console.log('💡 Use <script type="module" src="dist/app.js"> in HTML');
  }
}

runBuild().catch((error) => {
  console.error('❌ Build failed:', error);
  process.exit(1);
});
