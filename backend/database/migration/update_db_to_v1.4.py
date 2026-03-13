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
print("开始更新数据库表结构到技术文档V1.4标准")
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

try:
    print("\n1. 删除现有表...")
    tables_to_drop = [
        "user_settings",
        "refresh_tokens",
        "verification_codes",
        "recognition_records",
        "app_versions",
        "users"
    ]

    for table in tables_to_drop:
        try:
            cursor.execute(f"DROP TABLE IF EXISTS {table}")
            print(f"   ✓ 已删除表: {table}")
        except Exception as e:
            print(f"   ✗ 删除表 {table} 失败: {e}")

    print("\n2. 创建新表（按技术文档V1.4）...")

    create_users_table = """
    CREATE TABLE users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        uid VARCHAR(50) UNIQUE,
        username VARCHAR(255) NOT NULL,
        email VARCHAR(255),
        phone_number VARCHAR(20) UNIQUE,
        nickname VARCHAR(255),
        avatar_path VARCHAR(500),
        login_type ENUM('phone', 'guest') DEFAULT 'guest',
        cloud_sync_switch BOOLEAN DEFAULT FALSE,
        privacy_agreed BOOLEAN DEFAULT FALSE,
        privacy_agreed_version VARCHAR(20),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        last_login TIMESTAMP NULL,
        INDEX idx_uid (uid),
        INDEX idx_phone (phone_number)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    """

    create_recognition_records_table = """
    CREATE TABLE recognition_records (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL,
        fruit_veg_name VARCHAR(255) NOT NULL,
        confidence DECIMAL(5,4) NOT NULL,
        class_id INT NOT NULL,
        image_path VARCHAR(500) NOT NULL,
        image_url VARCHAR(500),
        nutrition_data JSON,
        is_collected BOOLEAN DEFAULT FALSE,
        recognition_type ENUM('local', 'cloud') DEFAULT 'local',
        sync_status ENUM('pending', 'synced', 'failed') DEFAULT 'pending',
        deleted BOOLEAN DEFAULT FALSE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        INDEX idx_user_id (user_id),
        INDEX idx_created_at (created_at),
        INDEX idx_is_collected (is_collected),
        INDEX idx_sync_status (sync_status),
        INDEX idx_user_updated (user_id, updated_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    """

    create_user_settings_table = """
    CREATE TABLE user_settings (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL UNIQUE,
        auto_save BOOLEAN DEFAULT TRUE,
        language VARCHAR(20) DEFAULT 'zh-CN',
        cloud_sync BOOLEAN DEFAULT FALSE,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    """

    create_verification_codes_table = """
    CREATE TABLE verification_codes (
        id INT AUTO_INCREMENT PRIMARY KEY,
        phone_number VARCHAR(20) NOT NULL,
        code VARCHAR(10) NOT NULL,
        expires_at TIMESTAMP NOT NULL,
        is_used BOOLEAN DEFAULT FALSE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_phone (phone_number),
        INDEX idx_expires (expires_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    """

    create_refresh_tokens_table = """
    CREATE TABLE refresh_tokens (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL,
        token VARCHAR(500) NOT NULL UNIQUE,
        expires_at TIMESTAMP NOT NULL,
        revoked BOOLEAN DEFAULT FALSE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        INDEX idx_user_id (user_id),
        INDEX idx_token (token),
        INDEX idx_expires (expires_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    """

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

    tables = [
        ("users", create_users_table),
        ("recognition_records", create_recognition_records_table),
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

    print("\n3. 插入初始版本数据...")
    insert_versions = """
    INSERT INTO app_versions (version, platform, download_url, update_log, force_update) VALUES
    ('1.0.0', 'android', 'https://example.com/app-v1.0.0.apk', '初始版本发布', 0),
    ('1.0.1', 'ios', 'https://example.com/app-v1.0.1.ipa', '初始版本发布', 0),
    ('1.1.0', 'android', 'https://example.com/app-v1.1.0.apk', '新增用户设置功能', 0),
    ('1.1.1', 'ios', 'https://example.com/app-v1.1.1.ipa', '新增用户设置功能', 0),
    ('1.2.0', 'android', 'https://example.com/app-v1.2.0.apk', '修复已知问题，优化性能', 1)
    """
    try:
        cursor.execute(insert_versions)
        conn.commit()
        print("   ✓ 初始版本数据已插入")
    except Exception as e:
        print(f"   ✗ 插入版本数据失败: {e}")

    print("\n4. 创建测试用户...")
    test_users = [
        {
            'username': '测试用户1',
            'uid': '12345678',
            'phone_number': '13800138001',
            'login_type': 'phone',
            'privacy_agreed': True,
            'privacy_agreed_version': '1.0'
        },
        {
            'username': '游客用户1',
            'uid': '87654321',
            'login_type': 'guest',
            'privacy_agreed': True,
            'privacy_agreed_version': '1.0'
        }
    ]

    for user in test_users:
        try:
            insert_user = """
            INSERT INTO users (username, uid, phone_number, login_type, privacy_agreed, privacy_agreed_version)
            VALUES (%s, %s, %s, %s, %s, %s)
            """
            cursor.execute(insert_user, (
                user['username'],
                user['uid'],
                user.get('phone_number'),
                user['login_type'],
                user['privacy_agreed'],
                user['privacy_agreed_version']
            ))
            print(f"   ✓ 创建用户: {user['username']}")
        except Exception as e:
            print(f"   ✗ 创建用户 {user['username']} 失败: {e}")

    conn.commit()

    print("\n5. 为测试用户创建设置...")
    cursor.execute("SELECT id, username FROM users")
    users = cursor.fetchall()
    
    for user in users:
        try:
            insert_settings = """
            INSERT INTO user_settings (user_id, auto_save, language, cloud_sync)
            VALUES (%s, %s, %s, %s)
            """
            cursor.execute(insert_settings, (user['id'], True, 'zh-CN', False))
            print(f"   ✓ 为 {user['username']} 创建设置")
        except Exception as e:
            print(f"   ✗ 为 {user['username']} 创建设置失败: {e}")

    conn.commit()

    print("\n6. 验证表结构...")

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
            print(f"   │ {col['Field']:25} {col['Type']:20} {null:6} {key:8} {default}")
        
        print(f"   └─────────────────────────────────────────────────────")

    print("\n" + "=" * 60)
    print("数据库表结构更新完成！符合技术文档V1.4标准")
    print("=" * 60)

except Exception as e:
    print(f"\n错误: {e}")
    conn.rollback()
finally:
    cursor.close()
    conn.close()
