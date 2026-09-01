import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '../types'
import { api } from '../api/mock'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)

  const isLoggedIn = computed(() => !!user.value)
  const isStudent = computed(() => user.value?.role === 'student')

  async function login(phone: string, password: string, role: 'student' | 'teacher') {
    const u = await api.login(phone, password, role)
    user.value = u
    return u
  }
  function logout() {
    user.value = null
  }
  function consumeCredit() {
    if (user.value && user.value.credits != null) user.value.credits--
  }

  return { user, isLoggedIn, isStudent, login, logout, consumeCredit }
})
