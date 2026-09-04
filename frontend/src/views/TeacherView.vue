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

// 周课表：按周翻页，显示具体日期 + 每天时段
const weekOffset = ref(0)
const weekSchedule = ref<any>({ weekStart: '', days: [] })
const scheduleStatus = { free: '未预约', booked: '已预约', completed: '已完成' }
const scheduleClass = { free: 's3', booked: 's1', completed: 's2' }
const wdLabel = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

async function load() {
  leaves.value = await api.getTeacherLeaves()
  templates.value = await api.getTemplates()
  await loadWeekSchedule()
}
async function loadWeekSchedule() {
  weekSchedule.value = await api.getWeekSchedule(weekOffset.value)
}
function shiftWeek(delta: number) {
  weekOffset.value += delta
  loadWeekSchedule()
}
// 把 date 字符串（yyyy-MM-dd）格式化为「M月d号」
function dayLabel(date: string) {
  const d = new Date(date + 'T00:00:00')
  return `${d.getMonth() + 1}月${d.getDate()}号`
}
// 某天的概览摘要：时段数 + 预约数
function daySummary(day: any) {
  const slots = day.slots || []
  const booked = slots.filter((s: any) => s.status !== 'free').length
  if (!slots.length) return '无安排'
  return `${slots.length} 个时段 · ${booked} 个已约`
}
onMounted(load)

async function handle(id: number, approve: boolean) {
  await api.handleLeave(id, approve)
  await load()
}
async function toggle(id: number) {
  const r = await api.toggleTemplate(id)
  if (!r.ok) { alert(r.msg || '操作失败'); return }
  await load()
}
async function removeTemplate(id: number) {
  if (!confirm('确定删除该时段吗？')) return
  await api.deleteTemplate(id)
  await load()
}
// 编辑停用模板的时间：改错直接改，无需删了重建
const editing = ref<number | null>(null)
const ef = ref({ start: '', end: '' })
function startEdit(t: any) {
  editing.value = t.id
  ef.value.start = String(t.startTime || '').slice(0, 5) // 后端时间可能带秒，裁剪到 HH:mm
  ef.value.end = String(t.endTime || '').slice(0, 5)
}
async function saveEdit() {
  if (!editing.value) return
  if (!ef.value.start || !ef.value.end) { alert('请选择开始与结束时间'); return }
  if (ef.value.start >= ef.value.end) { alert('结束时间必须晚于开始时间'); return }
  const r = await api.updateTemplate(editing.value, ef.value.start, ef.value.end)
  if (!r.ok) { alert(r.msg || '修改失败'); return }
  editing.value = null
  await load()
}
async function complete(id: number) {
  const r = await api.completeBooking(id)
  if (!r.ok) { alert(r.msg || '登记失败'); return }
  await load()
}

// 添加时段（weekday 由当前选中的星期决定；结束时间必须晚于开始时间）
const f = ref({ start: '', end: '' })
async function addTpl() {
  if (!f.value.start || !f.value.end) { alert('请先选择开始与结束时间'); return }
  if (f.value.start >= f.value.end) { alert('结束时间必须晚于开始时间'); return }
  const r = await api.addTemplate({ weekday: selWeekday.value, start: f.value.start, end: f.value.end })
  if (!r.ok) { alert(r.msg || '添加失败'); return }
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

    <!-- 周课表：周翻页 + 7 天概览 -->
    <div v-if="tab==='schedule'">
      <!-- 周翻页 -->
      <div class="row" style="margin-bottom:14px">
        <button class="btn ghost small" @click="shiftWeek(-1)">上一周</button>
        <b style="margin:0 12px">{{ weekOffset===0 ? '本周' : (weekOffset<0 ? '过去 ' + (-weekOffset) + ' 周' : '未来 ' + weekOffset + ' 周') }}</b>
        <button class="btn ghost small" @click="shiftWeek(1)">下一周</button>
      </div>

      <!-- 7 天概览（点击进该日详情） -->
      <div v-for="day in weekSchedule.days" :key="day.date" class="card" style="padding:12px 14px;cursor:pointer"
        @click="router.push('/teacher/schedule/' + day.date)">
        <div class="row">
          <b>{{ dayLabel(day.date) }} · {{ wdLabel[day.weekday-1] }}</b>
          <span class="muted">{{ daySummary(day) }}</span>
        </div>
      </div>
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

      <!-- 第二层：该天的时段列表 + 添加；停用中的模板可直接编辑时间 -->
      <div class="card" v-for="t in dayTemplates" :key="t.id"
        :style="t.enabled===1 ? 'background:#e6f7f4;border-color:var(--green)' : ''">
        <div v-if="editing===t.id" class="col">
          <div class="row">
            <input class="input" type="time" style="margin:0" v-model="ef.start" />
            <span class="muted">至</span>
            <input class="input" type="time" style="margin:0" v-model="ef.end" />
          </div>
          <div class="row mt">
            <button class="btn small" @click="saveEdit">保存</button>
            <button class="btn ghost small" @click="editing=null">取消</button>
          </div>
        </div>
        <div v-else class="row">
          <b>{{ trange(t) }}</b>
          <span style="display:flex;gap:8px">
            <button v-if="t.enabled!==1" class="btn ghost small" @click="startEdit(t)">编辑</button>
            <button class="btn ghost small" @click="toggle(t.id)">{{ t.enabled===1?'停用':'启用' }}</button>
            <button class="btn ghost small" style="color:var(--coral)" @click="removeTemplate(t.id)">删除</button>
          </span>
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
