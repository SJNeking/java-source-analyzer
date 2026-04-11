# Java Source Analyzer - Frontend

A modern, multi-view TypeScript frontend for visualizing Java source code analysis results.

## 🎯 Features

### 1. **力导向图 (Force-Directed Graph)** 
Interactive visualization of class dependencies using ECharts.

**Features:**
- 🔗 Force-directed graph layout
- 🔍 Search classes by name
- 🎨 Node type filtering (Interface, Abstract Class, Class, Enum, Utility)
- 🖱️ Zoom, pan, and drag
- 📊 Node details on hover
- 🖼️ Export graph as PNG
- 🎯 Click nodes to see detailed class info

### 2. **质量分析 (Quality Analysis)**
Comprehensive code quality issues dashboard.

**Features:**
- ⚠️ View all quality issues by severity (Critical, Major, Minor, Info)
- 📊 Stats cards showing issue counts by severity and category
- 🔴🟠🔵🟢 Color-coded severity badges
- 🎛️ Filter issues by severity
- 📝 Detailed issue descriptions with rule names, line numbers, and methods

### 3. **代码指标 (Code Metrics)**
Statistical analysis and visualization of code metrics.

**Features:**
- 📊 Overview cards showing:
  - Total classes, methods, fields
  - Lines of code (LOC)
  - Comment ratio
  - Average/max complexity
  - Average coupling
  - Cohesion index
- 📈 Bar chart showing complexity distribution
- 🥧 Pie chart showing asset type distribution (Interface, Class, Enum, etc.)

### 4. **跨文件关系 (Cross-File Relations)**
View relationships between Java files and other assets (XML, SQL, Config, etc.)

**Features:**
- 🔀 Total relation count and type breakdown
- 🎛️ Filter by relation type (JAVA_TO_MAPPER, SQL_TO_ENTITY, etc.)
- 📊 Table view with:
  - Source/target file paths
  - Relation type badges
  - Confidence score with progress bars
  - Evidence snippets

### 5. **项目资产 (Project Assets)**
Browse non-Java file assets in a tree view.

**Features:**
- 📁 Expandable tree structure
- 📦 Maven POM files with dependency counts
- ⚙️ YAML/Properties configs with middleware detection
- 🗄️ SQL scripts with table counts
- 🔗 MyBatis Mappers with namespace info
- 🐳 Dockerfiles and Docker Compose files
- 📜 Shell scripts
- 📝 Log configs
- 📖 Markdown documents

### 6. **类检查器面板 (Class Inspector Panel)**
Slide-out panel showing detailed class information when clicking on graph nodes.

**Features:**
- 📋 Basic info (type, fully qualified name, source file, modifiers)
- 📝 Javadoc description and details
- ⚙️ Methods list with tags (up to 20 shown)
- 📊 Fields list with types and descriptions
- 🔗 Dependencies (incoming and outgoing)

## 🚀 Quick Start

### 1. Install Dependencies

```bash
cd nginx/html
npm install
```

### 2. Build TypeScript

```bash
npm run build
```

For development with auto-rebuild:

```bash
npm run watch
```

### 3. Add Analysis Data

Copy your Java analyzer JSON output files to the `data/` directory:

```bash
cp /path/to/java-analysis/*.json data/
```

Ensure `data/projects.json` exists with the project index:

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
```

Visit: `http://localhost:8080/views/index.html`

## 📁 Project Structure

```
nginx/html/
├── src/                      # TypeScript source code
│   ├── app.ts               # Main application with view switching
│   ├── types/               
│   │   └── index.ts         # Complete type definitions
│   ├── config/              
│   │   └── index.ts         # App configuration
│   ├── utils/               
│   │   ├── index.ts         # Utility exports
│   │   ├── logger.ts        # Logging utility
│   │   ├── data-loader.ts   # JSON data loading
│   │   ├── dom-helpers.ts   # DOM manipulation
│   │   └── filter-utils.ts  # Filter logic
│   └── views/               
│       ├── ForceGraphView.ts         # ECharts force graph
│       ├── QualityDashboardView.ts   # Quality issues dashboard
│       ├── MetricsDashboardView.ts   # Code metrics with charts
│       ├── CrossFileRelationsView.ts # Cross-file relations table
│       ├── ProjectAssetsView.ts      # Project assets tree
│       └── ClassInspectorPanel.ts    # Slide-out class inspector
├── views/
│   └── index.html           # Main HTML with tab navigation
├── css/
│   └── styles.css           # Global styles with dashboard components
├── js/lib/
│   └── echarts.min.js       # ECharts library
├── data/                    # JSON analysis files
├── dist/                    # Compiled JavaScript (generated)
├── package.json             # Node.js dependencies
├── tsconfig.json            # TypeScript configuration
└── build.js                 # esbuild build script
```

## 🛠️ Technology Stack

- **TypeScript 5.3+** - Type-safe JavaScript
- **esbuild** - Fast bundler (10-100x faster than tsc + webpack)
- **ECharts 5.5** - Charting/visualization library
- **ES2018** - Modern JavaScript features

## 📊 Expected Data Format

The frontend expects analysis JSON files with this structure:

```json
{
  "framework": "Project Name",
  "version": "1.0.0",
  "scan_date": "2026-04-10 12:00:00",
  "assets": [
    {
      "address": "com.example.MyClass",
      "kind": "CLASS",
      "description": "Class description",
      "source_file": "src/main/java/com/example/MyClass.java",
      "modifiers": ["public"],
      "methods_full": [...],
      "fields_matrix": [...]
    }
  ],
  "dependencies": [
    {
      "source": "com.example.MyClass",
      "target": "com.example.OtherClass",
      "type": "DEPENDS_ON"
    }
  ],
  "quality_issues": [...],
  "quality_summary": {...},
  "code_metrics": {...},
  "cross_file_relations": {...},
  "project_assets": {...}
}
```

## 🎨 UI Features

- 🌙 **Dark theme** with glassmorphism effects
- 📱 **Responsive design** for mobile/tablet/desktop
- 🔄 **Smooth animations** and transitions
- 🎨 **Consistent color palette**:
  - 🔵 Blue (#4299e1) - Interfaces
  - 🟣 Purple (#9f7aea) - Abstract classes
  - 🟢 Green (#48bb78) - Classes
  - 🟠 Orange (#ed8936) - Enums
  - ⚪ Gray (#a0aec0) - Utility classes

## 🐛 Troubleshooting

### Build fails

```bash
# Clean and rebuild
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

## 🎯 Future Enhancements

- [ ] Tree map view for package/module hierarchy
- [ ] Dependency matrix/heatmap view
- [ ] Dark/light theme toggle
- [ ] Bookmarking and sharing
- [ ] Export data as CSV/Excel
- [ ] Search across all views
- [ ] Advanced filtering options

## 📄 License

MIT
