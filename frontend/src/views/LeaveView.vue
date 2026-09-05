<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { api } from '../api'
import type { Booking } from '../types'
import Tabbar from '../components/Tabbar.vue'
import { mdHmRange } from '../utils/datetime'

const bookings = ref<Booking[]>([])
const reason = ref('')
const target = ref<Booking | null>(null)
const done = ref(false)

onMounted(async () => { bookings.value = await api.listBookings() })
// 仅「未下课」(endAt 在未来) 的已确认课时可请假；已下课课程学生不可请假
const confirmed = computed(() => bookings.value.filter((b) => b.status === 1 && new Date(b.endAt).getTime() > Date.now()))
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
    <p class="muted">仅可对「未下课、已确认」的课时请假，提交后待老师审批；批准后时段释放、课时返还。已下课的课程不可请假。</p>

    <div class="card" v-if="done" style="border-top:6px solid var(--sun)">
      <b>✓ 请假已提交，待老师审批</b>
    </div>

    <div v-for="b in confirmed" :key="b.id" class="card">
      <div class="row">
        <b>{{ mdHmRange(b.startAt, b.endAt) }} · {{ b.teacherName }}</b>
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
