<script setup>
/* ============================================================
   搜索结果页
   契约：GET /user/goods/search?name=（按商品名称模糊查询，只返回上架商品）
   与分类无关：直接查商品表，结果独立成页（首页搜索框只作为入口）
   ============================================================ */
import { ref } from 'vue'
import { onUnload } from '@dcloudio/uni-app'
import { goodsApi } from '@/api/user'
import { useUserStore } from '@/store/user'
import { useCartActions } from '@/common/use-cart-actions'

const user = useUserStore()
const { specPopup, plusGoods, minusGoods, confirmSpec, closeSpec } = useCartActions()

const keyword = ref('')
const loading = ref(false)
const searched = ref(false) // 是否已发起过搜索（区分「还没搜」与「搜了没结果」）
const list = ref([])

let timer = null
let seq = 0 // 请求序号：丢弃过期响应，避免慢请求覆盖新结果

function onInput() {
  if (timer) clearTimeout(timer)
  const kw = keyword.value.trim()
  if (!kw) {
    seq++
    list.value = []
    searched.value = false
    loading.value = false
    return
  }
  timer = setTimeout(() => run(kw), 450)
}

function onSearch() {
  if (timer) clearTimeout(timer)
  const kw = keyword.value.trim()
  if (!kw) return
  run(kw)
}

async function run(kw) {
  const my = ++seq
  loading.value = true
  try {
    await user.ensureLogin()
    const data = await goodsApi.search(kw)
    if (my !== seq) return
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    if (my === seq) list.value = []
  } finally {
    if (my === seq) {
      loading.value = false
      searched.value = true
    }
  }
}

function clearKeyword() {
  keyword.value = ''
  onInput()
}

onUnload(() => {
  if (timer) clearTimeout(timer)
})
</script>

<template>
  <view class="page xh-page">
    <xh-nav-bar title="搜索商品" />

    <view class="bar">
      <view class="bar__box">
        <xh-icon name="search" :size="36" />
        <input
          v-model="keyword"
          class="bar__input"
          type="text"
          placeholder="输入商品名称"
          placeholder-class="bar__ph"
          confirm-type="search"
          focus
          @input="onInput"
          @confirm="onSearch"
        />
        <view v-if="keyword" class="bar__clear" @tap="clearKeyword">
          <xh-icon name="close" :size="32" />
        </view>
      </view>
    </view>

    <view class="body">
      <view v-if="loading" class="loading"><text>正在搜索…</text></view>

      <xh-empty
        v-else-if="!searched"
        title="搜索上架商品"
        desc="输入商品名称的关键词，例如「番茄」"
      />

      <xh-empty
        v-else-if="!list.length"
        title="没有找到相关商品"
        desc="换个关键词试试，或回首页按分类逛逛"
      />

      <view v-else class="goods">
        <text class="count">找到 <text class="count__num xh-num">{{ list.length }}</text> 件商品</text>
        <xh-goods-card
          v-for="item in list"
          :key="item.id"
          :item="item"
          mode="goods"
          @plus="plusGoods(item)"
          @minus="minusGoods(item)"
        />
      </view>
    </view>

    <xh-spec-popup
      :visible="specPopup.visible"
      :goods="specPopup.goods"
      :submitting="specPopup.submitting"
      @close="closeSpec"
      @confirm="confirmSpec"
    />
  </view>
</template>

<style lang="scss" scoped>
.bar {
  padding: $xh-3;
  background: $xh-surface;
  border-bottom: 2rpx solid $xh-line-soft;
}
.bar__box {
  height: 96rpx;
  display: flex;
  align-items: center;
  gap: $xh-2;
  padding: 0 $xh-3;
  background: $xh-paper;
  border: 2rpx solid $xh-line;
  border-radius: $xh-radius;
}
.bar__input {
  flex: 1;
  min-width: 0;
  min-height: 96rpx;
  font-size: $xh-text-sm;
  color: $xh-ink-900;
}
.bar__ph {
  color: $xh-ink-400;
}
.bar__clear {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.body {
  padding: $xh-3;
}
.loading {
  padding: $xh-8 0;
  text-align: center;
}
.loading text {
  font-size: $xh-text-sm;
  color: $xh-ink-400;
}
.count {
  display: block;
  padding: 0 $xh-1 $xh-3;
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.count__num {
  color: $xh-ink-700;
}
</style>
