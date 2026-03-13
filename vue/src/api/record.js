import request from './index'

export const recordApi = {
  getRecords(params) {
    return request({
      url: '/v1/admin/records',
      method: 'get',
      params
    })
  },

  getRecordDetail(id) {
    return request({
      url: `/v1/records/${id}`,
      method: 'get'
    })
  },

  deleteRecord(id) {
    return request({
      url: `/v1/admin/records/${id}`,
      method: 'delete'
    })
  },

  batchDeleteRecords(ids) {
    return request({
      url: '/v1/admin/records/batch-delete',
      method: 'post',
      data: { ids }
    })
  }
}
