import request from './index'

export const dashboardApi = {
  getStats() {
    return request({
      url: '/v1/admin/stats/overview',
      method: 'get'
    })
  },

  getTrendData(params) {
    return request({
      url: '/v1/admin/stats/trend',
      method: 'get',
      params
    })
  },

  getFruitDistribution() {
    return request({
      url: '/v1/admin/stats/fruit-distribution',
      method: 'get'
    })
  },

  getLoginTypeDistribution() {
    return request({
      url: '/v1/admin/stats/login-type',
      method: 'get'
    })
  },

  getRecognitionTypeDistribution() {
    return Promise.resolve({
      success: true,
      data: [
        { name: '本地识别', value: 0 },
        { name: '云端识别', value: 0 }
      ]
    })
  },

  getRecentRecords() {
    return request({
      url: '/v1/admin/records',
      method: 'get',
      params: { limit: 10 }
    })
  }
}
