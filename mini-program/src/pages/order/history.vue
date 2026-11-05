<script setup>
/* ============================================================
   历史订单
   契约：/user/order/historyOrders（分页 + 状态筛选）、/user/order/repetition/{id}
         /user/order/cancel/{id}、/user/order/reminder/{id}
   ============================================================ */
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { orderApi } from '@/api/user'
import { ORDER_TABS, canCancelOrder, canRemindOrder } from '@/common/dict'
import { friendlyTime, money } from '@/common/format'
import { DEFAULT_PAGE_SIZE } from '@/common/config'
import { useUserStore } from '@/store/user'

const user = useUserStore()

const activeTab = ref('all')
const list = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const noMore = computed(() => list.value.length >= total.value && total.value > 0)

const currentStatus = computed(() => {
  const tab = ORDER_TABS.find((t) => t.key === activeTab.value)
  return tab ? tab.status : undefined
})

onLoad(async () => {
  await init()
})

onShow(() => {
  /* 从支付/详情返回时刷新首屏，保证状态是最新的 */
  if (list.value.length) reload()
})

onPullDownRefresh(async () => {
  await reload()
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (!loading.value && !noMore.value) loadMore()
})

async function init() {
  try {
    await user.ensureLogin()
    await reload()
  } catch (e) {
  }
}

async function reload() {
  page.value = 1
  const items = await fetchPage(1)
  list.value = items
}

async function loadMore() {
  const items = await fetchPage(page.value + 1)
  list.value = list.value.concat(items)
}

async function fetchPage(p) {
  loading.value = true
  try {
    const params = { page: p, pageSize: DEFAULT_PAGE_SIZE }
    if (currentStatus.value !== undefined) params.status = currentStatus.value
    const data = await orderApi.history(params)
    page.value = p
    total.value = (data && data.total) || 0
    const records = (data && data.records) || []
    return records
  } catch (e) {
    return []
  } finally {
    loading.value = false
  }
}

function switchTab(key) {
  if (activeTab.value === key) return
  activeTab.value = key
  list.value = []
  total.value = 0
  reload()
}

/* 明细摘要：最多显示两行，其余折叠为「等 N 件」
   这里要给模板用的**对象**（模板取的是 item.name / item.image / item.specText / item.number），
   不能拼成字符串 —— 拼过一版，结果卡片上商品名空白、数量只剩一个「×」 */
function summary(order) {
  const items = order.orderItems || []
  const head = items.slice(0, 2)
  const rest = items.length - head.length
  return { head, rest: rest > 0 ? `等 ${items.length} 件` : '' }
}

function goDetail(order) {
  uni.navigateTo({ url: `/pages/order/detail?id=${order.id}` })
}

function goPay(order) {
  uni.navigateTo({
    url: `/pages/order/pay?orderNumber=${order.number}&amount=${order.amount}&orderTime=${encodeURIComponent(
      order.orderTime || ''
    )}`
  })
}

async function onCancel(order) {
  const res = await new Promise((resolve) =>
    uni.showModal({
      title: '取消订单',
      content: '取消后不可恢复，已支付金额将原路退回。',
      confirmText: '确认取消',
      cancelText: '再想想',
      success: (r) => resolve(r.confirm)
    })
  )
  if (!res) return
  try {
    await orderApi.cancel(order.id)
    uni.showToast({ title: '订单已取消', icon: 'none' })
    reload()
  } catch {
    /* 请求层已提示 */
  }
}

async function onRemind(order) {
  try {
    await orderApi.reminder(order.id)
    uni.showToast({ title: '已提醒门店', icon: 'none' })
  } catch {
    /* 请求层已提示 */
  }
}

async function onRepeat(order) {
  try {
    await orderApi.repetition(order.id)
    uni.showToast({ title: '已加入购物车', icon: 'none' })
    setTimeout(() => uni.navigateTo({ url: '/pages/order/confirm' }), 600)
  } catch {
    /* 请求层已提示 */
  }
}

function goIndex() {
  uni.reLaunch({ url: '/pages/index/index' })
}
</script>

<template>
  <view class="page xh-page xh-page--with-tabbar">
    <xh-nav-bar title="我的订单" :show-back="false" />

    <!-- 状态页签 -->
    <scroll-view class="tabs" scroll-x :show-scrollbar="false">
      <view class="tabs__row">
        <view
          v-for="tab in ORDER_TABS"
          :key="tab.key"
          class="tabs__item"
          :class="{ 'tabs__item--active': activeTab === tab.key }"
          @tap="switchTab(tab.key)"
        >
          <text>{{ tab.label }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- 列表 -->
    <view class="list">
      <view v-if="loading && !list.length" class="loading">
        <text>正在读取订单…</text>
      </view>


      <xh-empty
        v-else-if="!list.length"
        kind="empty"
        title="还没有订单"
        desc="选几样当日鲜货，30 分钟就能送到家门口"
        action-text="去选品"
        @action="goIndex"
      />

      <view v-else class="orders">
        <view v-for="order in list" :key="order.id" class="order xh-card" @tap="goDetail(order)">
          <view class="order__head">
            <text class="order__no xh-num">{{ order.number }}</text>
            <xh-status-tag :value="order.status" />
          </view>

          <view class="order__time">
            <text>{{ friendlyTime(order.orderTime) }}</text>
            <text v-if="order.deliveryStatus === 1" class="order__fast">尽快送达</text>
          </view>

          <view class="order__items">
            <view v-for="(item, i) in summary(order).head" :key="i" class="order__item">
              <xh-goods-thumb :name="item.name" :image="item.image" :size="88" :show-zone="false" />
              <view class="order__item-text">
                <text class="order__item-name">{{ item.name }}</text>
                <text class="order__item-spec">{{ item.specText || '标准规格' }}</text>
              </view>
              <text class="order__item-num xh-num">×{{ item.number }}</text>
            </view>
            <text v-if="summary(order).rest" class="order__more">{{ summary(order).rest }}</text>
          </view>

          <view class="order__foot">
            <view class="order__amount">
              <text class="order__amount-label">实付</text>
              <xh-price :value="order.amount" size="lg" />
            </view>
            <view class="order__ops" @tap.stop>
              <view
                v-if="order.status === 1"
                class="op op--primary"
                hover-class="op--hover"
                @tap="goPay(order)"
              >
                <text>去支付</text>
              </view>
              <view
                v-if="canCancelOrder(order.status)"
                class="op"
                hover-class="op--hover"
                @tap="onCancel(order)"
              >
                <text>取消订单</text>
              </view>
              <view
                v-if="canRemindOrder(order.status)"
                class="op op--primary"
                hover-class="op--hover"
                @tap="onRemind(order)"
              >
                <text>催单</text>
              </view>
              <view
                v-if="order.status !== 7"
                class="op"
                hover-class="op--hover"
                @tap="onRepeat(order)"
              >
                <text>再来一单</text>
              </view>
            </view>
          </view>
        </view>

        <view class="list__end">
          <text v-if="loading">加载中…</text>
          <text v-else-if="noMore">就这些了</text>
          <text v-else>上拉加载更多</text>
        </view>
      </view>
    </view>

    <xh-tabbar current="history" />
  </view>
</template>

<style lang="scss" scoped>
.page {
  background: $xh-paper;
}
.tabs {
  background: $xh-surface;
  border-bottom: 2rpx solid $xh-line-soft;
}
.tabs__row {
  display: flex;
  padding: 0 $xh-2;
}
.tabs__item {
  padding: $xh-3 $xh-3;
  position: relative;
}
.tabs__item text {
  font-size: $xh-text-sm;
  color: $xh-ink-400;
}
.tabs__item--active text {
  color: $xh-ink-900;
  font-weight: 600;
}
.tabs__item--active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 8rpx;
  transform: translateX(-50%);
  width: 40rpx;
  height: 6rpx;
  border-radius: 3rpx;
  background: $xh-teal-600;
}

.list {
  padding: $xh-3;
}
.loading {
  padding: $xh-10 0;
  text-align: center;
  font-size: $xh-text-sm;
  color: $xh-ink-400;
}

.order {
  padding: $xh-3;
  margin-bottom: $xh-3;
}
.order__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.order__no {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.order__time {
  margin-top: 6rpx;
  display: flex;
  align-items: center;
  gap: $xh-2;
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.order__fast {
  padding: 0 8rpx;
  border-radius: $xh-radius-xs;
  background: $xh-teal-50;
  color: $xh-teal-800;
}
.order__items {
  margin-top: $xh-3;
}
.order__item {
  display: flex;
  align-items: center;
  gap: $xh-3;
  padding: $xh-2 0;
}
.order__item-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.order__item-name {
  font-size: $xh-text-sm;
  color: $xh-ink-900;
}
.order__item-spec {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.order__item-num {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
}
.order__more {
  display: block;
  padding-top: $xh-1;
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.order__foot {
  margin-top: $xh-3;
  padding-top: $xh-3;
  border-top: 2rpx solid $xh-line-soft;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.order__amount {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}
.order__amount-label {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.order__ops {
  display: flex;
  align-items: center;
  gap: $xh-2;
}
.op {
  height: 96rpx;
  padding: 0 $xh-3;
  border-radius: $xh-radius;
  border: 2rpx solid $xh-line;
  display: flex;
  align-items: center;
  background: $xh-surface;
}
.op text {
  font-size: $xh-text-sm;
  color: $xh-ink-700;
}
.op--primary {
  border-color: $xh-teal-600;
  background: $xh-teal-600;
}
.op--primary text {
  color: #ffffff;
}
.op--hover {
  opacity: 0.85;
}
.list__end {
  padding: $xh-4 0 $xh-6;
  text-align: center;
  font-size: $xh-text-xs;
  color: $xh-ink-300;
}
</style>
