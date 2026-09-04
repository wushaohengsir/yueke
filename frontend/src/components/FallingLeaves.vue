<script setup lang="ts">
import MapleLeaf from './MapleLeaf.vue'

// 固定一批叶片参数，避免每次渲染随机跳动
const leaves = [
  { left: '4%',  size: 20, dur: 15, delay: 0,   color: '#c8402a', op: 0.45 },
  { left: '14%', size: 14, dur: 19, delay: 3,   color: '#d96a4a', op: 0.35 },
  { left: '26%', size: 24, dur: 13, delay: 6,   color: '#c9a96a', op: 0.4 },
  { left: '38%', size: 16, dur: 17, delay: 1.5, color: '#a03020', op: 0.4 },
  { left: '50%', size: 22, dur: 21, delay: 8,   color: '#d96a4a', op: 0.3 },
  { left: '62%', size: 15, dur: 14, delay: 4.5, color: '#c8402a', op: 0.42 },
  { left: '74%', size: 26, dur: 18, delay: 2,   color: '#e08a5a', op: 0.35 },
  { left: '85%', size: 18, dur: 16, delay: 7,   color: '#c9a96a', op: 0.38 },
  { left: '93%', size: 13, dur: 20, delay: 5,   color: '#a03020', op: 0.3 },
]
</script>

<template>
  <div class="falling-leaves" aria-hidden="true">
    <span
      v-for="(l, i) in leaves" :key="i" class="fl"
      :style="{ left: l.left, animationDuration: l.dur + 's', animationDelay: -l.delay + 's' }"
    >
      <span class="fl-sway" :style="{ animationDuration: l.dur / 3 + 's' }">
        <MapleLeaf :size="l.size" :color="l.color" :style="{ opacity: l.op }" />
      </span>
    </span>
  </div>
</template>
