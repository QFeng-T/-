"""
FreshID API 模块
包含 Android 应用和管理后台的 API 路由
"""

from .android_routes import android_router
from .admin_routes import admin_router

__all__ = ["android_router", "admin_router"]
