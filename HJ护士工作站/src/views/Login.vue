<template>
  <div class="login-page">
    <div class="login-page__mesh" aria-hidden="true" />
    <div class="login-page__orbs" aria-hidden="true">
      <span class="orb orb--1" />
      <span class="orb orb--2" />
      <span class="orb orb--3" />
    </div>
    <div class="login-page__grid" aria-hidden="true" />

    <aside class="login-page__showcase">
      <div class="login-showcase__content">
          <div class="login-showcase__brand">
            <div class="login-showcase__logo">
              <img src="../assets/huanjiang.png" alt="欢匠·序流">
            </div>
            <p class="login-showcase__eyebrow">HOSPITAL QUEUE · NURSE DESK</p>
            <h1 class="login-showcase__product">分诊台护士工作站</h1>
          </div>

        <ul class="login-showcase__summaries">
          <li v-for="item in projectSummaries" :key="item" class="login-summary">
            <el-icon class="login-summary__icon"><CircleCheck /></el-icon>
            <span>{{ item }}</span>
          </li>
        </ul>
      </div>
    </aside>

    <main class="login-page__form-wrap">
      <div class="login-form-card">
        <div class="login-form-card__glow" aria-hidden="true" />
        <div class="login-form-card__inner">
          <div class="login-form-card__header">
            <el-icon :size="36" color="#1E88E5"><Unlock /></el-icon>
            <div>
              <h2>欢迎回来</h2>
              <p>登录后即可处理本诊区候诊与告警</p>
            </div>
          </div>

          <el-form label-position="top" size="large" @submit.prevent="handleLogin">
            <el-form-item label="工号">
              <div class="input-wrapper">
                <el-icon class="input-icon"><User /></el-icon>
                <el-input
                  v-model="form.userNo"
                  placeholder="请输入工号"
                  class="login-input"
                  :disabled="needZonePick"
                />
              </div>
            </el-form-item>
            <el-form-item label="密码">
              <div class="input-wrapper">
                <el-icon class="input-icon"><Lock /></el-icon>
                <el-input
                  v-model="form.password"
                  type="password"
                  show-password
                  placeholder="请输入密码"
                  class="login-input"
                  :disabled="needZonePick"
                />
              </div>
            </el-form-item>
            <el-form-item v-if="needZonePick" label="当班诊区">
              <div class="input-wrapper">
                <el-icon class="input-icon"><OfficeBuilding /></el-icon>
                <el-select
                  v-model="selectedZoneId"
                  placeholder="请选择当班诊区"
                  class="login-input login-select"
                  style="width: 100%"
                >
                  <el-option
                    v-for="z in zoneOptions"
                    :key="z.id"
                    :label="z.name + (z.source === 'RESIDENT' ? '（常驻）' : '')"
                    :value="z.id"
                  />
                </el-select>
              </div>
              <p v-if="zoneHint" class="login-zone-hint">{{ zoneHint }}</p>
            </el-form-item>
            <div class="login-form-card__forgot">
              <button type="button" class="login-forgot-link" :disabled="forgotLoading" @click="handleForgot">
                {{ forgotLoading ? '提交中…' : '忘记密码' }}
              </button>
            </div>
            <el-button
              type="primary"
              class="login-form-card__submit"
              :loading="loading"
              native-type="submit"
            >
              <span>{{ needZonePick ? '进入所选诊区' : '进入工作站' }}</span>
              <el-icon class="login-form-card__arrow"><ArrowRight /></el-icon>
            </el-button>
          </el-form>

          <div class="login-form-card__hints">
            <div v-for="h in loginHints" :key="h" class="login-hint">
              <el-icon color="#43A047"><CircleCheck /></el-icon>
              <span>{{ h }}</span>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight, CircleCheck, Lock, OfficeBuilding, Unlock, User
} from '@element-plus/icons-vue'
import { loginUser, requestPasswordReset } from '../api/auth'
import { getSession, listDutyZones } from '../api/nurseDesk'
import { useSessionStore } from '../stores/session'

const router = useRouter()
const session = useSessionStore()
const loading = ref(false)
const forgotLoading = ref(false)
const needZonePick = ref(false)
const zoneOptions = ref([])
const selectedZoneId = ref('')
const zoneHint = ref('')
const form = reactive({ userNo: '', password: '' })

/** 项目定位归纳（登录页左侧展示） */
const projectSummaries = [
  '工作台掌握诊区 KPI、诊室概况与待处理通知',
  '操作区完成扫码签到、队列召回、前移与转诊',
  '辅助呼叫在医生端故障时代呼叫（能力建设中）',
  '系统管理追溯操作日志，WebSocket 实时刷新队列'
]

const loginHints = [
  'Navy Glass 分诊台，侧栏切换多业务页',
  '操作区支持扫码签到与快捷调度',
  '关键操作留痕，可在系统管理追溯'
]

/** 忘记密码：提交后由后台用户管理重置 */
async function handleForgot() {
  const account = (form.userNo || '').trim()
  if (!account) {
    ElMessage.warning('请先输入工号或手机号')
    return
  }
  forgotLoading.value = true
  try {
    const res = await requestPasswordReset(account)
    if (res?.success) {
      ElMessage.success(res.msg || '已提交重置申请，请联系管理员在「用户管理」中处理')
    } else {
      ElMessage.error(res?.msg || '申请失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '申请失败')
  } finally {
    forgotLoading.value = false
  }
}

/**
 * 登录：先鉴权落 token，再按排班/授权诊区进入。
 * 无当日排班且授权诊区多于一个时，停在登录页选择当班诊区。
 */
async function handleLogin() {
  if (!form.userNo || !form.password) {
    ElMessage.warning('请输入工号和密码')
    return
  }
  loading.value = true
  try {
    if (!needZonePick.value) {
      const res = await loginUser(form.userNo, form.password)
      if (!res.success || !res.data) {
        ElMessage.error(res.msg || '登录失败')
        return
      }
      const d = res.data
      const token = d.token || d.Token
      if (!token) {
        ElMessage.error('登录成功但未返回令牌，请联系管理员')
        return
      }
      session.setLogin(token, form.userNo, d.userName || d.UserName, d.role || d.Role)
    }

    const zonesRes = await listDutyZones()
    if (!zonesRes?.success) {
      ElMessage.error(zonesRes?.msg || '无法获取当班诊区')
      session.logout()
      needZonePick.value = false
      return
    }
    const z = zonesRes.data || {}
    zoneOptions.value = z.authorizedZones || []
    zoneHint.value = z.fallbackHint ? '今日无排班，请选择当班诊区（默认常驻）' : ''

    if (z.todaySchedule) {
      await enterDesk()
      return
    }
    if (z.needPick && !needZonePick.value) {
      needZonePick.value = true
      selectedZoneId.value = z.residentZoneId || (zoneOptions.value[0] && zoneOptions.value[0].id) || ''
      ElMessage.info(zoneHint.value || '今日无排班，请选择当班诊区')
      return
    }
    if (needZonePick.value && !selectedZoneId.value) {
      ElMessage.warning('请选择当班诊区')
      return
    }
    await enterDesk(needZonePick.value ? selectedZoneId.value : (z.residentZoneId || undefined))
    if (z.fallbackHint && (!needZonePick.value || selectedZoneId.value === z.residentZoneId)) {
      ElMessage.info('今日无排班，按常驻诊区进入')
    }
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
    session.logout()
    needZonePick.value = false
  } finally {
    loading.value = false
  }
}

async function enterDesk(zoneId) {
  try {
    const sess = await getSession(zoneId)
    if (sess?.success) {
      session.setSession(sess.data)
    } else if (sess?.msg) {
      ElMessage.warning(sess.msg)
    }
  } catch (sessErr) {
    ElMessage.warning(sessErr.message || '诊区会话加载失败，可稍后刷新')
  }
  router.push({ name: 'Desk' })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr minmax(420px, 480px);
  position: relative;
  overflow: hidden;
  background: #0a1628;
}
.login-page__mesh {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 60% at 20% 40%, rgba(30, 136, 229, 0.45), transparent),
    radial-gradient(ellipse 50% 50% at 85% 15%, rgba(67, 160, 71, 0.2), transparent),
    linear-gradient(145deg, #0d2137 0%, #0a1628 40%, #051018 100%);
  animation: mesh-shift 12s ease-in-out infinite alternate;
}
@keyframes mesh-shift {
  0% { filter: hue-rotate(0deg); }
  100% { filter: hue-rotate(8deg); }
}
.login-page__orbs {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.5;
  animation: orb-float 18s ease-in-out infinite;
}
.orb--1 {
  width: 320px;
  height: 320px;
  background: #1e88e5;
  top: 10%;
  left: 5%;
}
.orb--2 {
  width: 240px;
  height: 240px;
  background: #43a047;
  bottom: 20%;
  left: 35%;
  animation-delay: -6s;
}
.orb--3 {
  width: 180px;
  height: 180px;
  background: #7e57c2;
  top: 50%;
  right: 30%;
  animation-delay: -12s;
}
@keyframes orb-float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -20px) scale(1.05); }
  66% { transform: translate(-20px, 30px) scale(0.95); }
}
.login-page__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: linear-gradient(90deg, black 0%, black 55%, transparent 100%);
}

.login-page__showcase {
  position: relative;
  z-index: 1;
  padding: 2rem 3rem;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  text-align: center;
}

.login-showcase__content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2rem;
  width: 100%;
  max-width: 680px;
}

.login-showcase__brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}

.login-showcase__logo {
  width: 128px;
  height: 128px;
  margin: 0 auto;
  border-radius: 28px;
  overflow: hidden;
  background: transparent;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.4);
}
.login-showcase__logo img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-showcase__eyebrow {
  margin: 0;
  font-size: 1.375rem;
  letter-spacing: 0.2em;
  color: rgba(144, 202, 249, 0.9);
  font-weight: 600;
}

.login-showcase__product {
  margin: 0;
  font-size: 3.5rem;
  font-weight: 800;
  color: #fff;
  line-height: 1.2;
  letter-spacing: 0.04em;
}

.login-showcase__summaries {
  list-style: none;
  margin: 0;
  padding: 0;
  width: 100%;
  max-width: 640px;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  text-align: left;
}

.login-summary {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  font-size: 1rem;
  line-height: 1.55;
  color: rgba(255, 255, 255, 0.82);
  padding: 0.75rem 1rem;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.login-summary__icon {
  flex-shrink: 0;
  margin-top: 0.15rem;
  color: #81c784;
  font-size: 1.25rem;
}

.login-page__form-wrap {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(20px);
  border-left: 1px solid rgba(255, 255, 255, 0.08);
}
.login-form-card {
  width: 100%;
  max-width: 460px;
  position: relative;
}
.login-form-card__glow {
  position: absolute;
  inset: -2px;
  border-radius: 20px;
  background: linear-gradient(135deg, #1e88e5, #43a047, #1e88e5);
  background-size: 200% 200%;
  animation: border-flow 4s linear infinite;
  opacity: 0.7;
  filter: blur(1px);
}
@keyframes border-flow {
  0% { background-position: 0% 50%; }
  100% { background-position: 200% 50%; }
}
.login-form-card__inner {
  position: relative;
  background: rgba(255, 255, 255, 0.97);
  border-radius: 20px;
  padding: 2.25rem 2rem;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.35);
  color: #1a2332;
}
.login-form-card__header {
  display: flex;
  gap: 1.125rem;
  align-items: center;
  margin-bottom: 2rem;
}
.login-form-card__header h2 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 800;
  color: #122033;
}
.login-form-card__header p {
  margin: 0.35rem 0 0;
  font-size: 0.9375rem;
  color: #4b5b70;
}
.login-zone-hint {
  margin: 6px 0 0;
  font-size: 0.8125rem;
  color: #4b5b70;
}
.login-form-card__forgot {
  display: flex;
  justify-content: flex-end;
  margin: -0.35rem 0 0.5rem;
}
.login-forgot-link {
  border: none;
  background: none;
  padding: 0;
  cursor: pointer;
  font-size: 0.875rem;
  color: var(--nd-primary, #1e88e5);
  text-decoration: underline;
  text-underline-offset: 2px;
}
.login-forgot-link:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.login-form-card__submit {
  width: 100%;
  min-height: 52px;
  font-weight: 700;
  font-size: 1.0625rem;
  margin-top: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}
.login-form-card__arrow {
  transition: transform 0.2s;
}
.login-form-card__submit:hover .login-form-card__arrow {
  transform: translateX(4px);
}
.login-form-card__hints {
  margin-top: 1.5rem;
  padding-top: 1.25rem;
  border-top: 1px dashed #d7dee8;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.login-hint {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #4b5b70;
}

.login-input :deep(.el-input__wrapper),
.login-select :deep(.el-select__wrapper) {
  background: transparent !important;
  box-shadow: none !important;
  padding: 0 !important;
  min-height: 52px;
}
.login-input :deep(.el-input__inner),
.login-input :deep(.el-input__inner input),
.login-input :deep(input.el-input__inner),
.login-input :deep(input) {
  height: 52px;
  line-height: 52px;
  font-size: 16px;
  color: #1a2332 !important;
  -webkit-text-fill-color: #1a2332 !important;
  caret-color: #0ea5e9;
}
.login-input :deep(input::placeholder),
.login-input :deep(.el-input__inner::placeholder) {
  color: #8a97a8 !important;
  -webkit-text-fill-color: #8a97a8 !important;
  opacity: 1;
}
.login-input :deep(.el-input__suffix),
.login-input :deep(.el-input__password),
.login-input :deep(.el-icon) {
  color: #64748b !important;
}
.login-form-card__inner :deep(.el-form-item__label) {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #334155 !important;
}
.input-wrapper {
  display: flex;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d7dee8;
  border-radius: 12px;
  padding: 0 16px;
  min-height: 52px;
  background: #f4f7fb;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}
.input-wrapper:focus-within {
  border-color: #0ea5e9;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.2);
}
.input-icon {
  color: #8a97a8;
  font-size: 20px;
  margin-right: 12px;
  flex-shrink: 0;
  transition: color 0.2s ease;
}
.input-wrapper:focus-within .input-icon {
  color: #0ea5e9;
}

@media (max-width: 1024px) {
  .login-page {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr;
  }
  .login-page__showcase {
    padding: 2rem 1.5rem 1rem;
    min-height: auto;
  }
  .login-showcase__product {
    font-size: 2.25rem;
  }
  .login-showcase__eyebrow {
    font-size: 1rem;
  }
  .login-showcase__logo {
    width: 96px;
    height: 96px;
    border-radius: 22px;
  }
  .login-showcase__content {
    gap: 1.5rem;
  }
  .login-showcase__summaries {
    padding-bottom: 0;
  }
  .login-page__form-wrap {
    border-left: none;
    min-height: 45vh;
  }
  .login-page__grid {
    mask-image: none;
  }
}
</style>
