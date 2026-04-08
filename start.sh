#!/bin/bash

# 定义颜色
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 正在启动 Java Source Analyzer 前端...${NC}"

# 进入前端目录
cd "$(dirname "$0")/nginx/html" || exit

# 检查端口是否被占用，如果被占用则使用其他端口
PORT=8080
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null ; then
    echo -e "⚠️  端口 $PORT 已被占用，尝试使用 8081..."
    PORT=8081
fi

echo -e "${GREEN}✅ 服务器已启动！${NC}"
echo -e "🌐 请在浏览器中访问: ${BLUE}http://localhost:$PORT/views/index.html${NC}"
echo -e "${BLUE}按 Ctrl+C 停止服务器${NC}"

# 启动 Python HTTP 服务器
python3 -m http.server $PORT
