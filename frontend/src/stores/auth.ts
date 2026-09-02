import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '../types'
import { api } from '../api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)

  const isLoggedIn = computed(() => !!user.value && user.value.role !== 'guest')
  const isStudent = computed(() => user.value?.role === 'student')
  const isGuest = computed(() => user.value?.role === 'guest')

  async function login(phone: string, password: string, role: 'student' | 'teacher' | 'admin',
                       subjectId?: number | null, name?: string) {
    const u = await api.login(phone, password, role, subjectId, name)
    user.value = u
    return u
  }
  function enterAsGuest() {
    user.value = { id: 0, role: 'guest', name: '游客', phone: '' }
  }
  function logout() {
    user.value = null
  }
  function consumeCredit() {
    if (user.value && user.value.credits != null) user.value.credits--
  }

  return { user, isLoggedIn, isStudent, isGuest, login, enterAsGuest, logout, consumeCredit }
})
