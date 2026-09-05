import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '../types'
import { api } from '../api'

const STORAGE_KEY = 'bookmate_user'

function loadUser(): User | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as User) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  // 登录态存 sessionStorage（按标签页隔离）：同一标签刷新(F5/地址栏回车)仍保持登录；
  // 不同标签可各自登录不同身份（如一个学生端、一个老师端），互不覆盖。
  // 不用 localStorage 的原因：全浏览器标签共享同一份，后登录者会覆盖先登录者，刷新即串身份。
  const user = ref<User | null>(loadUser())

  const isLoggedIn = computed(() => !!user.value && user.value.role !== 'guest')
  const isStudent = computed(() => user.value?.role === 'student')
  const isGuest = computed(() => user.value?.role === 'guest')

  function persist() {
    if (user.value) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user.value))
    else sessionStorage.removeItem(STORAGE_KEY)
  }

  async function login(phone: string, password: string, role: 'student' | 'teacher' | 'admin') {
    const u = await api.login(phone, password, role)
    user.value = u
    persist()
    return u
  }
  function enterAsGuest() {
    user.value = { id: 0, role: 'guest', name: '游客', phone: '' }
    persist()
  }
  function logout() {
    user.value = null
    sessionStorage.removeItem('token')
    persist()
  }
  // 用后端返回的最新资料覆盖本地缓存（如进入「我的」页刷新真实姓名）
  function refreshUser(u: User) {
    user.value = u
    persist()
  }

  return { user, isLoggedIn, isStudent, isGuest, login, enterAsGuest, logout, refreshUser }
})
