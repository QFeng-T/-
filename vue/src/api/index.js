import axios from 'axios'
import { ElMessage } from 'element-plus'
import { requestCache } from '@/utils/performance'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

const pendingRequests = new Map()

const generateRequestKey = (config) => {
  const { method, url, params, data } = config
  return [method, url, JSON.stringify(params), JSON.stringify(data)].join('&')
}

const addPendingRequest = (config) => {
  const key = generateRequestKey(config)
  if (!pendingRequests.has(key)) {
    config.cancelToken = config.cancelToken || new axios.CancelToken((cancel) => {
      pendingRequests.set(key, cancel)
    })
  }
}

const removePendingRequest = (config) => {
  const key = generateRequestKey(config)
  if (pendingRequests.has(key)) {
    const cancel = pendingRequests.get(key)
    cancel(key)
    pendingRequests.delete(key)
  }
}

request.interceptors.request.use(
  config => {
    removePendingRequest(config)
    addPendingRequest(config)
    
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    removePendingRequest(response.config)
    
    const config = response.config
    if (config.method === 'get' && config.cache !== false) {
      const key = generateRequestKey(config)
      requestCache.set(key, response.data)
    }
    
    return response.data
  },
  error => {
    if (error.config) {
      removePendingRequest(error.config)
      
      if (error.config.method === 'get' && error.config.cache !== false) {
        const key = generateRequestKey(error.config)
        const cachedData = requestCache.get(key)
        if (cachedData) {
          return cachedData
        }
      }
    }
    
    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }
    
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        localStorage.removeItem('isAuthenticated')
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export const cachedRequest = async (config) => {
  const key = generateRequestKey(config)
  const cached = requestCache.get(key)
  if (cached && config.cache !== false) {
    return cached
  }
  return request(config)
}

export default request
