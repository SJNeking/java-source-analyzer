#!/usr/bin/env bash
#
# build.sh - 构建 Java Source Analyzer JAR 文件
#
# 用法:
#   ./scripts/build.sh          # 常规构建
#   ./scripts/build.sh clean    # 清理后构建
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

info()    { echo -e "${BLUE}[INFO]${NC}    $*"; }
success() { echo -e "${GREEN}[SUCCESS]${NC} $*"; }
error()   { echo -e "${RED}[ERROR]${NC}   $*"; }

echo -e "${BLUE}══════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  Java Source Analyzer - 构建脚本${NC}"
echo -e "${BLUE}══════════════════════════════════════════════════${NC}"
echo ""

cd "$PROJECT_ROOT"

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    error "未找到 Maven，请先安装 Maven"
    exit 1
fi

# 检查 Java
if ! command -v java &> /dev/null; then
    error "未找到 Java，请先安装 JDK 8+"
    exit 1
fi

info "Maven 版本:"
mvn --version | head -1
echo ""

info "Java 版本:"
java -version 2>&1 | head -1
echo ""

# 构建
if [[ "${1:-}" == "clean" ]]; then
    info "清理旧构建..."
    mvn clean -q
fi

info "开始构建..."
if mvn clean package -q -Dmaven.test.skip=true; then
    echo ""
    success "构建成功！"
    echo ""

    JAR_FILE="$PROJECT_ROOT/target/glossary-java-source-analyzer-1.0.jar"
    if [[ -f "$JAR_FILE" ]]; then
        local size
        size=$(ls -lh "$JAR_FILE" | awk '{print $5}')
        info "JAR 文件: $JAR_FILE"
        info "文件大小: $size"
        echo ""
        info "运行帮助:"
        echo -e "  ${YELLOW}java -jar $JAR_FILE --help${NC}"
        echo ""
        info "快速分析项目:"
        echo -e "  ${YELLOW}./scripts/analyze.sh /path/to/project${NC}"
    fi
else
    echo ""
    error "构建失败，请检查编译错误"
    exit 1
fi
