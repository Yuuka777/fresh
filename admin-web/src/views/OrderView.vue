<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '@/api/admin'
import { ORDER_TABS, DELIVERY_STATUS_MAP, money, yuan } from '@/constants/dict'
import StatusTag from '@/components/StatusTag.vue'
import PanelCard from '@/components/PanelCard.vue'
import { useOrderStatsStore } from '@/stores/orderStats'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const rows = ref([])
const total = ref(0)
/* 角标与顶栏共用一份状态：顶栏收到 WebSocket 来单提醒后，本页页签角标一起刷新 */
const stats = useOrderStatsStore()

const query = reactive({ number: '', phone: '', range: [], page: 1, pageSize: 10 })
const activeTab = ref('all')

const detailVisible = ref(false)
const detail = ref(null)
const detailLoading = ref(false)

/* 拒绝 / 取消 共用一个原因弹窗，用 mode 区分 */
const reasonDialog = reactive({ visible: false, mode: 'rejection', id: null, text: '', submitting: false })

const currentTab = computed(() => ORDER_TABS.find((t) => t.key === activeTab.value) || ORDER_TABS[0])

function badgeOf(tab) {
  if (!tab.badgeKey) return 0
  return stats[tab.badgeKey] || 0
}

const loadStats = () => stats.load()

async function loadList() {
  loading.value = true
  try {
    const params = {
      page: query.page,
      pageSize: query.pageSize
    }
    if (query.number) params.number = query.number
    if (query.phone) params.phone = query.phone
    if (currentTab.value.status !== undefined) params.status = currentTab.value.status
    if (query.range && query.range.length === 2) {
      params.beginTime = `${query.range[0]} 00:00:00`
      params.endTime = `${query.range[1]} 23:59:59`
    }
    const data = await orderApi.conditionSearch(params)
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function switchTab(key) {
  activeTab.value = key
  query.page = 1
  router.replace({ path: '/order', query: key === 'all' ? {} : { tab: key } })
  loadList()
}

function search() {
  query.page = 1
  loadList()
}

function reset() {
  query.number = ''
  query.phone = ''
  query.range = []
  query.page = 1
  loadList()
}

async function openDetail(id) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await orderApi.details(id)
  } finally {
    detailLoading.value = false
  }
}

async function afterAction(msg) {
  ElMessage.success(msg)
  await Promise.all([loadList(), loadStats()])
  if (detailVisible.value && detail.value) {
    detail.value = await orderApi.details(detail.value.id)
  }
}

async function onConfirm(row) {
  await orderApi.confirm(row.id)
  await afterAction(`订单 ${row.number} 已接单`)
  openNextTabIfEmpty('confirm', 'delivery')
}

async function onDelivery(row) {
  await orderApi.delivery(row.id)
  await afterAction(`订单 ${row.number} 已开始派送`)
  openNextTabIfEmpty('delivery', 'inProgress')
}

async function onComplete(row) {
  try {
    await ElMessageBox.confirm('确认该订单已送达并完成？完成后计入营业额统计。', '完成订单', {
      confirmButtonText: '确认完成',
      cancelButtonText: '取消',
      type: 'info'
    })
  } catch {
    return
  }
  await orderApi.complete(row.id)
  await afterAction(`订单 ${row.number} 已完成`)
}

/* 当前页签处理完最后一条时，自动跳到下一环节页签，贴近真实工作流 */
function openNextTabIfEmpty(fromKey, nextKey) {
  if (activeTab.value === fromKey && rows.value.length === 1) switchTab(nextKey)
}

function openReasonDialog(mode, id) {
  reasonDialog.mode = mode
  reasonDialog.id = id
  reasonDialog.text = ''
  reasonDialog.visible = true
}

async function submitReason() {
  const text = reasonDialog.text.trim()
  if (!text) {
    ElMessage.warning(reasonDialog.mode === 'rejection' ? '请填写拒单原因' : '请填写取消原因')
    return
  }
  reasonDialog.submitting = true
  try {
    if (reasonDialog.mode === 'rejection') {
      await orderApi.rejection({ id: reasonDialog.id, rejectionReason: text })
      await afterAction('已拒单，已支付订单同步标记为已退款')
    } else {
      await orderApi.cancel({ id: reasonDialog.id, cancelReason: text })
      await afterAction('订单已取消，已支付订单同步标记为已退款')
    }
    reasonDialog.visible = false
  } finally {
    reasonDialog.submitting = false
  }
}

const DETAIL_TIMELINE = [
  { key: 'orderTime', label: '下单' },
  { key: 'checkoutTime', label: '支付' },
  { key: 'estimatedDeliveryTime', label: '预计送达' },
  { key: 'deliveryTime', label: '送达' },
  { key: 'cancelTime', label: '取消' }
]

onMounted(() => {
  const tab = route.query.tab
  if (typeof tab === 'string' && ORDER_TABS.some((t) => t.key === tab)) activeTab.value = tab
  loadStats()
  loadList()

  /* 点顶栏的来单 / 催单提醒会带 orderId 跳进来，直接打开那一单的详情（契约第 10 节） */
  const orderId = route.query.orderId
  if (orderId) openDetail(Number(orderId))
})

/* 人已经在订单页时再点提醒，路由只换个 query、页面组件不会重建，
   onMounted 接不住，所以要单独盯 orderId */
watch(
  () => route.query.orderId,
  (orderId) => {
    if (orderId) openDetail(Number(orderId))
  }
)
</script>

<template>
  <div class="page">
    <!-- 页签：待付款是一等页签，与《接口文档》2.8 页签表一致 -->
    <nav class="tabs" aria-label="订单状态页签">
      <button
        v-for="t in ORDER_TABS"
        :key="t.key"
        type="button"
        class="tabs__item"
        :class="{ 'is-active': activeTab === t.key }"
        :aria-current="activeTab === t.key ? 'page' : undefined"
        @click="switchTab(t.key)"
      >
        <span class="tabs__label">{{ t.label }}</span>
        <span v-if="t.badgeKey" class="tabs__badge" :class="{ 'is-zero': badgeOf(t) === 0 }">
          {{ badgeOf(t) }}
        </span>
      </button>
    </nav>

    <PanelCard title="订单查询" sub="支持订单号 / 下单手机号 / 下单时间区间组合筛选" flush>
      <form class="filter" @submit.prevent="search">
        <div class="filter__item">
          <label class="filter__label" for="f-number">订单号</label>
          <el-input id="f-number" v-model.trim="query.number" placeholder="模糊匹配" clearable />
        </div>
        <div class="filter__item">
          <label class="filter__label" for="f-phone">下单手机号</label>
          <el-input id="f-phone" v-model.trim="query.phone" placeholder="模糊匹配" clearable />
        </div>
        <div class="filter__item filter__item--wide">
          <label class="filter__label" for="f-range">下单时间</label>
          <el-date-picker
            id="f-range"
            aria-label="下单时间区间"
            v-model="query.range"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :clearable="true"
          />
        </div>
        <div class="filter__actions">
          <el-button type="primary" native-type="submit">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </form>
    </PanelCard>

    <PanelCard :title="`${currentTab.label}（${total}）`" sub="待付款订单可查看与取消，但不可接单" flush>
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        class="orders"
        empty-text="当前条件下没有订单"
      >
        <el-table-column label="订单号" width="156">
          <template #default="{ row }">
            <button type="button" class="link xh-num" @click="openDetail(row.id)">
              {{ row.number }}
            </button>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="164">
          <template #default="{ row }">
            <span class="xh-num dim">{{ row.orderTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收货人" width="116">
          <template #default="{ row }">
            <div class="who">
              <span>{{ row.consignee }}</span>
              <span class="xh-num dim">{{ row.phone }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="送达地址" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.address }}</template>
        </el-table-column>
        <el-table-column label="实收金额" width="104" align="right">
          <template #default="{ row }">
            <span class="xh-num amount">{{ money(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <StatusTag type="order" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="支付" width="80">
          <template #default="{ row }">
            <StatusTag type="pay" :value="row.payStatus" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="208" fixed="right">
          <template #default="{ row }">
            <div class="ops">
              <el-button v-if="row.status === 2" size="small" type="primary" @click="onConfirm(row)">
                接单
              </el-button>
              <el-button v-if="row.status === 2" size="small" @click="openReasonDialog('rejection', row.id)">
                拒单
              </el-button>
              <el-button v-if="row.status === 3" size="small" type="primary" @click="onDelivery(row)">
                派送
              </el-button>
              <el-button v-if="row.status === 4" size="small" type="primary" @click="onComplete(row)">
                完成
              </el-button>
              <el-button v-if="row.status === 1 || row.status === 3" size="small" @click="openReasonDialog('cancel', row.id)">
                取消
              </el-button>
              <el-button size="small" text @click="openDetail(row.id)">详情</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <ListPager
        :total="total"
        :page="query.page"
        :page-size="query.pageSize"
        @change="(p) => { query.page = p; loadList() }"
        @size-change="(s) => { query.pageSize = s; query.page = 1; loadList() }"
      />
    </PanelCard>

    <!-- 订单详情抽屉 -->
    <el-drawer v-model="detailVisible" title="订单详情" size="520px" direction="rtl">
      <div v-if="detailLoading" class="detail-loading">正在读取订单…</div>
      <div v-else-if="detail" class="detail">
        <div class="detail__head">
          <span class="detail__number xh-num">{{ detail.number }}</span>
          <StatusTag type="order" :value="detail.status" />
        </div>

        <dl class="detail__meta">
          <div><dt>收货人</dt><dd>{{ detail.consignee }} · {{ detail.phone }}</dd></div>
          <div><dt>送达地址</dt><dd>{{ detail.address }}</dd></div>
          <div>
            <dt>配送时效</dt>
            <dd>{{ DELIVERY_STATUS_MAP[detail.deliveryStatus]?.label || '尽快送达' }}</dd>
          </div>
          <div><dt>客户备注</dt><dd>{{ detail.remark || '—' }}</dd></div>
          <div v-if="detail.cancelReason"><dt>取消原因</dt><dd>{{ detail.cancelReason }}</dd></div>
          <div v-if="detail.rejectionReason"><dt>拒单原因</dt><dd>{{ detail.rejectionReason }}</dd></div>
        </dl>

        <h3 class="detail__sub">时间节点</h3>
        <ul class="timeline">
          <li v-for="t in DETAIL_TIMELINE" :key="t.key" class="timeline__item">
            <span class="timeline__dot" :class="{ 'is-on': !!detail[t.key] }"></span>
            <span class="timeline__label">{{ t.label }}</span>
            <span class="timeline__time xh-num">{{ detail[t.key] || '—' }}</span>
          </li>
        </ul>

        <h3 class="detail__sub">商品明细</h3>
        <ul class="items">
          <li v-for="(it, i) in detail.orderItems" :key="i" class="items__row">
            <span class="items__name">
              {{ it.name }}
              <em v-if="it.specText" class="items__spec">{{ it.specText }}</em>
            </span>
            <span class="items__num xh-num">× {{ it.number }}</span>
            <span class="items__amount xh-num">{{ money(it.amount) }}</span>
          </li>
        </ul>

        <div class="sum">
          <span>运送费</span>
          <span class="xh-num">{{ money(detail.deliveryFee) }}</span>
        </div>
        <div class="sum sum--total">
          <span>实收金额</span>
          <span class="xh-num">{{ yuan(detail.amount) }}</span>
        </div>
      </div>
    </el-drawer>

    <!-- 拒单 / 取消 原因 -->
    <el-dialog
      v-model="reasonDialog.visible"
      :title="reasonDialog.mode === 'rejection' ? '拒单' : '取消订单'"
      width="440px"
    >
      <p class="dialog-hint">
        {{
          reasonDialog.mode === 'rejection'
            ? '拒单后订单置为已取消；若订单已支付，支付状态同步置为已退款。'
            : '取消后订单置为已取消；若订单已支付，支付状态同步置为已退款。'
        }}
      </p>
      <el-input
        v-model="reasonDialog.text"
        type="textarea"
        :rows="3"
        maxlength="60"
        show-word-limit
        :placeholder="reasonDialog.mode === 'rejection' ? '例如：门店备货不足' : '例如：与客户协商一致取消'"
      />
      <template #footer>
        <el-button @click="reasonDialog.visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="reasonDialog.submitting"
          @click="submitReason"
        >
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: var(--xh-5);
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  border: 1px solid var(--xh-line);
  border-radius: var(--xh-radius);
  background: var(--xh-surface);
  overflow: hidden;
}
.tabs__item {
  display: inline-flex;
  align-items: center;
  gap: var(--xh-2);
  min-height: 46px;
  padding: 0 var(--xh-4);
  border: 0;
  border-right: 1px solid var(--xh-line-soft);
  background: transparent;
  color: var(--xh-ink-500);
  font-size: var(--xh-text-sm);
  cursor: pointer;
  transition: background-color var(--xh-dur-fast) var(--xh-ease-out);
}
.tabs__item:hover {
  background: var(--xh-teal-50);
  color: var(--xh-teal-800);
}
.tabs__item.is-active {
  background: var(--xh-teal-800);
  color: #f2f9f6;
}
.tabs__badge {
  min-width: 20px;
  height: 18px;
  padding: 0 5px;
  display: inline-grid;
  place-items: center;
  border-radius: var(--xh-radius-xs);
  background: var(--xh-cinnabar-600);
  color: #fff;
  font-family: var(--xh-font-mono);
  font-size: 11px;
}
.tabs__badge.is-zero {
  background: rgba(10, 31, 25, 0.08);
  color: var(--xh-ink-400);
}
.tabs__item.is-active .tabs__badge {
  background: var(--xh-teal-200);
  color: var(--xh-teal-900);
}

.filter {
  display: flex;
  flex-wrap: wrap;
  gap: var(--xh-4);
  padding: var(--xh-5);
  align-items: flex-end;
}
.filter__item {
  display: flex;
  flex-direction: column;
  gap: var(--xh-2);
  min-width: 180px;
}
.filter__item--wide {
  min-width: 300px;
}
.filter__item--wide :deep(.el-date-editor) {
  width: 340px;
}
.filter__label {
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-400);
  letter-spacing: 0.04em;
}
.filter__actions {
  margin-left: auto;
  display: flex;
  gap: var(--xh-2);
}

.orders :deep(.el-table__cell) {
  padding: 10px 0;
}
.link {
  border: 0;
  background: transparent;
  padding: 0;
  color: var(--xh-teal-600);
  font-size: var(--xh-text-sm);
  cursor: pointer;
}
.link:hover {
  text-decoration: underline;
}
.who {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: var(--xh-text-sm);
}
.dim {
  color: var(--xh-ink-400);
  font-size: var(--xh-text-xs);
}
.amount {
  font-weight: 600;
  color: var(--xh-ink-900);
}
.ops {
  display: flex;
  flex-wrap: wrap;
  /* 间距只由 Element 的 button + button margin-left 提供，
     这里再设 gap 会叠加成 16px，导致三个按钮差 2px 被折行 */
  gap: 0;
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding: var(--xh-4) var(--xh-5);
  border-top: 1px solid var(--xh-line-soft);
}

.detail-loading {
  padding: var(--xh-8);
  text-align: center;
  color: var(--xh-ink-400);
  font-size: var(--xh-text-sm);
}
.detail__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--xh-3);
  padding-bottom: var(--xh-4);
  border-bottom: 1px solid var(--xh-line);
}
.detail__number {
  font-size: var(--xh-text-lg);
  font-weight: 600;
}
.detail__meta {
  margin: var(--xh-4) 0 0;
  display: flex;
  flex-direction: column;
  gap: var(--xh-3);
}
.detail__meta > div {
  display: grid;
  grid-template-columns: 76px 1fr;
  gap: var(--xh-3);
  font-size: var(--xh-text-sm);
}
.detail__meta dt {
  color: var(--xh-ink-400);
}
.detail__meta dd {
  margin: 0;
  color: var(--xh-ink-800);
}
.detail__sub {
  margin: var(--xh-6) 0 var(--xh-3);
  font-family: var(--xh-font-display);
  font-size: var(--xh-text-base);
}

.timeline {
  list-style: none;
  margin: 0;
  padding: 0;
  border-left: 1px solid var(--xh-line);
}
.timeline__item {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--xh-3);
  padding: var(--xh-2) 0 var(--xh-2) var(--xh-4);
  font-size: var(--xh-text-sm);
}
.timeline__dot {
  position: absolute;
  left: -4px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--xh-line-strong);
}
.timeline__dot.is-on {
  background: var(--xh-teal-600);
}
.timeline__label {
  color: var(--xh-ink-500);
  min-width: 62px;
}
.timeline__time {
  color: var(--xh-ink-800);
}

.items {
  list-style: none;
  margin: 0;
  padding: 0;
  border: 1px solid var(--xh-line);
  border-radius: var(--xh-radius-sm);
}
.items__row {
  display: flex;
  align-items: center;
  gap: var(--xh-3);
  padding: var(--xh-3) var(--xh-3);
  font-size: var(--xh-text-sm);
}
.items__row + .items__row {
  border-top: 1px solid var(--xh-line-soft);
}
.items__name {
  flex: 1;
  min-width: 0;
}
.items__spec {
  margin-left: var(--xh-2);
  font-style: normal;
  font-size: 11px;
  color: var(--xh-ink-400);
}
.items__num {
  color: var(--xh-ink-400);
}
.items__amount {
  min-width: 64px;
  text-align: right;
}
.sum {
  display: flex;
  justify-content: space-between;
  padding: var(--xh-3) var(--xh-3) 0;
  font-size: var(--xh-text-sm);
  color: var(--xh-ink-500);
}
.sum--total {
  margin-top: var(--xh-3);
  padding-top: var(--xh-3);
  border-top: 1px solid var(--xh-line);
  font-size: var(--xh-text-base);
  color: var(--xh-ink-900);
}
.sum--total .xh-num {
  font-size: var(--xh-text-xl);
  font-weight: 600;
}

.dialog-hint {
  margin-bottom: var(--xh-4);
  padding: var(--xh-3);
  border-radius: var(--xh-radius-sm);
  background: var(--xh-amber-50);
  color: var(--xh-amber-700);
  font-size: var(--xh-text-xs);
  line-height: 1.7;
}
</style>
