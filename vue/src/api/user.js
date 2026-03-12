import request from './index'

export const userApi = {
  login(data) {
    console.log('[API] 登录接口待实现', data)
    return new Promise(resolve => {
      setTimeout(() => {
        resolve({
          success: true,
          data: {
            token: 'mock-token-' + Date.now(),
            user: {
              id: 1,
              username: 'admin',
              nickname: '管理员'
            }
          }
        })
      }, 500)
    })
  },

  getUsers(params) {
    console.log('[API] 获取用户列表接口待实现', params)
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

  addUser(data) {
    console.log('[API] 新增用户接口待实现', data)
    return new Promise(resolve => {
      setTimeout(() => {
        resolve({ success: true, message: '添加成功' })
      }, 300)
    })
  },

  updateUser(data) {
    console.log('[API] 更新用户接口待实现', data)
    return new Promise(resolve => {
      setTimeout(() => {
        resolve({ success: true, message: '更新成功' })
      }, 300)
    })
  },

  deleteUser(id) {
    console.log('[API] 删除用户接口待实现', id)
    return new Promise(resolve => {
      setTimeout(() => {
        resolve({ success: true, message: '删除成功' })
      }, 300)
    })
  }
}
