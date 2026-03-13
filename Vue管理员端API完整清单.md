# FreshID Vue管理员端API完整清单

## 概述

本文档详细列出了FreshID Vue管理员端与后端API对接的完整功能清单。

---

## 🔌 服务连接信息

### 后端API服务
- **默认端口**: 8000
- **API前缀**: `/api/v1`
- **Vue代理配置**: 在 `vite.config.js` 中配置，代理 `/api` 到 `http://localhost:8000`

### 认证方式
- **JWT Token认证**
- **默认管理员账号**: `admin` / `admin123`
- **Token存储**: localStorage

---

## 📋 完整API接口清单

### 1. 认证接口 (`/api/v1/admin`)

| 接口 | 方法 | 说明 | 请求参数 | 返回数据 |
|------|------|------|----------|----------|
| `/login` | POST | 管理员登录 | `{ username, password }` | `{ success, data: { token, user } }` |

### 2. 用户管理接口 (`/api/v1/admin/users`)

| 接口 | 方法 | 说明 | 请求参数 | 返回数据 |
|------|------|------|----------|----------|
| `/` | GET | 获取用户列表 | `page, limit, search?` | `{ success, data: { list[], total } }` |
| `/:id` | PUT | 更新用户信息 | `{ username, nickname, email, phone_number }` | `{ success, message }` |
| `/:id` | DELETE | 删除用户 | - | `{ success, message }` |

### 3. 识别记录接口 (`/api/v1/admin/records`)

| 接口 | 方法 | 说明 | 请求参数 | 返回数据 |
|------|------|------|----------|----------|
| `/` | GET | 获取识别记录列表 | `page, limit, user_id?, fruit_name?, start_date?, end_date?` | `{ success, data: { list[], total } }` |
| `/:id` | DELETE | 删除单条记录 | - | `{ success, message }` |
| `/batch-delete` | POST | 批量删除记录 | `{ ids: [] }` | `{ success, message }` |

### 4. 数据统计接口 (`/api/v1/admin/stats`)

| 接口 | 方法 | 说明 | 请求参数 | 返回数据 |
|------|------|------|----------|----------|
| `/overview` | GET | 获取统计概览 | - | `{ success, data: { totalUsers, totalRecords, totalFavorites, avgAccuracy } }` |
| `/trend` | GET | 获取趋势数据 | `days?` | `{ success, data: { dates[], records[], users[] } }` |
| `/fruit-distribution` | GET | 获取果蔬分布 | - | `{ success, data: [{ name, value }] }` |
| `/login-type` | GET | 获取登录类型分布 | - | `{ success, data: [{ name, value }] }` |

### 5. 模型管理接口 (`/api/v1/admin/models`)

| 接口 | 方法 | 说明 | 请求参数 | 返回数据 |
|------|------|------|----------|----------|
| `/` | GET | 获取模型列表 | `page, limit` | `{ success, data: { list[], total } }` |
| `/upload` | POST | 上传模型文件 | `file (FormData), platform, version` | `{ success, data: { id, file_name } }` |
| `/:id/activate` | POST | 激活模型 | - | `{ success, message }` |
| `/:id` | DELETE | 删除模型 | - | `{ success, message }` |

---

## 📱 功能模块对应关系

### 1. 登录页面 (`/login`)
- **功能**: 管理员登录
- **API**: `POST /api/v1/admin/login`
- **默认账号**: admin / admin123

### 2. 数据统计页面 (`/dashboard`)
- **功能**: 
  - 显示统计概览卡片（用户总数、识别次数、收藏总数、平均准确率）
  - 识别趋势图表
  - 果蔬识别分布饼图
  - 登录类型分布饼图
  - 识别类型分布饼图
  - 最近识别记录列表
- **对应API**:
  - `GET /api/v1/admin/stats/overview`
  - `GET /api/v1/admin/stats/trend`
  - `GET /api/v1/admin/stats/fruit-distribution`
  - `GET /api/v1/admin/stats/login-type`
  - `GET /api/v1/admin/records` (limit=10)

### 3. 用户管理页面 (`/users`)
- **功能**:
  - 用户列表展示
  - 按用户名/手机号搜索
  - 新增用户
  - 编辑用户信息
  - 删除用户
- **对应API**:
  - `GET /api/v1/admin/users`
  - `POST /api/v1/users`
  - `PUT /api/v1/admin/users/:id`
  - `DELETE /api/v1/admin/users/:id`

### 4. 识别记录页面 (`/records`)
- **功能**:
  - 识别记录列表展示
  - 按果蔬名称、识别类型、收藏状态、日期范围筛选
  - 查看记录详情
  - 删除单条记录
  - 批量删除记录
- **对应API**:
  - `GET /api/v1/admin/records`
  - `DELETE /api/v1/admin/records/:id`
  - `POST /api/v1/admin/records/batch-delete`

### 5. 模型管理页面 (`/models`)
- **功能**:
  - 模型列表展示
  - 按平台筛选
  - 上传新模型
  - 激活模型
  - 查看模型详情
  - 下载模型
  - 删除模型
- **对应API**:
  - `GET /api/v1/admin/models`
  - `POST /api/v1/admin/models/upload`
  - `POST /api/v1/admin/models/:id/activate`
  - `DELETE /api/v1/admin/models/:id`

---

## 🗄️ 数据库表结构

### users (用户表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 用户ID (主键) |
| uid | VARCHAR(50) | 用户唯一标识 |
| username | VARCHAR(255) | 用户名 |
| email | VARCHAR(255) | 邮箱 |
| phone | VARCHAR(50) | 手机号 |
| login_type | VARCHAR(50) | 登录类型 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

### recognition_records (识别记录表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 记录ID (主键) |
| user_id | INT | 用户ID (外键) |
| image_url | VARCHAR(500) | 图片路径 |
| result | JSON | 识别结果 |
| is_collected | BOOLEAN | 是否收藏 |
| deleted | BOOLEAN | 是否已删除 |
| created_at | TIMESTAMP | 识别时间 |
| updated_at | TIMESTAMP | 更新时间 |

### app_versions (模型/版本表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 版本ID (主键) |
| platform | VARCHAR(20) | 平台 |
| version | VARCHAR(20) | 版本号 |
| build_number | INT | 构建号 |
| is_required | BOOLEAN | 是否激活 |
| download_url | VARCHAR(500) | 下载地址 |
| release_notes | TEXT | 发布说明 |
| released_at | TIMESTAMP | 发布时间 |

---

## 🚀 启动指南

### 1. 启动后端服务
```bash
cd backend
pip install -r requirements.txt
python main.py
```

### 2. 启动Vue管理员端
```bash
cd vue
npm install
npm run dev
```

### 3. 访问地址
- **Vue管理员端**: http://localhost:3000
- **后端API文档**: http://localhost:8000/docs (Swagger UI)
- **后端健康检查**: http://localhost:8000/health

---

## 🔐 安全说明

1. **认证**: 所有管理员API需要JWT Token认证
2. **默认账号**: 生产环境请修改默认密码
3. **CORS**: 后端已配置允许所有来源（开发环境）
4. **HTTPS**: 生产环境建议使用HTTPS

---

## 📝 注意事项

1. **API前缀**: 所有管理员接口都在 `/api/v1/admin` 前缀下
2. **分页**: 列表接口都支持分页查询（page, limit参数）
3. **软删除**: 识别记录使用软删除机制
4. **模型文件**: 支持格式包括 .pt, .pth, .onnx, .tflite

---

**文档版本**: V1.0  
**最后更新**: 2026-03-13
