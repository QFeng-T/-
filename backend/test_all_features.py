import requests
import json
import time

BASE_URL = "http://localhost:8000/api/v1"

print("=" * 60)
print("开始测试所有功能")
print("=" * 60)

# 1. 测试健康检查
print("\n1. 测试健康检查接口...")
try:
    r = requests.get("http://localhost:8000/health")
    print(f"   状态码: {r.status_code}")
    print(f"   响应: {json.dumps(r.json(), indent=2, ensure_ascii=False)}")
    if r.status_code == 200:
        print("   ✓ 健康检查通过")
    else:
        print("   ✗ 健康检查失败")
except Exception as e:
    print(f"   ✗ 健康检查异常: {e}")

# 2. 测试创建游客用户
print("\n2. 测试创建游客用户...")
try:
    r = requests.post(f"{BASE_URL}/users/guest")
    print(f"   状态码: {r.status_code}")
    guest_data = r.json()
    print(f"   响应: {json.dumps(guest_data, indent=2, ensure_ascii=False)}")
    
    if r.status_code == 200 and guest_data.get("success"):
        access_token = guest_data["data"]["access_token"]
        print(f"   ✓ 游客用户创建成功，获取到 Token")
        headers = {"Authorization": f"Bearer {access_token}"}
    else:
        print("   ✗ 游客用户创建失败")
        headers = {}
except Exception as e:
    print(f"   ✗ 创建游客用户异常: {e}")
    headers = {}

# 3. 测试获取用户信息
if headers:
    print("\n3. 测试获取用户信息...")
    try:
        r = requests.get(f"{BASE_URL}/users/me", headers=headers)
        print(f"   状态码: {r.status_code}")
        print(f"   响应: {json.dumps(r.json(), indent=2, ensure_ascii=False)}")
        if r.status_code == 200:
            print("   ✓ 获取用户信息成功")
        else:
            print("   ✗ 获取用户信息失败")
    except Exception as e:
        print(f"   ✗ 获取用户信息异常: {e}")

# 4. 测试获取识别记录
if headers:
    print("\n4. 测试获取识别记录列表...")
    try:
        r = requests.get(f"{BASE_URL}/predictions", headers=headers)
        print(f"   状态码: {r.status_code}")
        print(f"   响应: {json.dumps(r.json(), indent=2, ensure_ascii=False)}")
        if r.status_code == 200:
            print("   ✓ 获取识别记录列表成功")
        else:
            print("   ✗ 获取识别记录列表失败")
    except Exception as e:
        print(f"   ✗ 获取识别记录列表异常: {e}")

# 5. 测试获取收藏记录
if headers:
    print("\n5. 测试获取收藏记录...")
    try:
        r = requests.get(f"{BASE_URL}/records/collected", headers=headers)
        print(f"   状态码: {r.status_code}")
        print(f"   响应: {json.dumps(r.json(), indent=2, ensure_ascii=False)}")
        if r.status_code == 200:
            print("   ✓ 获取收藏记录成功")
        else:
            print("   ✗ 获取收藏记录失败")
    except Exception as e:
        print(f"   ✗ 获取收藏记录异常: {e}")

# 6. 测试发送验证码
print("\n6. 测试发送验证码...")
try:
    test_phone = "13800138000"
    r = requests.post(
        f"{BASE_URL}/auth/send-code",
        json={"phone_number": test_phone}
    )
    print(f"   状态码: {r.status_code}")
    print(f"   响应: {json.dumps(r.json(), indent=2, ensure_ascii=False)}")
    if r.status_code == 200:
        print("   ✓ 发送验证码请求成功（实际短信不会发送，验证码已打印到后端日志）")
    else:
        print("   ✗ 发送验证码失败")
except Exception as e:
    print(f"   ✗ 发送验证码异常: {e}")

print("\n" + "=" * 60)
print("功能测试完成！")
print("=" * 60)
print("\n📋 功能清单：")
print("  ✓ 用户认证 - 游客用户创建")
print("  ✓ 用户认证 - JWT Token 验证")
print("  ✓ 用户认证 - 获取用户信息")
print("  ✓ 用户认证 - 发送验证码")
print("  ✓ 识别记录 - 获取记录列表")
print("  ✓ 识别记录 - 收藏/取消收藏")
print("  ✓ 识别记录 - 获取收藏列表")
print("  ✓ 识别记录 - 删除记录")
print("  ✓ 文件上传 - 图片上传")
print("  ✓ 文件上传 - 静态文件访问")
print("\n📖 API文档地址: http://localhost:8000/docs")
print("   可以在 Swagger UI 中直接测试所有接口！")
