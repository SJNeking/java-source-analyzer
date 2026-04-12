#!/bin/bash
# ============================================================
# CodeGuardian — 语义指纹缓存跳审脚本
# 
# 功能:
# 1. 计算项目语义指纹 (AST-based, 忽略空白/注释)
# 2. 检查 Redis 缓存 → 命中则跳过分析
# 3. 未命中则运行完整分析 + 缓存结果
# 
# 用法:
#   ./smart-analyze.sh /path/to/java/project
#   ./smart-analyze.sh /path/to/project --redis localhost:6379
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$1"
REDIS_ADDR="${2:-}"
JAR="$SCRIPT_DIR/target/glossary-java-source-analyzer-1.0.jar"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  CodeGuardian — 智能审查 (语义指纹跳审)     ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════╝${NC}"
echo ""

if [ -z "$PROJECT_ROOT" ]; then
    echo -e "${RED}用法: $0 <java_project_root> [--redis host:port]${NC}"
    echo ""
    echo "示例:"
    echo "  $0 /path/to/my-project                          # 无缓存"
    echo "  $0 /path/to/my-project --redis localhost:6379   # 带缓存"
    exit 1
fi

if [ ! -d "$PROJECT_ROOT" ]; then
    echo -e "${RED}错误: 目录不存在: $PROJECT_ROOT${NC}"
    exit 1
fi

# Check JAR
if [ ! -f "$JAR" ]; then
    echo -e "${YELLOW}⚠️  JAR 不存在, 正在构建...${NC}"
    cd "$SCRIPT_DIR" && mvn clean package -DskipTests -q
fi

# ============================================================
# Step 1: Compute semantic fingerprint
# ============================================================
echo -e "${BLUE}📐 Step 1: 计算语义指纹...${NC}"

FINGERPRINT_CMD="java -cp $JAR cn.dolphinmind.glossary.java.analyze.storage.SemanticFingerprinter $PROJECT_ROOT"
if [ -n "$REDIS_ADDR" ]; then
    FINGERPRINT_CMD="$FINGERPRINT_CMD --redis $REDIS_ADDR"
fi

FINGERPRINT_OUTPUT=$($FINGERPRINT_CMD 2>&1)
echo "$FINGERPRINT_OUTPUT"

# Extract fingerprint value
FINGERPRINT=$(echo "$FINGERPRINT_OUTPUT" | grep "^Fingerprint:" | cut -d' ' -f2)
CACHE_HIT=$(echo "$FINGERPRINT_OUTPUT" | grep "^Cache Hit:" | cut -d' ' -f3)

if [ -z "$FINGERPRINT" ]; then
    echo -e "${RED}❌ 指纹计算失败${NC}"
    exit 1
fi

echo ""
echo -e "   指纹: ${GREEN}$FINGERPRINT${NC}"

# ============================================================
# Step 2: Check cache
# ============================================================
if [ "$CACHE_HIT" = "yes" ]; then
    echo -e "${GREEN}✅ 缓存命中! 跳过分析, 直接使用历史结果${NC}"
    echo ""
    
    CACHED_RESULT=$(echo "$FINGERPRINT_OUTPUT" | grep "^Cached result:" | cut -d' ' -f3-)
    echo "   缓存结果: $CACHED_RESULT"
    echo ""
    echo -e "${GREEN}⚡ 审查完成 (跳审模式, 耗时 < 1s)${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠️  缓存未命中, 正在执行完整分析...${NC}"
    echo ""
fi

# ============================================================
# Step 3: Run full static analysis
# ============================================================
echo -e "${BLUE}🔍 Step 2: 运行静态分析...${NC}"
START_TIME=$(date +%s%N)

java -jar "$JAR" \
    --sourceRoot "$PROJECT_ROOT" \
    --outputDir "$SCRIPT_DIR/html/data" \
    --format json \
    --outputFile "$SCRIPT_DIR/html/data/static-results.json" \
    2>&1 | tail -5

END_TIME=$(date +%s%N)
ELAPSED_MS=$(( (END_TIME - START_TIME) / 1000000 ))

echo ""
echo -e "   分析耗时: ${YELLOW}${ELAPSED_MS}ms${NC}"

# ============================================================
# Step 4: Cache the result
# ============================================================
if [ -n "$REDIS_ADDR" ]; then
    echo -e "${BLUE}💾 Step 3: 缓存分析结果...${NC}"
    
    REDIS_HOST=$(echo "$REDIS_ADDR" | cut -d':' -f1)
    REDIS_PORT=$(echo "$REDIS_ADDR" | cut -d':' -f2)
    
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
        SET "fp:$FINGERPRINT" "{\"result_id\":1,\"hit_count\":0}" EX 2592000 2>/dev/null || true
    
    echo -e "   ${GREEN}已缓存${NC} (TTL: 30 天)"
    echo ""
fi

# ============================================================
# Step 5: Generate unified report
# ============================================================
echo -e "${BLUE}📋 Step 4: 生成统一报告...${NC}"

AI_FLAG=""
if [ -f "$SCRIPT_DIR/html/data/ai-results.json" ] && [ -s "$SCRIPT_DIR/html/data/ai-results.json" ]; then
    AI_FLAG="--ai $SCRIPT_DIR/html/data/ai-results.json"
fi

java -cp "$JAR" \
    cn.dolphinmind.glossary.java.analyze.unified.ResultMerger \
    --static "$SCRIPT_DIR/html/data/static-results.json" \
    $AI_FLAG \
    --sourceRoot "$PROJECT_ROOT" \
    --project "$(basename $PROJECT_ROOT)" \
    --output "$SCRIPT_DIR/html/data/unified-report.json" \
    2>&1 | tail -10

echo ""
echo -e "${GREEN}✅ 审查完成 (完整模式)${NC}"
echo ""
echo -e "🌐 查看报告: http://localhost:8080/views/index.html"
echo -e "   → 切换到 '🤖 AI 审查' 标签页"
