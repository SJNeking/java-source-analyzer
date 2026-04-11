# Quick Reference - TypeScript Frontend

## 📋 Commands

### Build & Run
```bash
# Build TypeScript
npm run build

# Watch mode (auto-rebuild)
npm run watch

# Clean build artifacts
npm run clean

# Start dev server
./dev.sh

# Start production server (from project root)
../../start.sh
```

### Install
```bash
# Install dependencies
npm install

# Update dependencies
npm update
```

## 📁 File Locations

| Purpose | Location |
|---------|----------|
| TypeScript sources | `src/` |
| Main app entry | `src/app.ts` |
| Type definitions | `src/types/index.ts` |
| Configuration | `src/config/index.ts` |
| Compiled JavaScript | `dist/` |
| HTML entry | `views/index.html` |
| Styles | `css/styles.css` |
| JSON data | `data/*.json` |
| Third-party libs | `js/lib/` |

## 🔧 Common Tasks

### Add New Data
```bash
# Copy analysis JSON to data folder
cp /path/to/analysis/*.json data/

# Create/update project index
cat > data/projects.json << EOF
{
  "frameworks": [
    {
      "file": "project_v1.0_full_20260410.json",
      "name": "Project v1.0"
    }
  ]
}
EOF
```

### Modify Configuration
Edit `src/config/index.ts`:
```typescript
export const CONFIG: AppConfig = {
  dataPath: '/data/',
  projectsIndexUrl: '/data/projects.json',
  largeDatasetThreshold: 1000,
  colorMap: { /* ... */ }
};
```

### Add New Utility
1. Create `src/utils/my-util.ts`
2. Export functions
3. Add to `src/utils/index.ts`
4. Import where needed

### Add New Type
Edit `src/types/index.ts`:
```typescript
export interface MyNewType {
  field1: string;
  field2: number;
}
```

## 🐛 Debugging

### Check Build
```bash
# Verify compilation
npm run build

# Check for type errors
npx tsc --noEmit
```

### View Compiled Output
```bash
# See generated JavaScript
cat dist/app.js

# See source maps
cat dist/app.js.map
```

### Browser Debug
1. Open DevTools (F12)
2. Go to Sources tab
3. Find `app.ts` in source tree (via source maps)
4. Set breakpoints in TypeScript!

## 📊 Data Structures

### AnalysisResult (from Java)
```typescript
{
  framework: string,
  version: string,
  assets: Asset[],
  dependencies: Dependency[],
  quality_summary: QualitySummary,
  comment_coverage: CommentCoverage
}
```

### GraphData (for visualization)
```typescript
{
  framework: string,
  version: string,
  nodes: GraphNode[],
  links: GraphLink[]
}
```

### GraphNode
```typescript
{
  id: string,           // Fully qualified name
  name: string,         // Short name
  category: AssetKind,  // CLASS, INTERFACE, etc.
  description: string,
  color: string,
  methodCount: number,
  fieldCount: number
}
```

## 🎨 Customization

### Change Colors
Edit `src/config/index.ts`:
```typescript
colorMap: {
  'INTERFACE': '#your-color',
  'CLASS': '#your-color',
  // ...
}
```

### Change Graph Layout
Edit `src/views/ForceGraphView.ts`:
```typescript
force: {
  repulsion: 300,      // Node separation
  edgeLength: 100,     // Link length
  gravity: 0.1,        // Center gravity
  friction: 0.65       // Damping
}
```

### Add Filter Type
1. Update `NodeTypeFilters` in `src/types/index.ts`
2. Add to `DEFAULT_NODE_TYPE_FILTERS` in `src/config/index.ts`
3. Add UI element in `views/index.html`

## 🔍 Useful Patterns

### Load Data
```typescript
const data = await loadProjectData('my-project.json');
```

### Apply Filters
```typescript
const filtered = applyFilters(originalData, myFilters);
forceView.render(filtered);
```

### Search Nodes
```typescript
forceView.searchNodes('UserService');
```

### Export Image
```typescript
forceView.downloadImage('my-graph.png');
```

## ⚡ Performance Tips

1. **Large datasets**: Automatically disables animations
2. **Filtering**: Always filter before rendering
3. **Search**: Use keyword search instead of visual scanning
4. **Zoom**: Use toolbar buttons for precise zoom

## 🆘 Help

- **Build errors**: Read compiler messages (they're helpful!)
- **Type errors**: Check `src/types/index.ts`
- **Runtime errors**: Check browser console
- **Import errors**: Verify file paths (use `./` not `../`)

## 📚 Resources

- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [ECharts Documentation](https://echarts.apache.org/)
- [Project README](./README.md)
- [Migration Guide](./MIGRATION.md)
