<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { api } from '../api'
import type { Credit } from '../types'
import Tabbar from '../components/Tabbar.vue'

const router = useRouter()
const auth = useAuthStore()
const credits = ref<Credit[]>([])
onMounted(async () => {
  if (auth.isStudent) credits.value = await api.getCredits()
})

const items = [
  { label: '约课', to: '/book' },
  { label: '请假', to: '/leave' },
  { label: '历史记录', to: '/history' },
  { label: '我的信息', to: '/mine' },
  { label: '合同', to: '/contract' },
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
      <p>红叶有信 · 好课有约</p>
    </div>

    <!-- 分课程课时（不通用） -->
    <div class="card" v-if="auth.isStudent">
      <div class="row" v-for="c in credits" :key="c.subjectId" style="padding:4px 0">
        <span class="muted">{{ c.subjectName }}（{{ c.category }}）</span>
        <b :style="c.remaining > 0 ? 'color:var(--sun-deep)' : 'color:var(--coral)'">剩余 {{ c.remaining }} 节</b>
      </div>
      <p class="muted" v-if="!credits.length" style="margin:0">暂无课时，请先购买课时包</p>
    </div>

    <div class="grid">
      <div v-for="it in items" :key="it.label" class="item" @click="go(it.to)">
        <span class="ic"></span>{{ it.label }}
      </div>
    </div>

    <Tabbar />
  </div>
</template>
