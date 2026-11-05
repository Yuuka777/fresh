<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi, goodsApi } from '@/api/admin'
import { money, zoneLabel } from '@/constants/dict'
import GoodsThumb from '@/components/GoodsThumb.vue'
import ZoneTag from '@/components/ZoneTag.vue'
import StatusTag from '@/components/StatusTag.vue'
import PanelCard from '@/components/PanelCard.vue'

const router = useRouter()

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const categories = ref([])
const selected = ref([])
const busyId = ref(null)

const query = reactive({ name: '', categoryId: undefined, status: undefined, page: 1, pageSize: 10 })

async function loadCategories() {
  categories.value = await categoryApi.list(1)
}

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.name) params.name = query.name
    if (query.categoryId !== undefined) params.categoryId = query.categoryId
    if (query.status !== undefined) params.status = query.status
    const data = await goodsApi.page(params)
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  loadList()
}

function reset() {
  query.name = ''
  query.categoryId = undefined
  query.status = undefined
  search()
}

async function onToggleStatus(row, next) {
  busyId.value = row.id
  try {
    await goodsApi.setStatus(next ? 1 : 0, row.id)
    row.status = next ? 1 : 0
    ElMessage.success(`「${row.name}」已${next ? '上架' : '下架'}`)
  } catch {
    /* 下架可能被「仍被起售中组合箱引用」拦下：回滚开关，并重新拉取对齐服务端 */
    row.status = next ? 0 : 1
    loadList()
  } finally {
    busyId.value = null
  }
}

async function onRemove(row) {
  try {
    await ElMessageBox.confirm(`确认删除商品「${row.name}」？删除后其规格一并移除。`, '删除商品', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await goodsApi.remove([row.id])
  ElMessage.success('已删除')
  loadList()
}

async function onBatchRemove() {
  if (!selected.value.length) {
    ElMessage.warning('请先勾选要删除的商品')
    return
  }
  const ids = selected.value.map((r) => r.id)
  try {
    await ElMessageBox.confirm(
      `已勾选 ${ids.length} 个商品。上架中的商品、以及被组合箱引用的商品无法删除，将由服务端拒绝，是否继续？`,
      '批量删除',
      { confirmButtonText: '继续删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await goodsApi.remove(ids)
  ElMessage.success('已删除所选商品')
  selected.value = []
  loadList()
}

onMounted(async () => {
  await loadCategories()
  loadList()
})
</script>

<template>
  <div class="page">
    <PanelCard title="商品查询" sub="按名称、分类、上架状态筛选" flush>
      <form class="filter" @submit.prevent="search">
        <div class="filter__item">
          <label class="filter__label" for="g-name">商品名称</label>
          <el-input id="g-name" v-model.trim="query.name" placeholder="模糊匹配" clearable />
        </div>
        <div class="filter__item">
          <label class="filter__label" for="g-cat">所属分类</label>
          <el-select
            id="g-cat"
            aria-label="按所属分类筛选"
            v-model="query.categoryId"
            placeholder="全部分类"
            clearable
          >
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </div>
        <div class="filter__item">
          <label class="filter__label" for="g-status">上架状态</label>
          <el-select
            id="g-status"
            aria-label="按上架状态筛选"
            v-model="query.status"
            placeholder="全部状态"
            clearable
          >
            <el-option label="已上架" :value="1" />
            <el-option label="已下架" :value="0" />
          </el-select>
        </div>
        <div class="filter__actions">
          <el-button type="primary" native-type="submit">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </form>
    </PanelCard>

    <PanelCard :title="`商品列表（${total}）`" flush>
      <template #extra>
        <el-button plain :disabled="!selected.length" @click="onBatchRemove">批量删除</el-button>
        <el-button type="primary" @click="router.push('/goods/add')">新增商品</el-button>
      </template>

      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        empty-text="没有符合条件的商品"
        @selection-change="(v) => (selected = v)"
      >
        <el-table-column type="selection" width="44" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="cell-goods">
              <GoodsThumb :name="row.name" :image="row.image" :zone="row.storageZone" :size="44" />
              <div class="cell-goods__text">
                <span class="cell-goods__name">{{ row.name }}</span>
                <span class="cell-goods__desc">{{ row.description || '—' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="100" prop="categoryName" />
        <el-table-column label="产地" width="100">
          <template #default="{ row }">
            <span class="dim">{{ row.origin || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="温区" width="76">
          <template #default="{ row }">
            <ZoneTag :zone="row.storageZone" />
          </template>
        </el-table-column>
        <el-table-column label="保质期" width="100">
          <template #default="{ row }">
            <span class="dim">{{ row.shelfLife || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="售价" width="88" align="right">
          <template #default="{ row }">
            <span class="xh-num price">{{ money(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="132">
          <template #default="{ row }">
            <div class="cell-status">
              <el-switch
                :model-value="row.status === 1"
                :loading="busyId === row.id"
                size="small"
                :aria-label="`切换 ${row.name} 上架状态`"
                @change="(v) => onToggleStatus(row, v)"
              />
              <StatusTag type="goods" :value="row.status" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="144">
          <template #default="{ row }">
            <span class="xh-num dim">{{ row.updateTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="124" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              text
              @click="router.push({ path: '/goods/add', query: { id: row.id } })"
            >
              修改
            </el-button>
            <el-button size="small" text type="danger" @click="onRemove(row)">删除</el-button>
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
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: var(--xh-5);
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
  min-width: 190px;
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
.cell-goods {
  display: flex;
  align-items: center;
  gap: var(--xh-3);
}
.cell-goods__text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.cell-goods__name {
  font-size: var(--xh-text-sm);
  color: var(--xh-ink-900);
}
.cell-goods__desc {
  font-size: 11px;
  color: var(--xh-ink-300);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 120px;
}
.cell-status {
  display: flex;
  align-items: center;
  gap: var(--xh-2);
}
.price {
  font-weight: 600;
  color: var(--xh-ink-900);
}
.dim {
  color: var(--xh-ink-400);
  font-size: var(--xh-text-xs);
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding: var(--xh-4) var(--xh-5);
  border-top: 1px solid var(--xh-line-soft);
}
</style>
