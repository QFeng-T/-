<template>
  <div class="records-container">
    <el-card class="search-card">
      <template #header>
        <div class="card-header">
          <el-icon class="card-icon"><Filter /></el-icon>
          <span class="card-title">搜索筛选</span>
        </div>
      </template>
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="果蔬名称">
          <el-input v-model="searchForm.fruitName" placeholder="请输入果蔬名称" clearable prefix-icon="Apple" />
        </el-form-item>
        <el-form-item label="识别类型">
          <el-select v-model="searchForm.recognitionType" placeholder="请选择识别类型" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="本地" value="local" />
            <el-option label="云端" value="cloud" />
          </el-select>
        </el-form-item>
        <el-form-item label="收藏状态">
          <el-select v-model="searchForm.isCollected" placeholder="请选择收藏状态" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="已收藏" :value="true" />
            <el-option label="未收藏" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="识别时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshLeft /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="card-icon"><Document /></el-icon>
            <span class="card-title">识别记录列表</span>
          </div>
          <el-button type="danger" size="small" @click="handleBatchDelete">
            <el-icon><Delete /></el-icon> 批量删除
          </el-button>
        </div>
      </template>
      
      <el-table :data="tableData" stripe v-loading="loading" @selection-change="handleSelectionChange" class="records-table">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="id" label="记录ID" width="80" align="center" />
        <el-table-column prop="user_id" label="用户ID" width="100" align="center" />
        <el-table-column label="图片" width="100" align="center">
          <template #default="{ row }">
            <el-image
              :src="row.image_url || 'https://via.placeholder.com/60x60'"
              fit="cover"
              class="record-image"
              :preview-src-list="[row.image_url || 'https://via.placeholder.com/60x60']"
              preview-teleported
            />
          </template>
        </el-table-column>
        <el-table-column prop="fruit_veg_name" label="果蔬名称" width="120">
          <template #default="{ row }">
            <el-tag type="success" size="small" effect="dark">{{ row.fruit_veg_name || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="confidence" label="置信度" width="150">
          <template #default="{ row }">
            <el-progress
              :percentage="(row.confidence || 0) * 100"
              :color="getConfidenceColor(row.confidence)"
              :stroke-width="8"
              :show-text="false"
            />
            <span class="confidence-text">{{ Math.round((row.confidence || 0) * 100) }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="class_id" label="分类ID" width="80" align="center" />
        <el-table-column prop="recognition_type" label="识别类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.recognition_type === 'cloud' ? 'primary' : 'info'" size="small">
              {{ row.recognition_type === 'cloud' ? '云端' : '本地' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="is_collected" label="是否收藏" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.is_collected ? 'warning' : 'info'" size="small">
              <el-icon v-if="row.is_collected"><StarFilled /></el-icon>
              <el-icon v-else><Star /></el-icon>
              {{ row.is_collected ? '已收藏' : '未收藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sync_status" label="同步状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getSyncStatusType(row.sync_status)" size="small" effect="dark">
              {{ getSyncStatusText(row.sync_status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="识别时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleView(row)">
              <el-icon><View /></el-icon> 查看
            </el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination"
      />
    </el-card>

    <el-dialog v-model="viewDialogVisible" title="识别记录详情" width="700px" class="detail-dialog">
      <el-descriptions :column="2" border v-if="currentRecord" class="detail-descriptions">
        <el-descriptions-item label="记录ID">{{ currentRecord.id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentRecord.user_id }}</el-descriptions-item>
        <el-descriptions-item label="果蔬名称">{{ currentRecord.fruit_veg_name }}</el-descriptions-item>
        <el-descriptions-item label="置信度">
          <el-progress
            :percentage="(currentRecord.confidence || 0) * 100"
            :color="getConfidenceColor(currentRecord.confidence)"
          />
        </el-descriptions-item>
        <el-descriptions-item label="分类ID">{{ currentRecord.class_id }}</el-descriptions-item>
        <el-descriptions-item label="识别类型">
          <el-tag :type="currentRecord.recognition_type === 'cloud' ? 'primary' : 'info'">
            {{ currentRecord.recognition_type === 'cloud' ? '云端' : '本地' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="是否收藏">
          <el-tag :type="currentRecord.is_collected ? 'warning' : 'info'">
            <el-icon v-if="currentRecord.is_collected"><StarFilled /></el-icon>
            <el-icon v-else><Star /></el-icon>
            {{ currentRecord.is_collected ? '已收藏' : '未收藏' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="同步状态">
          <el-tag :type="getSyncStatusType(currentRecord.sync_status)" effect="dark">
            {{ getSyncStatusText(currentRecord.sync_status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="识别时间" :span="2">{{ currentRecord.created_at }}</el-descriptions-item>
        <el-descriptions-item label="图片" :span="2">
          <el-image
            :src="currentRecord.image_url || 'https://via.placeholder.com/200x200'"
            fit="cover"
            class="detail-image"
            :preview-src-list="[currentRecord.image_url || 'https://via.placeholder.com/200x200']"
            preview-teleported
          />
        </el-descriptions-item>
        <el-descriptions-item label="营养数据" :span="2">
          <pre v-if="currentRecord.nutrition_data" class="nutrition-data">{{ JSON.stringify(currentRecord.nutrition_data, null, 2) }}</pre>
          <span v-else class="no-data">暂无数据</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="viewDialogVisible = false">
          <el-icon><Close /></el-icon> 关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { recordApi } from '@/api/record'
import { debounce, requestCache } from '@/utils/performance'

const loading = ref(false)
const viewDialogVisible = ref(false)
const currentRecord = ref(null)
const selectedRecords = ref([])

const searchForm = reactive({
  fruitName: '',
  recognitionType: '',
  isCollected: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const tableData = ref([])

const getConfidenceColor = (confidence) => {
  if (confidence >= 0.9) return '#67C23A'
  if (confidence >= 0.8) return '#E6A23C'
  return '#F56C6C'
}

const getSyncStatusType = (status) => {
  const map = {
    pending: 'info',
    synced: 'success',
    failed: 'danger'
  }
  return map[status] || 'info'
}

const getSyncStatusText = (status) => {
  const map = {
    pending: '待同步',
    synced: '已同步',
    failed: '同步失败'
  }
  return map[status] || status
}

const loadData = async (useCache = true) => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      limit: pagination.size
    }
    if (searchForm.fruitName) {
      params.fruit_name = searchForm.fruitName
    }
    if (searchForm.recognitionType) {
      params.recognition_type = searchForm.recognitionType
    }
    if (searchForm.isCollected !== '') {
      params.is_collected = searchForm.isCollected
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.start_date = searchForm.dateRange[0]
      params.end_date = searchForm.dateRange[1]
    }
    
    const cacheKey = `records_${JSON.stringify(params)}`
    
    if (useCache) {
      const cached = requestCache.get(cacheKey)
      if (cached) {
        tableData.value = cached.data?.list || []
        pagination.total = cached.data?.total || 0
        loading.value = false
        return
      }
    }
    
    const res = await recordApi.getRecords(params)
    if (res.success && res.data) {
      tableData.value = res.data.list || []
      pagination.total = res.data.total || 0
      requestCache.set(cacheKey, res)
    } else if (res.success === false) {
      console.log('[Records] API返回成功标志为false:', res)
    }
  } catch (error) {
    console.log('[Records] 加载数据失败，使用缓存或空数据', error)
    const cacheKey = `records_${JSON.stringify({
      page: pagination.page,
      limit: pagination.size,
      fruit_name: searchForm.fruitName,
      recognition_type: searchForm.recognitionType,
      is_collected: searchForm.isCollected,
      start_date: searchForm.dateRange?.[0],
      end_date: searchForm.dateRange?.[1]
    })}`
    const cached = requestCache.get(cacheKey)
    if (cached) {
      tableData.value = cached.data?.list || []
      pagination.total = cached.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

const debouncedSearch = debounce(() => {
  pagination.page = 1
  loadData(false)
}, 400)

const handleSearch = () => {
  pagination.page = 1
  loadData(false)
}

watch(() => searchForm.fruitName, debouncedSearch)
watch(() => searchForm.recognitionType, debouncedSearch)
watch(() => searchForm.isCollected, debouncedSearch)

const handleReset = () => {
  searchForm.fruitName = ''
  searchForm.recognitionType = ''
  searchForm.isCollected = ''
  searchForm.dateRange = []
  pagination.page = 1
  loadData()
}

const handleSelectionChange = (selection) => {
  selectedRecords.value = selection
}

const handleView = (row) => {
  currentRecord.value = row
  viewDialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除这条记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await recordApi.deleteRecord(row.id)
      if (res.success) {
        ElMessage.success('删除成功')
        loadData()
      }
    } catch (error) {
      console.error('[Records] 删除记录失败', error)
      ElMessage.error('删除记录失败')
    }
  }).catch(() => {})
}

const handleBatchDelete = () => {
  if (selectedRecords.value.length === 0) {
    ElMessage.warning('请先选择要删除的记录')
    return
  }
  ElMessageBox.confirm(`确定要删除选中的 ${selectedRecords.value.length} 条记录吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const ids = selectedRecords.value.map(item => item.id)
      const res = await recordApi.batchDeleteRecords(ids)
      if (res.success) {
        ElMessage.success('批量删除成功')
        loadData()
      }
    } catch (error) {
      console.error('[Records] 批量删除失败', error)
      ElMessage.error('批量删除失败')
    }
  }).catch(() => {})
}

const handleSizeChange = (val) => {
  pagination.size = val
  loadData()
}

const handleCurrentChange = (val) => {
  pagination.page = val
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.records-container {
  padding: 0;
}

.search-card {
  margin-bottom: 20px;
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.table-card {
  margin-bottom: 20px;
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
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

.records-table {
  border-radius: 8px;
  overflow: hidden;
}

.record-image {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.record-image:hover {
  transform: scale(1.1);
}

.confidence-text {
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

.detail-image {
  width: 200px;
  height: 200px;
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.nutrition-data {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 8px;
  font-size: 12px;
  max-height: 150px;
  overflow-y: auto;
}

.no-data {
  color: #999;
  font-style: italic;
}
</style>
