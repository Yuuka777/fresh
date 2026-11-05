<script setup>
/* ============================================================
   个人中心
   契约：会员信息来自登录响应（/user/member/login）；最近订单复用 /user/order/historyOrders
   ============================================================ */
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { orderApi } from '@/api/user'
import { friendlyTime } from '@/common/format'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'

const user = useUserStore()
const cart = useCartStore()

const recent = ref([])
const loadingRecent = ref(false)

const initial = computed(() => (user.nickname || '鲜').slice(0, 1))
/* 微信登录只拿得到 openid（昵称头像不由前端提交，见接口文档 2.1），
   因此账号标识就用 openid 尾号；未登录时不写「未获取」这类半成品字样，
   改为说明登录能带来什么 */
const openidTail = computed(() => {
  const id = user.openid || ''
  return id ? `…${id.slice(-6)}` : ''
})
const subText = computed(() => {
  if (!user.logged) return '微信登录后可同步订单与地址'
  return openidTail.value ? `微信用户 · ${openidTail.value}` : '微信登录用户'
})

onShow(async () => {
  if (!user.logged) return
  loadingRecent.value = true
  try {
    const data = await orderApi.history({ page: 1, pageSize: 1 })
    recent.value = (data && data.records) || []
  } catch (e) {
    recent.value = []
  } finally {
    loadingRecent.value = false
  }
})

/* 订单与购物车不在这里重复列：下方固定入口里已有「订单」，购物车在选品页的篮子里直接开，
   放在这里点进去只是切了个入口，反而绕路 */
const entries = [
  { key: 'address', label: '收货地址', desc: '管理常用地址', url: '/pages/address/list' }
]

function go(url) {
  if (url === '/pages/order/history') {
    uni.reLaunch({ url })
    return
  }
  uni.navigateTo({ url })
}

function goDetail(order) {
  uni.navigateTo({ url: `/pages/order/detail?id=${order.id}` })
}

async function onLogout() {
  const ok = await new Promise((resolve) =>
    uni.showModal({
      title: '退出登录',
      content: '退出后会清除本机登录态，下次进入需重新授权。',
      confirmText: '退出',
      success: (r) => resolve(r.confirm)
    })
  )
  if (!ok) return
  user.logout()
  cart.items = []
  uni.showToast({ title: '已退出登录', icon: 'none' })
  recent.value = []
}
</script>

<template>
  <view class="page xh-page xh-page--with-tabbar">
    <xh-nav-bar title="我的" :show-back="false" />

    <!-- 会员卡 -->
    <view class="card">
      <view class="card__avatar">{{ initial }}</view>
      <view class="card__text">
        <text class="card__name">{{ user.nickname }}</text>
        <text class="card__sub">{{ subText }}</text>
      </view>
      <view class="card__tag">
        <text>{{ user.logged ? '已登录' : '未登录' }}</text>
      </view>
    </view>

    <!-- 最近订单 -->
    <view class="section">
      <view class="section__head">
        <text class="section__title">最近订单</text>
        <text class="section__more" @tap="go('/pages/order/history')">全部订单 ›</text>
      </view>

      <view v-if="loadingRecent" class="recent-loading"><text>正在读取…</text></view>


      <xh-empty
        v-else-if="!recent.length"
        kind="empty"
        compact
        title="还没有订单"
        desc="下单后可在这里快速复购"
        action-text="去选品"
        @action="() => uni.reLaunch({ url: '/pages/index/index' })"
      />

      <view v-else class="recent" @tap="goDetail(recent[0])">
        <view class="recent__head">
          <text class="recent__no xh-num">{{ recent[0].number }}</text>
          <xh-status-tag :value="recent[0].status" />
        </view>
        <text class="recent__time">{{ friendlyTime(recent[0].orderTime) }}</text>
        <view class="recent__foot">
          <text class="recent__names">
            {{ (recent[0].orderItems || []).map((i) => i.name).slice(0, 2).join('、') }}
          </text>
          <xh-price :value="recent[0].amount" size="md" />
        </view>
      </view>
    </view>

    <!-- 功能入口 -->
    <view class="section">
      <view v-for="(e, i) in entries" :key="e.key" class="entry" :class="{ 'entry--first': i === 0 }" @tap="go(e.url)">
        <view class="entry__text">
          <text class="entry__label">{{ e.label }}</text>
          <text class="entry__desc">{{ e.desc }}</text>
        </view>
        <xh-icon name="arrow-right" :size="30" />
      </view>
    </view>

    <view class="section about">
      <text class="about__title">鲜禾前置仓 · 用户端</text>
      <text class="about__desc">本小程序仅用于本地开发与演示，不对外发布</text>
    </view>

    <view class="section">
      <view class="entry entry--first entry--center" @tap="onLogout">
        <text class="entry__label entry__label--danger">退出登录</text>
      </view>
    </view>

    <view class="page__space" />
    <xh-tabbar current="mine" />
  </view>
</template>

<style lang="scss" scoped>
.page {
  background: $xh-paper;
}
.card {
  margin: $xh-3;
  padding: $xh-5 $xh-4;
  border-radius: $xh-radius;
  background: $xh-teal-800;
  display: flex;
  align-items: center;
  gap: $xh-3;
}
.card__avatar {
  width: 108rpx;
  height: 108rpx;
  border-radius: $xh-radius-sm;
  background: rgba(159, 225, 203, 0.16);
  color: #eaf6f1;
  font-size: $xh-text-2xl;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.card__text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.card__name {
  font-size: $xh-text-xl;
  font-weight: 600;
  color: #f3faf7;
}
.card__sub {
  font-size: $xh-text-xs;
  color: $xh-teal-200;
}
.card__tag {
  padding: 6rpx 14rpx;
  border-radius: $xh-radius-xs;
  background: rgba(255, 255, 255, 0.12);
}
.card__tag text {
  font-size: $xh-text-xs;
  color: #dcefe7;
}

.section {
  margin: 0 $xh-3 $xh-3;
  background: $xh-surface;
  border-radius: $xh-radius;
  border: 2rpx solid $xh-line;
  overflow: hidden;
}
.section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $xh-3 $xh-3 0;
}
.section__title {
  font-size: $xh-text-base;
  font-weight: 600;
  color: $xh-ink-900;
}
.section__more {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}

.recent-loading {
  padding: $xh-5 0;
  text-align: center;
  font-size: $xh-text-sm;
  color: $xh-ink-400;
}
.recent {
  padding: $xh-3;
}
.recent__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.recent__no {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.recent__time {
  display: block;
  margin-top: 6rpx;
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.recent__foot {
  margin-top: $xh-3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $xh-3;
}
.recent__names {
  flex: 1;
  font-size: $xh-text-sm;
  color: $xh-ink-700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $xh-3;
  border-top: 2rpx solid $xh-line-soft;
}
.entry--first {
  border-top: 0;
}
.entry--center {
  justify-content: center;
}
.entry__text {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.entry__label {
  font-size: $xh-text-base;
  color: $xh-ink-900;
}
.entry__label--danger {
  color: $xh-cinnabar-700;
}
.entry__desc {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.entry__arrow {
  font-size: 32rpx;
  color: $xh-ink-300;
}

.about {
  padding: $xh-4;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  align-items: center;
  border-style: dashed;
}
.about__title {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
  letter-spacing: 2rpx;
}
.about__desc {
  font-size: $xh-text-xs;
  color: $xh-ink-300;
}
.page__space {
  height: 40rpx;
}
</style>
