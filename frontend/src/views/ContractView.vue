<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '../api'
import type { Teacher } from '../types'
import Tabbar from '../components/Tabbar.vue'

const contracts = ref<any[]>([])
const teachers = ref<Teacher[]>([])
const buyTeacher = ref<number | null>(null)
const buyCredits = ref(10)
const msg = ref('')

const stText = ['待签署', '已生效', '已结束']
const stClass = ['s4', 's2', 's3']

async function load() {
  contracts.value = await api.getContracts()
  teachers.value = await api.listTeachers()
}
onMounted(load)

async function purchase() {
  if (!buyTeacher.value) { msg.value = '请选择老师'; return }
  const r = await api.purchase(buyTeacher.value, buyCredits.value)
  msg.value = r.ok ? '✓ 已生成合同，请签署生效' : '✗ ' + (r.msg || '购买失败')
  if (r.ok) await load()
}
async function sign(id: number) {
  const r = await api.signContract(id)
  if (r.ok) await load()
}
</script>

<template>
  <div class="page">
    <h2 class="page-title">合同 · 课时包</h2>

    <div class="card">
      <b>购买课时包</b>
      <p class="muted" style="margin:6px 0 10px">选择老师购买对应课程课时包，生成合同；签署后课时自动入账该课程（V1.0 线下结算）。</p>
      <select class="input" v-model.number="buyTeacher">
        <option :value="null" disabled>选择老师</option>
        <option v-for="t in teachers" :key="t.id" :value="t.id">{{ t.name }}（{{ t.subjects[0]?.name }}）</option>
      </select>
      <div class="row">
        <span class="muted">课时数</span>
        <input class="input" style="margin:0;width:120px" type="number" min="1" max="200" v-model.number="buyCredits" />
      </div>
      <p class="muted" v-if="msg" style="margin:10px 0 0">{{ msg }}</p>
      <button class="btn mt" @click="purchase">生成合同</button>
    </div>

    <div class="card" v-for="c in contracts" :key="c.id">
      <div class="row">
        <b>{{ c.teacherName }} · {{ c.subjectName }}</b>
        <span class="st" :class="stClass[c.status]">{{ stText[c.status] }}</span>
      </div>
      <div class="row mt" style="font-size:14px">
        <span class="muted">课时包 {{ c.totalCredits }} 节</span>
        <button class="btn small" v-if="c.status===0" @click="sign(c.id)">签署生效</button>
      </div>
    </div>
    <p class="muted" v-if="!contracts.length">暂无合同</p>
    <Tabbar />
  </div>
</template>
