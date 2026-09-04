// 退出登录并回登录页（原 4 个视图各自重复 auth.logout()+router.replace）
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

export function useLogout() {
  const auth = useAuthStore()
  const router = useRouter()
  return function logout() {
    auth.logout()
    router.replace('/login')
  }
}
