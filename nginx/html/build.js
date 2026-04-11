#!/usr/bin/env node
/**
 * Build script using esbuild to bundle TypeScript
 */

const { build } = require('esbuild');
const path = require('path');

const isWatch = process.argv.includes('--watch');

const buildConfig = {
  entryPoints: [path.join(__dirname, 'src/app.ts')],
  bundle: true,
  outfile: path.join(__dirname, 'dist/app.js'),
  sourcemap: true,
  minify: !isWatch,
  target: 'es2018',
  loader: {
    '.ts': 'ts'
  },
  globalName: 'app'
};

if (isWatch) {
  console.log('🔍 Watch mode enabled');
  build({
    ...buildConfig,
    watch: {
      onRebuild: (error, result) => {
        if (error) {
          console.error('❌ Build failed:', error);
        } else {
          console.log('✅ Build succeeded');
        }
      }
    }
  }).then(() => {
    console.log('⏳ Watching for changes...');
  });
} else {
  console.log('🔨 Building...');
  build(buildConfig).then(() => {
    console.log('✅ Build successful!');
  }).catch((error) => {
    console.error('❌ Build failed:', error);
    process.exit(1);
  });
}
