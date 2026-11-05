<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="nf">
    <p class="nf__code xh-figure">404</p>
    <h1 class="nf__title">这条台账不存在</h1>
    <p class="nf__text">页面可能已被移动或从未存在，请从工作台重新进入。</p>
    <button type="button" class="nf__btn" @click="router.replace('/dashboard')">回到工作台</button>
  </div>
</template>

<style scoped>
.nf {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--xh-3);
  text-align: center;
  padding: var(--xh-8);
}
.nf__code {
  font-size: 88px;
  line-height: 1;
  color: var(--xh-teal-800);
  opacity: 0.35;
}
.nf__title {
  font-family: var(--xh-font-display);
  font-size: var(--xh-text-xl);
}
.nf__text {
  font-size: var(--xh-text-sm);
  color: var(--xh-ink-400);
}
.nf__btn {
  margin-top: var(--xh-4);
  min-height: 40px;
  padding: 0 var(--xh-5);
  border: 1px solid var(--xh-teal-800);
  border-radius: var(--xh-radius-sm);
  background: var(--xh-teal-800);
  color: #f2f9f6;
  font-size: var(--xh-text-sm);
  cursor: pointer;
}
.nf__btn:hover {
  background: var(--xh-teal-600);
  border-color: var(--xh-teal-600);
}
</style>
