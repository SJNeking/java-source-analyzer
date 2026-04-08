# Framework Assets Database Schema Design

## 📋 Overview

This schema extends the existing tagging system to support complete framework asset metadata storage, specifically designed for the Redisson JSON structure you provided.

## 🏗️ Architecture

### Core Tables

```
┌─────────────────────────────────────────────────────────────┐
│                  Framework Scan Session                      │
│              (framework_scans)                               │
└──────────────────┬──────────────────────────────────────────┘
                   │ 1:N
                   ▼
┌─────────────────────────────────────────────────────────────┐
│                    Code Assets                               │
│              (code_assets - extended)                        │
│  - Basic class info                                         │
│  - Generics, modifiers                                      │
│  - Inheritance hierarchy                                    │
└────┬────────────────────┬───────────────────────────────────┘
     │ 1:N                │ 1:N
     ▼                    ▼
┌──────────────┐   ┌──────────────────┐
│ Method       │   │ Field            │
│ Matrices     │   │ Matrices         │
│ (detailed    │   │ (categorized by  │
│ method info) │   │ role segments)   │
└──────────────┘   └──────────────────┘
```

## 📊 Table Structure

### 1. **framework_scans** - Scan Session Management
Tracks each framework scan with version and import status.

**Key Fields:**
- `framework`: "Redisson"
- `version`: "4.3.1-SNAPSHOT"
- `scan_date`: Timestamp of scan
- `import_status`: PENDING/IMPORTING/COMPLETED/FAILED

### 2. **code_assets** (Extended)
Enhanced existing table with:
- `scan_id`: Links to framework_scans
- `class_generics`: Class-level generic parameters
- `constructor_matrix`: JSONB storage for constructors

### 3. **method_matrices** - Detailed Method Information
Stores complete method metadata from `methods` and `method_matrix` arrays.

**Key Features:**
- Full method signature breakdown
- Generic parameters
- Exception declarations (throws_matrix + internal_throws)
- Method call relationships
- Parameter inventory as JSONB

**Example:**
```json
{
  "address": "org.redisson.hibernate.RedissonRegionNativeFactory#start(Settings,Properties)",
  "modifiers": ["public"],
  "is_override": true,
  "return_type_path": "void",
  "throws_matrix": ["CacheException"],
  "internal_throws": [
    "new IllegalArgumentException(\".eviction.max_entries setting can't be non-zero\")"
  ],
  "calls_methods": ["entrySet", "endsWith", "toString"],
  "parameters_inventory": [
    {"name": "settings", "type_path": "Settings"},
    {"name": "properties", "type_path": "java.util.Properties"}
  ]
}
```

### 4. **field_matrices** - Categorized Field Storage
Maps to `field_segments` structure with role categorization.

**Role Categories:**
- `INTERNAL_STATE`: Private state fields (e.g., serialVersionUID, log)
- `INTERNAL_COMPONENT`: Internal component references
- `EXTERNAL_SERVICE`: External service dependencies

**Example:**
```json
{
  "name": "serialVersionUID",
  "address": "org.redisson.hibernate.RedissonRegionNativeFactory.serialVersionUID",
  "type_path": "long",
  "modifiers": ["private", "static", "final"],
  "role_category": "INTERNAL_STATE"
}
```

### 5. **method_call_relationships** - Call Graph
Tracks method-to-method call relationships for dependency analysis.

## 🔍 Key Views

### **field_segments_view**
Quickly query fields grouped by role category:
```sql
SELECT * FROM field_segments_view 
WHERE asset_address = 'org.redisson.hibernate.RedissonRegionFactory';
```

### **inheritance_hierarchy_view**
Recursive view showing full inheritance chains up to 10 levels deep.

### **asset_completeness_view**
Monitors data import completeness by comparing declared vs actual counts.

## 🛠️ Utility Functions

### 1. **search_methods_by_name(p_method_name, p_limit)**
Full-text search across method names and descriptions.

```sql
SELECT * FROM search_methods_by_name('getCache', 10);
```

### 2. **get_asset_method_matrix(p_asset_address)**
Retrieve complete method matrix for an asset.

```sql
SELECT * FROM get_asset_method_matrix(
  'org.redisson.hibernate.RedissonRegionFactory'
);
```

### 3. **get_asset_field_segments(p_asset_address)**
Get fields organized by role category.

```sql
SELECT * FROM get_asset_field_segments(
  'org.redisson.hibernate.RedissonRegionFactory'
);
```

### 4. **find_callers_of_method(p_callee_method_name)**
Find all assets that call a specific method.

```sql
SELECT * FROM find_callers_of_method('start');
```

## 🔄 Data Import Workflow

```sql
-- Step 1: Create scan session
INSERT INTO framework_scans (framework, version, scan_date, import_status)
VALUES ('Redisson', '4.3.1-SNAPSHOT', NOW(), 'IMPORTING')
RETURNING id;

-- Step 2: Insert assets (linked to scan_id)
INSERT INTO code_assets (scan_id, address, simple_name, ...)
VALUES ('scan-id', 'org.redisson...', ...);

-- Step 3: Insert field matrices
INSERT INTO field_matrices (asset_id, scan_id, name, type_path, role_category, ...)
VALUES ('asset-id', 'scan-id', 'log', 'java.lang.Object', 'INTERNAL_STATE', ...);

-- Step 4: Insert method matrices
INSERT INTO method_matrices (asset_id, scan_id, address, method_name, ...)
VALUES ('asset-id', 'scan-id', '...#start()', 'start', ...);

-- Step 5: Mark scan as completed
UPDATE framework_scans 
SET import_status = 'COMPLETED', 
    asset_count = (SELECT COUNT(*) FROM code_assets WHERE scan_id = 'scan-id')
WHERE id = 'scan-id';
```

## 🎯 Integration with Tagging System

The new schema fully integrates with the existing tagging system:

1. **Assets maintain compatibility**: All existing tag queries work unchanged
2. **Enhanced metadata**: More detailed method/field info improves auto-tagging accuracy
3. **Version tracking**: Each scan is isolated, enabling version comparison
4. **Confidence scoring**: Can weight tags based on method/field analysis

### Enhanced Auto-Tagging Rules

With method_matrix data, you can add rules like:

```java
// Rule: Methods throwing CacheException → CACHE_ERROR_HANDLING tag
applyTagRule(conn,
    "SELECT DISTINCT ca.id FROM code_assets ca " +
    "JOIN method_matrices mm ON ca.id = mm.asset_id " +
    "WHERE 'CacheException' = ANY(mm.throws_matrix)",
    "CACHE_ERROR_HANDLING",
    0.85
);

// Rule: Override methods → OVERRIDDEN_METHOD tag
applyTagRule(conn,
    "SELECT DISTINCT ca.id FROM code_assets ca " +
    "JOIN method_matrices mm ON ca.id = mm.asset_id " +
    "WHERE mm.is_override = true",
    "OVERRIDDEN_METHOD",
    0.90
);
```

## 📈 Query Examples

### Find all public override methods
```sql
SELECT 
    ca.address AS class_address,
    mm.method_name,
    mm.return_type_path,
    mm.parameters_inventory
FROM method_matrices mm
JOIN code_assets ca ON mm.asset_id = ca.id
WHERE mm.is_override = true 
  AND 'public' = ANY(mm.modifiers)
ORDER BY ca.address, mm.method_name;
```

### Analyze field distribution by role
```sql
SELECT 
    role_category,
    COUNT(*) AS field_count,
    COUNT(DISTINCT asset_id) AS class_count
FROM field_matrices
GROUP BY role_category
ORDER BY field_count DESC;
```

### Find classes with most method calls
```sql
SELECT 
    ca.address,
    ca.simple_name,
    SUM(array_length(mm.calls_methods, 1)) AS total_calls
FROM code_assets ca
JOIN method_matrices mm ON ca.id = mm.asset_id
GROUP BY ca.id, ca.address, ca.simple_name
ORDER BY total_calls DESC
LIMIT 10;
```

## 🔐 Indexing Strategy

### GIN Indexes (Array & JSONB)
- `modifiers`, `class_generics`, `extends_from`, `implements_list`
- `method_generics`, `throws_matrix`, `calls_methods`
- `parameters_inventory` (JSONB)
- Full-text search vectors

### B-tree Indexes
- Foreign keys: `asset_id`, `scan_id`
- Type paths: `return_type_path`, `type_path`
- Role categories: `role_category`

### Trigram Indexes (Fuzzy Search)
- Method names and addresses
- Field names and addresses
- Asset addresses

## 🚀 Performance Considerations

1. **Partitioning**: For large frameworks (>100k assets), consider partitioning by package
2. **Materialized Views**: Pre-compute complex aggregations for dashboards
3. **Batch Inserts**: Use COPY or multi-row INSERT for ETL performance
4. **Connection Pooling**: Essential for concurrent scanning operations

## 📝 Next Steps

1. **Update ETL Script**: Modify `JsonToPostgresImporter.java` to populate new tables
2. **Add Validation**: Implement completeness checks during import
3. **Create Migration Script**: If upgrading from old schema
4. **Test Queries**: Validate all utility functions with sample data

## 🔗 Related Files

- **Schema**: `dev-ops/db/schema/framework_assets.sql`
- **Existing Tags**: `dev-ops/db/schema/tagging_system.sql`
- **ETL Script**: `src/main/java/cn/dolphinmind/glossary/redisson/etl/JsonToPostgresImporter.java`

---

**Design Date**: 2026-04-05  
**Compatible With**: Redisson 4.3.1-SNAPSHOT JSON structure  
**Database**: PostgreSQL 12+ (with pg_trgm, btree_gin, uuid-ossp extensions)
