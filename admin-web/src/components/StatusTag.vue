<script setup>
import { computed } from 'vue'
import {
  BUNDLE_STATUS_MAP,
  CATEGORY_STATUS_MAP,
  GOODS_STATUS_MAP,
  ORDER_STATUS_MAP,
  PAY_STATUS_MAP,
  STAFF_STATUS_MAP
} from '@/constants/dict'

/* 语义标签：五种语义（ok/warn/accent/info/muted）各自一色，不做彩虹 */
const props = defineProps({
  /* order 订单状态 / pay 支付状态 / staff 店员启停 / category 分类启停
     / goods 商品上下架 / bundle 组合箱起售停售 */
  type: { type: String, default: 'order' },
  value: { type: [Number, String], default: null }
})

/* 各对象的字典不同：同样是 1，店员是「已启用」、商品是「已上架」、组合箱是「已起售」 */
const DICT = {
  staff: STAFF_STATUS_MAP,
  category: CATEGORY_STATUS_MAP,
  goods: GOODS_STATUS_MAP,
  bundle: BUNDLE_STATUS_MAP,
  pay: PAY_STATUS_MAP,
  order: ORDER_STATUS_MAP
}

const item = computed(() => {
  const dict = DICT[props.type] || ORDER_STATUS_MAP
  return dict[props.value] || { label: '未知', tone: 'muted' }
})
</script>

<template>
  <span class="tag" :class="`tag--${item.tone || 'muted'}`">{{ item.label }}</span>
</template>

<style scoped>
.tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 var(--xh-2);
  border-radius: var(--xh-radius-xs);
  border: 1px solid transparent;
  font-size: var(--xh-text-xs);
  white-space: nowrap;
}
.tag--ok {
  background: var(--xh-teal-50);
  border-color: #b8e3d4;
  color: var(--xh-teal-800);
}
.tag--accent {
  background: var(--xh-cinnabar-50);
  border-color: #eec7bb;
  color: var(--xh-cinnabar-700);
}
.tag--warn {
  background: var(--xh-amber-50);
  border-color: #ecd7b4;
  color: var(--xh-amber-700);
}
.tag--info {
  background: var(--xh-blue-50);
  border-color: #c3d6e8;
  color: var(--xh-blue-600);
}
.tag--muted {
  background: #f1f2ef;
  border-color: var(--xh-line);
  color: var(--xh-ink-400);
}
</style>
