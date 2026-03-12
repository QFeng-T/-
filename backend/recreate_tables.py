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
print("开始重新创建数据库表")
print("=" * 60)

conn = pymysql.connect(
    host=DB_HOST,
    port=DB_PORT,
    user=DB_USER,
    password=DB_PASSWORD,
    database=DB_NAME,
    cursorclass=pymysql.cursors.DictCursor
)
cursor = conn.cursor()

print("\n1. 删除现有表...")
tables_to_drop = [
    "app_versions",
    "user_settings",
    "verification_codes",
    "refresh_tokens",
    "recognition_records",
    "users"
]

for table in tables_to_drop:
    try:
        cursor.execute(f"DROP TABLE IF EXISTS {table}")
        print(f"   ✓ 已删除表: {table}")
    except Exception as e:
        print(f"   ✗ 删除表 {table} 失败: {e}")

print("\n2. 创建新表...")

create_users_table = """
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    uid VARCHAR(255),
    avatar_path VARCHAR(500),
    login_type VARCHAR(50) DEFAULT 'email',
    privacy_agreed BOOLEAN DEFAULT TRUE,
    privacy_agreed_version VARCHAR(20) DEFAULT '1.0',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY idx_email (email),
    UNIQUE KEY idx_phone (phone),
    UNIQUE KEY idx_uid (uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
"""

create_predictions_table = """
CREATE TABLE recognition_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    result JSON NOT NULL,
    is_collected BOOLEAN DEFAULT FALSE,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_user_updated (user_id, updated_at),
    INDEX idx_deleted (deleted),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
"""

create_refresh_tokens_table = """
CREATE TABLE refresh_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_token (token),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
"""

create_verification_codes_table = """
CREATE TABLE verification_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(50) NOT NULL,
    code VARCHAR(10) NOT NULL,
    type VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_phone (phone),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
"""

create_user_settings_table = """
CREATE TABLE user_settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    auto_sync BOOLEAN DEFAULT TRUE,
    sync_on_wifi_only BOOLEAN DEFAULT FALSE,
    notification_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
"""

create_app_versions_table = """
CREATE TABLE app_versions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    platform VARCHAR(20) NOT NULL,
    version VARCHAR(20) NOT NULL,
    build_number INT NOT NULL,
    is_required BOOLEAN DEFAULT FALSE,
    download_url VARCHAR(500),
    release_notes TEXT,
    released_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_platform_version (platform, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
"""

tables = [
    ("users", create_users_table),
    ("recognition_records", create_predictions_table),
    ("refresh_tokens", create_refresh_tokens_table),
    ("verification_codes", create_verification_codes_table),
    ("user_settings", create_user_settings_table),
    ("app_versions", create_app_versions_table)
]

for table_name, table_sql in tables:
    try:
        cursor.execute(table_sql)
        print(f"   ✓ 已创建表: {table_name}")
    except Exception as e:
        print(f"   ✗ 创建表 {table_name} 失败: {e}")

conn.commit()

print("\n3. 验证表结构...")

for table_name, _ in tables:
    print(f"\n   ┌─────────────────────────────────────────────────────")
    print(f"   │ 表名: {table_name}")
    print(f"   ├─────────────────────────────────────────────────────")
    
    cursor.execute(f"DESCRIBE {table_name}")
    columns = cursor.fetchall()
    
    print(f"   │ {'字段名':<25} {'类型':<20} {'NULL':<6} {'KEY':<8} {'默认值'}")
    print(f"   ├─────────────────────────────────────────────────────")
    
    for col in columns:
        null = "YES" if col['Null'] == 'YES' else "NO"
        key = col['Key'] if col['Key'] else ""
        default = str(col['Default']) if col['Default'] is not None else ""
        print(f"   │ {col['Field']:<25} {col['Type']:<20} {null:<6} {key:<8} {default}")
    
    print(f"   └─────────────────────────────────────────────────────")

print("\n4. 查看索引信息...")
for table_name, _ in tables:
    cursor.execute(f"SHOW INDEX FROM {table_name}")
    indexes = cursor.fetchall()
    
    if indexes:
        print(f"\n   表: {table_name}")
        for idx in indexes:
            key_type = "UNIQUE" if idx['Non_unique'] == 0 else "INDEX"
            print(f"     - {idx['Key_name']: <20} ({key_type}) 列: {idx['Column_name']}")

cursor.close()
conn.close()

print("\n" + "=" * 60)
print("所有表创建完成！")
print("=" * 60)
