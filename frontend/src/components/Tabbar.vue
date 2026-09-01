<script setup lang="ts">
import { useRoute } from 'vue-router'
const route = useRoute()
const tabs = [
  { to: '/', label: '首页' },
  { to: '/book', label: '约课' },
  { to: '/history', label: '记录' },
  { to: '/mine', label: '我的' },
]
</script>

<template>
  <nav class="tabbar">
    <router-link v-for="t in tabs" :key="t.to" :to="t.to" :class="{ on: route.path === t.to }">
      {{ t.label }}
    </router-link>
  </nav>
</template>
