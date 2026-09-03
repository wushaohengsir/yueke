<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { api } from '../api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const date = String(route.params.date) // yyyy-MM-dd
const day = ref<any>(null)
const scheduleStatus = { free: '未预约', booked: '已预约', completed: '已完成' }
const scheduleClass = { free: 's3', booked: 's1', completed: 's2' }
const wdLabel = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

// 根据日期反推 weekOffset（该日期所在周相对本周的偏移）
function weekOffsetOf(dateStr: string): number {
  const d = new Date(dateStr + 'T00:00:00')
  const now = new Date()
  const curMonday = new Date(now)
  const curDay = (curMonday.getDay() + 6) % 7 // 周一=0
  curMonday.setDate(curMonday.getDate() - curDay)
  curMonday.setHours(0, 0, 0, 0)
  const targetMonday = new Date(d)
  const tDay = (targetMonday.getDay() + 6) % 7
  targetMonday.setDate(targetMonday.getDate() - tDay)
  targetMonday.setHours(0, 0, 0, 0)
  return Math.round((targetMonday.getTime() - curMonday.getTime()) / (7 * 24 * 3600 * 1000))
}

const dayLabel = computed(() => {
  const d = new Date(date + 'T00:00:00')
  return `${d.getMonth() + 1}月${d.getDate()}号 · ${wdLabel[(d.getDay() + 6) % 7]}`
})

onMounted(async () => {
  const data: any = await api.getWeekSchedule(weekOffsetOf(date))
  const found = (data.days || []).find((x: any) => x.date === date)
  day.value = found || { date, weekday: 1, slots: [] }
})

async function complete(id: number) {
  const r = await api.completeBooking(id)
  if (!r.ok) { alert(r.msg || '登记失败'); return }
  const data: any = await api.getWeekSchedule(weekOffsetOf(date))
  day.value = (data.days || []).find((x: any) => x.date === date) || day.value
}

function logout() { auth.logout(); router.replace('/login') }
</script>

<template>
  <div class="page">
    <div class="banner">
      <h1>课表详情</h1>
      <p>{{ auth.user?.name }} · {{ dayLabel }}</p>
    </div>

    <button class="btn ghost small" @click="router.back()">← 返回周课表</button>

    <div class="card mt">
      <b>{{ dayLabel }} 的时段安排</b>
    </div>

    <div class="card" v-for="s in (day && day.slots || [])" :key="(day.date + s.startTime + s.bookingId)">
      <div class="row">
        <span>{{ s.startTime }}-{{ s.endTime }}</span>
        <span class="st" :class="scheduleClass[s.status]">{{ scheduleStatus[s.status] }}</span>
      </div>
      <div class="row" v-if="s.status!=='free' && s.studentName" style="padding-top:6px">
        <span class="muted">{{ s.studentName }} · {{ s.subjectName }}</span>
        <button class="btn small" v-if="s.status==='booked'" @click="complete(s.bookingId)">登记完成</button>
      </div>
    </div>
    <p class="muted" v-if="!day || !day.slots.length" style="margin-top:12px">该日无安排</p>

    <button class="btn ghost mt" @click="logout">退出登录</button>
  </div>
</template>
