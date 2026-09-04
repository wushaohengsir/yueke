<script setup lang="ts">
withDefaults(defineProps<{ size?: number; color?: string }>(), { size: 24, color: 'currentColor' })
</script>

<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" :fill="color" aria-hidden="true">
    <path d="M12 1.5L13.6 6.2L18.2 3.8L16.6 8.8L21.5 9.8L17.8 13.2L20.6 17.4L15.4 16.6L16.2 21.5L12 19L7.8 21.5L8.6 16.6L3.4 17.4L6.2 13.2L2.5 9.8L7.4 8.8L5.8 3.8L10.4 6.2Z" />
    <path d="M11.5 18.6L12.5 18.6L12.35 23.2L11.65 23.2Z" />
  </svg>
</template>
