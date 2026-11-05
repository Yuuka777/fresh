<script setup>
/* ============================================================
   新增 / 编辑收货地址
   契约：/user/address/{id}（GET）、/user/address（POST 新增 / PUT 修改）
   省市区使用内置 region picker，返回 [省, 市, 区] 名称与编码
   ============================================================ */
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { addressApi } from '@/api/user'
import { useUserStore } from '@/store/user'

const user = useUserStore()

const editId = ref(0)
const loading = ref(true)
const saving = ref(false)
/* 编辑态取数失败：不弹提示，改为呈现中性空态（避免出现「标题是编辑、表单全空」的假页面） */
const missing = ref(false)

const LABELS = ['家', '公司', '门店']

const form = ref({
  consignee: '',
  sex: '男',
  phone: '',
  region: [], // [{ code, value }] 或 ['省','市','区']
  regionNames: [],
  detail: '',
  label: '',
  isDefault: 0
})

const isEdit = computed(() => editId.value > 0)

function goBack() {
  uni.navigateBack()
}

onLoad(async (options) => {
  editId.value = Number(options.id || 0)
  try {
    await user.ensureLogin()
    if (isEdit.value) {
      const data = await addressApi.detail(editId.value)
      form.value = {
        consignee: data.consignee || '',
        sex: data.sex === '女' ? '女' : '男',
        phone: data.phone || '',
        region: [],
        regionNames: [data.provinceName || '', data.cityName || '', data.districtName || ''],
        detail: data.detail || '',
        label: data.label || '',
        isDefault: data.isDefault === 1 ? 1 : 0
      }
      /* 编码回填（region picker 支持 code 数组） */
      if (data.provinceCode && data.cityCode && data.districtCode) {
        form.value.region = [data.provinceCode, data.cityCode, data.districtCode]
      }
    }
  } catch (e) {
    missing.value = isEdit.value
  } finally {
    loading.value = false
  }
})

function onRegionChange(e) {
  const detail = e.detail
  form.value.region = detail.code || []
  form.value.regionNames = detail.value || []
}

function validate() {
  const f = form.value
  if (!f.consignee.trim()) return '请填写收货人'
  if (!/^1\d{10}$/.test(f.phone.trim())) return '请填写 11 位手机号'
  if (!f.detail.trim()) return '请填写详细地址'
  return ''
}

async function onSave() {
  const err = validate()
  if (err) {
    uni.showToast({ title: err, icon: 'none' })
    return
  }
  if (saving.value) return
  saving.value = true
  try {
    const names = form.value.regionNames
    const codes = form.value.region
    const payload = {
      consignee: form.value.consignee.trim(),
      sex: form.value.sex,
      phone: form.value.phone.trim(),
      provinceName: names[0] || '',
      cityName: names[1] || '',
      districtName: names[2] || '',
      provinceCode: codes[0] || '',
      cityCode: codes[1] || '',
      districtCode: codes[2] || '',
      detail: form.value.detail.trim(),
      label: form.value.label,
      isDefault: form.value.isDefault
    }
    if (isEdit.value) {
      await addressApi.update({ ...payload, id: editId.value })
      uni.showToast({ title: '地址已更新', icon: 'none' })
    } else {
      await addressApi.add(payload)
      uni.showToast({ title: '地址已保存', icon: 'none' })
    }
    setTimeout(() => uni.navigateBack(), 600)
  } catch {
    /* 请求层已提示 */
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <view class="page xh-page">
    <xh-nav-bar :title="isEdit ? '编辑地址' : '新增地址'" />

    <view v-if="loading" class="loading"><text>正在准备表单…</text></view>

    <xh-empty
      v-else-if="missing"
      title="没能取到这条地址"
      desc="请返回地址列表后重试"
      action-text="返回"
      @action="goBack"
    />

    <view v-else class="body">
      <view class="block xh-card">
        <view class="field">
          <text class="field__label">收货人</text>
          <input
            v-model="form.consignee"
            class="field__input"
            type="text"
            maxlength="20"
            placeholder="姓名"
            placeholder-class="field__ph"
          />
        </view>

        <view class="xh-hairline" />

        <view class="field">
          <text class="field__label">性别</text>
          <view class="sex">
            <view
              v-for="s in ['男', '女']"
              :key="s"
              class="sex__item"
              :class="{ 'sex__item--active': form.sex === s }"
              @tap="form.sex = s"
            >
              <text>{{ s }}</text>
            </view>
          </view>
        </view>

        <view class="xh-hairline" />

        <view class="field">
          <text class="field__label">手机号</text>
          <input
            v-model="form.phone"
            class="field__input"
            type="number"
            maxlength="11"
            placeholder="11 位手机号"
            placeholder-class="field__ph"
          />
        </view>

        <view class="xh-hairline" />

        <picker mode="region" :value="form.region" @change="onRegionChange">
          <view class="field">
            <text class="field__label">所在地区</text>
            <text class="field__value" :class="{ 'field__value--ph': !form.regionNames.length }">
              {{ form.regionNames.length ? form.regionNames.join(' / ') : '请选择省 / 市 / 区' }}
            </text>
            <xh-icon name="arrow-right" :size="32" />
          </view>
        </picker>

        <view class="xh-hairline" />

        <view class="field field--column">
          <text class="field__label">详细地址</text>
          <textarea
            v-model="form.detail"
            class="field__area"
            maxlength="200"
            auto-height
            placeholder="街道、楼栋、门牌号"
            placeholder-class="field__ph"
          />
        </view>
      </view>

      <view class="block xh-card">
        <text class="block__title">标签</text>
        <view class="tags">
          <view
            v-for="l in LABELS"
            :key="l"
            class="tag"
            :class="{ 'tag--active': form.label === l }"
            @tap="form.label = form.label === l ? '' : l"
          >
            <text>{{ l }}</text>
          </view>
        </view>

        <view class="default">
          <view class="default__text">
            <text class="default__title">设为默认地址</text>
            <text class="default__desc">下单时优先使用该地址</text>
          </view>
          <switch
            :checked="form.isDefault === 1"
            color="#0F6E56"
            @change="(e) => (form.isDefault = e.detail.value ? 1 : 0)"
          />
        </view>
      </view>

      <view class="body__space" />
    </view>

    <view v-if="!loading && !missing" class="bar">
      <view
        class="bar__save"
        :class="{ 'bar__save--busy': saving }"
        hover-class="bar__save--hover"
        @tap="onSave"
      >
        <text>{{ saving ? '保存中…' : '保存地址' }}</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page {
  background: $xh-paper;
  padding-bottom: 170rpx;
}
.loading {
  padding: $xh-10 0;
  text-align: center;
  font-size: $xh-text-sm;
  color: $xh-ink-400;
}
.body {
  padding: $xh-3;
}
.body__space {
  height: 40rpx;
}
.block {
  padding: $xh-3;
  margin-bottom: $xh-3;
}
.block__title {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
}

.field {
  display: flex;
  align-items: center;
  gap: $xh-3;
  min-height: 96rpx;
  padding: $xh-3 0;
}
.field--column {
  flex-direction: column;
  align-items: flex-start;
  gap: $xh-2;
}
.field__label {
  width: 130rpx;
  flex-shrink: 0;
  font-size: $xh-text-sm;
  color: $xh-ink-500;
}
.field__input {
  flex: 1;
  min-height: 96rpx;
  font-size: $xh-text-base;
  color: $xh-ink-900;
}
.field__area {
  width: 100%;
  min-height: 120rpx;
  font-size: $xh-text-base;
  line-height: 1.6;
  color: $xh-ink-900;
}
.field__value {
  flex: 1;
  font-size: $xh-text-base;
  color: $xh-ink-900;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.field__value--ph {
  color: $xh-ink-300;
}
.field__arrow {
  font-size: 32rpx;
  color: $xh-ink-300;
}
.field__ph {
  color: $xh-ink-300;
}

.sex {
  display: flex;
  gap: $xh-2;
}
.sex__item {
  min-width: 120rpx;
  height: 64rpx;
  border-radius: $xh-radius-sm;
  border: 2rpx solid $xh-line;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $xh-surface-2;
}
.sex__item text {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
}
.sex__item--active {
  border-color: $xh-teal-600;
  background: $xh-teal-50;
}
.sex__item--active text {
  color: $xh-teal-800;
  font-weight: 600;
}

.tags {
  margin-top: $xh-3;
  display: flex;
  gap: $xh-2;
}
.tag {
  min-width: 120rpx;
  height: 64rpx;
  border-radius: $xh-radius-sm;
  border: 2rpx solid $xh-line;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $xh-surface-2;
}
.tag text {
  font-size: $xh-text-sm;
  color: $xh-ink-500;
}
.tag--active {
  border-color: $xh-teal-600;
  background: $xh-teal-50;
}
.tag--active text {
  color: $xh-teal-800;
  font-weight: 600;
}

.default {
  margin-top: $xh-4;
  padding-top: $xh-3;
  border-top: 2rpx solid $xh-line-soft;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.default__text {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.default__title {
  font-size: $xh-text-base;
  color: $xh-ink-900;
}
.default__desc {
  font-size: $xh-text-xs;
  color: $xh-ink-400;
}

.bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: $xh-3 $xh-4 calc(#{$xh-3} + env(safe-area-inset-bottom));
  background: $xh-surface;
  border-top: 2rpx solid $xh-line;
}
.bar__save {
  height: 88rpx;
  border-radius: $xh-radius;
  background: $xh-teal-600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bar__save text {
  font-size: $xh-text-base;
  color: #ffffff;
  font-weight: 600;
}
.bar__save--hover {
  background: $xh-teal-800;
}
.bar__save--busy {
  background: $xh-teal-500;
}
</style>
