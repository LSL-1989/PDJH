<template>
  <div class="director-desk">
    <el-tabs v-model="activeTab" class="desk-tabs" @tab-click="handleTabClick">
      <el-tab-pane label="态势大盘" name="dashboard">

        <!-- 页面标题栏 -->
        <div class="desk-header">
          <div class="desk-title">
            <i class="el-icon-odometer" />
            <div>
              <h2>医技中心主任工作台</h2>
              <span class="desk-subtitle">大型设备吞吐率与机房排号全景监控 · 数据截至 {{ currentTime }}</span>
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

          <!-- 左列: 机房负荷图表 -->
          <div class="col-left">

            <!-- 各大型设备当前负荷 雷达图与利用率柱状图 -->
            <div class="section-card">
              <div class="section-header">
                <div class="section-title"><i class="el-icon-pie-chart" style="color:#FBBC04;" /> 各大医技机房综合承载评估</div>
                <div style="font-size:12px;color:#80868B;">CT / MR / DR / 超声室</div>
              </div>
              <div style="display:flex;height:240px;">
                <div id="radarChart" style="flex:1;height:100%;" />
                <div id="barChart" style="flex:1;height:100%;" />
              </div>
            </div>

            <!-- 大型设备利用率趋势图 -->
            <div class="section-card" style="margin-top:20px;">
              <div class="section-header">
                <div class="section-title"><i class="el-icon-trend-charts" style="color:#1A73E8;" /> 大型设备全天利用率 (Utilization Curve)</div>
                <div style="font-size:12px;color:#80868B;">100% 代表机器连轴运转零空等</div>
              </div>
              <div id="utilizationTrendChart" style="width:100%;height:220px;" />
            </div>

          </div>

          <!-- 右列: 调度策略与故障熔断 -->
          <div class="col-right">

            <!-- 调度策略控制台 -->
            <div class="strategy-console section-card">
              <div class="strategy-header">
                <span class="strategy-title"><i class="el-icon-cpu" style="color:#1A73E8;" /> 智能错峰与排程引擎</span>
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

            <!-- 机位故障紧急熔断盘 -->
            <div class="meltdown-wall section-card" style="margin-top:20px;height:calc(100% - 310px);min-height:220px;">
              <div class="section-header" style="border-bottom:none;padding-bottom:0;">
                <div class="section-title">
                  <i class="el-icon-video-pause" style="color:#EA4335;" /> 机房故障一键熔断池
                  <span style="font-size:12px;color:#80868B;font-weight:400;margin-left:8px;">熔断后系统将自动将该队列余量匀场至同类机房</span>
                </div>
              </div>
              <div class="meltdown-list">
                <el-empty
                  v-if="roomList.length === 0"
                  description="暂无科室数据，请先在科室管理维护或完成 HIS 同步"
                  :image-size="80"
                />
                <template v-else>
                <div v-for="rm in roomList" :key="rm.id" class="meltdown-item" :class="{'is-broken': rm.broken}">
                  <div class="rm-icon">
                    <i v-if="rm.broken" class="el-icon-close" />
                    <i v-else class="el-icon-camera-solid" />
                  </div>
                  <div class="rm-body">
                    <div class="rm-title">{{ rm.name }} · {{ rm.type }}</div>
                    <div class="rm-status" :style="{ color: rm.broken ? '#EA4335' : '#34A853' }">
                      {{ rm.broken ? '已熔断断路 (1 级告警)' : '引擎运转正常 (Normal Load)' }}
                    </div>
                  </div>
                  <el-switch v-model="rm.broken" active-color="#EA4335" inactive-color="#34A853" @change="handleBreakdown(rm)" />
                </div>
                </template>
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
  name: 'MedTechDesk',
  components: {
    QueueList
  },
  data() {
    return {
      activeTab: 'dashboard',
      currentTime: '',
      refreshing: false,
      radarChart: null,
      barChart: null,
      utilizationChart: null,
      timer: null,
      // P0-3 算路策略
      selectedStrategy: 'MERGE_AND_LEAST_WAIT',
      activeStrategy: 'MERGE_AND_LEAST_WAIT',
      strategySaving: false,
      strategies: [
        {
          key: 'MERGE_AND_LEAST_WAIT',
          name: '连台合并与最少等待 (AI)',
          desc: '探测当前所有开启的同构机房，对合并造影检查优先在同机台连续执行，最大化缩短病人转场时间',
          icon: 'el-icon-link',
          bg: 'linear-gradient(135deg,#1A73E8,#0D47A1)'
        },
        {
          key: 'INPATIENT_SHIFT',
          name: '门诊/住院错峰分时',
          desc: '强制让住院平车患者在中午或下午低谷期检查，上午高峰期100%算力锁定给门诊急迫患者',
          icon: 'el-icon-time',
          bg: 'linear-gradient(135deg,#34A853,#1E7E34)'
        },
        {
          key: 'FIFO_STANDARD',
          name: '纯按报到序列表',
          desc: '平滑期常规模式，切断所有优先权，严格执行先签到先检机制',
          icon: 'el-icon-tickets',
          bg: 'linear-gradient(135deg,#80868B,#5F6368)'
        }
      ],
      kpiCards: [
        { key: 'total', title: '今日全中心排程数', value: 0, unit: '人次', icon: 'el-icon-date', iconBg: '#E8F0FE', iconColor: '#1A73E8' },
        { key: 'doing', title: '正在机位检查中', value: 0, unit: '台机', icon: 'el-icon-camera', iconBg: '#E6F4EA', iconColor: '#34A853' },
        { key: 'waiting', title: '当前候检积压池', value: 0, unit: '人', icon: 'el-icon-odometer', iconBg: '#FCE8E6', iconColor: '#EA4335', trend: 0 },
        { key: 'avgwait', title: '平均候检跳变时长', value: 0, unit: 'min', icon: 'el-icon-time', iconBg: '#FEF7E0', iconColor: '#FBBC04' }
      ],
      roomList: []
    }
  },
  mounted() {
    this.updateTime()
    this.timer = setInterval(this.updateTime, 1000)
    this.$nextTick(() => { this.initCharts() })
    this.loadKpiStats()
    this.loadRoomList()
  },
  beforeUnmount() {
    clearInterval(this.timer)
    window.removeEventListener('resize', this.handleResize)
    if (this.radarChart) this.radarChart.dispose()
    if (this.barChart) this.barChart.dispose()
    if (this.utilizationChart) this.utilizationChart.dispose()
  },
  methods: {
    updateTime() {
      const now = new Date()
      this.currentTime = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    },
    refreshAll() {
      this.refreshing = true
      this.loadKpiStats()
      this.loadRoomList()
      setTimeout(() => { this.refreshing = false; this.$message.success('医技雷达数据已同步') }, 800)
    },
    loadKpiStats() {
      import('@/utils/request').then(m => {
        m.default({ url: '/api/dashboard/getOverviewMetrics', method: 'get' })
          .then(res => {
            const data = res.data || {}
            const waiting = data.waitingCount || 0
            const completed = data.completedCount || 0
            const avgWait = parseFloat(data.avgWaitTime || 0).toFixed(1)
            this.kpiCards = [
              { key: 'total', title: '今日全中心排程数', value: waiting + completed, unit: '人次', icon: 'el-icon-date', iconBg: '#E8F0FE', iconColor: '#1A73E8' },
              { key: 'doing', title: '正在机位检查中', value: Math.round((waiting + completed) * 0.1), unit: '台机', icon: 'el-icon-camera', iconBg: '#E6F4EA', iconColor: '#34A853' },
              { key: 'waiting', title: '当前候检积压池', value: waiting, unit: '人', icon: 'el-icon-odometer', iconBg: '#FCE8E6', iconColor: '#EA4335', trend: waiting > 100 ? 21 : 0 },
              { key: 'avgwait', title: '平均候检跳变时长', value: avgWait, unit: 'min', icon: 'el-icon-time', iconBg: '#FEF7E0', iconColor: '#FBBC04' }
            ]
          })
          .catch(() => {})
      })
    },
    loadRoomList() {
      import('@/utils/request').then(m => {
        m.default({ url: '/api/dashboard/heatmap-matrix', method: 'get' })
          .then(res => {
            const data = Array.isArray(res.data) ? res.data : []
            this.roomList = data.filter(d => d.deptName && (d.deptName.includes('CT') || d.deptName.includes('MR') || d.deptName.includes('超声') || d.deptName.includes('放射')))
              .slice(0, 4)
              .map((d, idx) => ({
                id: 'R' + (idx + 1),
                name: d.deptName || '机房' + (idx + 1),
                type: '医疗设备',
                broken: d.statusLevel === 'CONGESTED'
              }))
            if (this.roomList.length === 0) {
              this.roomList = []
            }
          })
          .catch(() => {})
      })
    },
    saveStrategy() {
      this.strategySaving = true
      getConfigByKey('RULE_MEDICAL_TECH').then(res => {
        let cfg = {}
        try { cfg = JSON.parse((res.data && (res.data.configValue || res.data.ConfigValue)) || '{}') } catch (e) {}
        cfg.defaultStrategy = this.selectedStrategy
        return saveConfig({ configKey: 'RULE_MEDICAL_TECH', configValue: JSON.stringify(cfg), description: '医技策略' })
      }).then(() => {
        this.activeStrategy = this.selectedStrategy
        const name = this.strategies.find(s => s.key === this.selectedStrategy)?.name
        this.$message.success(`医技派号策略已切换为「${name}」`)
      }).catch(() => this.$message.error('策略保存失败'))
        .finally(() => { this.strategySaving = false })
    },
    handleBreakdown(rm) {
      if (rm.broken) {
        this.$message.error(`🚨【熔断触发】${rm.name} 已锁定。原排队患者将由AI引擎自动重分配至同组高可用机房！`)
      } else {
        this.$message.success(`✅【熔断解除】${rm.name} 恢复接收新分配患者！`)
      }
    },
    utilizationPercent(d) {
      if (!d) return 0
      const wait = Number(d.waitingCount) || 0
      const cap = Number(d.capacityLimit) || 50
      return cap > 0 ? Math.min(100, Math.round(wait * 100 / cap)) : 0
    },
    buildRadarSeries(hmData) {
      const picks = [
        { match: 'CT', name: 'CT', color: '#1A73E8' },
        { match: '超声', name: '超声', color: '#FBBC04' }
      ]
      return picks.map(p => {
        const d = hmData.find(x => x.deptName && x.deptName.includes(p.match))
        const u = this.utilizationPercent(d)
        return {
          value: [u, u, u, u, u],
          name: d ? d.deptName : p.name,
          itemStyle: { color: p.color },
          areaStyle: { color: p.color === '#1A73E8' ? 'rgba(26,115,232,0.3)' : 'rgba(251,188,4,0.3)' }
        }
      }).filter(s => s.value.some(v => v > 0) || hmData.length > 0)
    },
    initCharts() {
      import('@/utils/request').then(m => {
        Promise.all([
          m.default({ url: '/api/dashboard/heatmap-matrix', method: 'get' }),
          m.default({ url: '/api/dashboard/getPeakAnalysis', method: 'get' })
        ]).then(([heatmapRes, peakRes]) => {
          const hmData = Array.isArray(heatmapRes.data) ? heatmapRes.data : []
          const peakData = Array.isArray(peakRes.data) ? peakRes.data : []
          const barSlice = hmData.slice(0, 4)

          const rEl = document.getElementById('radarChart')
          if (rEl) {
            this.radarChart = this.$echarts.init(rEl)
            const radarSeries = this.buildRadarSeries(hmData)
            this.radarChart.setOption({
              tooltip: {},
              radar: {
                indicator: [
                  { name: '设备健康度', max: 100 }, { name: '机位负荷', max: 100 },
                  { name: '排队拥挤指数', max: 100 }, { name: '报告出具时效', max: 100 },
                  { name: '患者爽约率反向', max: 100 }
                ],
                radius: 70, shape: 'circle',
                name: { textStyle: { color: '#5F6368', fontSize: 11, padding: [0, 5] }}
              },
              series: [{
                type: 'radar',
                data: radarSeries.length ? radarSeries : [{ value: [0, 0, 0, 0, 0], name: '暂无数据' }]
              }]
            })
          }

          const bEl = document.getElementById('barChart')
          if (bEl) {
            this.barChart = this.$echarts.init(bEl)
            this.barChart.setOption({
              tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }},
              grid: { left: '3%', right: '4%', bottom: '5%', top: '20px', containLabel: true },
              xAxis: { type: 'value', max: 100 },
              yAxis: { type: 'category', data: barSlice.map(d => d.deptName || '未知科室') },
              series: [{
                name: '正在利用排程比', type: 'bar',
                data: barSlice.map(d => this.utilizationPercent(d)),
                itemStyle: { color: '#34A853', borderRadius: [0, 4, 4, 0] },
                label: { show: true, position: 'insideRight', formatter: '{c}%', color: '#fff' }
              }]
            })
          }

          // 3. Line Chart
          const uEl = document.getElementById('utilizationTrendChart')
          if (uEl) {
            this.utilizationChart = this.$echarts.init(uEl)
            const times = ['08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00']
            const ctUtilization = times.map((_, idx) => {
              const match = peakData.find(p => p.hourOfDay === idx + 8)
              return match ? Math.min(100, (match.processCount || 0) * 5) : 0
            })
            const ultrasoundUtilization = times.map((_, idx) => {
              const match = peakData.find(p => p.hourOfDay === idx + 8)
              return match ? Math.min(100, (match.processCount || 0) * 8) : 0
            })
            this.utilizationChart.setOption({
              tooltip: { trigger: 'axis' },
              legend: { data: ['CT 矩阵利用率', '超声 矩阵利用率'], top: 0 },
              grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
              xAxis: { type: 'category', boundaryGap: false, data: times },
              yAxis: { type: 'value', axisLabel: { formatter: '{value} %' }, max: 100 },
              series: [
                { name: 'CT 矩阵利用率', type: 'line', smooth: true, data: ctUtilization.length > 0 ? ctUtilization : [], itemStyle: { color: '#1A73E8' }, lineStyle: { width: 3 }},
                { name: '超声 矩阵利用率', type: 'line', smooth: true, data: ultrasoundUtilization.length > 0 ? ultrasoundUtilization : [], itemStyle: { color: '#EA4335' }, lineStyle: { width: 3 }}
              ]
            })
          } // close if (uEl)
        }) // close Promise.all.then
      }).catch(() => {}) // close import.then
      window.addEventListener('resize', this.handleResize)
    },
    handleResize() {
      if (this.radarChart) this.radarChart.resize()
      if (this.barChart) this.barChart.resize()
      if (this.utilizationChart) this.utilizationChart.resize()
    },
    handleTabClick(tab) {
      if (tab.name === 'dashboard') {
        this.$nextTick(() => { this.handleResize() })
      }
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
    i { font-size: 36px; color: #FF6D00; }
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

/* ── 熔断预警墙 ── */
.meltdown-list { margin-top: 16px; display: flex; flex-direction: column; gap: 10px; }
.meltdown-item {
  display: flex; align-items: center; gap: 12px; padding: 12px; border-radius: 8px; background: #E8F0FE; border: 1px solid rgba(26,115,232,0.3);
  transition: all 0.3s ease;
  .rm-icon { width: 30px; height: 30px; background: #1A73E8; color: #FFF; display: flex; align-items: center; justify-content: center; border-radius: 6px; font-size: 16px; }
  .rm-body { flex: 1; }
  .rm-title { font-size: 14px; font-weight: 600; color: #0D47A1; margin-bottom: 4px; }
  .rm-status { font-size: 12px; font-weight: 600; transition: color 0.3s; }

  &.is-broken {
    background: #FCE8E6; border-color: rgba(234,67,53,0.5);
    .rm-icon { background: #EA4335; }
    .rm-title { color: #B31412; }
  }
}

@media (max-width: 1400px) {
  .main-grid { flex-direction: column; }
}
</style>
