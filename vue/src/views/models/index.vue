<template>
  <div class="models-container">
    <el-card class="upload-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="card-icon"><UploadFilled /></el-icon>
            <span class="card-title">模型上传</span>
          </div>
        </div>
      </template>
      <el-upload
        class="model-uploader"
        drag
        :auto-upload="false"
        :show-file-list="true"
        :limit="1"
        accept=".pt,.pth,.onnx,.tflite"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将模型文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            <el-icon><InfoFilled /></el-icon>
            支持格式：.pt, .pth, .onnx, .tflite | 文件大小不超过 500MB
          </div>
        </template>
      </el-upload>
      
      <el-form :model="uploadForm" :rules="uploadRules" ref="uploadFormRef" label-width="120px" class="upload-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模型名称" prop="name">
              <el-input v-model="uploadForm.name" placeholder="请输入模型名称" prefix-icon="Box" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本号" prop="version">
              <el-input v-model="uploadForm.version" placeholder="例如：v1.0.0" prefix-icon="Tickets" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="适用平台" prop="platform">
              <el-select v-model="uploadForm.platform" placeholder="请选择适用平台" style="width: 100%">
                <el-option label="后端 (Server)" value="server" />
                <el-option label="Android端" value="android" />
                <el-option label="全平台" value="all" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型类型" prop="type">
              <el-select v-model="uploadForm.type" placeholder="请选择模型类型" style="width: 100%">
                <el-option label="YOLOv8-nano" value="yolov8n" />
                <el-option label="YOLOv8-small" value="yolov8s" />
                <el-option label="YOLOv8-medium" value="yolov8m" />
                <el-option label="YOLOv8-large" value="yolov8l" />
                <el-option label="自定义" value="custom" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述信息">
          <el-input
            v-model="uploadForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模型的描述信息，包括更新内容、性能指标等"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleUpload" :loading="uploading">
            <el-icon><Upload /></el-icon> 上传并发布
          </el-button>
          <el-button @click="handleResetUpload">
            <el-icon><RefreshLeft /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="card-icon"><Box /></el-icon>
            <span class="card-title">模型列表</span>
          </div>
          <el-radio-group v-model="filterPlatform" size="small" @change="loadData">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="server">后端</el-radio-button>
            <el-radio-button label="android">Android</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="tableData" stripe v-loading="loading" class="models-table">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="模型名称" min-width="150">
          <template #default="{ row }">
            <div class="model-name">
              <el-icon class="model-icon"><Box /></el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small" effect="dark">{{ row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="platform" label="适用平台" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getPlatformType(row.platform)" size="small">
              {{ getPlatformText(row.platform) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="模型类型" width="140" align="center" />
        <el-table-column prop="file_size" label="文件大小" width="100" align="center" />
        <el-table-column prop="accuracy" label="准确率" width="120">
          <template #default="{ row }">
            <el-progress
              :percentage="row.accuracy"
              :color="getAccuracyColor(row.accuracy)"
              :stroke-width="8"
              :show-text="false"
            />
            <span class="accuracy-text">{{ row.accuracy }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.is_active ? 'success' : 'info'" size="small" effect="dark">
              <el-icon v-if="row.is_active"><CircleCheck /></el-icon>
              {{ row.is_active ? '使用中' : '已归档' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="上传时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="!row.is_active"
              type="success"
              size="small"
              link
              @click="handleActivate(row)"
            >
              <el-icon><CircleCheck /></el-icon> 激活
            </el-button>
            <el-button
              type="primary"
              size="small"
              link
              @click="handleViewDetail(row)"
            >
              <el-icon><View /></el-icon> 详情
            </el-button>
            <el-button
              type="warning"
              size="small"
              link
              @click="handleDownload(row)"
            >
              <el-icon><Download /></el-icon> 下载
            </el-button>
            <el-button
              v-if="!row.is_active"
              type="danger"
              size="small"
              link
              @click="handleDelete(row)"
            >
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination"
      />
    </el-card>

    <el-dialog
      v-model="detailDialogVisible"
      title="模型详情"
      width="600px"
      class="detail-dialog"
    >
      <el-descriptions :column="2" border v-if="currentModel" class="detail-descriptions">
        <el-descriptions-item label="模型ID">{{ currentModel.id }}</el-descriptions-item>
        <el-descriptions-item label="版本号">{{ currentModel.version }}</el-descriptions-item>
        <el-descriptions-item label="模型名称" :span="2">{{ currentModel.name }}</el-descriptions-item>
        <el-descriptions-item label="适用平台">
          <el-tag :type="getPlatformType(currentModel.platform)">
            {{ getPlatformText(currentModel.platform) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="模型类型">{{ currentModel.type }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ currentModel.file_size }}</el-descriptions-item>
        <el-descriptions-item label="准确率">
          <el-progress
            :percentage="currentModel.accuracy"
            :color="getAccuracyColor(currentModel.accuracy)"
          />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentModel.is_active ? 'success' : 'info'" effect="dark">
            <el-icon v-if="currentModel.is_active"><CircleCheck /></el-icon>
            {{ currentModel.is_active ? '使用中' : '已归档' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="上传时间" :span="2">{{ currentModel.created_at }}</el-descriptions-item>
        <el-descriptions-item label="描述信息" :span="2">
          {{ currentModel.description || '暂无描述' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">
          <el-icon><Close /></el-icon> 关闭
        </el-button>
        <el-button
          v-if="!currentModel?.is_active"
          type="success"
          @click="handleActivate(currentModel)"
        >
          <el-icon><CircleCheck /></el-icon> 激活此模型
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { modelApi } from '@/api/model'
import { requestCache } from '@/utils/performance'

const loading = ref(false)
const uploading = ref(false)
const filterPlatform = ref('')
const detailDialogVisible = ref(false)
const currentModel = ref(null)
const uploadFormRef = ref(null)

const uploadForm = reactive({
  version: '',
  platform: 'android',
  file: null
})

const uploadRules = {
  version: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  platform: [{ required: true, message: '请选择适用平台', trigger: 'change' }]
}

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const tableData = ref([
  {
    id: 1,
    name: 'YOLOv8n 果蔬识别模型',
    version: 'v1.2.0',
    platform: 'all',
    type: 'yolov8n',
    file_size: '6.2MB',
    accuracy: 88.5,
    is_active: true,
    created_at: '2026-03-10 14:30:00',
    description: '优化了番茄和黄瓜的识别准确率，新增了10种果蔬类别'
  },
  {
    id: 2,
    name: 'YOLOv8s 高精度模型',
    version: 'v1.1.0',
    platform: 'server',
    type: 'yolov8s',
    file_size: '22.5MB',
    accuracy: 92.3,
    is_active: false,
    created_at: '2026-03-05 10:20:00',
    description: '后端专用高精度模型，适合服务器端部署'
  },
  {
    id: 3,
    name: 'YOLOv8n 轻量版 (Android)',
    version: 'v1.0.5',
    platform: 'android',
    type: 'yolov8n',
    file_size: '4.8MB',
    accuracy: 85.2,
    is_active: true,
    created_at: '2026-02-28 16:45:00',
    description: 'Android端专用轻量化模型，量化优化'
  },
  {
    id: 4,
    name: 'YOLOv8m 标准版',
    version: 'v1.0.0',
    platform: 'server',
    type: 'yolov8m',
    file_size: '49.7MB',
    accuracy: 94.1,
    is_active: false,
    created_at: '2026-02-20 09:15:00',
    description: '初始版本标准模型'
  }
])

const getPlatformType = (platform) => {
  const map = {
    server: 'primary',
    android: 'success',
    all: 'warning'
  }
  return map[platform] || 'info'
}

const getPlatformText = (platform) => {
  const map = {
    server: '后端',
    android: 'Android',
    all: '全平台'
  }
  return map[platform] || platform
}

const getAccuracyColor = (accuracy) => {
  if (accuracy >= 90) return '#67C23A'
  if (accuracy >= 85) return '#E6A23C'
  return '#F56C6C'
}

const handleFileChange = (file) => {
  uploadForm.file = file.raw
}

const handleFileRemove = () => {
  uploadForm.file = null
}

const handleResetUpload = () => {
  uploadForm.version = ''
  uploadForm.platform = 'android'
  uploadForm.file = null
  uploadFormRef.value?.resetFields()
}

const handleUpload = async () => {
  if (!uploadFormRef.value) return
  
  await uploadFormRef.value.validate(async (valid) => {
    if (valid) {
      if (!uploadForm.file) {
        ElMessage.warning('请选择要上传的模型文件')
        return
      }
      
      uploading.value = true
      try {
        const formData = new FormData()
        formData.append('file', uploadForm.file)
        
        const res = await modelApi.uploadModel(formData, {
          platform: uploadForm.platform,
          version: uploadForm.version
        })
        if (res.success) {
          ElMessage.success(res.message || '模型上传成功')
          handleResetUpload()
          loadData()
        }
      } catch (error) {
        console.error('[Models] 上传失败', error)
        ElMessage.error('模型上传失败，请重试')
      } finally {
        uploading.value = false
      }
    }
  })
}

const handleActivate = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要激活模型 "${row.name}" (${row.version}) 吗？\n激活后将成为${getPlatformText(row.platform)}平台的当前使用模型。`,
      '激活模型',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await modelApi.activateModel(row.id, row.platform)
    if (res.success) {
      ElMessage.success(res.message || '模型激活成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[Models] 激活失败', error)
      ElMessage.error('模型激活失败')
    }
  }
}

const handleViewDetail = (row) => {
  currentModel.value = row
  detailDialogVisible.value = true
}

const handleDownload = (row) => {
  ElMessage.info(`正在下载模型: ${row.name}`)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除模型 "${row.name}" 吗？此操作不可恢复。`, '删除模型', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await modelApi.deleteModel(row.id)
    if (res.success) {
      ElMessage.success(res.message || '模型删除成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[Models] 删除失败', error)
      ElMessage.error('模型删除失败')
    }
  }
}

const loadData = async (useCache = true) => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      limit: pagination.size
    }
    
    const cacheKey = `models_${JSON.stringify(params)}`
    
    if (useCache) {
      const cached = requestCache.get(cacheKey)
      if (cached) {
        let models = cached.data?.list || []
        if (filterPlatform.value) {
          models = models.filter(m => m.platform === filterPlatform.value || m.platform === 'all')
        }
        tableData.value = models.map(m => ({
          ...m,
          is_active: m.is_required,
          name: m.download_url?.split('/').pop() || '模型'
        }))
        pagination.total = cached.data?.total || 0
        loading.value = false
        return
      }
    }
    
    const res = await modelApi.getModels(params)
    if (res.success && res.data) {
      let models = res.data.list || []
      if (filterPlatform.value) {
        models = models.filter(m => m.platform === filterPlatform.value || m.platform === 'all')
      }
      tableData.value = models.map(m => ({
        ...m,
        is_active: m.is_required,
        name: m.download_url?.split('/').pop() || '模型'
      }))
      pagination.total = res.data.total || 0
      requestCache.set(cacheKey, res)
    } else if (res.success === false) {
      console.log('[Models] API返回成功标志为false:', res)
    }
  } catch (error) {
    console.log('[Models] 加载数据失败，使用缓存或空数据', error)
    const cacheKey = `models_${JSON.stringify({
      page: pagination.page,
      limit: pagination.size
    })}`
    const cached = requestCache.get(cacheKey)
    if (cached) {
      let models = cached.data?.list || []
      if (filterPlatform.value) {
        models = models.filter(m => m.platform === filterPlatform.value || m.platform === 'all')
      }
      tableData.value = models.map(m => ({
        ...m,
        is_active: m.is_required,
        name: m.download_url?.split('/').pop() || '模型'
      }))
      pagination.total = cached.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (val) => {
  pagination.size = val
  loadData(false)
}

const handleCurrentChange = (val) => {
  pagination.page = val
  loadData(false)
}

watch(filterPlatform, () => {
  loadData(false)
})

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.models-container {
  padding: 0;
}

.upload-card,
.table-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-icon {
  color: #409EFF;
  font-size: 18px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.model-uploader {
  width: 100%;
}

.model-uploader :deep(.el-upload-dragger) {
  width: 100%;
  border-radius: 12px;
  padding: 40px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8f4fd 100%);
  border: 2px dashed #409EFF;
  transition: all 0.3s;
}

.model-uploader :deep(.el-upload-dragger:hover) {
  border-color: #66b1ff;
  background: linear-gradient(135deg, #e8f4fd 0%, #d9ecff 100%);
}

.el-icon--upload {
  font-size: 67px;
  color: #409EFF;
  margin: 20px 0 16px;
}

.el-upload__text {
  color: #606266;
  font-size: 14px;
}

.el-upload__text em {
  color: #409EFF;
  font-style: normal;
  font-weight: 500;
}

.el-upload__tip {
  color: #909399;
  font-size: 12px;
  margin-top: 7px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.upload-form {
  margin-top: 20px;
}

.models-table {
  border-radius: 8px;
  overflow: hidden;
}

.model-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-icon {
  color: #409EFF;
  font-size: 18px;
}

.accuracy-text {
  display: inline-block;
  margin-left: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #666;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

:deep(.el-dialog__header) {
  padding: 20px 20px 10px;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.el-dialog__title) {
  font-weight: 600;
  font-size: 18px;
}

.detail-descriptions {
  margin-top: 10px;
}
</style>
