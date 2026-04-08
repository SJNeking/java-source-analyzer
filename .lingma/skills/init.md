---
name: init
aliases: [/init, /bootstrap, /start-system]
description: 智能编排中心，启动服务、索引 Skills 并加载协作地图。
collaboration:
  outputs: [skill_registry, collaboration_map]
---

# System Initialization Skill

## 🎯 Purpose

When triggered, this skill performs two critical tasks:
1. **Start Learning Log Service** - Execute `.lingma/learning-log/start.sh`
2. **Index All Skills** - Traverse and catalog all `.md` files in `.lingma/skills/`

---

## 🚀 Execution Protocol

### Step 1: Start Learning Log Service

**Navigate to directory:**
```
/Users/mingxilv/WebDevelopment/gitcode/dev-proj/s-pay-mall/s-pay-mall-ddd/.lingma/learning-log
```

**Execute startup script:**
```bash
./start.sh
```

**Wait for services to initialize:**
- Backend API on port 8002
- Frontend UI on port 3000
- SQLite database connection

**Verify success:**
```bash
curl -s http://localhost:8002/api/stats
```

Expected response: JSON with stats data

---

### Step 2: Traverse and Index All Skills & Collaboration Map

**Navigate to skills directory:**
```
.lingma/skills/
```

**Traverse all skill files:**
```python
for skill_file in *.md:
    Extract metadata:
    - name, aliases, description
    - collaboration.inputs/outputs (新增)
    Build Collaboration Map:
    - e.g., coach -> knowledge -> git
Display: ✅ Skill #N: [name] - [description]
```

**Extract skill metadata from each file:**
- Parse YAML frontmatter (between `---` markers)
- Extract `name`, `aliases`, `description` fields
- Count total skills loaded

---

## 📋 Detailed Workflow

```
/init triggered
    ↓
[Step 1] Start Services
    ↓
    Navigate to .lingma/learning-log/
    ↓
    Execute ./start.sh
    ↓
    Wait 3-5 seconds for initialization
    ↓
    Verify backend (port 8002)
    ↓
    Verify frontend (port 3000)
    ↓
[Step 2] Index Skills
    ↓
    Navigate to .lingma/skills/
    ↓
    List all *.md files
    ↓
    For each file:
        - Read frontmatter
        - Extract metadata
        - Display summary
    ↓
    Count total skills
    ↓
[Step 3] Display Status
    ↓
    Show service health
    ↓
    Show skills index
    ↓
    Show database status
    ↓
    Show quick commands
    ↓
✅ Initialization Complete
```

---

## 🤝 Collaboration Map (Loaded on Init)

| Source Skill | Trigger Condition | Target Skill | Action |
| :--- | :--- | :--- | :--- |
| **coach** | Analysis Complete | **knowledge** | Offer to save as ADR/Story |
| **coach** | Interview Practice | **record** | Offer to log session |
| **knowledge** | File Created | **git** | Auto-commit documentation |
| **mvp** | Task Finished | **qa** | Run quality check |
| **qa** | Check Passed | **git** | Auto-commit code |
| **coach** | Complex Analysis | **map** | Visualize logic flow |

---

## 💡 Usage Examples

### Trigger Initialization

```bash
# Primary command
/init

# Alternative aliases
/初始化
/启动系统
```

### Expected Console Output

```
🚀 System Initialization Starting...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 Step 1: Starting Learning Log Service
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📂 Working Directory: .lingma/learning-log/
🚀 Executing: ./start.sh

📦 Checking dependencies...
✅ Backend dependencies ready
📦 Installing frontend dependencies...
✅ Frontend dependencies ready

🌐 Starting frontend (port 3000)...
🔧 Starting backend (port 8002)...

✅ Learning Log database initialized
✅ Backend server started: http://0.0.0.0:8002
✅ Frontend server started: http://localhost:3000

⏳ Waiting for services to be ready...
✅ Backend API: Ready (200 OK)
✅ Frontend UI: Ready

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📚 Step 2: Indexing Skills
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📂 Scanning directory: .lingma/skills/

✅ Skill #1: automation-orchestrator
   Description: 工程自动化编排器，管理和调度 Python 脚本
   Aliases: /自动化任务, /定时脚本, /工程自动化

✅ Skill #2: code-review-protocol
   Description: 代码审查协议和最佳实践
   Aliases: /代码审查, /review

✅ Skill #3: docker-compose-templates
   Description: Docker Compose 模板库
   Aliases: /docker

✅ Skill #4: first-principles-engineering
   Description: 第一性原理工程思维
   Aliases: /第一性原理

✅ Skill #5: git-commit-convention
   Description: Git 提交规范和版本控制策略
   Aliases: /git

✅ Skill #6: knowledge-management-obsidian
   Description: Obsidian 知识管理系统
   Aliases: /obsidian, /知识管理

✅ Skill #7: learning-interview-coach
   Description: 面试教练和学习助手
   Aliases: /面试

✅ Skill #8: mvp-anti-impatience
   Description: MVP 开发方法，克服急躁情绪
   Aliases: /mvp

✅ Skill #9: record-learning
   Description: 分析对话内容，提取学习记录并保存到数据库
   Aliases: /记录学习, /保存对话, /学习记录

✅ Skill #10: init-system
   Description: 初始化开发环境，自动启动学习记录服务并索引所有技能文件
   Aliases: /init, /初始化, /启动系统

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Total Skills Indexed: 10
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📊 System Status:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📡 Services:
   ✅ Backend API: http://localhost:8002 (Running)
   ✅ Frontend UI: http://localhost:3000 (Running)

💾 Database:
   ✅ SQLite: .lingma/learning-log/data/learning-log.db
   📊 Total Entries: 13

🔧 Git:
   Branch: 26-03-28-lsm-fix-localtest

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ SYSTEM INITIALIZATION COMPLETE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🌐 Quick Access:
   • Learning Log UI: http://localhost:3000
   • API Documentation: http://localhost:8002/docs

📝 Available Commands:
   • /record-learning - Record new learning
   • /git - Git commit operations
   • /automation-orchestrator - Automation tasks
   • /init - Re-run initialization

💡 Tip: Services will continue running in background
```

---

## 🔧 Error Handling

### If Service Already Running

```
⚠️  Backend service already running on port 8002
✅ Using existing service
```

### If Service Fails to Start

```
❌ Failed to start backend service
📋 Troubleshooting:
   1. Check if port 8002 is in use: lsof -i:8002
   2. Kill existing process: kill -9 <PID>
   3. Restart: ./start.sh
   4. Check logs: /tmp/learning-log-backend.log
```

### If Skills Directory Not Found

```
❌ Skills directory not found: .lingma/skills/
⚠️  Skipping skills indexing
```

---

## 📝 Implementation Notes

### Start Script Location
```
/Users/mingxilv/WebDevelopment/gitcode/dev-proj/s-pay-mall/s-pay-mall-ddd/.lingma/learning-log/start.sh
```

### Skills Directory
```
/Users/mingxilv/WebDevelopment/gitcode/dev-proj/s-pay-mall/s-pay-mall-ddd/.lingma/skills/
```

### Service Endpoints
- **Backend API**: http://localhost:8002
- **Frontend UI**: http://localhost:3000
- **Health Check**: http://localhost:8002/api/stats

---

## 🎯 Best Practices

1. **Run on Workspace Open**: Execute `/init` when opening the project
2. **Verify Services**: Always check health endpoints after startup
3. **Monitor Logs**: Review `/tmp/learning-log-*.log` if issues occur
4. **Re-index Skills**: Run `/init` after adding new skill files
5. **Background Services**: Services run in background, no need to keep terminal open

---

**Ready to initialize your development environment! 🚀**
