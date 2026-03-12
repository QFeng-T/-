<template>
  <div class="users-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable />
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
          <span>用户列表</span>
          <el-button type="primary" size="small" @click="handleAdd">新增用户</el-button>
        </div>
      </template>
      
      <el-table :data="tableData" stripe border v-loading="loading">
        <el-table-column prop="id" label="用户ID" width="80" />
        <el-table-column prop="uid" label="唯一标识" width="120" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="phone_number" label="手机号" width="130" />
        <el-table-column prop="login_type" label="登录类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.login_type === 'phone' ? 'success' : 'info'">
              {{ row.login_type === 'phone' ? '手机号' : '游客' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cloud_sync_switch" label="云端同步" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.cloud_sync_switch" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="180" />
        <el-table-column prop="last_login" label="最后登录" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="userForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone_number">
          <el-input v-model="userForm.phone_number" placeholder="请输入手机号" />
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
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

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
  size: 10,
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

const tableData = ref([
  {
    id: 1,
    uid: '10000001',
    username: 'user001',
    nickname: '小明',
    email: 'xiaoming@example.com',
    phone_number: '138****1234',
    login_type: 'phone',
    cloud_sync_switch: true,
    created_at: '2026-03-01 10:30:00',
    last_login: '2026-03-12 08:15:00'
  },
  {
    id: 2,
    uid: '10000002',
    username: 'user002',
    nickname: '小红',
    email: 'xiaohong@example.com',
    phone_number: '139****5678',
    login_type: 'phone',
    cloud_sync_switch: true,
    created_at: '2026-03-02 14:20:00',
    last_login: '2026-03-11 16:45:00'
  },
  {
    id: 3,
    uid: null,
    username: 'guest_001',
    nickname: null,
    email: null,
    phone_number: null,
    login_type: 'guest',
    cloud_sync_switch: false,
    created_at: '2026-03-03 09:10:00',
    last_login: '2026-03-10 12:30:00'
  },
  {
    id: 4,
    uid: '10000004',
    username: 'user004',
    nickname: '小华',
    email: 'xiaohua@example.com',
    phone_number: '137****9012',
    login_type: 'phone',
    cloud_sync_switch: true,
    created_at: '2026-03-05 16:40:00',
    last_login: '2026-03-12 09:20:00'
  },
  {
    id: 5,
    uid: null,
    username: 'guest_002',
    nickname: null,
    email: null,
    phone_number: null,
    login_type: 'guest',
    cloud_sync_switch: false,
    created_at: '2026-03-08 11:25:00',
    last_login: '2026-03-09 14:10:00'
  }
])

const loadData = () => {
  loading.value = true
  pagination.total = tableData.value.length
  loading.value = false
}

const handleSearch = () => {
  ElMessage.success('搜索功能待实现')
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.phone = ''
  loadData()
}

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
  }).then(() => {
    const index = tableData.value.findIndex(item => item.id === row.id)
    if (index > -1) {
      tableData.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  }).catch(() => {})
}

const handleSubmit = async () => {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate((valid) => {
    if (valid) {
      submitLoading.value = true
      setTimeout(() => {
        if (userForm.id) {
          const index = tableData.value.findIndex(item => item.id === userForm.id)
          if (index > -1) {
            tableData.value[index] = { ...userForm }
          }
          ElMessage.success('更新成功')
        } else {
          userForm.id = tableData.value.length + 1
          userForm.uid = userForm.login_type === 'phone' ? `1000000${userForm.id}` : null
          userForm.created_at = new Date().toLocaleString()
          userForm.last_login = new Date().toLocaleString()
          tableData.value.push({ ...userForm })
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        submitLoading.value = false
      }, 500)
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
