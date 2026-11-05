<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { categoryApi, commonApi, goodsApi } from '@/api/admin'
import { STORAGE_ZONE_MAP } from '@/constants/dict'
import GoodsThumb from '@/components/GoodsThumb.vue'
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
/* 每个规格组一个独立草稿，避免同一输入框内容串到其它分组 */
const specDrafts = reactive({})

const form = reactive({
  name: '',
  categoryId: undefined,
  price: undefined,
  image: null,
  description: '',
  origin: '',
  shelfLife: '',
  storageZone: 2,
  specs: []
})

const rules = {
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { max: 32, message: '不超过 32 个字符', trigger: 'blur' }
  ],
  categoryId: [{ required: true, message: '请选择所属分类', trigger: 'change' }],
  price: [
    { required: true, message: '请输入售价', trigger: 'blur' },
    {
      validator: (_r, v, cb) => (Number(v) > 0 ? cb() : cb(new Error('售价需大于 0'))),
      trigger: 'blur'
    }
  ],
  storageZone: [{ required: true, message: '请选择温区', trigger: 'change' }]
}

function addSpecGroup() {
  form.specs.push({ name: '', value: [] })
}

function removeSpecGroup(i) {
  form.specs.splice(i, 1)
}

function addSpecValue(i) {
  const text = String(specDrafts[i] || '').trim()
  if (!text) return
  if (form.specs[i].value.includes(text)) {
    ElMessage.warning('该候选值已存在')
    return
  }
  form.specs[i].value.push(text)
  specDrafts[i] = ''
}

function removeSpecValue(i, v) {
  const list = form.specs[i].value
  list.splice(list.indexOf(v), 1)
}

async function onUpload({ file }) {
  uploading.value = true
  try {
    form.image = await commonApi.upload(file)
    ElMessage.success('图片已上传')
  } finally {
    uploading.value = false
  }
}

async function loadDetail() {
  loading.value = true
  try {
    const data = await goodsApi.getById(editId.value)
    Object.assign(form, {
      name: data.name,
      categoryId: data.categoryId,
      price: data.price,
      image: data.image,
      description: data.description,
      origin: data.origin,
      shelfLife: data.shelfLife,
      storageZone: data.storageZone,
      specs: (data.specs || []).map((s) => ({ name: s.name, value: [...s.value] }))
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
  const hasBrokenSpec = form.specs.some((s) => !s.name.trim() || !s.value.length)
  if (hasBrokenSpec) {
    ElMessage.warning('规格类型需填写名称且至少保留一个候选值')
    return
  }
  submitting.value = true
  try {
    const payload = { ...form, specs: form.specs.map((s) => ({ name: s.name.trim(), value: s.value })) }
    if (isEdit.value) {
      await goodsApi.update({ ...payload, id: editId.value })
      ElMessage.success('商品已更新')
    } else {
      await goodsApi.add(payload)
      ElMessage.success('商品已新增')
    }
    router.push('/goods')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  categories.value = await categoryApi.list(1)
  if (isEdit.value) await loadDetail()
})
</script>

<template>
  <div class="page" v-loading="loading">
    <PanelCard
      :title="isEdit ? '修改商品' : '新增商品'"
      :sub="
        isEdit
          ? '保存后规格以本次提交为准，上架状态请在列表中切换'
          : '新增后为「下架」状态，需在列表中上架；规格可有多组（如净重、包装）'
      "
    >
      <template #extra>
        <el-button @click="router.push('/goods')">返回列表</el-button>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" class="form">
        <div class="form__cols">
          <div class="form__col">
            <el-form-item label="商品名称" for="goods-name" prop="name">
              <el-input
                id="goods-name"
                v-model.trim="form.name"
                placeholder="如：本地小番茄"
                maxlength="32"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="所属分类" for="goods-category" prop="categoryId">
              <el-select
                id="goods-category"
                aria-label="所属分类"
                v-model="form.categoryId"
                placeholder="请选择商品分类"
                class="w-full"
              >
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="售价" for="goods-price" prop="price">
              <el-input id="goods-price" v-model="form.price" placeholder="0.00">
                <template #prepend>¥</template>
              </el-input>
            </el-form-item>
            <el-form-item label="产地" for="goods-origin">
              <el-input id="goods-origin" v-model.trim="form.origin" placeholder="如：山东寿光" maxlength="64" />
            </el-form-item>
            <el-form-item label="保质期" for="goods-shelflife">
              <el-input
                id="goods-shelflife"
                v-model.trim="form.shelfLife"
                placeholder="如：冷藏 5 天"
                maxlength="32"
              />
            </el-form-item>
            <el-form-item label="存储温区" prop="storageZone">
              <el-radio-group v-model="form.storageZone" aria-label="存储温区">
                <el-radio-button v-for="(z, k) in STORAGE_ZONE_MAP" :key="k" :value="Number(k)">
                  {{ z.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="描述" for="goods-desc">
              <el-input
                id="goods-desc"
                v-model.trim="form.description"
                type="textarea"
                :rows="3"
                maxlength="255"
                show-word-limit
                placeholder="一句话卖点，将展示在用户端详情"
              />
            </el-form-item>
          </div>

          <div class="form__col form__col--side">
            <el-form-item label="商品图片">
              <div class="uploader">
                <GoodsThumb
                  :name="form.name || '新'"
                  :image="form.image"
                  :zone="form.storageZone"
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
                  <p class="uploader__tip">
                    允许 jpg / png / webp，单文件 ≤ 2MB。<br />
                    未上传时自动用商品名首字生成占位图。
                  </p>
                </div>
              </div>
            </el-form-item>

            <el-form-item label="规格">
              <div class="specs">
                <div v-for="(spec, i) in form.specs" :key="i" class="specs__group">
                  <div class="specs__head">
                    <el-input
                      v-model.trim="spec.name"
                      placeholder="规格类型，如：净重"
                      maxlength="16"
                      class="specs__name"
                    />
                    <el-button text type="danger" @click="removeSpecGroup(i)">删除该规格</el-button>
                  </div>
                  <div class="specs__values">
                    <el-tag
                      v-for="v in spec.value"
                      :key="v"
                      closable
                      type="info"
                      @close="removeSpecValue(i, v)"
                    >
                      {{ v }}
                    </el-tag>
                    <span v-if="!spec.value.length" class="specs__empty">至少添加一个候选值</span>
                  </div>
                  <div class="specs__add">
                    <el-input
                      v-model="specDrafts[i]"
                      size="small"
                      placeholder="候选值，如：约500g"
                      maxlength="24"
                      @keyup.enter="addSpecValue(i)"
                    />
                    <el-button size="small" @click="addSpecValue(i)">添加</el-button>
                  </div>
                </div>

                <el-button plain @click="addSpecGroup">+ 添加规格类型</el-button>
                <p class="specs__tip">
                  下单时用户需在每个规格类型下选择一个候选值。
                </p>
              </div>
            </el-form-item>
          </div>
        </div>

        <div class="form__foot">
          <el-button type="primary" :loading="submitting" @click="onSubmit">
            {{ isEdit ? '保存修改' : '确认新增' }}
          </el-button>
          <el-button @click="router.push('/goods')">取消</el-button>
        </div>
      </el-form>
    </PanelCard>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: var(--xh-5);
}
.form__cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--xh-10);
}
@media (max-width: 1180px) {
  .form__cols {
    grid-template-columns: 1fr;
    gap: var(--xh-6);
  }
}
.w-full {
  width: 100%;
}
.form__hint {
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
.uploader__tip {
  margin-top: var(--xh-1);
  font-size: 11px;
  line-height: 1.7;
  color: var(--xh-ink-300);
}
.specs {
  display: flex;
  flex-direction: column;
  gap: var(--xh-3);
  width: 100%;
}
/* 「+ 添加规格类型」不能被拉伸成整行，否则看起来像一个输入框 */
.specs > .el-button {
  align-self: flex-start;
}
.specs__group {
  padding: var(--xh-3);
  border: 1px solid var(--xh-line);
  border-radius: var(--xh-radius-sm);
  background: var(--xh-surface-2);
}
.specs__head {
  display: flex;
  align-items: center;
  gap: var(--xh-2);
}
.specs__name {
  max-width: 200px;
}
.specs__values {
  display: flex;
  flex-wrap: wrap;
  gap: var(--xh-2);
  margin: var(--xh-3) 0;
}
.specs__empty {
  font-size: var(--xh-text-xs);
  color: var(--xh-cinnabar-600);
}
.specs__add {
  display: flex;
  gap: var(--xh-2);
  max-width: 320px;
}
.specs__tip {
  font-size: 11px;
  line-height: 1.7;
  color: var(--xh-ink-300);
}
.form__foot {
  margin-top: var(--xh-6);
  padding-top: var(--xh-5);
  border-top: 1px solid var(--xh-line);
  display: flex;
  gap: var(--xh-2);
}
</style>
