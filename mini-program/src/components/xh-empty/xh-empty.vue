<script setup>
/* 空态 / 错误态 / 网络态统一组件
   在「等待后端」的交付形态下，这个组件是各页面的默认可见状态，故排版按正式内容对待 */
import { computed } from 'vue'

const props = defineProps({
  kind: { type: String, default: 'empty' }, // empty | error | network | login
  title: { type: String, default: '' },
  desc: { type: String, default: '' },
  actionText: { type: String, default: '' },
  retryText: { type: String, default: '' },
  compact: { type: Boolean, default: false }
})

const emit = defineEmits(['action', 'retry'])

const PRESET = {
  empty: { icon: 'empty', iconTone: 'ink', title: '这里还没有内容', tone: 'muted' },
  error: { icon: 'warning', iconTone: 'cinnabar', title: '数据没能取到', tone: 'accent' },
  network: { icon: 'close', iconTone: 'cinnabar', title: '网络好像不太顺', tone: 'accent' },
  login: { icon: 'nav-mine', iconTone: 'teal', title: '登录后可继续', tone: 'primary' }
}

const preset = computed(() => PRESET[props.kind] || PRESET.empty)
</script>

<template>
  <view class="empty" :class="[`empty--${preset.tone}`, { 'empty--compact': compact }]">
    <view class="empty__icon">
      <xh-icon :name="preset.icon" :tone="preset.iconTone" :size="112" />
    </view>
    <text class="empty__title">{{ title || preset.title }}</text>
    <text v-if="desc" class="empty__desc">{{ desc }}</text>
    <view class="empty__actions">
      <view
        v-if="actionText"
        class="empty__btn empty__btn--primary"
        hover-class="empty__btn--hover"
        @tap="emit('action')"
      >
        <text>{{ actionText }}</text>
      </view>
      <view
        v-if="retryText"
        class="empty__btn empty__btn--ghost"
        hover-class="empty__btn--hover"
        @tap="emit('retry')"
      >
        <text>{{ retryText }}</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $xh-10 $xh-4;
}
.empty--compact {
  padding: $xh-6 $xh-4;
}
.empty__icon {
  margin-bottom: $xh-1;
  opacity: 0.92;
}
.empty--compact .empty__icon {
  transform: scale(0.8);
}
.empty__title {
  margin-top: $xh-3;
  font-size: $xh-text-base;
  font-weight: 600;
  color: $xh-ink-900;
}
.empty__desc {
  margin-top: $xh-1;
  max-width: 520rpx;
  text-align: center;
  font-size: $xh-text-sm;
  line-height: 1.7;
  color: $xh-ink-400;
}
.empty__actions {
  margin-top: $xh-4;
  display: flex;
  gap: $xh-2;
}
.empty__btn {
  min-width: 200rpx;
  height: 96rpx;
  padding: 0 $xh-4;
  border-radius: $xh-radius;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $xh-text-sm;
}
.empty__btn--primary {
  background: $xh-teal-600;
  color: #ffffff;
}
.empty__btn--ghost {
  background: $xh-surface;
  color: $xh-teal-800;
  border: 2rpx solid $xh-teal-200;
}
.empty__btn--hover {
  opacity: 0.85;
}
</style>
