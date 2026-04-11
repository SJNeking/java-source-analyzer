#!/bin/bash

# Java Source Analyzer Frontend - Development Server
# Watches TypeScript changes and serves the application

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🚀 Starting TypeScript development server...${NC}"

# Check if we're in the right directory
if [ ! -f "package.json" ]; then
    echo -e "${RED}❌ Error: package.json not found${NC}"
    echo "Please run this script from the nginx/html directory"
    exit 1
fi

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}📦 Installing dependencies...${NC}"
    npm install
fi

# Start TypeScript watcher in background
echo -e "${BLUE}👀 Watching TypeScript files...${NC}"
npm run watch &
TS_WATCH_PID=$!

# Trap to clean up background process on exit
cleanup() {
    echo -e "\n${YELLOW}🛑 Stopping development server...${NC}"
    kill $TS_WATCH_PID 2>/dev/null || true
    exit 0
}

trap cleanup INT TERM

# Start HTTP server
PORT=8080

# Check if port is already in use
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Port $PORT is already in use, trying 8081...${NC}"
    PORT=8081
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo -e "${YELLOW}⚠️  Port $PORT is also in use, trying 8082...${NC}"
        PORT=8082
    fi
fi

echo -e ""
echo -e "${GREEN}✅ Development server started!${NC}"
echo -e "🌐 URL: ${BLUE}http://localhost:$PORT/views/index.html${NC}"
echo -e "📂 Root: ${BLUE}$(pwd)${NC}"
echo -e "👀 TypeScript: ${GREEN}watching${NC} (PID: $TS_WATCH_PID)"
echo -e ""
echo -e "${YELLOW}Press Ctrl+C to stop${NC}"
echo -e ""

# Start HTTP server
python3 -m http.server $PORT
