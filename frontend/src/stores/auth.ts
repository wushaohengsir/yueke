import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '../types'
import { api } from '../api'

const STORAGE_KEY = 'bookmate_user'

function loadUser(): User | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as User) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  // 登录态持久化：刷新后仍保持登录
  const user = ref<User | null>(loadUser())

  const isLoggedIn = computed(() => !!user.value && user.value.role !== 'guest')
  const isStudent = computed(() => user.value?.role === 'student')
  const isGuest = computed(() => user.value?.role === 'guest')

  function persist() {
    if (user.value) localStorage.setItem(STORAGE_KEY, JSON.stringify(user.value))
    else localStorage.removeItem(STORAGE_KEY)
  }

  async function login(phone: string, password: string, role: 'student' | 'teacher' | 'admin',
                       subjectId?: number | null, name?: string) {
    const u = await api.login(phone, password, role, subjectId, name)
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
    localStorage.removeItem('token')
    persist()
  }
  function consumeCredit() {
    if (user.value && user.value.credits != null) user.value.credits--
    persist()
  }

  return { user, isLoggedIn, isStudent, isGuest, login, enterAsGuest, logout, consumeCredit }
})
