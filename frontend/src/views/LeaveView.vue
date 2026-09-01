<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { api } from '../api'
import type { Booking } from '../types'
import Tabbar from '../components/Tabbar.vue'

const bookings = ref<Booking[]>([])
const reason = ref('')
const target = ref<Booking | null>(null)
const done = ref(false)

onMounted(async () => { bookings.value = await api.listBookings() })
const confirmed = computed(() => bookings.value.filter((b) => b.status === 1))

function fmt(d: string) {
  const dt = new Date(d)
  return `${dt.getMonth() + 1}/${dt.getDate()} ${dt.getHours()}:00`
}
async function submit() {
  if (!target.value || !reason.value) return
  await api.createLeave(target.value.id, reason.value)
  done.value = true
  bookings.value = await api.listBookings()
  target.value = null
  reason.value = ''
}
</script>

<template>
  <div class="page">
    <h2 class="page-title">请假</h2>
    <p class="muted">仅可对「已确认」课时请假，批准后时段自动释放、课时返还。</p>

    <div class="card" v-if="done" style="border-top:6px solid var(--green)">
      <b>✓ 请假已提交并获批（演示），时段已释放</b>
    </div>

    <div v-for="b in confirmed" :key="b.id" class="card">
      <div class="row">
        <b>{{ fmt(b.startAt) }} · {{ b.teacherName }}</b>
        <button class="btn small" @click="target = b">申请请假</button>
      </div>
      <p class="muted" style="margin:6px 0 0">{{ b.subjectName }}</p>
      <div v-if="target?.id === b.id" class="mt">
        <input class="input" v-model="reason" placeholder="请假事由" />
        <button class="btn" @click="submit">提交</button>
      </div>
    </div>
    <p v-if="!confirmed.length" class="muted">暂无可请假的课时</p>
    <Tabbar />
  </div>
</template>
