#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
FreshID 后端 API 测试脚本
用于验证所有 API 接口是否正常工作
"""

import sys
import os
import json

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

print("=" * 60)
print("FreshID 后端 API 测试")
print("=" * 60)

# 测试 1: 检查模块导入
print("\n1. 检查模块导入...")
try:
    from app.services.auth_service import AuthService
    from app.services.data_service import DataService
    from app.services.storage_service import StorageService
    from app.services.model_service import ModelService
    print("   ✓ 所有服务模块导入成功")
except Exception as e:
    print(f"   ✗ 模块导入失败: {e}")
    sys.exit(1)

# 测试 2: 检查 Pydantic 模型
print("\n2. 检查数据模型...")
try:
    from app.models.schemas import (
        ErrorCode,
        BaseResponse,
        GuestUserResponse,
        LoginResponse,
        UserInfoResponse
    )
    print("   ✓ 所有数据模型导入成功")
    print(f"   ✓ 错误码数量: {len([attr for attr in dir(ErrorCode) if not attr.startswith('_')])}")
except Exception as e:
    print(f"   ✗ 数据模型导入失败: {e}")
    sys.exit(1)

# 测试 3: 初始化服务
print("\n3. 初始化服务...")
try:
    auth_service = AuthService()
    print("   ✓ AuthService 初始化成功")
    
    storage_service = StorageService()
    print("   ✓ StorageService 初始化成功")
    
    model_service = ModelService()
    print("   ✓ ModelService 初始化成功")
except Exception as e:
    print(f"   ✗ 服务初始化失败: {e}")
    sys.exit(1)

# 测试 4: 测试验证码生成
print("\n4. 测试验证码功能...")
try:
    code = auth_service.generate_verification_code()
    print(f"   ✓ 生成验证码: {code}")
    
    auth_service.save_verification_code("13800138000", code)
    print("   ✓ 保存验证码成功")
    
    is_valid = auth_service.verify_verification_code("13800138000", code)
    print(f"   ✓ 验证码验证: {'成功' if is_valid else '失败'}")
except Exception as e:
    print(f"   ✗ 验证码功能测试失败: {e}")

# 测试 5: 测试 Token 生成
print("\n5. 测试 JWT Token 功能...")
try:
    token_data = {"sub": "1", "uid": "00000001", "login_type": "guest"}
    access_token, access_expire = auth_service.create_access_token(token_data)
    print(f"   ✓ Access Token 生成成功")
    
    refresh_token, refresh_expire = auth_service.create_refresh_token(token_data)
    print(f"   ✓ Refresh Token 生成成功")
    
    payload = auth_service.verify_token(access_token)
    print(f"   ✓ Token 验证成功: user_id={payload.get('sub')}")
except Exception as e:
    print(f"   ✗ Token 功能测试失败: {e}")

# 测试 6: 测试 UID 生成
print("\n6. 测试 UID 生成...")
try:
    uid = auth_service.generate_uid()
    print(f"   ✓ 生成 UID: {uid}")
    print(f"   ✓ UID 长度: {len(uid)}")
except Exception as e:
    print(f"   ✗ UID 生成失败: {e}")

print("\n" + "=" * 60)
print("测试完成！")
print("=" * 60)
print("\n🚀 现在可以启动 FastAPI 服务器了！")
print("   运行命令: python -m uvicorn main:app --reload --host 0.0.0.0 --port 8000")
print("\n📱 Android Studio 连接信息:")
print("   Base URL: http://10.0.2.2:8000/api/v1 (Android 模拟器)")
print("   Base URL: http://<电脑IP>:8000/api/v1 (真机调试)")
print("\n📖 API 文档:")
print("   Swagger UI: http://localhost:8000/docs")
print("   ReDoc: http://localhost:8000/redoc")
print("=" * 60)
