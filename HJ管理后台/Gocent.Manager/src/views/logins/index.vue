<template>
  <div class="login-container">
    <canvas ref="particleCanvas" class="login-particles" aria-hidden="true" />
    <transition name="fade-up">
      <div v-show="isShow" class="login-card">
        <!-- Brand Header -->
        <div class="brand-header">
          <div class="brand-logo">
            <img src="@/assets/huanjiang.png" alt="logo">
          </div>
          <h1 class="brand-title">欢匠·序流</h1>
          <p class="brand-subtitle">排队叫号后台管理</p>
        </div>

        <!-- Login Form：禁止浏览器记住账号密码，每次需重新输入 -->
        <el-form
          ref="loginForm"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          autocomplete="off"
          label-width="0px"
          @submit.prevent
        >
          <input class="login-autofill-trap" type="text" name="fake-user" tabindex="-1" autocomplete="off">
          <input class="login-autofill-trap" type="password" name="fake-pass" tabindex="-1" autocomplete="new-password">
          <el-form-item prop="UserNo">
            <div class="input-wrapper">
              <span class="input-icon">
                <i class="el-icon-user" />
              </span>
              <el-input
                ref="UserNo"
                v-model="loginForm.UserNo"
                placeholder="请输入账号"
                name="loginAccount"
                type="text"
                tabindex="1"
                autocomplete="off"
                :readonly="accountLocked"
                clearable
                @focus="accountLocked = false"
              />
            </div>
          </el-form-item>

          <el-form-item prop="Password">
            <div class="input-wrapper">
              <span class="input-icon">
                <i class="el-icon-lock" />
              </span>
              <el-input
                :key="PasswordType"
                ref="Password"
                v-model="loginForm.Password"
                :type="PasswordType"
                placeholder="请输入密码"
                name="loginSecret"
                tabindex="2"
                autocomplete="new-password"
                :readonly="passwordLocked"
                show-password
                @focus="passwordLocked = false"
                @keyup.enter="handleLogin"
              />
            </div>
          </el-form-item>

          <el-button
            :loading="loading"
            type="primary"
            class="login-btn"
            @click.prevent="handleLogin"
          >
            {{ loading ? '正在登录...' : '登录系统' }}
          </el-button>
        </el-form>

        <div class="login-footer">
          <span>南京吾爱网络技术有限公司</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
/**
 * 管理后台登录页：账号密码居中、禁止浏览器记住密码、背景粒子坠落
 * 消费者：路由 /login；依赖 store user/login → User/LoginUser
 */
import { getApiErrorMessage } from '@/utils/request'

export default {
  name: 'Logins',
  data() {
    const validateUserNo = (rule, value, callback) => {
      if (value.length <= 0) {
        callback(new Error('请输入用户名'))
      } else {
        callback()
      }
    }
    const validatePassword = (rule, value, callback) => {
      if (value.length <= 0) {
        callback(new Error('请输入密码'))
      } else {
        callback()
      }
    }
    return {
      loading: false,
      isShow: false,
      PasswordType: 'password',
      accountLocked: true,
      passwordLocked: true,
      loginForm: {
        UserNo: '',
        Password: ''
      },
      loginRules: {
        UserNo: [{ required: true, trigger: 'blur', validator: validateUserNo }],
        Password: [{ required: true, trigger: 'blur', validator: validatePassword }]
      },
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  mounted() {
    this.loginForm.UserNo = ''
    this.loginForm.Password = ''
    this.isShow = true
    this.$nextTick(() => this.startParticles())
  },
  beforeUnmount() {
    this.stopParticles()
  },
  methods: {
    spawnParticle(canvas, scatterY) {
      const colors = ['125,211,252', '56,189,248', '186,230,253', '255,255,255']
      return {
        x: Math.random() * canvas.width,
        y: scatterY ? Math.random() * canvas.height : -12 - Math.random() * 40,
        size: 0.7 + Math.random() * 2.1,
        speed: 0.45 + Math.random() * 1.55,
        drift: (Math.random() - 0.5) * 0.4,
        opacity: 0.28 + Math.random() * 0.5,
        phase: Math.random() * Math.PI * 2,
        twinkle: 0.02 + Math.random() * 0.04,
        color: colors[Math.floor(Math.random() * colors.length)]
      }
    },
    startParticles() {
      const canvas = this.$refs.particleCanvas
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      const resize = () => {
        canvas.width = window.innerWidth
        canvas.height = window.innerHeight
      }
      resize()
      window.addEventListener('resize', resize)
      this._particleResize = resize
      const count = Math.min(88, Math.max(42, Math.floor(window.innerWidth / 18)))
      const particles = Array.from({ length: count }, () => this.spawnParticle(canvas, true))
      const tick = () => {
        ctx.clearRect(0, 0, canvas.width, canvas.height)
        for (const p of particles) {
          p.y += p.speed
          p.x += p.drift + Math.sin(p.phase) * 0.12
          p.phase += p.twinkle
          if (p.y > canvas.height + 16 || p.x < -20 || p.x > canvas.width + 20) {
            Object.assign(p, this.spawnParticle(canvas, false))
          }
          const alpha = p.opacity * (0.5 + 0.5 * Math.sin(p.phase))
          const trail = 10 + p.speed * 8
          const grad = ctx.createLinearGradient(p.x, p.y - trail, p.x, p.y)
          grad.addColorStop(0, `rgba(${p.color},0)`)
          grad.addColorStop(1, `rgba(${p.color},${alpha * 0.7})`)
          ctx.beginPath()
          ctx.strokeStyle = grad
          ctx.lineWidth = Math.max(0.6, p.size * 0.55)
          ctx.moveTo(p.x, p.y - trail)
          ctx.lineTo(p.x, p.y)
          ctx.stroke()
          ctx.beginPath()
          ctx.fillStyle = `rgba(${p.color},${alpha})`
          ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
          ctx.fill()
        }
        this._particleRaf = requestAnimationFrame(tick)
      }
      this._particleRaf = requestAnimationFrame(tick)
    },
    stopParticles() {
      if (this._particleRaf) {
        cancelAnimationFrame(this._particleRaf)
        this._particleRaf = null
      }
      if (this._particleResize) {
        window.removeEventListener('resize', this._particleResize)
        this._particleResize = null
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate((valid) => {
        if (valid) {
          this.loading = true
          this.$store
            .dispatch('user/login', this.loginForm)
            .then(() => {
              localStorage.setItem('UserNo', this.loginForm.UserNo)
              this.$router.push({ name: 'Dashboard' })
              this.$message({
                message: '登陆成功，进入系统！',
                type: 'success'
              })
              this.loading = false
            })
            .catch((error) => {
              this.loading = false
              const reason = getApiErrorMessage(error, '登录失败，请稍后重试')
              this.$message.error(reason)
            })
        } else {
          return false
        }
      })
    }
  }
}
</script>

<style lang="scss">
/* Global styles for element-ui overrides inside login container */
$cursor: #0EA5E9;

.login-container {
  --el-fill-color-blank: transparent;
  --el-input-bg-color: transparent;
  --el-disabled-bg-color: transparent;
  --el-input-border-color: transparent;
  --el-input-hover-border-color: transparent;
  --el-input-focus-border-color: transparent;

  .el-input {
    display: flex;
    height: 52px;
    width: 100%;
    flex: 1;
    min-width: 0;

    .el-input__wrapper {
      flex: 1;
      align-items: center;
      background: transparent !important;
      background-color: transparent !important;
      box-shadow: none !important;
      padding: 0 !important;
    }

    input,
    .el-input__inner {
      background: transparent !important;
      border: 0 !important;
      border-radius: 0 !important;
      padding: 0 8px 0 4px !important;
      color: #ffffff !important;
      height: 52px !important;
      line-height: 52px !important;
      caret-color: $cursor !important;
      font-size: 16px !important;
      box-shadow: none !important;
      text-align: left;

      &:-webkit-autofill,
      &:-webkit-autofill:hover,
      &:-webkit-autofill:focus,
      &:-webkit-autofill:active {
        box-shadow: 0 0 0px 1000px transparent inset !important;
        transition: background-color 5000s ease-in-out 0s;
        -webkit-text-fill-color: #ffffff !important;
      }
    }

    .el-input__suffix .el-input__icon,
    .el-input__password,
    .el-input__clear {
      color: #718096;
      font-size: 18px;
    }
  }

  .el-form,
  .el-form-item,
  .el-form-item__content {
    background: transparent !important;
  }

  .el-form-item {
    border: none;
    border-radius: 0;
    color: #ffffff;
    margin-bottom: 28px;
  }

  .el-form-item__content {
    margin-left: 0 !important;
    justify-content: center;
  }

  .el-form-item__error {
    padding-top: 6px;
    color: #ff4d4f;
  }
}
</style>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  width: 100%;
  background-image: url('../../assets/login_bg.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at center, transparent 0%, rgba(10, 15, 30, 0.7) 100%);
    z-index: 1;
  }
}

.login-particles {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 2;
  pointer-events: none;
}

.login-autofill-trap {
  position: absolute;
  left: -9999px;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

/* Login Card (Glassmorphism) */
.login-card {
  width: 440px;
  max-width: 90vw;
  background: rgba(15, 23, 42, 0.72);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 24px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.1);
  padding: 48px 40px 40px;
  position: relative;
  z-index: 10;
  color: #ffffff;
  transition: transform 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    box-shadow: 0 30px 60px -10px rgba(0, 0, 0, 0.6), inset 0 1px 0 rgba(255, 255, 255, 0.2);
    transform: translateY(-2px);
  }
}

/* Brand */
.brand-header {
  text-align: center;
  margin-bottom: 40px;
}

.brand-logo {
  width: 88px;
  height: 88px;
  margin: 0 auto 16px;
  padding: 0;
  border-radius: 22px;
  overflow: hidden;
  background: transparent;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.35);
  img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 8px;
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5);
}

.brand-subtitle {
  font-size: 14px;
  color: #a0aec0;
  margin: 0;
  letter-spacing: 2px;
  text-transform: uppercase;
  font-weight: 500;
}

/* Input */
.login-form {
  width: 100%;
}

.input-wrapper {
  display: flex;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  padding: 0 16px;
  transition: all 0.3s ease;
  background: rgba(0, 0, 0, 0.2);

  &:focus-within {
    border-color: #0EA5E9;
    background: rgba(15, 23, 42, 0.6);
    box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.2);
  }
}

.input-icon {
  color: #718096;
  font-size: 20px;
  margin-right: 12px;
  flex-shrink: 0;
  transition: color 0.3s;
}
.input-wrapper:focus-within .input-icon {
  color: #0EA5E9;
}

/* Button */
.login-btn {
  width: 100%;
  height: 52px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #0EA5E9 0%, #0284C7 100%);
  border: none;
  color: white;
  margin-top: 12px;
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: 0 10px 15px -3px rgba(14, 165, 233, 0.4);

  &:hover, &:focus {
    background: linear-gradient(135deg, #38BDF8 0%, #0EA5E9 100%);
    box-shadow: 0 20px 25px -5px rgba(14, 165, 233, 0.5);
    transform: translateY(-2px);
  }

  &:active {
    transform: translateY(1px);
    box-shadow: 0 4px 6px -2px rgba(14, 165, 233, 0.4);
  }
}

/* Footer */
.login-footer {
  text-align: center;
  margin-top: 32px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 0.5px;
}

/* Transition */
.fade-up-enter-active {
  transition: all 0.8s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.fade-up-enter {
  opacity: 0;
  transform: translateY(40px) scale(0.98);
}
</style>
