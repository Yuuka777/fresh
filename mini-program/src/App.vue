<script setup>
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'

onLaunch(() => {
  /* 恢复本地登录态（token 存 storage，见 store/user.js） */
  useUserStore().restore()
})

onShow(() => {
  /* 从后台切回前台时不做自动登录，避免用过期 token 反复请求 */
})
</script>

<style lang="scss">
/* ============================================================
   全局样式（编译进 app.wxss，对全部页面生效）
   只放三类东西：页面基线、可复用的视觉件、文字层级
   ============================================================ */
page {
  background-color: $xh-paper;
  color: $xh-ink-900;
  font-size: $xh-text-base;
  line-height: 1.5;
  -webkit-font-smoothing: antialiased;
}

view,
text,
scroll-view {
  box-sizing: border-box;
}

/* —— 页面容器：底部留出自绘导航栏的高度 —— */
.xh-page {
  min-height: 100vh;
  padding-bottom: env(safe-area-inset-bottom);
}
.xh-page--with-tabbar {
  padding-bottom: calc(112rpx + env(safe-area-inset-bottom));
}

/* —— 纸卡片：白底 + 细线，不用重阴影 —— */
.xh-card {
  background: $xh-surface;
  border-radius: $xh-radius;
  border: 2rpx solid $xh-line;
}
.xh-card--flat {
  border: 0;
  border-radius: 0;
}

/* —— 分隔线 —— */
.xh-hairline {
  height: 2rpx;
  background: $xh-line-soft;
}

/* —— 数字：等宽 + 表格数字，金额与计数专用；价格用墨色（规范：不用橙红） —— */
.xh-num {
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.5rpx;
}
.xh-price {
  color: $xh-ink-900;
  font-weight: 600;
}
.xh-price__symbol {
  font-size: 0.7em;
  margin-right: 2rpx;
}

/* —— 文字层级 —— */
.xh-label {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
  letter-spacing: 2rpx;
}
.xh-dim {
  color: $xh-ink-400;
}
.xh-muted {
  color: $xh-ink-300;
}
.xh-strong {
  color: $xh-ink-900;
  font-weight: 600;
}
.xh-title {
  font-size: $xh-text-xl;
  font-weight: 600;
  letter-spacing: -0.5rpx;
  color: $xh-ink-900;
}
.xh-section-title {
  font-size: $xh-text-lg;
  font-weight: 600;
  color: $xh-ink-900;
}

/* —— 主按钮 / 次按钮：触控高度 ≥ 88rpx —— */
.xh-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88rpx;
  border-radius: $xh-radius;
  font-size: $xh-text-base;
  letter-spacing: 1rpx;
}
.xh-btn--primary {
  background: $xh-teal-600;
  color: #ffffff;
}
.xh-btn--primary--active {
  background: $xh-teal-800;
}
.xh-btn--ghost {
  background: $xh-surface;
  color: $xh-teal-800;
  border: 2rpx solid $xh-teal-200;
}
.xh-btn--disabled {
  background: $xh-line;
  color: $xh-ink-300;
}

/* —— 无图商品占位块（首字 + 温区配色由内联样式给） —— */
.xh-thumb {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $xh-radius-sm;
  overflow: hidden;
  border: 2rpx solid $xh-line;
  background: $xh-surface-2;
  flex-shrink: 0;
}
.xh-thumb__initial {
  font-weight: 600;
}
</style>
