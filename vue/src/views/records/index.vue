<template>
  <div class="records-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="果蔬名称">
          <el-input v-model="searchForm.fruitName" placeholder="请输入果蔬名称" clearable />
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
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
        <span>识别记录列表</span>
        <el-button type="danger" size="small" @click="handleBatchDelete">批量删除</el-button>
      </div>
      </template>
      
      <el-table :data="tableData" stripe border v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="记录ID" width="80" />
        <el-table-column prop="user_id" label="用户ID" width="100" />
        <el-table-column label="图片" width="100">
          <template #default="{ row }">
            <el-image
              :src="row.image_url || 'https://via.placeholder.com/60x60'"
              fit="cover"
              style="width: 60px; height: 60px; border-radius: 4px"
              :preview-src-list="[row.image_url || 'https://via.placeholder.com/60x60']"
            />
          </template>
        </el-table-column>
        <el-table-column prop="fruit_veg_name" label="果蔬名称" width="120" />
        <el-table-column prop="confidence" label="置信度" width="100">
          <template #default="{ row }">
            <el-progress :percentage="row.confidence * 100" :color="getConfidenceColor(row.confidence)" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column prop="class_id" label="分类ID" width="80" />
        <el-table-column prop="recognition_type" label="识别类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.recognition_type === 'cloud' ? 'success' : 'info'">
              {{ row.recognition_type === 'cloud' ? '云端' : '本地' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="is_collected" label="是否收藏" width="100">
          <template #default="{ row }">
            <el-tag :type="row.is_collected ? 'warning' : 'info'">
              {{ row.is_collected ? '已收藏' : '未收藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sync_status" label="同步状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getSyncStatusType(row.sync_status)">
              {{ getSyncStatusText(row.sync_status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="识别时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleView(row)">查看</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
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
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <el-dialog v-model="viewDialogVisible" title="识别记录详情" width="700px">
      <el-descriptions :column="2" border v-if="currentRecord">
        <el-descriptions-item label="记录ID">{{ currentRecord.id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentRecord.user_id }}</el-descriptions-item>
        <el-descriptions-item label="果蔬名称">{{ currentRecord.fruit_veg_name }}</el-descriptions-item>
        <el-descriptions-item label="置信度">
          <el-progress :percentage="currentRecord.confidence * 100" :color="getConfidenceColor(currentRecord.confidence)" />
        </el-descriptions-item>
        <el-descriptions-item label="分类ID">{{ currentRecord.class_id }}</el-descriptions-item>
        <el-descriptions-item label="识别类型">
          <el-tag :type="currentRecord.recognition_type === 'cloud' ? 'success' : 'info'">
            {{ currentRecord.recognition_type === 'cloud' ? '云端' : '本地' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="是否收藏">
          <el-tag :type="currentRecord.is_collected ? 'warning' : 'info'">
            {{ currentRecord.is_collected ? '已收藏' : '未收藏' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="同步状态">
          <el-tag :type="getSyncStatusType(currentRecord.sync_status)">
            {{ getSyncStatusText(currentRecord.sync_status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="识别时间" :span="2">{{ currentRecord.created_at }}</el-descriptions-item>
        <el-descriptions-item label="图片" :span="2">
          <el-image
            :src="currentRecord.image_url || 'https://via.placeholder.com/200x200'"
            fit="cover"
            style="width: 200px; height: 200px"
          />
        </el-descriptions-item>
        <el-descriptions-item label="营养数据" :span="2">
          <pre v-if="currentRecord.nutrition_data">{{ JSON.stringify(currentRecord.nutrition_data, null, 2) }}</pre>
          <span v-else>暂无数据</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

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
  size: 10,
  total: 0
})

const tableData = ref([
  {
    id: 1,
    user_id: 1,
    fruit_veg_name: '番茄',
    confidence: 0.95,
    class_id: 0,
    image_url: 'https://via.placeholder.com/60x60/FF6B6B/ffffff?text=番茄',
    recognition_type: 'cloud',
    is_collected: true,
    sync_status: 'synced',
    created_at: '2026-03-12 10:30:00',
    nutrition_data: { calories: '18kcal', water: '95g', protein: '0.9g', vitamin_c: '19mg' }
  },
  {
    id: 2,
    user_id: 2,
    fruit_veg_name: '黄瓜',
    confidence: 0.88,
    class_id: 1,
    image_url: 'https://via.placeholder.com/60x60/4CAF50/ffffff?text=黄瓜',
    recognition_type: 'local',
    is_collected: false,
    sync_status: 'pending',
    created_at: '2026-03-12 09:15:00',
    nutrition_data: { calories: '16kcal', water: '96g', protein: '0.8g', vitamin_c: '9mg' }
  },
  {
    id: 3,
    user_id: 1,
    fruit_veg_name: '苹果',
    confidence: 0.92,
    class_id: 2,
    image_url: 'https://via.placeholder.com/60x60/FF9800/ffffff?text=苹果',
    recognition_type: 'cloud',
    is_collected: true,
    sync_status: 'synced',
    created_at: '2026-03-11 16:40:00',
    nutrition_data: { calories: '52kcal', water: '86g', protein: '0.2g', vitamin_c: '4mg' }
  },
  {
    id: 4,
    user_id: 4,
    fruit_veg_name: '香蕉',
    confidence: 0.85,
    class_id: 3,
    image_url: 'https://via.placeholder.com/60x60/FFEB3B/000000?text=香蕉',
    recognition_type: 'local',
    is_collected: false,
    sync_status: 'failed',
    created_at: '2026-03-11 14:20:00',
    nutrition_data: { calories: '89kcal', water: '75g', protein: '1.1g', vitamin_c: '8.7mg' }
  },
  {
    id: 5,
    user_id: 2,
    fruit_veg_name: '番茄',
    confidence: 0.78,
    class_id: 0,
    image_url: 'https://via.placeholder.com/60x60/FF6B6B/ffffff?text=番茄',
    recognition_type: 'cloud',
    is_collected: true,
    sync_status: 'synced',
    created_at: '2026-03-10 11:30:00',
    nutrition_data: { calories: '18kcal', water: '95g', protein: '0.9g', vitamin_c: '19mg' }
  }
])

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

const loadData = () => {
  loading.value = true
  pagination.total = tableData.value.length
  loading.value = false
}

const handleSearch = () => {
  ElMessage.success('搜索功能待实现')
}

const handleReset = () => {
  searchForm.fruitName = ''
  searchForm.recognitionType = ''
  searchForm.isCollected = ''
  searchForm.dateRange = []
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
  }).then(() => {
    const index = tableData.value.findIndex(item => item.id === row.id)
    if (index > -1) {
      tableData.value.splice(index, 1)
      ElMessage.success('删除成功')
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
  }).then(() => {
    const ids = selectedRecords.value.map(item => item.id)
    tableData.value = tableData.value.filter(item => !ids.includes(item.id))
    ElMessage.success('批量删除成功')
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
}

.table-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
