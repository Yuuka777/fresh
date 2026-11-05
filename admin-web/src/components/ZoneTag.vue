<script setup>
import { computed } from 'vue'
import { STORAGE_ZONE_MAP } from '@/constants/dict'

const props = defineProps({
  zone: { type: Number, default: 1 }
})
const tone = computed(() => STORAGE_ZONE_MAP[props.zone] || STORAGE_ZONE_MAP[1])
</script>

<template>
  <span class="zone" :style="{ background: tone.bg, color: tone.fg }">{{ tone.label }}</span>
</template>

<style scoped>
.zone {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 var(--xh-2);
  border-radius: var(--xh-radius-xs);
  font-size: var(--xh-text-xs);
  font-family: var(--xh-font-mono);
  letter-spacing: 0.02em;
  white-space: nowrap;
}
</style>
