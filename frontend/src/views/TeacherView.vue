<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { api } from '../api'
import type { Booking } from '../types'

const router = useRouter()
const auth = useAuthStore()
const tab = ref<'schedule' | 'leave' | 'template'>('schedule')
const leaves = ref<any[]>([])
const templates = ref<any[]>([])

const wd = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const p2 = (n: number) => String(n).padStart(2, '0')
function range(s: string, e: string) {
  const a = new Date(s), b = new Date(e)
  return `${a.getMonth() + 1}/${a.getDate()} ${p2(a.getHours())}:${p2(a.getMinutes())}-${p2(b.getHours())}:${p2(b.getMinutes())}`
}
function trange(t: any) {
  return `${t.startTime}-${t.endTime}`
}

// 时段模板两级：第一层选星期（1-7），第二层该天的时段列表 + 添加
const selWeekday = ref(1)
const dayTemplates = computed(() => templates.value.filter((t: any) => t.weekday === selWeekday.value))

// 周课表两级：第一层选星期，第二层该天的时段 + 预约状态
const selScheduleWeekday = ref(1)
const weekSchedule = ref<any[]>([])
const scheduleStatus = { free: '未预约', booked: '已预约', completed: '已完成' }
const scheduleClass = { free: 's3', booked: 's1', completed: 's2' }

async function load() {
  leaves.value = await api.getTeacherLeaves()
  templates.value = await api.getTemplates()
  await loadWeekSchedule()
}
async function loadWeekSchedule() {
  weekSchedule.value = await api.getWeekSchedule(selScheduleWeekday.value)
}
function pickScheduleWeekday(w: number) {
  selScheduleWeekday.value = w
  loadWeekSchedule()
}
onMounted(load)

async function handle(id: number, approve: boolean) {
  await api.handleLeave(id, approve)
  await load()
}
async function toggle(id: number) {
  await api.toggleTemplate(id)
  await load()
}
async function complete(id: number) {
  await api.completeBooking(id)
  await load()
}

// 添加时段（weekday 由当前选中的星期决定，无默认时间值）
const f = ref({ start: '', end: '' })
async function addTpl() {
  await api.addTemplate({ weekday: selWeekday.value, start: f.value.start, end: f.value.end })
  await load()
}
function logout() { auth.logout(); router.replace('/login') }
</script>

<template>
  <div class="page">
    <div class="banner">
      <h1>老师工作台</h1>
      <p>{{ auth.user?.name }} · 管理课表 / 审批请假 / 时段模板</p>
    </div>

    <div style="display:flex;gap:8px;margin-bottom:14px">
      <button class="tag" :style="tab==='schedule'?'background:var(--sun)':''" @click="tab='schedule'">周课表</button>
      <button class="tag" :style="tab==='leave'?'background:var(--sun)':''" @click="tab='leave'">请假审批</button>
      <button class="tag" :style="tab==='template'?'background:var(--sun)':''" @click="tab='template'">时段模板</button>
    </div>

    <!-- 周课表：第一层选星期 -->
    <div v-if="tab==='schedule'">
      <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:14px">
        <button v-for="(w, i) in wd" :key="i" class="tag"
          :style="selScheduleWeekday===i+1 ? 'background:var(--sun)' : ''"
          @click="pickScheduleWeekday(i+1)">{{ w }}</button>
      </div>

      <!-- 第二层：该天的时段 + 预约状态 -->
      <div class="card" v-for="s in weekSchedule" :key="s.id">
        <div class="row">
          <b>{{ trange(s) }}</b>
          <span class="st" :class="scheduleClass[s.status]">{{ scheduleStatus[s.status] }}</span>
        </div>
        <p class="muted" style="margin:6px 0" v-if="s.status!=='free'">{{ s.studentName }} · {{ s.subjectName }}</p>
        <button class="btn small" v-if="s.status==='booked'" @click="complete(s.bookingId)">登记完成</button>
      </div>
      <p class="muted" v-if="!weekSchedule.length">暂无 {{ wd[selScheduleWeekday-1] }} 的时段</p>
    </div>

    <!-- 请假审批 -->
    <div v-if="tab==='leave'">
      <div class="card" v-for="l in leaves" :key="l.id">
        <div class="row">
          <b>{{ l.studentName }} · {{ l.subjectName }}</b>
          <span class="st" :class="l.status===0?'s1':(l.status===1?'s4':'s3')">
            {{ l.status===0?'待审批':(l.status===1?'已批准':'已驳回') }}</span>
        </div>
        <p class="muted" style="margin:6px 0">{{ range(l.startAt, l.endAt) }} · 事由：{{ l.reason }}</p>
        <div class="row" v-if="l.status===0">
          <button class="btn small" @click="handle(l.id, true)">批准</button>
          <button class="btn ghost small" @click="handle(l.id, false)">驳回</button>
        </div>
      </div>
      <p class="muted" v-if="!leaves.length">暂无请假申请</p>
    </div>

    <!-- 时段模板：第一层选星期 -->
    <div v-if="tab==='template'">
      <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:14px">
        <button v-for="(w, i) in wd" :key="i" class="tag"
          :style="selWeekday===i+1 ? 'background:var(--sun)' : ''"
          @click="selWeekday = i+1">{{ w }}</button>
      </div>

      <!-- 第二层：该天的时段列表 + 添加 -->
      <div class="card" v-for="t in dayTemplates" :key="t.id">
        <div class="row">
          <b>{{ trange(t) }}</b>
          <button class="btn ghost small" @click="toggle(t.id)">{{ t.enabled===1?'停用':'启用' }}</button>
        </div>
      </div>
      <p class="muted" v-if="!dayTemplates.length">暂无 {{ wd[selWeekday-1] }} 的时段</p>

      <div class="card mt">
        <b>添加 {{ wd[selWeekday-1] }} 时段</b>
        <div class="row mt">
          <input class="input" type="time" style="margin:0" v-model="f.start" />
          <input class="input" type="time" style="margin:0" v-model="f.end" />
        </div>
        <button class="btn mt" @click="addTpl">添加</button>
      </div>
    </div>

    <button class="btn ghost mt" @click="logout">退出登录</button>
  </div>
</template>
