// Axios 实例（供登录页等直接使用原始 http）
import axios from 'axios'

export const http = axios.create({ baseURL: 'http://localhost:8080', timeout: 10000 })

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})
