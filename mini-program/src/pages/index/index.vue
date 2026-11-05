<script setup>
/* ============================================================
   首页选品
   契约：/user/goodsCategory/list、/user/goods/list、/user/bundle/list、/user/bundle/goods/{id}
         /user/cart/{add,sub,list}、/user/shop/status
   布局：左侧分类栏（商品分类 + 组合分类两组）＋ 右侧商品列表 ＋ 底部购物车条
   素材约束：商品图留空 → 「首字 + 温区配色」占位；Banner 用色块占位，不使用任何图片素材
   数据：不做本地兜底，后端未就绪时如实呈现错误态与重试入口
   ============================================================ */
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { categoryApi, goodsApi, bundleApi, shopApi } from '@/api/user'
import { useUserStore } from '@/store/user'
import { useCartActions } from '@/common/use-cart-actions'
import { money } from '@/common/format'

/* 首页轮播：文案压在照片上，图片集中在 static/banner */
const BANNERS = [
  { src: '/static/banner/banner-1.jpg', title: '产地直采 · 门店分拣', desc: '凌晨采、当日拣，全程冷链可溯' },
  { src: '/static/banner/banner-2.jpg', title: '30 分钟送达', desc: '社区前置仓发货，就近配送' },
  { src: '/static/banner/banner-3.jpg', title: '三区温控', desc: '常温 / 冷藏 / 冷冻，分温区配齐' }
]

/* 分类图标：分类由管理端动态增删改，因此按关键词匹配而不是逐名登记，
   顺序即优先级（越具体的语义越靠前）；全部未命中时落到通用包裹图标，
   保证新增分类也不会出现空图 */
const CAT_ICON_RULES = [
  [/水产|海产|海鲜|鱼|虾|蟹|贝|蛤|螺|鱿/, 'cat-seafood'],
  [/肉|禽|牛羊|猪|鸡|鸭|排骨|肘子|香肠|腊/, 'cat-meat'],
  [/菌|菇|木耳/, 'cat-mushroom'],
  [/蛋/, 'cat-egg'],
  [/乳|奶|烘焙|面包|蛋糕|吐司|芝士|黄油/, 'cat-dairy'],
  [/冷冻|速食|雪糕|冰淇淋|丸子|水饺|冻品/, 'cat-frozen'],
  [/酒|饮料|饮品|茶|咖啡|汽水|果汁/, 'cat-drink'],
  [/零食|糖果|饼干|坚果|干果|蜜饯/, 'cat-snack'],
  [/熟食|凉菜|半成品|预制|卤味|烧腊/, 'cat-deli'],
  [/火锅|锅底|涮|汤料/, 'cat-hotpot'],
  [/粮油|米面|调味|食用油|酱油|醋|盐|香辛|干货|杂粮/, 'cat-grain'],
  [/日用|百货|居家|清洁|纸品|洗护/, 'cat-daily'],
  [/鲜花|绿植|花卉/, 'cat-flower'],
  [/有机|生态|农场|叶菜/, 'cat-organic'],
  [/水果|鲜果|莓|橙|柚|葡萄|枣|果/, 'cat-fruit'],
  [/蔬菜|时蔬|菜|青|叶|薯|萝卜|葱|姜|蒜/, 'cat-veg'],
  [/组合|套餐|礼盒|箱|搭配/, 'package']
]

const catIcon = (name) => {
  const hit = CAT_ICON_RULES.find(([re]) => re.test(name || ''))
  return hit ? hit[1] : 'package'
}

/* 分类栏选中判定：'c' 商品分类 / 'b' 组合分类 */
const isActiveCat = (prefix, id) => activeKey.value === `${prefix}:${id}`

const user = useUserStore()
const { cart, specPopup, addToCart, plusGoods, minusGoods, confirmSpec, closeSpec } = useCartActions()

const loading = ref(true)
const shopOpen = ref(true)

const categories = ref([]) // 商品分类（type=1）
const bundleCategories = ref([]) // 组合分类（type=2）
const activeKey = ref('') // 'c:{id}' 或 'b:{id}'
const list = ref([]) // 当前分类下的商品或组合箱
const listLoading = ref(false)

const bundlePopup = ref({ visible: false, bundle: {}, goods: [], loading: false })
const cartPopup = ref(false) // 购物车弹层（点篮子打开，可编辑）

const activeType = computed(() => (activeKey.value.startsWith('b:') ? 'bundle' : 'goods'))
const activeId = computed(() => Number(activeKey.value.split(':')[1] || 0))
const activeName = computed(() => {
  const pool = activeType.value === 'bundle' ? bundleCategories.value : categories.value
  const hit = pool.find((c) => c.id === activeId.value)
  return hit ? hit.name : ''
})

/* 搜索：跳转到独立结果页，与当前分类无关（见 /pages/search/result） */
function goSearch() {
  uni.navigateTo({ url: '/pages/search/result' })
}

const cartCount = computed(() => cart.count)

/* 车里已下架 / 已停售的行数：> 0 就不能结算（服务端下单时也会再拦一道） */
const blockedCount = computed(() => cart.items.filter((i) => i.status !== 1).length)
/* 待付金额只算还能买的那些：把下架项算进去会让合计与实际要付的钱对不上 */
const payableAmount = computed(() =>
  Number(
    cart.items
      .filter((i) => i.status === 1)
      .reduce((sum, i) => sum + Number(i.amount || 0), 0)
      .toFixed(2)
  )
)
const canSettle = computed(() => cartCount.value > 0 && blockedCount.value === 0 && shopOpen.value)
const settleText = computed(() => {
  if (!shopOpen.value) return '门店已打烊'
  if (blockedCount.value) return `${blockedCount.value} 件已下架`
  return '去结算'
})

onLoad(async () => {
  await bootstrap()
})

onShow(() => {
  /* 从确认订单/详情返回时同步购物车 */
  if (user.logged) cart.load(true)
})

async function bootstrap() {
  loading.value = true
  try {
    /* 商品/组合箱列表都要求 authentication，先确保登录 */
    await user.ensureLogin()
    const [status, goodsCats, bundleCats] = await Promise.all([
      shopApi.status(),
      categoryApi.list(1),
      categoryApi.list(2)
    ])
    shopOpen.value = Number(status) === 1
    categories.value = Array.isArray(goodsCats) ? goodsCats : []
    bundleCategories.value = Array.isArray(bundleCats) ? bundleCats : []
    await cart.load(true)

    const first = categories.value[0] || bundleCategories.value[0]
    if (first) {
      selectCategory(categories.value[0] ? `c:${first.id}` : `b:${first.id}`)
    } else {
      loading.value = false
    }
  } catch (e) {
    loading.value = false
  }
}

async function selectCategory(key) {
  if (activeKey.value === key && list.value.length) return
  activeKey.value = key
  await loadList()
}

async function loadList() {
  if (!activeId.value) return
  listLoading.value = true
  try {
    if (activeType.value === 'bundle') {
      const data = await bundleApi.listByCategory(activeId.value)
      list.value = Array.isArray(data) ? data : []
    } else {
      const data = await goodsApi.listByCategory(activeId.value)
      list.value = Array.isArray(data) ? data : []
    }
  } catch (e) {
    list.value = []
  } finally {
    listLoading.value = false
    loading.value = false
  }
}

function onRetry() {
  bootstrap()
}

/* —— 加购：组合箱直接加；商品交给共用动作（有规格走弹层，无规格直接加） —— */
function onPlus(item) {
  if (!shopOpen.value) {
    uni.showToast({ title: '门店已打烊，暂不接单', icon: 'none' })
    return
  }
  if (activeType.value === 'bundle') {
    addToCart({ bundleId: item.id })
    return
  }
  plusGoods(item)
}

function onMinus(item) {
  if (activeType.value === 'bundle') {
    cart.sub({ bundleId: item.id }).catch(() => {})
    return
  }
  minusGoods(item)
}

/* 组合箱弹层内的「加入购物车」 */
function addBundleFromPopup() {
  if (!shopOpen.value) {
    uni.showToast({ title: '门店已打烊，暂不接单', icon: 'none' })
    return
  }
  addToCart({ bundleId: bundlePopup.value.bundle.id })
  bundlePopup.value.visible = false
}

/* 组合箱：查看内含商品 */
async function openBundle(bundle) {
  bundlePopup.value = { visible: true, bundle, goods: [], loading: true }
  try {
    const data = await bundleApi.goods(bundle.id)
    bundlePopup.value.goods = Array.isArray(data) ? data : []
  } catch {
    bundlePopup.value.goods = []
  } finally {
    bundlePopup.value.loading = false
  }
}

/* —— 购物车：篮子是「查看并编辑」，去结算才是下单 —— */
function closeCart() {
  cartPopup.value = false
}

/* 弹层里按单价展示：库里这一行存的是小计（单价 × 数量），这里还原回单价 */
function lineUnitPrice(line) {
  const n = Number(line.number || 0)
  return n > 0 ? Number((Number(line.amount || 0) / n).toFixed(2)) : 0
}

function goCart() {
  if (cartCount.value === 0) {
    uni.showToast({ title: '购物车还是空的', icon: 'none' })
    return
  }
  cartPopup.value = true
  /* 别的页面（详情、结算）也可能改过车，打开时拉一次最新的 */
  cart.load(true)
}

function goSettle() {
  if (cartCount.value === 0) {
    uni.showToast({ title: '购物车还是空的', icon: 'none' })
    return
  }
  if (!shopOpen.value) {
    uni.showToast({ title: '门店已打烊，暂不接单', icon: 'none' })
    return
  }
  /* 有下架项就不能往下走：让用户先移除，而不是等下单接口报错 */
  if (blockedCount.value) {
    uni.showToast({ title: `有 ${blockedCount.value} 件已下架，请先移除`, icon: 'none' })
    return
  }
  cartPopup.value = false
  uni.navigateTo({ url: '/pages/order/confirm' })
}

/* 弹层里加减的是已在车里的那一行：按这一行自己的商品/规格回传，服务端按同款判定 */
function cartLinePayload(line) {
  const payload = line.goodsId ? { goodsId: line.goodsId } : { bundleId: line.bundleId }
  /* 无规格的行不传该字段：库里存 NULL，传空串会被当成另一个规格（后端也会把空串归一掉） */
  if (line.specText) payload.specText = line.specText
  return payload
}

function onCartPlus(line) {
  if (!shopOpen.value) {
    uni.showToast({ title: '门店已打烊，暂不接单', icon: 'none' })
    return
  }
  cart.add(cartLinePayload(line)).catch(() => {})
}

function onCartMinus(line) {
  cart.sub(cartLinePayload(line)).catch(() => {})
}

/* 已下架 / 已停售的行不能加减，给它一个「移除」出口 */
async function onRemoveLine(line) {
  try {
    await cart.removeLine(line.id)
    uni.showToast({ title: '已移除', icon: 'none' })
  } catch {
    /* 请求层已提示 */
  }
}

async function onClearCart() {
  const ok = await new Promise((resolve) =>
    uni.showModal({
      title: '清空购物车',
      content: '确认清空购物车里的全部商品？',
      confirmText: '清空',
      confirmColor: '#B23A22',
      success: (r) => resolve(r.confirm)
    })
  )
  if (!ok) return
  try {
    await cart.clean()
    uni.showToast({ title: '已清空', icon: 'none' })
    cartPopup.value = false
  } catch {
    /* 请求层已提示 */
  }
}
</script>

<template>
  <view class="page xh-page xh-page--with-tabbar">
    <!-- 顶部：品牌 + 营业状态 -->
    <view class="hero">
      <view class="hero__top">
        <view class="hero__brand">
          <text class="hero__name">鲜禾</text>
          <text class="hero__slogan">社区生鲜前置仓 · 30 分钟达</text>
        </view>
        <view class="hero__status" :class="{ 'hero__status--closed': !shopOpen }">
          <text class="hero__dot" />
          <text>{{ shopOpen ? '营业中' : '已打烊' }}</text>
        </view>
      </view>

      <!-- Banner：真实照片轮播，文案压在图上 -->
      <swiper
        class="banner"
        :indicator-dots="true"
        indicator-color="rgba(255,255,255,0.42)"
        indicator-active-color="#ffffff"
        :autoplay="true"
        :interval="4200"
        :duration="420"
        circular
      >
        <swiper-item v-for="b in BANNERS" :key="b.src">
          <view class="banner__slide">
            <image class="banner__img" :src="b.src" mode="aspectFill" />
            <view class="banner__mask" />
            <view class="banner__text">
              <text class="banner__title">{{ b.title }}</text>
              <text class="banner__desc">{{ b.desc }}</text>
            </view>
          </view>
        </swiper-item>
      </swiper>

      <!-- 搜索入口：首页不做站内过滤，点击进入独立搜索结果页（与外卖软件一致） -->
      <view class="search" hover-class="search--hover" @tap="goSearch">
        <xh-icon name="search" :size="36" />
        <text class="search__ph">搜索上架商品</text>
      </view>
    </view>

    <!-- 主体：左分类 + 右列表 -->
    <view class="body">
      <scroll-view class="rail" scroll-y :show-scrollbar="false">
        <view v-if="categories.length" class="rail__label">
          <text>商品分类</text>
        </view>
        <view
          v-for="c in categories"
          :key="`c-${c.id}`"
          class="rail__item"
          :class="{ 'rail__item--active': activeKey === `c:${c.id}` }"
          @tap="selectCategory(`c:${c.id}`)"
        >
          <xh-icon
            :name="catIcon(c.name)"
            :tone="isActiveCat(activeKey.charAt(0), c.id) ? 'teal' : 'ink'"
            :size="32"
          />
          <text class="rail__text">{{ c.name }}</text>
        </view>

        <template v-if="bundleCategories.length">
          <view class="rail__label rail__label--divider">
            <text>组合箱</text>
          </view>
          <view
            v-for="c in bundleCategories"
            :key="`b-${c.id}`"
            class="rail__item"
            :class="{ 'rail__item--active': activeKey === `b:${c.id}` }"
            @tap="selectCategory(`b:${c.id}`)"
          >
            <xh-icon
            :name="catIcon(c.name)"
            :tone="isActiveCat(activeKey.charAt(0), c.id) ? 'teal' : 'ink'"
            :size="32"
          />
          <text class="rail__text">{{ c.name }}</text>
          </view>
        </template>
      </scroll-view>

      <scroll-view class="list" scroll-y :show-scrollbar="false">
        <view class="list__head">
          <text class="list__title">{{ activeName || '选品' }}</text>
          <text class="list__count xh-num">{{ list.length }} 件</text>
        </view>

        <!-- 加载中骨架 -->
        <view v-if="listLoading" class="skeleton">
          <view v-for="n in 4" :key="n" class="skeleton__row">
            <view class="skeleton__thumb" />
            <view class="skeleton__lines">
              <view class="skeleton__line skeleton__line--wide" />
              <view class="skeleton__line" />
              <view class="skeleton__line skeleton__line--short" />
            </view>
          </view>
        </view>

        <!-- 空态 -->
        <xh-empty v-else-if="loading" kind="empty" title="正在准备今日货架" />
        <xh-empty
          v-else-if="!list.length"
          kind="empty"
          title="这个分类暂时没有上架商品"
          desc="门店上架后会在这里出现"
          action-text="重新加载"
          @action="loadList"
        />

        <!-- 商品 / 组合箱 -->
        <view v-else class="goods">
          <xh-goods-card
            v-for="item in list"
            :key="item.id"
            :item="item"
            :mode="activeType"
            @plus="onPlus(item)"
            @minus="onMinus(item)"
            @more="openBundle(item)"
          />
        </view>

        <view class="list__foot-space" />
      </scroll-view>
    </view>

    <!-- 底部购物车条 -->
    <view class="cartbar" :class="{ 'cartbar--empty': cartCount === 0 }">
      <view class="cartbar__left" @tap="goCart">
        <view class="cartbar__badge">
          <text class="cartbar__badge-mark">篮</text>
          <text v-if="cartCount" class="cartbar__badge-num xh-num">{{ cartCount }}</text>
        </view>
        <view class="cartbar__text">
          <xh-price v-if="cartCount" :value="payableAmount" size="lg" />
          <text v-else class="cartbar__tip">购物车是空的</text>
        </view>
      </view>
      <view
        class="cartbar__cta"
        :class="{ 'cartbar__cta--disabled': !canSettle }"
        hover-class="cartbar__cta--hover"
        @tap="goSettle"
      >
        <text>{{ settleText }}</text>
      </view>
    </view>

    <xh-tabbar current="index" />

    <!-- 规格选择 -->
    <xh-spec-popup
      :visible="specPopup.visible"
      :goods="specPopup.goods"
      :submitting="specPopup.submitting"
      @close="closeSpec"
      @confirm="confirmSpec"
    />

    <!-- 组合箱内含商品 -->
    <view v-if="bundlePopup.visible" class="bpopup">
      <view class="bpopup__mask" @tap="bundlePopup.visible = false" />
      <view class="bpopup__sheet">
        <view class="bpopup__head">
          <text class="bpopup__title">{{ bundlePopup.bundle.name }}</text>
          <view class="bpopup__close" @tap="bundlePopup.visible = false"><xh-icon name="close" :size="34" /></view>
        </view>
        <text class="bpopup__desc">{{ bundlePopup.bundle.description || '门店按温区配齐' }}</text>

        <view v-if="bundlePopup.loading" class="bpopup__loading">正在读取内含商品…</view>
        <xh-empty
          v-else-if="!bundlePopup.goods.length"
          kind="empty"
          compact
          title="内含商品暂时取不到"
          desc="请确认后端服务已启动"
        />
        <scroll-view v-else class="bpopup__list" scroll-y>
          <view v-for="(g, i) in bundlePopup.goods" :key="i" class="bpopup__item">
            <text class="bpopup__item-name">{{ g.name }}</text>
            <text class="bpopup__item-x xh-num">× {{ g.copies || 1 }}</text>
            <xh-price :value="g.price" size="sm" />
          </view>
        </scroll-view>

        <view class="bpopup__foot">
          <xh-price :value="bundlePopup.bundle.price" size="lg" />
          <view class="bpopup__add" hover-class="bpopup__add--hover" @tap="addBundleFromPopup">
            <text>加入购物车</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 购物车弹层：点篮子打开，可查看并编辑（逐项加减、整单清空） -->
    <view v-if="cartPopup" class="cpopup">
      <view class="cpopup__mask" @tap="closeCart" />
      <view class="cpopup__sheet">
        <view class="cpopup__head">
          <text class="cpopup__title">购物车</text>
          <view class="cpopup__tools">
            <text v-if="cartCount" class="cpopup__clear" @tap="onClearCart">清空</text>
            <view class="cpopup__close" @tap="closeCart"><xh-icon name="close" :size="34" /></view>
          </view>
        </view>

        <xh-empty
          v-if="!cartCount"
          kind="empty"
          compact
          title="购物车是空的"
          desc="回列表挑点新鲜的"
        />
        <template v-else>
          <scroll-view class="cpopup__list" scroll-y>
            <view
              v-for="(line, i) in cart.items"
              :key="i"
              class="cpopup__item"
              :class="{ 'cpopup__item--off': line.status !== 1 }"
            >
              <xh-goods-thumb
                :name="line.name"
                :image="line.image"
                :size="128"
                :show-zone="false"
              />
              <view class="cpopup__info">
                <text class="cpopup__name">{{ line.name }}</text>
                <text v-if="line.specText" class="cpopup__spec">{{ line.specText }}</text>
                <view class="cpopup__row">
                  <xh-price :value="lineUnitPrice(line)" size="md" />
                  <!-- 已下架的不能结算，也不该再加减，给一个移除出口 -->
                  <view
                    v-if="line.status !== 1"
                    class="cpopup__off"
                    hover-class="cpopup__off--hover"
                    @tap="onRemoveLine(line)"
                  >
                    <text>{{ line.goodsId ? '已下架' : '已停售' }} · 移除</text>
                  </view>
                  <xh-stepper
                    v-else
                    :value="line.number"
                    :min="0"
                    size="small"
                    @plus="onCartPlus(line)"
                    @minus="onCartMinus(line)"
                  />
                </view>
                <text v-if="line.priceChanged" class="cpopup__changed">价格已更新，按最新价计算</text>
              </view>
            </view>
          </scroll-view>

          <view class="cpopup__foot">
            <view class="cpopup__total">
              <text class="cpopup__total-label">合计</text>
              <xh-price :value="payableAmount" size="lg" />
            </view>
            <view
              class="cpopup__settle"
              :class="{ 'cpopup__settle--disabled': !canSettle }"
              hover-class="cpopup__settle--hover"
              @tap="goSettle"
            >
              <text>{{ settleText }}</text>
            </view>
          </view>
        </template>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page {
  background: $xh-paper;
}

/* —— 顶部 —— */
.hero {
  padding: $xh-4 $xh-4 $xh-3;
  background: linear-gradient(180deg, #eef5f1 0%, $xh-paper 100%);
}
.hero__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.hero__brand {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.hero__name {
  font-size: $xh-text-3xl;
  font-weight: 700;
  letter-spacing: 8rpx;
  color: $xh-ink-900;
}
.hero__slogan {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
  letter-spacing: 2rpx;
}
.hero__status {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 16rpx;
  border-radius: $xh-radius-xs;
  background: $xh-teal-50;
  border: 2rpx solid #b8e3d4;
  color: $xh-teal-800;
  font-size: $xh-text-xs;
}
.hero__status--closed {
  background: #f1f2ef;
  border-color: $xh-line;
  color: $xh-ink-400;
}
.hero__dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: $xh-teal-500;
}
.hero__status--closed .hero__dot {
  background: $xh-ink-300;
}

.banner {
  margin-top: $xh-3;
  height: 296rpx;
  border-radius: $xh-radius;
  overflow: hidden;
}
.banner__slide {
  position: relative;
  width: 100%;
  height: 296rpx;
}
.banner__img {
  width: 100%;
  height: 296rpx;
  display: block;
}
/* 只在底部压一层暗色保证白字可读，不做整块渐变 */
.banner__mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 66%;
  background: linear-gradient(180deg, rgba(10, 28, 22, 0) 0%, rgba(10, 28, 22, 0.68) 100%);
}
.banner__text {
  position: absolute;
  left: $xh-4;
  right: $xh-4;
  bottom: 52rpx;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.banner__title {
  font-size: $xh-text-lg;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: 2rpx;
}
.banner__desc {
  font-size: $xh-text-xs;
  color: rgba(255, 255, 255, 0.84);
}

.search {
  margin-top: $xh-3;
  height: 96rpx;
  display: flex;
  align-items: center;
  gap: $xh-2;
  padding: 0 $xh-3;
  background: $xh-surface;
  border: 2rpx solid $xh-line;
  border-radius: $xh-radius;
}
.search--hover {
  background: $xh-paper;
}
.search__ph {
  font-size: $xh-text-sm;
  color: $xh-ink-400;
}

/* —— 主体 —— */
.body {
  display: flex;
  height: calc(100vh - 640rpx);
  min-height: 720rpx;
}
.rail {
  width: 176rpx;
  background: #eef0ed;
}
.rail__label {
  padding: $xh-3 $xh-2 $xh-1;
}
.rail__label text {
  font-size: $xh-text-xs;
  letter-spacing: 3rpx;
  color: $xh-ink-300;
}
.rail__label--divider {
  margin-top: $xh-2;
  border-top: 2rpx solid $xh-line;
  padding-top: $xh-3;
}
.rail__item {
  padding: $xh-2 $xh-1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  position: relative;
}
.rail__item--active {
  background: $xh-surface;
}
.rail__item--active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 16rpx;
  bottom: 16rpx;
  width: 6rpx;
  background: $xh-teal-600;
  border-radius: 0 4rpx 4rpx 0;
}
.rail__text {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
  line-height: 1.4;
}
.rail__item--active .rail__text {
  color: $xh-ink-900;
  font-weight: 600;
}

.list {
  flex: 1;
  background: $xh-surface;
  padding: 0 $xh-3;
}
.list__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: $xh-3 0 $xh-2;
}
.list__title {
  font-size: $xh-text-lg;
  font-weight: 600;
  color: $xh-ink-900;
}
.list__count {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.list__foot-space {
  height: 200rpx;
}

/* —— 骨架 —— */
.skeleton {
  padding-top: $xh-2;
}
.skeleton__row {
  display: flex;
  gap: $xh-3;
  padding: $xh-3 0;
  border-bottom: 2rpx solid $xh-line-soft;
}
.skeleton__thumb {
  width: 168rpx;
  height: 168rpx;
  border-radius: $xh-radius-sm;
  background: #eef1ee;
}
.skeleton__lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  padding-top: 8rpx;
}
.skeleton__line {
  height: 24rpx;
  border-radius: 6rpx;
  background: #eef1ee;
  width: 60%;
}
.skeleton__line--wide {
  width: 90%;
}
.skeleton__line--short {
  width: 40%;
}

/* —— 商品行 —— */



/* —— 底部购物车条 —— */
.cartbar {
  position: fixed;
  left: $xh-3;
  right: $xh-3;
  bottom: calc(112rpx + env(safe-area-inset-bottom));
  height: 104rpx;
  display: flex;
  align-items: center;
  padding: 0 $xh-2 0 $xh-3;
  background: #0a241d;
  border-radius: $xh-radius-lg;
  box-shadow: 0 12rpx 32rpx rgba(10, 31, 25, 0.18);
  z-index: 180;
}
.cartbar__left {
  flex: 1;
  display: flex;
  align-items: center;
  gap: $xh-3;
  min-width: 0;
}
.cartbar__badge {
  position: relative;
  width: 68rpx;
  height: 68rpx;
  border-radius: 50%;
  background: rgba(159, 225, 203, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
}
.cartbar__badge-mark {
  font-size: $xh-text-base;
  color: $xh-teal-200;
}
.cartbar__badge-num {
  position: absolute;
  top: -6rpx;
  right: -6rpx;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  text-align: center;
  padding: 0 6rpx;
  border-radius: 999rpx;
  background: $xh-cinnabar-600;
  color: #ffffff;
  font-size: $xh-text-xs;
}
.cartbar__text :deep(.price) {
  color: #f3faf7;
}
.cartbar__tip {
  font-size: $xh-text-sm;
  color: $xh-teal-200;
}
.cartbar__cta {
  height: 80rpx;
  min-width: 200rpx;
  padding: 0 $xh-4;
  border-radius: $xh-radius;
  background: $xh-teal-500;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cartbar__cta text {
  font-size: $xh-text-base;
  color: #ffffff;
  font-weight: 600;
}
.cartbar__cta--hover {
  background: $xh-teal-600;
}
.cartbar__cta--disabled {
  background: rgba(255, 255, 255, 0.14);
}
.cartbar__cta--disabled text {
  color: rgba(255, 255, 255, 0.6);
}

/* —— 组合箱内含商品弹层 —— */
.bpopup {
  position: fixed;
  inset: 0;
  z-index: 500;
}
.bpopup__mask {
  position: absolute;
  inset: 0;
  background: rgba(10, 31, 25, 0.45);
}
.bpopup__sheet {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  background: $xh-surface;
  border-radius: $xh-radius-lg $xh-radius-lg 0 0;
  padding: $xh-4;
  padding-bottom: calc(#{$xh-4} + env(safe-area-inset-bottom));
}
.bpopup__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.bpopup__title {
  font-size: $xh-text-lg;
  font-weight: 600;
  color: $xh-ink-900;
}
.bpopup__close {
  width: 56rpx;
  text-align: center;
  font-size: 40rpx;
  color: $xh-ink-400;
}
.bpopup__desc {
  margin-top: 6rpx;
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.bpopup__loading {
  padding: $xh-6 0;
  text-align: center;
  font-size: $xh-text-sm;
  color: $xh-ink-400;
}
.bpopup__list {
  max-height: 44vh;
  margin-top: $xh-3;
}
.bpopup__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $xh-3;
  padding: $xh-3 0;
  border-bottom: 2rpx solid $xh-line-soft;
}
.bpopup__item-name {
  flex: 1;
  font-size: $xh-text-sm;
  color: $xh-ink-900;
}
.bpopup__item-x {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.bpopup__foot {
  margin-top: $xh-3;
  padding-top: $xh-3;
  border-top: 2rpx solid $xh-line-soft;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.bpopup__add {
  height: 84rpx;
  min-width: 240rpx;
  border-radius: $xh-radius;
  background: $xh-teal-600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bpopup__add text {
  color: #ffffff;
  font-size: $xh-text-base;
}
.bpopup__add--hover {
  background: $xh-teal-800;
}

/* —— 购物车弹层 —— */
.cpopup {
  position: fixed;
  inset: 0;
  z-index: 520;
}
.cpopup__mask {
  position: absolute;
  inset: 0;
  background: rgba(10, 31, 25, 0.45);
}
.cpopup__sheet {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  background: $xh-surface;
  border-radius: $xh-radius-lg $xh-radius-lg 0 0;
  padding: $xh-4;
  padding-bottom: calc(#{$xh-4} + env(safe-area-inset-bottom));
}
.cpopup__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cpopup__title {
  font-size: $xh-text-lg;
  font-weight: 600;
  color: $xh-ink-900;
}
.cpopup__tools {
  display: flex;
  align-items: center;
  gap: $xh-3;
}
.cpopup__clear {
  font-size: $xh-text-sm;
  color: $xh-ink-400;
}
.cpopup__close {
  width: 56rpx;
  text-align: center;
  font-size: 40rpx;
  color: $xh-ink-400;
}
.cpopup__list {
  max-height: 52vh;
  margin-top: $xh-3;
}
.cpopup__item {
  display: flex;
  gap: $xh-3;
  padding: $xh-3 0;
  border-bottom: 2rpx solid $xh-line-soft;
}
.cpopup__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 8rpx;
}
.cpopup__name {
  font-size: $xh-text-sm;
  color: $xh-ink-900;
}
.cpopup__spec {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}
.cpopup__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cpopup__foot {
  margin-top: $xh-3;
  padding-top: $xh-3;
  border-top: 2rpx solid $xh-line-soft;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cpopup__total {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}
.cpopup__total-label {
  font-size: $xh-text-sm;
  color: $xh-ink-400;
}
.cpopup__settle {
  height: 84rpx;
  min-width: 240rpx;
  border-radius: $xh-radius;
  background: $xh-teal-600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cpopup__settle text {
  color: #ffffff;
  font-size: $xh-text-base;
}
.cpopup__settle--hover {
  background: $xh-teal-800;
}
.cpopup__settle--disabled {
  background: $xh-line;
}
.cpopup__settle--disabled text {
  color: $xh-ink-300;
}

/* 已下架 / 已停售的项：文字压暗，减号位置换成「移除」 */
.cpopup__item--off .cpopup__name,
.cpopup__item--off .cpopup__spec {
  color: $xh-ink-300;
}
.cpopup__off {
  height: 52rpx;
  padding: 0 $xh-2;
  border-radius: $xh-radius-xs;
  border: 2rpx solid $xh-line;
  display: flex;
  align-items: center;
}
.cpopup__off text {
  font-size: $xh-text-xs;
  color: $xh-cinnabar-700;
}
.cpopup__off--hover {
  background: $xh-line-soft;
}
.cpopup__changed {
  font-size: $xh-text-xs;
  color: $xh-teal-800;
}
</style>
