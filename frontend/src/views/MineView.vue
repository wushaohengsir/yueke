<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { api } from '../api'
import type { Credit } from '../types'
import Tabbar from '../components/Tabbar.vue'
import { useLogout } from '../composables/useLogout'

const auth = useAuthStore()
const logout = useLogout()
const credits = ref<Credit[]>([])

onMounted(async () => {
  // 按 token 拉取后端真实资料（姓名/手机号可能被管理员改名，避免显示旧缓存）
  if (auth.isLoggedIn) {
    try { auth.refreshUser(await api.getMe()) } catch { /* token 失效等，保持现状 */ }
  }
  if (auth.isStudent) credits.value = await api.getCredits()
})
</script>

<template>
  <div class="page">
    <h2 class="page-title">我的信息</h2>
    <div class="card">
      <div class="row">
        <b style="font-size:18px">{{ auth.user?.name }}</b>
        <span class="tag">{{ auth.user?.role === 'student' ? '学员' : '老师' }}</span>
      </div>
      <p class="muted" style="margin:8px 0 0">手机号：{{ auth.user?.phone }}</p>
    </div>

    <!-- 分课程课时（不通用） -->
    <h3 style="font-size:16px;margin:16px 0 10px">我的课程课时</h3>
    <div class="card" v-for="c in credits" :key="c.subjectId">
      <div class="row">
        <b>{{ c.subjectName }}</b>
        <span class="tag">{{ c.category }}</span>
      </div>
      <div class="row mt" style="font-size:14px">
        <span class="muted">总 {{ c.total }} 节 · 已用 {{ c.used }} 节</span>
        <b :style="c.remaining > 0 ? 'color:var(--green)' : 'color:var(--coral)'">剩余 {{ c.remaining }} 节</b>
      </div>
    </div>
    <p class="muted" v-if="auth.isStudent && !credits.length">暂无课程课时，请先购买课时包</p>

    <div class="card">账号设置</div>
    <div class="card">消息通知</div>
    <button class="btn ghost mt" @click="logout">退出登录</button>
    <Tabbar />
  </div>
</template>
