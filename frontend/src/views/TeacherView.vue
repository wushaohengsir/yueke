<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { api } from '../api'
import type { Booking } from '../types'

const router = useRouter()
const auth = useAuthStore()
const tab = ref<'schedule' | 'leave' | 'template'>('schedule')
const bookings = ref<any[]>([])
const leaves = ref<any[]>([])
const templates = ref<any[]>([])

const wd = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const p2 = (n: number) => String(n).padStart(2, '0')
const stText = ['待确认', '已确认', '已完成', '已取消', '已请假']
const stClass = ['s1', 's1', 's2', 's3', 's4']
function range(s: string, e: string) {
  const a = new Date(s), b = new Date(e)
  return `${a.getMonth() + 1}/${a.getDate()} ${p2(a.getHours())}:${p2(a.getMinutes())}-${p2(b.getHours())}:${p2(b.getMinutes())}`
}
function trange(t: any) {
  return `${t.startTime}-${t.endTime}`
}

async function load() {
  bookings.value = await api.getTeacherBookings()
  leaves.value = await api.getTeacherLeaves()
  templates.value = await api.getTemplates()
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

// 添加模板表单（无默认值，由老师填写）
const f = ref({ weekday: 1, start: '', end: '' })
async function addTpl() {
  await api.addTemplate({ weekday: f.value.weekday, start: f.value.start, end: f.value.end })
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

    <!-- 周课表 -->
    <div v-if="tab==='schedule'">
      <div class="card" v-for="b in bookings" :key="b.id">
        <div class="row">
          <b>{{ range(b.startAt, b.endAt) }}</b>
          <span class="st" :class="stClass[b.status]">{{ stText[b.status] }}</span>
        </div>
        <p class="muted" style="margin:6px 0">{{ b.studentName }} · {{ b.subjectName }}</p>
        <button class="btn small" v-if="b.status===1" @click="complete(b.id)">登记完成</button>
      </div>
      <p class="muted" v-if="!bookings.length">暂无课表</p>
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

    <!-- 时段模板 -->
    <div v-if="tab==='template'">
      <div class="card" v-for="t in templates" :key="t.id">
        <div class="row">
          <b>{{ wd[t.weekday-1] }} {{ trange(t) }}</b>
          <button class="btn ghost small" @click="toggle(t.id)">{{ t.enabled===1?'停用':'启用' }}</button>
        </div>
      </div>
      <div class="card mt">
        <b>添加时段</b>
        <div class="row mt">
          <select class="input" style="margin:0" v-model.number="f.weekday">
            <option v-for="(w,i) in wd" :key="i" :value="i+1">{{ w }}</option>
          </select>
        </div>
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
