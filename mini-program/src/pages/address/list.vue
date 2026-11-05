<script setup>
/* ============================================================
   收货地址管理
   契约：/user/address/list、/user/address（POST / PUT / DELETE）
         /user/address/default（PUT 设默认）、/user/address/{id}
   从确认订单页进入带 mode=select，点击地址即回传选择
   ============================================================ */
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { addressApi } from '@/api/user'
import { useUserStore } from '@/store/user'

const user = useUserStore()

const mode = ref('manage')
const list = ref([])
const loading = ref(true)

onLoad((options) => {
  mode.value = options.mode === 'select' ? 'select' : 'manage'
})

onShow(() => {
  load()
})

async function load() {
  loading.value = true
  try {
    await user.ensureLogin()
    const data = await addressApi.list()
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function fullAddress(a) {
  return `${a.provinceName || ''}${a.cityName || ''}${a.districtName || ''}${a.detail || ''}`
}

function onPick(a) {
  if (mode.value !== 'select') return
  /* 回传选择：确认订单页在 onShow 时读取并请求该地址详情 */
  uni.setStorageSync('xh_selected_address_id', a.id)
  uni.navigateBack()
}

function goEdit(a) {
  uni.navigateTo({ url: a && a.id ? `/pages/address/edit?id=${a.id}` : '/pages/address/edit' })
}

async function onSetDefault(a) {
  try {
    await addressApi.setDefault(a.id)
    uni.showToast({ title: '已设为默认', icon: 'none' })
    load()
  } catch {
    /* 请求层已提示 */
  }
}

async function onRemove(a) {
  const ok = await new Promise((resolve) =>
    uni.showModal({
      title: '删除地址',
      content: `确认删除 ${a.consignee} 的这个收货地址？`,
      confirmText: '删除',
      confirmColor: '#B23A22',
      success: (r) => resolve(r.confirm)
    })
  )
  if (!ok) return
  try {
    await addressApi.remove(a.id)
    uni.showToast({ title: '已删除', icon: 'none' })
    load()
  } catch {
    /* 请求层已提示 */
  }
}
</script>

<template>
  <view class="page xh-page">
    <xh-nav-bar :title="mode === 'select' ? '选择收货地址' : '收货地址'" />

    <view v-if="loading" class="loading"><text>正在读取地址…</text></view>


    <xh-empty
      v-else-if="!list.length"
      kind="empty"
      title="还没有收货地址"
      desc="添加一个常用地址，下单会快很多"
      action-text="新增地址"
      @action="goEdit(null)"
    />

    <view v-else class="list">
      <view v-for="a in list" :key="a.id" class="addr xh-card" @tap="onPick(a)">
        <view class="addr__main">
          <view class="addr__row">
            <text class="addr__name">{{ a.consignee }}</text>
            <text class="addr__phone xh-num">{{ a.phone }}</text>
            <text v-if="a.isDefault === 1" class="addr__badge">默认</text>
            <text v-if="a.label" class="addr__label">{{ a.label }}</text>
          </view>
          <text class="addr__detail">{{ fullAddress(a) }}</text>
        </view>

        <view class="addr__ops" @tap.stop>
          <text v-if="a.isDefault !== 1" class="op" @tap="onSetDefault(a)">设为默认</text>
          <text class="op" @tap="goEdit(a)">编辑</text>
          <text class="op op--danger" @tap="onRemove(a)">删除</text>
        </view>
      </view>

      <view v-if="mode === 'select'" class="select-hint">
        <text>点击地址即选用该地址下单</text>
      </view>
    </view>

    <view class="bar">
      <view class="bar__add" hover-class="bar__add--hover" @tap="goEdit(null)">
        <text>+ 新增收货地址</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page {
  background: $xh-paper;
  padding-bottom: 170rpx;
}
.loading {
  padding: $xh-10 0;
  text-align: center;
  font-size: $xh-text-sm;
  color: $xh-ink-400;
}
.list {
  padding: $xh-3;
}
.addr {
  padding: $xh-3;
  margin-bottom: $xh-3;
}
.addr__row {
  display: flex;
  align-items: center;
  gap: $xh-2;
}
.addr__name {
  font-size: $xh-text-base;
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
.addr__label {
  font-size: $xh-text-xs;
  line-height: 30rpx;
  padding: 0 8rpx;
  border-radius: $xh-radius-xs;
  background: #f1f2ef;
  color: $xh-ink-400;
}
.addr__detail {
  display: block;
  margin-top: 10rpx;
  font-size: $xh-text-sm;
  line-height: 1.5;
  color: $xh-ink-700;
}
.addr__ops {
  margin-top: $xh-3;
  padding-top: $xh-3;
  border-top: 2rpx solid $xh-line-soft;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: $xh-4;
}
.op {
  min-height: 96rpx;
  display: flex;
  align-items: center;
  font-size: $xh-text-sm;
  color: $xh-ink-500;
}
.op--danger {
  color: $xh-cinnabar-700;
}
.select-hint {
  padding: $xh-3 0;
  text-align: center;
  font-size: $xh-text-xs;
  color: $xh-ink-300;
}

.bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: $xh-3 $xh-4 calc(#{$xh-3} + env(safe-area-inset-bottom));
  background: $xh-surface;
  border-top: 2rpx solid $xh-line;
}
.bar__add {
  height: 96rpx;
  border-radius: $xh-radius;
  background: $xh-teal-600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bar__add text {
  font-size: $xh-text-base;
  color: #ffffff;
  font-weight: 600;
}
.bar__add--hover {
  background: $xh-teal-800;
}
</style>
