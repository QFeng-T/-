<template>
  <div class="dashboard-container">
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalUsers.toLocaleString() }}</div>
              <div class="stat-label">用户总数</div>
            </div>
          </div>
          <div class="stat-trend">
            <span class="trend-up"><el-icon><Top /></el-icon> 12%</span>
            <span class="trend-text">较上月</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
              <el-icon><Camera /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalRecords.toLocaleString() }}</div>
              <div class="stat-label">识别次数</div>
            </div>
          </div>
          <div class="stat-trend">
            <span class="trend-up"><el-icon><Top /></el-icon> 25%</span>
            <span class="trend-text">较上月</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
              <el-icon><Star /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalFavorites.toLocaleString() }}</div>
              <div class="stat-label">收藏总数</div>
            </div>
          </div>
          <div class="stat-trend">
            <span class="trend-up"><el-icon><Top /></el-icon> 18%</span>
            <span class="trend-text">较上月</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
              <el-icon><DataLine /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.avgAccuracy }}%</div>
              <div class="stat-label">平均准确率</div>
            </div>
          </div>
          <div class="stat-trend">
            <span class="trend-up"><el-icon><Top /></el-icon> 3%</span>
            <span class="trend-text">较上月</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :xs="24" :lg="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">识别趋势</span>
              <el-radio-group v-model="chartPeriod" size="small">
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
                <el-radio-button label="year">本年</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card class="chart-card">
          <template #header>
            <span class="card-title">果蔬识别分布</span>
          </template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :xs="24" :md="12">
        <el-card class="chart-card">
          <template #header>
            <span class="card-title">登录类型分布</span>
          </template>
          <div ref="loginTypeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="chart-card">
          <template #header>
            <span class="card-title">识别类型分布</span>
          </template>
          <div ref="recognitionTypeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row style="margin-top: 20px">
      <el-col :span="24">
        <el-card class="table-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">最近识别记录</span>
              <el-button type="primary" size="small" link @click="$router.push('/records')">
                查看全部 <el-icon class="link-icon"><Right /></el-icon>
              </el-button>
            </div>
          </template>
          <el-table :data="recentRecords" stripe v-loading="loading">
            <el-table-column prop="id" label="记录ID" width="80" />
            <el-table-column label="图片" width="80">
              <template #default="{ row }">
                <el-image
                  :src="row.image_url"
                  fit="cover"
                  class="table-image"
                  :preview-src-list="[row.image_url]"
                  preview-teleported
                />
              </template>
            </el-table-column>
            <el-table-column prop="fruit_veg_name" label="果蔬名称" min-width="120">
              <template #default="{ row }">
                <el-tag type="success" size="small">{{ row.fruit_veg_name }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="confidence" label="置信度" width="150">
              <template #default="{ row }">
                <el-progress
                  :percentage="row.confidence * 100"
                  :color="getConfidenceColor(row.confidence)"
                  :stroke-width="8"
                />
              </template>
            </el-table-column>
            <el-table-column prop="user_id" label="用户ID" width="100" />
            <el-table-column prop="recognition_type" label="识别类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.recognition_type === 'cloud' ? 'primary' : 'info'" size="small">
                  {{ row.recognition_type === 'cloud' ? '云端' : '本地' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="created_at" label="识别时间" min-width="180" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { dashboardApi } from '@/api/dashboard'

const router = useRouter()
const loading = ref(false)

const stats = reactive({
  totalUsers: 1234,
  totalRecords: 5678,
  totalFavorites: 987,
  avgAccuracy: 88.5
})

const chartPeriod = ref('week')
const trendChartRef = ref(null)
const pieChartRef = ref(null)
const loginTypeChartRef = ref(null)
const recognitionTypeChartRef = ref(null)

const recentRecords = ref([
  {
    id: 1,
    image_url: 'https://via.placeholder.com/60x60/FF6B6B/ffffff?text=番茄',
    fruit_veg_name: '番茄',
    confidence: 0.95,
    user_id: 1,
    recognition_type: 'cloud',
    created_at: '2026-03-12 10:30:00'
  },
  {
    id: 2,
    image_url: 'https://via.placeholder.com/60x60/4CAF50/ffffff?text=黄瓜',
    fruit_veg_name: '黄瓜',
    confidence: 0.88,
    user_id: 2,
    recognition_type: 'local',
    created_at: '2026-03-12 09:15:00'
  },
  {
    id: 3,
    image_url: 'https://via.placeholder.com/60x60/FF9800/ffffff?text=苹果',
    fruit_veg_name: '苹果',
    confidence: 0.92,
    user_id: 1,
    recognition_type: 'cloud',
    created_at: '2026-03-11 16:40:00'
  },
  {
    id: 4,
    image_url: 'https://via.placeholder.com/60x60/FFEB3B/000000?text=香蕉',
    fruit_veg_name: '香蕉',
    confidence: 0.85,
    user_id: 4,
    recognition_type: 'local',
    created_at: '2026-03-11 14:20:00'
  },
  {
    id: 5,
    image_url: 'https://via.placeholder.com/60x60/FF6B6B/ffffff?text=番茄',
    fruit_veg_name: '番茄',
    confidence: 0.78,
    user_id: 2,
    recognition_type: 'cloud',
    created_at: '2026-03-10 11:30:00'
  }
])

let trendChart = null
let pieChart = null
let loginTypeChart = null
let recognitionTypeChart = null

const getConfidenceColor = (confidence) => {
  if (confidence >= 0.9) return '#67C23A'
  if (confidence >= 0.8) return '#E6A23C'
  return '#F56C6C'
}

const initTrendChart = () => {
  if (!trendChartRef.value) return

  trendChart = echarts.init(trendChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e0e0e0',
      textStyle: {
        color: '#333'
      }
    },
    legend: {
      data: ['识别次数', '用户数'],
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisLine: {
        lineStyle: {
          color: '#e0e0e0'
        }
      }
    },
    yAxis: {
      type: 'value',
      splitLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      }
    },
    series: [
      {
        name: '识别次数',
        type: 'line',
        smooth: true,
        data: [120, 132, 101, 134, 90, 230, 210],
        itemStyle: { color: '#667eea' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(102, 126, 234, 0.5)' },
            { offset: 1, color: 'rgba(102, 126, 234, 0.1)' }
          ])
        }
      },
      {
        name: '用户数',
        type: 'line',
        smooth: true,
        data: [220, 182, 191, 234, 290, 330, 310],
        itemStyle: { color: '#f5576c' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(245, 87, 108, 0.5)' },
            { offset: 1, color: 'rgba(245, 87, 108, 0.1)' }
          ])
        }
      }
    ]
  }

  trendChart.setOption(option)
}

const initPieChart = () => {
  if (!pieChartRef.value) return

  pieChart = echarts.init(pieChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e0e0e0'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center'
    },
    series: [
      {
        name: '果蔬分布',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '14',
            fontWeight: 'bold'
          }
        },
        data: [
          { value: 1048, name: '番茄', itemStyle: { color: '#FF6B6B' } },
          { value: 735, name: '黄瓜', itemStyle: { color: '#4CAF50' } },
          { value: 580, name: '苹果', itemStyle: { color: '#FF9800' } },
          { value: 484, name: '香蕉', itemStyle: { color: '#FFEB3B' } },
          { value: 300, name: '其他', itemStyle: { color: '#9C27B0' } }
        ]
      }
    ]
  }

  pieChart.setOption(option)
}

const initLoginTypeChart = () => {
  if (!loginTypeChartRef.value) return

  loginTypeChart = echarts.init(loginTypeChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e0e0e0'
    },
    series: [
      {
        name: '登录类型',
        type: 'pie',
        radius: '60%',
        data: [
          { value: 735, name: '手机号登录', itemStyle: { color: '#667eea' } },
          { value: 580, name: '游客登录', itemStyle: { color: '#4facfe' } }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  loginTypeChart.setOption(option)
}

const initRecognitionTypeChart = () => {
  if (!recognitionTypeChartRef.value) return

  recognitionTypeChart = echarts.init(recognitionTypeChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e0e0e0'
    },
    series: [
      {
        name: '识别类型',
        type: 'pie',
        radius: '60%',
        data: [
          { value: 820, name: '云端识别', itemStyle: { color: '#43e97b' } },
          { value: 540, name: '本地识别', itemStyle: { color: '#f093fb' } }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  recognitionTypeChart.setOption(option)
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await dashboardApi.getStats()
    if (res.success) {
      Object.assign(stats, res.data)
    }
  } catch (error) {
    console.log('[Dashboard] 使用模拟数据')
  } finally {
    loading.value = false
  }
}

const handleResize = () => {
  trendChart?.resize()
  pieChart?.resize()
  loginTypeChart?.resize()
  recognitionTypeChart?.resize()
}

watch(chartPeriod, () => {
  initTrendChart()
})

onMounted(() => {
  loadData()
  setTimeout(() => {
    initTrendChart()
    initPieChart()
    initLoginTypeChart()
    initRecognitionTypeChart()
  }, 100)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
  loginTypeChart?.dispose()
  recognitionTypeChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  padding: 0;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
  border: none;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 10px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin-top: 5px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.trend-up {
  color: #67C23A;
  display: flex;
  align-items: center;
  gap: 2px;
  font-weight: 500;
}

.trend-text {
  color: #999;
}

.chart-card,
.table-card {
  border-radius: 12px;
  border: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.chart-container {
  height: 350px;
  width: 100%;
}

.table-image {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.table-image:hover {
  transform: scale(1.1);
}

.link-icon {
  margin-left: 4px;
  font-size: 12px;
}
</style>
