import pymysql
from dotenv import load_dotenv
import os

load_dotenv()

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", 3306))
DB_USER = os.getenv("DB_USER", "root")
DB_PASSWORD = os.getenv("DB_PASSWORD", "")
DB_NAME = os.getenv("DB_NAME", "freshid")

print("=" * 60)
print("添加初始数据")
print("=" * 60)

conn = pymysql.connect(
    host=DB_HOST,
    port=DB_PORT,
    user=DB_USER,
    password=DB_PASSWORD,
    database=DB_NAME
)
cursor = conn.cursor()

print("\n1. 添加初始版本信息...")
try:
    insert_version_sql = """
    INSERT INTO app_versions (platform, version, build_number, is_required, download_url, release_notes) 
    VALUES 
    ('android', '1.0.0', 1, FALSE, 'https://example.com/app-v1.0.0.apk', '初始版本'),
    ('ios', '1.0.0', 1, FALSE, 'https://example.com/app-v1.0.0.ipa', '初始版本')
    ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP
    """
    cursor.execute(insert_version_sql)
    conn.commit()
    print("   ✓ 初始版本信息添加成功")
except Exception as e:
    print(f"   ✗ 添加版本信息失败: {e}")

cursor.close()
conn.close()

print("\n" + "=" * 60)
print("初始数据添加完成！")
print("=" * 60)
