<script setup>
/* 自绘底部导航（项目不使用 tabBar，由本组件承担）
   三个入口与页面一一对应，用 reLaunch 避免页面堆栈累积 */
const props = defineProps({
  current: { type: String, default: 'index' }
})

const TABS = [
  { key: 'index', label: '选品', path: '/pages/index/index', icon: 'nav-goods' },
  { key: 'history', label: '订单', path: '/pages/order/history', icon: 'nav-order' },
  { key: 'mine', label: '我的', path: '/pages/mine/index', icon: 'nav-mine' }
]

const iconOf = (tab) => `/static/icons/${props.current === tab.key ? 'teal' : 'ink'}/${tab.icon}.png`

function go(tab) {
  if (tab.key === props.current) return
  uni.reLaunch({ url: tab.path })
}
</script>

<template>
  <view class="tabbar">
    <view class="tabbar__inner">
      <view
        v-for="tab in TABS"
        :key="tab.key"
        class="tabbar__item"
        :class="{ 'tabbar__item--active': current === tab.key }"
        hover-class="tabbar__item--hover"
        @tap="go(tab)"
      >
        <image class="tabbar__icon" :src="iconOf(tab)" mode="aspectFit" />
        <text class="tabbar__label">{{ tab.label }}</text>
      </view>
    </view>
    <view class="tabbar__safe" />
  </view>
</template>

<style lang="scss" scoped>
.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 200;
  background: $xh-surface;
  border-top: 2rpx solid $xh-line;
}
.tabbar__inner {
  height: 104rpx;
  display: flex;
}
.tabbar__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
}
.tabbar__item--hover {
  background: $xh-paper;
}
.tabbar__icon {
  width: 44rpx;
  height: 44rpx;
}
.tabbar__label {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
  letter-spacing: 2rpx;
}
.tabbar__item--active .tabbar__label {
  color: $xh-teal-800;
  font-weight: 600;
}
.tabbar__safe {
  height: env(safe-area-inset-bottom);
}
</style>
