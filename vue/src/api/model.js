import request from './index'

export const modelApi = {
  getModels(params) {
    return request({
      url: '/v1/admin/models',
      method: 'get',
      params
    })
  },

  uploadModel(formData, params = {}, onUploadProgress) {
    return request({
      url: '/v1/admin/models/upload',
      method: 'post',
      data: formData,
      params: params,
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress
    })
  },

  activateModel(id, platform) {
    return request({
      url: `/v1/admin/models/${id}/activate`,
      method: 'post'
    })
  },

  deleteModel(id) {
    return request({
      url: `/v1/admin/models/${id}`,
      method: 'delete'
    })
  },

  getModelDetail(id) {
    return request({
      url: `/v1/admin/models/${id}`,
      method: 'get'
    })
  }
}
