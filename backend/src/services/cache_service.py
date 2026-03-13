import os
import json
import time
from dotenv import load_dotenv

load_dotenv()

class CacheService:
    _instance = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialize()
        return cls._instance
    
    def _initialize(self):
        self.use_redis = False
        self.redis_client = None
        self.memory_cache = {}
        self.memory_ttl = {}
        
        # 尝试连接Redis
        self._try_connect_redis()
    
    def _try_connect_redis(self):
        try:
            import redis
            redis_url = os.getenv("REDIS_URL", "redis://localhost:6379/0")
            
            if redis_url:
                self.redis_client = redis.from_url(redis_url, decode_responses=True)
                self.redis_client.ping()
                self.use_redis = True
                print("Redis缓存服务已连接")
            else:
                print("未配置Redis，使用内存缓存")
        except ImportError:
            print("Redis Python客户端未安装，使用内存缓存")
        except Exception as e:
            print(f"Redis连接失败: {e}，使用内存缓存")
    
    def get(self, key):
        """获取缓存"""
        if self.use_redis and self.redis_client:
            try:
                data = self.redis_client.get(key)
                if data:
                    return json.loads(data)
            except Exception as e:
                print(f"Redis获取失败: {e}")
        else:
            if key in self.memory_cache:
                if key in self.memory_ttl:
                    if time.time() > self.memory_ttl[key]:
                        del self.memory_cache[key]
                        del self.memory_ttl[key]
                        return None
                return self.memory_cache[key]
        return None
    
    def set(self, key, value, ttl=300):
        """设置缓存，ttl单位：秒"""
        try:
            if self.use_redis and self.redis_client:
                self.redis_client.setex(key, ttl, json.dumps(value))
            else:
                self.memory_cache[key] = value
                self.memory_ttl[key] = time.time() + ttl
        except Exception as e:
            print(f"缓存设置失败: {e}")
    
    def delete(self, key):
        """删除缓存"""
        try:
            if self.use_redis and self.redis_client:
                self.redis_client.delete(key)
            else:
                if key in self.memory_cache:
                    del self.memory_cache[key]
                if key in self.memory_ttl:
                    del self.memory_ttl[key]
        except Exception as e:
            print(f"缓存删除失败: {e}")
    
    def delete_pattern(self, pattern):
        """按模式删除缓存"""
        try:
            if self.use_redis and self.redis_client:
                keys = self.redis_client.keys(pattern)
                if keys:
                    self.redis_client.delete(*keys)
            else:
                keys_to_delete = [k for k in self.memory_cache.keys() if pattern.replace('*', '') in k]
                for key in keys_to_delete:
                    self.delete(key)
        except Exception as e:
            print(f"批量缓存删除失败: {e}")
