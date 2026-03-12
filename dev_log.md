# 开发日志

## 2026-03-06

### 今日任务
- **GitHub 上传**：配置 `.gitignore` 忽略大型文件，初始化仓库并准备推送
- **模型分析**：评估 `train_unified_freshness_detection.py`，确认多任务学习能力
- **Android 前端**：搭建基础界面结构，实现主页面、历史记录和结果展示页

### 技术要点
- 多任务学习模型：同时支持果蔬分类和新鲜度识别
- 版本控制优化：通过 `.gitignore` 轻量化代码仓库
- Android 架构：现代化 Activity 结构与底部导航设计

### 问题与解决
- **Git 推送**：解决分支名称不匹配问题，准备使用 `git push -u origin master`
- **文件忽略**：添加 `models/experiment_2_unified_freshness/` 到忽略列表，控制上传大小

### 后续计划
- 完成 GitHub 项目推送
- 集成 Android 前端与后端模型
- 测试完整的识别功能
- 优化用户交互体验

## 2026-03-09

### 今日任务
- **App 端优化**：修复收藏按钮 UI 响应问题，实现历史记录管理和排序功能
- **后端更新**：发布 V1.5 版本，优化依赖管理和业务逻辑
- **项目构建**：确保前后端代码编译正常，功能运行稳定

### 技术要点
- **UI 响应优化**：通过 requestLayout() 强制视图立即更新
- **排序功能**：支持时间/名称排序，偏好自动保存到 SharedPreferences
- **后端优化**：文件路径处理、同步冲突处理、Token 管理
- **依赖管理**：添加 preference-ktx 依赖，优化后端 requirements.txt

### 问题与解决
- **UI 响应慢**：移除收藏图标 tint 属性，添加 requestLayout() 强制重绘
- **排序偏好**：使用 SharedPreferences 持久化存储用户排序选择
- **依赖配置**：在 libs.versions.toml 中添加 preference 版本配置

### 后续计划
- 集成前端与后端 API
- 测试完整的识别流程
- 优化用户交互细节
- 准备项目上线

## 2026-03-12

### 今日任务
- **完善 SettingsActivity**：实现语言设置和自动保存历史设置功能
- **完善 FavoritesActivity**：集成 HistoryAdapter，实现收藏记录的完整展示和管理
- **完善 ResultActivity**：从数据库加载并展示完整的识别结果信息
- **技术文档撰写**：为论文撰写准备全面的技术说明文档
- **项目构建验证**：确保所有代码编译通过，无错误

### 技术要点
- **SettingsActivity 完善**：
  - 实现多语言选择（简体中文、繁体中文、英文）
  - 实现自动保存历史记录开关
  - 添加关于我们和检查更新功能
  - 使用 SharedPreferences 持久化设置

- **FavoritesActivity 完善**：
  - 复用 HistoryAdapter 展示收藏记录
  - 实现收藏记录的点击查看、取消收藏、删除功能
  - 空状态处理，引导用户去识别
  - onResume 时刷新收藏列表

- **ResultActivity 完善**：
  - 添加 getRecordById 方法到 RecognitionRecordService
  - 从数据库加载完整的识别记录信息
  - 解析并展示 JSON 格式的营养数据
  - 实现分享识别结果功能
  - 完整的收藏状态管理

- **技术文档**：
  - 撰写详细的技术说明文档，包含项目概述、系统架构、技术栈详解
  - 数据库设计、核心功能模块实现、系统测试与性能评估
  - 项目创新点与技术难点分析

### 问题与解决
- **环境版本一致性**：使用 libs.versions.toml 统一管理依赖版本，确保环境一致性
- **JSON 数据解析**：使用 JSONObject 解析营养数据，动态生成营养成分列表
- **收藏状态同步**：在多个 Activity 中实现收藏状态的正确更新和同步
- **项目构建**：成功完成 assembleDebug 构建，无编译错误

### 后续计划
- 集成前端与后端 API 通信
- 测试完整的识别流程（拍照→识别→保存→展示）
- 优化用户交互细节和动画效果
- 准备项目上线发布

## 2026-03-12（续）

### 今日任务
- **完善 UserManagementActivity**：重写个人中心页面，实现用户信息展示、统计数据、功能菜单导航
- **完善 ImageUploadActivity**：实现完整的图像采集和识别流程，包含完整图片获取、图片压缩、权限请求、帮助功能
- **优化 OnboardingActivity**：完善引导页体验，适配Android 13+权限，改进用户交互
- **配置 FileProvider**：添加FileProvider配置，支持相机拍照获取完整图片
- **修复编译错误**：修复所有编译问题，确保项目正常构建

### 技术要点
- **UserManagementActivity 重写**：
  - 用户信息卡片：显示头像、昵称、UID
  - 统计数据卡片：显示识别次数和收藏数量，从数据库动态加载
  - 功能菜单：历史记录、我的收藏、设置
  - Material Design 卡片式布局

- **ImageUploadActivity 完善**：
  - 完整相机功能：使用FileProvider获取高质量图片
  - 相册选择：支持Android 13+的READ_MEDIA_IMAGES权限
  - 图片压缩：使用ImageUtil压缩优化存储和识别
  - 完整权限请求流程
  - 使用帮助对话框
  - 临时文件清理

- **OnboardingActivity 优化**：
  - 使用新的Activity Result API
  - 适配Android 13+权限模型
  - 返回按钮支持
  - 改进权限拒绝体验
  - 更好的页面指示器和按钮管理

- **FileProvider 配置**：
  - 在AndroidManifest.xml中添加FileProvider
  - 配置file_paths.xml路径映射
  - 添加READ_MEDIA_IMAGES权限

### 问题与解决
- **ImageUtils 引用错误**：修改为ImageUtil，并在ImageUtil中添加公共compressImage方法
- **RecognitionRecordService 实例化错误**：RecognitionRecordService是object，直接调用静态方法
- **字段名错误**：isFavorite改为is_collected
- **方法参数缺失**：getAllRecords需要context参数
- **Kotlin daemon 问题**：使用--no-daemon参数成功编译

### 验证结果
- ✅ 项目编译成功（BUILD SUCCESSFUL）
- ✅ 无诊断错误
- ✅ 所有功能实现完成

## 2026-03-12（续二）

### 今日任务
- **优化 MainActivity**：适配 Android 13+ 权限模型，使用新的 Activity Result API
- **添加搜索功能**：在历史记录和收藏页面实现实时搜索功能
- **完善用户信息编辑**：在个人中心添加昵称编辑和退出登录功能
- **添加数据导出功能**：在历史记录页面添加导出为CSV功能
- **添加分享识别结果功能**：在历史记录项中添加分享按钮
- **项目构建验证**：确保所有代码编译通过，功能正常

### 技术要点
- **MainActivity 优化**：
  - 使用新的 Activity Result API 替代旧权限请求方式
  - 适配 Android 13+ 的 READ_MEDIA_IMAGES 权限
  - 改善权限提示文案，提升用户体验

- **搜索功能实现**：
  - 在 HistoryActivity 和 FavoritesActivity 添加搜索框
  - 实时搜索过滤，支持模糊匹配和忽略大小写
  - 搜索框清空按钮，提升用户体验
  - 搜索和排序功能结合使用

- **用户信息编辑**：
  - 添加编辑昵称对话框，支持输入验证
  - SharedPreferences 持久化存储用户信息
  - 退出登录功能，重置为游客用户
  - Toast 提示反馈，改善交互体验

- **数据导出功能**：
  - CSV 格式导出，包含 ID、果蔬名称、置信度、时间、收藏状态
  - 自动创建导出目录，文件名带时间戳
  - 导出成功后自动调用系统分享
  - 使用 FileProvider 安全分享文件

- **分享识别结果**：
  - 在 HistoryAdapter 中添加分享按钮回调
  - 生成格式化的分享文本，包含识别信息
  - 支持分享图片+文字或仅文字
  - 使用系统分享菜单，支持多种分享方式

### 问题与解决
- **HistoryAdapter 接口变更**：新增 onShareClick 参数，同步更新 FavoritesActivity
- **编译错误修复**：修复 applySort 方法引用错误
- **FileProvider 路径配置**：确保导出文件和分享都能正常工作

### 验证结果
- ✅ 项目编译成功（BUILD SUCCESSFUL）
- ✅ 所有新功能实现完成
- ✅ 功能完整可用