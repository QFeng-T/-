<template>
  <div class="users-container">
    <el-card class="search-card">
      <template #header>
        <div class="card-header">
          <el-icon class="card-icon"><Search /></el-icon>
          <span class="card-title">搜索筛选</span>
        </div>
      </template>
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable prefix-icon="User" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable prefix-icon="Phone" />
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
            <el-icon class="card-icon"><UserFilled /></el-icon>
            <span class="card-title">用户列表</span>
          </div>
          <el-button type="primary" size="small" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增用户
          </el-button>
        </div>
      </template>
      
      <el-table :data="tableData" stripe v-loading="loading" class="users-table">
        <el-table-column prop="id" label="用户ID" width="80" align="center" />
        <el-table-column prop="uid" label="唯一标识" width="120" align="center" />
        <el-table-column prop="username" label="用户名" width="150">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="32" :src="row.avatar" :icon="User" />
              <span class="username">{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="phone_number" label="手机号" width="130" />
        <el-table-column prop="login_type" label="登录类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.login_type === 'phone' ? 'success' : 'info'" effect="dark" size="small">
              {{ row.login_type === 'phone' ? '手机号' : '游客' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cloud_sync_switch" label="云端同步" width="100" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.cloud_sync_switch" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="180" />
        <el-table-column prop="last_login" label="最后登录" width="180" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      :close-on-click-modal="false"
      class="user-dialog"
    >
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="userForm.nickname" placeholder="请输入昵称" prefix-icon="UserFilled" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" prefix-icon="Message" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone_number">
          <el-input v-model="userForm.phone_number" placeholder="请输入手机号" prefix-icon="Phone" />
        </el-form-item>
        <el-form-item label="登录类型" prop="login_type">
          <el-select v-model="userForm.login_type" placeholder="请选择登录类型" style="width: 100%">
            <el-option label="手机号" value="phone" />
            <el-option label="游客" value="guest" />
          </el-select>
        </el-form-item>
        <el-form-item label="云端同步" prop="cloud_sync_switch">
          <el-switch v-model="userForm.cloud_sync_switch" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          <el-icon><Close /></el-icon> 取消
        </el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          <el-icon><Check /></el-icon> 确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi } from '@/api/user'
import { debounce, requestCache } from '@/utils/performance'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const userFormRef = ref(null)

const searchForm = reactive({
  username: '',
  phone: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const userForm = reactive({
  id: null,
  username: '',
  nickname: '',
  email: '',
  phone_number: '',
  login_type: 'guest',
  cloud_sync_switch: false
})

const userRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}

const tableData = ref([])

const loadData = async (useCache = true) => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      limit: pagination.size
    }
    if (searchForm.username) {
      params.search = searchForm.username
    }
    
    const cacheKey = `users_${JSON.stringify(params)}`
    
    if (useCache) {
      const cached = requestCache.get(cacheKey)
      if (cached) {
        tableData.value = cached.data?.list || []
        pagination.total = cached.data?.total || 0
        loading.value = false
        return
      }
    }
    
    const res = await userApi.getUsers(params)
    if (res.success && res.data) {
      tableData.value = res.data.list || []
      pagination.total = res.data.total || 0
      requestCache.set(cacheKey, res)
    } else if (res.success === false) {
      console.log('[Users] API返回成功标志为false:', res)
    }
  } catch (error) {
    console.log('[Users] 加载数据失败，使用缓存或空数据', error)
    const cacheKey = `users_${JSON.stringify({
      page: pagination.page,
      limit: pagination.size,
      search: searchForm.username
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
}, 300)

const handleSearch = () => {
  pagination.page = 1
  loadData(false)
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.phone = ''
  pagination.page = 1
  loadData(false)
}

watch(() => searchForm.username, debouncedSearch)
watch(() => searchForm.phone, debouncedSearch)

const handleAdd = () => {
  dialogTitle.value = '新增用户'
  Object.assign(userForm, {
    id: null,
    username: '',
    nickname: '',
    email: '',
    phone_number: '',
    login_type: 'guest',
    cloud_sync_switch: false
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑用户'
  Object.assign(userForm, row)
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除用户 "${row.username}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await userApi.deleteUser(row.id)
      if (res.success) {
        ElMessage.success('删除成功')
        loadData()
      }
    } catch (error) {
      console.error('[Users] 删除用户失败', error)
      ElMessage.error('删除用户失败')
    }
  }).catch(() => {})
}

const handleSubmit = async () => {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (userForm.id) {
          const res = await userApi.updateUser(userForm)
          if (res.success) {
            ElMessage.success('更新成功')
            dialogVisible.value = false
            loadData()
          }
        } else {
          const res = await userApi.addUser(userForm)
          if (res.success) {
            ElMessage.success('添加成功')
            dialogVisible.value = false
            loadData()
          }
        }
      } catch (error) {
        console.error('[Users] 提交失败', error)
        ElMessage.error('提交失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
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
.users-container {
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

.users-table {
  border-radius: 8px;
  overflow: hidden;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.username {
  font-weight: 500;
  color: #333;
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
</style>
