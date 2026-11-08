<template>
  <div class="director-desk">
    <el-tabs v-model="activeTab" class="desk-tabs" @tab-click="handleTabClick">
      <el-tab-pane label="态势大盘" name="dashboard">

        <!-- 页面标题栏 -->
        <div class="desk-header">
          <div class="desk-title">
            <i class="el-icon-microscope" />
            <div>
              <h2>检验中心主任工作台</h2>
              <span class="desk-subtitle">采血排队与样本运流全景监控 · 数据截至 {{ currentTime }}</span>
            </div>
          </div>
          <div class="header-actions">
            <el-button type="primary" size="small" icon="el-icon-refresh" :loading="refreshing" @click="refreshAll">刷新数据</el-button>
          </div>
        </div>

        <!-- KPI 数字卡 -->
        <div class="kpi-row">
          <div v-for="kpi in kpiCards" :key="kpi.key" class="kpi-card">
            <div class="kpi-icon-wrapper" :style="{ background: kpi.iconBg, color: kpi.iconColor }">
              <i :class="kpi.icon" />
            </div>
            <div class="kpi-info">
              <div class="kpi-title">{{ kpi.title }}</div>
              <div class="kpi-value" :style="{ color: kpi.valueColor || '#202124' }">
                {{ kpi.value }} <span class="kpi-unit">{{ kpi.unit }}</span>
              </div>
              <div v-if="kpi.trend" class="kpi-trend">
                <i :class="kpi.trend > 0 ? 'el-icon-top' : 'el-icon-bottom'" :style="{ color: kpi.trend > 0 ? '#EA4335' : '#34A853' }" />
                <span :style="{ color: kpi.trend > 0 ? '#EA4335' : '#34A853', fontSize: '12px' }">{{ Math.abs(kpi.trend) }}% 倍增</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 主体区域 -->
        <div class="main-grid">

          <!-- 左列: 热力图与线图 -->
          <div class="col-left">
            <!-- 采血窗口拥堵热力矩阵 -->
            <div class="section-card">
              <div class="section-header">
                <div class="section-title"><i class="el-icon-hot-water" style="color:#EA4335;" /> 采血窗口实时等候负荷热力</div>
                <div style="display:flex;align-items:center;gap:6px;font-size:12px;color:#80868B;">
                  <span class="legend-dot" style="background:#E6F4EA;border:1px solid #34A853;" />空闲/畅通
                  <span class="legend-dot" style="background:#FEF7E0;border:1px solid #FBBC04;" />繁忙
                  <span class="legend-dot" style="background:#FCE8E6;border:1px solid #EA4335;" />告急
                </div>
              </div>
              <div v-loading="heatmapLoading" class="heatmap-grid">
                <el-empty
                  v-if="!heatmapLoading && heatmapData.length === 0"
                  description="暂无科室数据，请先在科室管理维护或完成 HIS 同步"
                  :image-size="80"
                />
                <div v-for="win in heatmapData" :key="win.windowId" class="heat-cell" :style="getCellStyle(win.statusLevel)">
                  <div class="heat-dept-name">{{ win.windowName }}</div>
                  <div class="heat-count">{{ win.waitingCount }}</div>
                  <div class="heat-label">管等候</div>
                </div>
              </div>
            </div>

            <!-- TAT (样本处理时效) 图表 -->
            <div class="section-card" style="margin-top:20px;">
              <div class="section-header">
                <div class="section-title"><i class="el-icon-data-line" style="color:#1A73E8;" /> 样本周转时间 (TAT) 达标率趋势监控</div>
                <div style="font-size:12px;color:#80868B;">生化(绿线) vs 临检(蓝线)</div>
              </div>
              <div id="tatTrendChart" style="width:100%;height:220px;" />
            </div>
          </div>

          <!-- 右列: 算路策略与预警墙 -->
          <div class="col-right">

            <!-- 算路策略控制台 -->
            <div class="strategy-console section-card">
              <div class="strategy-header">
                <span class="strategy-title"><i class="el-icon-magic-stick" style="color:#1A73E8;" /> 采血叫号策略引擎</span>
                <div style="display:flex;align-items:center;gap:12px;">
                  <span class="strategy-current-label">当前生效：</span>
                  <el-button type="primary" size="small" :loading="strategySaving" @click="saveStrategy">立即部署</el-button>
                </div>
              </div>
              <div class="strategy-cards vertical-layout">
                <div
                  v-for="s in strategies"
                  :key="s.key"
                  class="strategy-card"
                  :class="{ active: selectedStrategy === s.key }"
                  @click="selectedStrategy = s.key"
                >
                  <div class="sc-icon" :style="{ background: s.bg }"><i :class="s.icon" /></div>
                  <div class="sc-body">
                    <div class="sc-name">{{ s.name }}</div>
                    <div class="sc-desc">{{ s.desc }}</div>
                  </div>
                  <i v-if="selectedStrategy === s.key" class="el-icon-check sc-check" />
                </div>
              </div>
            </div>

            <!-- 危机值标本墙 -->
            <div class="urgent-wall section-card" style="margin-top:20px;height:calc(100% - 310px);min-height:220px;">
              <div class="section-header" style="border-bottom:none;padding-bottom:0;">
                <div class="section-title">
                  <i class="el-icon-warning" style="color:#EA4335;" /> 加急与危机值预警雷达
                  <el-badge is-dot class="item" style="margin-left: 8px;" />
                </div>
              </div>
              <div class="urgent-list">
                <div v-for="ug in urgentList" :key="ug.id" class="urgent-item">
                  <div class="ug-tag">急</div>
                  <div class="ug-body">
                    <div class="ug-title">{{ ug.patientName }} · {{ ug.testType }}</div>
                    <div class="ug-time">触发于 {{ ug.time }} · 剩余处理限时: <span style="color:#EA4335;font-weight:bold;">{{ ug.limitMin }} min</span></div>
                  </div>
                  <el-button size="small" type="danger" plain>拔高优位</el-button>
                </div>
              </div>
            </div>

          </div>
        </div>

      </el-tab-pane>

      <el-tab-pane label="队列明细管控" name="queueList" :lazy="true">
        <queue-list v-if="activeTab === 'queueList'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import QueueList from './queueList.vue'
import { getConfigByKey, saveConfig } from '@/api/systemConfig'

export default {
  name: 'LabDesk',
  components: {
    QueueList
  },
  data() {
    return {
      activeTab: 'dashboard',
      currentTime: '',
      refreshing: false,
      tatChart: null,
      timer: null,
      urgentTimer: null,
      // P0-3 算路策略
      selectedStrategy: 'MULTI_TUBE_MERGE',
      activeStrategy: 'MULTI_TUBE_MERGE',
      strategySaving: false,
      strategies: [
        {
          key: 'MULTI_TUBE_MERGE',
          name: '多管智能合并',
          desc: '基于LIS医嘱自动分析，将同一患者血项管打包归集至同一最快空闲窗口，减少排队折返',
          icon: 'el-icon-set-up',
          bg: 'linear-gradient(135deg,#1A73E8,#0D47A1)'
        },
        {
          key: 'ELDER_CHILD_FAST',
          name: '绿色通道提权',
          desc: '依据身份识别，赋予>70岁老人及<3岁儿童绝对优先插队权重',
          icon: 'el-icon-medal-1',
          bg: 'linear-gradient(135deg,#34A853,#1E7E34)'
        },
        {
          key: 'FIFO_STANDARD',
          name: '签到序列表',
          desc: '平滑期常规模式，严格执行先签到先叫号机制（FIFO）',
          icon: 'el-icon-tickets',
          bg: 'linear-gradient(135deg,#80868B,#5F6368)'
        }
      ],
      kpiCards: [
        { key: 'total', title: '今日已采血人次', value: 0, unit: '人', icon: 'el-icon-user', iconBg: '#E8F0FE', iconColor: '#1A73E8', trend: 0 },
        { key: 'waiting', title: '当前候采血', value: 0, unit: '人', icon: 'el-icon-hot-water', iconBg: '#FCE8E6', iconColor: '#EA4335', trend: 0 },
        { key: 'analysis', title: '送检机器分析中', value: 0, unit: '管', icon: 'el-icon-loading', iconBg: '#FEF7E0', iconColor: '#FBBC04' },
        { key: 'done', title: '今日已发报告', value: 0, unit: '份', icon: 'el-icon-document-checked', iconBg: '#E6F4EA', iconColor: '#34A853' }
      ],
      heatmapLoading: false,
      heatmapData: [],
      urgentList: []
    }
  },
  mounted() {
    this.updateTime()
    this.timer = setInterval(this.updateTime, 1000)
    this.$nextTick(() => { this.initTATChart() })
    this.loadKpiStats()
    this.loadHeatmap()
    this.loadUrgentList()
    this.urgentTimer = setInterval(() => {
      if (this.urgentList.length > 0) this.urgentList[0].limitMin--
    }, 60000)
  },
  beforeUnmount() {
    clearInterval(this.timer)
    clearInterval(this.urgentTimer)
    if (this.tatChart) { this.tatChart.dispose(); this.tatChart = null }
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    updateTime() {
      const now = new Date()
      this.currentTime = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    },
    refreshAll() {
      this.refreshing = true
      this.loadKpiStats()
      this.loadHeatmap()
      this.loadUrgentList()
      setTimeout(() => { this.refreshing = false; this.$message.success('实验室数据已同步') }, 800)
    },
    loadKpiStats() {
      import('@/utils/request').then(m => {
        m.default({ url: '/api/dashboard/getOverviewMetrics', method: 'get' })
          .then(res => {
            const data = res.data || {}
            this.kpiCards = [
              { key: 'total', title: '今日已采血人次', value: (data.completedCount || 0) + (data.waitingCount || 0), unit: '人', icon: 'el-icon-user', iconBg: '#E8F0FE', iconColor: '#1A73E8', trend: 0 },
              { key: 'waiting', title: '当前候采血', value: data.waitingCount || 0, unit: '人', icon: 'el-icon-hot-water', iconBg: '#FCE8E6', iconColor: '#EA4335', trend: 0 },
              { key: 'analysis', title: '送检机器分析中', value: 0, unit: '管', icon: 'el-icon-loading', iconBg: '#FEF7E0', iconColor: '#FBBC04' },
              { key: 'done', title: '今日已发报告', value: data.completedCount || 0, unit: '份', icon: 'el-icon-document-checked', iconBg: '#E6F4EA', iconColor: '#34A853' }
            ]
          })
          .catch(() => {})
      })
    },
    loadUrgentList() {
      import('@/utils/request').then(m => {
        m.default({ url: '/api/dashboard/getOvertimeAlerts', method: 'get', params: { thresholdMinutes: 45 } })
          .then(res => {
            const list = Array.isArray(res.data) ? res.data : []
            this.urgentList = list.slice(0, 5).map((item, idx) => ({
              id: 'u' + (idx + 1),
              patientName: item.patientName || '未知',
              testType: item.testType || '常规检验',
              time: item.callTime ? new Date(item.callTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '--:--',
              limitMin: item.waitMinutes || 0
            }))
          })
          .catch(() => {})
      })
    },
    saveStrategy() {
      this.strategySaving = true
      getConfigByKey('RULE_LAB').then(res => {
        let cfg = {}
        try { cfg = JSON.parse((res.data && (res.data.configValue || res.data.ConfigValue)) || '{}') } catch (e) {}
        cfg.labStrategy = this.selectedStrategy
        return saveConfig({ configKey: 'RULE_LAB', configValue: JSON.stringify(cfg), description: '检验科策略' })
      }).then(() => {
        this.activeStrategy = this.selectedStrategy
        const name = this.strategies.find(s => s.key === this.selectedStrategy)?.name
        this.$message.success(`采血派号策略已切换为「${name}」`)
      }).catch(() => this.$message.error('策略保存失败'))
        .finally(() => { this.strategySaving = false })
    },
    getCellStyle(statusLevel) {
      if (statusLevel === 'CONGESTED') return { background: '#FCE8E6', borderColor: '#EA4335', color: '#B31412' }
      if (statusLevel === 'BUSY') return { background: '#FEF7E0', borderColor: '#FBBC04', color: '#B06000' }
      return { background: '#E6F4EA', borderColor: '#34A853', color: '#137333' }
    },
    initTATChart() {
      const el = document.getElementById('tatTrendChart')
      if (!el) return
      this.tatChart = this.$echarts.init(el)
      const times = ['08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00']
      // 从 API 获取峰值分析数据
      import('@/utils/request').then(m => {
        m.default({ url: '/api/dashboard/getPeakAnalysis', method: 'get' })
          .then(res => {
            const peakData = Array.isArray(res.data) ? res.data : []
            const bioChemData = times.map((_, idx) => {
              const match = peakData.find(p => p.hourOfDay === idx + 8)
              return match ? Math.round(match.processCount * 0.6) : 0
            })
            const clinicalData = times.map((_, idx) => {
              const match = peakData.find(p => p.hourOfDay === idx + 8)
              return match ? Math.round(match.processCount * 0.4) : 0
            })
            this.tatChart.setOption({
              tooltip: { trigger: 'axis' },
              legend: { data: ['生化标本 (超2h率)', '临检标本 (超30m率)'], top: 0 },
              grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
              xAxis: { type: 'category', boundaryGap: false, data: times },
              yAxis: { type: 'value', axisLabel: { formatter: '{value} %' }, max: 100 },
              series: [
                { name: '生化标本 (超2h率)', type: 'line', smooth: true, data: bioChemData, itemStyle: { color: '#34A853' }, areaStyle: { color: 'rgba(52,168,83,0.1)' }},
                { name: '临检标本 (超30m率)', type: 'line', smooth: true, data: clinicalData, itemStyle: { color: '#1A73E8' }, areaStyle: { color: 'rgba(26,115,232,0.1)' }}
              ]
            })
          })
          .catch(() => {
            // 保持空白图表
            this.tatChart.setOption({
              tooltip: { trigger: 'axis' },
              legend: { data: ['生化标本 (超2h率)', '临检标本 (超30m率)'], top: 0 },
              grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
              xAxis: { type: 'category', boundaryGap: false, data: times },
              yAxis: { type: 'value', axisLabel: { formatter: '{value} %' }, max: 100 },
              series: [
                { name: '生化标本 (超2h率)', type: 'line', smooth: true, data: [], itemStyle: { color: '#34A853' }},
                { name: '临检标本 (超30m率)', type: 'line', smooth: true, data: [], itemStyle: { color: '#1A73E8' }}
              ]
            })
          })
      })
      window.addEventListener('resize', this.handleResize)
    },
    handleResize() {
      if (this.tatChart) {
        this.tatChart.resize()
      }
    },
    handleTabClick(tab) {
      if (tab.name === 'dashboard') {
        this.$nextTick(() => { this.handleResize() })
      }
    },
    loadHeatmap() {
      this.heatmapLoading = true
      import('@/utils/request').then(m => {
        m.default({ url: '/api/dashboard/heatmap-matrix', method: 'get' })
          .then(res => {
            const data = Array.isArray(res.data) ? res.data : []
            // 过滤检验科数据作为采血窗口
            this.heatmapData = data.filter(d => d.deptName && (d.deptName.includes('检验') || d.deptName.includes('采血')))
              .slice(0, 7)
              .map((d, idx) => ({
                windowId: 'W' + (idx + 1),
                windowName: d.deptName || '窗口' + (idx + 1),
                waitingCount: d.waitingCount || 0,
                statusLevel: d.statusLevel || 'NORMAL'
              }))
          })
          .catch(() => {})
          .finally(() => { this.heatmapLoading = false })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.director-desk { padding: 0; min-height: 100%; }
.desk-tabs {
  background: #fff;
  border-radius: 8px;
  padding: 0 20px 20px 20px;
  min-height: calc(100vh - 84px);
}
::v-deep .desk-tabs > .el-tabs__header .el-tabs__nav-wrap::after { height: 1px; background-color: #F1F3F4; }
::v-deep .desk-tabs > .el-tabs__header .el-tabs__item {
  font-size: 16px; font-weight: 600; color: #5F6368; padding: 0 24px; height: 50px; line-height: 50px;
  &.is-active { color: #1A73E8; }
}
::v-deep .desk-tabs > .el-tabs__header .el-tabs__active-bar { height: 3px; border-radius: 3px; background-color: #1A73E8; }

.desk-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;
  .desk-title { display: flex; align-items: center; gap: 14px;
    i { font-size: 36px; color: #0D47A1; }
    h2 { margin: 0; font-size: 22px; font-weight: 700; color: #202124; }
    .desk-subtitle { font-size: 13px; color: #80868B; margin-top: 4px; display: block; }
  }
  .header-actions { display: flex; gap: 10px; }
}
.kpi-row {
  display: flex; gap: 20px; margin-bottom: 24px;
  .kpi-card {
    flex: 1; background: #FFF; border-radius: 12px; padding: 20px; border: 1px solid #F1F3F4;
    display: flex; align-items: flex-start; gap: 14px; box-shadow: 0 1px 4px rgba(0,0,0,0.02); transition: all 0.2s;
    &:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
  }
  .kpi-icon-wrapper { width: 48px; height: 48px; border-radius: 8px; display: flex; justify-content: center; align-items: center; font-size: 20px; flex-shrink: 0; }
  .kpi-info { flex: 1; }
  .kpi-title { font-size: 13px; color: #5F6368; margin-bottom: 6px; }
  .kpi-value { font-size: 28px; font-weight: 700; line-height: 1; margin-bottom: 8px; }
  .kpi-unit { font-size: 13px; color: #80868B; font-weight: 400; }
}
.main-grid { display: flex; gap: 20px; align-items: stretch; }
.col-left { flex: 7; display: flex; flex-direction: column; }
.col-right { flex: 5; display: flex; flex-direction: column; }
.section-card { background: #FFF; border-radius: 12px; border: 1px solid #F1F3F4; padding: 20px; }
.section-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; padding-bottom: 14px; border-bottom: 1px solid #F1F3F4;
  .section-title { font-size: 15px; font-weight: 600; color: #202124; display: flex; align-items: center; gap: 6px; }
}
/* ── 热力图 ── */
.heatmap-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.heat-cell {
  border: 1.5px solid; border-radius: 12px; padding: 12px; text-align: center; cursor: default; transition: transform 0.2s;
  &:hover { transform: scale(1.04); }
}
.heat-dept-name { font-size: 12px; font-weight: 600; margin-bottom: 4px; }
.heat-count { font-size: 26px; font-weight: 700; line-height: 1; }
.heat-label { font-size: 11px; opacity: 0.75; }
.legend-dot { display: inline-block; width: 12px; height: 12px; border-radius: 3px; margin-right: 2px; vertical-align: middle; }

/* ── 算路策略 ── */
.strategy-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.strategy-title { font-size: 15px; font-weight: 600; color: #202124; }
.strategy-current-label { font-size: 13px; color: #5F6368; }
.strategy-cards.vertical-layout { display: flex; flex-direction: column; gap: 10px; }
.strategy-card {
  display: flex; align-items: center; gap: 12px; padding: 12px 14px; border-radius: 8px; border: 2px solid #F1F3F4;
  cursor: pointer; transition: all 0.2s; position: relative;
  &:hover { border-color: #AECBFA; background: #F0F5FF; }
  &.active { border-color: #1A73E8; background: linear-gradient(135deg, #E8F0FE, #F0F5FF); }
}
.sc-icon { width: 36px; height: 36px; border-radius: 8px; flex-shrink: 0; display: flex; justify-content: center; align-items: center; i { font-size: 16px; color: #FFF; } }
.sc-body { flex: 1; min-width: 0; }
.sc-name { font-size: 14px; font-weight: 600; color: #202124; margin-bottom: 2px; }
.sc-desc { font-size: 12px; color: #80868B; line-height: 1.3; }
.sc-check { position: absolute; right: 14px; color: #1A73E8; font-size: 16px; font-weight: 700; }

/* ── 危机预警墙 ── */
.urgent-list { margin-top: 16px; display: flex; flex-direction: column; gap: 10px; }
.urgent-item {
  display: flex; align-items: center; gap: 12px; padding: 12px; border-radius: 8px; background: #FCE8E6; border: 1px solid rgba(234,67,53,0.3);
  .ug-tag { width: 30px; height: 30px; background: #EA4335; color: #FFF; font-weight: 700; display: flex; align-items: center; justify-content: center; border-radius: 6px; font-size: 14px; }
  .ug-body { flex: 1; }
  .ug-title { font-size: 14px; font-weight: 600; color: #B31412; margin-bottom: 4px; }
  .ug-time { font-size: 12px; color: #C5221F; }
}

@media (max-width: 1400px) {
  .heatmap-grid { grid-template-columns: repeat(3, 1fr); }
  .main-grid { flex-direction: column; }
}
</style>
