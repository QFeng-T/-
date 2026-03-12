import request from './index'

export const dashboardApi = {
  getStats() {
    console.log('[API] 获取统计数据接口待实现')
    return new Promise(resolve => {
      setTimeout(() => {
        resolve({
          success: true,
          data: {
            totalUsers: 0,
            totalRecords: 0,
            totalFavorites: 0,
            avgAccuracy: 0
          }
        })
      }, 300)
    })
  },

  getTrendData(params) {
    console.log('[API] 获取趋势数据接口待实现', params)
    return new Promise(resolve => {
      setTimeout(() => {
        resolve({
          success: true,
          data: {
            dates: [],
            records: [],
            users: []
          }
        })
      }, 300)
    })
  },

  getFruitDistribution() {
    console.log('[API] 获取果蔬分布数据接口待实现')
    return new Promise(resolve => {
      setTimeout(() => {
        resolve({
          success: true,
          data: []
        })
      }, 300)
    })
  },

  getLoginTypeDistribution() {
    console.log('[API] 获取登录类型分布接口待实现')
    return new Promise(resolve => {
      setTimeout(() => {
        resolve({
          success: true,
          data: []
        })
      }, 300)
    })
  },

  getRecognitionTypeDistribution() {
    console.log('[API] 获取识别类型分布接口待实现')
    return new Promise(resolve => {
      setTimeout(() => {
        resolve({
          success: true,
          data: []
        })
      }, 300)
    })
  },

  getRecentRecords() {
    console.log('[API] 获取最近识别记录接口待实现')
    return new Promise(resolve => {
      setTimeout(() => {
        resolve({
          success: true,
          data: []
        })
      }, 300)
    })
  }
}
