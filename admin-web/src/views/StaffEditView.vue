<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { staffApi } from '@/api/admin'
import PanelCard from '@/components/PanelCard.vue'

const route = useRoute()
const router = useRouter()

const editId = computed(() => (route.query.id ? Number(route.query.id) : null))
const isEdit = computed(() => editId.value !== null)

const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)

const form = reactive({
  id: null,
  name: '',
  username: '',
  phone: '',
  sex: '男',
  idNumber: ''
})

const rules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 32, message: '不超过 32 个字符', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '长度 3~32 位', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' }
  ],
  sex: [{ required: true, message: '请选择性别', trigger: 'change' }],
  idNumber: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '请输入 18 位身份证号', trigger: 'blur' }
  ]
}

async function loadDetail() {
  loading.value = true
  try {
    const data = await staffApi.getById(editId.value)
    Object.assign(form, {
      id: data.id,
      name: data.name,
      username: data.username,
      phone: data.phone,
      sex: data.sex === '女' ? '女' : '男',
      idNumber: data.idNumber
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
  submitting.value = true
  try {
    if (isEdit.value) {
      await staffApi.update({
        id: editId.value,
        name: form.name,
        username: form.username,
        phone: form.phone,
        sex: form.sex,
        idNumber: form.idNumber
      })
      ElMessage.success('店员信息已更新')
    } else {
      await staffApi.add({
        name: form.name,
        username: form.username,
        phone: form.phone,
        sex: form.sex,
        idNumber: form.idNumber
      })
      ElMessage.success('店员已新增，初始密码 123456')
    }
    router.push('/staff')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (isEdit.value) loadDetail()
})
</script>

<template>
  <div class="page" v-loading="loading">
    <PanelCard
      :title="isEdit ? '修改店员' : '新增店员'"
      sub="用户名全局唯一；新增后初始密码为 123456"
    >
      <template #extra>
        <el-button @click="router.push('/staff')">返回列表</el-button>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="form">
        <el-form-item label="姓名" for="staff-name" prop="name">
          <el-input id="staff-name" v-model.trim="form.name" placeholder="请输入真实姓名" maxlength="32" />
        </el-form-item>
        <el-form-item label="用户名" for="staff-username" prop="username">
          <el-input
            id="staff-username"
            v-model.trim="form.username"
            placeholder="登录账号，3~32 位"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item label="手机号" for="staff-phone" prop="phone">
          <el-input id="staff-phone" v-model.trim="form.phone" placeholder="11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="form.sex" aria-label="性别">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="身份证号" for="staff-idnumber" prop="idNumber">
          <el-input
            id="staff-idnumber"
            v-model.trim="form.idNumber"
            placeholder="18 位身份证号"
            maxlength="18"
          />
          <p class="tip">店员入职需实名，身份证号仅用于门店用工备案。</p>
        </el-form-item>

        <div class="foot">
          <el-button type="primary" :loading="submitting" @click="onSubmit">
            {{ isEdit ? '保存修改' : '确认新增' }}
          </el-button>
          <el-button @click="router.push('/staff')">取消</el-button>
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
.form {
  max-width: 560px;
}
.tip {
  margin-top: var(--xh-2);
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-300);
}
.foot {
  margin-top: var(--xh-6);
  display: flex;
  gap: var(--xh-2);
}
</style>
