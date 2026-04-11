# Java Source Analyzer - TypeScript Frontend

A modern, type-safe frontend for visualizing Java source code analysis results.

## 🏗️ Architecture

```
Java CLI Tool → JSON Files → TypeScript Frontend → Interactive Visualization
```

The Java backend analyzes Java projects and generates JSON files. This TypeScript frontend reads those JSON files and creates interactive force-directed graph visualizations.

## 📁 Project Structure

```
nginx/html/
├── src/                      # TypeScript source code
│   ├── app.ts               # Main application entry point
│   ├── types/               # TypeScript type definitions
│   │   └── index.ts         # All type definitions
│   ├── config/              # Configuration
│   │   └── index.ts         # App configuration
│   ├── utils/               # Utility modules
│   │   ├── index.ts         # Utility exports
│   │   ├── logger.ts        # Logging utility
│   │   ├── data-loader.ts   # JSON data loading
│   │   ├── dom-helpers.ts   # DOM manipulation
│   │   └── filter-utils.ts  # Filter logic
│   └── views/               # View components
│       └── ForceGraphView.ts # ECharts force graph
├── views/
│   └── index.html           # Main HTML file (loads dist/app.js)
├── css/
│   └── styles.css           # Global styles
├── js/lib/
│   └── echarts.min.js       # ECharts library
├── data/                    # JSON analysis files (put your *.json here)
├── dist/                    # Compiled JavaScript (generated)
├── package.json             # Node.js dependencies
├── tsconfig.json            # TypeScript configuration
├── build.sh                 # Build script
└── dev.sh                   # Development server with hot reload
```

## 🚀 Quick Start

### 1. Install Dependencies

```bash
cd nginx/html
npm install
```

### 2. Build TypeScript

```bash
# Using build script
./build.sh

# Or manually
npm run build
```

This compiles TypeScript files to `dist/app.js`.

### 3. Add Analysis Data

Copy your Java analyzer JSON output files to the `data/` directory:

```bash
cp /path/to/java-analysis/*.json data/
```

Create a `projects.json` index file:

```json
{
  "frameworks": [
    {
      "file": "myframework_v2.0_full_20260410.json",
      "name": "My Framework v2.0"
    }
  ]
}
```

### 4. Start the Server

```bash
# From project root
./start.sh

# Or from nginx/html
./dev.sh  # Development mode with TypeScript watching
```

Visit: `http://localhost:8080/views/index.html`

## 🛠️ Development

### Watch Mode (Auto-rebuild on changes)

```bash
npm run watch
```

### Build

```bash
npm run build
```

### Clean

```bash
npm run clean
```

## 📊 Data Format

### projects.json

```json
{
  "frameworks": [
    {
      "file": "project_v1.0_full_20260410.json",
      "name": "Project Name v1.0"
    }
  ]
}
```

### Analysis JSON (from Java backend)

The Java backend generates:
- `{name}_v{ver}_full_{date}.json` - Full analysis
- `{name}_v{ver}_summary_{date}.json` - Summary
- `{name}_semantic_dictionary.json` - Semantic dictionary

The frontend expects the full analysis format with:
- `framework` - Project name
- `version` - Version string
- `assets[]` - Classes, interfaces, etc.
- `dependencies[]` - Relationships between assets

## 🎨 Features

### 1. Force-Directed Graph
Interactive visualization of class dependencies using ECharts.

### 2. Node Type Filtering
Filter by:
- Interface
- Abstract Class
- Concrete Class
- Enum
- Utility Class

### 3. Search
Search for classes by name with highlighting.

### 4. Zoom & Pan
- Mouse wheel to zoom
- Drag to pan
- Click nodes to see details

### 5. Export
Export graph as PNG image.

## 🔧 Configuration

Edit `src/config/index.ts` to customize:

```typescript
export const CONFIG: AppConfig = {
  DATA_PATH: '/data/',
  PROJECTS_INDEX: '/data/projects.json',
  largeDatasetThreshold: 1000,
  colorMap: {
    'INTERFACE': '#4299e1',
    'ABSTRACT_CLASS': '#9f7aea',
    'CLASS': '#48bb78',
    // ...
  }
};
```

## 📝 Type Safety

All data structures are fully typed:

```typescript
interface AnalysisResult {
  framework: string;
  version: string;
  assets: Asset[];
  dependencies: Dependency[];
  // ...
}
```

Benefits:
- Compile-time error checking
- IDE autocomplete
- Safer refactoring
- Self-documenting code

## 🐛 Troubleshooting

### Build fails

```bash
# Check TypeScript version
npx tsc --version

# Clear and rebuild
rm -rf dist/ node_modules/
npm install
npm run build
```

### Data not loading

1. Check `data/projects.json` exists
2. Verify JSON files are valid
3. Check browser console for errors
4. Ensure CORS is enabled (Python server does this automatically)

### Port already in use

```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill

# Or use different port
PORT=8081 python3 -m http.server $PORT
```

## 📚 Technology Stack

- **TypeScript 5.3+** - Type-safe JavaScript
- **ECharts 5.5** - Charting/visualization library
- **ES2018** - Modern JavaScript features
- **ES Modules** - Native module system

## 🎯 Future Enhancements

- [ ] Additional view types (tree map, dependency matrix)
- [ ] Detailed class/method inspection panels
- [ ] Quality metrics dashboard
- [ ] Dark/light theme toggle
- [ ] Bookmarking and sharing
- [ ] Real-time collaboration
- [ ] Export to various formats (SVG, PDF, GraphML)

## 📄 License

MIT
