#!/usr/bin/env python3
"""
增强版学习记录保存脚本 - 带完整性验证
使用方法:
  1. 从文件读取: python3 safe_save_log.py < input.json
  2. 从参数读取: python3 safe_save_log.py '{"topic":"..."}'
"""

import json
import sys
import os
import sqlite3
import requests

BACKEND_URL = "http://localhost:8002"
DB_PATH = os.path.expanduser("~/learn/s-pay-mall-ddd/.lingma/learning-log/data/learning-log.db")
AUTO_RECORD_SCRIPT = os.path.expanduser("~/learn/s-pay-mall-ddd/.lingma/learning-log/scripts/auto_record.py")

def validate_before_save(data):
    """保存前验证数据完整性"""
    issues = []
    
    # 1. 检查必填字段
    required_fields = ['topic', 'insight', 'star_situation', 'star_task', 'star_action', 'star_result']
    for field in required_fields:
        if not data.get(field):
            issues.append(f"缺少必填字段: {field}")
    
    # 2. 检查insight长度
    insight_length = len(data.get('insight', ''))
    if insight_length < 2500:
        issues.append(f"⚠️ 警告: insight长度仅{insight_length}字符,未达到2500字要求(当前:{insight_length})")
    
    # 3. 检查diagram格式
    diagram = data.get('diagram')
    if diagram:
        valid_starts = ['flowchart TD', 'flowchart LR', 'sequenceDiagram', 'graph TD', 'graph LR']
        if not any(diagram.strip().startswith(s) for s in valid_starts):
            issues.append("⚠️ Diagram格式不符合Mermaid规范")
    
    if issues:
        print("=" * 80)
        print("📋 数据验证报告:")
        print("=" * 80)
        for issue in issues:
            print(f"  • {issue}")
        print("=" * 80)
        
        # 如果有严重问题,询问是否继续
        critical_issues = [i for i in issues if not i.startswith("⚠️")]
        if critical_issues:
            response = input("\n❌ 存在严重问题,是否继续保存? (y/N): ")
            if response.lower() != 'y':
                print("已取消保存")
                return False
    
    return True

def save_with_verification(data):
    """保存并验证数据完整性"""
    print(f"\n📤 正在保存: {data['topic']}")
    print(f"   Insight长度: {len(data.get('insight', ''))}字符")
    
    # 调用原有的auto_record.py脚本
    import subprocess
    json_str = json.dumps(data, ensure_ascii=False)
    
    result = subprocess.run(
        ['python3', AUTO_RECORD_SCRIPT, json_str],
        capture_output=True,
        text=True
    )
    
    # 打印脚本输出
    print(result.stdout)
    if result.stderr:
        print("STDERR:", result.stderr)
    
    if result.returncode != 0:
        print(f"❌ 保存失败,退出码: {result.returncode}")
        return False
    
    # 从输出中提取ID
    saved_id = None
    for line in result.stdout.split('\n'):
        if 'ID:' in line:
            try:
                saved_id = int(line.split('ID:')[1].strip())
            except:
                pass
    
    if not saved_id:
        print("⚠️ 无法获取保存的ID,跳过验证")
        return True
    
    # 端到端验证: 查询数据库
    print(f"\n🔍 正在验证数据库中的实际内容...")
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
        
        if row:
            db_id, db_topic, db_insight_len, db_timestamp = row
            expected_len = len(data['insight'])
            
            print(f"   数据库记录:")
            print(f"   • ID: {db_id}")
            print(f"   • 主题: {db_topic[:50]}...")
            print(f"   • Insight长度: {db_insight_len}字符")
            print(f"   • 保存时间: {db_timestamp}")
            
            if db_insight_len == expected_len:
                print(f"\n✅ 验证通过! 数据库中insight长度({db_insight_len})与输入一致")
                return True
            else:
                print(f"\n❌ 验证失败!")
                print(f"   预期长度: {expected_len}字符")
                print(f"   实际长度: {db_insight_len}字符")
                print(f"   差异: {expected_len - db_insight_len}字符")
                print(f"\n⚠️ 可能存在数据截断,建议手动检查数据库")
                
                # 显示实际保存的内容片段
                conn = sqlite3.connect(DB_PATH)
                cursor = conn.cursor()
                cursor.execute("SELECT substr(insight, 1, 200) FROM learning_entries WHERE id=?", (saved_id,))
                snippet = cursor.fetchone()[0]
                conn.close()
                print(f"\n   实际保存的内容片段:")
                print(f"   {snippet[:100]}...")
                
                return False
        else:
            print(f"❌ 数据库中未找到ID={saved_id}的记录")
            return False
            
    except Exception as e:
        print(f"❌ 验证过程出错: {e}")
        import traceback
        traceback.print_exc()
        return False

def main():
    # 1. 读取输入
    if len(sys.argv) > 1:
        # 从命令行参数读取
        json_str = sys.argv[1]
    else:
        # 从标准输入读取
        print("请粘贴JSON数据(以Ctrl+D结束):")
        json_str = sys.stdin.read()
    
    # 2. 解析JSON
    try:
        data = json.loads(json_str)
    except json.JSONDecodeError as e:
        print(f"❌ JSON解析失败: {e}")
        sys.exit(1)
    
    # 3. 保存前验证
    if not validate_before_save(data):
        sys.exit(1)
    
    # 4. 保存并验证
    success = save_with_verification(data)
    
    # 5. 退出码
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
