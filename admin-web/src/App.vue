<template>
  <router-view v-slot="{ Component }">
    <transition name="xh-view" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<style>
/* 页面切换：轻微上浮 + 淡入，时长统一 300ms，不拖沓 */
.xh-view-enter-active {
  transition: opacity var(--xh-dur) var(--xh-ease-out), transform var(--xh-dur) var(--xh-ease-out);
}
.xh-view-leave-active {
  transition: opacity var(--xh-dur-fast) var(--xh-ease-in);
}
.xh-view-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.xh-view-leave-to {
  opacity: 0;
}
</style>
