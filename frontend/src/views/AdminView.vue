<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { api } from '../api'

const router = useRouter()
const auth = useAuthStore()
const tab = ref<'dashboard' | 'audit' | 'users' | 'subjects' | 'booking'>('dashboard')
const dash = ref<any>(null)
const teachers = ref<any[]>([])
const auditFilter = ref<number | null>(null)
const users = ref<any[]>([])
const userRoleFilter = ref<number | null>(null)
const subjects = ref<any[]>([])
const subjForm = ref({ name: '', category: '' })
const subjMsg = ref('')

const stText = ['待审核', '已通过', '已驳回']
const stClass = ['s4', 's2', 's3']
const roleText = ['', '学员', '老师', '管理员']

async function load() {
  dash.value = await api.getDashboard()
  teachers.value = await api.getAdminTeachers(auditFilter.value ?? undefined)
}
async function loadUsers() {
  users.value = await api.getAdminUsers(userRoleFilter.value ?? undefined)
}
async function loadSubjects() {
  subjects.value = await api.getAdminSubjects()
}
onMounted(() => { load(); loadUsers(); loadSubjects(); loadPlanBase() })

async function audit(userId: number, approve: boolean) {
  await api.auditTeacher(userId, approve)
  await load()
}
function setFilter(s: number | null) {
  auditFilter.value = s
  load()
}
async function toggleUser(id: number, enable: boolean) {
  await api.toggleUser(id, enable)
  await loadUsers()
}
function setUserFilter(r: number | null) {
  userRoleFilter.value = r
  loadUsers()
}
async function addSubject() {
  if (!subjForm.value.name) { subjMsg.value = '请输入科目名'; return }
  const r = await api.addSubject(subjForm.value.name, subjForm.value.category)
  subjMsg.value = r.ok ? '✓ 已添加' : '✗ ' + (r.msg || '添加失败')
  if (r.ok) { subjForm.value = { name: '', category: '' }; await loadSubjects() }
}
// ---- 排课：管理员代学生预约未来课程 ----
const students = ref<any[]>([])
const planTeachers = ref<any[]>([])
const planForm = ref({ studentId: '', teacherId: '', date: '' })
const planSlots = ref<any[]>([])
const planLoaded = ref(false)
const selSlot = ref<any>(null)
const planMsg = ref('')
const p2 = (n: number) => String(n).padStart(2, '0')
function fmtDate(d: Date) {
  return `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())}`
}
const todayMin = fmtDate(new Date())
function defaultDate() {
  const d = new Date(); d.setDate(d.getDate() + 1); return fmtDate(d)
}
async function loadPlanBase() {
  const all = await api.getAdminUsers(1)
  students.value = (all || []).filter((s: any) => s.status === 1)
  planTeachers.value = await api.listTeachers()
  if (!planForm.value.studentId) planForm.value.date = defaultDate()
}
function clearPlanSlots() { planSlots.value = []; planLoaded.value = false; selSlot.value = null; planMsg.value = '' }
function fmtSlot(s: any) {
  const st = new Date(s.startAt), et = new Date(s.endAt)
  return `${p2(st.getHours())}:${p2(st.getMinutes())}-${p2(et.getHours())}:${p2(et.getMinutes())}`
}
function dayCN(dateStr: string) {
  const d = new Date(dateStr + 'T00:00:00')
  return `${d.getMonth() + 1}月${d.getDate()}日 周${'日一二三四五六'[d.getDay()]}`
}
function studentName() {
  return (students.value.find((s: any) => String(s.id) === String(planForm.value.studentId)) || {}).name || '该学员'
}
async function loadPlanSlots() {
  if (!planForm.value.studentId || !planForm.value.teacherId || !planForm.value.date) { alert('请先选择学生、老师与日期'); return }
  planMsg.value = ''; selSlot.value = null
  planSlots.value = await api.getAdminPlanSlots(Number(planForm.value.teacherId), planForm.value.date)
  planLoaded.value = true
}
function pick(s: any) {
  if (s.status !== 'available') return
  selSlot.value = selSlot.value && selSlot.value.id === s.id ? null : s
}
async function bookPlan() {
  if (!selSlot.value) { alert('请先选择要排课的时段'); return }
  const r = await api.adminBookPlan({
    studentId: Number(planForm.value.studentId),
    teacherId: Number(planForm.value.teacherId),
    startAt: selSlot.value.startAt, endAt: selSlot.value.endAt,
  })
  planMsg.value = r.ok ? `✓ 已为「${studentName()}」安排 ${fmtSlot(selSlot.value)} 的课` : '✗ ' + (r.msg || '排课失败')
  if (r.ok) await loadPlanSlots()
}
function logout() { auth.logout(); router.replace('/login') }
</script>

<template>
  <div class="page">
    <div class="banner">
      <h1>管理后台</h1>
      <p>{{ auth.user?.name }} · 老师审核 / 数据看板</p>
    </div>

    <div style="display:flex;gap:8px;margin-bottom:14px;flex-wrap:wrap">
      <button class="tag" :style="tab==='dashboard'?'background:var(--sun)':''" @click="tab='dashboard'">数据看板</button>
      <button class="tag" :style="tab==='audit'?'background:var(--sun)':''" @click="tab='audit'">老师审核</button>
      <button class="tag" :style="tab==='users'?'background:var(--sun)':''" @click="tab='users'">用户管理</button>
      <button class="tag" :style="tab==='subjects'?'background:var(--sun)':''" @click="tab='subjects'">科目管理</button>
      <button class="tag" :style="tab==='booking'?'background:var(--sun)':''" @click="tab='booking'">排课</button>
    </div>

    <!-- 数据看板 -->
    <div v-if="tab==='dashboard' && dash">
      <div class="grid" style="grid-template-columns:repeat(3,1fr);gap:10px">
        <div class="card" style="margin:0;padding:14px"><p class="muted" style="margin:0">学员</p><b style="font-size:22px">{{ dash.students }}</b></div>
        <div class="card" style="margin:0;padding:14px"><p class="muted" style="margin:0">老师</p><b style="font-size:22px">{{ dash.teachers }}</b></div>
        <div class="card" style="margin:0;padding:14px"><p class="muted" style="margin:0">待审核</p><b style="font-size:22px;color:var(--sun-deep)">{{ dash.pendingAudit }}</b></div>
        <div class="card" style="margin:0;padding:14px"><p class="muted" style="margin:0">进行中预约</p><b style="font-size:22px">{{ dash.activeBookings }}</b></div>
        <div class="card" style="margin:0;padding:14px"><p class="muted" style="margin:0">已完成课时</p><b style="font-size:22px;color:var(--green)">{{ dash.completed }}</b></div>
        <div class="card" style="margin:0;padding:14px"><p class="muted" style="margin:0">已请假</p><b style="font-size:22px;color:var(--sun-deep)">{{ dash.leaves }}</b></div>
      </div>
      <div class="card mt" v-if="dash.bookingsBySubject">
        <b>预约按科目分布</b>
        <div v-for="(v, k) in dash.bookingsBySubject" :key="k" class="row mt" style="padding:2px 0">
          <span class="muted">{{ k }}</span><b>{{ v }} 单</b>
        </div>
        <p class="muted" v-if="!Object.keys(dash.bookingsBySubject).length">暂无数据</p>
      </div>
    </div>

    <!-- 老师审核 -->
    <div v-if="tab==='audit'">
      <div style="display:flex;gap:8px;margin-bottom:10px">
        <button class="tag" :style="auditFilter===null?'background:var(--sun)':''" @click="setFilter(null)">全部</button>
        <button class="tag" :style="auditFilter===0?'background:var(--sun)':''" @click="setFilter(0)">待审核</button>
        <button class="tag" :style="auditFilter===1?'background:var(--sun)':''" @click="setFilter(1)">已通过</button>
        <button class="tag" :style="auditFilter===2?'background:var(--sun)':''" @click="setFilter(2)">已驳回</button>
      </div>
      <div class="card" v-for="t in teachers" :key="t.userId">
        <div class="row">
          <b>{{ t.name }}</b>
          <span class="st" :class="stClass[t.auditStatus]">{{ stText[t.auditStatus] }}</span>
        </div>
        <p class="muted" style="margin:6px 0">{{ t.phone }} · {{ t.title }} · {{ t.subjects }}</p>
        <div class="row" v-if="t.auditStatus===0">
          <button class="btn small" @click="audit(t.userId, true)">通过</button>
          <button class="btn ghost small" @click="audit(t.userId, false)">驳回</button>
        </div>
      </div>
      <p class="muted" v-if="!teachers.length">暂无老师</p>
    </div>

    <!-- 用户管理 -->
    <div v-if="tab==='users'">
      <div style="display:flex;gap:8px;margin-bottom:10px">
        <button class="tag" :style="userRoleFilter===null?'background:var(--sun)':''" @click="setUserFilter(null)">全部</button>
        <button class="tag" :style="userRoleFilter===1?'background:var(--sun)':''" @click="setUserFilter(1)">学员</button>
        <button class="tag" :style="userRoleFilter===2?'background:var(--sun)':''" @click="setUserFilter(2)">老师</button>
      </div>
      <div class="card" v-for="u in users" :key="u.id">
        <div class="row">
          <b>{{ u.name }}</b>
          <span class="st" :class="u.status===1?'s2':'s3'">{{ u.status===1?'正常':'已禁用' }}</span>
        </div>
        <p class="muted" style="margin:6px 0">{{ u.phone }} · {{ roleText[u.role] }}</p>
        <button class="btn ghost small" v-if="u.role!==3" @click="toggleUser(u.id, u.status!==1)">
          {{ u.status===1?'禁用':'启用' }}
        </button>
      </div>
      <p class="muted" v-if="!users.length">暂无用户</p>
    </div>

    <!-- 科目管理 -->
    <div v-if="tab==='subjects'">
      <div class="card" v-for="s in subjects" :key="s.id">
        <div class="row">
          <b>{{ s.name }}</b>
          <span class="tag">{{ s.category || '未分类' }}</span>
        </div>
      </div>
      <div class="card mt">
        <b>新增科目</b>
        <input class="input mt" v-model="subjForm.name" placeholder="科目名（如 吉他）" />
        <input class="input" v-model="subjForm.category" placeholder="分类（如 音乐）" />
        <p class="muted" v-if="subjMsg" style="margin:0 0 8px">{{ subjMsg }}</p>
        <button class="btn" @click="addSubject">添加</button>
      </div>
    </div>

    <!-- 排课：管理员代学生预约未来课程（学生端只能约今明两天） -->
    <div v-if="tab==='booking'">
      <div class="card">
        <b>选择学生 / 老师 / 目标日期</b>
        <select class="input mt" v-model="planForm.studentId" @change="clearPlanSlots">
          <option value="">学员</option>
          <option v-for="s in students" :key="s.id" :value="s.id">{{ s.name }}（{{ s.phone }}）</option>
        </select>
        <select class="input" v-model="planForm.teacherId" @change="clearPlanSlots">
          <option value="">老师</option>
          <option v-for="t in planTeachers" :key="t.id" :value="t.id">{{ t.name }}（{{ (t.subjects && t.subjects[0]) || '未设科目' }}）</option>
        </select>
        <input class="input" type="date" :min="todayMin" v-model="planForm.date" @change="clearPlanSlots" />
        <button class="btn mt" @click="loadPlanSlots">查看该日可约时段</button>
      </div>

      <div class="card mt" v-if="planLoaded">
        <b>{{ dayCN(planForm.date) }} · 老师开放时段</b>
        <p class="muted" style="margin:6px 0 10px">仅可安排老师该日开放时段；已约时段不可重复排。</p>
        <div style="display:flex;flex-wrap:wrap;gap:8px">
          <button v-for="s in planSlots" :key="s.id" class="tag"
            :disabled="s.status!=='available'"
            :style="s.status==='available'
              ? (selSlot && selSlot.id===s.id ? 'background:var(--sun);font-weight:600' : '')
              : 'opacity:.45'"
            @click="pick(s)">{{ fmtSlot(s) }}{{ s.status!=='available' ? ' · 已约' : '' }}</button>
        </div>
        <p class="muted mt" style="margin-bottom:0" v-if="!planSlots.length">该老师当日没有可安排的时段（请确认老师已启用该日的时段模板）</p>
      </div>

      <div class="card mt" v-if="selSlot">
        <p class="muted" style="margin:0 0 8px">将为「{{ studentName() }}」预约 {{ fmtSlot(selSlot) }} 的课程</p>
        <button class="btn" @click="bookPlan">为该学员排课</button>
      </div>
      <p class="muted mt" v-if="planMsg">{{ planMsg }}</p>
    </div>

    <button class="btn ghost mt" @click="logout">退出登录</button>
  </div>
</template>
