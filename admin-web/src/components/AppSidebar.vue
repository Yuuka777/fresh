<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const MENUS = [
  { key: 'dashboard', index: '01', label: '工作台', path: '/dashboard' },
  { key: 'statistics', index: '02', label: '数据统计', path: '/statistics' },
  { key: 'order', index: '03', label: '订单管理', path: '/order' },
  { key: 'goods', index: '04', label: '商品管理', path: '/goods' },
  { key: 'goodsCategory', index: '05', label: '商品分类', path: '/goodsCategory' },
  { key: 'bundle', index: '06', label: '组合箱管理', path: '/bundle' },
  { key: 'staff', index: '07', label: '店员管理', path: '/staff' },
  { key: 'profile', index: '08', label: '个人主页', path: '/profile' }
]

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const activeKey = computed(() => route.meta.menu || '')

const go = (path) => {
  if (route.path !== path) router.push(path)
}
</script>

<template>
  <aside class="sidebar" aria-label="主导航">
    <div class="brand">
      <div class="brand__mark">鲜禾</div>
      <div class="brand__sub">
        <span class="brand__latin">XIANHE FRESH</span>
        <span class="brand__role">前置仓运营台</span>
      </div>
    </div>

    <nav class="nav">
      <button
        v-for="m in MENUS"
        :key="m.key"
        type="button"
        class="nav__item"
        :class="{ 'is-active': activeKey === m.key }"
        :aria-current="activeKey === m.key ? 'page' : undefined"
        @click="go(m.path)"
      >
        <span class="nav__index">{{ m.index }}</span>
        <span class="nav__label">{{ m.label }}</span>
      </button>
    </nav>

    <div class="footnote">
      <div class="footnote__line"></div>
      <p class="footnote__text">冷链全程可溯<br />门店直配 30 分钟达</p>
      <p class="footnote__who">
        <span class="footnote__dot" :class="{ 'is-off': user.shopStatus === 0 }"></span>
        {{ user.shopStatus === 0 ? '门店休息中' : '门店营业中' }}
      </p>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: var(--xh-sidebar-w);
  flex: 0 0 var(--xh-sidebar-w);
  height: 100vh;
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  background: #0a241d;
  color: #dbe7e1;
  background-image: linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 220px);
}

/* —— 品牌区：宋体大字 + 等宽小字，拉开层级 —— */
.brand {
  padding: var(--xh-6) var(--xh-5) var(--xh-5);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.brand__mark {
  font-family: var(--xh-font-display);
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: #f2f7f4;
}
.brand__sub {
  margin-top: var(--xh-2);
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.brand__latin {
  font-family: var(--xh-font-mono);
  font-size: 10px;
  letter-spacing: 0.22em;
  color: #6f8d81;
}
.brand__role {
  font-size: var(--xh-text-xs);
  color: #9db4aa;
  letter-spacing: 0.06em;
}

.nav {
  padding: var(--xh-4) 0;
  display: flex;
  flex-direction: column;
}
.nav__item {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--xh-3);
  width: 100%;
  min-height: 44px;
  padding: 0 var(--xh-5);
  border: 0;
  background: transparent;
  color: #b9cdc5;
  font-size: var(--xh-text-base);
  text-align: left;
  cursor: pointer;
  transition: color var(--xh-dur-fast) var(--xh-ease-out), background-color var(--xh-dur-fast) var(--xh-ease-out);
}
.nav__item::before {
  content: "";
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: var(--xh-teal-200);
  transform: scaleY(0);
  transition: transform var(--xh-dur-fast) var(--xh-ease-out);
}
.nav__item:hover {
  color: #eef5f1;
  background: rgba(255, 255, 255, 0.04);
}
.nav__item.is-active {
  color: #ffffff;
  background: rgba(159, 225, 203, 0.1);
}
.nav__item.is-active::before {
  transform: scaleY(1);
}
.nav__index {
  font-family: var(--xh-font-mono);
  font-size: 11px;
  color: #7c9c8f;
  letter-spacing: 0.04em;
}
.nav__item.is-active .nav__index {
  color: var(--xh-teal-200);
}
.nav__label {
  letter-spacing: 0.04em;
}

.footnote {
  margin-top: auto;
  padding: var(--xh-5);
}
.footnote__line {
  height: 1px;
  background: rgba(255, 255, 255, 0.08);
  margin-bottom: var(--xh-4);
}
.footnote__text {
  font-size: var(--xh-text-xs);
  line-height: 1.8;
  color: #7d9a8e;
}
.footnote__who {
  margin-top: var(--xh-3);
  display: flex;
  align-items: center;
  gap: var(--xh-2);
  font-size: var(--xh-text-xs);
  color: #a9c0b6;
}
.footnote__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--xh-teal-200);
}
.footnote__dot.is-off {
  background: var(--xh-cinnabar-600);
}
</style>
