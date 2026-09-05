<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { api } from '../api'
import heroImg from '../assets/kazuha-hero.jpg'

const router = useRouter()
const auth = useAuthStore()

const mode = ref<'student' | 'teacher'>('student')
const name = ref('')
const phone = ref('')
const password = ref('')
const confirmPwd = ref('')
const subjects = ref<{ id: number; name: string }[]>([])
const subjectId = ref<number | null>(null)
const err = ref('')
const busy = ref(false)
const submitted = ref(false) // 老师：已提交待审

onMounted(async () => {
  if (auth.isLoggedIn) { router.replace('/'); return }
  try { subjects.value = await api.listSubjects() } catch { /* 稍后可重试 */ }
})

function pickMode(m: 'student' | 'teacher') {
  mode.value = m
  err.value = ''
}

function toLogin() {
  router.replace('/login')
}

async function submit() {
  if (busy.value) return
  err.value = ''
  if (!name.value.trim()) { err.value = '请填写姓名'; return }
  if (!/^\d{6,20}$/.test(phone.value)) { err.value = '请填写正确的手机号'; return }
  if (password.value.length < 6) { err.value = '密码至少 6 位'; return }
  if (password.value !== confirmPwd.value) { err.value = '两次输入的密码不一致'; return }
  if (mode.value === 'teacher' && !subjectId.value) { err.value = '老师注册请选择授课科目'; return }

  busy.value = true
  const r = await api.register({
    role: mode.value,
    name: name.value.trim(),
    phone: phone.value,
    password: password.value,
    subjectId: mode.value === 'teacher' ? subjectId.value : null,
  })
  busy.value = false

  if (!r.ok) { err.value = r.msg || '注册失败'; return }
  if (r.pending) { submitted.value = true; return } // 老师待审
  if (r.user) { auth.refreshUser(r.user); router.replace('/') } // 学员自动登录
}
</script>

<template>
  <div class="page vcenter">
    <div class="hero-art">
      <img :src="heroImg" alt="枫叶剪影" />
      <div class="hero-title">
        <h1>通用师生约课平台</h1>
        <p>深山踏红叶 · 好课待人归</p>
      </div>
    </div>

    <!-- 老师：注册成功 → 待审核 -->
    <div v-if="submitted" class="card">
      <p style="margin:0 0 8px;font-size:18px">🎉 入驻申请已提交</p>
      <p class="muted" style="margin:0 0 14px">我们已收到您的入驻申请，管理员审核通过后，您即可用该手机号登录老师端。</p>
      <button class="btn" @click="toLogin">返回登录</button>
    </div>

    <!-- 注册表单 -->
    <div v-else class="card">
      <p class="muted" style="margin:0 0 14px">
        注册账号
        <a href="#" style="float:right" @click.prevent="toLogin">已有账号？去登录</a>
      </p>

      <!-- 选择身份 -->
      <div class="row" style="margin-bottom:12px">
        <button class="tag" style="flex:1"
          :style="mode==='student'?'background:var(--sun);font-weight:600':''"
          @click="pickMode('student')">我是学员</button>
        <button class="tag" style="flex:1"
          :style="mode==='teacher'?'background:var(--sun);font-weight:600':''"
          @click="pickMode('teacher')">我是老师</button>
      </div>

      <input class="input" v-model="name" placeholder="真实姓名（审核/约课会用到）" />
      <input class="input" v-model="phone" placeholder="手机号（登录账号）" />
      <input class="input" v-model="password" type="password" placeholder="密码（至少 6 位）" />
      <input class="input" v-model="confirmPwd" type="password" placeholder="确认密码" />
      <template v-if="mode === 'teacher'">
        <select class="input" v-model.number="subjectId">
          <option :value="null" disabled>选择授课科目</option>
          <option v-for="s in subjects" :key="s.id" :value="s.id">{{ s.name }}</option>
        </select>
        <p class="muted" style="margin:0 0 10px">注册后将提交管理员审核，通过后方可开放约课与登录老师端</p>
      </template>
      <p class="muted" v-else style="margin:0 0 10px">注册成功即可浏览并预约心仪老师的课程</p>

      <p v-if="err" class="muted" style="color:var(--coral)">{{ err }}</p>
      <button class="btn" :disabled="busy" @click="submit">{{ busy ? '提交中…' : '注册' }}</button>
    </div>
  </div>
</template>
