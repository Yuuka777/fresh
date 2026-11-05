<script setup>
/* 价格排版：符号小、数字大，金额一律墨色（视觉规范：价格不用橙红） */
import { computed } from 'vue'
import { money } from '@/common/format'

const props = defineProps({
  value: { type: [Number, String], default: 0 },
  size: { type: String, default: 'md' }, // sm | md | lg | xl
  showSymbol: { type: Boolean, default: true },
  prefix: { type: String, default: '' },
  suffix: { type: String, default: '' },
  strike: { type: Boolean, default: false }
})

const text = computed(() => money(props.value))
</script>

<template>
  <text class="price" :class="[`price--${size}`, { 'price--strike': strike }]">
    <text v-if="prefix" class="price__prefix">{{ prefix }}</text>
    <text v-if="showSymbol" class="xh-price__symbol">¥</text>
    <text class="xh-num price__num">{{ text }}</text>
    <text v-if="suffix" class="price__suffix">{{ suffix }}</text>
  </text>
</template>

<style lang="scss" scoped>
.price {
  color: $xh-ink-900;
  font-weight: 600;
}
.price__num {
  /* 等宽数字，避免加减时数字宽度跳动 */
}
.price__prefix,
.price__suffix {
  font-size: 0.72em;
  font-weight: 400;
  color: $xh-ink-400;
  margin-right: 6rpx;
}
.price__suffix {
  margin: 0 0 0 6rpx;
}
/* ¥ 符号由全局基类按 0.7em 缩放；小号/中号价格下会掉到 9px 左右，
   低于可读底线，这里兜到 12px（大号仍按比例缩放，保持符号小于数字的排法） */
.price--sm .xh-price__symbol,
.price--md .xh-price__symbol {
  font-size: $xh-text-xs;
}
.price--sm {
  font-size: $xh-text-sm;
}
.price--md {
  font-size: $xh-text-lg;
}
.price--lg {
  font-size: $xh-text-xl;
}
.price--xl {
  font-size: $xh-text-2xl;
}
.price--strike {
  color: $xh-ink-300;
  font-weight: 400;
  text-decoration: line-through;
}
</style>
