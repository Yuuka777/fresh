<script setup>
/* ============================================================
   网络异常兜底页
   触发：请求层判定后端未就绪 / 网络不可达时，由页面自行跳转过来
   职责：给出可执行的下一步（重试 / 回首页 / 检查设置），不做自动轮询
   ============================================================ */
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const reason = ref('')
const checking = ref(false)
const networkType = ref('unknown')

onLoad((options) => {
  reason.value = decodeURIComponent(options.reason || '')
  uni.getNetworkType({
    success: (res) => {
      networkType.value = res.networkType
    }
  })
})

function onRetry() {
  checking.value = true
  uni.getNetworkType({
    success: (res) => {
      networkType.value = res.networkType
      checking.value = false
      if (res.networkType === 'none') {
        uni.showToast({ title: '当前没有网络连接', icon: 'none' })
        return
      }
      /* 网络已恢复：回到首页重新拉取 */
      uni.reLaunch({ url: '/pages/index/index' })
    },
    fail: () => {
      checking.value = false
      uni.showToast({ title: '网络状态获取失败', icon: 'none' })
    }
  })
}

function openSetting() {
  uni.openSetting({
    fail: () => uni.showToast({ title: '请在系统设置中检查网络', icon: 'none' })
  })
}

function goHome() {
  uni.reLaunch({ url: '/pages/index/index' })
}
</script>

<template>
  <view class="page xh-page">
    <xh-nav-bar title="网络异常" :show-back="false" />

    <view class="wrap">
      <xh-icon name="warning" :size="88" />
      <text class="title">没连上服务</text>
      <text class="desc">
        {{ reason || '页面需要的接口没有响应。若是本地开发环境，请确认后端服务已启动（默认 8080 端口）。' }}
      </text>

      <view class="meta">
        <view class="meta__row">
          <text class="meta__label">当前网络</text>
          <text class="meta__value">{{ networkType === 'none' ? '未连接' : networkType }}</text>
        </view>
        <view class="meta__row">
          <text class="meta__label">接口地址</text>
          <text class="meta__value xh-num">http://localhost:8080</text>
        </view>
      </view>

      <view class="ops">
        <view class="ops__btn ops__btn--primary" hover-class="ops__btn--hover" @tap="onRetry">
          <text>{{ checking ? '检查中…' : '重新连接' }}</text>
        </view>
        <view class="ops__btn" hover-class="ops__btn--hover" @tap="openSetting">
          <text>网络设置</text>
        </view>
      </view>

      <text class="back" @tap="goHome">返回首页</text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page {
  background: $xh-paper;
}
.wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $xh-10 $xh-4;
}
.mark {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  background: $xh-cinnabar-50;
  border: 2rpx solid #eec7bb;
  color: $xh-cinnabar-700;
  font-size: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.title {
  margin-top: $xh-4;
  font-size: $xh-text-xl;
  font-weight: 700;
  color: $xh-ink-900;
  letter-spacing: 2rpx;
}
.desc {
  margin-top: $xh-2;
  max-width: 560rpx;
  text-align: center;
  font-size: $xh-text-sm;
  line-height: 1.8;
  color: $xh-ink-400;
}
.meta {
  margin-top: $xh-6;
  width: 100%;
  padding: $xh-3;
  border-radius: $xh-radius;
  background: $xh-surface;
  border: 2rpx solid $xh-line;
}
.meta__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $xh-2 0;
}
.meta__label {
  font-size: $xh-text-sm;
  color: $xh-ink-400;
}
.meta__value {
  font-size: $xh-text-sm;
  color: $xh-ink-900;
}
.ops {
  margin-top: $xh-6;
  display: flex;
  gap: $xh-3;
}
.ops__btn {
  min-width: 220rpx;
  height: 96rpx;
  border-radius: $xh-radius;
  border: 2rpx solid $xh-line;
  background: $xh-surface;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ops__btn text {
  font-size: $xh-text-base;
  color: $xh-ink-700;
}
.ops__btn--primary {
  background: $xh-teal-600;
  border-color: $xh-teal-600;
}
.ops__btn--primary text {
  color: #ffffff;
  font-weight: 600;
}
.ops__btn--hover {
  opacity: 0.85;
}
.back {
  margin-top: $xh-6;
  font-size: $xh-text-sm;
  color: $xh-teal-800;
}
</style>
