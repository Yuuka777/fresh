<script setup>
/* ============================================================
   确认订单
   契约：/user/cart/list、/user/address/default、/user/order/submit
   注意：提交订单只创建订单（status = 1 待付款），支付是下一步的独立动作
   ============================================================ */
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { addressApi, orderApi } from '@/api/user'
import { DELIVERY_FEE, STORAGE_KEYS } from '@/common/config'
import { money, toServerTime } from '@/common/format'
import { useCartStore } from '@/store/cart'
import { useUserStore } from '@/store/user'

const cart = useCartStore()
const user = useUserStore()

const address = ref(null)
const addressError = ref('')
const remark = ref('')
const deliveryStatus = ref(1) // 1 尽快送达 / 0 预约时段
const appointIndex = ref(1)
const submitting = ref(false)
const loading = ref(true)

/* 预约时段候选：从下一个整点起，每 30 分钟一档，共 12 档 */
const appointSlots = computed(() => {
  const slots = []
  const base = new Date()
  base.setMinutes(base.getMinutes() + 60, 0, 0)
  for (let i = 0; i < 12; i++) {
    const d = new Date(base.getTime() + i * 30 * 60000)
    slots.push({
      label: `${d.getHours() < 10 ? '0' : ''}${d.getHours()}:${d.getMinutes() < 10 ? '0' : ''}${
        d.getMinutes() === 0 ? '0' : ''
      }`,
      value: toServerTime(d)
    })
  }
  return slots
})

const goodsAmount = computed(() => cart.amount)
const totalAmount = computed(() => Number((goodsAmount.value + DELIVERY_FEE).toFixed(2)))
const estimatedDeliveryTime = computed(() => {
  if (deliveryStatus.value === 1) {
    return toServerTime(new Date(Date.now() + 30 * 60000))
  }
  const slot = appointSlots.value[appointIndex.value]
  return slot ? slot.value : toServerTime(new Date(Date.now() + 30 * 60000))
})

onShow(async () => {
  /* 备注页回填 */
  const cached = uni.getStorageSync('xh_order_remark')
  if (cached) remark.value = cached

  try {
    await user.ensureLogin()
    await cart.load(true)
    await loadAddress()
  } catch (e) {
  } finally {
    loading.value = false
  }
})

async function loadAddress() {
  addressError.value = ''

  /* 从地址列表选择返回时优先使用所选地址（接口文档 8.3） */
  const pickedId = uni.getStorageSync('xh_selected_address_id')
  if (pickedId) {
    uni.removeStorageSync('xh_selected_address_id')
    try {
      address.value = await addressApi.detail(pickedId)
      return
    } catch {
      /* 读取失败则回落到默认地址 */
    }
  }

  try {
    const data = await addressApi.getDefault()
    address.value = data || null
    if (!address.value) {
      addressError.value = '还没有收货地址，请先添加'
    }
  } catch (e) {
    /* 无默认地址时服务端返回 msg = "没有查询到默认地址"，属正常分支 */
    address.value = null
    addressError.value = '还没有收货地址，请先添加'
  }
}

function goAddressList() {
  uni.navigateTo({ url: '/pages/address/list?mode=select' })
}

function goAddressEdit() {
  const id = address.value && address.value.id
  uni.navigateTo({ url: id ? `/pages/address/edit?id=${id}` : '/pages/address/edit' })
}

function goRemark() {
  uni.navigateTo({ url: '/pages/order/remark' })
}

function pickAppoint(e) {
  appointIndex.value = Number(e.detail.value)
}

function buildOrderItems() {
  return cart.items.map((i) => ({
    goodsId: i.goodsId || null,
    bundleId: i.bundleId || null,
    name: i.name,
    image: i.image || null,
    specText: i.specText || null,
    number: i.number,
    amount: i.amount
  }))
}

async function onSubmit() {
  if (submitting.value) return

  if (cart.empty) {
    uni.showToast({ title: '购物车为空', icon: 'none' })
    return
  }
  if (!address.value) {
    uni.showToast({ title: '收货地址为空', icon: 'none' })
    goAddressEdit()
    return
  }

  submitting.value = true
  try {
    const payload = {
      addressBookId: address.value.id,
      payMethod: 1,
      remark: remark.value,
      estimatedDeliveryTime: estimatedDeliveryTime.value,
      deliveryStatus: deliveryStatus.value,
      amount: goodsAmount.value,
      deliveryFee: DELIVERY_FEE,
      phone: address.value.phone,
      address: `${address.value.provinceName || ''}${address.value.cityName || ''}${
        address.value.districtName || ''
      }${address.value.detail || ''}`,
      consignee: address.value.consignee,
      userName: user.nickname,
      orderItems: buildOrderItems()
    }
    const data = await orderApi.submit(payload)
    uni.removeStorageSync('xh_order_remark')
    /* 下单成功只说明订单已创建（待付款），紧接着进入支付页 */
    uni.redirectTo({
      url: `/pages/order/pay?orderNumber=${data.orderNumber}&amount=${data.orderAmount}&orderTime=${encodeURIComponent(
        data.orderTime || ''
      )}`
    })
  } catch {
    /* 请求层已提示（购物车为空 / 收货地址为空等文案来自服务端） */
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="page xh-page">
    <xh-nav-bar title="确认订单" />

    <view v-if="loading" class="loading"><text>正在准备订单…</text></view>


    <xh-empty
      v-else-if="cart.empty"
      kind="empty"
      title="购物车是空的"
      desc="先去选几样今日鲜货吧"
      action-text="去选品"
      @action="() => uni.reLaunch({ url: '/pages/index/index' })"
    />

    <view v-else class="body">
      <!-- 收货地址 -->
      <view class="block xh-card" @tap="goAddressList">
        <view v-if="address" class="addr">
          <view class="addr__row">
            <text class="addr__name">{{ address.consignee }}</text>
            <text class="addr__phone xh-num">{{ address.phone }}</text>
            <text v-if="address.isDefault === 1" class="addr__badge">默认</text>
          </view>
          <text class="addr__detail">
            {{ address.provinceName }}{{ address.cityName }}{{ address.districtName }}{{ address.detail }}
          </text>
        </view>
        <view v-else class="addr addr--empty">
          <text class="addr__name">{{ addressError || '请选择收货地址' }}</text>
          <text class="addr__hint">去添加 →</text>
        </view>
      </view>

      <!-- 配送时效 -->
      <view class="block xh-card">
        <text class="block__title">配送时效</text>
        <view class="seg">
          <view
            class="seg__item"
            :class="{ 'seg__item--active': deliveryStatus === 1 }"
            @tap="deliveryStatus = 1"
          >
            <text>尽快送达</text>
          </view>
          <view
            class="seg__item"
            :class="{ 'seg__item--active': deliveryStatus === 0 }"
            @tap="deliveryStatus = 0"
          >
            <text>预约时段</text>
          </view>
        </view>

        <picker
          v-if="deliveryStatus === 0"
          mode="selector"
          :range="appointSlots.map((s) => s.label)"
          :value="appointIndex"
          @change="pickAppoint"
        >
          <view class="appoint">
            <text>送达时间</text>
            <text class="appoint__value xh-num">{{ appointSlots[appointIndex].label }} 前后</text>
          </view>
        </picker>
        <text v-else class="hint">门店接单后预计 30 分钟内送达</text>
      </view>

      <!-- 商品明细 -->
      <view class="block xh-card">
        <text class="block__title">商品明细（{{ cart.count }} 件）</text>
        <view v-for="item in cart.items" :key="item.id" class="line">
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

      <!-- 备注 -->
      <view class="block xh-card" @tap="goRemark">
        <view class="row">
          <text class="row__label">订单备注</text>
          <text class="row__value">{{ remark || '口味、包装、放置位置等' }}</text>
          <xh-icon name="arrow-right" :size="30" />
        </view>
      </view>

      <!-- 金额 -->
      <view class="block xh-card">
        <view class="row">
          <text class="row__label">商品金额</text>
          <text class="xh-num row__num">¥{{ money(goodsAmount) }}</text>
        </view>
        <view class="row">
          <text class="row__label">运送费</text>
          <text class="xh-num row__num">¥{{ money(DELIVERY_FEE) }}</text>
        </view>
        <view class="xh-hairline" />
        <view class="row row--total">
          <text class="row__label">合计</text>
          <xh-price :value="totalAmount" size="xl" />
        </view>
      </view>

      <view class="body__space" />
    </view>

    <!-- 底部提交条 -->
    <view v-if="!loading && !cart.empty" class="bar">
      <view class="bar__left">
        <text class="bar__label">合计</text>
        <xh-price :value="totalAmount" size="xl" />
      </view>
      <view
        class="bar__submit"
        :class="{ 'bar__submit--busy': submitting }"
        hover-class="bar__submit--hover"
        @tap="onSubmit"
      >
        <text>{{ submitting ? '提交中…' : '提交订单' }}</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page {
  background: $xh-paper;
  padding-bottom: 140rpx;
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
.block {
  padding: $xh-3;
  margin-bottom: $xh-3;
}
.block__title {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
  letter-spacing: 1rpx;
}
.body__space {
  height: 40rpx;
}

.addr {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}
.addr__row {
  display: flex;
  align-items: center;
  gap: $xh-2;
}
.addr__name {
  font-size: $xh-text-lg;
  font-weight: 600;
  color: $xh-ink-900;
}
.addr__phone {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
}
.addr__badge {
  font-size: $xh-text-xs;
  line-height: 30rpx;
  padding: 0 8rpx;
  border-radius: $xh-radius-xs;
  background: $xh-teal-50;
  color: $xh-teal-800;
}
.addr__detail {
  font-size: $xh-text-sm;
  color: $xh-ink-700;
  line-height: 1.5;
}
.addr--empty {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}
.addr--empty .addr__name {
  font-size: $xh-text-base;
  color: $xh-cinnabar-700;
  font-weight: 400;
}
.addr__hint {
  font-size: $xh-text-sm;
  color: $xh-teal-800;
}

.seg {
  margin-top: $xh-3;
  display: flex;
  gap: $xh-2;
}
.seg__item {
  flex: 1;
  height: 76rpx;
  border-radius: $xh-radius;
  border: 2rpx solid $xh-line;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $xh-surface-2;
}
.seg__item text {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
}
.seg__item--active {
  border-color: $xh-teal-600;
  background: $xh-teal-50;
}
.seg__item--active text {
  color: $xh-teal-800;
  font-weight: 600;
}
.hint {
  display: block;
  margin-top: $xh-2;
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.appoint {
  margin-top: $xh-3;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 $xh-3;
  border-radius: $xh-radius;
  background: $xh-surface-2;
  border: 2rpx solid $xh-line;
  font-size: $xh-text-sm;
  color: $xh-ink-500;
}
.appoint__value {
  color: $xh-ink-900;
  font-weight: 600;
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

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $xh-2 0;
}
.row__label {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
}
.row__value {
  flex: 1;
  margin: 0 $xh-2;
  text-align: right;
  font-size: $xh-text-sm;
  color: $xh-ink-400;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row__arrow {
  font-size: 32rpx;
  color: $xh-ink-300;
}
.row__num {
  font-size: $xh-text-sm;
  color: $xh-ink-900;
}
.row--total {
  padding-top: $xh-3;
}

.bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 128rpx;
  padding: 0 $xh-4 calc(env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: $xh-surface;
  border-top: 2rpx solid $xh-line;
}
.bar__left {
  display: flex;
  align-items: baseline;
  gap: $xh-2;
}
.bar__label {
  font-size: $xh-text-sm;
  color: $xh-ink-400;
}
.bar__submit {
  height: 88rpx;
  min-width: 260rpx;
  border-radius: $xh-radius;
  background: $xh-teal-600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bar__submit text {
  font-size: $xh-text-base;
  color: #ffffff;
  font-weight: 600;
}
.bar__submit--hover {
  background: $xh-teal-800;
}
.bar__submit--busy {
  background: $xh-teal-500;
}
</style>
