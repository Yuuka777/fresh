<script setup>
/* 台账式面板：左侧竖线 + 标题区 + 内容区，替代 Element 默认卡片的大圆角阴影 */
defineProps({
  title: { type: String, default: '' },
  sub: { type: String, default: '' },
  flush: { type: Boolean, default: false }
})
</script>

<template>
  <section class="panel">
    <header v-if="title" class="panel__head">
      <div class="panel__title-wrap">
        <h2 class="panel__title">{{ title }}</h2>
        <p v-if="sub" class="panel__sub">{{ sub }}</p>
      </div>
      <div class="panel__extra">
        <slot name="extra" />
      </div>
    </header>
    <div class="panel__body" :class="{ 'is-flush': flush }">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.panel {
  background: var(--xh-surface);
  border: 1px solid var(--xh-line);
  border-radius: var(--xh-radius);
  overflow: hidden;
}
.panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--xh-4);
  padding: var(--xh-4) var(--xh-5);
  border-bottom: 1px solid var(--xh-line);
  background: var(--xh-surface-2);
}
.panel__title-wrap {
  position: relative;
  padding-left: var(--xh-3);
}
.panel__title-wrap::before {
  content: "";
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 2px;
  background: var(--xh-teal-600);
}
.panel__title {
  font-family: var(--xh-font-display);
  font-size: var(--xh-text-lg);
  font-weight: 600;
}
.panel__sub {
  margin-top: 2px;
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-400);
}
.panel__extra {
  display: flex;
  align-items: center;
  gap: var(--xh-2);
}
.panel__body {
  padding: var(--xh-5);
}
.panel__body.is-flush {
  padding: 0;
}
</style>
