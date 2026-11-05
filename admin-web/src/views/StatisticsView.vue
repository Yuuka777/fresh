<script setup>
import { computed, onMounted, ref } from 'vue'
import { reportApi } from '@/api/admin'
import { money, yuan } from '@/constants/dict'
import EChart from '@/components/EChart.vue'
import PanelCard from '@/components/PanelCard.vue'

const loading = ref(false)
const range = ref([])

const turnover = ref({ dateList: '', turnoverList: '' })
const members = ref({ dateList: '', newMemberList: '', totalMemberList: '' })
const orders = ref({ dateList: '', orderCountList: '', validOrderCountList: '', totalOrderCount: 0, validOrderCount: 0, orderCompletionRate: 0 })
const top10 = ref([])

const dates = computed(() => String(turnover.value.dateList || '').split(',').filter(Boolean))
const shortDates = computed(() => dates.value.map((d) => d.slice(5)))
const num = (s) => String(s || '').split(',').map(Number)

/* 统一图表基线：字体、线宽、网格，避免四张图各长一副样子 */
const baseFont = { fontFamily: 'JetBrains Mono', fontSize: 11, color: '#8fa39a' }
const axisLine = { lineStyle: { color: '#dce4dd' } }
const splitLine = { lineStyle: { color: '#eef1ed' } }
const tooltip = {
  trigger: 'axis',
  backgroundColor: '#ffffff',
  borderColor: '#dce4dd',
  borderWidth: 1,
  textStyle: { color: '#0a1f19', fontSize: 12 }
}

const turnoverOption = computed(() => ({
  grid: { left: 8, right: 12, top: 28, bottom: 8, containLabel: true },
  tooltip: { ...tooltip, valueFormatter: (v) => yuan(v) },
  xAxis: { type: 'category', data: shortDates.value, axisLine, axisTick: { show: false }, axisLabel: baseFont },
  yAxis: { type: 'value', splitLine, axisLabel: baseFont },
  series: [
    {
      name: '营业额',
      type: 'line',
      smooth: true,
      symbolSize: 5,
      data: num(turnover.value.turnoverList),
      lineStyle: { width: 2, color: '#0f6e56' },
      itemStyle: { color: '#0f6e56' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(15,110,86,0.18)' },
            { offset: 1, color: 'rgba(15,110,86,0)' }
          ]
        }
      }
    }
  ]
}))

const orderOption = computed(() => ({
  grid: { left: 8, right: 12, top: 40, bottom: 8, containLabel: true },
  tooltip,
  legend: { top: 0, right: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11, color: '#6b8177' } },
  xAxis: { type: 'category', data: shortDates.value, axisLine, axisTick: { show: false }, axisLabel: baseFont },
  yAxis: { type: 'value', splitLine, axisLabel: baseFont },
  series: [
    {
      name: '订单总数',
      type: 'bar',
      barMaxWidth: 14,
      data: num(orders.value.orderCountList),
      itemStyle: { color: '#cfdcd6', borderRadius: [2, 2, 0, 0] }
    },
    {
      name: '有效订单',
      type: 'bar',
      barMaxWidth: 14,
      data: num(orders.value.validOrderCountList),
      itemStyle: { color: '#0f6e56', borderRadius: [2, 2, 0, 0] }
    }
  ]
}))

const memberOption = computed(() => ({
  grid: { left: 8, right: 12, top: 40, bottom: 8, containLabel: true },
  tooltip,
  legend: { top: 0, right: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11, color: '#6b8177' } },
  xAxis: { type: 'category', data: shortDates.value, axisLine, axisTick: { show: false }, axisLabel: baseFont },
  yAxis: [
    { type: 'value', name: '新增', nameTextStyle: baseFont, splitLine, axisLabel: baseFont },
    { type: 'value', name: '累计', nameTextStyle: baseFont, splitLine: { show: false }, axisLabel: baseFont }
  ],
  series: [
    {
      name: '每日新增',
      type: 'bar',
      barMaxWidth: 14,
      data: num(members.value.newMemberList),
      itemStyle: { color: '#9fe1cb', borderRadius: [2, 2, 0, 0] }
    },
    {
      name: '累计会员',
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      symbolSize: 5,
      data: num(members.value.totalMemberList),
      lineStyle: { width: 2, color: '#b23a22' },
      itemStyle: { color: '#b23a22' }
    }
  ]
}))

const top10Option = computed(() => {
  const list = [...top10.value].reverse()
  return {
    grid: { left: 8, right: 32, top: 12, bottom: 8, containLabel: true },
    tooltip: { ...tooltip, trigger: 'item' },
    xAxis: { type: 'value', splitLine, axisLabel: baseFont },
    yAxis: {
      type: 'category',
      data: list.map((i) => i.name),
      axisLine,
      axisTick: { show: false },
      axisLabel: { ...baseFont, fontFamily: 'Noto Sans SC' }
    },
    series: [
      {
        name: '销量',
        type: 'bar',
        barMaxWidth: 16,
        data: list.map((i) => i.number),
        itemStyle: { color: '#0f6e56', borderRadius: [0, 2, 2, 0] },
        label: { show: true, position: 'right', ...baseFont, color: '#4c6459' }
      }
    ]
  }
})

async function load() {
  loading.value = true
  try {
    const params = {}
    if (range.value && range.value.length === 2) {
      params.begin = range.value[0]
      params.end = range.value[1]
    }
    const [t, m, o, top] = await Promise.all([
      reportApi.turnover(params),
      reportApi.members(params),
      reportApi.orders(params),
      reportApi.top10(params)
    ])
    turnover.value = t
    members.value = m
    orders.value = o
    top10.value = top || []
  } finally {
    loading.value = false
  }
}

function quick(days) {
  const end = new Date()
  const begin = new Date(end.getTime() - (days - 1) * 86400000)
  const f = (d) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  range.value = [f(begin), f(end)]
  load()
}

onMounted(load)
</script>

<template>
  <div class="page" v-loading="loading">
    <PanelCard title="统计区间" sub="默认展示近 30 天，可自定义区间；无订单的日期营业额补 0">
      <div class="toolbar">
        <el-date-picker
          v-model="range"
          aria-label="统计区间"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :clearable="true"
        />
        <el-button type="primary" @click="load">查询</el-button>
        <div class="quick">
          <span class="quick__label">快捷</span>
          <el-button size="small" @click="quick(7)">近 7 天</el-button>
          <el-button size="small" @click="quick(30)">近 30 天</el-button>
        </div>
      </div>

      <div class="summary">
        <div class="summary__item">
          <span class="summary__label">区间订单总数</span>
          <span class="xh-figure summary__value">{{ orders.totalOrderCount }}</span>
        </div>
        <div class="summary__item">
          <span class="summary__label">区间有效订单</span>
          <span class="xh-figure summary__value">{{ orders.validOrderCount }}</span>
        </div>
        <div class="summary__item">
          <span class="summary__label">订单完成率</span>
          <span class="xh-figure summary__value">
            {{ (orders.orderCompletionRate * 100).toFixed(1) }}
            <em class="summary__unit">%</em>
          </span>
        </div>
      </div>
    </PanelCard>

    <div class="grid">
      <PanelCard title="营业额趋势" sub="按已完成订单金额统计（元）">
        <EChart :option="turnoverOption" height="260px" />
      </PanelCard>

      <PanelCard title="订单量趋势" sub="有效订单 = 已完成订单">
        <EChart :option="orderOption" height="260px" />
      </PanelCard>

      <PanelCard title="会员增长" sub="柱：每日新增；线：累计会员（右轴）">
        <EChart :option="memberOption" height="260px" />
      </PanelCard>

      <PanelCard title="销量 TOP10" sub="按已完成订单明细聚合商品销量">
        <EChart v-if="top10.length" :option="top10Option" height="300px" />
        <p v-else class="empty">区间内没有已完成订单，暂无销量数据</p>
      </PanelCard>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: var(--xh-5);
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--xh-3);
}
/* 日期区间默认会撑满整行，收窄成与其它筛选控件同一量级 */
.toolbar :deep(.el-date-editor) {
  width: 340px;
}
.quick {
  display: flex;
  align-items: center;
  gap: var(--xh-2);
  margin-left: auto;
}
.quick__label {
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-400);
}
.summary {
  display: flex;
  gap: var(--xh-10);
  margin-top: var(--xh-5);
  padding-top: var(--xh-5);
  border-top: 1px solid var(--xh-line);
}
.summary__item {
  display: flex;
  flex-direction: column;
  gap: var(--xh-1);
}
.summary__label {
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-400);
}
.summary__value {
  font-size: var(--xh-text-2xl);
}
.summary__unit {
  font-style: normal;
  font-size: var(--xh-text-base);
  color: var(--xh-ink-400);
  margin-left: 2px;
}
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--xh-5);
}
@media (max-width: 1180px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
.empty {
  padding: var(--xh-10) 0;
  text-align: center;
  font-size: var(--xh-text-sm);
  color: var(--xh-ink-300);
}
</style>
