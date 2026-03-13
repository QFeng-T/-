import mysql.connector
from mysql.connector import Error
import os
import json
from datetime import datetime
from dotenv import load_dotenv
from src.models.schemas import UserCreate

load_dotenv()

class DataService:
    def __init__(self):
        self.connection = None
        self.cursor = None
        self.connect()
    
    def connect(self):
        try:
            self.connection = mysql.connector.connect(
                host=os.getenv("DB_HOST", "localhost"),
                user=os.getenv("DB_USER", "root"),
                password=os.getenv("DB_PASSWORD", ""),
                database=os.getenv("DB_NAME", "freshid")
            )
            self.cursor = self.connection.cursor(dictionary=True)
            print("数据库连接成功")
            self.create_tables()
        except Error as e:
            print(f"数据库连接失败: {e}")
            self.connection = None
            self.memory_storage = {
                "users": [],
                "predictions": [],
                "refresh_tokens": [],
                "verification_codes": []
            }
    
    def create_tables(self):
        create_users_table = """
        CREATE TABLE IF NOT EXISTS users (
            id INT AUTO_INCREMENT PRIMARY KEY,
            username VARCHAR(255) NOT NULL,
            email VARCHAR(255),
            phone VARCHAR(50),
            uid VARCHAR(255),
            login_type VARCHAR(50) DEFAULT 'email',
            privacy_agreed BOOLEAN DEFAULT TRUE,
            privacy_agreed_version VARCHAR(20) DEFAULT '1.0',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            UNIQUE KEY idx_email (email),
            UNIQUE KEY idx_phone (phone),
            UNIQUE KEY idx_uid (uid)
        )
        """
        
        create_predictions_table = """
        CREATE TABLE IF NOT EXISTS recognition_records (
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
        )
        """
        
        create_refresh_tokens_table = """
        CREATE TABLE IF NOT EXISTS refresh_tokens (
            id INT AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL,
            token VARCHAR(500) NOT NULL,
            expires_at TIMESTAMP NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_user_id (user_id),
            INDEX idx_token (token),
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )
        """
        
        create_verification_codes_table = """
        CREATE TABLE IF NOT EXISTS verification_codes (
            id INT AUTO_INCREMENT PRIMARY KEY,
            phone VARCHAR(50) NOT NULL,
            code VARCHAR(10) NOT NULL,
            type VARCHAR(20) NOT NULL,
            expires_at TIMESTAMP NOT NULL,
            used BOOLEAN DEFAULT FALSE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_phone (phone),
            INDEX idx_expires_at (expires_at)
        )
        """
        
        create_user_settings_table = """
        CREATE TABLE IF NOT EXISTS user_settings (
            id INT AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL UNIQUE,
            auto_sync BOOLEAN DEFAULT TRUE,
            sync_on_wifi_only BOOLEAN DEFAULT FALSE,
            notification_enabled BOOLEAN DEFAULT TRUE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )
        """
        
        create_app_versions_table = """
        CREATE TABLE IF NOT EXISTS app_versions (
            id INT AUTO_INCREMENT PRIMARY KEY,
            platform VARCHAR(20) NOT NULL,
            version VARCHAR(20) NOT NULL,
            build_number INT NOT NULL,
            is_required BOOLEAN DEFAULT FALSE,
            download_url VARCHAR(500),
            release_notes TEXT,
            released_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_platform_version (platform, version)
        )
        """
        
        try:
            self.cursor.execute(create_users_table)
            self.cursor.execute(create_predictions_table)
            self.cursor.execute(create_refresh_tokens_table)
            self.cursor.execute(create_verification_codes_table)
            self.cursor.execute(create_user_settings_table)
            self.cursor.execute(create_app_versions_table)
            self.connection.commit()
            print("表创建成功")
        except Error as e:
            print(f"表创建失败: {e}")
    
    async def create_user(self, user: UserCreate) -> int:
        if self.connection is not None:
            try:
                query = """
                INSERT INTO users (username, email, phone, login_type, 
                                   privacy_agreed, privacy_agreed_version) 
                VALUES (%s, %s, %s, %s, %s, %s)
                """
                values = (user.username, user.email, user.phone, user.login_type,
                         user.privacy_agreed, user.privacy_agreed_version)
                self.cursor.execute(query, values)
                self.connection.commit()
                return self.cursor.lastrowid
            except Error as e:
                print(f"创建用户失败: {e}")
                return self._create_user_in_memory(user)
        else:
            return self._create_user_in_memory(user)
    
    async def get_user(self, user_id: int) -> dict:
        if self.connection is not None:
            try:
                query = "SELECT * FROM users WHERE id = %s"
                self.cursor.execute(query, (user_id,))
                user = self.cursor.fetchone()
                return user
            except Error as e:
                print(f"获取用户失败: {e}")
                return self._get_user_in_memory(user_id)
        else:
            return self._get_user_in_memory(user_id)
    
    async def save_prediction(self, prediction: dict, file_path: str, user_id: int = 1) -> int:
        if self.connection is not None:
            try:
                query = """
                INSERT INTO recognition_records (user_id, image_url, result) 
                VALUES (%s, %s, %s)
                """
                values = (user_id, file_path, json.dumps(prediction))
                self.cursor.execute(query, values)
                self.connection.commit()
                return self.cursor.lastrowid
            except Error as e:
                print(f"保存预测记录失败: {e}")
                return self._save_prediction_in_memory(prediction, file_path)
        else:
            return self._save_prediction_in_memory(prediction, file_path)
    
    async def get_predictions(self, user_id: int = 1, page: int = 1, limit: int = 20) -> dict:
        if self.connection is not None:
            try:
                offset = (page - 1) * limit
                count_query = "SELECT COUNT(*) as total FROM recognition_records WHERE user_id = %s AND deleted = FALSE"
                self.cursor.execute(count_query, (user_id,))
                total = self.cursor.fetchone()['total']
                
                query = """
                SELECT * FROM recognition_records 
                WHERE user_id = %s AND deleted = FALSE 
                ORDER BY created_at DESC 
                LIMIT %s OFFSET %s
                """
                self.cursor.execute(query, (user_id, limit, offset))
                predictions = self.cursor.fetchall()
                
                total_pages = (total + limit - 1) // limit if total > 0 else 0
                has_next = page < total_pages
                has_prev = page > 1
                
                return {
                    "records": predictions,
                    "pagination": {
                        "page": page,
                        "limit": limit,
                        "total": total,
                        "total_pages": total_pages,
                        "has_next": has_next,
                        "has_prev": has_prev
                    }
                }
            except Error as e:
                print(f"获取预测记录失败: {e}")
                return {"records": self.memory_storage["predictions"], "pagination": None}
        else:
            return {"records": self.memory_storage["predictions"], "pagination": None}
    
    async def sync_download(self, user_id: int, since: datetime = None):
        if self.connection is not None:
            try:
                base_query = "SELECT * FROM recognition_records WHERE user_id = %s"
                params = [user_id]
                
                if since:
                    base_query += " AND updated_at >= %s"
                    params.append(since)
                
                self.cursor.execute(base_query, params)
                records = self.cursor.fetchall()
                
                deleted_query = "SELECT id FROM recognition_records WHERE user_id = %s AND deleted = TRUE"
                if since:
                    deleted_query += " AND updated_at >= %s"
                    self.cursor.execute(deleted_query, [user_id, since])
                else:
                    self.cursor.execute(deleted_query, [user_id])
                deleted_ids = [row['id'] for row in self.cursor.fetchall()]
                
                last_updated_query = "SELECT MAX(updated_at) as last_updated FROM recognition_records WHERE user_id = %s"
                self.cursor.execute(last_updated_query, [user_id])
                last_updated = self.cursor.fetchone()['last_updated']
                
                return {
                    "records": records,
                    "deleted_ids": deleted_ids,
                    "last_updated": last_updated
                }
            except Error as e:
                print(f"同步下载失败: {e}")
                return {"records": [], "deleted_ids": [], "last_updated": None}
        else:
            return {"records": [], "deleted_ids": [], "last_updated": None}
    
    async def soft_delete_record(self, record_id: int, user_id: int) -> bool:
        if self.connection is not None:
            try:
                query = "UPDATE recognition_records SET deleted = TRUE WHERE id = %s AND user_id = %s"
                self.cursor.execute(query, (record_id, user_id))
                self.connection.commit()
                return self.cursor.rowcount > 0
            except Error as e:
                print(f"软删除记录失败: {e}")
                return False
        return False
    
    async def save_refresh_token(self, user_id: int, token: str, expires_at: datetime) -> bool:
        if self.connection is not None:
            try:
                query = "INSERT INTO refresh_tokens (user_id, token, expires_at) VALUES (%s, %s, %s)"
                self.cursor.execute(query, (user_id, token, expires_at))
                self.connection.commit()
                return True
            except Error as e:
                print(f"保存 refresh token 失败: {e}")
                return False
        return False
    
    async def revoke_refresh_token(self, token: str) -> bool:
        if self.connection is not None:
            try:
                query = "DELETE FROM refresh_tokens WHERE token = %s"
                self.cursor.execute(query, (token,))
                self.connection.commit()
                return self.cursor.rowcount > 0
            except Error as e:
                print(f"撤销 refresh token 失败: {e}")
                return False
        return False
    
    async def revoke_user_refresh_tokens(self, user_id: int) -> bool:
        if self.connection is not None:
            try:
                query = "DELETE FROM refresh_tokens WHERE user_id = %s"
                self.cursor.execute(query, (user_id,))
                self.connection.commit()
                return True
            except Error as e:
                print(f"撤销用户 refresh token 失败: {e}")
                return False
        return False
    
    async def sync_upload(self, user_id: int, records: list, deleted_ids: list = None):
        if self.connection is not None:
            try:
                for record in records:
                    record_id = record.get('id')
                    client_updated_at = record.get('updated_at')
                    
                    if record_id:
                        check_query = "SELECT updated_at FROM recognition_records WHERE id = %s AND user_id = %s"
                        self.cursor.execute(check_query, (record_id, user_id))
                        existing = self.cursor.fetchone()
                        
                        if existing:
                            cloud_updated_at = existing['updated_at']
                            if client_updated_at and client_updated_at > cloud_updated_at:
                                update_query = """
                                UPDATE recognition_records 
                                SET image_url = %s, result = %s, is_collected = %s, 
                                    deleted = %s, updated_at = CURRENT_TIMESTAMP
                                WHERE id = %s AND user_id = %s
                                """
                                self.cursor.execute(update_query, (
                                    record.get('image_url'),
                                    json.dumps(record.get('result', {})),
                                    record.get('is_collected', False),
                                    record.get('deleted', False),
                                    record_id,
                                    user_id
                                ))
                        else:
                            insert_query = """
                            INSERT INTO recognition_records 
                            (user_id, image_url, result, is_collected, deleted)
                            VALUES (%s, %s, %s, %s, %s)
                            """
                            self.cursor.execute(insert_query, (
                                user_id,
                                record.get('image_url'),
                                json.dumps(record.get('result', {})),
                                record.get('is_collected', False),
                                record.get('deleted', False)
                            ))
                    else:
                        insert_query = """
                        INSERT INTO recognition_records 
                        (user_id, image_url, result, is_collected, deleted)
                        VALUES (%s, %s, %s, %s, %s)
                        """
                        self.cursor.execute(insert_query, (
                            user_id,
                            record.get('image_url'),
                            json.dumps(record.get('result', {})),
                            record.get('is_collected', False),
                            record.get('deleted', False)
                        ))
                
                if deleted_ids:
                    for record_id in deleted_ids:
                        delete_query = """
                        UPDATE recognition_records 
                        SET deleted = TRUE, updated_at = CURRENT_TIMESTAMP
                        WHERE id = %s AND user_id = %s
                        """
                        self.cursor.execute(delete_query, (record_id, user_id))
                
                self.connection.commit()
                return True
            except Error as e:
                print(f"同步上传失败: {e}")
                self.connection.rollback()
                return False
        return False
    
    def _create_user_in_memory(self, user: UserCreate) -> int:
        user_id = len(self.memory_storage["users"]) + 1
        user_data = {
            "id": user_id,
            "username": user.username,
            "email": user.email,
            "phone": user.phone,
            "login_type": user.login_type,
            "privacy_agreed": user.privacy_agreed,
            "privacy_agreed_version": user.privacy_agreed_version,
            "created_at": datetime.now().isoformat(),
            "updated_at": datetime.now().isoformat()
        }
        self.memory_storage["users"].append(user_data)
        return user_id
    
    def _get_user_in_memory(self, user_id: int) -> dict:
        for user in self.memory_storage["users"]:
            if user["id"] == user_id:
                return user
        return None
    
    def _save_prediction_in_memory(self, prediction: dict, file_path: str) -> int:
        prediction_id = len(self.memory_storage["predictions"]) + 1
        prediction_data = {
            "id": prediction_id,
            "user_id": 1,
            "image_url": file_path,
            "result": prediction,
            "is_collected": False,
            "deleted": False,
            "created_at": datetime.now().isoformat(),
            "updated_at": datetime.now().isoformat()
        }
        self.memory_storage["predictions"].append(prediction_data)
        return prediction_id
    
    async def create_guest_user(self, username: str, uid: str) -> dict:
        if self.connection is not None:
            try:
                query = """
                INSERT INTO users (username, uid, login_type, privacy_agreed, privacy_agreed_version)
                VALUES (%s, %s, %s, %s, %s)
                """
                values = (username, uid, 'guest', True, '1.0')
                self.cursor.execute(query, values)
                self.connection.commit()
                user_id = self.cursor.lastrowid
                
                return {
                    "id": user_id,
                    "uid": uid,
                    "username": username,
                    "login_type": "guest"
                }
            except Error as e:
                print(f"创建游客用户失败: {e}")
                return self._create_guest_user_in_memory(username, uid)
        else:
            return self._create_guest_user_in_memory(username, uid)
    
    async def get_user_by_phone(self, phone: str) -> dict:
        if self.connection is not None:
            try:
                query = "SELECT * FROM users WHERE phone = %s"
                self.cursor.execute(query, (phone,))
                return self.cursor.fetchone()
            except Error as e:
                print(f"通过手机号获取用户失败: {e}")
                return self._get_user_by_phone_in_memory(phone)
        else:
            return self._get_user_by_phone_in_memory(phone)
    
    async def save_verification_code_db(self, phone: str, code: str, code_type: str, expires_at: datetime) -> bool:
        if self.connection is not None:
            try:
                query = """
                INSERT INTO verification_codes (phone, code, type, expires_at)
                VALUES (%s, %s, %s, %s)
                """
                self.cursor.execute(query, (phone, code, code_type, expires_at))
                self.connection.commit()
                return True
            except Error as e:
                print(f"保存验证码到数据库失败: {e}")
                return False
        return False
    
    async def verify_verification_code_db(self, phone: str, code: str, code_type: str) -> bool:
        if self.connection is not None:
            try:
                query = """
                SELECT * FROM verification_codes 
                WHERE phone = %s AND code = %s AND type = %s 
                AND used = FALSE AND expires_at > %s
                """
                self.cursor.execute(query, (phone, code, code_type, datetime.now()))
                result = self.cursor.fetchone()
                
                if result:
                    update_query = "UPDATE verification_codes SET used = TRUE WHERE id = %s"
                    self.cursor.execute(update_query, (result['id'],))
                    self.connection.commit()
                    return True
                return False
            except Error as e:
                print(f"验证验证码失败: {e}")
                return False
        return False
    
    async def update_user_last_login(self, user_id: int) -> bool:
        if self.connection is not None:
            try:
                query = "UPDATE users SET updated_at = CURRENT_TIMESTAMP WHERE id = %s"
                self.cursor.execute(query, (user_id,))
                self.connection.commit()
                return True
            except Error as e:
                print(f"更新用户最后登录时间失败: {e}")
                return False
        return False
    
    def _create_guest_user_in_memory(self, username: str, uid: str) -> dict:
        user_id = len(self.memory_storage["users"]) + 1
        user_data = {
            "id": user_id,
            "uid": uid,
            "username": username,
            "login_type": "guest",
            "privacy_agreed": True,
            "privacy_agreed_version": "1.0",
            "created_at": datetime.now().isoformat(),
            "updated_at": datetime.now().isoformat()
        }
        self.memory_storage["users"].append(user_data)
        return user_data
    
    def _get_user_by_phone_in_memory(self, phone: str) -> dict:
        for user in self.memory_storage["users"]:
            if user.get("phone") == phone:
                return user
        return None
    
    async def get_all_users(self, page: int = 1, limit: int = 20, search: str = None) -> dict:
        if self.connection is not None:
            try:
                offset = (page - 1) * limit
                count_query = "SELECT COUNT(*) as total FROM users"
                params = []
                
                if search:
                    count_query += " WHERE username LIKE %s OR email LIKE %s OR phone LIKE %s"
                    search_term = f"%{search}%"
                    params.extend([search_term, search_term, search_term])
                
                self.cursor.execute(count_query, params)
                total = self.cursor.fetchone()['total']
                
                query = "SELECT * FROM users"
                if search:
                    query += " WHERE username LIKE %s OR email LIKE %s OR phone LIKE %s"
                query += " ORDER BY created_at DESC LIMIT %s OFFSET %s"
                
                params.extend([limit, offset])
                self.cursor.execute(query, params)
                users = self.cursor.fetchall()
                
                total_pages = (total + limit - 1) // limit if total > 0 else 0
                
                return {
                    "records": users,
                    "pagination": {
                        "page": page,
                        "limit": limit,
                        "total": total,
                        "total_pages": total_pages
                    }
                }
            except Error as e:
                print(f"获取用户列表失败: {e}")
                return {"records": [], "pagination": None}
        return {"records": [], "pagination": None}
    
    async def update_user(self, user_id: int, **kwargs) -> bool:
        if self.connection is not None:
            try:
                update_fields = []
                values = []
                
                for key, value in kwargs.items():
                    if value is not None:
                        update_fields.append(f"{key} = %s")
                        values.append(value)
                
                if not update_fields:
                    return False
                
                values.append(user_id)
                query = f"UPDATE users SET {', '.join(update_fields)} WHERE id = %s"
                self.cursor.execute(query, values)
                self.connection.commit()
                return self.cursor.rowcount > 0
            except Error as e:
                print(f"更新用户失败: {e}")
                return False
        return False
    
    async def delete_user(self, user_id: int) -> bool:
        if self.connection is not None:
            try:
                query = "DELETE FROM users WHERE id = %s"
                self.cursor.execute(query, (user_id,))
                self.connection.commit()
                return self.cursor.rowcount > 0
            except Error as e:
                print(f"删除用户失败: {e}")
                return False
        return False
    
    async def get_all_records(self, page: int = 1, limit: int = 20, user_id: int = None,
                              fruit_name: str = None, start_date: datetime = None,
                              end_date: datetime = None) -> dict:
        if self.connection is not None:
            try:
                offset = (page - 1) * limit
                count_query = "SELECT COUNT(*) as total FROM recognition_records WHERE 1=1"
                data_query = "SELECT * FROM recognition_records WHERE 1=1"
                params = []
                
                if user_id is not None:
                    count_query += " AND user_id = %s"
                    data_query += " AND user_id = %s"
                    params.append(user_id)
                
                if fruit_name:
                    count_query += " AND JSON_UNQUOTE(result->'$.label') LIKE %s"
                    data_query += " AND JSON_UNQUOTE(result->'$.label') LIKE %s"
                    params.append(f"%{fruit_name}%")
                
                if start_date:
                    count_query += " AND created_at >= %s"
                    data_query += " AND created_at >= %s"
                    params.append(start_date)
                
                if end_date:
                    count_query += " AND created_at <= %s"
                    data_query += " AND created_at <= %s"
                    params.append(end_date)
                
                self.cursor.execute(count_query, params)
                total = self.cursor.fetchone()['total']
                
                data_query += " ORDER BY created_at DESC LIMIT %s OFFSET %s"
                params.extend([limit, offset])
                self.cursor.execute(data_query, params)
                records = self.cursor.fetchall()
                
                total_pages = (total + limit - 1) // limit if total > 0 else 0
                
                return {
                    "records": records,
                    "pagination": {
                        "page": page,
                        "limit": limit,
                        "total": total,
                        "total_pages": total_pages
                    }
                }
            except Error as e:
                print(f"获取记录列表失败: {e}")
                return {"records": [], "pagination": None}
        return {"records": [], "pagination": None}
    
    async def admin_delete_record(self, record_id: int) -> bool:
        if self.connection is not None:
            try:
                query = "DELETE FROM recognition_records WHERE id = %s"
                self.cursor.execute(query, (record_id,))
                self.connection.commit()
                return self.cursor.rowcount > 0
            except Error as e:
                print(f"删除记录失败: {e}")
                return False
        return False
    
    async def batch_delete_records(self, ids: list[int]) -> bool:
        if self.connection is not None:
            try:
                if not ids:
                    return True
                placeholders = ','.join(['%s'] * len(ids))
                query = f"DELETE FROM recognition_records WHERE id IN ({placeholders})"
                self.cursor.execute(query, ids)
                self.connection.commit()
                return True
            except Error as e:
                print(f"批量删除记录失败: {e}")
                return False
        return False
    
    async def get_admin_stats_overview(self) -> dict:
        if self.connection is not None:
            try:
                self.cursor.execute("SELECT COUNT(*) as total FROM users")
                total_users = self.cursor.fetchone()['total']
                
                self.cursor.execute("SELECT COUNT(*) as total FROM recognition_records")
                total_records = self.cursor.fetchone()['total']
                
                self.cursor.execute("SELECT COUNT(*) as today FROM recognition_records WHERE DATE(created_at) = CURDATE()")
                today_records = self.cursor.fetchone()['today']
                
                self.cursor.execute("SELECT COUNT(*) as this_week FROM recognition_records WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)")
                week_records = self.cursor.fetchone()['this_week']
                
                return {
                    "total_users": total_users,
                    "total_records": total_records,
                    "today_records": today_records,
                    "week_records": week_records
                }
            except Error as e:
                print(f"获取统计概览失败: {e}")
                return {}
        return {}
    
    async def get_admin_stats_trend(self, days: int = 7) -> list:
        if self.connection is not None:
            try:
                query = """
                SELECT 
                    DATE(created_at) as date,
                    COUNT(*) as count
                FROM recognition_records
                WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL %s DAY)
                GROUP BY DATE(created_at)
                ORDER BY date
                """
                self.cursor.execute(query, (days,))
                return self.cursor.fetchall()
            except Error as e:
                print(f"获取趋势数据失败: {e}")
                return []
        return []
    
    async def get_fruit_distribution(self) -> list:
        if self.connection is not None:
            try:
                query = """
                SELECT 
                    JSON_UNQUOTE(result->'$.label') as fruit_name,
                    COUNT(*) as count
                FROM recognition_records
                WHERE result IS NOT NULL
                GROUP BY JSON_UNQUOTE(result->'$.label')
                ORDER BY count DESC
                LIMIT 10
                """
                self.cursor.execute(query)
                return self.cursor.fetchall()
            except Error as e:
                print(f"获取果蔬分布失败: {e}")
                return []
        return []
    
    async def get_login_type_distribution(self) -> list:
        if self.connection is not None:
            try:
                query = """
                SELECT 
                    login_type,
                    COUNT(*) as count
                FROM users
                GROUP BY login_type
                """
                self.cursor.execute(query)
                return self.cursor.fetchall()
            except Error as e:
                print(f"获取登录类型分布失败: {e}")
                return []
        return []
    
    async def get_models(self, page: int = 1, limit: int = 20) -> dict:
        if self.connection is not None:
            try:
                offset = (page - 1) * limit
                count_query = "SELECT COUNT(*) as total FROM app_versions"
                self.cursor.execute(count_query)
                total = self.cursor.fetchone()['total']
                
                query = "SELECT * FROM app_versions ORDER BY released_at DESC LIMIT %s OFFSET %s"
                self.cursor.execute(query, (limit, offset))
                models = self.cursor.fetchall()
                
                total_pages = (total + limit - 1) // limit if total > 0 else 0
                
                return {
                    "records": models,
                    "pagination": {
                        "page": page,
                        "limit": limit,
                        "total": total,
                        "total_pages": total_pages
                    }
                }
            except Error as e:
                print(f"获取模型列表失败: {e}")
                return {"records": [], "pagination": None}
        return {"records": [], "pagination": None}
    
    async def activate_model(self, model_id: int) -> bool:
        if self.connection is not None:
            try:
                self.cursor.execute("UPDATE app_versions SET is_required = FALSE WHERE is_required = TRUE")
                query = "UPDATE app_versions SET is_required = TRUE WHERE id = %s"
                self.cursor.execute(query, (model_id,))
                self.connection.commit()
                return self.cursor.rowcount > 0
            except Error as e:
                print(f"激活模型失败: {e}")
                return False
        return False
    
    async def delete_model(self, model_id: int) -> bool:
        if self.connection is not None:
            try:
                query = "DELETE FROM app_versions WHERE id = %s"
                self.cursor.execute(query, (model_id,))
                self.connection.commit()
                return self.cursor.rowcount > 0
            except Error as e:
                print(f"删除模型失败: {e}")
                return False
        return False
    
    def __del__(self):
        if self.cursor:
            self.cursor.close()
        if self.connection:
            self.connection.close()
