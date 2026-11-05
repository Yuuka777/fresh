<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { reportApi, workspaceApi, orderApi } from '@/api/admin'
import { yuan } from '@/constants/dict'
import PanelCard from '@/components/PanelCard.vue'
import EChart from '@/components/EChart.vue'

const router = useRouter()

const loading = ref(true)
const business = ref({ turnover: 0, validOrderCount: 0, orderCompletionRate: 0, unitPrice: 0, newMembers: 0 })
const overview = ref({ waitingOrders: 0, deliveredOrders: 0, completedOrders: 0, cancelledOrders: 0, allOrders: 0 })
const goodsOv = ref({ sold: 0, discontinued: 0 })
const bundleOv = ref({ sold: 0, discontinued: 0 })
/* 待付款数量不在工作台接口里（工作台只有 waitingOrders 等 5 项），
   改用订单统计接口，避免把「待付款」错算成「待接单」 */
const orderStats = ref({ toBePaid: 0, toBeConfirmed: 0 })
const turnoverSeries = ref([])

const tiles = computed(() => [
  { key: 'turnover', label: '今日营业额', value: yuan(business.value.turnover), wide: true },
  { key: 'valid', label: '有效订单', value: String(business.value.validOrderCount), unit: '单' },
  {
    key: 'rate',
    label: '订单完成率',
    value: (business.value.orderCompletionRate * 100).toFixed(1),
    unit: '%'
  },
  { key: 'unit', label: '平均客单价', value: yuan(business.value.unitPrice) },
  { key: 'member', label: '今日新增会员', value: String(business.value.newMembers), unit: '人' }
])

const orderRows = computed(() => [
  {
    label: '待付款',
    value: orderStats.value.toBePaid,
    tab: 'paid',
    hint: '已提交未支付，店员不可接单'
  },
  {
    label: '待接单',
    value: orderStats.value.toBeConfirmed,
    tab: 'confirm',
    hint: '已支付等待接单'
  },
  { label: '待派送', value: overview.value.deliveredOrders, tab: 'delivery', hint: '已接单待派送' },
  { label: '今日已完成', value: overview.value.completedOrders, tab: 'done', hint: '已送达并完成' },
  { label: '今日已取消', value: overview.value.cancelledOrders, tab: 'cancelled', hint: '含超时未支付取消' },
  { label: '今日全部订单', value: overview.value.allOrders, tab: 'all', hint: '' }
])

const chartOption = computed(() => ({
  grid: { left: 8, right: 8, top: 24, bottom: 8, containLabel: true },
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#ffffff',
    borderColor: '#dce4dd',
    borderWidth: 1,
    textStyle: { color: '#0a1f19', fontSize: 12 },
    valueFormatter: (v) => yuan(v)
  },
  xAxis: {
    type: 'category',
    data: turnoverSeries.value.map((d) => d.date.slice(5)),
    axisLine: { lineStyle: { color: '#dce4dd' } },
    axisTick: { show: false },
    axisLabel: { color: '#6b8177', fontSize: 11, fontFamily: 'JetBrains Mono' }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#eef1ed' } },
    axisLabel: { color: '#8fa39a', fontSize: 11, fontFamily: 'JetBrains Mono' }
  },
  series: [
    {
      type: 'line',
      name: '营业额',
      data: turnoverSeries.value.map((d) => d.value),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2, color: '#0f6e56' },
      itemStyle: { color: '#0f6e56', borderColor: '#ffffff', borderWidth: 1 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(15,110,86,0.16)' },
            { offset: 1, color: 'rgba(15,110,86,0)' }
          ]
        }
      }
    }
  ]
}))

function gotoOrder(tab) {
  router.push({ path: '/order', query: { tab } })
}

onMounted(async () => {
  try {
    const [b, o, g, bu, turnover, st] = await Promise.all([
      workspaceApi.businessData(),
      workspaceApi.overviewOrders(),
      workspaceApi.overviewGoods(),
      workspaceApi.overviewBundles(),
      reportApi.turnover({}),
      orderApi.statistics()
    ])
    business.value = b
    overview.value = o
    goodsOv.value = g
    bundleOv.value = bu
    orderStats.value = st

    const dates = String(turnover.dateList || '').split(',').filter(Boolean)
    const values = String(turnover.turnoverList || '').split(',').map(Number)
    turnoverSeries.value = dates
      .map((date, i) => ({ date, value: values[i] ?? 0 }))
      .slice(-7)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page" v-loading="loading">
    <!-- 待处理提醒：只在有待付款单时出现，避免常驻噪音 -->
    <div v-if="orderStats.toBePaid > 0" class="alert" role="status">
      <span class="alert__dot"></span>
      <p class="alert__text">
        有 <span class="xh-num alert__num">{{ orderStats.toBePaid }}</span> 笔订单已提交但
        <strong>尚未支付</strong>，可在「待付款」页签查看；超时 15 分钟将自动取消。
      </p>
      <button type="button" class="alert__btn" @click="gotoOrder('paid')">去查看</button>
    </div>

    <!-- 关键数字：台账式数字块 -->
    <section class="tiles" aria-label="今日关键指标">
      <article v-for="t in tiles" :key="t.key" class="tile" :class="{ 'tile--wide': t.wide }">
        <p class="tile__label">{{ t.label }}</p>
        <p class="tile__value">
          <span :class="t.wide ? 'xh-figure' : 'xh-figure tile__value-sm'">{{ t.value }}</span>
          <span v-if="t.unit" class="tile__unit">{{ t.unit }}</span>
        </p>
      </article>
    </section>

    <div class="grid">
      <PanelCard title="近 7 日营业额" sub="按已完成订单金额统计，单位：元">
        <EChart :option="chartOption" height="272px" />
      </PanelCard>

      <PanelCard title="订单总览" sub="点击任意一行进入对应页签" flush>
        <ul class="ledger">
          <li v-for="row in orderRows" :key="row.label" class="ledger__item xh-row-action">
            <button type="button" class="ledger__btn" @click="gotoOrder(row.tab)">
              <span class="ledger__label">
                {{ row.label }}
                <em v-if="row.hint" class="ledger__hint">{{ row.hint }}</em>
              </span>
              <span class="ledger__value xh-figure">{{ row.value }}</span>
            </button>
          </li>
        </ul>
      </PanelCard>

      <PanelCard title="商品总览" sub="上架 / 下架分布">
        <div class="split">
          <div class="split__item">
            <p class="split__num xh-figure">{{ goodsOv.sold }}</p>
            <p class="split__cap">已上架</p>
          </div>
          <div class="split__item split__item--muted">
            <p class="split__num xh-figure">{{ goodsOv.discontinued }}</p>
            <p class="split__cap">已下架</p>
          </div>
        </div>
        <div class="bar" aria-hidden="true">
          <span
            class="bar__fill"
            :style="{
              width:
                (goodsOv.sold + goodsOv.discontinued
                  ? (goodsOv.sold / (goodsOv.sold + goodsOv.discontinued)) * 100
                  : 0) + '%'
            }"
          ></span>
        </div>
      </PanelCard>

      <PanelCard title="组合箱总览" sub="组合箱上架 / 下架分布">
        <div class="split">
          <div class="split__item">
            <p class="split__num xh-figure">{{ bundleOv.sold }}</p>
            <p class="split__cap">已上架</p>
          </div>
          <div class="split__item split__item--muted">
            <p class="split__num xh-figure">{{ bundleOv.discontinued }}</p>
            <p class="split__cap">已下架</p>
          </div>
        </div>
        <div class="bar" aria-hidden="true">
          <span
            class="bar__fill"
            :style="{
              width:
                (bundleOv.sold + bundleOv.discontinued
                  ? (bundleOv.sold / (bundleOv.sold + bundleOv.discontinued)) * 100
                  : 0) + '%'
            }"
          ></span>
        </div>
      </PanelCard>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: var(--xh-6);
}

.alert {
  display: flex;
  align-items: center;
  gap: var(--xh-3);
  padding: var(--xh-3) var(--xh-4);
  border: 1px solid #eec7bb;
  border-left: 3px solid var(--xh-cinnabar-600);
  border-radius: var(--xh-radius-sm);
  background: var(--xh-cinnabar-50);
  animation: rise var(--xh-dur) var(--xh-ease-out) both;
}
.alert__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--xh-cinnabar-600);
}
.alert__text {
  font-size: var(--xh-text-sm);
  color: var(--xh-cinnabar-700);
}
.alert__num {
  font-weight: 600;
  font-size: var(--xh-text-lg);
}
.alert__btn {
  margin-left: auto;
  min-height: 30px;
  padding: 0 var(--xh-3);
  border: 1px solid #e0b3a4;
  border-radius: var(--xh-radius-xs);
  background: #fff;
  color: var(--xh-cinnabar-700);
  font-size: var(--xh-text-xs);
  cursor: pointer;
}
.alert__btn:hover {
  background: #fff5f2;
}

.tiles {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr 1fr 1fr;
  gap: var(--xh-4);
}
.tile {
  padding: var(--xh-5);
  background: var(--xh-surface);
  border: 1px solid var(--xh-line);
  border-radius: var(--xh-radius);
  display: flex;
  flex-direction: column;
  gap: var(--xh-2);
  min-height: 118px;
  justify-content: space-between;
  transition: border-color var(--xh-dur-fast) var(--xh-ease-out);
}
.tile:hover {
  border-color: var(--xh-line-strong);
}
.tile--wide {
  background: linear-gradient(180deg, #f3f9f6, var(--xh-surface));
  border-color: #cfe3da;
}
.tile__label {
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-400);
  letter-spacing: 0.04em;
}
.tile__value {
  display: flex;
  align-items: baseline;
  gap: var(--xh-1);
}
.tile__value .xh-figure {
  font-size: var(--xh-text-3xl);
}
.tile__value-sm {
  font-size: var(--xh-text-2xl) !important;
}
.tile__unit {
  font-size: var(--xh-text-sm);
  color: var(--xh-ink-400);
}

.grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: var(--xh-5);
}
.grid > :nth-child(3),
.grid > :nth-child(4) {
  grid-column: span 1;
}
@media (max-width: 1280px) {
  .tiles {
    grid-template-columns: repeat(3, 1fr);
  }
  .grid {
    grid-template-columns: 1fr;
  }
}

.ledger {
  list-style: none;
  margin: 0;
  padding: 0;
}
.ledger__item + .ledger__item {
  border-top: 1px solid var(--xh-line-soft);
}
.ledger__btn {
  width: 100%;
  min-height: 52px;
  padding: 0 var(--xh-5);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--xh-4);
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
}
.ledger__btn:hover {
  background: var(--xh-teal-50);
}
.ledger__label {
  font-size: var(--xh-text-sm);
  color: var(--xh-ink-700);
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.ledger__hint {
  font-style: normal;
  font-size: 11px;
  color: var(--xh-ink-300);
}
.ledger__value {
  font-size: var(--xh-text-xl);
}

.split {
  display: flex;
  gap: var(--xh-8);
}
.split__num {
  font-size: var(--xh-text-3xl);
  line-height: 1.1;
}
.split__item--muted .split__num {
  color: var(--xh-ink-400);
}
.split__cap {
  margin-top: var(--xh-1);
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-400);
}
.bar {
  margin-top: var(--xh-5);
  height: 6px;
  border-radius: 99px;
  background: var(--xh-paper-2);
  overflow: hidden;
}
.bar__fill {
  display: block;
  height: 100%;
  background: var(--xh-teal-600);
  transition: width var(--xh-dur-slow) var(--xh-ease-out);
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
</style>
