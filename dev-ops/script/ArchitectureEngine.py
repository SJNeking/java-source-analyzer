import json
import psycopg2
from psycopg2 import sql
from psycopg2.extras import Json

class ArchitectureEngine:
    def __init__(self, host, user, password):
        self.host = host
        self.user = user
        self.password = password

    def build_version_castle(self, framework, version, json_path):
        """
        物理工程闭环：创建库 -> 建表 -> 灌数 (包含类、方法、调度窗口)
        """
        # 1. 规范化物理库名
        db_name = f"assets_{framework}_{version.replace('.', '_')}".lower()

        # --- 第一阶段：物理建库 ---
        print(f"🚀 第1步：正在连接系统库，准备创建物理空间...")
        try:
            admin_conn = psycopg2.connect(
                host=self.host, user=self.user, password=self.password, dbname="postgres"
            )
            admin_conn.autocommit = True
            with admin_conn.cursor() as cur:
                cur.execute("SELECT 1 FROM pg_database WHERE datname = %s", (db_name,))
                if not cur.fetchone():
                    print(f"🏗️  物理库不存在，正在创建数据库: {db_name}")
                    cur.execute(sql.SQL("CREATE DATABASE {}").format(sql.Identifier(db_name)))
                else:
                    print(f"📢 数据库 {db_name} 已存在，准备刷新数据。")
            admin_conn.close()
        except Exception as e:
            print(f"❌ 连接系统库失败: {e}")
            return

        # --- 第二阶段：初始化资产表结构 (DDL) ---
        print(f"🛠️  第2步：正在进入库 {db_name} 初始化资产表...")
        try:
            dest_conn = psycopg2.connect(
                host=self.host, user=self.user, password=self.password, dbname=db_name
            )
            dest_conn.autocommit = True
            with dest_conn.cursor() as cur:
                cur.execute("""
                    -- 1. 物理资产表 (类级别)
                    CREATE TABLE IF NOT EXISTS nodes (
                        id SERIAL PRIMARY KEY,
                        address VARCHAR(512) UNIQUE NOT NULL,
                        kind VARCHAR(50),
                        description TEXT,
                        raw_json JSONB
                    );

                    -- 2. 逻辑资产表 (方法级别 - 核心补全)
                    CREATE TABLE IF NOT EXISTS methods (
                        id SERIAL PRIMARY KEY,
                        node_address VARCHAR(512) REFERENCES nodes(address) ON DELETE CASCADE,
                        full_address VARCHAR(1500) UNIQUE,
                        calls JSONB DEFAULT '[]',
                        method_raw JSONB
                    );

                    -- 3. 语义调度窗口 (AI 检索核心)
                    CREATE TABLE IF NOT EXISTS dispatch (
                        id SERIAL PRIMARY KEY,
                        node_address VARCHAR(512) REFERENCES nodes(address) ON DELETE CASCADE,
                        scenario_tags JSONB DEFAULT '[]',
                        capability_label VARCHAR(255),
                        weight INTEGER DEFAULT 50,
                        UNIQUE(node_address)
                    );

                    -- 4. AI 约束与样板
                    CREATE TABLE IF NOT EXISTS specimens (
                        id SERIAL PRIMARY KEY,
                        dispatch_id INTEGER REFERENCES dispatch(id) ON DELETE CASCADE,
                        title VARCHAR(255),
                        snippet TEXT,
                        ai_constraints TEXT
                    );
                """)

                # --- 第三阶段：解析并注入数据 ---
                print(f"📦 第3步：正在解析 JSON 并映射到物理资产...")
                with open(json_path, 'r', encoding='utf-8') as f:
                    nodes_data = json.load(f)

                for node in nodes_data:
                    addr = node['address']

                    # A. 注入 nodes 表
                    cur.execute("""
                        INSERT INTO nodes (address, kind, description, raw_json)
                        VALUES (%s, %s, %s, %s) ON CONFLICT (address) DO UPDATE
                        SET description = EXCLUDED.description, raw_json = EXCLUDED.raw_json
                    """, (addr, node['kind'], node.get('javadoc', {}).get('description', ''), Json(node)))

                    # B. 注入 methods 表 (补全逻辑：遍历并存入方法数据)
                    if 'methods' in node and node['methods']:
                        for method in node['methods']:
                            cur.execute("""
                                INSERT INTO methods (node_address, full_address, calls, method_raw)
                                VALUES (%s, %s, %s, %s) ON CONFLICT (full_address) DO UPDATE
                                SET calls = EXCLUDED.calls, method_raw = EXCLUDED.method_raw
                            """, (
                                addr,
                                method['address'],
                                Json(method.get('calls', [])),
                                Json(method)
                            ))

                    # C. 注入 dispatch 表
                    cur.execute("""
                        INSERT INTO dispatch (node_address, capability_label)
                        VALUES (%s, %s) ON CONFLICT (node_address) DO NOTHING
                    """, (addr, addr.split('.')[-1]))

            dest_conn.close()
            print(f"✅ 工程闭环完成！版本库 {db_name} 资产已同步，methods 表已填满。")
        except Exception as e:
            print(f"❌ 资产注入失败: {e}")

# --- 执行入口 ---
if __name__ == "__main__":
    # 配置信息：请确保 Docker 映射端口为 5432
    DB_HOST = "127.0.0.1"
    DB_USER = "postgres"
    DB_PASS = "your_strong_password"

    # 你的 JSON 文件绝对路径
    JSON_SOURCE = "/Users/mingxilv/WebDevelopment/gitcode/dev-proj/framework_source/redisson/glossary-redisson/Universe_Export_3_24_0.json"

    engine = ArchitectureEngine(DB_HOST, DB_USER, DB_PASS)
    engine.build_version_castle("redisson", "3.24.0", JSON_SOURCE)