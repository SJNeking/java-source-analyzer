#!/bin/bash

# Java Source Analyzer Frontend - Build Script
# Compiles TypeScript to JavaScript

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Configuration (can be overridden via environment variables)
DEFAULT_PORT="${DEV_PORT:-8080}"

echo -e "${BLUE}🔨 Building TypeScript frontend...${NC}"

# Check if we're in the right directory
if [ ! -f "package.json" ]; then
    echo -e "${RED}❌ Error: package.json not found${NC}"
    echo "Please run this script from the nginx/html directory"
    exit 1
fi

# Check if node is installed
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Error: node not found${NC}"
    echo "Please install Node.js (v16 or later)"
    exit 1
fi

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo -e "${RED}❌ Error: npm not found${NC}"
    echo "Please install npm (comes with Node.js)"
    exit 1
fi

# Install dependencies
echo -e "${BLUE}📦 Installing dependencies...${NC}"
npm install

# Clean previous build
echo -e "${BLUE}🧹 Cleaning previous build...${NC}"
rm -rf dist/*

# Compile TypeScript
echo -e "${BLUE}⚙️  Compiling TypeScript...${NC}"
npm run build

# Check if build was successful
if [ ! -f "dist/app.js" ]; then
    echo -e "${RED}❌ Build failed: dist/app.js not found${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Build successful!${NC}"
echo -e "📁 Output: ${BLUE}dist/${NC}"
echo -e "🌐 Entry point: ${BLUE}views/index.html${NC}"
echo -e ""
echo -e "${GREEN}Next steps:${NC}"
echo -e "  1. Copy JSON data files to ${BLUE}data/${NC}"
echo -e "  2. Run ${BLUE}../../start.sh${NC} to start the server"
echo -e "  3. Open http://localhost:$DEFAULT_PORT/views/index.html"
