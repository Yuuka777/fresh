<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { staffApi } from '@/api/admin'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'

const user = useUserStore()
const router = useRouter()

const info = ref(null)
const loading = ref(false)

const formRef = ref(null)
const submitting = ref(false)
const pwd = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })


const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '新密码长度 6~20 位', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '新密码需同时包含字母与数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_r, v, cb) =>
        v === pwd.newPassword ? cb() : cb(new Error('两次输入的新密码不一致')),
      trigger: 'blur'
    }
  ]
}

const avatarText = computed(() => (info.value?.name || user.displayName || '鲜').slice(0, 1))

async function loadInfo() {
  loading.value = true
  try {
    info.value = await staffApi.getById(user.id)
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
  try {
    await ElMessageBox.confirm('修改成功后原密码立即失效，需要重新登录。确认修改？', '修改密码', {
      confirmButtonText: '确认修改',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  submitting.value = true
  try {
    await staffApi.editPassword({
      empId: user.id,
      oldPassword: pwd.oldPassword,
      newPassword: pwd.newPassword
    })
    ElMessage.success('密码已修改，请重新登录')
    await user.logout()
    router.replace('/login')
  } finally {
    submitting.value = false
  }
}

onMounted(loadInfo)
</script>

<template>
  <div class="page" v-loading="loading">
    <div class="grid">
      <PanelCard title="我的账号" sub="当前登录店员信息">
        <div class="identity">
          <div class="identity__avatar" aria-hidden="true">{{ avatarText }}</div>
          <div class="identity__text">
            <p class="identity__name">
              {{ info?.name || '—' }}
              <StatusTag v-if="info" type="staff" :value="info.status" />
            </p>
            <p class="identity__account xh-num">{{ info?.username || '—' }}</p>
          </div>
        </div>

        <dl class="meta">
          <div><dt>手机号</dt><dd class="xh-num">{{ info?.phone || '—' }}</dd></div>
          <div><dt>性别</dt><dd>{{ info?.sex || '—' }}</dd></div>
          <div><dt>身份证号</dt><dd class="xh-num">{{ info?.idNumber || '—' }}</dd></div>
          <div><dt>创建时间</dt><dd class="xh-num">{{ info?.createTime || '—' }}</dd></div>
          <div><dt>最近更新</dt><dd class="xh-num">{{ info?.updateTime || '—' }}</dd></div>
        </dl>
      </PanelCard>

      <PanelCard title="修改密码" sub="修改成功后需重新登录">
        <el-form ref="formRef" :model="pwd" :rules="rules" label-width="92px" class="pwd-form">
          <el-form-item label="原密码" for="pwd-old" prop="oldPassword">
            <el-input
              id="pwd-old"
              v-model="pwd.oldPassword"
              type="password"
              show-password
              autocomplete="current-password"
              placeholder="请输入当前密码"
            />
          </el-form-item>
          <el-form-item label="新密码" for="pwd-new" prop="newPassword">
            <el-input
              id="pwd-new"
              v-model="pwd.newPassword"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="6~20 位，需含字母与数字"
            />
          </el-form-item>
          <el-form-item label="确认新密码" for="pwd-confirm" prop="confirmPassword">
            <el-input
              id="pwd-confirm"
              v-model="pwd.confirmPassword"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请再次输入新密码"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="onSubmit">确认修改</el-button>
          </el-form-item>
        </el-form>

        <p class="note">
          密码以明文传输、服务端 MD5 比对存储；原密码错误时服务端返回
          <span class="xh-num">msg = "原密码错误"</span>。
        </p>
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
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--xh-5);
}
@media (max-width: 1100px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
.identity {
  display: flex;
  align-items: center;
  gap: var(--xh-4);
  padding-bottom: var(--xh-5);
  border-bottom: 1px solid var(--xh-line);
}
.identity__avatar {
  width: 64px;
  height: 64px;
  display: grid;
  place-items: center;
  border-radius: var(--xh-radius-sm);
  background: #0a241d;
  color: #eaf6f1;
  font-family: var(--xh-font-display);
  font-size: 28px;
}
.identity__name {
  display: flex;
  align-items: center;
  gap: var(--xh-2);
  font-family: var(--xh-font-display);
  font-size: var(--xh-text-xl);
  font-weight: 600;
}
.identity__account {
  margin-top: var(--xh-1);
  font-size: var(--xh-text-xs);
  color: var(--xh-ink-400);
}
.meta {
  margin: var(--xh-5) 0 0;
  display: flex;
  flex-direction: column;
  gap: var(--xh-3);
}
.meta > div {
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: var(--xh-3);
  font-size: var(--xh-text-sm);
}
.meta dt {
  color: var(--xh-ink-400);
}
.meta dd {
  margin: 0;
  color: var(--xh-ink-800);
}
.pwd-form {
  max-width: 420px;
}
.note {
  margin-top: var(--xh-4);
  padding-top: var(--xh-4);
  border-top: 1px dashed var(--xh-line);
  font-size: var(--xh-text-xs);
  line-height: 1.8;
  color: var(--xh-ink-400);
}
</style>
