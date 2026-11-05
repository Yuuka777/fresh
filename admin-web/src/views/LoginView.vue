<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const user = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度 3~32 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码不少于 6 位', trigger: 'blur' }
  ]
}

/* 登录失败的具体文案（账号不存在 / 密码错误 / 账号被锁定）由服务端下发，
   这里不做任何账号预置或自动填充 */

async function onSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const data = await user.login({ username: form.username, password: form.password })
    ElMessage.success(`欢迎回来，${data.name}`)
    const redirect = route.query.redirect
    router.replace(typeof redirect === 'string' ? redirect : '/dashboard')
  } catch {
    /* 拦截器已提示具体文案 */
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login">
    <!-- 左：品牌叙事（非对称，占 5/9） -->
    <section class="brandpane">
      <div class="brandpane__grid" aria-hidden="true"></div>
      <div class="brandpane__inner">
        <p class="brandpane__eyebrow">XIANHE FRESH · 前置仓运营台</p>
        <h1 class="brandpane__title">
          鲜禾
          <span class="brandpane__title-sub">社区生鲜前置仓</span>
        </h1>
        <p class="brandpane__lead">
          产地直采、门店分拣、30 分钟送达。<br />
          这里是门店的一天：从接单到派送的每一条台账。
        </p>

        <dl class="ledger">
          <div class="ledger__row">
            <dt>履约时效</dt>
            <dd>门店直配 · 30 分钟达</dd>
          </div>
          <div class="ledger__row">
            <dt>温区管理</dt>
            <dd>常温 / 冷藏 / 冷冻</dd>
          </div>
          <div class="ledger__row">
            <dt>经营口径</dt>
            <dd>订单 · 营收 · 会员</dd>
          </div>
        </dl>

        <p class="brandpane__foot">冷链全程可溯 · 损耗可视 · 数据可追</p>
      </div>
    </section>

    <!-- 右：登录表单 -->
    <section class="formpane">
      <div class="formcard">
        <header class="formcard__head">
          <p class="xh-label">STAFF SIGN IN</p>
          <h2 class="formcard__title">店员登录</h2>
          <p class="formcard__sub">请使用门店分配的员工账号登录运营台</p>
        </header>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="onSubmit"
        >
          <el-form-item label="用户名" for="login-username" prop="username">
            <el-input
              id="login-username"
              v-model.trim="form.username"
              placeholder="请输入用户名"
              autocomplete="username"
              clearable
            />
          </el-form-item>
          <el-form-item label="密码" for="login-password" prop="password">
            <el-input
              id="login-password"
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              show-password
              @keyup.enter="onSubmit"
            />
          </el-form-item>

          <el-button
            type="primary"
            class="formcard__submit"
            size="large"
            :loading="loading"
            @click="onSubmit"
          >
            {{ loading ? '正在登录…' : '登录运营台' }}
          </el-button>
        </el-form>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login {
  display: grid;
  grid-template-columns: 5fr 4fr;
  min-height: 100vh;
  background: var(--xh-paper);
}

/* —— 左侧品牌面板 —— */
.brandpane {
  position: relative;
  overflow: hidden;
  background: #08201a;
  color: #e8f2ed;
  display: flex;
  align-items: center;
  padding: var(--xh-16) var(--xh-12);
}
.brandpane__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(159, 225, 203, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(159, 225, 203, 0.07) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(120% 90% at 15% 20%, #000 20%, transparent 75%);
  -webkit-mask-image: radial-gradient(120% 90% at 15% 20%, #000 20%, transparent 75%);
}
.brandpane__inner {
  position: relative;
  max-width: 460px;
  animation: rise var(--xh-dur-slow) var(--xh-ease-out) both;
}
.brandpane__eyebrow {
  font-family: var(--xh-font-mono);
  font-size: 11px;
  letter-spacing: 0.24em;
  color: #6f9484;
}
.brandpane__title {
  margin-top: var(--xh-5);
  font-family: var(--xh-font-display);
  font-size: 64px;
  font-weight: 700;
  letter-spacing: 0.12em;
  line-height: 1;
  color: #f4faf7;
}
.brandpane__title-sub {
  display: block;
  margin-top: var(--xh-4);
  font-size: var(--xh-text-base);
  font-weight: 400;
  letter-spacing: 0.36em;
  color: #9dc0b1;
}
.brandpane__lead {
  margin-top: var(--xh-8);
  font-size: var(--xh-text-base);
  line-height: 2;
  color: #b6cec4;
}

.ledger {
  margin: var(--xh-10) 0 0;
  border-top: 1px solid rgba(159, 225, 203, 0.18);
}
.ledger__row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--xh-4);
  padding: var(--xh-3) 0;
  border-bottom: 1px solid rgba(159, 225, 203, 0.12);
}
.ledger__row dt {
  font-size: var(--xh-text-sm);
  color: #7fa496;
  letter-spacing: 0.08em;
}
.ledger__row dd {
  margin: 0;
  font-size: var(--xh-text-base);
  color: #e4f2eb;
}
.ledger__row .xh-figure {
  color: #ffffff;
  font-size: var(--xh-text-xl);
  margin-right: 4px;
}
.brandpane__foot {
  margin-top: var(--xh-8);
  font-size: var(--xh-text-xs);
  letter-spacing: 0.1em;
  color: #6d9484;
}

/* —— 右侧表单 —— */
.formpane {
  display: grid;
  place-items: center;
  padding: var(--xh-10) var(--xh-8);
  background: var(--xh-surface);
}
.formcard {
  width: 100%;
  max-width: 372px;
  animation: rise var(--xh-dur-slow) var(--xh-ease-out) 120ms both;
}
.formcard__head {
  margin-bottom: var(--xh-8);
}
.formcard__title {
  margin-top: var(--xh-3);
  font-family: var(--xh-font-display);
  font-size: var(--xh-text-2xl);
  font-weight: 600;
  letter-spacing: 0.02em;
}
.formcard__sub {
  margin-top: var(--xh-2);
  font-size: var(--xh-text-sm);
  color: var(--xh-ink-400);
}
.formcard__submit {
  width: 100%;
  margin-top: var(--xh-3);
  letter-spacing: 0.08em;
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@media (max-width: 900px) {
  .login {
    grid-template-columns: 1fr;
  }
  .brandpane {
    padding: var(--xh-10) var(--xh-6);
  }
  .brandpane__title {
    font-size: 44px;
  }
  .brandpane__lead,
  .ledger,
  .brandpane__foot {
    display: none;
  }
}
</style>
