<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '@/api/admin'
import { CATEGORY_TYPE_MAP } from '@/constants/dict'
import StatusTag from '@/components/StatusTag.vue'
import PanelCard from '@/components/PanelCard.vue'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const busyId = ref(null)

const query = reactive({ name: '', type: undefined, page: 1, pageSize: 10 })

const dialog = reactive({ visible: false, mode: 'add', submitting: false })
const formRef = ref(null)
const form = reactive({ id: null, type: 1, name: '', sort: null })

const rules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 32, message: '不超过 32 个字符', trigger: 'blur' }
  ],
  type: [{ required: true, message: '请选择分类类型', trigger: 'change' }],
  /*
    排序值必填：库中该列为 NOT NULL 且无默认值，留空会被数据库直接拒绝。
    这里也不给默认值 —— 默认 0 等于替使用者做了「排在最前」的决定。
  */
  sort: [{ required: true, message: '请输入排序值', trigger: 'change' }]
}

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.name) params.name = query.name
    if (query.type !== undefined) params.type = query.type
    const data = await categoryApi.page(params)
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
  query.type = undefined
  search()
}

function openAdd() {
  dialog.mode = 'add'
  Object.assign(form, { id: null, type: 1, name: '', sort: null })
  dialog.visible = true
}

function openEdit(row) {
  dialog.mode = 'edit'
  Object.assign(form, { id: row.id, type: row.type, name: row.name, sort: row.sort })
  dialog.visible = true
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  dialog.submitting = true
  try {
    if (dialog.mode === 'add') {
      await categoryApi.add({ type: form.type, name: form.name, sort: form.sort })
      ElMessage.success('分类已新增')
    } else {
      await categoryApi.update({ id: form.id, name: form.name, sort: form.sort })
      ElMessage.success('分类已更新')
    }
    dialog.visible = false
    loadList()
  } finally {
    dialog.submitting = false
  }
}

async function onToggleStatus(row, next) {
  busyId.value = row.id
  try {
    await categoryApi.setStatus(next ? 1 : 0, row.id)
    row.status = next ? 1 : 0
    ElMessage.success(`分类「${row.name}」已${next ? '启用' : '禁用'}`)
  } catch {
    /* 拦截器已提示 */
  } finally {
    busyId.value = null
  }
}

async function onRemove(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除分类「${row.name}」？分类下若仍关联商品或组合箱，服务端会拒绝删除。`,
      '删除分类',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await categoryApi.remove(row.id)
  ElMessage.success('已删除')
  loadList()
}

onMounted(loadList)
</script>

<template>
  <div class="page">
    <PanelCard title="分类查询" sub="分类分两类：商品分类挂商品，组合分类挂组合箱" flush>
      <form class="filter" @submit.prevent="search">
        <div class="filter__item">
          <label class="filter__label" for="c-name">分类名称</label>
          <el-input id="c-name" v-model.trim="query.name" placeholder="模糊匹配" clearable />
        </div>
        <div class="filter__item">
          <label class="filter__label" for="c-type">分类类型</label>
          <el-select
            id="c-type"
            aria-label="按分类类型筛选"
            v-model="query.type"
            placeholder="全部类型"
            clearable
          >
            <el-option label="商品分类" :value="1" />
            <el-option label="组合分类" :value="2" />
          </el-select>
        </div>
        <div class="filter__actions">
          <el-button type="primary" native-type="submit">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </form>
    </PanelCard>

    <PanelCard :title="`分类列表（${total}）`" sub="排序值越小越靠前；禁用后用户端不再展示" flush>
      <template #extra>
        <el-button type="primary" @click="openAdd">新增分类</el-button>
      </template>

      <el-table v-loading="loading" :data="rows" row-key="id" empty-text="没有符合条件的分类">
        <el-table-column label="分类名称" min-width="180" prop="name" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <span class="type">{{ CATEGORY_TYPE_MAP[row.type]?.label || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="90" align="right">
          <template #default="{ row }">
            <span class="xh-num">{{ row.sort }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <div class="cell-status">
              <el-switch
                :model-value="row.status === 1"
                :loading="busyId === row.id"
                size="small"
                :aria-label="`切换 ${row.name} 启停状态`"
                @change="(v) => onToggleStatus(row, v)"
              />
              <StatusTag type="category" :value="row.status" />
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
            <el-button size="small" text @click="openEdit(row)">修改</el-button>
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

    <el-dialog
      v-model="dialog.visible"
      :title="dialog.mode === 'add' ? '新增分类' : '修改分类'"
      width="440px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类类型" prop="type">
          <el-radio-group v-model="form.type" :disabled="dialog.mode === 'edit'">
            <el-radio :value="1">商品分类</el-radio>
            <el-radio :value="2">组合分类</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model.trim="form.name" placeholder="如：当季水果" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="排序值" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" placeholder="必填" />
          <span class="hint">数值越小越靠前</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="submit">确认</el-button>
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
.cell-status {
  display: flex;
  align-items: center;
  gap: var(--xh-2);
}
.type {
  font-size: var(--xh-text-sm);
  color: var(--xh-ink-700);
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
.hint {
  margin-left: var(--xh-3);
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-400);
}
</style>
