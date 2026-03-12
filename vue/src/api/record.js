import request from './index'

export const recordApi = {
  getRecords(params) {
    console.log('[API] 获取识别记录列表接口待实现', params)
    return new Promise(resolve => {
      setTimeout(() => {
        resolve({
          success: true,
          data: {
            list: [],
            total: 0
          }
        })
      }, 300)
    })
  },

  getRecordDetail(id) {
    console.log('[API] 获取识别记录详情接口待实现', id)
    return new Promise(resolve => {
      setTimeout(() => {
        resolve({
          success: true,
          data: {}
        })
      }, 300)
    })
  },

  deleteRecord(id) {
    console.log('[API] 删除识别记录接口待实现', id)
    return new Promise(resolve => {
      setTimeout(() => {
        resolve({ success: true, message: '删除成功' })
      }, 300)
    })
  },

  batchDeleteRecords(ids) {
    console.log('[API] 批量删除识别记录接口待实现', ids)
    return new Promise(resolve => {
      setTimeout(() => {
        resolve({ success: true, message: '批量删除成功' })
      }, 300)
    })
  }
}
