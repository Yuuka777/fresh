<script setup>
/* 温区标签：生鲜的冷链语言，纯文字不配图标 */
import { computed } from 'vue'
import { zoneTone } from '@/common/dict'

const props = defineProps({
  zone: { type: Number, default: 1 },
  size: { type: String, default: 'normal' } // normal | small
})

const tone = computed(() => zoneTone(props.zone))
</script>

<template>
  <text
    class="zone"
    :class="{ 'zone--small': size === 'small' }"
    :style="{ background: tone.bg, color: tone.fg }"
  >
    {{ tone.label }}
  </text>
</template>

<style lang="scss" scoped>
.zone {
  display: inline-block;
  font-size: $xh-text-xs;
  line-height: 32rpx;
  padding: 0 10rpx;
  border-radius: $xh-radius-xs;
}
.zone--small {
  font-size: $xh-text-xs;
  line-height: 28rpx;
  padding: 0 8rpx;
}
</style>
