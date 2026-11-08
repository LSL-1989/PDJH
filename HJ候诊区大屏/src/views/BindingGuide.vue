<template>
  <!-- ScreenFrame：450×800 设计稿等比缩放，与自助签到机一致；仅包裹绑定页 -->
  <ScreenFrame>
    <div class="bind-page" :class="{ 'bind-page--manual': phase === 'manual' }">
      <div v-if="phase === 'manual'" class="manual-panel">
        <header class="manual-header">
          <p class="manual-title">录入设备编号</p>
          <p class="bind-hint bind-hint--compact">请输入管理后台已添加的候诊区大屏设备编号</p>
        </header>
        <div class="bind-input-display" aria-live="polite">{{ manualCode || '—' }}</div>
        <p v-if="manualError" class="manual-error">{{ manualError }}</p>
        <DeviceCodeKeyboard
          class="manual-keyboard"
          @input="appendManualChar"
          @backspace="backspaceManual"
          @clear="clearManual"
          @confirm="submitManual"
        />
        <el-button size="large" class="bind-btn bind-btn--compact" :disabled="loading" @click="goGuide()">
          返回
        </el-button>
      </div>

      <div v-else class="bind-card">
        <h1>终端设备绑定</h1>
        <p class="bind-type">{{ terminalTypeLabel }}</p>

        <template v-if="phase === 'guide'">
          <p class="bind-hint">首次使用请选择绑定方式</p>
          <p v-if="guideHint" class="guide-error">{{ guideHint }}</p>
          <el-button type="primary" size="large" class="bind-btn" :loading="loading" @click="useShellId">
            使用本机设备 ID
          </el-button>
          <el-button size="large" class="bind-btn" @click="openManual">录入设备编号</el-button>
        </template>

        <template v-else-if="phase === 'confirmShell'">
          <p class="status-title">确认本机设备</p>
          <p class="bind-hint">将使用以下设备 ID 向系统发起登记与绑定</p>
          <p class="device-code-label">设备 ID</p>
          <p class="device-code">{{ shellConfirmId }}</p>
          <el-button type="primary" size="large" class="bind-btn" :loading="loading" @click="confirmShellId">
            {{ loading ? '处理中…' : '确认并登记' }}
          </el-button>
          <el-button size="large" class="bind-btn" :disabled="loading" @click="cancelShellConfirm">取消</el-button>
        </template>

        <template v-else-if="phase === 'pending'">
          <p class="status-title">等待管理员配置</p>
          <p class="device-code-label">设备编号</p>
          <p class="device-code">{{ deviceCode }}</p>
          <p class="bind-hint">{{ pendingMsg }}</p>
          <el-button type="primary" size="large" class="bind-btn" :loading="loading" @click="recheck()">
            {{ loading ? '检测中…' : '重新检测' }}
          </el-button>
          <el-button size="large" class="bind-btn" :disabled="loading" @click="cancelBinding">取消绑定</el-button>
        </template>

        <template v-else>
          <p class="error-msg">{{ errorMsg }}</p>
          <p v-if="awaitNetwork" class="bind-hint">请检查后台服务与网络，恢复后将自动进入候诊界面</p>
          <p v-if="deviceCode" class="device-code">{{ deviceCode }}</p>
          <el-button size="large" class="bind-btn" @click="goGuide()">返回绑定</el-button>
          <el-button type="primary" size="large" class="bind-btn" :loading="loading" @click="recheck()">
            {{ loading ? '检测中…' : '重新检测' }}
          </el-button>
        </template>
      </div>
    </div>
  </ScreenFrame>
</template>

<script>
/**
 * 候诊区大屏终端设备绑定引导：本机 ID 登记、后台编号录入（软键盘）、等待配置。
 * 消费者：App.vue bootstrap 未绑定/待配置时跳转 /bind。
 * 录入规则与自助签到终端一致：≤20 位，仅数字、大写字母与连字符。
 */
import DeviceCodeKeyboard from '@/components/DeviceCodeKeyboard.vue';
import ScreenFrame from '@/components/ScreenFrame.vue';
import {
  getShellDeviceId,
  getShellTerminalType,
  getStoredDeviceCode,
  setStoredDeviceCode,
  registerTerminal,
  resolveTerminal,
  TERMINAL_STATUS,
  SHELL_DEVICE_ID_MAX_LEN,
  normalizeShellDeviceId,
  isTransientResolveFailure,
  installAndroidNativeMock
} from '@/utils/terminalBinding';

const TYPE_LABELS = { SCREEN: '候诊区大屏', CLINIC: '诊室门边屏', CHECKIN: '自助签到' };
const MANUAL_INVALID_MSG = '设备编号无效，请重新确认输入';
const PENDING_POLL_MS = 3000;
const NETWORK_RETRY_HINT = '网络异常，正在自动重试连接…';

export default {
  name: 'BindingGuide',
  components: { DeviceCodeKeyboard, ScreenFrame },
  data() {
    return {
      phase: 'guide',
      loading: false,
      terminalType: 'SCREEN',
      deviceCode: '',
      manualCode: '',
      pendingMsg: '',
      errorMsg: '',
      guideHint: '',
      manualError: '',
      shellConfirmId: '',
      pollInFlight: false,
      /** 冻结待机中：持续 Resolve，解冻后进入业务页 */
      awaitUnfreeze: false,
      /** 网络待机中：持续 Resolve，服务恢复后进入业务页 */
      awaitNetwork: false
    };
  },
  computed: {
    terminalTypeLabel() {
      return TYPE_LABELS[this.terminalType] || this.terminalType;
    },
    shouldAutoPoll() {
      return this.phase === 'pending' || this.awaitUnfreeze || this.awaitNetwork;
    }
  },
  mounted() {
    if (import.meta.env.DEV) {
      installAndroidNativeMock('SCREEN');
    }
    this.terminalType = getShellTerminalType('SCREEN');

    if (this.$route.query.wrongShell === '1') {
      this.phase = 'error';
      this.errorMsg = '该设备为诊室门边屏 (CLINIC)，当前为候诊大屏壳。请改用 CLINIC 壳访问 /clinic/';
      return;
    }
    if (getShellTerminalType('SCREEN') !== 'SCREEN') {
      this.phase = 'error';
      this.errorMsg = '当前安装包不是候诊区大屏 (SCREEN) 类型，请安装正确的 Shell';
      return;
    }
    if (this.$route.query.frozen === '1') {
      this.enterFrozenStandby('设备已停用，请联系管理员');
      document.addEventListener('visibilitychange', this.onVisibilityChange);
      return;
    }
    if (this.$route.query.network === '1') {
      this.enterNetworkStandby(NETWORK_RETRY_HINT);
      document.addEventListener('visibilitychange', this.onVisibilityChange);
      return;
    }

    const fromQuery = (this.$route.query.deviceCode || '').toString().trim();
    if (fromQuery) {
      setStoredDeviceCode(fromQuery);
    }

    void this.bootstrapStoredBinding();
    document.addEventListener('visibilitychange', this.onVisibilityChange);
  },
  watch: {
    shouldAutoPoll(next) {
      if (next) {
        this.ensurePendingPoll();
      } else {
        this.stopPoll();
      }
    }
  },
  beforeUnmount() {
    this.stopPoll();
    document.removeEventListener('visibilitychange', this.onVisibilityChange);
  },
  methods: {
    onVisibilityChange() {
      if (document.visibilityState !== 'visible') return;
      if (this.shouldAutoPoll) {
        void this.recheck({ silent: true });
      }
    },
    /** 进入冻结待机并开启解冻轮询 */
    enterFrozenStandby(msg) {
      this.awaitUnfreeze = true;
      this.awaitNetwork = false;
      this.deviceCode = this.deviceCode || getStoredDeviceCode();
      this.phase = 'error';
      this.errorMsg = msg || '设备已冻结';
      this.ensurePendingPoll();
    },
    /** 进入网络待机并开启自动重试（服务恢复后回到业务页） */
    enterNetworkStandby(msg) {
      this.awaitNetwork = true;
      this.awaitUnfreeze = false;
      this.deviceCode = this.deviceCode || getStoredDeviceCode();
      this.phase = 'error';
      this.errorMsg = msg || NETWORK_RETRY_HINT;
      this.ensurePendingPoll();
    },
    clearStandbyFlags() {
      this.awaitUnfreeze = false;
      this.awaitNetwork = false;
    },
    goGuide(hint = '') {
      this.clearStandbyFlags();
      this.stopPoll();
      this.phase = 'guide';
      this.guideHint = hint;
      this.errorMsg = '';
      this.manualError = '';
      this.manualCode = '';
    },
    clearDeviceBinding() {
      setStoredDeviceCode('');
      this.deviceCode = '';
    },
    persistDeviceBinding(code) {
      setStoredDeviceCode(code);
      this.deviceCode = code;
    },
    cancelBinding() {
      this.clearDeviceBinding();
      this.pendingMsg = '';
      this.goGuide();
    },
    useShellId() {
      const id = getShellDeviceId();
      if (!id) {
        this.$message.error('无法获取本机设备 ID，请确认安卓 Shell 已注入 AndroidNative');
        return;
      }
      this.shellConfirmId = id;
      this.phase = 'confirmShell';
    },
    cancelShellConfirm() {
      this.shellConfirmId = '';
      this.phase = 'guide';
    },
    async confirmShellId() {
      const id = normalizeShellDeviceId(this.shellConfirmId);
      if (!id) {
        this.cancelShellConfirm();
        return;
      }
      setStoredDeviceCode(id);
      this.shellConfirmId = '';
      await this.tryResolve(id, { allowRegister: true });
    },
    openManual() {
      this.clearDeviceBinding();
      this.manualCode = '';
      this.manualError = '';
      this.phase = 'manual';
    },
    /** 软键盘输入；超过 SHELL_DEVICE_ID_MAX_LEN 忽略 */
    appendManualChar(char) {
      if (this.manualCode.length >= SHELL_DEVICE_ID_MAX_LEN) return;
      this.manualCode += char;
    },
    backspaceManual() {
      this.manualCode = this.manualCode.slice(0, -1);
    },
    clearManual() {
      this.manualCode = '';
      this.manualError = '';
    },
    /** 录入编号：normalize 后提交；无效格式不请求后端 */
    async submitManual() {
      const code = normalizeShellDeviceId(this.manualCode);
      if (!code) {
        this.manualError = `请输入有效设备编号（不超过${SHELL_DEVICE_ID_MAX_LEN}位，仅数字、大写字母与-）`;
        return;
      }
      this.manualCode = code;
      await this.tryResolve(code, { allowRegister: false, manualEntry: true });
    },
    async tryResolve(code, options = {}) {
      const { allowRegister = false, manualEntry = false, silent = false } = options;
      if (!silent) this.loading = true;
      if (manualEntry) {
        this.manualError = '';
      } else {
        this.deviceCode = code;
      }

      try {
        const result = await resolveTerminal(code, this.terminalType);

        if (result.ok && result.status === TERMINAL_STATUS.READY) {
          const data = result.data || {};
          if (data.terminalType === 'CLINIC') {
            if (manualEntry) {
              this.clearDeviceBinding();
              this.manualError = MANUAL_INVALID_MSG;
              return;
            }
            this.clearStandbyFlags();
            this.stopPoll();
            this.phase = 'error';
            this.errorMsg = '该设备登记为诊室门边屏 (CLINIC)，请使用 CLINIC 壳访问 /clinic/';
            return;
          }
          this.clearStandbyFlags();
          this.stopPoll();
          if (manualEntry) this.persistDeviceBinding(code);
          this.enterBusiness(data);
          return;
        }

        if (result.status === TERMINAL_STATUS.PENDING) {
          this.clearStandbyFlags();
          this.persistDeviceBinding(code);
          this.phase = 'pending';
          this.pendingMsg =
            result.msg ||
            '请在【屏幕管理】将终端类型设为 SCREEN 并完成科室/诊室绑定与模板路由配置后启用';
          this.ensurePendingPoll();
          return;
        }

        if (result.status === TERMINAL_STATUS.NOT_REGISTERED) {
          if (allowRegister) {
            const reg = await registerTerminal(code, this.terminalType);
            if (reg.ok) {
              this.clearStandbyFlags();
              this.persistDeviceBinding(code);
              this.phase = 'pending';
              this.pendingMsg = reg.msg || '登记成功，等待管理员配置';
              this.ensurePendingPoll();
              return;
            }
            this.goGuide(reg.msg || '登记失败，请稍后重试');
            return;
          }
          if (manualEntry) {
            this.clearDeviceBinding();
            this.manualError = MANUAL_INVALID_MSG;
            return;
          }
          this.clearStandbyFlags();
          this.stopPoll();
          this.phase = 'error';
          this.errorMsg = result.msg || '设备未在管理后台登记，请联系管理员添加候诊大屏信息';
          return;
        }

        if (result.status === TERMINAL_STATUS.FROZEN) {
          if (manualEntry) {
            this.clearDeviceBinding();
            this.manualError = result.msg || '设备已冻结';
            return;
          }
          this.enterFrozenStandby(result.msg || '设备已冻结');
          return;
        }

        // 瞬时网络失败：保持/进入网络待机并继续轮询
        if (isTransientResolveFailure(result)) {
          if (manualEntry) {
            this.manualError = result.msg || '网络异常，请稍后重试';
            return;
          }
          this.enterNetworkStandby(result.msg || NETWORK_RETRY_HINT);
          return;
        }

        const failMsg = result.msg || '绑定失败';
        if (manualEntry) {
          this.clearDeviceBinding();
          this.manualError = failMsg;
          return;
        }
        this.clearStandbyFlags();
        this.stopPoll();
        this.phase = 'error';
        this.errorMsg = failMsg;
      } finally {
        if (!silent) this.loading = false;
      }
    },
    enterBusiness(data) {
      this.clearStandbyFlags();
      const routePath = data.templateRoute || '/default';
      const screenId = data.screenId || data.recordId;
      const target = `${routePath}?screenId=${screenId}`;
      if (this.$route.fullPath !== target) {
        this.$router.replace(target);
      }
    },
    async bootstrapStoredBinding() {
      const code = getStoredDeviceCode();
      if (!code) return;
      this.deviceCode = code;
      await this.tryResolve(code, { allowRegister: false });
    },
    async recheck(options = {}) {
      const silent = options.silent ?? false;
      if (silent && this.pollInFlight) return;
      const code = this.deviceCode || getStoredDeviceCode();
      if (!code) return;
      this.pollInFlight = true;
      try {
        await this.tryResolve(code, { allowRegister: false, silent });
      } finally {
        this.pollInFlight = false;
      }
    },
    ensurePendingPoll() {
      if (this._pollTimer) return;
      this._pollTimer = setInterval(() => void this.recheck({ silent: true }), PENDING_POLL_MS);
    },
    stopPoll() {
      if (this._pollTimer) {
        clearInterval(this._pollTimer);
        this._pollTimer = null;
      }
    }
  }
};
</script>

<style scoped>
/* 填满 ScreenFrame 450×800 画布，由外层 transform 等比缩放 */
.bind-page {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 16px;
  background: transparent;
  color: #e8eaed;
  box-sizing: border-box;
  overflow: hidden;
}

.bind-page--manual {
  padding: 0;
}

.manual-panel {
  width: 410px;
  min-width: 410px;
  max-width: 410px;
  flex: 0 0 auto;
  align-self: center;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 24px 20px 20px;
  text-align: center;
  background: #2d2e33;
  border-radius: 16px;
  box-sizing: border-box;
}

.manual-header {
  flex-shrink: 0;
}

.manual-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: #e8eaed;
}

.bind-input-display {
  min-height: 44px;
  padding: 12px 14px;
  font-family: monospace;
  font-size: 18px;
  font-weight: 700;
  color: #8ab4f8;
  letter-spacing: 1px;
  word-break: break-all;
  background: rgba(138, 180, 248, 0.1);
  border-radius: 8px;
}

.manual-keyboard {
  margin-top: 4px;
}

.bind-card {
  text-align: center;
  width: 100%;
  max-width: 100%;
  padding: 32px 20px 28px;
  background: #2d2e33;
  border-radius: 16px;
  box-sizing: border-box;
}

.bind-type {
  display: inline-block;
  margin: 0 0 24px;
  padding: 4px 14px;
  font-size: 14px;
  font-weight: 600;
  color: #8ab4f8;
  background: rgba(138, 180, 248, 0.12);
  border-radius: 999px;
}

.bind-hint {
  color: #9aa0a6;
  margin-bottom: 20px;
}

.bind-hint--compact {
  margin-bottom: 0;
  font-size: 13px;
}

.guide-error {
  color: #f28b82;
  margin: -12px 0 20px;
  font-size: 14px;
  padding: 10px 14px;
  background: rgba(242, 139, 130, 0.12);
  border-radius: 8px;
}

.manual-error {
  color: #f28b82;
  margin: 0;
  font-size: 14px;
}

.bind-btn {
  display: block;
  width: 100%;
  margin: 12px 0;
}

.bind-btn--compact {
  margin: 4px 0 0;
}

.device-code-label {
  margin: 0 0 4px;
  font-size: 13px;
  color: #9aa0a6;
}

.device-code {
  font-family: monospace;
  font-size: 18px;
  color: #8ab4f8;
  margin: 0 0 16px;
  word-break: break-all;
  padding: 12px 16px;
  background: rgba(138, 180, 248, 0.1);
  border-radius: 8px;
}

.status-title {
  margin: 0 0 16px;
  font-size: 20px;
  font-weight: bold;
}

.error-msg {
  color: #f28b82;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: rgba(242, 139, 130, 0.12);
  border-radius: 8px;
  line-height: 1.5;
}
</style>
