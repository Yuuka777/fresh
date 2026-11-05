<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bundleApi, categoryApi } from '@/api/admin'
import { money } from '@/constants/dict'
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
  categories.value = await categoryApi.list(2)
}

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.name) params.name = query.name
    if (query.categoryId !== undefined) params.categoryId = query.categoryId
    if (query.status !== undefined) params.status = query.status
    const data = await bundleApi.page(params)
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
    await bundleApi.setStatus(next ? 1 : 0, row.id)
    row.status = next ? 1 : 0
    ElMessage.success(`「${row.name}」已${next ? '起售' : '停售'}`)
  } catch {
    /* 上架校验失败时回滚显示 */
    row.status = next ? 0 : 1
    loadList()
  } finally {
    busyId.value = null
  }
}

async function onRemove(row) {
  try {
    await ElMessageBox.confirm(`确认删除组合箱「${row.name}」？`, '删除组合箱', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await bundleApi.remove([row.id])
  ElMessage.success('已删除')
  loadList()
}

async function onBatchRemove() {
  if (!selected.value.length) {
    ElMessage.warning('请先勾选要删除的组合箱')
    return
  }
  const ids = selected.value.map((r) => r.id)
  try {
    await ElMessageBox.confirm(`已勾选 ${ids.length} 个组合箱，起售中的将被服务端拒绝删除。`, '批量删除', {
      confirmButtonText: '继续删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await bundleApi.remove(ids)
  ElMessage.success('已删除所选组合箱')
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
    <PanelCard title="组合箱查询" sub="组合箱是若干商品的打包售卖单元，温区由内含商品最高要求温区自动推导" flush>
      <form class="filter" @submit.prevent="search">
        <div class="filter__item">
          <label class="filter__label" for="b-name">组合箱名称</label>
          <el-input id="b-name" v-model.trim="query.name" placeholder="模糊匹配" clearable />
        </div>
        <div class="filter__item">
          <label class="filter__label" for="b-cat">所属分类</label>
          <el-select
            id="b-cat"
            aria-label="按所属分类筛选"
            v-model="query.categoryId"
            placeholder="全部分类"
            clearable
          >
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </div>
        <div class="filter__item">
          <label class="filter__label" for="b-status">起售状态</label>
          <el-select
            id="b-status"
            aria-label="按起售状态筛选"
            v-model="query.status"
            placeholder="全部状态"
            clearable
          >
            <el-option label="已起售" :value="1" />
            <el-option label="已停售" :value="0" />
          </el-select>
        </div>
        <div class="filter__actions">
          <el-button type="primary" native-type="submit">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </form>
    </PanelCard>

    <PanelCard :title="`组合箱列表（${total}）`" sub="内含商品未上架时无法起售" flush>
      <template #extra>
        <el-button plain :disabled="!selected.length" @click="onBatchRemove">批量删除</el-button>
        <el-button type="primary" @click="router.push('/bundle/add')">新增组合箱</el-button>
      </template>

      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        empty-text="没有符合条件的组合箱"
        @selection-change="(v) => (selected = v)"
      >
        <el-table-column type="selection" width="44" />
        <el-table-column label="组合箱" min-width="240">
          <template #default="{ row }">
            <div class="cell-bundle">
              <GoodsThumb :name="row.name" :image="row.image" :zone="row.storageZone" :size="44" />
              <div class="cell-bundle__text">
                <span class="cell-bundle__name">{{ row.name }}</span>
                <span class="cell-bundle__desc">{{ row.description || '—' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120" prop="categoryName" />
        <el-table-column label="整箱温区" width="106">
          <template #default="{ row }">
            <ZoneTag :zone="row.storageZone" />
          </template>
        </el-table-column>
        <el-table-column label="售价" width="104" align="right">
          <template #default="{ row }">
            <span class="xh-num price">{{ money(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="128">
          <template #default="{ row }">
            <div class="cell-status">
              <el-switch
                :model-value="row.status === 1"
                :loading="busyId === row.id"
                size="small"
                :aria-label="`切换 ${row.name} 起售状态`"
                @change="(v) => onToggleStatus(row, v)"
              />
              <StatusTag type="bundle" :value="row.status" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="164">
          <template #default="{ row }">
            <span class="xh-num dim">{{ row.updateTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="132" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              text
              @click="router.push({ path: '/bundle/add', query: { id: row.id } })"
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
.cell-bundle {
  display: flex;
  align-items: center;
  gap: var(--xh-3);
}
.cell-bundle__text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.cell-bundle__name {
  font-size: var(--xh-text-sm);
  color: var(--xh-ink-900);
}
.cell-bundle__desc {
  font-size: 11px;
  color: var(--xh-ink-300);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
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
