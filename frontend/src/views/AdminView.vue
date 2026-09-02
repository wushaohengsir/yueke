<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { api } from '../api'

const router = useRouter()
const auth = useAuthStore()
const tab = ref<'dashboard' | 'audit' | 'users' | 'subjects'>('dashboard')
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
onMounted(() => { load(); loadUsers(); loadSubjects() })

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

    <button class="btn ghost mt" @click="logout">退出登录</button>
  </div>
</template>
