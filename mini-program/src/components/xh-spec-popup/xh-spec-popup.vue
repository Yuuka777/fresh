<script setup>
/* 规格选择弹层（商品卡「选规格」触发）
   规格数据来自 /user/goods/list：specs 为 [{ name, value: [] }]，无规格时为 null
   选定后的 specText 形如「净重:约500g,包装:散装」，与《接口文档》7.1 口径一致
   数量固定为 1 件：加购接口不带数量，由服务端累加（与苍穹外卖口径一致） */
import { computed, ref, watch } from 'vue'
import { money } from '@/common/format'

const props = defineProps({
  visible: { type: Boolean, default: false },
  goods: { type: Object, default: () => ({}) },
  submitting: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'confirm'])

const selected = ref({})

/* 每次打开重置选择：默认勾选每个规格的第一个候选值，减少一次点击 */
watch(
  () => props.visible,
  (v) => {
    if (!v) return
    const next = {}
    ;(props.goods.specs || []).forEach((s) => {
      next[s.name] = (s.value && s.value[0]) || ''
    })
    selected.value = next
  }
)

const specGroups = computed(() => props.goods.specs || [])

const specText = computed(() =>
  specGroups.value
    .map((s) => `${s.name}:${selected.value[s.name]}`)
    .filter((t) => t.indexOf('undefined') === -1)
    .join(',')
)

function pick(name, value) {
  selected.value = { ...selected.value, [name]: value }
}

function onConfirm() {
  const missing = specGroups.value.find((s) => !selected.value[s.name])
  if (missing) {
    uni.showToast({ title: `请选择${missing.name}`, icon: 'none' })
    return
  }
  emit('confirm', { specText: specText.value })
}
</script>

<template>
  <view v-if="visible" class="popup">
    <view class="popup__mask" @tap="emit('close')" />

    <view class="popup__sheet">
      <view class="popup__head">
        <xh-goods-thumb :name="goods.name" :image="goods.image" :zone="goods.storageZone" :size="160" />
        <view class="popup__head-text">
          <text class="popup__name">{{ goods.name }}</text>
          <view class="popup__meta">
            <xh-price :value="goods.price" size="lg" />
            <xh-zone-tag :zone="goods.storageZone" size="small" />
          </view>
          <text class="popup__origin">{{ goods.origin || '产地直采' }}</text>
        </view>
        <view class="popup__close" hover-class="popup__close--hover" @tap="emit('close')">
          <xh-icon name="close" :size="34" />
        </view>
      </view>

      <scroll-view class="popup__body" scroll-y>
        <view v-for="group in specGroups" :key="group.name" class="spec">
          <text class="spec__name">{{ group.name }}</text>
          <view class="spec__values">
            <view
              v-for="v in group.value"
              :key="v"
              class="spec__chip"
              :class="{ 'spec__chip--active': selected[group.name] === v }"
              hover-class="spec__chip--hover"
              @tap="pick(group.name, v)"
            >
              <text>{{ v }}</text>
            </view>
          </view>
        </view>
        <view class="spec__hint">库存以门店实际分拣为准，缺货会由店员联系你</view>
      </scroll-view>

      <view class="popup__foot">
        <view
          class="popup__submit"
          :class="{ 'popup__submit--busy': submitting }"
          hover-class="popup__submit--hover"
          @tap="onConfirm"
        >
          <text>{{ submitting ? '加入中…' : `加入购物车 · ¥${money(goods.price)}` }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.popup {
  position: fixed;
  inset: 0;
  z-index: 500;
}
.popup__mask {
  position: absolute;
  inset: 0;
  background: rgba(10, 31, 25, 0.45);
}
.popup__sheet {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  background: $xh-surface;
  border-radius: $xh-radius-lg $xh-radius-lg 0 0;
  padding-bottom: env(safe-area-inset-bottom);
  animation: sheet-up 240ms cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes sheet-up {
  from {
    transform: translateY(24rpx);
    opacity: 0.6;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.popup__head {
  position: relative;
  display: flex;
  gap: $xh-3;
  padding: $xh-4;
  border-bottom: 2rpx solid $xh-line-soft;
}
.popup__head-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.popup__name {
  font-size: $xh-text-lg;
  font-weight: 600;
  color: $xh-ink-900;
}
.popup__meta {
  display: flex;
  align-items: center;
  gap: $xh-2;
}
.popup__origin {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.popup__close {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 40rpx;
  color: $xh-ink-400;
}
.popup__close--hover {
  background: $xh-paper;
}

.popup__body {
  max-height: 46vh;
  padding: $xh-4;
}
.spec + .spec {
  margin-top: $xh-4;
}
.spec__name {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
  letter-spacing: 1rpx;
}
.spec__values {
  margin-top: $xh-2;
  display: flex;
  flex-wrap: wrap;
  gap: $xh-2;
}
.spec__chip {
  min-width: 140rpx;
  height: 68rpx;
  padding: 0 $xh-3;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid $xh-line;
  border-radius: $xh-radius-sm;
  font-size: $xh-text-sm;
  color: $xh-ink-700;
  background: $xh-surface-2;
}
.spec__chip--active {
  border-color: $xh-teal-600;
  background: $xh-teal-50;
  color: $xh-teal-800;
  font-weight: 600;
}
.spec__chip--hover {
  border-color: $xh-teal-200;
}
.spec__hint {
  margin-top: $xh-4;
  font-size: $xh-text-xs;
  line-height: 1.7;
  color: $xh-ink-300;
}

.popup__foot {
  display: flex;
  align-items: center;
  gap: $xh-3;
  padding: $xh-3 $xh-4 $xh-4;
  border-top: 2rpx solid $xh-line-soft;
}
.popup__submit {
  flex: 1;
  height: 88rpx;
  border-radius: $xh-radius;
  background: $xh-teal-600;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $xh-text-base;
}
.popup__submit--hover {
  background: $xh-teal-800;
}
.popup__submit--busy {
  background: $xh-teal-500;
}
</style>
