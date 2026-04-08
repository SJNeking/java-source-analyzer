#!/usr/bin/env bash
#
# quick-start.sh - 一键构建并运行示例
#
# 用法:
#   ./scripts/quick-start.sh          # 构建 + 分析本项目
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  Java Source Analyzer - 快速开始${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""

# Step 1: 构建
echo -e "${BLUE}[1/3]${NC} 构建项目..."
cd "$PROJECT_ROOT"
mvn clean package -q -Dmaven.test.skip=true
echo -e "${GREEN}  ✓ 构建完成${NC}"
echo ""

# Step 2: 分析本项目
OUTPUT_DIR="$PROJECT_ROOT/dev-ops/analysis-output"
echo -e "${BLUE}[2/3]${NC} 分析当前项目..."
echo -e "  输出目录: $OUTPUT_DIR"
echo ""

java -jar "$PROJECT_ROOT/target/glossary-java-source-analyzer-1.0.jar" \
    --sourceRoot "$PROJECT_ROOT" \
    --outputDir "$OUTPUT_DIR" \
    --version "$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null || echo '1.0')"

echo ""

# Step 3: 展示结果
echo -e "${BLUE}[3/3]${NC} 分析结果:"
echo ""
echo -e "  ${GREEN}输出文件:${NC}"
find "$OUTPUT_DIR" -name "*.json" -type f | sort | while read -r file; do
    size=$(ls -lh "$file" | awk '{print $5}')
    echo -e "    ${GREEN}✓${NC} $(basename "$file") ($size)"
done

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  快速开始完成！${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  ${YELLOW}查看完整文档:${NC}  cat $PROJECT_ROOT/USAGE.md"
echo -e "  ${YELLOW}分析其他项目:${NC}  ./scripts/analyze.sh /path/to/project"
echo ""
