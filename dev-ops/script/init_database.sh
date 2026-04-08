#!/bin/bash
# ============================================================================
# Database Schema Initialization Script
# Executes SQL files in correct dependency order
# ============================================================================

# Database configuration
DB_HOST="localhost"
DB_PORT="15432"
DB_NAME="redisson_brain_db"
DB_USER="root"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Database Schema Initialization${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo -e "${RED}Error: psql command not found${NC}"
    echo "Please install PostgreSQL client tools"
    exit 1
fi

# Step 1: Execute tagging_system.sql (base schema)
echo -e "${YELLOW}[1/2] Executing tagging_system.sql...${NC}"
if psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f ../db/schema/tagging_system.sql; then
    echo -e "${GREEN}✓ tagging_system.sql executed successfully${NC}"
else
    echo -e "${RED}✗ Failed to execute tagging_system.sql${NC}"
    exit 1
fi

echo ""

# Step 2: Execute framework_assets.sql (extended schema)
echo -e "${YELLOW}[2/2] Executing framework_assets.sql...${NC}"
if psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f ../db/schema/framework_assets.sql; then
    echo -e "${GREEN}✓ framework_assets.sql executed successfully${NC}"
else
    echo -e "${RED}✗ Failed to execute framework_assets.sql${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All schemas initialized successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Database: $DB_NAME"
echo "Host: $DB_HOST:$DB_PORT"
echo "User: $DB_USER"
echo ""
echo "Next steps:"
echo "  1. Run ETL script to import JSON data"
echo "  2. Query using the provided utility functions"
