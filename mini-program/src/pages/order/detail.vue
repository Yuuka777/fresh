<script setup>
/* ============================================================
   订单详情
   契约：/user/order/orderDetail/{id}、/user/order/cancel/{id}
         /user/order/reminder/{id}、/user/order/repetition/{id}
   ============================================================ */
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { orderApi } from '@/api/user'
import { ORDER_STATUS, ORDER_STATUS_MAP, canCancelOrder, canRemindOrder } from '@/common/dict'
import { countdown, money, shortTime } from '@/common/format'
import { ORDER_PAY_TIMEOUT_MINUTES } from '@/common/config'
import { useUserStore } from '@/store/user'

const user = useUserStore()

const orderId = ref(0)
const order = ref(null)
const loading = ref(true)
const ticking = ref('')

let timer = null

const statusMap = computed(() => ORDER_STATUS_MAP[order.value ? order.value.status : 0] || {})
const isUnpaid = computed(() => order.value && order.value.status === ORDER_STATUS.TO_BE_PAID)

/* 待付款倒计时：以下单时间 + 15 分钟为截止（与服务端定时任务口径一致） */
const deadline = computed(() => {
  if (!order.value || !order.value.orderTime) return null
  const t = new Date(String(order.value.orderTime).replace(/-/g, '/')).getTime()
  return new Date(t + ORDER_PAY_TIMEOUT_MINUTES * 60000)
})

const timeline = computed(() => {
  if (!order.value) return []
  return [
    { label: '下单', time: order.value.orderTime },
    { label: '支付', time: order.value.checkoutTime },
    { label: '预计送达', time: order.value.estimatedDeliveryTime },
    { label: '送达', time: order.value.deliveryTime },
    { label: '取消', time: order.value.cancelTime }
  ].filter((i) => i.time)
})

onLoad(async (options) => {
  orderId.value = Number(options.id || 0)
  await load()
})

function startTick() {
  stopTick()
  if (!isUnpaid.value) return
  timer = setInterval(() => {
    const c = countdown(deadline.value)
    ticking.value = c.text
    if (c.expired) {
      stopTick()
      /* 超时由服务端定时任务取消，这里只刷新视图 */
      load()
    }
  }, 1000)
}

function stopTick() {
  if (timer) clearInterval(timer)
  timer = null
}

async function load() {
  loading.value = true
  try {
    await user.ensureLogin()
    const data = await orderApi.detail(orderId.value)
    order.value = data
    ticking.value = countdown(deadline.value).text
    startTick()
  } catch (e) {
  } finally {
    loading.value = false
  }
}

function goPay() {
  uni.navigateTo({
    url: `/pages/order/pay?orderNumber=${order.value.number}&amount=${order.value.amount}&orderTime=${encodeURIComponent(
      order.value.orderTime || ''
    )}`
  })
}

async function onCancel() {
  const ok = await new Promise((resolve) =>
    uni.showModal({
      title: '取消订单',
      content: '取消后不可恢复，已支付金额将原路退回。',
      confirmText: '确认取消',
      cancelText: '再想想',
      success: (r) => resolve(r.confirm)
    })
  )
  if (!ok) return
  try {
    await orderApi.cancel(orderId.value)
    uni.showToast({ title: '订单已取消', icon: 'none' })
    load()
  } catch {
    /* 请求层已提示 */
  }
}

async function onRemind() {
  try {
    await orderApi.reminder(orderId.value)
    uni.showToast({ title: '已提醒门店', icon: 'none' })
  } catch {
    /* 请求层已提示 */
  }
}

async function onRepeat() {
  try {
    await orderApi.repetition(orderId.value)
    uni.showToast({ title: '已加入购物车', icon: 'none' })
    setTimeout(() => uni.redirectTo({ url: '/pages/order/confirm' }), 600)
  } catch {
    /* 请求层已提示 */
  }
}

function copyNumber() {
  uni.setClipboardData({ data: order.value.number })
}

function goBack() {
  uni.navigateBack()
}
</script>

<template>
  <view class="page xh-page">
    <xh-nav-bar title="订单详情" />

    <view v-if="loading" class="loading"><text>正在读取订单…</text></view>

    <xh-empty
      v-else-if="!order"
      kind="empty"
      title="暂时没有订单信息"
      action-text="返回"
      @action="goBack"
    />

    <view v-else class="body">
      <!-- 状态头 -->
      <view class="status">
        <text class="status__label">{{ statusMap.label }}</text>
        <text class="status__desc">{{ statusMap.desc }}</text>
        <view v-if="isUnpaid" class="status__countdown">
          <text>支付剩余</text>
          <text class="xh-num status__clock">{{ ticking }}</text>
        </view>
      </view>

      <!-- 收货信息 -->
      <view class="block xh-card">
        <view class="row">
          <text class="row__label">收货人</text>
          <text class="row__value">{{ order.consignee }} · {{ order.phone }}</text>
        </view>
        <view class="row">
          <text class="row__label">送达地址</text>
          <text class="row__value row__value--wrap">{{ order.address }}</text>
        </view>
        <view class="row">
          <text class="row__label">配送时效</text>
          <text class="row__value">{{ order.deliveryStatus === 1 ? '尽快送达' : '预约时段' }}</text>
        </view>
        <view v-if="order.remark" class="row">
          <text class="row__label">订单备注</text>
          <text class="row__value row__value--wrap">{{ order.remark }}</text>
        </view>
      </view>

      <!-- 时间轴 -->
      <view class="block xh-card">
        <text class="block__title">时间节点</text>
        <view class="tl">
          <view v-for="(item, i) in timeline" :key="i" class="tl__item">
            <view class="tl__dot" />
            <text class="tl__label">{{ item.label }}</text>
            <text class="tl__time xh-num">{{ shortTime(item.time) }}</text>
          </view>
        </view>
      </view>

      <!-- 明细 -->
      <view class="block xh-card">
        <text class="block__title">商品明细</text>
        <view v-for="(item, i) in order.orderItems" :key="i" class="line">
          <xh-goods-thumb :name="item.name" :image="item.image" :size="96" :show-zone="false" />
          <view class="line__text">
            <text class="line__name">{{ item.name }}</text>
            <text class="line__spec">{{ item.specText || '标准规格' }}</text>
          </view>
          <view class="line__right">
            <xh-price :value="item.amount" size="sm" />
            <text class="line__num xh-num">×{{ item.number }}</text>
          </view>
        </view>
      </view>

      <!-- 金额 -->
      <view class="block xh-card">
        <view class="row">
          <text class="row__label">商品金额</text>
          <text class="xh-num row__num">¥{{ money(order.amount - order.deliveryFee) }}</text>
        </view>
        <view class="row">
          <text class="row__label">运送费</text>
          <text class="xh-num row__num">¥{{ money(order.deliveryFee) }}</text>
        </view>
        <view class="xh-hairline" />
        <view class="row row--total">
          <text class="row__label">实付金额</text>
          <xh-price :value="order.amount" size="xl" />
        </view>
      </view>

      <!-- 订单信息 -->
      <view class="block xh-card">
        <view class="row" @tap="copyNumber">
          <text class="row__label">订单号</text>
          <text class="row__value xh-num">{{ order.number }}</text>
          <text class="row__copy">复制</text>
        </view>
        <view v-if="order.cancelReason" class="row">
          <text class="row__label">取消原因</text>
          <text class="row__value row__value--wrap">{{ order.cancelReason }}</text>
        </view>
        <view v-if="order.rejectionReason" class="row">
          <text class="row__label">拒单原因</text>
          <text class="row__value row__value--wrap">{{ order.rejectionReason }}</text>
        </view>
      </view>

      <view class="body__space" />
    </view>

    <!-- 操作条 -->
    <view v-if="order" class="bar">
      <view v-if="order.status === 1" class="bar__btn bar__btn--primary" hover-class="bar__btn--hover" @tap="goPay">
        <text>去支付</text>
      </view>
      <view v-if="canCancelOrder(order.status)" class="bar__btn" hover-class="bar__btn--hover" @tap="onCancel">
        <text>取消订单</text>
      </view>
      <view v-if="canRemindOrder(order.status)" class="bar__btn bar__btn--primary" hover-class="bar__btn--hover" @tap="onRemind">
        <text>催单</text>
      </view>
      <view
        v-if="order.status !== 7"
        class="bar__btn"
        hover-class="bar__btn--hover"
        @tap="onRepeat"
      >
        <text>再来一单</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page {
  background: $xh-paper;
  padding-bottom: 160rpx;
}
.loading {
  padding: $xh-10 0;
  text-align: center;
  font-size: $xh-text-sm;
  color: $xh-ink-400;
}
.body {
  padding: $xh-3;
}
.body__space {
  height: 40rpx;
}

.status {
  padding: $xh-5 $xh-4;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}
.status__label {
  font-size: $xh-text-2xl;
  font-weight: 700;
  color: $xh-ink-900;
  letter-spacing: 2rpx;
}
.status__desc {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
}
.status__countdown {
  margin-top: $xh-2;
  display: flex;
  align-items: center;
  gap: $xh-2;
  font-size: $xh-text-sm;
  color: $xh-cinnabar-700;
}
.status__clock {
  font-size: $xh-text-lg;
  font-weight: 600;
}

.block {
  padding: $xh-3;
  margin-bottom: $xh-3;
}
.block__title {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
  letter-spacing: 1rpx;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $xh-2 0;
}
.row__label {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
  flex-shrink: 0;
}
.row__value {
  flex: 1;
  margin-left: $xh-3;
  text-align: right;
  font-size: $xh-text-sm;
  color: $xh-ink-900;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row__value--wrap {
  white-space: normal;
  line-height: 1.5;
}
.row__copy {
  margin-left: $xh-2;
  padding: 0 12rpx;
  height: 40rpx;
  line-height: 40rpx;
  border-radius: $xh-radius-xs;
  border: 2rpx solid $xh-line;
  font-size: $xh-text-xs;
  color: $xh-teal-800;
}
.row__num {
  font-size: $xh-text-sm;
  color: $xh-ink-900;
}
.row--total {
  padding-top: $xh-3;
}

.tl {
  margin-top: $xh-3;
  padding-left: 6rpx;
}
.tl__item {
  position: relative;
  display: flex;
  align-items: center;
  gap: $xh-3;
  padding: $xh-2 0 $xh-2 $xh-4;
  border-left: 2rpx solid $xh-line;
}
.tl__dot {
  position: absolute;
  left: -7rpx;
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: $xh-teal-600;
}
.tl__label {
  font-size: $xh-text-sm;
  color: $xh-ink-700;
  min-width: 120rpx;
}
.tl__time {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}

.line {
  display: flex;
  align-items: center;
  gap: $xh-3;
  padding: $xh-3 0;
}
.line + .line {
  border-top: 2rpx solid $xh-line-soft;
}
.line__text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.line__name {
  font-size: $xh-text-sm;
  color: $xh-ink-900;
}
.line__spec {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.line__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6rpx;
}
.line__num {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}

.bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: $xh-3 $xh-4 calc(#{$xh-3} + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: $xh-2;
  background: $xh-surface;
  border-top: 2rpx solid $xh-line;
}
.bar__btn {
  height: 96rpx;
  min-width: 180rpx;
  padding: 0 $xh-4;
  border-radius: $xh-radius;
  border: 2rpx solid $xh-line;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $xh-surface;
}
.bar__btn text {
  font-size: $xh-text-sm;
  color: $xh-ink-700;
}
.bar__btn--primary {
  background: $xh-teal-600;
  border-color: $xh-teal-600;
}
.bar__btn--primary text {
  color: #ffffff;
  font-weight: 600;
}
.bar__btn--hover {
  opacity: 0.85;
}
</style>
