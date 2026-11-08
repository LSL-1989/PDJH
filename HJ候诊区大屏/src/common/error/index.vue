<template>
  <div class="error-container">
    <div class="error-card">
      <div class="error-icon">
        <svg viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="60" cy="60" r="56" stroke="#8AB4F8" stroke-width="4" stroke-dasharray="8 6" opacity="0.4"/>
          <text x="60" y="68" text-anchor="middle" font-size="36" font-weight="900" fill="#8AB4F8">!</text>
        </svg>
      </div>
      <h1 class="error-title">终端设备未授权</h1>
      <div class="mac-badge">
        <span class="mac-label">设备识别码</span>
        <span class="mac-value">{{ queryMac }}</span>
      </div>
      <p class="error-message">{{ message }}</p>
      <div class="error-guide">
        请联系系统管理员，在【后台管理系统】→【参数设置】→【屏幕管理】中录入该设备码完成大屏分配。
      </div>
      <a href="javascript:location.reload();" class="retry-btn">
        <span>🔄</span> 重新检查配置
      </a>
    </div>
  </div>
</template>

<script>
export default {
  name: "Page404",
  data() {
    return {
      queryMac: this.$route.query.mac || '未知',
      queryMsg: this.$route.query.msg || ''
    }
  },
  computed: {
    message() {
      return this.queryMsg || "访问的页面丢失，或此设备尚未被分配大屏逻辑。";
    },
  },
};
</script>

<style lang="scss" scoped>
.error-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #1E1F25;
}

.error-card {
  text-align: center;
  max-width: 560px;
  padding: 48px 40px;
  background: #2D2E33;
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.4);
}

.error-icon {
  width: 100px;
  height: 100px;
  margin: 0 auto 24px;
  animation: pulse-ring 2s infinite;
}
@keyframes pulse-ring {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.05); opacity: 0.8; }
}

.error-title {
  font-size: 28px;
  color: #E8EAED;
  font-weight: 800;
  margin: 0 0 20px;
  letter-spacing: 2px;
}

.mac-badge {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  background: rgba(138, 180, 248, 0.1);
  border: 1px solid rgba(138, 180, 248, 0.3);
  border-radius: 12px;
  padding: 12px 24px;
  margin-bottom: 20px;
}

.mac-label {
  font-size: 12px;
  color: #9AA0A6;
  margin-bottom: 4px;
}

.mac-value {
  font-size: 20px;
  color: #F28B82;
  font-weight: 700;
  font-family: monospace;
  letter-spacing: 1px;
}

.error-message {
  font-size: 16px;
  color: #9AA0A6;
  margin: 0 0 16px;
  line-height: 1.6;
}

.error-guide {
  font-size: 13px;
  color: #80868B;
  line-height: 1.8;
  margin-bottom: 28px;
  padding: 12px 16px;
  background: rgba(255,255,255,0.03);
  border-radius: 8px;
  border-left: 3px solid #8AB4F8;
  text-align: left;
}

.retry-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 36px;
  background: #4285F4;
  color: #fff;
  border-radius: 24px;
  text-decoration: none;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(66, 133, 244, 0.3);

  &:hover {
    background: #3367D6;
    box-shadow: 0 6px 20px rgba(66, 133, 244, 0.5);
    transform: translateY(-2px);
  }
}
</style>
