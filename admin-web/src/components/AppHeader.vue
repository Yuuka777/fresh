<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useOrderStatsStore } from '@/stores/orderStats'
import { getCookie } from '@/utils/cookie'
import { playNewOrderSound, playReminderSound } from '@/utils/notify'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const stats = useOrderStatsStore()
const now = ref(formatNow())
let clockTimer = null
let statTimer = null

function formatNow() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/* 新增与编辑共用同一路由，标题按是否带 id 切换（否则编辑商品时顶栏还写着「新增商品」） */
const title = computed(() => {
  if (route.query.id && route.meta.editTitle) return route.meta.editTitle
  return route.meta.title || '运营台'
})
const pendingTotal = computed(() => stats.toBePaid + stats.toBeConfirmed)

async function loadStats() {
  /* 角标走共享 store：顶栏与订单页页签一起刷新（见 stores/orderStats.js） */
  await stats.load()
  try {
    await user.fetchShopStatus()
  } catch {
    /* 营业状态取不到不影响主流程 */
  }
}

function gotoOrder(tabKey) {
  router.push({ path: '/order', query: { tab: tabKey } })
}

async function onToggleShop(value) {
  const next = value ? 1 : 0
  try {
    await ElMessageBox.confirm(
      next === 1 ? '确认将门店状态置为「营业中」？' : '确认将门店状态置为「休息中」？休息后用户端将无法下单。',
      '门店营业状态',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: next === 1 ? 'info' : 'warning' }
    )
  } catch {
    return
  }
  try {
    await user.setShopStatus(next)
    ElMessage.success(next === 1 ? '门店已开始营业' : '门店已置为休息中')
  } catch {
    loadStats()
  }
}

function onCommand(cmd) {
  if (cmd === 'profile') router.push('/profile')
  if (cmd === 'logout') doLogout()
  if (cmd === 'chime-order') {
    playNewOrderSound()
    ElMessage.success('已试听：来单提醒')
  }
  if (cmd === 'chime-remind') {
    playReminderSound()
    ElMessage.success('已试听：催单提醒')
  }
}

async function doLogout() {
  try {
    await ElMessageBox.confirm('退出后需要重新登录，确认退出？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await user.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}

/* ---------------- WebSocket 来单 / 催单提醒（《接口文档》第 10 节） ----------------
   服务端只在「支付成功」与「客户催单」两处推送；收到就响铃、弹通知、刷新角标。
   sid 由前端生成并持久化，刷新页面沿用同一个，避免服务端会话表里堆一堆死连接。 */
const SID_KEY = 'xh_ws_sid'
let socket = null

function ensureSid() {
  let sid = localStorage.getItem(SID_KEY)
  if (!sid) {
    sid = Math.random().toString(36).slice(2, 12)
    localStorage.setItem(SID_KEY, sid)
  }
  return sid
}

function wsUrl() {
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
  // 浏览器无法给 WebSocket 加自定义请求头，令牌只能走 query，服务端 onOpen 验签
  return `${proto}//${location.host}/ws/${ensureSid()}?token=${encodeURIComponent(getCookie('token'))}`
}

function onSocketMessage(event) {
  let msg = null
  try {
    msg = JSON.parse(event.data)
  } catch {
    // 收到非 JSON 的消息说明服务端或代理出了问题，不弹给使用者
    return
  }

  if (msg.type === 1) {
    playNewOrderSound()
    /* 通知是常驻的（duration: 0），而订单详情抽屉从右侧滑出，正好被它压住关闭按钮；
       点过通知就说明已经去看这一单了，顺手关掉，不留遮挡 */
    const tip = ElNotification({
      title: '来单提醒',
      message: msg.content,
      duration: 0,
      onClick: () => {
        tip.close()
        router.push({ path: '/order', query: { orderId: msg.orderId } })
      }
    })
  } else if (msg.type === 2) {
    playReminderSound()
    const tip = ElNotification({
      title: '客户催单',
      message: msg.content,
      type: 'warning',
      duration: 0,
      onClick: () => {
        tip.close()
        router.push({ path: '/order', query: { orderId: msg.orderId } })
      }
    })
  }
  // 订单状态变了，角标要跟着变（契约 2.8.2 的四个数字）
  loadStats()
}

function connectSocket() {
  if (typeof WebSocket === 'undefined') return
  socket = new WebSocket(wsUrl())
  socket.onmessage = onSocketMessage
  socket.onerror = () => {
    // 连不上只是收不到实时提醒，页面其余功能不受影响，交给控制台留痕即可
    console.warn('WebSocket 连接异常，来单提醒暂不可用')
  }
}

onMounted(() => {
  loadStats()
  clockTimer = setInterval(() => (now.value = formatNow()), 30000)
  statTimer = setInterval(loadStats, 60000)
  connectSocket()
})
onBeforeUnmount(() => {
  clearInterval(clockTimer)
  clearInterval(statTimer)
  if (socket) socket.close()
})
</script>

<template>
  <header class="topbar">
    <div class="topbar__left">
      <p class="topbar__crumb">
        <span class="topbar__crumb-root">鲜禾</span>
        <span class="topbar__crumb-sep">/</span>
        <span class="topbar__crumb-now">{{ title }}</span>
      </p>
      <h1 class="topbar__title">{{ title }}</h1>
    </div>

    <div class="topbar__right">
      <div class="topbar__clock xh-num">{{ now }}</div>

      <div class="pending" role="group" aria-label="待处理订单">
        <button
          type="button"
          class="pending__item"
          :class="{ 'is-hot': stats.toBePaid > 0 }"
          @click="gotoOrder('paid')"
        >
          <span class="pending__label">待付款</span>
          <span class="pending__num xh-num">{{ stats.toBePaid }}</span>
        </button>
        <button
          type="button"
          class="pending__item"
          :class="{ 'is-hot': pendingTotal > 0 }"
          @click="gotoOrder('confirm')"
        >
          <span class="pending__label">待接单</span>
          <span class="pending__num xh-num">{{ stats.toBeConfirmed }}</span>
        </button>
      </div>

      <div class="shop-switch">
        <span class="shop-switch__text">{{ user.shopStatus === 1 ? '营业中' : '休息中' }}</span>
        <el-switch
          :model-value="user.shopStatus === 1"
          size="small"
          aria-label="切换门店营业状态"
          @change="onToggleShop"
        />
      </div>

      <el-dropdown trigger="click" @command="onCommand">
        <button type="button" class="who" aria-label="账号菜单">
          <span class="who__avatar">{{ user.displayName.slice(0, 1) }}</span>
          <span class="who__name">{{ user.displayName }}</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人主页</el-dropdown-item>
            <el-dropdown-item command="chime-order" divided>试听来单提示音</el-dropdown-item>
            <el-dropdown-item command="chime-remind">试听催单提示音</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  height: var(--xh-header-h);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--xh-6);
  padding: 0 var(--xh-8) 0 var(--xh-6);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(6px);
  border-bottom: 1px solid var(--xh-line);
  position: sticky;
  top: 0;
  z-index: 20;
}

.topbar__crumb {
  display: flex;
  align-items: center;
  gap: var(--xh-2);
  font-family: var(--xh-font-mono);
  font-size: 10px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--xh-ink-300);
}
.topbar__crumb-root {
  color: var(--xh-teal-600);
}
.topbar__title {
  font-family: var(--xh-font-display);
  font-size: var(--xh-text-xl);
  font-weight: 600;
  letter-spacing: -0.01em;
  line-height: 1.2;
}

.topbar__right {
  display: flex;
  align-items: center;
  gap: var(--xh-5);
}

.topbar__clock {
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-400);
  letter-spacing: 0.02em;
}

.pending {
  display: flex;
  border: 1px solid var(--xh-line);
  border-radius: var(--xh-radius-sm);
  overflow: hidden;
}
.pending__item {
  display: flex;
  align-items: center;
  gap: var(--xh-2);
  height: 34px;
  padding: 0 var(--xh-3);
  border: 0;
  background: var(--xh-surface);
  color: var(--xh-ink-500);
  font-size: var(--xh-text-xs);
  cursor: pointer;
  transition: background-color var(--xh-dur-fast) var(--xh-ease-out);
}
.pending__item + .pending__item {
  border-left: 1px solid var(--xh-line);
}
.pending__item:hover {
  background: var(--xh-teal-50);
  color: var(--xh-teal-800);
}
.pending__num {
  font-size: var(--xh-text-sm);
  font-weight: 600;
  color: var(--xh-ink-900);
}
.pending__item.is-hot .pending__num {
  color: var(--xh-cinnabar-600);
}

.shop-switch {
  display: flex;
  align-items: center;
  gap: var(--xh-2);
}
.shop-switch__text {
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-500);
}

.who {
  display: flex;
  align-items: center;
  gap: var(--xh-2);
  height: 36px;
  padding: 0 var(--xh-3) 0 var(--xh-1);
  border: 1px solid var(--xh-line);
  border-radius: var(--xh-radius-sm);
  background: var(--xh-surface);
  cursor: pointer;
  transition: border-color var(--xh-dur-fast) var(--xh-ease-out);
}
.who:hover {
  border-color: var(--xh-line-strong);
}
.who__avatar {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: var(--xh-radius-xs);
  background: var(--xh-teal-800);
  color: #eaf6f1;
  font-family: var(--xh-font-display);
  font-size: var(--xh-text-sm);
}
.who__name {
  font-size: var(--xh-text-sm);
  color: var(--xh-ink-700);
}
</style>
