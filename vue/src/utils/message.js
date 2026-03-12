import { ElMessage, ElMessageBox } from 'element-plus'

export const message = {
  success(msg) {
    return ElMessage({
      message: msg,
      type: 'success',
      duration: 2000
    })
  },

  error(msg) {
    return ElMessage({
      message: msg,
      type: 'error',
      duration: 3000
    })
  },

  warning(msg) {
    return ElMessage({
      message: msg,
      type: 'warning',
      duration: 2500
    })
  },

  info(msg) {
    return ElMessage({
      message: msg,
      type: 'info',
      duration: 2000
    })
  },

  confirm(msg, title = '提示') {
    return ElMessageBox.confirm(msg, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  }
}
