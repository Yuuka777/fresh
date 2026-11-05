<script setup>
/* 加减控件：触控区按 88rpx 高度做，符合小程序最小点击尺寸 */
const props = defineProps({
  value: { type: Number, default: 0 },
  min: { type: Number, default: 0 },
  max: { type: Number, default: 99 },
  size: { type: String, default: 'normal' }, // normal | small
  showWhenZero: { type: Boolean, default: false }
})

const emit = defineEmits(['plus', 'minus'])

function onPlus() {
  if (props.value >= props.max) {
    uni.showToast({ title: `最多 ${props.max} 份`, icon: 'none' })
    return
  }
  emit('plus')
}

function onMinus() {
  if (props.value <= props.min) return
  emit('minus')
}
</script>

<template>
  <view class="stepper" :class="{ 'stepper--small': size === 'small' }">
    <view
      v-if="value > min || showWhenZero"
      class="stepper__btn stepper__btn--minus"
      :class="{ 'stepper__btn--disabled': value <= min }"
      hover-class="stepper__btn--hover"
      @tap="onMinus"
    >
      <text class="stepper__sign">−</text>
    </view>
    <text v-if="value > 0 || showWhenZero" class="stepper__value xh-num">{{ value }}</text>
    <view class="stepper__btn stepper__btn--plus" hover-class="stepper__btn--hover" @tap="onPlus">
      <text class="stepper__sign stepper__sign--plus">+</text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.stepper {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.stepper__btn {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid $xh-teal-200;
  background: $xh-surface;
}
.stepper__btn--plus {
  background: $xh-teal-600;
  border-color: $xh-teal-600;
}
.stepper__btn--disabled {
  border-color: $xh-line;
}
.stepper__btn--hover {
  opacity: 0.8;
}
.stepper__sign {
  font-size: 34rpx;
  line-height: 1;
  color: $xh-teal-800;
}
.stepper__sign--plus {
  color: #ffffff;
}
.stepper__btn--disabled .stepper__sign {
  color: $xh-ink-300;
}
.stepper__value {
  min-width: 40rpx;
  text-align: center;
  font-size: $xh-text-base;
  font-weight: 600;
  color: $xh-ink-900;
}
.stepper--small .stepper__btn {
  width: 44rpx;
  height: 44rpx;
}
.stepper--small .stepper__sign {
  font-size: 28rpx;
}
.stepper--small .stepper__value {
  font-size: $xh-text-sm;
}
</style>
