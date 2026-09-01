<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '../api/mock'
import type { Teacher } from '../types'
import Tabbar from '../components/Tabbar.vue'

const teachers = ref<Teacher[]>([])
const kw = ref('')
onMounted(async () => { teachers.value = await api.listTeachers() })

function filtered() {
  if (!kw.value) return teachers.value
  return teachers.value.filter((t) => t.name.includes(kw.value) || t.subjects.some((s) => s.name.includes(kw.value)))
}
</script>

<template>
  <div class="page">
    <h2 class="page-title">选老师</h2>
    <input class="input" v-model="kw" placeholder="搜索老师 / 科目" />
    <router-link v-for="t in filtered()" :key="t.id" :to="`/book/${t.id}`" style="text-decoration:none;color:inherit">
      <div class="card row">
        <div>
          <b style="font-size:17px">{{ t.name }}</b>
          <span class="tag" style="margin-left:8px">{{ t.subjects[0].name }}</span>
          <p class="muted" style="margin:6px 0 0">{{ t.title }} · ⭐{{ t.rating }}</p>
        </div>
        <span class="btn small">约</span>
      </div>
    </router-link>
    <Tabbar />
  </div>
</template>
