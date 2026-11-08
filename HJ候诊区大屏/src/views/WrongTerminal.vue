<template>
  <div class="wrong-terminal">
    <div class="card">
      <h1>终端类型不匹配</h1>
      <p>当前设备在后台登记为 <strong>诊室门边屏 (CLINIC)</strong>。</p>
      <p>候诊区大屏壳 (SCREEN) 无法加载此设备，请改用门边屏安装包，访问 <code>/clinic/</code>。</p>
      <p v-if="deviceCode" class="code">设备编号：{{ deviceCode }}</p>
    </div>
  </div>
</template>

<script>
import { getStoredDeviceCode } from '@/utils/terminalBinding'

export default {
  name: 'WrongTerminal',
  data() {
    return {
      deviceCode: getStoredDeviceCode() || ''
    }
  }
}
</script>

<style scoped>
.wrong-terminal {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #202124;
  color: #e8eaed;
  font-family: 'Microsoft YaHei', sans-serif;
}
.card {
  max-width: 640px;
  padding: 32px 40px;
  border-radius: 12px;
  background: #303134;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
}
h1 {
  margin: 0 0 16px;
  font-size: 28px;
  color: #f28b82;
}
p {
  margin: 0 0 12px;
  line-height: 1.6;
  font-size: 16px;
}
code {
  background: #3c4043;
  padding: 2px 8px;
  border-radius: 4px;
}
.code {
  margin-top: 20px;
  font-size: 14px;
  color: #9aa0a6;
}
</style>
