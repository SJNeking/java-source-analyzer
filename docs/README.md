# 项目文档中心

## 📚 文档分类

### 根目录文档
- [README.md](../README.md) - 项目主文档（技术栈、架构、快速开始）

### 通用文档
- [USAGE.md](./USAGE.md) - 使用指南
- [RELEASE_NOTES.md](./RELEASE_NOTES.md) - 版本发布说明

### 前端文档
- [frontend/README.md](./frontend/README.md) - React前端技术文档
- [frontend/MIGRATION_COMPLETE.md](./frontend/MIGRATION_COMPLETE.md) - React迁移完成报告

## 📂 文档结构

```
docs/
├── README.md                    # 本文档（索引）
├── USAGE.md                     # 使用指南
├── RELEASE_NOTES.md             # 发布说明
└── frontend/                    # 前端相关文档
    ├── README.md                # 前端技术文档
    └── MIGRATION_COMPLETE.md    # React迁移报告
```

## 🔗 相关资源

### 代码目录
- `src/main/java/` - Java后端源码
- `frontend/src/` - React前端源码

### 配置文件
- `pom.xml` - Maven构建配置
- `frontend/package.json` - npm依赖配置
- `docker-compose.yml` - Docker编排配置

## 📝 文档规范

所有项目文档统一存放在`docs/`目录下，保持根目录整洁。

**文档类型**：
- 技术设计文档 → `docs/design/`
- API文档 → `docs/api/`
- 部署文档 → `docs/deployment/`
- 用户手册 → `docs/user-guide/`
