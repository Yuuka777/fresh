<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { staffApi } from '@/api/admin'
import { useUserStore } from '@/stores/user'
import StatusTag from '@/components/StatusTag.vue'
import PanelCard from '@/components/PanelCard.vue'

const router = useRouter()
const user = useUserStore()

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const busyId = ref(null)

/* 三个筛选条件均为模糊匹配，可任意组合 */
const query = reactive({ name: '', username: '', phone: '', page: 1, pageSize: 10 })

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.name) params.name = query.name
    if (query.username) params.username = query.username
    if (query.phone) params.phone = query.phone
    const data = await staffApi.page(params)
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

async function onToggleStatus(row, next) {
  if (row.id === user.id && !next) {
    ElMessage.warning('不能禁用当前登录的账号')
    row.status = 1
    return
  }
  busyId.value = row.id
  try {
    await staffApi.setStatus(next ? 1 : 0, row.id)
    row.status = next ? 1 : 0
    ElMessage.success(`「${row.name}」账号已${next ? '启用' : '禁用'}`)
  } catch {
    row.status = next ? 0 : 1
  } finally {
    busyId.value = null
  }
}

/*
  删除店员：物理删除，不可恢复。
  业务校验（不能删自己、启用中不能删）由服务端负责——前端只做一次确认，
  并把规则写进确认文案，避免使用者反复试错。
*/
async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除店员「${row.name}」？删除后无法恢复。启用中的账号需先禁用才能删除。`,
      '删除店员',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    // 使用者点了取消
    return
  }
  try {
    await staffApi.remove(row.id)
    ElMessage.success(`已删除「${row.name}」`)
    // 删掉当页最后一条时回退一页，避免停在空页上
    if (rows.value.length === 1 && query.page > 1) query.page -= 1
    loadList()
  } catch {
    /* 失败原因由请求层统一提示 */
  }
}

onMounted(loadList)
</script>

<template>
  <div class="page">
    <PanelCard title="店员查询" sub="新增店员的初始密码为 123456、初始状态为禁用，需手动启用后才能登录" flush>
      <form class="filter" @submit.prevent="search">
        <div class="filter__item">
          <label class="filter__label" for="s-name">姓名</label>
          <el-input id="s-name" v-model.trim="query.name" placeholder="模糊匹配" clearable />
        </div>
        <div class="filter__item">
          <label class="filter__label" for="s-username">用户名</label>
          <el-input id="s-username" v-model.trim="query.username" placeholder="模糊匹配" clearable />
        </div>
        <div class="filter__item">
          <label class="filter__label" for="s-phone">手机号</label>
          <el-input id="s-phone" v-model.trim="query.phone" placeholder="模糊匹配" clearable />
        </div>
        <div class="filter__actions">
          <el-button type="primary" native-type="submit">查询</el-button>
        </div>
      </form>
    </PanelCard>

    <PanelCard :title="`店员列表（${total}）`" flush>
      <template #extra>
        <el-button type="primary" @click="router.push('/staff/add')">新增店员</el-button>
      </template>

      <el-table v-loading="loading" :data="rows" row-key="id" empty-text="没有查询到店员">
        <el-table-column label="姓名" min-width="120">
          <template #default="{ row }">
            <span class="name">
              {{ row.name }}
              <span v-if="row.id === user.id" class="name__me">当前登录</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="用户名" width="150">
          <template #default="{ row }">
            <span class="xh-num dim">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="150">
          <template #default="{ row }">
            <span class="xh-num">{{ row.phone }}</span>
          </template>
        </el-table-column>
        <el-table-column label="性别" width="80">
          <template #default="{ row }">
            <span class="dim">{{ row.sex === '女' ? '女' : '男' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="身份证号" width="190">
          <template #default="{ row }">
            <span class="xh-num dim">{{ row.idNumber }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <div class="cell-status">
              <el-switch
                :model-value="row.status === 1"
                :loading="busyId === row.id"
                size="small"
                :aria-label="`切换 ${row.name} 账号状态`"
                @change="(v) => onToggleStatus(row, v)"
              />
              <StatusTag type="staff" :value="row.status" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="132" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              text
              @click="router.push({ path: '/staff/add', query: { id: row.id } })"
            >
              修改
            </el-button>
            <el-button
              size="small"
              text
              type="danger"
              :disabled="row.id === user.id"
              :title="row.id === user.id ? '不能删除当前登录账号' : ''"
              @click="onDelete(row)"
            >
              删除
            </el-button>
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
  min-width: 220px;
}
.filter__label {
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-400);
  letter-spacing: 0.04em;
}
.filter__actions {
  margin-left: auto;
}
.cell-status {
  display: flex;
  align-items: center;
  gap: var(--xh-2);
}
.name {
  font-size: var(--xh-text-sm);
  color: var(--xh-ink-900);
}
.name__me {
  margin-left: var(--xh-2);
  padding: 1px 5px;
  border-radius: var(--xh-radius-xs);
  background: var(--xh-teal-50);
  color: var(--xh-teal-800);
  font-size: 11px;
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
