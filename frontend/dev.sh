#!/bin/bash

# Java Source Analyzer - Frontend Development Script
echo "🚀 Starting frontend development server..."

cd "$(dirname "$0")"

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
fi

# Start dev server
echo "⚡ Starting Vite dev server on http://localhost:3000"
npm run dev
