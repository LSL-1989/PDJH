<template>
  <div class="global-hub-container">
    <div class="header">
      <div class="brand">
        <div class="logo-box"></div>
        <h1>{{ hospitalName }}门诊服务中心</h1>
      </div>
      <div class="time">{{ currentTime }}</div>
    </div>
    
    <div class="content-wrapper">
      <div class="left-panel">
        <h2 class="panel-title">各科室/窗口当前等候人数</h2>
        <div class="dept-grid">
          <recycle-scroller
            v-if="summaryData.length > 0"
            class="scroller"
            :items="chunkedSummaryData"
            :item-size="140"
            key-field="id"
            v-slot="{ item: row }"
          >
            <div class="dept-row">
              <div class="dept-card" v-for="(dept, i) in row.items" :key="dept.deptId || i" style="flex: 1;">
                <div class="dept-name">{{ dept.deptName || `科室代码: ${dept.deptId}` }}</div>
                <div class="dept-status">
                  <div class="dept-count">前方等候 <span class="num">{{ dept.waitCount }}</span> 人</div>
                  <div class="dept-eta" v-if="dept.waitCount > 0">预计 {{ dept.estimatedWaitMinutes || (dept.waitCount * 3) }} 分钟</div>
                  <div class="dept-eta-free" v-else>当前空闲</div>
                </div>
              </div>
              <!-- 占位补齐 -->
              <div v-for="empty in (3 - row.items.length)" :key="'empty'+empty" style="flex: 1; visibility: hidden;"></div>
            </div>
          </recycle-scroller>

          <!-- 缺省提示 -->
          <div class="empty-tip" v-if="summaryData.length === 0">
            <div class="empty-icon">☕</div>
            暂无排队数据，大夫们正火力全开
          </div>
        </div>
      </div>
      
      <div class="right-panel">
        <h2 class="panel-title highlight-title"><span class="blink-dot"></span> 正在呼叫 (请前往就诊)</h2>
        <div class="call-list">
          <transition-group name="list" tag="div">
            <div 
              class="call-item" 
              v-for="(call, index) in recentCalls" 
              :key="call.id"
              :class="{ 'latest-call': index === 0 && call.isNew }"
            >
              <div class="call-index" v-if="index > 0">{{ index }}</div>
              <div class="call-index-new" v-else>NEW</div>
              <span class="call-text">{{ call.text }}</span>
            </div>
          </transition-group>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "GlobalHub",
  data() {
    return {
      hospitalName: window.$sysConfig ? window.$sysConfig['HospitalName'] : '排队叫号系统',
      currentTime: this.$moment().format("YYYY年MM月DD日 HH:mm:ss"),
      timer: null,
      summaryData: [],
      recentCalls: [],
      callIdCounter: 0,
      refreshSummaryTimer: null,
      stompClient: null,
      retryCount: 0,
      retryTimer: null
    };
  },
  computed: {
    chunkedSummaryData() {
      const result = [];
      const data = this.summaryData;
      for (let i = 0; i < data.length; i += 3) {
        result.push({
          id: 'row_' + i,
          items: data.slice(i, i + 3)
        });
      }
      return result;
    }
  },
  mounted() {
    this.startClock();
    this.fetchSummary();
    this.initWebSocket();
    
    // 每 30 秒轮询一次汇总数据（防止断连导致不一致）
    this.refreshSummaryTimer = setInterval(() => {
      this.fetchSummary();
    }, 30000);
  },
  beforeUnmount() {
    clearInterval(this.timer);
    clearInterval(this.refreshSummaryTimer);
    if (this.retryTimer) clearTimeout(this.retryTimer);
  },
  methods: {
    startClock() {
      this.timer = setInterval(() => {
        this.currentTime = this.$moment().format("YYYY年MM月DD日 HH:mm:ss");
      }, 1000);
    },
    fetchSummary() {
      this.$axios.get("/Screens/GlobalSummary").then(res => {
        if (res.data && res.data.Result) {
          this.summaryData = res.data.Data;
        }
      });
    },
    initWebSocket() {
      if (this.$proxy) {
        this.stompClient = this.$proxy;
        this.checkAndSubscribe();
      }
    },
    checkAndSubscribe() {
      if (this.stompClient && this.stompClient.connected) {
         this.retryCount = 0;
         this.stompClient.subscribe('/topic/global-call', (message) => {
             const data = JSON.parse(message.body);
             if (data.type === 'CALL_MSG') {
               this.addRecentCall(data.callText);
               setTimeout(this.fetchSummary, 1500);
             }
          });
      } else {
         this.retryCount++;
         const delay = Math.min(this.retryCount * 1000, 30000); 
         this.retryTimer = setTimeout(this.checkAndSubscribe, delay);
      }
    },
    addRecentCall(text) {
      const newCall = { id: this.callIdCounter++, text, isNew: true };
      this.recentCalls.unshift(newCall);
      
      // 15秒后移除新呼叫强高亮状态
      setTimeout(() => {
        newCall.isNew = false;
      }, 15000);
      
      if (this.recentCalls.length > 20) {
        this.recentCalls.pop();
      }
    }
  }
};
</script>

<style scoped>
.global-hub-container {
  width: 100vw;
  height: 100vh;
  background-color: #0d1117;
  color: #c9d1d9;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: inherit;
}
.header {
  height: 12vh;
  background: linear-gradient(180deg, #161b22 0%, #0d1117 100%);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4vw;
  border-bottom: 1px solid #30363d;
  box-shadow: 0 4px 20px rgba(0,0,0,0.5);
  z-index: 10;
}
.brand {
  display: flex;
  align-items: center;
  gap: 1.5vw;
}
.logo-box {
  width: 3.5vw;
  height: 3.5vw;
  background: linear-gradient(135deg, #58a6ff 0%, #1f6feb 100%);
  border-radius: 12px;
  box-shadow: 0 0 15px rgba(88, 166, 255, 0.4);
}
.header h1 {
  font-size: 2.5vw;
  margin: 0;
  color: #f0f6fc;
  letter-spacing: 4px;
  font-weight: 900;
  text-shadow: 0 2px 10px rgba(0,0,0,0.5);
}
.header .time {
  font-size: 1.8vw;
  color: #8b949e;
  font-weight: 600;
  background: #21262d;
  padding: 0.8vw 1.5vw;
  border-radius: 50px;
  border: 1px solid #30363d;
}
.content-wrapper {
  display: flex;
  flex: 1;
  padding: 2.5vw;
  gap: 2.5vw;
}
.left-panel {
  flex: 5.5;
  background-color: #161b22;
  border-radius: 1.5rem;
  padding: 2.5vw;
  display: flex;
  flex-direction: column;
  border: 1px solid #30363d;
  box-shadow: 0 10px 40px rgba(0,0,0,0.4);
}
.right-panel {
  flex: 4.5;
  background: linear-gradient(180deg, #1c2128 0%, #161b22 100%);
  border-radius: 1.5rem;
  padding: 2.5vw;
  display: flex;
  flex-direction: column;
  border: 1px solid #30363d;
  box-shadow: 0 10px 40px rgba(0,0,0,0.4);
  position: relative;
  overflow: hidden;
}
/* 发光装饰 */
.right-panel::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 50% 0%, rgba(31, 111, 235, 0.15), transparent 60%);
  pointer-events: none;
}
.panel-title {
  font-size: 2vw;
  color: #8b949e;
  text-align: left;
  margin-top: 0;
  margin-bottom: 2vw;
  border-bottom: 2px solid #30363d;
  padding-bottom: 1.5vw;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 1vw;
}
.highlight-title {
  color: #58a6ff;
}
.blink-dot {
  width: 1.2vw;
  height: 1.2vw;
  background-color: #f85149;
  border-radius: 50%;
  animation: pulse-red 1.5s infinite;
}
@keyframes pulse-red {
  0% { box-shadow: 0 0 0 0 rgba(248, 81, 73, 0.7); }
  70% { box-shadow: 0 0 0 15px rgba(248, 81, 73, 0); }
  100% { box-shadow: 0 0 0 0 rgba(248, 81, 73, 0); }
}
.dept-grid {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}
.scroller {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
}
.dept-row {
  display: flex;
  gap: 1.5vw;
  margin-bottom: 1.5vw;
}
.dept-card {
  width: calc(33.333% - 1vw);
  background: #21262d;
  border-radius: 1.2rem;
  padding: 1.5vw;
  border: 1px solid #30363d;
  border-top: 4px solid #3fb950;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.dept-name {
  font-size: 1.4vw;
  color: #f0f6fc;
  margin-bottom: 1.5vw;
  font-weight: bold;
}
.dept-status {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}
.dept-count {
  font-size: 1.1vw;
  color: #8b949e;
}
.dept-count .num {
  font-size: 3vw;
  color: #f2cc60;
  font-weight: 900;
  line-height: 1;
}
.dept-eta {
  font-size: 1vw;
  background: rgba(242, 204, 96, 0.1);
  color: #f2cc60;
  padding: 0.3vw 0.8vw;
  border-radius: 4px;
  font-weight: bold;
}
.dept-eta-free {
  font-size: 1vw;
  background: rgba(63, 185, 80, 0.1);
  color: #3fb950;
  padding: 0.3vw 0.8vw;
  border-radius: 4px;
  font-weight: bold;
}
.empty-tip {
  width: 100%;
  text-align: center;
  color: #484f58;
  font-size: 1.8vw;
  margin-top: 10vh;
  font-weight: bold;
}
.empty-icon {
  font-size: 4vw;
  margin-bottom: 1vw;
  opacity: 0.5;
}
.call-list {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 1vw;
  z-index: 1;
}
.call-item {
  background: rgba(48, 54, 61, 0.5);
  margin-bottom: 1vw;
  padding: 1.5vw 2vw;
  border-radius: 1rem;
  border: 1px solid #484f58;
  display: flex;
  align-items: center;
  gap: 1.5vw;
  transition: all 0.3s ease;
}
.call-index {
  font-size: 1.5vw;
  color: #8b949e;
  font-weight: 900;
  width: 3vw;
  text-align: center;
}
.call-index-new {
  font-size: 1.2vw;
  background: #1f6feb;
  color: white;
  padding: 0.2vw 0.8vw;
  border-radius: 6px;
  font-weight: 900;
  word-break: keep-all;
}
.call-text {
  font-size: 1.8vw;
  color: #c9d1d9;
  letter-spacing: 2px;
  font-weight: bold;
}
/* 最新呼叫强高亮展示 */
.latest-call {
  background: linear-gradient(90deg, rgba(88,166,255,0.15) 0%, rgba(31,111,235,0.05) 100%);
  border: 2px solid #58a6ff;
  border-left: 8px solid #58a6ff;
  transform: scale(1.02);
  transform-origin: left center;
  box-shadow: 0 0 30px rgba(88, 166, 255, 0.3);
}
.latest-call .call-text {
  font-size: 2.4vw;
  color: #ffffff;
  font-weight: 900;
  text-shadow: 0 2px 10px rgba(0,0,0,0.8);
}

/* 列表过渡动画 */
.list-enter-active {
  transition: all 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.list-leave-active {
  transition: all 0.5s ease;
  position: absolute;
}
.list-enter {
  opacity: 0;
  transform: translateY(-50px) scale(0.9);
}
.list-leave-to {
  opacity: 0;
  transform: translateX(100px);
}
.list-move {
  transition: transform 0.6s cubic-bezier(0.25, 0.8, 0.25, 1);
}
</style>
