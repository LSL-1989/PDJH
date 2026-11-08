<template>
  <div class="h5-scan-container">
    <van-nav-bar title="医院智慧导诊系统" fixed placeholder />

    <div class="welcome-header">
      <h2 class="main-title">扫码签到 / 实时排队</h2>
      <p class="sub-title">请对准医院提供的条形码或二维码，或者点击下方区域</p>
    </div>
    
    <div class="scan-area-wrap">
      <div class="scan-area" @click="startScan">
        <div class="scan-content">
          <van-icon name="scan" size="64" color="#1989fa" class="scan-icon" />
          <span class="scan-text">点击此处打开扫一扫</span>
        </div>
        <div class="scan-corner top-left"></div>
        <div class="scan-corner top-right"></div>
        <div class="scan-corner bottom-left"></div>
        <div class="scan-corner bottom-right"></div>
      </div>
    </div>
    
    <div v-if="patientInfo" class="queue-status-block">
      <van-divider dashed>您当前的排队状态</van-divider>
      
      <div class="patient-banner">
        <span class="greeting">尊敬的</span>
        <span class="patient-name">{{ patientInfo.name }}</span>
      </div>

      <div class="status-cards-row">
        <div class="status-card highlight-card">
          <span class="card-label">前方等待</span>
          <div class="card-value-wrap">
            <span class="card-number">{{ waitCount }}</span>
            <span class="card-unit">人</span>
          </div>
        </div>
        
        <div class="status-card highlight-card outline">
          <span class="card-label">预计等待</span>
          <div class="card-value-wrap">
            <span class="card-number">{{ estimatedTime }}</span>
            <span class="card-unit">分钟</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { setupSseListener } from '../api/sse.js';
import { showDialog } from 'vant';

export default {
  name: 'ScanSignIn',
  data() {
    return {
      patientInfo: null,
      waitCount: 0,
      estimatedTime: 0,
      sseConnection: null
    };
  },
  methods: {
    startScan() {
      // 模拟扫码成功
      this.patientInfo = { patientCode: 'P12345', name: '张三' };
      this.waitCount = 5;
      this.estimatedTime = 15;
      
      this.listenForQueueUpdates(this.patientInfo.patientCode);
    },
    listenForQueueUpdates(patientCode) {
      if (this.sseConnection) {
        this.sseConnection.close();
      }
      this.sseConnection = setupSseListener(patientCode, (event) => {
        if (event.type === 'QUEUE_UPDATE') {
          this.waitCount = event.data.waitCount;
          this.estimatedTime = event.data.estimatedTime;
        } else if (event.type === 'CALL_NEXT') {
          showDialog({
            title: '就诊呼叫提醒',
            message: `请注意：轮到您了！请立即前往 ${event.data.roomName} 就诊。`,
            confirmButtonText: '我知道了',
            theme: 'round-button',
          }).then(() => {
            // on close
          });
        }
      });
    }
  },
  beforeUnmount() {
    if (this.sseConnection) {
      this.sseConnection.close();
    }
  }
};
</script>

<style scoped>
.h5-scan-container {
  min-height: 100vh;
  background-color: #f7f8fa;
  font-family: 'PingFang SC', sans-serif;
  padding-bottom: 40px;
}

.welcome-header {
  padding: 30px 20px 20px;
  background: white;
  border-radius: 0 0 24px 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}
.main-title {
  font-size: 28px;
  color: #323233;
  margin: 0 0 12px 0;
  font-weight: 600;
}
.sub-title {
  font-size: 16px;
  color: #969799;
  margin: 0;
  line-height: 1.5;
}

.scan-area-wrap {
  padding: 40px 30px;
  display: flex;
  justify-content: center;
}
.scan-area {
  position: relative;
  width: 100%;
  max-width: 320px;
  aspect-ratio: 1;
  background-color: white;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(25, 137, 250, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.scan-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.scan-icon {
  animation: float 3s ease-in-out infinite;
}
@keyframes float {
  0% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
  100% { transform: translateY(0px); }
}
.scan-text {
  font-size: 20px;
  color: #1989fa;
  font-weight: 500;
}

/* 扫码框四角 */
.scan-corner {
  position: absolute;
  width: 40px;
  height: 40px;
  border: 4px solid #1989fa;
}
.top-left { top: 10px; left: 10px; border-right: none; border-bottom: none; border-radius: 12px 0 0 0; }
.top-right { top: 10px; right: 10px; border-left: none; border-bottom: none; border-radius: 0 12px 0 0; }
.bottom-left { bottom: 10px; left: 10px; border-right: none; border-top: none; border-radius: 0 0 0 12px; }
.bottom-right { bottom: 10px; right: 10px; border-left: none; border-top: none; border-radius: 0 0 12px 0; }

.queue-status-block {
  padding: 0 20px;
  margin-top: 10px;
  animation: slideUp 0.4s ease-out forwards;
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.patient-banner {
  text-align: center;
  margin-bottom: 24px;
}
.greeting {
  font-size: 18px;
  color: #646566;
}
.patient-name {
  font-size: 24px;
  color: #323233;
  font-weight: 600;
  margin-left: 8px;
}

.status-cards-row {
  display: flex;
  gap: 15px;
}
.status-card {
  flex: 1;
  background: white;
  border-radius: 16px;
  padding: 20px 15px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.highlight-card {
  background: linear-gradient(135deg, #1989fa, #0570db);
  color: white;
}
.highlight-card.outline {
  background: white;
  border: 2px solid #1989fa;
  color: #323233;
}
.highlight-card.outline .card-label {
  color: #646566;
}
.highlight-card.outline .card-number {
  color: #1989fa;
}
.highlight-card.outline .card-unit {
  color: #969799;
}

.card-label {
  font-size: 16px;
  opacity: 0.9;
}
.card-value-wrap {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
}
.card-number {
  font-size: 48px;
  font-weight: bold;
  line-height: 1;
}
.card-unit {
  font-size: 18px;
}
</style>
