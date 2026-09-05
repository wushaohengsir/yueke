<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import heroImg from '../assets/kazuha-hero.jpg'

const router = useRouter()
const auth = useAuthStore()

// 第一步：选身份；第二步：登录表单（游客跳过）
const step = ref<'role' | 'login'>('role')
const role = ref<'student' | 'teacher' | 'admin'>('student')
const phone = ref('')
const password = ref('')
const err = ref('')

function choose(r: 'student' | 'teacher' | 'admin') {
  role.value = r
  step.value = 'login'
}

function enterAsGuest() {
  auth.enterAsGuest()
  router.replace('/')
}

async function submit() {
  if (!phone.value) { err.value = '请输入手机号'; return }
  if (!password.value) { err.value = '请输入密码'; return }
  try {
    const u = await auth.login(phone.value, password.value, role.value)
    router.replace(u.role === 'teacher' ? '/teacher' : u.role === 'admin' ? '/admin' : '/')
  } catch (e: any) {
    err.value = e.message || '登录失败'
  }
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

    <!-- 第一步：选择身份 -->
    <div v-if="step === 'role'" class="card">
      <p class="muted" style="margin:0 0 14px">请选择你的身份</p>
      <button class="btn" @click="enterAsGuest">游客</button>
      <button class="btn mt" @click="choose('student')">学员</button>
      <button class="btn mt" @click="choose('teacher')">老师</button>
      <button class="btn mt" @click="choose('admin')">管理员</button>
      <p class="muted mt" style="margin:0">游客可浏览师资介绍，无需登录</p>
      <p class="mt" style="margin:0;text-align:center">
        新学员 / 新老师？<a href="#" @click.prevent="router.push('/register')">点此注册</a>
      </p>
    </div>

    <!-- 第二步：登录表单 -->
    <div v-else class="card">
      <p class="muted" style="margin:0 0 14px">
        以「{{ role === 'student' ? '学员' : role === 'teacher' ? '老师' : '管理员' }}」身份登录
        <a href="#" style="float:right" @click.prevent="step = 'role'">返回</a>
      </p>
      <input class="input" v-model="phone" placeholder="手机号" />
      <input class="input" v-model="password" type="password" placeholder="密码" />
      <p class="muted" v-if="role === 'teacher'" style="margin:0 0 10px">老师账号须管理员审核通过后方可登录</p>
      <p class="muted" v-else-if="role === 'admin'" style="margin:0 0 10px">管理员账号由平台开通，不开放自助注册</p>
      <p v-if="err" class="muted" style="color:var(--coral)">{{ err }}</p>
      <button class="btn" @click="submit">登录</button>
      <template v-if="role !== 'admin'">
        <p class="muted" style="margin:12px 0 0;text-align:center">还没有账号？<a href="#" @click.prevent="router.push('/register')">注册{{ role === 'student' ? '学员' : '老师' }}账号</a></p>
      </template>
    </div>
  </div>
</template>
