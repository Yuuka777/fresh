<script setup>
/* 订单备注：写入本地暂存，返回上一页由确认订单页读取回填 */
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const MAX = 100

const PHRASES = [
  '放门口就行',
  '请电话联系',
  '不要辣',
  '需要冰袋',
  '工作日送公司',
  '蔬菜挑新鲜的'
]

const remark = ref('')

const left = computed(() => MAX - remark.value.length)

onLoad(() => {
  remark.value = uni.getStorageSync('xh_order_remark') || ''
})

function pick(p) {
  if (remark.value.indexOf(p) !== -1) return
  const next = remark.value ? `${remark.value}，${p}` : p
  remark.value = next.slice(0, MAX)
}

function save() {
  uni.setStorageSync('xh_order_remark', remark.value)
  uni.navigateBack()
}

function clear() {
  remark.value = ''
}
</script>

<template>
  <view class="page xh-page">
    <xh-nav-bar title="订单备注" />

    <view class="body">
      <view class="block xh-card">
        <textarea
          v-model="remark"
          class="editor"
          :maxlength="MAX"
          placeholder="口味、包装、放置位置等，例如：放门口就行"
          placeholder-class="editor__ph"
          auto-height
        />
        <view class="editor__foot">
          <text class="editor__left xh-num">{{ left }} 字可填</text>
          <text v-if="remark" class="editor__clear" @tap="clear">清空</text>
        </view>
      </view>

      <view class="block">
        <text class="block__title">常用备注</text>
        <view class="phrases">
          <view
            v-for="p in PHRASES"
            :key="p"
            class="phrase"
            :class="{ 'phrase--active': remark.indexOf(p) !== -1 }"
            hover-class="phrase--hover"
            @tap="pick(p)"
          >
            <text>{{ p }}</text>
          </view>
        </view>
      </view>

      <view class="tip">
        <text>备注会随订单一起发送给门店；如需修改配送时间，请在确认订单页选择「预约时段」。</text>
      </view>
    </view>

    <view class="bar">
      <view class="bar__save" hover-class="bar__save--hover" @tap="save">
        <text>保存备注</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page {
  background: $xh-paper;
  padding-bottom: 160rpx;
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
}
.editor {
  width: 100%;
  min-height: 180rpx;
  font-size: $xh-text-base;
  line-height: 1.6;
  color: $xh-ink-900;
}
.editor__ph {
  color: $xh-ink-300;
}
.editor__foot {
  margin-top: $xh-2;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.editor__left {
  font-size: $xh-text-xs;
  color: $xh-ink-300;
}
.editor__clear {
  font-size: $xh-text-xs;
  color: $xh-cinnabar-700;
}

.phrases {
  margin-top: $xh-3;
  display: flex;
  flex-wrap: wrap;
  gap: $xh-2;
}
.phrase {
  padding: 0 $xh-3;
  height: 68rpx;
  display: flex;
  align-items: center;
  border-radius: $xh-radius-sm;
  border: 2rpx solid $xh-line;
  background: $xh-surface-2;
}
.phrase text {
  font-size: $xh-text-sm;
  color: $xh-ink-700;
}
.phrase--active {
  border-color: $xh-teal-600;
  background: $xh-teal-50;
}
.phrase--active text {
  color: $xh-teal-800;
}
.phrase--hover {
  border-color: $xh-teal-200;
}

.tip {
  padding: $xh-3;
  border-radius: $xh-radius-sm;
  background: $xh-surface-2;
  border: 2rpx solid $xh-line-soft;
  font-size: $xh-text-xs;
  line-height: 1.8;
  color: $xh-ink-400;
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
.bar__save {
  height: 88rpx;
  border-radius: $xh-radius;
  background: $xh-teal-600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bar__save text {
  font-size: $xh-text-base;
  color: #ffffff;
  font-weight: 600;
}
.bar__save--hover {
  background: $xh-teal-800;
}
</style>
