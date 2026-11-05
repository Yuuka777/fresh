<script setup>
/* ============================================================
   支付
   契约：/user/order/payment（PUT）
   说明：本项目不接真实微信支付，点击即调用接口推进状态；
        支付成功后订单才会进入「待接单」并触发管理端来单提醒
   ============================================================ */
import { computed, onUnmounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { orderApi } from '@/api/user'
import { ORDER_PAY_TIMEOUT_MINUTES } from '@/common/config'
import { countdown, money } from '@/common/format'

const orderNumber = ref('')
const amount = ref('0')
const orderTime = ref('')
const paying = ref(false)
const left = ref({ text: '15:00', expired: false })

let timer = null

const deadline = computed(() => {
  if (!orderTime.value) return new Date(Date.now() + ORDER_PAY_TIMEOUT_MINUTES * 60000)
  const t = new Date(String(orderTime.value).replace(/-/g, '/')).getTime()
  return new Date(t + ORDER_PAY_TIMEOUT_MINUTES * 60000)
})

onLoad((options) => {
  orderNumber.value = options.orderNumber || ''
  amount.value = options.amount || '0'
  orderTime.value = decodeURIComponent(options.orderTime || '')
  tick()
  timer = setInterval(tick, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

function tick() {
  left.value = countdown(deadline.value)
  if (left.value.expired && timer) {
    clearInterval(timer)
    timer = null
  }
}

async function onPay() {
  if (paying.value) return
  if (left.value.expired) {
    uni.showModal({
      title: '订单已超时',
      content: '超过 15 分钟未支付的订单会被系统自动取消，请重新下单。',
      showCancel: false,
      confirmText: '返回首页',
      success: () => uni.reLaunch({ url: '/pages/index/index' })
    })
    return
  }

  paying.value = true
  try {
    await orderApi.payment(orderNumber.value)
    /* 支付成功即视为订单进入待接单，管理端此时收到来单提醒 */
    uni.redirectTo({
      url: `/pages/order/success?orderNumber=${orderNumber.value}&amount=${amount.value}`
    })
  } catch (e) {
    /* 订单状态错误（如已被超时取消）时提示重新下单 */
    if ((e.message || '').indexOf('状态') !== -1) {
      setTimeout(() => uni.reLaunch({ url: '/pages/order/history' }), 1200)
    }
  } finally {
    paying.value = false
  }
}

function goBack() {
  uni.reLaunch({ url: '/pages/order/history' })
}
</script>

<template>
  <view class="page xh-page">
    <xh-nav-bar title="支付订单" />

    <view class="body">
      <view class="amount">
        <text class="amount__label">待支付金额</text>
        <text class="amount__value xh-num">
          <text class="amount__symbol">¥</text>{{ money(amount) }}
        </text>
        <view class="amount__timer" :class="{ 'amount__timer--expired': left.expired }">
          <text v-if="left.expired">订单已超时，请重新下单</text>
          <text v-else>请在 <text class="xh-num">{{ left.text }}</text> 内完成支付，超时自动取消</text>
        </view>
      </view>

      <view class="order-no">
        <text class="order-no__label">订单号</text>
        <text class="order-no__value xh-num">{{ orderNumber }}</text>
      </view>

      <!-- 支付方式：仅微信支付。不放图标与单选圈 —— 只有一个选项，图形元素没有信息量 -->
      <view class="block xh-card">
        <text class="block__title">支付方式</text>
        <text class="pay-way__name">微信支付</text>
      </view>
    </view>

    <view class="bar">
      <view class="bar__cancel" hover-class="bar__cancel--hover" @tap="goBack">
        <text>稍后支付</text>
      </view>
      <view
        class="bar__pay"
        :class="{ 'bar__pay--disabled': left.expired || paying }"
        hover-class="bar__pay--hover"
        @tap="onPay"
      >
        <text>{{ left.expired ? '订单已超时' : paying ? '支付中…' : '确认支付' }}</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page {
  background: $xh-paper;
  padding-bottom: 160rpx;
}
.body {
  padding: $xh-4;
}
.amount {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $xh-2;
  padding: $xh-6 0 $xh-5;
}
.amount__label {
  font-size: $xh-text-sm;
  color: $xh-ink-400;
  letter-spacing: 2rpx;
}
.amount__value {
  font-size: 84rpx;
  font-weight: 700;
  color: $xh-ink-900;
  line-height: 1.1;
}
.amount__symbol {
  font-size: 40rpx;
  margin-right: 6rpx;
}
.amount__timer {
  margin-top: $xh-2;
  padding: 8rpx 20rpx;
  border-radius: $xh-radius-xs;
  background: $xh-amber-50;
  color: $xh-amber-700;
  font-size: $xh-text-sm;
}
.amount__timer--expired {
  background: $xh-cinnabar-50;
  color: $xh-cinnabar-700;
}

.order-no {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 $xh-1 $xh-3;
}
.order-no__label {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.order-no__value {
  font-size: $xh-text-xs;
  color: $xh-ink-700;
}

.block {
  padding: $xh-3;
}
.block__title {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
}
.pay-way__name {
  display: block;
  margin-top: $xh-3;
  font-size: $xh-text-base;
  color: $xh-ink-900;
}

.bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: $xh-3 $xh-4 calc(#{$xh-3} + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  gap: $xh-3;
  background: $xh-surface;
  border-top: 2rpx solid $xh-line;
}
.bar__cancel {
  height: 88rpx;
  min-width: 180rpx;
  border-radius: $xh-radius;
  border: 2rpx solid $xh-line;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bar__cancel text {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
}
.bar__cancel--hover {
  background: $xh-paper;
}
.bar__pay {
  flex: 1;
  height: 88rpx;
  border-radius: $xh-radius;
  background: $xh-teal-600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bar__pay text {
  font-size: $xh-text-base;
  color: #ffffff;
  font-weight: 600;
}
.bar__pay--hover {
  background: $xh-teal-800;
}
.bar__pay--disabled {
  background: $xh-line;
}
.bar__pay--disabled text {
  color: $xh-ink-300;
}
</style>
