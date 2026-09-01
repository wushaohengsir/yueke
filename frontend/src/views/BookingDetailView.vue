<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api/mock'
import { useAuthStore } from '../stores/auth'
import type { Teacher, Timeslot } from '../types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const teacher = ref<Teacher>()
const slots = ref<Timeslot[]>([])
const selected = ref<Timeslot | null>(null)
const remark = ref('')
const result = ref<null | { ok: boolean; msg?: string }>(null)

const tid = Number(route.params.id)
onMounted(async () => {
  teacher.value = await api.getTeacher(tid)
  slots.value = await api.listSlots(tid)
})

// 按天分组，取前 7 天
const days = computed(() => {
  const map = new Map<string, Timeslot[]>()
  for (const s of slots.value) {
    const d = new Date(s.startAt).toDateString()
    if (!map.has(d)) map.set(d, [])
    map.get(d)!.push(s)
  }
  return [...map.entries()].slice(0, 7)
})
const activeDay = ref(0)
const daySlots = computed(() => days.value[activeDay.value]?.[1] ?? [])

function fmt(d: string) {
  const dt = new Date(d)
  return `${dt.getMonth() + 1}/${dt.getDate()} 周${'日一二三四五六'[dt.getDay()]}`
}
function hh(d: string) {
  const dt = new Date(d)
  return `${dt.getHours()}:00`
}

async function confirm() {
  if (!selected.value) return
  const r = await api.createBooking(tid, selected.value.id, auth.user!)
  if (r.ok) { auth.consumeCredit() }
  result.value = { ok: r.ok, msg: r.msg }
}
</script>

<template>
  <div class="page">
    <h2 class="page-title">选时段</h2>
    <div class="card" v-if="teacher">
      <b style="font-size:18px">{{ teacher.name }}</b>
      <span class="tag" style="margin-left:8px">{{ teacher.subjects[0].name }}</span>
      <p class="muted" style="margin:6px 0 0">{{ teacher.intro }}</p>
    </div>

    <!-- 日期条 -->
    <div style="display:flex;gap:8px;overflow-x:auto;margin-bottom:14px">
      <button v-for="(d, i) in days" :key="d[0]" class="tag"
        :style="i === activeDay ? 'background:var(--sun)' : ''" @click="activeDay = i">
        {{ fmt(d[1][0].startAt) }}
      </button>
    </div>

    <!-- 时段网格 -->
    <div class="slots">
      <div v-for="s in daySlots" :key="s.id" class="slot"
        :class="{ booked: s.status === 'booked', selected: selected?.id === s.id }"
        @click="s.status !== 'booked' && (selected = s)">
        {{ hh(s.startAt) }}<br /><small>{{ s.status === 'booked' ? '已约' : '可选' }}</small>
      </div>
    </div>

    <!-- 确认 -->
    <div class="card mt" v-if="selected">
      <div class="row"><span class="muted">时段</span><b>{{ fmt(selected.startAt) }} {{ hh(selected.startAt) }}</b></div>
      <div class="row mt"><span class="muted">消耗</span><b>1 课时（剩余 {{ auth.user?.credits }}）</b></div>
      <input class="input mt" v-model="remark" placeholder="备注（可选）" />
      <button class="btn" @click="confirm">确认预约</button>
    </div>

    <!-- 结果 -->
    <div class="card mt" v-if="result" :style="result.ok ? 'border-top:6px solid var(--green)' : 'border-top:6px solid var(--coral)'">
      <b>{{ result.ok ? '✓ 预约成功！已通知老师' : '✗ ' + result.msg }}</b>
      <div class="mt row">
        <button class="btn ghost small" @click="router.push('/history')">查看记录</button>
        <button class="btn small" @click="router.push('/')">返回首页</button>
      </div>
    </div>
  </div>
</template>
