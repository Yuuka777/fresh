<script setup>
/* 提交成功（支付完成）结果页 */
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { money } from '@/common/format'

const orderNumber = ref('')
const amount = ref('0')

onLoad((options) => {
  orderNumber.value = options.orderNumber || ''
  amount.value = options.amount || '0'
})

function goHistory() {
  uni.reLaunch({ url: '/pages/order/history' })
}

function goIndex() {
  uni.reLaunch({ url: '/pages/index/index' })
}

function copyNumber() {
  uni.setClipboardData({ data: orderNumber.value })
}
</script>

<template>
  <view class="page xh-page">
    <xh-nav-bar title="支付成功" :show-back="false" />

    <view class="result">
      <xh-icon name="success" :size="88" />
      <text class="result__title">支付成功</text>
      <text class="result__desc">门店已收到订单，正在分拣打包，预计 30 分钟内送达</text>

      <view class="result__amount">
        <text class="xh-num">
          <text class="result__symbol">¥</text>{{ money(amount) }}
        </text>
      </view>

      <view class="result__no" @tap="copyNumber">
        <text class="result__no-label">订单号</text>
        <text class="result__no-value xh-num">{{ orderNumber }}</text>
      </view>

      <view class="result__ops">
        <view class="result__btn result__btn--primary" hover-class="result__btn--hover" @tap="goHistory">
          <text>查看订单</text>
        </view>
        <view class="result__btn" hover-class="result__btn--hover" @tap="goIndex">
          <text>继续选品</text>
        </view>
      </view>

      <view class="result__tip">
        <text>若门店未及时接单，可在订单详情里点击「催单」提醒店员。</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page {
  background: $xh-paper;
}
.result {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $xh-10 $xh-4;
}
.result__mark {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  background: $xh-teal-50;
  border: 2rpx solid #b8e3d4;
  color: $xh-teal-800;
  font-size: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.result__title {
  margin-top: $xh-4;
  font-size: $xh-text-2xl;
  font-weight: 700;
  color: $xh-ink-900;
  letter-spacing: 4rpx;
}
.result__desc {
  margin-top: $xh-2;
  max-width: 540rpx;
  text-align: center;
  font-size: $xh-text-sm;
  line-height: 1.7;
  color: $xh-ink-400;
}
.result__amount {
  margin-top: $xh-6;
  font-size: 72rpx;
  font-weight: 700;
  color: $xh-ink-900;
}
.result__symbol {
  font-size: 36rpx;
  margin-right: 6rpx;
}
.result__no {
  margin-top: $xh-3;
  display: flex;
  align-items: center;
  gap: $xh-2;
}
.result__no-label {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.result__no-value {
  font-size: $xh-text-xs;
  color: $xh-ink-700;
}
.result__ops {
  margin-top: $xh-8;
  display: flex;
  gap: $xh-3;
}
.result__btn {
  min-width: 240rpx;
  height: 96rpx;
  border-radius: $xh-radius;
  border: 2rpx solid $xh-line;
  background: $xh-surface;
  display: flex;
  align-items: center;
  justify-content: center;
}
.result__btn text {
  font-size: $xh-text-base;
  color: $xh-ink-700;
}
.result__btn--primary {
  background: $xh-teal-600;
  border-color: $xh-teal-600;
}
.result__btn--primary text {
  color: #ffffff;
  font-weight: 600;
}
.result__btn--hover {
  opacity: 0.85;
}
.result__tip {
  margin-top: $xh-8;
  padding: $xh-3;
  border-radius: $xh-radius-sm;
  background: $xh-surface-2;
  border: 2rpx solid $xh-line-soft;
  font-size: $xh-text-xs;
  line-height: 1.8;
  color: $xh-ink-400;
  text-align: center;
}
</style>
