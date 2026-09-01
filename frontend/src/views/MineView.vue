<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import Tabbar from '../components/Tabbar.vue'

const router = useRouter()
const auth = useAuthStore()
function logout() {
  auth.logout()
  router.replace('/login')
}
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
      <p class="muted" v-if="auth.isStudent">剩余课时：{{ auth.user?.credits }} 节</p>
    </div>
    <div class="card">账号设置</div>
    <div class="card">消息通知</div>
    <button class="btn ghost mt" @click="logout">退出登录</button>
    <Tabbar />
  </div>
</template>
