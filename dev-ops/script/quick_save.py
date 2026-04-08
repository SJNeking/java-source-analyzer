#!/usr/bin/env python3
"""
快速保存学习记录 - 带完整性验证
用法: python3 quick_save.py < json_file.json
"""
import json
import sys
import sqlite3
import requests

DB_PATH = "/Users/mingxilv/learn/s-pay-mall-ddd/.lingma/learning-log/data/learning-log.db"
API_URL = "http://localhost:8002/api/entries"

def main():
    # 1. 读取JSON文件
    if len(sys.argv) < 2:
        print("用法: python3 quick_save.py <json_file>")
        sys.exit(1)
    
    with open(sys.argv[1], 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    insight_len = len(data.get('insight', ''))
    print(f"📝 主题: {data['topic']}")
    print(f"📏 Insight长度: {insight_len}字符")
    
    # 2. 保存前检查
    if insight_len < 2500:
        print(f"⚠️  警告: insight不足2500字符!")
        resp = input("是否继续? (y/N): ")
        if resp.lower() != 'y':
            print("已取消")
            sys.exit(0)
    
    # 3. 调用API保存
    print("\n💾 正在保存...")
    response = requests.post(API_URL, json=data)
    
    if response.status_code != 200:
        print(f"❌ 保存失败: {response.status_code}")
        print(response.text)
        sys.exit(1)
    
    result = response.json()
    saved_id = result.get('id')
    print(f"✅ API返回成功, ID: {saved_id}")
    
    # 4. 验证数据库
    print("\n🔍 验证数据库中实际内容...")
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT LENGTH(insight) FROM learning_entries WHERE id=?", (saved_id,))
    row = cursor.fetchone()
    conn.close()
    
    if not row:
        print(f"❌ 数据库中未找到ID={saved_id}的记录")
        sys.exit(1)
    
    actual_len = row[0]
    print(f"   预期长度: {insight_len}字符")
    print(f"   实际长度: {actual_len}字符")
    
    if actual_len == insight_len:
        print(f"\n✅✅✅ 验证通过! 数据完整保存 ✅✅✅")
        sys.exit(0)
    else:
        print(f"\n❌❌❌ 验证失败! 数据被截断 ❌❌❌")
        print(f"   差异: {insight_len - actual_len}字符丢失")
        
        # 显示实际内容片段
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("SELECT substr(insight, 1, 150) FROM learning_entries WHERE id=?", (saved_id,))
        snippet = cursor.fetchone()[0]
        conn.close()
        print(f"\n   实际保存的内容:")
        print(f"   {snippet}...")
        sys.exit(1)

if __name__ == "__main__":
    main()
