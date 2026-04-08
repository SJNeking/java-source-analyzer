---
name: docker-compose-templates
aliases: [/env-up, /docker]
description: 自动化编排开发环境，一键生成配置并启动 Docker 容器。
---

# Docker Compose Automation (v1.0)

## 🎯 核心目标

通过标准化的模板和自动化脚本，实现开发环境的**秒级启动**与**安全配置**。

---

## ⚙️ 强制执行流程

### Step 1: 环境检查
```python
# 检查 Docker 是否运行
run_in_terminal("docker info")
# 检查当前目录是否已有 .env 文件
```

### Step 2: 配置生成
如果 `.env` 不存在，自动生成包含随机强密码的配置文件：
```python
import secrets
env_content = f"""
MYSQL_ROOT_PASSWORD={secrets.token_urlsafe(16)}
MYSQL_DATABASE=learning_log
REDIS_PORT=6379
"""
# 写入 .env 文件
```

### Step 3: 服务启动
根据预设模板（如 `data-store.yml`）执行：
```bash
docker compose -f templates/data-store.yml up -d
```

---

## 📋 常用模板库

| 模板名称 | 包含服务 | 适用场景 |
| :--- | :--- | :--- |
| `data-store` | MySQL 8.0, Redis 7 | 学习日志后端、通用数据存储 |
| `full-stack` | Nginx, FastAPI, MySQL, Redis | 完整的前后端联调环境 |

---

## 🛠️ 路径规范

| 资源 | 路径 |
| :--- | :--- |
| **模板目录** | `.lingma/templates/` |
| **环境变量** | 项目根目录 `.env` (已加入 .gitignore) |

---

## 💡 维护建议

1. **数据重置**：执行 `docker compose down -v` 可清除所有持久化数据。
2. **日志查看**：使用 `docker compose logs -f [service_name]` 实时追踪。
3. **安全第一**：严禁将生成的 `.env` 文件提交到 Git 仓库。
