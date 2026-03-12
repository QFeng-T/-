import aiomysql
from typing import Optional
from app.database import DatabaseConfig

class DatabasePool:
    """数据库连接池管理类"""
    _pool: Optional[aiomysql.Pool] = None
    
    @classmethod
    async def create_pool(cls) -> aiomysql.Pool:
        """创建连接池"""
        if cls._pool is None:
            cls._pool = await aiomysql.create_pool(
                host=DatabaseConfig.HOST,
                port=DatabaseConfig.PORT,
                user=DatabaseConfig.USER,
                password=DatabaseConfig.PASSWORD,
                db=DatabaseConfig.DATABASE,
                minsize=5,
                maxsize=DatabaseConfig.POOL_SIZE,
                pool_recycle=DatabaseConfig.POOL_RECYCLE,
                autocommit=False,
                cursorclass=aiomysql.DictCursor
            )
            print(f"数据库连接池创建成功，最大连接数: {DatabaseConfig.POOL_SIZE}")
        return cls._pool
    
    @classmethod
    async def close_pool(cls):
        """关闭连接池"""
        if cls._pool:
            cls._pool.close()
            await cls._pool.wait_closed()
            cls._pool = None
            print("数据库连接池已关闭")
    
    @classmethod
    def get_pool(cls) -> aiomysql.Pool:
        """获取连接池"""
        if cls._pool is None:
            raise RuntimeError("连接池未初始化，请先调用 create_pool()")
        return cls._pool
