<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const phone = ref('')
const password = ref('')
const role = ref<'student' | 'teacher' | 'admin'>('student')
const err = ref('')

async function submit() {
  if (!phone.value) { err.value = '请输入手机号'; return }
  await auth.login(phone.value, password.value, role.value)
  const r = auth.user?.role
  router.replace(r === 'teacher' ? '/teacher' : r === 'admin' ? '/admin' : '/')
}
</script>

<template>
  <div class="page" style="padding-top:60px">
    <div class="banner">
      <h1>通用师生约课平台</h1>
      <p>找好老师，约好每一课</p>
    </div>
    <div class="card">
      <input class="input" v-model="phone" placeholder="手机号" />
      <input class="input" v-model="password" type="password" placeholder="密码（演示任意）" />
      <div class="row" style="margin-bottom:14px">
        <label><input type="radio" value="student" v-model="role" /> 学员</label>
        <label><input type="radio" value="teacher" v-model="role" /> 老师</label>
        <label><input type="radio" value="admin" v-model="role" /> 管理员</label>
      </div>
      <p v-if="err" class="muted" style="color:var(--coral)">{{ err }}</p>
      <button class="btn" @click="submit">登录 / 注册</button>
      <p class="muted mt">游客可先浏览，约课需登录</p>
    </div>
  </div>
</template>
