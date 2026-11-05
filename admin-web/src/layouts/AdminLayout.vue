<script setup>
import AppSidebar from '@/components/AppSidebar.vue'
import AppHeader from '@/components/AppHeader.vue'
</script>

<template>
  <div class="shell">
    <AppSidebar />
    <div class="shell__main">
      <AppHeader />
      <main class="shell__content xh-ledger-bg">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  min-height: 100vh;
}
.shell__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.shell__content {
  flex: 1;
  padding: var(--xh-6) var(--xh-8) var(--xh-12);
}
@media (max-width: 1024px) {
  .shell__content {
    padding: var(--xh-5) var(--xh-4) var(--xh-10);
  }
}
</style>
