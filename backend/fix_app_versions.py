
import pymysql
from dotenv import load_dotenv
import os

load_dotenv()

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", 3306))
DB_USER = os.getenv("DB_USER", "root")
DB_PASSWORD = os.getenv("DB_PASSWORD", "")
DB_NAME = os.getenv("DB_NAME", "freshid")

print("连接到数据库...")
conn = pymysql.connect(
    host=DB_HOST,
    port=DB_PORT,
    user=DB_USER,
    password=DB_PASSWORD,
    database=DB_NAME,
    cursorclass=pymysql.cursors.DictCursor
)
cursor = conn.cursor()

print("删除并重建 app_versions 表...")
cursor.execute("DROP TABLE IF EXISTS app_versions")

create_app_versions_table = """
CREATE TABLE app_versions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    version VARCHAR(20) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    download_url VARCHAR(500) NOT NULL,
    update_log TEXT,
    force_update BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY idx_platform_version (platform, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
"""

cursor.execute(create_app_versions_table)
conn.commit()
print("✓ app_versions 表重建完成")

print("\n插入初始版本数据...")
insert_versions = """
INSERT INTO app_versions (version, platform, download_url, update_log, force_update) VALUES
('1.0.0', 'android', 'https://example.com/app-v1.0.0.apk', '初始版本', FALSE),
('1.0.0', 'ios', 'https://example.com/app-v1.0.0.ipa', '初始版本', FALSE)
"""

try:
    cursor.execute(insert_versions)
    conn.commit()
    print("✓ 初始版本数据已插入")
    
    print("\n查询插入的数据:")
    cursor.execute("SELECT * FROM app_versions")
    versions = cursor.fetchall()
    for v in versions:
        print(f"  - {v['platform']} v{v['version']}: {v['update_log']}")
        
except Exception as e:
    print(f"✗ 插入失败: {e}")

cursor.close()
conn.close()
print("\n完成！")
