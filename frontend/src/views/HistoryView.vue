<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '../api'
import { useAuthStore } from '../stores/auth'
import type { Booking } from '../types'
import Tabbar from '../components/Tabbar.vue'
import { BOOKING_STATUS } from '../utils/status'
import { mdHm } from '../utils/datetime'

const auth = useAuthStore()
const bookings = ref<Booking[]>([])
onMounted(async () => { bookings.value = await api.listBookings() })
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
          <div class="row">
            <b>{{ mdHm(b.startAt) }} · {{ b.teacherName }}</b>
            <span class="st" :class="BOOKING_STATUS[b.status]?.cls">{{ BOOKING_STATUS[b.status]?.text }}</span>
          </div>
          <p class="muted" style="margin:4px 0 0">{{ b.subjectName }}</p>
        </div>
      </div>
      <p v-if="!bookings.length" class="muted">暂无记录，去约第一节课吧</p>
    </div>
    <Tabbar />
  </div>
</template>
