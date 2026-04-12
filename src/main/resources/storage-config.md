# ============================================================
# MinIO 统一存储配置
# 用于: Java Source Analyzer + CodeGuardian AI
# ============================================================

# Bucket 结构:
# codeguardian/                    ← 主 bucket
# ├── analysis/                    ← 分析结果
# │   └── {project}/
# │       └── {commit}/
# │           ├── static-results.json
# │           ├── ai-results.json
# │           ├── unified-report.json
# │           └── rag-context.json
# │
# ├── reviews/                     ← 审查报告 (GitHub Pages 可部署)
# │   └── {project}/
# │       └── {commit}/
# │           ├── views/           ← 完整前端站点
# │           │   ├── index.html
# │           │   ├── css/
# │           │   └── dist/
# │           └── unified-report.json
# │
# ├── snapshots/                   ← 代码快照
# │   └── {project}/
# │       └── {commit}/
# │           └── source-code.zip
# │
# └── embeddings/                  ← 向量索引备份
#     └── {project}/
#         └── {date}/
#             └── embeddings.parquet

# ============================================================
# Redis 键设计
# ============================================================

# 1. 语义指纹缓存 (跳审)
# Key:   fp:{fingerprint}
# TTL:   30 天
# Value: JSON { "result_id": 123, "created_at": "...", "hit_count": 5 }
# 
# 用法:
#   FP = md5(find . -name "*.java" | sort | xargs md5sum | md5sum | cut -c1-16)
#   EXISTS fp:$FP → 命中则跳过静态分析

# 2. AI 审查缓存
# Key:   ai:{file_path_hash}:{method_name}:{content_hash}
# TTL:   7 天
# Value: JSON { "suggestion": "...", "confidence": 0.85, "fixed_code": "..." }

# 3. 分析结果缓存
# Key:   analysis:{project}:{commit}
# TTL:   90 天
# Value: JSON { "result_id": 456, "status": "completed", "issues": 191 }

# 4. 质量门禁
# Key:   gate:{project}:{branch}
# TTL:   永久 (覆盖写)
# Value: JSON { "passed": true, "last_commit": "abc123", "updated_at": "..." }

# 5. 速率限制 (防止 AI API 超限)
# Key:   ratelimit:ai:{model}:{project}
# TTL:   60 秒
# Value: 当前请求数
#
# 用法: INCR + EXPIRE, 超过阈值则排队

# 6. 分布式锁 (防止并发分析冲突)
# Key:   lock:analysis:{project}
# TTL:   10 分钟
# Value: {worker_id}:{timestamp}
#
# 用法: SET NX EX, 获取锁才执行分析

# ============================================================
# 初始化脚本
# ============================================================

# PostgreSQL (需要 pgvector 扩展):
#   psql -U root -d postgres -f unified-schema.sql
#   psql -U root -d postgres -c "CREATE EXTENSION IF NOT EXISTS vector;"

# MinIO (mc alias):
#   mc alias set local http://localhost:9000 root minio123
#   mc mb local/codeguardian
#   mc anonymous set download local/codeguardian  # 允许公开读取报告

# Redis:
#   redis-cli PING → PONG
