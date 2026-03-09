# FreshID 后端 V1.5 更新说明

## 更新日期
2026-03-09

## 更新内容概述
本次更新基于 V1.4 文档的建议，进行了多项优化和改进。

---

## 🔧 具体更新内容

### 1. requirements.txt 优化
- **添加了可选依赖说明**：`redis>=5.0.0  # 可选，用于限流和缓存`
- **删除了重复的 python-multipart**

### 2. 环境变量配置补充
- **新增限流配置**：
  - `RATE_LIMIT_DEFAULT=60`
  - `RATE_LIMIT_RECOGNITION=10`
  - `RATE_LIMIT_VERIFICATION=1`
- **新增 BASE_URL 配置**：`BASE_URL=http://localhost:8000`
- **完善 OSS 配置**：添加了 `OSS_REGION` 配置项

### 3. 业务错误码扩展
- **新增错误码 1011**：`FILE_UPLOAD_VALIDATION_FAILED` - 文件上传校验失败

### 4. 文件路径处理优化
- **storage_service.py 更新**：
  - 保存文件时返回相对路径（如 `uploads/xxx.jpg`）
  - 通过 `get_file_url()` 方法拼接完整 URL
  - 支持 BASE_URL 配置
  - 删除文件时正确处理相对路径

### 5. 同步冲突处理逻辑
- **data_service.py 新增 `sync_upload()` 方法**：
  - 对每条上传记录，根据 `updated_at` 判断是否接受更新
  - 客户端时间晚于服务端时间则接受更新，否则忽略
  - 对 `deleted_ids`，将对应记录的 `deleted` 字段置为 `true` 并更新 `updated_at`

### 6. 登出接口与 Token 管理
- **data_service.py 新增 `revoke_user_refresh_tokens()` 方法**：撤销用户所有 Refresh Token
- **routes.py 新增 `/auth/logout` 接口**：登出时撤销用户所有 Refresh Token

### 7. 定时清理脚本
- **新增 `scripts/cleanup.py`**：
  - 清理过期的验证码记录
  - 清理过期的 Refresh Token
  - 支持通过 cron 或其他调度工具定时运行
- **新增 `scripts/__init__.py`**

---

## 📝 使用说明

### 定时任务配置（Linux/Mac）
```bash
# 编辑 crontab
crontab -e

# 添加每天凌晨 3 点执行清理任务
0 3 * * * cd /path/to/backend && /usr/bin/python scripts/cleanup.py >> logs/cleanup.log 2>&1
```

### Windows 定时任务
使用任务计划程序，设置每天执行 `python scripts/cleanup.py`

---

## 📁 更新的文件列表
1. `requirements.txt` - 依赖优化
2. `.env.example` - 环境变量补充
3. `app/models/schemas.py` - 错误码扩展
4. `app/services/storage_service.py` - 文件路径优化
5. `app/services/data_service.py` - 同步逻辑与 Token 管理
6. `app/api/routes.py` - 新增登出接口
7. `scripts/cleanup.py` - 新增清理脚本
8. `scripts/__init__.py` - 新增

---

## ✅ 已采纳的建议
1. ✅ 同步冲突处理逻辑补充
2. ✅ 环境变量补充限流配置
3. ✅ 对象存储配置示例完善
4. ✅ 文件路径统一（相对路径 + BASE_URL）
5. ✅ 登出时 Refresh Token 撤销策略
6. ✅ requirements.txt 可选依赖说明
7. ✅ 定时任务清理示例
8. ✅ 错误码补充（1011）
