<template>
  <div class="voice-player">
    <h1>此入口已迁移</h1>
    <p>独立音响请部署并打开工程 <strong>HJ音响终端</strong>：</p>
    <p class="url">/voice/#/?deviceCode=设备编号</p>
    <p class="hint">仓库目录：HJ音响终端 · 勿再使用候诊大屏内嵌 /screen/#/voice</p>
  </div>
</template>

<script setup>
</script>

<style scoped>
.voice-player {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #0f172a;
  color: #e2e8f0;
  text-align: center;
  padding: 24px;
}
.url {
  font-size: 22px;
  color: #38bdf8;
  margin: 16px 0;
}
.hint {
  opacity: 0.6;
  font-size: 13px;
}
</style>
