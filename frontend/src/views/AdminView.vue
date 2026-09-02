<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { api } from '../api'

const router = useRouter()
const auth = useAuthStore()
const tab = ref<'dashboard' | 'audit'>('dashboard')
const dash = ref<any>(null)
const teachers = ref<any[]>([])
const auditFilter = ref<number | null>(null)

const stText = ['待审核', '已通过', '已驳回']
const stClass = ['s4', 's2', 's3']

async function load() {
  dash.value = await api.getDashboard()
  teachers.value = await api.getAdminTeachers(auditFilter.value ?? undefined)
}
onMounted(load)

async function audit(userId: number, approve: boolean) {
  await api.auditTeacher(userId, approve)
  await load()
}
function setFilter(s: number | null) {
  auditFilter.value = s
  load()
}
function logout() { auth.logout(); router.replace('/login') }
</script>

<template>
  <div class="page">
    <div class="banner">
      <h1>管理后台</h1>
      <p>{{ auth.user?.name }} · 老师审核 / 数据看板</p>
    </div>

    <div style="display:flex;gap:8px;margin-bottom:14px">
      <button class="tag" :style="tab==='dashboard'?'background:var(--sun)':''" @click="tab='dashboard'">数据看板</button>
      <button class="tag" :style="tab==='audit'?'background:var(--sun)':''" @click="tab='audit'">老师审核</button>
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

    <button class="btn ghost mt" @click="logout">退出登录</button>
  </div>
</template>
