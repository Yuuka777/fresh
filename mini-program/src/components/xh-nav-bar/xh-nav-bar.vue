<script setup>
/* 自绘导航栏：无 tabBar 方案下统一承担标题与返回
   状态栏高度取系统信息；内容区高度与右侧留白按微信胶囊按钮实测值推算，
   避免标题变长后钻到胶囊下方（胶囊在 navigationStyle: custom 下始终浮在页面上） */
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  showBack: { type: Boolean, default: true },
  subtitle: { type: String, default: '' },
  /* 透明导航栏：用于首页（内容顶到状态栏下方） */
  transparent: { type: Boolean, default: false }
})

const sys = uni.getSystemInfoSync()
const statusBarHeight = sys.statusBarHeight || 20
const windowWidth = sys.windowWidth || 375

let capsule = null
try {
  capsule = typeof uni.getMenuButtonBoundingClientRect === 'function'
    ? uni.getMenuButtonBoundingClientRect()
    : null
} catch (e) {
  capsule = null
}
const hasCapsule = !!(capsule && capsule.height && capsule.left)

/* 内容区高度 = 胶囊上下留白对称 + 胶囊自身高度 */
const barHeight = hasCapsule
  ? `${(capsule.top - statusBarHeight) * 2 + capsule.height}px`
  : ''

/* 右侧避让宽度 = 屏宽 - 胶囊左边界；左侧取同宽，使标题在屏内居中 */
const sideWidth = hasCapsule
  ? `${Math.max(64 * (windowWidth / 750), windowWidth - capsule.left + 8)}px`
  : ''

const barStyle = computed(() => ({ paddingTop: `${statusBarHeight}px` }))
const innerStyle = computed(() => (barHeight ? { height: barHeight } : {}))
const sideStyle = computed(() => (sideWidth ? { width: sideWidth } : {}))

function onBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.reLaunch({ url: '/pages/index/index' })
  }
}
</script>

<template>
  <view class="nav" :class="{ 'nav--transparent': transparent }" :style="barStyle">
    <view class="nav__bar" :style="innerStyle">
      <view class="nav__side" :style="sideStyle">
        <view v-if="showBack" class="nav__back" hover-class="nav__back--hover" @tap="onBack">
          <xh-icon name="back" :size="40" />
        </view>
      </view>

      <view class="nav__center">
        <text class="nav__title">{{ title }}</text>
        <text v-if="subtitle" class="nav__subtitle">{{ subtitle }}</text>
      </view>

      <view class="nav__side nav__side--right" :style="sideStyle">
        <slot name="right" />
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.nav {
  position: sticky;
  top: 0;
  z-index: 100;
  background: $xh-surface;
  border-bottom: 2rpx solid $xh-line-soft;
}
.nav--transparent {
  background: transparent;
  border-bottom: 0;
}
/* 88rpx 为取不到胶囊信息时的回退高度（H5 / 非微信端） */
.nav__bar {
  height: 88rpx;
  display: flex;
  align-items: center;
  padding: 0 $xh-3;
}
.nav__side {
  min-width: 64rpx;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
.nav__side--right {
  justify-content: flex-end;
}
.nav__back {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $xh-radius-sm;
}
.nav__back--hover {
  background: $xh-paper;
}
.nav__center {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.nav__title {
  font-size: $xh-text-lg;
  font-weight: 600;
  color: $xh-ink-900;
  letter-spacing: 1rpx;
}
.nav__subtitle {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
  margin-top: 2rpx;
}
</style>
