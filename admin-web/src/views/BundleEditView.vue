<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { bundleApi, categoryApi, commonApi, goodsApi } from '@/api/admin'
import { money } from '@/constants/dict'
import GoodsThumb from '@/components/GoodsThumb.vue'
import StatusTag from '@/components/StatusTag.vue'
import ZoneTag from '@/components/ZoneTag.vue'
import PanelCard from '@/components/PanelCard.vue'

const route = useRoute()
const router = useRouter()

const editId = computed(() => (route.query.id ? Number(route.query.id) : null))
const isEdit = computed(() => editId.value !== null)

const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const categories = ref([])
const goodsPool = ref([])
const picker = reactive({ visible: false, keyword: '', checked: [], loading: false })

const form = reactive({
  name: '',
  categoryId: undefined,
  price: undefined,
  image: null,
  description: '',
  bundleGoods: []
})

const rules = {
  name: [
    { required: true, message: '请输入组合箱名称', trigger: 'blur' },
    { max: 32, message: '不超过 32 个字符', trigger: 'blur' }
  ],
  categoryId: [{ required: true, message: '请选择所属组合分类', trigger: 'change' }],
  price: [
    { required: true, message: '请输入售价', trigger: 'blur' },
    {
      validator: (_r, v, cb) => (Number(v) > 0 ? cb() : cb(new Error('售价需大于 0'))),
      trigger: 'blur'
    }
  ]
}

/* 整箱温区：按内含商品的最高要求温区推导（常温 1 < 冷藏 2 < 冷冻 3） */
const derivedZone = computed(() => {
  if (!form.bundleGoods.length) return null
  const zones = form.bundleGoods.map((i) => {
    const g = goodsPool.value.find((x) => x.id === i.goodsId)
    return g ? g.storageZone : 1
  })
  return Math.max(...zones)
})

const filteredPool = computed(() => {
  const kw = picker.keyword.trim()
  const chosen = new Set(form.bundleGoods.map((i) => i.goodsId))
  return goodsPool.value
    .filter((g) => (kw ? g.name.includes(kw) : true))
    .map((g) => ({ ...g, disabled: chosen.has(g.id) }))
})

const poolPriceSum = computed(() =>
  form.bundleGoods.reduce((s, i) => s + Number(i.price) * Number(i.copies), 0)
)

async function onUpload({ file }) {
  uploading.value = true
  try {
    form.image = await commonApi.upload(file)
    ElMessage.success('图片已上传')
  } finally {
    uploading.value = false
  }
}

/* —— 商品选择弹层 ——
   选择状态以 picker.checked 为准（而非 el-table 内部状态），
   这样切换关键字筛选时不会丢掉先前勾选的商品 */
const pickerTableRef = ref(null)

async function syncPickerSelection() {
  await nextTick()
  if (!pickerTableRef.value) return
  filteredPool.value.forEach((g) => {
    const should = picker.checked.includes(g.id)
    const is = pickerTableRef.value.getSelectionRows().some((r) => r.id === g.id)
    if (should !== is) pickerTableRef.value.toggleRowSelection(g, should)
  })
}

function onPickerSelect(selection) {
  const visibleIds = new Set(filteredPool.value.map((r) => r.id))
  const selectedIds = selection.map((r) => r.id)
  picker.checked = picker.checked.filter((id) => !visibleIds.has(id)).concat(selectedIds)
}

async function openPicker() {
  picker.checked = form.bundleGoods.map((i) => i.goodsId)
  picker.keyword = ''
  picker.visible = true
  await syncPickerSelection()
}

watch(() => picker.keyword, syncPickerSelection)

function confirmPicker() {
  const picked = goodsPool.value.filter((g) => picker.checked.includes(g.id))
  form.bundleGoods = picked.map((g) => {
    const exist = form.bundleGoods.find((i) => i.goodsId === g.id)
    return {
      goodsId: g.id,
      name: g.name,
      price: g.price,
      copies: exist ? exist.copies : 1
    }
  })
  picker.visible = false
}

function removeLine(i) {
  form.bundleGoods.splice(i, 1)
}

async function loadDetail() {
  loading.value = true
  try {
    const data = await bundleApi.getById(editId.value)
    Object.assign(form, {
      name: data.name,
      categoryId: data.categoryId,
      price: data.price,
      image: data.image,
      description: data.description,
      bundleGoods: (data.bundleGoods || []).map((r) => ({
        goodsId: r.goodsId,
        name: r.name,
        price: r.price,
        copies: r.copies
      }))
    })
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (!form.bundleGoods.length) {
    ElMessage.warning('请至少为组合箱添加一个商品')
    return
  }
  submitting.value = true
  try {
    const payload = {
      name: form.name,
      categoryId: form.categoryId,
      price: form.price,
      image: form.image,
      description: form.description,
      bundleGoods: form.bundleGoods.map((i) => ({
        goodsId: i.goodsId,
        name: i.name,
        price: i.price,
        copies: Number(i.copies)
      }))
    }
    if (isEdit.value) {
      await bundleApi.update({ ...payload, id: editId.value })
      ElMessage.success('组合箱已更新，整箱温区已重新推导')
    } else {
      await bundleApi.add(payload)
      ElMessage.success('组合箱已新增')
    }
    router.push('/bundle')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const [cats, goods] = await Promise.all([categoryApi.list(2), goodsApi.list()])
  categories.value = cats
  goodsPool.value = goods
  if (isEdit.value) await loadDetail()
})
</script>

<template>
  <div class="page" v-loading="loading">
    <PanelCard
      :title="isEdit ? '修改组合箱' : '新增组合箱'"
      :sub="
        isEdit
          ? '整箱温区按内含商品的最高温区重新推导，起售状态请在列表中切换'
          : '新增后为「停售」状态，需在列表中起售；整箱温区按内含商品自动推导'
      "
    >
      <template #extra>
        <el-button @click="router.push('/bundle')">返回列表</el-button>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <div class="cols">
          <div>
            <el-form-item label="组合箱名称" for="bundle-name" prop="name">
              <el-input
                id="bundle-name"
                v-model.trim="form.name"
                placeholder="如：一周蔬菜箱"
                maxlength="32"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="所属分类" for="bundle-category" prop="categoryId">
              <el-select
                id="bundle-category"
                aria-label="所属分类"
                v-model="form.categoryId"
                placeholder="请选择组合分类"
                class="w-full"
              >
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="售价" for="bundle-price" prop="price">
              <el-input id="bundle-price" v-model="form.price" placeholder="0.00">
                <template #prepend>¥</template>
              </el-input>
            </el-form-item>
            <el-form-item label="描述" for="bundle-desc">
              <el-input
                id="bundle-desc"
                v-model.trim="form.description"
                type="textarea"
                :rows="3"
                maxlength="255"
                show-word-limit
                placeholder="一句话说明搭配场景，如：三口之家一周蔬菜"
              />
            </el-form-item>
          </div>

          <div>
            <el-form-item label="组合箱图">
              <div class="uploader">
                <GoodsThumb
                  :name="form.name || '箱'"
                  :image="form.image"
                  :zone="derivedZone || 1"
                  :size="120"
                />
                <div class="uploader__ops">
                  <el-upload
                    :show-file-list="false"
                    :http-request="onUpload"
                    accept="image/jpeg,image/png,image/webp"
                    :disabled="uploading"
                  >
                    <el-button :loading="uploading" plain>
                      {{ form.image ? '替换图片' : '上传图片' }}
                    </el-button>
                  </el-upload>
                  <el-button v-if="form.image" text type="danger" @click="form.image = ''">
                    移除图片
                  </el-button>
                </div>
              </div>
            </el-form-item>

            <el-form-item label="整箱温区">
              <div class="zone-preview">
                <ZoneTag v-if="derivedZone" :zone="derivedZone" />
                <span v-else class="hint">添加商品后自动推导</span>
                <span class="hint">＝ 内含商品温区最大值（常温 &lt; 冷藏 &lt; 冷冻）</span>
              </div>
            </el-form-item>

            <el-form-item label="商品原价合计">
              <span class="xh-num sum">{{ money(poolPriceSum) }}</span>
              <span class="hint">仅供定价参考，不与售价联动</span>
            </el-form-item>
          </div>
        </div>

        <div class="lines">
          <div class="lines__head">
            <h3 class="lines__title">内含商品（{{ form.bundleGoods.length }}）</h3>
            <el-button plain @click="openPicker">选择商品</el-button>
          </div>

          <el-table :data="form.bundleGoods" row-key="goodsId" empty-text="还没有添加商品">
            <el-table-column label="商品" min-width="220">
              <template #default="{ row }">
                <div class="line-goods">
                  <GoodsThumb :name="row.name" :size="34" />
                  <span>{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="110" align="right">
              <template #default="{ row }">
                <span class="xh-num">{{ money(row.price) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="份数" width="150">
              <template #default="{ row }">
                <el-input-number v-model="row.copies" :min="1" :max="99" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="小计" width="120" align="right">
              <template #default="{ row }">
                <span class="xh-num">{{ money(row.price * row.copies) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="right">
              <template #default="{ $index }">
                <el-button size="small" text type="danger" @click="removeLine($index)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="foot">
          <el-button type="primary" :loading="submitting" @click="onSubmit">
            {{ isEdit ? '保存修改' : '确认新增' }}
          </el-button>
          <el-button @click="router.push('/bundle')">取消</el-button>
        </div>
      </el-form>
    </PanelCard>

    <el-dialog v-model="picker.visible" title="选择商品" width="680px">
      <el-input v-model.trim="picker.keyword" placeholder="按商品名称搜索" clearable class="picker-search" />
      <p class="picker-tip">已下架的商品也可以加入，但在它重新上架前，本组合箱无法起售。</p>
      <el-table
        ref="pickerTableRef"
        :data="filteredPool"
        height="380"
        row-key="id"
        @selection-change="onPickerSelect"
      >
        <el-table-column type="selection" width="44" reserve-selection />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="line-goods">
              <GoodsThumb :name="row.name" :image="row.image" :zone="row.storageZone" :size="32" />
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="84">
          <template #default="{ row }">
            <StatusTag type="goods" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="温区" width="90">
          <template #default="{ row }">
            <ZoneTag :zone="row.storageZone" />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="100" align="right">
          <template #default="{ row }">
            <span class="xh-num">{{ money(row.price) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <span class="picker-count">已选 {{ picker.checked.length }} 个</span>
        <el-button @click="picker.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmPicker">确认</el-button>
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
.cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--xh-10);
}
@media (max-width: 1180px) {
  .cols {
    grid-template-columns: 1fr;
    gap: var(--xh-6);
  }
}
.w-full {
  width: 100%;
}
.hint {
  margin-left: var(--xh-3);
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-400);
}
.uploader {
  display: flex;
  gap: var(--xh-4);
}
.uploader__ops {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--xh-2);
}
.zone-preview {
  display: flex;
  align-items: center;
  gap: var(--xh-3);
}
.sum {
  font-size: var(--xh-text-lg);
  font-weight: 600;
  color: var(--xh-ink-900);
}
.lines {
  margin-top: var(--xh-6);
  padding-top: var(--xh-5);
  border-top: 1px solid var(--xh-line);
}
.lines__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--xh-3);
}
.lines__title {
  font-family: var(--xh-font-display);
  font-size: var(--xh-text-lg);
}
.line-goods {
  display: flex;
  align-items: center;
  gap: var(--xh-3);
  font-size: var(--xh-text-sm);
}
.foot {
  margin-top: var(--xh-6);
  display: flex;
  gap: var(--xh-2);
}
.picker-search {
  margin-bottom: var(--xh-3);
}
.picker-tip {
  margin-bottom: var(--xh-3);
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-400);
}
.picker-count {
  float: left;
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-400);
  line-height: 32px;
}
</style>
