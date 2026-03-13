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
print("开始添加初始数据")
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
    print("\n1. 清空所有表...")
    cursor.execute("DELETE FROM user_settings")
    cursor.execute("DELETE FROM refresh_tokens")
    cursor.execute("DELETE FROM verification_codes")
    cursor.execute("DELETE FROM recognition_records")
    cursor.execute("DELETE FROM users")
    cursor.execute("DELETE FROM app_versions")
    print("   ✓ 已清空所有表")

    print("\n2. 插入初始版本数据...")
    insert_versions = """
    INSERT INTO app_versions (version, platform, download_url, update_log, force_update) VALUES
    ('1.0.0', 'android', 'https://example.com/app-v1.0.0.apk', '初始版本发布', 0),
    ('1.0.1', 'ios', 'https://example.com/app-v1.0.1.ipa', '初始版本发布', 0),
    ('1.1.0', 'android', 'https://example.com/app-v1.1.0.apk', '新增用户设置功能', 0),
    ('1.1.1', 'ios', 'https://example.com/app-v1.1.1.ipa', '新增用户设置功能', 0),
    ('1.2.0', 'android', 'https://example.com/app-v1.2.0.apk', '修复已知问题，优化性能', 1)
    """
    cursor.execute(insert_versions)
    conn.commit()
    print("   ✓ 初始版本数据已插入")

    print("\n3. 查看插入的版本数据...")
    cursor.execute("SELECT * FROM app_versions ORDER BY id")
    versions = cursor.fetchall()
    for v in versions:
        print(f"   - {v['platform']} v{v['version']} - {'强制更新' if v['force_update'] else '可选更新'}")

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

    print("\n6. 为测试用户创建设置...")
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

    print("\n" + "=" * 60)
    print("初始数据添加完成！")
    print("=" * 60)

except Exception as e:
    print(f"\n错误: {e}")
    conn.rollback()
finally:
    cursor.close()
    conn.close()
