<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import Tabbar from '../components/Tabbar.vue'

const router = useRouter()
const auth = useAuthStore()

const items = [
  { label: '约课', to: '/book', core: true },
  { label: '请假', to: '/leave' },
  { label: '历史记录', to: '/history' },
  { label: '我的信息', to: '/mine' },
  { label: '合同', to: '' },
  { label: '商品售卖', to: '' },
  { label: '特惠开团', to: '' },
  { label: '公众号', to: '' },
]
function go(to: string) {
  if (to) router.push(to)
  else alert('该模块为 P1/P2，V0.1 暂未开放')
}
</script>

<template>
  <div class="page">
    <div class="banner">
      <h1>通用师生约课平台</h1>
      <p>找好老师，约好每一课</p>
    </div>

    <div class="card row" v-if="auth.isStudent">
      <span class="muted">剩余课时</span>
      <b style="font-size:20px;color:var(--sun-deep)">{{ auth.user?.credits }} 节</b>
    </div>

    <div class="grid">
      <div v-for="it in items" :key="it.label" class="item" :class="{ core: it.core }" @click="go(it.to)">
        <span class="ic"></span>{{ it.label }}
      </div>
    </div>

    <p class="muted mt">v0.1 · 概念验证版</p>
    <Tabbar />
  </div>
</template>
