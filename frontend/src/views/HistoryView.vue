<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '../api/mock'
import { useAuthStore } from '../stores/auth'
import type { Booking } from '../types'
import Tabbar from '../components/Tabbar.vue'

const auth = useAuthStore()
const bookings = ref<Booking[]>([])
onMounted(async () => { bookings.value = await api.listBookings() })

const stText = ['待确认', '已确认', '已完成', '已取消', '已请假']
const stClass = ['s1', 's1', 's2', 's3', 's4']
const dotColor = ['var(--blue)', 'var(--blue)', 'var(--green)', '#ccc', 'var(--sun)']
function fmt(d: string) {
  const dt = new Date(d)
  return `${dt.getMonth() + 1}/${dt.getDate()} ${dt.getHours()}:00`
}
</script>

<template>
  <div class="page">
    <h2 class="page-title">历史记录</h2>
    <div class="card row">
      <span class="muted">剩余课时</span><b style="color:var(--sun-deep)">{{ auth.user?.credits }} 节</b>
    </div>

    <div class="card">
      <div class="timeline">
        <div v-for="b in bookings" :key="b.id" style="position:relative;margin-bottom:18px">
          <span class="dot" :style="`background:${dotColor[b.status]}`"></span>
          <div class="row">
            <b>{{ fmt(b.startAt) }} · {{ b.teacherName }}</b>
            <span class="st" :class="stClass[b.status]">{{ stText[b.status] }}</span>
          </div>
          <p class="muted" style="margin:4px 0 0">{{ b.subjectName }}</p>
        </div>
      </div>
      <p v-if="!bookings.length" class="muted">暂无记录，去约第一节课吧</p>
    </div>
    <Tabbar />
  </div>
</template>
