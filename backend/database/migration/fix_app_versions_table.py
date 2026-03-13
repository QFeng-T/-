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
print("修复 app_versions 表")
print("=" * 60)

conn = pymysql.connect(
    host=DB_HOST,
    port=DB_PORT,
    user=DB_USER,
    password=DB_PASSWORD,
    database=DB_NAME
)
cursor = conn.cursor()

print("\n1. 删除旧表...")
try:
    cursor.execute("DROP TABLE IF EXISTS app_versions")
    print("   ✓ 旧表已删除")
except Exception as e:
    print(f"   ✗ 删除旧表失败: {e}")

print("\n2. 创建新表...")
try:
    create_table_sql = """
    CREATE TABLE app_versions (
        id INT AUTO_INCREMENT PRIMARY KEY,
        platform VARCHAR(20) NOT NULL,
        version VARCHAR(20) NOT NULL,
        build_number INT NOT NULL,
        is_required BOOLEAN DEFAULT FALSE,
        download_url VARCHAR(500),
        release_notes TEXT,
        released_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_platform_version (platform, version)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    """
    cursor.execute(create_table_sql)
    print("   ✓ 新表创建成功")
except Exception as e:
    print(f"   ✗ 创建新表失败: {e}")

print("\n3. 添加初始版本信息...")
try:
    insert_version_sql = """
    INSERT INTO app_versions (platform, version, build_number, is_required, download_url, release_notes) 
    VALUES 
    ('android', '1.0.0', 1, FALSE, 'https://example.com/app-v1.0.0.apk', '初始版本'),
    ('ios', '1.0.0', 1, FALSE, 'https://example.com/app-v1.0.0.ipa', '初始版本')
    """
    cursor.execute(insert_version_sql)
    conn.commit()
    print("   ✓ 初始版本信息添加成功")
except Exception as e:
    print(f"   ✗ 添加版本信息失败: {e}")

cursor.close()
conn.close()

print("\n" + "=" * 60)
print("app_versions 表修复完成！")
print("=" * 60)
