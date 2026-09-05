// Axios 实例（供登录页等直接使用原始 http）
// 开发走 :8080；生产打包后走相对路径，由 nginx 把 /api 反代到后端容器（同源，避免 CORS）
import axios from 'axios'

export const http = axios.create({
  baseURL: import.meta.env.DEV ? 'http://localhost:8080' : '',
  timeout: 10000,
})

http.interceptors.request.use((cfg) => {
  const token = sessionStorage.getItem('token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})
