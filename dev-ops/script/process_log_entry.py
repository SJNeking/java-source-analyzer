#!/usr/bin/env python3
"""
学习记录处理脚本 - 文件驱动模式

职责:
1. 读取 /tmp/log_entry_pending.json
2. 验证数据完整性
3. 保存到数据库
4. 验证实际保存结果
5. 归档文件到 /tmp/log_entry_archived/

设计原则:
- 确定性执行: 文件IO是原子操作
- 明确的状态转换: pending → archived (成功) 或 pending (失败)
- 可审计: 所有操作都有日志记录
"""

import json
import os
import sys
import sqlite3
import requests
from datetime import datetime
from pathlib import Path

# 配置
PENDING_FILE = "/tmp/log_entry_pending.json"
ARCHIVED_DIR = "/tmp/log_entry_archived"
DB_PATH = os.path.expanduser("~/learn/s-pay-mall-ddd/.lingma/learning-log/data/learning-log.db")
API_URL = "http://localhost:8002/api/entries"

# 确保归档目录存在
os.makedirs(ARCHIVED_DIR, exist_ok=True)


def log(message, level="INFO"):
    """统一的日志输出"""
    timestamp = datetime.now().strftime("%H:%M:%S")
    print(f"[{timestamp}] [{level}] {message}")


def read_pending_file():
    """读取待处理文件"""
    if not os.path.exists(PENDING_FILE):
        log(f"❌ 待处理文件不存在: {PENDING_FILE}", "ERROR")
        log("请先运行 /log 命令生成思考内容", "ERROR")
        sys.exit(1)
    
    try:
        with open(PENDING_FILE, 'r', encoding='utf-8') as f:
            data = json.load(f)
        log(f"📖 读取待处理文件: {PENDING_FILE}")
        return data
    except json.JSONDecodeError as e:
        log(f"❌ JSON解析失败: {e}", "ERROR")
        sys.exit(1)


def validate_data(data):
    """验证数据完整性"""
    errors = []
    warnings = []
    
    # 1. 检查必填字段
    required_fields = ['topic', 'insight', 'star_situation', 'star_task', 
                      'star_action', 'star_result', 'topic_tag_id']
    for field in required_fields:
        if not data.get(field):
            errors.append(f"缺少必填字段: {field}")
    
    # 2. 检查insight长度
    insight_length = len(data.get('insight', ''))
    if insight_length < 2500:
        errors.append(f"insight长度不足: {insight_length}字符 < 2500字符要求")
    else:
        log(f"✅ 数据验证通过: insight长度={insight_length}字符")
    
    # 3. 检查diagram格式
    diagram = data.get('diagram')
    if diagram:
        valid_starts = ['flowchart TD', 'flowchart LR', 'sequenceDiagram', 
                       'graph TD', 'graph LR']
        if not any(diagram.strip().startswith(s) for s in valid_starts):
            warnings.append("diagram格式不符合Mermaid规范")
    
    # 4. 检查project_tag_id类型
    if data.get('project_tag_id') == "null":
        warnings.append("project_tag_id应该是None而非字符串'null'")
        data['project_tag_id'] = None
    
    # 输出警告
    for warning in warnings:
        log(f"⚠️  {warning}", "WARNING")
    
    # 如果有错误,退出
    if errors:
        log("❌ 数据验证失败:", "ERROR")
        for error in errors:
            log(f"   - {error}", "ERROR")
        log(f"\n⚠️  文件保留在: {PENDING_FILE}", "WARNING")
        log("请修正后重新执行脚本", "WARNING")
        sys.exit(1)
    
    return True


def ensure_backend_running():
    """确保后端服务运行"""
    try:
        response = requests.get("http://localhost:8002/health", timeout=2)
        if response.status_code == 200:
            log("✅ 后端服务正常运行")
            return
    except:
        pass
    
    log("⚠️  后端服务未运行,尝试启动...", "WARNING")
    home_dir = os.path.expanduser("~")
    backend_path = f"{home_dir}/learn/s-pay-mall-ddd/.lingma/learning-log/backend"
    
    import subprocess
    subprocess.Popen(
        ["nohup", "python3", "main.py"],
        cwd=backend_path,
        stdout=open("/tmp/ll.log", "w"),
        stderr=subprocess.STDOUT,
        start_new_session=True
    )
    
    # 等待服务启动
    import time
    time.sleep(3)
    log("✅ 后端服务已启动")


def save_to_database(data):
    """保存到数据库并返回ID"""
    log("💾 正在保存到数据库...")
    
    try:
        response = requests.post(API_URL, json=data, timeout=10)
        
        if response.status_code == 200:
            result = response.json()
            saved_id = result.get('id')
            
            # 处理deduplicated情况
            if result.get('status') == 'skipped':
                log("⚠️  记录已存在(deduplicated),查询现有ID...", "WARNING")
                saved_id = query_existing_entry_id(data['topic'])
            
            if saved_id:
                log(f"✅ API返回成功, ID: {saved_id}")
                return saved_id
            else:
                log("❌ API返回成功但未获取到ID", "ERROR")
                return None
        else:
            log(f"❌ API返回错误: {response.status_code}", "ERROR")
            log(f"   {response.text}", "ERROR")
            return None
            
    except Exception as e:
        log(f"❌ 保存失败: {e}", "ERROR")
        return None


def query_existing_entry_id(topic):
    """查询已存在的记录ID"""
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM learning_entries WHERE topic=?", (topic,))
        row = cursor.fetchone()
        conn.close()
        
        if row:
            return row[0]
        return None
    except Exception as e:
        log(f"❌ 查询失败: {e}", "ERROR")
        return None


def verify_saved_content(saved_id, expected_insight_length):
    """验证数据库中实际保存的内容"""
    log("🔍 验证数据库中实际内容...")
    
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("""
            SELECT id, topic, LENGTH(insight) as insight_len, timestamp 
            FROM learning_entries 
            WHERE id=?
        """, (saved_id,))
        row = cursor.fetchone()
        conn.close()
        
        if not row:
            log(f"❌ 数据库中未找到ID={saved_id}的记录", "ERROR")
            return False
        
        db_id, db_topic, actual_len, db_timestamp = row
        
        log(f"   数据库记录:")
        log(f"   • ID: {db_id}")
        log(f"   • 主题: {db_topic[:50]}...")
        log(f"   • Insight长度: {actual_len}字符")
        log(f"   • 保存时间: {db_timestamp}")
        
        if actual_len == expected_insight_length:
            log(f"✅ 验证通过! 数据库中insight长度({actual_len})与输入一致")
            return True
        else:
            log(f"❌ 验证失败!", "ERROR")
            log(f"   预期长度: {expected_insight_length}字符", "ERROR")
            log(f"   实际长度: {actual_len}字符", "ERROR")
            log(f"   差异: {expected_insight_length - actual_len}字符", "ERROR")
            
            # 显示实际内容片段
            conn = sqlite3.connect(DB_PATH)
            cursor = conn.cursor()
            cursor.execute(
                "SELECT substr(insight, 1, 150) FROM learning_entries WHERE id=?", 
                (saved_id,)
            )
            snippet = cursor.fetchone()[0]
            conn.close()
            
            log(f"\n   实际保存的内容片段:", "ERROR")
            log(f"   {snippet}...", "ERROR")
            return False
            
    except Exception as e:
        log(f"❌ 验证过程出错: {e}", "ERROR")
        import traceback
        traceback.print_exc()
        return False


def archive_file(success):
    """归档文件"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    
    if success:
        # 成功: 移动到archived目录
        archived_file = os.path.join(ARCHIVED_DIR, f"{timestamp}_success.json")
        os.rename(PENDING_FILE, archived_file)
        log(f"📦 归档文件: {archived_file}")
    else:
        # 失败: 保留在pending,但添加.failed后缀
        failed_file = f"{PENDING_FILE}.failed"
        if os.path.exists(failed_file):
            os.remove(failed_file)
        os.rename(PENDING_FILE, failed_file)
        log(f"⚠️  失败文件保留: {failed_file}", "WARNING")
        log("请修正后重命名为 log_entry_pending.json 并重新执行", "WARNING")


def main():
    """主流程"""
    print("=" * 80)
    print("🚀 学习记录处理脚本 - 文件驱动模式")
    print("=" * 80)
    print()
    
    # Step 1: 读取文件
    data = read_pending_file()
    
    # Step 2: 验证数据
    validate_data(data)
    
    # Step 3: 确保后端运行
    ensure_backend_running()
    
    # Step 4: 保存到数据库
    saved_id = save_to_database(data)
    if not saved_id:
        log("❌ 保存失败,流程终止", "ERROR")
        archive_file(False)
        sys.exit(1)
    
    # Step 5: 验证实际保存的内容
    expected_length = len(data['insight'])
    verified = verify_saved_content(saved_id, expected_length)
    
    if not verified:
        log("❌ 验证失败,流程终止", "ERROR")
        archive_file(False)
        sys.exit(1)
    
    # Step 6: 归档文件
    archive_file(True)
    
    # 完成
    print()
    print("=" * 80)
    print("✅✅✅ 学习记录已成功保存并验证 ✅✅✅")
    print("=" * 80)
    print()
    print(f"📊 总结:")
    print(f"   • 主题: {data['topic']}")
    print(f"   • ID: {saved_id}")
    print(f"   • Insight长度: {expected_length}字符")
    print(f"   • 研究类型: {data.get('research_type', 'N/A')}")
    print(f"   • 归档文件: {os.path.join(ARCHIVED_DIR, '*_success.json')}")
    print()


if __name__ == "__main__":
    main()
