<script setup>
/* 状态标签：订单状态 / 支付状态共用，档位决定配色（不做彩虹色） */
import { computed } from 'vue'
import { orderStatusLabel, orderStatusTone, PAY_STATUS_MAP } from '@/common/dict'

const props = defineProps({
  type: { type: String, default: 'order' }, // order | pay
  value: { type: [Number, String], default: null },
  solid: { type: Boolean, default: false }
})

const item = computed(() => {
  if (props.type === 'pay') {
    return PAY_STATUS_MAP[props.value] || { label: '未知', tone: 'muted' }
  }
  return { label: orderStatusLabel(props.value), tone: orderStatusTone(props.value) }
})
</script>

<template>
  <text class="tag" :class="[`tag--${item.tone}`, { 'tag--solid': solid }]">{{ item.label }}</text>
</template>

<style lang="scss" scoped>
.tag {
  display: inline-block;
  font-size: $xh-text-xs;
  line-height: 34rpx;
  padding: 0 12rpx;
  border-radius: $xh-radius-xs;
  border: 2rpx solid transparent;
}
.tag--ok {
  background: $xh-teal-50;
  border-color: #b8e3d4;
  color: $xh-teal-800;
}
.tag--accent {
  background: $xh-cinnabar-50;
  border-color: #eec7bb;
  color: $xh-cinnabar-700;
}
.tag--warn {
  background: $xh-amber-50;
  border-color: #ecd7b4;
  color: $xh-amber-700;
}
.tag--info {
  background: $xh-blue-50;
  border-color: #c3d6e8;
  color: $xh-blue-600;
}
.tag--muted {
  background: #f1f2ef;
  border-color: $xh-line;
  color: $xh-ink-400;
}
.tag--solid {
  border-color: transparent;
}
</style>
