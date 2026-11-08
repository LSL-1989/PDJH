<template>
  <div class="dashboard-container">
    <div class="header">
      <h1>医疗业务效能监控中心 (SLA)</h1>
      <div class="time">{{ currentTime }}</div>
    </div>
    
    <div class="main-content">
      <!-- 核心指标区 -->
      <div class="kpi-panel">
        <div class="kpi-card" v-for="(item, key) in kpiMap" :key="key" :class="{ 'warning': item.isWarning }">
          <div class="kpi-title">{{ item.title }}</div>
          <div class="kpi-value">{{ item.value }} <span class="unit">{{ item.unit }}</span></div>
        </div>
      </div>

      <div class="lower-section">
        <!-- 左侧告警区 -->
        <div class="alert-panel">
          <h3><i class="el-icon-warning" style="color:#ff4d4f"></i> 超时拥堵与积压告警 (>45分钟)</h3>
          <div class="alert-list" v-if="overtimeAlerts.length > 0">
            <div class="alert-item" v-for="(alert, index) in overtimeAlerts" :key="index">
              <span class="alert-patient">{{ alert.patientName }} ({{ alert.patientCode }})</span>
              <span class="alert-time">已苦等 <strong>{{ alert.waitMinutes }}</strong> min</span>
            </div>
          </div>
          <div class="empty-state" v-else>
            全院流转通畅，无拥堵警报。
          </div>
        </div>
        
        <!-- 右侧图表区 -->
        <div class="chart-panel">
          <h3>门诊全天运力与接诊峰值分析</h3>
          <div id="peak-chart" style="width: 100%; height: calc(100% - 40px);"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'DirectorDashboard',
  data() {
    return {
      currentTime: new Date().toLocaleString(),
      kpiMap: {
        avgWaitTime: { title: '全院平均等待耗时', value: '0.0', unit: '分钟', isWarning: false },
        avgProcessTime: { title: '平均单客诊疗时长', value: '0.0', unit: '分钟', isWarning: false },
        waitingCount: { title: '当前全院滞留候诊人数', value: '0', unit: '人', isWarning: false },
        completedCount: { title: '本日全院累计看诊人次', value: '0', unit: '次', isWarning: false }
      },
      overtimeAlerts: [],
      chartInstance: null,
      timer: null
    }
  },
  mounted() {
    this.timer = setInterval(() => {
      this.currentTime = new Date().toLocaleString();
    }, 1000);
    this.initChart();
    this.fetchData();
    // 5秒轮询刷新大屏数据
    setInterval(this.fetchData, 5000);
  },
  beforeUnmount() {
    clearInterval(this.timer);
    if(this.chartInstance) {
      this.chartInstance.dispose();
    }
  },
  methods: {
    initChart() {
      const chartDom = document.getElementById('peak-chart');
      this.chartInstance = window.echarts.init(chartDom);
    },
    async fetchData() {
      try {
        const port = process.env.VUE_APP_BACKEND_PORT || 8081;
        const apiBase = `http://${window.location.hostname}:${port}/api/dashboard`;
        
        // 并行拉取三维数据
        const [overviewRes, alertRes, peakRes] = await Promise.all([
          axios.get(`${apiBase}/getOverviewMetrics`),
          axios.get(`${apiBase}/getOvertimeAlerts`),
          axios.get(`${apiBase}/getPeakAnalysis`)
        ]);

        this.updateKPIs(overviewRes.data);
        this.overtimeAlerts = alertRes.data;
        this.updateChart(peakRes.data);

      } catch (e) {
        console.error('获取看板数据失败', e);
      }
    },
    updateKPIs(data) {
      this.kpiMap.avgWaitTime.value = parseFloat(data.avgWaitTime).toFixed(1);
      // 如果超过 30 分钟，标红高亮
      this.kpiMap.avgWaitTime.isWarning = data.avgWaitTime > 30;

      this.kpiMap.avgProcessTime.value = parseFloat(data.avgProcessTime).toFixed(1);
      this.kpiMap.waitingCount.value = data.waitingCount || 0;
      this.kpiMap.completedCount.value = data.completedCount || 0;
    },
    updateChart(data) {
      // 构造 0-23 小时的横坐标轴
      const xData = Array.from({length: 24}, (_, i) => `${i}点`);
      // 初始化数据阵列
      const enqueueData = new Array(24).fill(0);
      const processData = new Array(24).fill(0);

      data.forEach(item => {
        let h = item.hourOfDay;
        if(h >= 0 && h < 24) {
          enqueueData[h] = item.enqueueCount;
          processData[h] = item.processCount;
        }
      });

      const option = {
        tooltip: { trigger: 'axis' },
        legend: { data: ['全院挂号涌入量', '全院实际接诊运力'], textStyle: { color: '#ccc' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', boundaryGap: false, data: xData, axisLabel: { color: '#8892B0' } },
        yAxis: { type: 'value', axisLabel: { color: '#8892B0' }, splitLine: { lineStyle: { color: '#233055' } } },
        series: [
          {
            name: '全院挂号涌入量',
            type: 'line',
            smooth: true,
            areaStyle: { opacity: 0.1 },
            itemStyle: { color: '#ff4d4f' },
            data: enqueueData
          },
          {
            name: '全院实际接诊运力',
            type: 'line',
            smooth: true,
            areaStyle: { opacity: 0.1 },
            itemStyle: { color: '#52c41a' },
            data: processData
          }
        ]
      };
      this.chartInstance.setOption(option);
    }
  }
}
</script>

<style scoped>
.dashboard-container {
  width: 100vw;
  height: 100vh;
  background-color: #0A192F;
  color: #CCD6F6;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.header {
  height: 80px;
  background: linear-gradient(180deg, #112240 0%, #0A192F 100%);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
  border-bottom: 1px solid #233055;
}

.header h1 {
  margin: 0;
  font-size: 28px;
  letter-spacing: 2px;
  color: #64FFDA;
  text-shadow: 0 0 10px rgba(100, 255, 218, 0.3);
}

.header .time {
  font-size: 18px;
  color: #8892B0;
  font-family: 'Courier New', Courier, monospace;
}

.main-content {
  flex: 1;
  padding: 20px 40px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.kpi-panel {
  display: flex;
  gap: 20px;
  height: 140px;
}

.kpi-card {
  flex: 1;
  background: rgba(17, 34, 64, 0.7);
  border: 1px solid #233055;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  transition: all 0.3s ease;
  box-shadow: 0 4px 6px rgba(0,0,0,0.3);
}

.kpi-card:hover {
  transform: translateY(-5px);
  border-color: #64FFDA;
  box-shadow: 0 6px 12px rgba(100,255,218,0.1);
}

.kpi-card.warning {
  border-color: #ff4d4f;
  box-shadow: 0 0 15px rgba(255,77,79,0.2);
}

.kpi-card.warning .kpi-value {
  color: #ff4d4f;
  text-shadow: 0 0 10px rgba(255,77,79,0.5);
}

.kpi-title {
  font-size: 16px;
  color: #8892B0;
  margin-bottom: 15px;
}

.kpi-value {
  font-size: 42px;
  font-weight: bold;
  color: #E6F1FF;
}

.unit {
  font-size: 16px;
  color: #8892B0;
  font-weight: normal;
}

.lower-section {
  flex: 1;
  display: flex;
  gap: 20px;
  min-height: 0;
}

.alert-panel {
  flex: 3;
  background: rgba(17, 34, 64, 0.7);
  border: 1px solid #233055;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.alert-panel h3, .chart-panel h3 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  color: #E6F1FF;
  border-bottom: 1px solid #233055;
  padding-bottom: 10px;
}

.alert-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 10px;
}

/* 自定义滚动条 */
.alert-list::-webkit-scrollbar {
  width: 6px;
}
.alert-list::-webkit-scrollbar-thumb {
  background: #233055;
  border-radius: 3px;
}

.alert-item {
  background: rgba(255, 77, 79, 0.05);
  border-left: 4px solid #ff4d4f;
  margin-bottom: 10px;
  padding: 12px 15px;
  border-radius: 0 4px 4px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.alert-patient {
  font-size: 16px;
  color: #E6F1FF;
  font-weight: 500;
}

.alert-time {
  color: #ff4d4f;
  font-size: 15px;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #52c41a;
  font-size: 18px;
}

.chart-panel {
  flex: 7;
  background: rgba(17, 34, 64, 0.7);
  border: 1px solid #233055;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  flex-direction: column;
}
</style>
