# FreshID 管理员后台

基于 Vue 3 + Element Plus 构建的 FreshID 智能果蔬识别系统管理员后台。

## ✨ 功能特性

### 已完成功能

1. **登录页面**
   - 美观的渐变背景设计
   - 动画效果和毛玻璃效果
   - 表单验证
   - 默认账号：admin / 密码：123456

2. **数据统计仪表板**
   - 4个统计卡片（用户总数、识别次数、收藏总数、平均准确率）
   - 识别趋势折线图（支持周/月/年切换）
   - 果蔬识别分布饼图
   - 登录类型分布饼图
   - 识别类型分布饼图
   - 最近识别记录列表
   - 响应式布局适配

3. **用户管理**
   - 用户列表展示（含所有数据库字段）
   - 搜索和筛选功能
   - 新增用户
   - 编辑用户
   - 删除用户
   - 分页功能

4. **识别记录管理**
   - 识别记录列表展示
   - 多条件搜索（果蔬名称、识别类型、收藏状态、时间范围）
   - 记录详情查看
   - 单个删除和批量删除
   - 图片预览功能
   - 置信度进度条显示

5. **模型管理**
   - 模型上传（支持拖拽上传，格式：.pt, .pth, .onnx, .tflite）
   - 模型列表展示（支持按平台筛选：后端、Android、全部）
   - 模型激活（支持后端、Android端、全平台分别激活）
   - 模型版本管理
   - 模型详情查看
   - 模型下载
   - 模型删除
   - 准确率进度条显示
   - 文件大小和状态管理

6. **主布局**
   - 可折叠侧边栏导航
   - 顶部导航栏（面包屑、用户信息、退出登录）
   - 响应式设计

7. **API 服务层**
   - 统一的 HTTP 请求封装
   - 请求/响应拦截器
   - 用户 API 接口（预留位置）
   - 识别记录 API 接口（预留位置）
   - 数据统计 API 接口（预留位置）
   - 模型管理 API 接口（预留位置）

8. **统一反馈机制**
   - 成功/错误/警告/信息提示
   - 确认对话框
   - 统一的消息样式和时长

## 🛠️ 技术栈

- **框架**: Vue 3 (Composition API)
- **UI 组件库**: Element Plus
- **路由**: Vue Router 4
- **图表**: ECharts
- **HTTP 客户端**: Axios
- **构建工具**: Vite

## 📁 项目结构

```
vue/
├── src/
│   ├── api/              # API 接口层
│   │   ├── index.js      # HTTP 请求封装
│   │   ├── user.js       # 用户相关接口
│   │   ├── record.js     # 识别记录相关接口
│   │   ├── dashboard.js  # 数据统计相关接口
│   │   └── model.js      # 模型管理相关接口
│   ├── layout/           # 布局组件
│   │   └── index.vue     # 主布局
│   ├── views/            # 页面组件
│   │   ├── login/        # 登录页
│   │   ├── dashboard/    # 数据统计页
│   │   ├── users/        # 用户管理页
│   │   ├── records/      # 识别记录页
│   │   └── models/       # 模型管理页
│   ├── router/           # 路由配置
│   │   └── index.js
│   ├── utils/            # 工具函数
│   │   └── message.js    # 统一消息提示
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── index.html
├── package.json
├── vite.config.js
├── README.md
└── 技术说明文档.md
```

## 🚀 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

开发服务器将在 http://localhost:3000 启动

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

## 🔌 API 接口说明

所有 API 接口已在 `src/api/` 目录下预留位置，当前使用模拟数据。后续对接真实后端时，只需修改对应 API 文件中的实现即可。

### 用户相关接口 (`src/api/user.js`)
- `login(data)` - 登录
- `getUsers(params)` - 获取用户列表
- `addUser(data)` - 新增用户
- `updateUser(data)` - 更新用户
- `deleteUser(id)` - 删除用户

### 识别记录相关接口 (`src/api/record.js`)
- `getRecords(params)` - 获取识别记录列表
- `getRecordDetail(id)` - 获取识别记录详情
- `deleteRecord(id)` - 删除识别记录
- `batchDeleteRecords(ids)` - 批量删除识别记录

### 数据统计相关接口 (`src/api/dashboard.js`)
- `getStats()` - 获取统计数据
- `getTrendData(params)` - 获取趋势数据
- `getFruitDistribution()` - 获取果蔬分布
- `getLoginTypeDistribution()` - 获取登录类型分布
- `getRecognitionTypeDistribution()` - 获取识别类型分布
- `getRecentRecords()` - 获取最近识别记录

### 模型管理相关接口 (`src/api/model.js`)
- `getModels(params)` - 获取模型列表
- `uploadModel(formData, onUploadProgress)` - 上传模型文件
- `activateModel(id, platform)` - 激活指定平台的模型
- `deleteModel(id)` - 删除模型
- `getModelDetail(id)` - 获取模型详情

## 💡 注意事项

1. 当前版本使用模拟数据，实际使用时需要连接后端 API
2. 所有 API 接口位置已预留，方便后续对接
3. 项目已配置代理，开发环境下 `/api` 请求会代理到 `http://localhost:8000`
4. 路由守卫已实现，未登录用户会自动跳转到登录页

## 🎨 UI 优化亮点

- 现代化的渐变配色方案
- 流畅的动画效果
- 响应式布局适配
- 统一的组件样式
- 友好的用户反馈
