<template>
  <div class="dashboard-container">
    <div class="page-header">
      <div class="header-left">
        <h2>智能调度规则大盘</h2>
        <span class="header-sub">全院队列态势感知与管线调度执行监控核心</span>
      </div>
      <div class="header-right">
        <el-tag effect="dark" type="success" size="small"><i class="el-icon-cpu" /> 核心路由引擎：稳定运行中</el-tag>
        <span class="refresh-time">数据时间截: {{ currentTime }}</span>
      </div>
    </div>

    <!-- 待处理消息栏 -->
    <el-card v-if="pendingCount > 0" shadow="never" class="pending-bar" style="margin-bottom:16px;border:1px solid #FFE0B2;background:#FFF8E1;">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px;">
        <div style="font-weight:600;color:#E65100;">
          <i class="el-icon-warning" /> 待处理事项（{{ pendingCount }}）
        </div>
        <el-button text size="small" @click="loadPending">刷新</el-button>
      </div>
      <div v-for="item in pendingItems.slice(0, 8)" :key="item.id" class="pending-item" style="display:flex;align-items:center;justify-content:space-between;padding:6px 0;border-top:1px dashed #FFE0B2;">
        <div>
          <div style="font-size:13px;color:#333;">{{ item.title }}</div>
          <div style="font-size:12px;color:#888;">{{ item.detail }}</div>
        </div>
        <div>
          <el-button type="primary" size="small" plain @click="goPending(item)">去处理</el-button>
          <el-button text size="small" @click="markPendingDone(item)">忽略</el-button>
        </div>
      </div>
    </el-card>

    <!-- 顶部核心规则触发生态 (KPI) -->
    <el-row :gutter="20" class="kpi-row">
      <el-col :span="6">
        <div class="kpi-card glass-blue">
          <div class="kpi-icon"><i class="el-icon-s-operation text-primary" /></div>
          <div class="kpi-info">
            <div class="kpi-title">今日算法调度人次</div>
            <div class="kpi-value">
              <span class="number">{{ metrics.dispatchedCount }}</span>
            </div>
            <div class="kpi-desc">智能引擎分流执行中</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card glass-blue">
          <div class="kpi-icon"><i class="el-icon-warning-outline text-warning" /></div>
          <div class="kpi-info">
            <div class="kpi-title">过号拦截 / 拥堵分流数</div>
            <div class="kpi-value">
              <span class="number">{{ metrics.alertsCount }}</span>
            </div>
            <div class="kpi-desc">系统已自动重排触发规则</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card glass-blue">
          <div class="kpi-icon"><i class="el-icon-first-aid-kit text-danger" /></div>
          <div class="kpi-info">
            <div class="kpi-title">全院平均处理用时(分)</div>
            <div class="kpi-value">
              <span class="number">{{ metrics.avgProcessTime }}</span>
            </div>
            <div class="kpi-desc">接诊至完结时长</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card glass-blue">
          <div class="kpi-icon"><i class="el-icon-monitor text-success" /></div>
          <div class="kpi-info">
            <div class="kpi-title">全院平均等候用时(分)</div>
            <div class="kpi-value">
              <span class="number">{{ metrics.avgWaitTime }}</span>
            </div>
            <div class="kpi-desc">报到至呼叫时长</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 中段：四大管线运行雷达图 -->
    <div class="pipeline-section">
      <h3 class="section-title"><i class="el-icon-set-up" /> 主力管线协同调度网格 (Rule Engine Activity)</h3>
      <el-row :gutter="20">
        <!-- 门诊 -->
        <el-col :span="6">
          <div class="pipeline-card">
            <h4><i class="el-icon-office-building" /> 门诊分诊管线</h4>
            <div class="pipeline-status">
              <div class="status-item">
                <span class="label">当前主控策略</span>
                <el-tag size="small" effect="dark" type="warning">过号自动归队</el-tag>
              </div>
              <div class="status-item">
                <span class="label">老年人优先规则</span>
                <span class="value text-success">等候 {{ metrics.waitingCount || 0 }} 人</span>
              </div>
              <div class="status-item">
                <span class="label">极端拥堵熔断线</span>
                <span class="value text-primary">健康未触发</span>
              </div>
              <div id="outpatientGauge" class="pipeline-chart" />
            </div>
          </div>
        </el-col>
        <!-- 药房 -->
        <el-col :span="6">
          <div class="pipeline-card">
            <h4><i class="el-icon-box" /> 药房防滞留池</h4>
            <div class="pipeline-status">
              <div class="status-item">
                <span class="label">分流分配算法</span>
                <el-tag size="small" effect="dark" type="success">最短队列优先动态</el-tag>
              </div>
              <div class="status-item">
                <span class="label">中西药组拆单防混</span>
                <span class="value text-success">100%分流执行</span>
              </div>
              <div class="status-item">
                <span class="label">30分钟防滞留锁</span>
                <span class="value text-danger">超时告警 {{ metrics.alertsCount }} 笔</span>
              </div>
              <div id="pharmacyGauge" class="pipeline-chart" />
            </div>
          </div>
        </el-col>
        <!-- 体检 -->
        <el-col :span="6">
          <div class="pipeline-card">
            <h4><i class="el-icon-data-analysis" /> TSP 分布式体检路由</h4>
            <div class="pipeline-status">
              <div class="status-item">
                <span class="label">TSP 权重倾向</span>
                <el-tag size="small" effect="dark" type="primary">早高峰空腹优先</el-tag>
              </div>
              <div class="status-item">
                <span class="label">动态路线重算频率</span>
                <span class="value text-warning">完成 {{ metrics.completedCount || 0 }} 人次</span>
              </div>
              <div class="status-item">
                <span class="label">AI 寻路平均步数</span>
                <span class="value text-primary">4.1 个科室</span>
              </div>
              <div id="examGauge" class="pipeline-chart" />
            </div>
          </div>
        </el-col>
        <!-- 医技 -->
        <el-col :span="6">
          <div class="pipeline-card">
            <h4><i class="el-icon-c-scale-to-original" /> 医技异步连读队列</h4>
            <div class="pipeline-status">
              <div class="status-item">
                <span class="label">检验科采血策略</span>
                <el-tag size="small" effect="dark" type="success">多管合一叫号生效</el-tag>
              </div>
              <div class="status-item">
                <span class="label">放射科造影剂缓流</span>
                <span class="value text-success">30池位运转正常</span>
              </div>
              <div class="status-item">
                <span class="label">科室间并联冲突锁</span>
                <span class="value text-primary">等候 {{ metrics.waitingCount || 0 }} 人</span>
              </div>
              <div id="medtechGauge" class="pipeline-chart" />
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 底部：规则调优 ROI (投资回报图表) -->
    <div class="roi-section mt-4">
      <el-row :gutter="20">
        <el-col :span="24">
          <div class="chart-card glass-blue">
            <h3 class="chart-title"><i class="el-icon-s-data" /> 全院就诊动态趋势图 (当日实时入列量 vs 当日实时接诊量)</h3>
            <div id="roiChart" style="height: 380px;" />
          </div>
        </el-col>
      </el-row>
    </div>

  </div>
</template>

<script>
// 引入 ECharts
import * as echarts from 'echarts'
import { getOverviewMetrics, getOvertimeAlerts, getPeakAnalysis, getHeatmapMatrix, getPendingSummary, resolvePending } from '@/api/dashboard'

export default {
  name: 'Dashboard',
  data() {
    return {
      currentTime: new Date().toLocaleTimeString(),
      timer: null,
      charts: [],
      pendingCount: 0,
      pendingItems: [],
      metrics: {
        dispatchedCount: 0,
        alertsCount: 0,
        avgProcessTime: 0,
        avgWaitTime: 0,
        waitingCount: 0,
        completedCount: 0
      }
    }
  },
  mounted() {
    // 启动脉搏时钟
    this.timer = setInterval(() => {
      this.currentTime = new Date().toLocaleTimeString()
    }, 1000)

    // 请求数据并初始化图表
    this.fetchData()
    this.loadPending()

    window.addEventListener('resize', this.resizeCharts)
  },
  beforeUnmount() {
    if (this.timer) clearInterval(this.timer)
    window.removeEventListener('resize', this.resizeCharts)
    this.charts.forEach(c => c.dispose())
  },
  methods: {
    async loadPending() {
      try {
        const res = await getPendingSummary()
        if (res && res.code === 200 && res.data) {
          this.pendingCount = res.data.count || 0
          this.pendingItems = res.data.items || []
        }
      } catch (e) {
        this.pendingCount = 0
        this.pendingItems = []
      }
    },
    goPending(item) {
      if (item.route) {
        this.$router.push(item.route).catch(() => {})
      }
    },
    async markPendingDone(item) {
      try {
        await resolvePending(item.id)
        this.$message.success('已忽略')
        this.loadPending()
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    resizeCharts() {
      this.charts.forEach(c => c.resize())
    },
    async fetchData() {
      try {
        const overviewRes = await getOverviewMetrics()
        if (overviewRes && overviewRes.code === 200) {
          const data = overviewRes.data || {}
          this.metrics.dispatchedCount = (data.waitingCount || 0) + (data.completedCount || 0)
          this.metrics.waitingCount = data.waitingCount || 0
          this.metrics.completedCount = data.completedCount || 0
          this.metrics.avgWaitTime = parseFloat(data.avgWaitTime || 0).toFixed(1)
          this.metrics.avgProcessTime = parseFloat(data.avgProcessTime || 0).toFixed(1)
        }

        const alertsRes = await getOvertimeAlerts(45)
        if (alertsRes && alertsRes.code === 200) {
          this.metrics.alertsCount = (alertsRes.data || []).length || 0
        }

        const peakRes = await getPeakAnalysis()
        const peakData = (peakRes && peakRes.code === 200) ? (peakRes.data || []) : []

        const hmRes = await getHeatmapMatrix()
        const hmData = (hmRes && hmRes.code === 200) ? (hmRes.data || []) : []

        this.$nextTick(() => {
          this.initRoiChart(peakData)
          this.initGauges(hmData)
        })
      } catch (error) {
        console.error('Failed to load dashboard metrics', error)
      }
    },
    initRoiChart(peakData) {
      const chartDom = document.getElementById('roiChart')
      if (!chartDom) return
      const myChart = echarts.init(chartDom)

      const hours = ['08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00']

      // Map API data back to 08:00 through 17:00 (hours 8-17)
      const enqueueData = hours.map((h, index) => {
        const hourNum = index + 8
        const match = peakData.find(p => p.hourOfDay === hourNum)
        return match ? match.enqueueCount : 0
      })
      const processData = hours.map((h, index) => {
        const hourNum = index + 8
        const match = peakData.find(p => p.hourOfDay === hourNum)
        return match ? match.processCount : 0
      })

      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'cross', label: { backgroundColor: '#0B1120' }}
        },
        legend: {
          data: ['当日实时入列量 (Enqueue)', '当日实时接诊量 (Process)'],
          textStyle: { color: '#94a3b8' },
          top: 0
        },
        grid: { left: '3%', right: '4%', bottom: '3%', top: '40px', containLabel: true },
        xAxis: [
          {
            type: 'category',
            boundaryGap: false,
            data: hours,
            axisLabel: { color: '#94a3b8' }
          }
        ],
        yAxis: [
          {
            type: 'value',
            name: '人次',
            nameTextStyle: { color: '#94a3b8' },
            axisLabel: { color: '#94a3b8' },
            splitLine: { lineStyle: { color: '#1e293b' }}
          }
        ],
        series: [
          {
            name: '当日实时入列量 (Enqueue)',
            type: 'line',
            smooth: true,
            lineStyle: { width: 0 },
            showSymbol: false,
            areaStyle: {
              opacity: 0.8,
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(239, 68, 68, 0.3)' },
                { offset: 1, color: 'rgba(239, 68, 68, 0.02)' }
              ])
            },
            data: enqueueData.length > 0 ? enqueueData : [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            itemStyle: { color: '#ef4444' }
          },
          {
            name: '当日实时接诊量 (Process)',
            type: 'line',
            smooth: true,
            lineStyle: { width: 3, shadowColor: 'rgba(14, 165, 233, 0.8)', shadowBlur: 10 },
            showSymbol: true,
            symbolSize: 8,
            areaStyle: {
              opacity: 0.8,
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(14, 165, 233, 0.6)' },
                { offset: 1, color: 'rgba(14, 165, 233, 0.05)' }
              ])
            },
            data: processData.length > 0 ? processData : [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            itemStyle: { color: '#0ea5e9', borderColor: '#fff', borderWidth: 2 }
          }
        ]
      }
      myChart.setOption(option)
      this.charts.push(myChart)
    },
    initGauges(hmData) {
      const ids = ['outpatientGauge', 'pharmacyGauge', 'examGauge', 'medtechGauge']
      const colors = ['#0ea5e9', '#10b981', '#f59e0b', '#8b5cf6']
      const businessTypes = ['CLINIC', 'PHARMACY', 'HEALTH_CHECKUP', 'OUTPATIENT_EXAM']

      // Calc percentage for each pipe (assuming default value if empty)
      const values = ids.map((id, index) => {
        const matchingDepts = hmData.filter(d => d.businessType === businessTypes[index])
        if (matchingDepts && matchingDepts.length > 0) {
          const totalWait = matchingDepts.reduce((sum, d) => sum + (d.waitingCount || 0), 0)
          const totalCap = matchingDepts.reduce((sum, d) => sum + (d.capacityLimit || 50), 0)
          const percent = totalCap > 0 ? (totalWait / totalCap) * 100 : 0
          return Math.min(Math.round(percent), 100)
        }
        return 0 // Default to 0 load if no data vs mock data 78, 92..
      })

      ids.forEach((id, index) => {
        const dom = document.getElementById(id)
        if (!dom) return
        const instance = echarts.init(dom)
        instance.setOption({
          series: [
            {
              type: 'gauge',
              startAngle: 180,
              endAngle: 0,
              center: ['50%', '80%'],
              radius: '100%',
              min: 0,
              max: 100,
              splitNumber: 2,
              axisLine: {
                lineStyle: {
                  width: 8,
                  color: [
                    [values[index] / 100, colors[index]],
                    [1, '#1e293b']
                  ]
                }
              },
              pointer: { show: false },
              axisTick: { show: false },
              splitLine: { show: false },
              axisLabel: { show: false },
              detail: {
                show: true,
                offsetCenter: [0, '-15%'],
                fontSize: 18,
                fontWeight: 'bold',
                color: colors[index],
                formatter: '{value}%'
              },
              data: [{ value: values[index], name: '引擎负荷' }],
              title: {
                show: true,
                offsetCenter: [0, '25%'],
                fontSize: 12,
                color: '#64748b'
              }
            }
          ]
        })
        this.charts.push(instance)
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  padding: 24px;
  background: #0B1120; // 极深科技蓝背景
  min-height: calc(100vh - 50px);
  color: #f8fafc;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
  border-bottom: 1px solid rgba(14, 165, 233, 0.2);
  padding-bottom: 16px;

  .header-left {
    h2 {
      margin: 0 0 8px 0;
      font-size: 24px;
      font-weight: 600;
      color: #fff;
      display: flex;
      align-items: center;
      &::before {
        content: '';
        display: inline-block;
        width: 4px;
        height: 20px;
        background: #0ea5e9;
        margin-right: 12px;
        border-radius: 2px;
        box-shadow: 0 0 8px rgba(14, 165, 233, 0.8);
      }
    }
    .header-sub {
      color: #94a3b8;
      font-size: 14px;
      margin-left: 16px;
    }
  }

  .header-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 8px;

    .refresh-time {
      font-size: 12px;
      color: #64748b;
    }
  }
}

/* 玻璃拟态基础卡片 */
.glass-blue {
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(30, 41, 59, 0.8);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
  border-radius: 12px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 2px;
    background: linear-gradient(90deg, transparent, rgba(14, 165, 233, 0.4), transparent);
  }
}

.kpi-row {
  margin-bottom: 24px;
}

/* 顶部统管核心卡片 */
.kpi-card {
  display: flex;
  padding: 24px 20px;
  align-items: center;
  transition: transform 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 25px rgba(14, 165, 233, 0.15);
    border-color: rgba(14, 165, 233, 0.4);
  }

  .kpi-icon {
    font-size: 38px;
    width: 64px;
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(15, 23, 42, 0.8);
    border-radius: 16px;
    margin-right: 20px;
    border: 1px solid rgba(51, 65, 85, 0.5);

    /* 渐变与主题色支持 */
    &.text-primary i { color: #0ea5e9; text-shadow: 0 0 10px rgba(14,165,233,0.5); }
    &.text-warning i { color: #f59e0b; text-shadow: 0 0 10px rgba(245,158,11,0.5); }
    &.text-danger i  { color: #ef4444; text-shadow: 0 0 10px rgba(239,68,68,0.5); }
    &.text-success i { color: #10b981; text-shadow: 0 0 10px rgba(16,185,129,0.5); }
  }

  .kpi-info {
    flex: 1;

    .kpi-title {
      font-size: 13px;
      color: #94a3b8;
      margin-bottom: 6px;
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .kpi-value {
      font-size: 32px;
      font-weight: 700;
      color: #f8fafc;
      margin-bottom: 8px;
      line-height: 1;
      .number { font-family: 'Din', 'Roboto', sans-serif; }
    }

    .kpi-desc {
      font-size: 12px;
      color: #64748b;
      .text-success { color: #10b981; font-weight: 600; }
      .text-warning { color: #f59e0b; font-weight: 600; }
      .text-danger  { color: #ef4444; font-weight: 600; }
      .text-primary { color: #0ea5e9; font-weight: 600; }
    }
  }
}

/* 中段管线区 */
.pipeline-section {
  margin-bottom: 24px;

  .section-title {
    color: #e2e8f0;
    font-size: 18px;
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    font-weight: 500;
    i { margin-right: 8px; color: #0ea5e9; font-size: 20px;}
  }
}

/* 垂直管线网格卡片 */
.pipeline-card {
  background: rgba(15, 23, 42, 0.4);
  border: 1px solid rgba(30, 41, 59, 1);
  border-radius: 12px;
  padding: 20px;
  height: 380px;
  display: flex;
  flex-direction: column;
  transition: all 0.3s;
  box-shadow: inset 0 0 30px rgba(0,0,0,0.5);

  &:hover {
    background: rgba(30, 41, 59, 0.5);
    border-color: rgba(14, 165, 233, 0.3);
  }

  h4 {
    margin: 0 0 20px 0;
    font-size: 16px;
    color: #f8fafc;
    display: flex;
    align-items: center;
    i { margin-right: 8px; color: #94a3b8; font-size: 18px; }
  }

  .pipeline-status {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 12px;

    .status-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: rgba(0, 0, 0, 0.25);
      padding: 12px 14px;
      border-radius: 8px;
      border-left: 3px solid #334155;

      .label {
        font-size: 13px;
        color: #94a3b8;
      }
      .value {
        font-size: 13px;
        font-weight: 500;
        &.text-success { color: #10b981; }
        &.text-warning { color: #f59e0b; }
        &.text-danger  { color: #ef4444; }
        &.text-primary { color: #0ea5e9; }
      }
    }

    .pipeline-chart {
      flex: 1;
      width: 100%;
      min-height: 100px;
      margin-top: 10px;
    }
  }
}

.chart-card {
  padding: 24px;
  .chart-title {
    margin: 0 0 20px 0;
    font-size: 16px;
    color: #f1f5f9;
    font-weight: 500;
    display: flex;
    align-items: center;
    i {
      margin-right: 8px;
      color: #0ea5e9;
    }
  }
}
.mt-4 { margin-top: 24px; }
</style>
