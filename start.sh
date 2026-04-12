#!/bin/bash

# Java Source Analyzer - 启动脚本
# 启动前端可视化服务器 + 可选的后端分析服务

# 定义颜色
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     Java Source Analyzer - 全方位代码可视化工具          ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""

# 解析参数
ANALYZE_MODE=false
SOURCE_ROOT=""
WEBSOCKET_PORT=8887

for arg in "$@"; do
    case $arg in
        --analyze)
            ANALYZE_MODE=true
            shift
            ;;
        --sourceRoot=*)
            SOURCE_ROOT="${arg#*=}"
            shift
            ;;
        --websocket-port=*)
            WEBSOCKET_PORT="${arg#*=}"
            shift
            ;;
        --help)
            echo "用法: ./start.sh [选项]"
            echo ""
            echo "选项:"
            echo "  --analyze              启用分析模式（需配合 --sourceRoot）"
            echo "  --sourceRoot=<路径>     要分析的 Java 项目源码路径"
            echo "  --websocket-port=<端口> WebSocket 端口 (默认: 8887)"
            echo "  --help                 显示帮助"
            echo ""
            echo "示例:"
            echo "  ./start.sh                                  # 仅启动前端"
            echo "  ./start.sh --analyze --sourceRoot=/path/to/project  # 分析项目"
            echo "  ./start.sh --analyze --sourceRoot=... --websocket-port=8887"
            exit 0
            ;;
    esac
done

# 进入前端目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$SCRIPT_DIR/html"
cd "$FRONTEND_DIR" || exit

# 构建前端
echo -e "${BLUE}🔨 正在构建前端...${NC}"
if command -v node &> /dev/null; then
    if [ ! -d "node_modules" ]; then
        echo -e "${BLUE}📦 安装依赖...${NC}"
        npm install --silent 2>/dev/null
    fi

    npm run build 2>/dev/null

    if [ ! -f "dist/app.js" ]; then
        echo -e "${YELLOW}⚠️  构建失败，将使用现有的 JS 文件${NC}"
    else
        echo -e "${GREEN}✅ 前端构建完成${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Node.js 未安装，跳过构建${NC}"
fi

# 检查数据文件
if [ ! -f "data/projects.json" ]; then
    echo -e "${YELLOW}⚠️  未找到 data/projects.json，请添加分析数据文件${NC}"
fi

# 检查端口
PORT=8080
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  端口 $PORT 已被占用，尝试使用 8081...${NC}"
    PORT=8081
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        PORT=8082
    fi
fi

# 启动分析服务（如果启用）
if [ "$ANALYZE_MODE" = true ] && [ -n "$SOURCE_ROOT" ]; then
    echo ""
    echo -e "${BLUE}🔍 正在分析项目: ${GREEN}$SOURCE_ROOT${NC}"
    echo -e "${BLUE}🔌 WebSocket 端口: ${GREEN}$WEBSOCKET_PORT${NC}"
    echo ""

    # 构建 Java 项目
    echo -e "${BLUE}🔨 正在编译 Java 分析工具...${NC}"
    cd "$SCRIPT_DIR" && mvn package -DskipTests -q 2>/dev/null

    # 运行分析
    JAR_FILE=$(ls -t "$SCRIPT_DIR"/target/*-jar-with-dependencies.jar 2>/dev/null | head -1)
    if [ -n "$JAR_FILE" ]; then
        echo -e "${GREEN}✅ 开始分析 (WebSocket: ws://localhost:$WEBSOCKET_PORT)${NC}"
        echo -e "${YELLOW}⏳ 分析过程中，前端会实时显示进度...${NC}"
        echo ""

        # 后台运行分析
        java -jar "$JAR_FILE" \
            --sourceRoot "$SOURCE_ROOT" \
            --websocket "$WEBSOCKET_PORT" \
            2>&1 &

        ANALYZER_PID=$!
        echo -e "${BLUE}📊 分析进程 PID: $ANALYZER_PID${NC}"
    else
        echo -e "${RED}❌ 未找到 JAR 文件，请先运行 mvn package${NC}"
    fi

    cd "$FRONTEND_DIR"
fi

# 启动前端服务器
echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  ✅ 服务器已启动！                                      ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "🌐 前端地址: ${BLUE}http://localhost:$PORT/views/index.html${NC}"
echo ""
echo -e "${YELLOW}📂 7 个可视化视图:${NC}"
echo -e "   🔗 力导向图    - 类依赖关系图"
echo -e "   ⚠️  质量分析    - 代码质量问题"
echo -e "   📊 代码指标     - LOC/复杂度/耦合"
echo -e "   🌐 API 端点    - REST API 列表"
echo -e "   🏗️  架构分层    - Controller→Service→Repository"
echo -e "   🔀 跨文件关系   - XML/SQL/配置关联"
echo -e "   📁 项目资产     - 非 Java 文件"
echo ""
if [ "$ANALYZE_MODE" = true ]; then
    echo -e "${BLUE}🔌 WebSocket 实时分析: ws://localhost:$WEBSOCKET_PORT${NC}"
    echo -e "${YELLOW}   连接后可在浏览器中实时查看分析进度${NC}"
    echo ""
fi
echo -e "${BLUE}按 Ctrl+C 停止服务器${NC}"
echo ""

# 启动 HTTP 服务器
python3 -m http.server $PORT --bind 127.0.0.1 2>/dev/null
