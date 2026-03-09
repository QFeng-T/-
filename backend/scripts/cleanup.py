import os
import sys
import mysql.connector
from mysql.connector import Error
from dotenv import load_dotenv
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

load_dotenv()

def get_db_connection():
    try:
        connection = mysql.connector.connect(
            host=os.getenv("DB_HOST", "localhost"),
            user=os.getenv("DB_USER", "root"),
            password=os.getenv("DB_PASSWORD", ""),
            database=os.getenv("DB_NAME", "freshid")
        )
        return connection
    except Error as e:
        print(f"数据库连接失败: {e}")
        return None

def cleanup_expired_verification_codes(cursor):
    try:
        query = "DELETE FROM verification_codes WHERE expires_at < %s"
        cursor.execute(query, (datetime.now(),))
        return cursor.rowcount
    except Error as e:
        print(f"清理验证码失败: {e}")
        return 0

def cleanup_expired_refresh_tokens(cursor):
    try:
        query = "DELETE FROM refresh_tokens WHERE expires_at < %s"
        cursor.execute(query, (datetime.now(),))
        return cursor.rowcount
    except Error as e:
        print(f"清理 Refresh Token 失败: {e}")
        return 0

def main():
    print(f"[{datetime.now()}] 开始清理任务...")
    
    connection = get_db_connection()
    if not connection:
        print("无法连接到数据库，退出")
        return
    
    try:
        cursor = connection.cursor()
        
        codes_deleted = cleanup_expired_verification_codes(cursor)
        print(f"清理了 {codes_deleted} 条过期验证码")
        
        tokens_deleted = cleanup_expired_refresh_tokens(cursor)
        print(f"清理了 {tokens_deleted} 条过期 Refresh Token")
        
        connection.commit()
        print(f"[{datetime.now()}] 清理任务完成")
        
    except Exception as e:
        print(f"清理任务执行出错: {e}")
        connection.rollback()
    finally:
        if cursor:
            cursor.close()
        if connection:
            connection.close()

if __name__ == "__main__":
    main()
