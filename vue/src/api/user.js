import request from './index'

export const userApi = {
  login(data) {
    return request({
      url: '/v1/admin/login',
      method: 'post',
      data
    })
  },

  getUsers(params) {
    return request({
      url: '/v1/admin/users',
      method: 'get',
      params
    })
  },

  addUser(data) {
    return request({
      url: '/v1/users',
      method: 'post',
      data
    })
  },

  updateUser(data) {
    return request({
      url: `/v1/admin/users/${data.id}`,
      method: 'put',
      data
    })
  },

  deleteUser(id) {
    return request({
      url: `/v1/admin/users/${id}`,
      method: 'delete'
    })
  }
}
