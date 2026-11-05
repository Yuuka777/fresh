<script setup>
/* 商品 / 组合箱卡片：首页与搜索结果页共用，避免两处样式与交互漂移
   mode = 'goods'  → 有规格走「选规格」，无规格直接步进器
   mode = 'bundle' → 「查看内含」+ 步进器
   已加购数量直接读购物车 store（服务端是唯一事实来源），父级只需处理加减意图 */
import { computed } from 'vue'
import { useCartStore } from '@/store/cart'

const props = defineProps({
  item: { type: Object, required: true },
  mode: { type: String, default: 'goods' }
})

defineEmits(['plus', 'minus', 'more'])

const cart = useCartStore()

const hasSpecs = computed(() => (props.item.specs || []).length > 0)

const count = computed(() =>
  props.mode === 'bundle'
    ? cart.numberInCart({ bundleId: props.item.id })
    : cart.numberInCart({ goodsId: props.item.id })
)
</script>

<template>
  <view class="card xh-card">
    <xh-goods-thumb
      :name="item.name"
      :image="item.image"
      :zone="item.storageZone"
      :size="168"
    />

    <view class="card__info">
      <text class="card__name">{{ item.name }}</text>
      <text class="card__desc">{{ item.description || item.origin || '门店直配' }}</text>
      <view class="card__tags">
        <xh-zone-tag :zone="item.storageZone" size="small" />
        <text v-if="item.shelfLife" class="card__life">{{ item.shelfLife }}</text>
      </view>

      <view class="card__foot">
        <xh-price :value="item.price" size="md" />

        <view class="card__actions">
          <template v-if="mode === 'goods'">
            <xh-stepper
              v-if="!hasSpecs"
              :value="count"
              :min="0"
              size="small"
              @plus="$emit('plus')"
              @minus="$emit('minus')"
            />
            <view v-else class="spec-btn" hover-class="spec-btn--hover" @tap="$emit('plus')">
              <text>选规格</text>
              <text v-if="count > 0" class="spec-btn__badge xh-num">{{ count }}</text>
            </view>
          </template>

          <template v-else>
            <view class="more-btn" hover-class="more-btn--hover" @tap="$emit('more')">
              <text>查看内含</text>
            </view>
            <xh-stepper
              :value="count"
              :min="0"
              size="small"
              @plus="$emit('plus')"
              @minus="$emit('minus')"
            />
          </template>
        </view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.card {
  display: flex;
  gap: $xh-3;
  padding: $xh-3;
  margin-bottom: $xh-3;
}
.card__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.card__name {
  font-size: $xh-text-base;
  font-weight: 600;
  color: $xh-ink-900;
}
.card__desc {
  margin-top: 6rpx;
  font-size: $xh-text-xs;
  color: $xh-ink-400;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card__tags {
  margin-top: $xh-2;
  display: flex;
  align-items: center;
  gap: $xh-2;
}
.card__life {
  font-size: $xh-text-xs;
  color: $xh-ink-300;
}
.card__foot {
  margin-top: auto;
  padding-top: $xh-2;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}
.card__actions {
  display: flex;
  align-items: center;
  gap: $xh-2;
}

.spec-btn {
  height: 96rpx;
  padding: 0 $xh-3;
  border-radius: 999rpx;
  background: $xh-teal-600;
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.spec-btn text {
  font-size: $xh-text-xs;
  color: #ffffff;
}
.spec-btn--hover {
  background: $xh-teal-800;
}
.spec-btn__badge {
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  text-align: center;
  border-radius: 999rpx;
  background: #ffffff;
  color: $xh-teal-800;
  font-size: $xh-text-xs;
}

.more-btn {
  height: 96rpx;
  padding: 0 $xh-3;
  border-radius: 999rpx;
  border: 2rpx solid $xh-teal-200;
  display: flex;
  align-items: center;
}
.more-btn text {
  font-size: $xh-text-xs;
  color: $xh-teal-800;
}
.more-btn--hover {
  background: $xh-teal-50;
}
</style>
