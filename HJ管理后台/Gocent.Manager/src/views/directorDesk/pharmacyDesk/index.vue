<template>
  <div class="director-desk">
    <el-tabs v-model="activeTab" class="desk-tabs" @tab-click="handleTabClick">
      <el-tab-pane label="态势大盘" name="dashboard">
        <!-- 页面标题栏 -->
        <div class="desk-header">
          <div class="desk-title">
            <i class="el-icon-medal-1" />
            <div>
              <h2>药房主任工作台</h2>
              <span class="desk-subtitle">全量处方发药进度监控 · 数据截至 {{ currentTime }}</span>
            </div>
          </div>
          <div class="header-actions">
            <el-button type="primary" plain size="small" icon="el-icon-setting" @click="goToRules">发药规则配置</el-button>
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
              <div v-if="kpi.sub" class="kpi-sub">{{ kpi.sub }}</div>
            </div>
          </div>
        </div>

        <!-- 主体区域 -->
        <div class="main-grid">

          <!-- 左列：窗口实时状态 -->
          <div class="col-left">
            <div class="section-card">
              <div class="section-header">
                <div class="section-title"><i class="el-icon-s-shop" style="color:#1A73E8;" /> 发药窗口实时状态</div>
                <el-tag type="success" size="small" effect="plain">{{ openWindowCount }}/{{ windowList.length }} 窗口开放</el-tag>
              </div>
              <div class="window-list">
                <el-empty
                  v-if="windowList.length === 0"
                  description="暂无科室数据，请先在科室管理维护或完成 HIS 同步"
                  :image-size="80"
                />
                <div v-for="win in windowList" :key="win.id" class="window-item" :class="{ 'window-closed': win.status === '关闭' }">
                  <div class="window-header">
                    <div class="window-badge" :class="win.status === '开放' ? 'badge-open' : 'badge-closed'">
                      {{ win.id }}号窗
                    </div>
                    <div class="window-name">{{ win.name }}</div>
                    <el-tag :type="win.status === '开放' ? 'success' : 'info'" size="small" effect="dark">{{ win.status }}</el-tag>
                  </div>
                  <div v-if="win.status === '开放'" class="window-stats">
                    <div class="wstat-item">
                      <div class="wstat-value" :style="{ color: win.queue > 10 ? '#EA4335' : '#1A73E8' }">{{ win.queue }}</div>
                      <div class="wstat-label">待发药</div>
                    </div>
                    <div class="wstat-item">
                      <div class="wstat-value">{{ win.todayDone }}</div>
                      <div class="wstat-label">今日完成</div>
                    </div>
                    <div class="wstat-item">
                      <div class="wstat-value" :style="{ color: typeof win.avgTime === 'number' && win.avgTime > 8 ? '#FBBC04' : '#34A853' }">{{ typeof win.avgTime === 'number' ? win.avgTime + 'min' : win.avgTime }}</div>
                      <div class="wstat-label">均配药时长</div>
                    </div>
                    <div class="wstat-item">
                      <div class="wstat-value" style="font-size:13px;">{{ win.lastDone }}</div>
                      <div class="wstat-label">最近完成</div>
                    </div>
                  </div>
                  <div v-if="win.status === '开放'" class="window-progress">
                    <el-progress
                      :percentage="Math.min(100, Math.round(win.queue / 15 * 100))"
                      :color="win.queue > 10 ? '#EA4335' : '#1A73E8'"
                      :stroke-width="6"
                      :show-text="false"
                    />
                    <span style="font-size:11px;color:#80868B;margin-left:8px;">负荷 {{ Math.min(100, Math.round(win.queue / 15 * 100)) }}%</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 右列：图表和告警 -->
          <div class="col-right">

            <!-- 发药量趋势图 -->
            <div class="section-card">
              <div class="section-header">
                <div class="section-title"><i class="el-icon-data-line" style="color:#1A73E8;" /> 今日分时发药量趋势</div>
                <div style="display:flex;align-items:center;gap:8px;">
                  <el-tag type="info" size="small">今日: {{ todayTotal }}支</el-tag>
                  <el-date-picker v-model="selectedDate" type="date" size="small" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:140px;" :clearable="false" />
                </div>
              </div>
              <div id="pharmacyTrendChart" style="width:100%;height:220px;" />
            </div>

            <!-- 超时滞留处方告警 -->
            <div class="section-card" style="margin-top:20px;">
              <div class="section-header">
                <div class="section-title">
                  <i class="el-icon-alarm-clock" style="color:#EA4335;" /> 超时滞留处方告警
                  <el-tag type="danger" size="small" style="margin-left:8px;">{{ overtimeList.length }} 张超时</el-tag>
                </div>
                <el-button text size="small" @click="batchSendReminder">批量催促</el-button>
              </div>
              <el-table :data="overtimeList" size="small" :stripe="true" style="width:100%;" :row-class-name="tableRowClass">
                <el-table-column prop="patientName" label="患者" width="80" />
                <el-table-column prop="window" label="发药窗口" width="90">
                  <template #default="scope">
                    <el-tag type="info" size="small">{{ scope.row.window }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="prescriptionType" label="处方类型">
                  <template #default="scope">
                    <el-tag :type="scope.row.prescriptionType === '急诊' ? 'danger' : 'primary'" size="small" effect="plain">{{ scope.row.prescriptionType }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="waitMinutes" label="等候" width="75">
                  <template #default="scope">
                    <span :style="{ color: scope.row.waitMinutes > 30 ? '#EA4335' : '#FBBC04', fontWeight: 700 }">{{ scope.row.waitMinutes }}min</span>
                  </template>
                </el-table-column>
                <table-action-column :count="1">
                  <template #default="{ row }">
                    <table-action-icon icon="el-icon-bell" tip="催取药" type="warning" @click="sendReminder(row)" />
                  </template>
                </table-action-column>
              </el-table>
            </div>

            <!-- 药品库存快速预警 -->
            <div class="section-card" style="margin-top:20px;">
              <div class="section-header">
                <div class="section-title"><i class="el-icon-box" style="color:#FBBC04;" /> 高频药品库存预警</div>
                <el-tag type="warning" size="small">{{ lowStockList.filter(d=>d.level==='告急').length }} 种告急</el-tag>
              </div>
              <div class="stock-list">
                <div v-for="drug in lowStockList" :key="drug.name" class="stock-item">
                  <div class="drug-name">{{ drug.name }}</div>
                  <el-progress
                    :percentage="Math.round(drug.stock / drug.capacity * 100)"
                    :color="drug.level === '告急' ? '#EA4335' : '#FBBC04'"
                    :stroke-width="8"
                    style="flex:1;margin:0 12px;"
                  />
                  <div class="drug-info">
                    <span class="stock-num" :style="{ color: drug.level === '告急' ? '#EA4335' : '#B06000' }">{{ drug.stock }}</span>
                    <el-tag :type="drug.level === '告急' ? 'danger' : 'warning'" size="small" style="margin-left:6px;">{{ drug.level }}</el-tag>
                  </div>
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

export default {
  name: 'PharmacyDesk',
  components: {
    QueueList
  },
  data() {
    return {
      activeTab: 'dashboard',
      currentTime: '',
      refreshing: false,
      selectedDate: new Date().toISOString().split('T')[0],
      trendChart: null,
      todayTotal: 0,
      kpiCards: [
        { key: 'pending', title: '待发药处方', value: 0, unit: '张', icon: 'el-icon-document', iconBg: '#E8F0FE', iconColor: '#1A73E8', sub: '当前队列' },
        { key: 'done', title: '今日已完成', value: 0, unit: '张', icon: 'el-icon-circle-check', iconBg: '#E6F4EA', iconColor: '#34A853', sub: '' },
        { key: 'overtime', title: '超时滞留', value: 0, unit: '张', icon: 'el-icon-alarm-clock', iconBg: '#FCE8E6', iconColor: '#EA4335', valueColor: '#EA4335', sub: '> 20分钟' },
        { key: 'windows', title: '开放窗口', value: 0, unit: '个', icon: 'el-icon-s-shop', iconBg: '#FEF7E0', iconColor: '#B06000', sub: '' }
      ],
      windowList: [],
      overtimeList: [],
      lowStockList: []
    }
  },
  computed: {
    openWindowCount() { return this.windowList.filter(w => w.status === '开放').length }
  },
  mounted() {
    this.updateTime()
    this.timer = setInterval(this.updateTime, 1000)
    this.$nextTick(() => { this.initTrendChart() })
    this.loadKpiStats()
    this.loadWindowList()
    this.loadOvertimeList()
  },
  beforeUnmount() {
    clearInterval(this.timer)
    if (this.trendChart) { this.trendChart.dispose(); this.trendChart = null }
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
      this.loadWindowList()
      this.loadOvertimeList()
      setTimeout(() => { this.refreshing = false; this.$message.success('数据已刷新') }, 800)
    },
    loadKpiStats() {
      import('@/utils/request').then(m => {
        m.default({ url: '/api/dashboard/getOverviewMetrics', method: 'get' })
          .then(res => {
            const data = res.data || {}
            const waiting = data.waitingCount || 0
            const completed = data.completedCount || 0
            this.kpiCards = [
              { key: 'pending', title: '待发药处方', value: waiting, unit: '张', icon: 'el-icon-document', iconBg: '#E8F0FE', iconColor: '#1A73E8', sub: '当前队列' },
              { key: 'done', title: '今日已完成', value: completed, unit: '张', icon: 'el-icon-circle-check', iconBg: '#E6F4EA', iconColor: '#34A853', sub: '' },
              { key: 'overtime', title: '超时滞留', value: 0, unit: '张', icon: 'el-icon-alarm-clock', iconBg: '#FCE8E6', iconColor: '#EA4335', valueColor: '#EA4335', sub: '> 20分钟' },
              { key: 'windows', title: '开放窗口', value: this.windowList.filter(w => w.status === '开放').length, unit: '个', icon: 'el-icon-s-shop', iconBg: '#FEF7E0', iconColor: '#B06000', sub: '' }
            ]
            this.todayTotal = completed
          })
          .catch(() => {})
      })
    },
    loadWindowList() {
      import('@/utils/request').then(m => {
        m.default({ url: '/api/dashboard/heatmap-matrix', method: 'get' })
          .then(res => {
            const data = Array.isArray(res.data) ? res.data : []
            this.windowList = data.filter(d => d.deptName && (d.deptName.includes('药') || d.deptName.includes('房')))
              .slice(0, 6)
              .map((d, idx) => ({
                id: idx + 1,
                name: d.deptName || '窗口' + (idx + 1),
                status: '开放',
                queue: d.waitingCount || 0,
                todayDone: d.completedCount || 0,
                avgTime: d.avgTime != null ? d.avgTime : '—',
                lastDone: d.lastDone || '—'
              }))
          })
          .catch(() => {})
      })
    },
    loadOvertimeList() {
      import('@/utils/request').then(m => {
        m.default({ url: '/api/dashboard/getOvertimeAlerts', method: 'get', params: { thresholdMinutes: 20 } })
          .then(res => {
            const data = Array.isArray(res.data) ? res.data : []
            this.overtimeList = data.slice(0, 6).map(d => ({
              patientName: d.patientName || '未知',
              window: d.deptName || d.deptId || '药房',
              prescriptionType: d.patientType === 'emergency' ? '急诊' : '普通',
              waitMinutes: d.waitMinutes || 0
            }))
          })
          .catch(() => {})
      })
    },
    goToRules() { this.$router.push('/patientCenter/pharmacyCall') },
    sendReminder(row) { this.$message.success(`已向 ${row.patientName} 发送取药催促提醒`) },
    batchSendReminder() { this.$message.success(`已批量向 ${this.overtimeList.length} 位患者发送催促提醒`) },
    tableRowClass({ row }) { return row.prescriptionType === '急诊' ? 'urgent-row' : '' },
    initTrendChart() {
      const el = document.getElementById('pharmacyTrendChart')
      if (!el) return
      this.trendChart = this.$echarts.init(el)
      // 从 API 获取峰值分析数据
      import('@/utils/request').then(m => {
        m.default({ url: '/api/dashboard/getPeakAnalysis', method: 'get' })
          .then(res => {
            const peakData = Array.isArray(res.data) ? res.data : []
            const hours = ['08:00', '09:00', '10:00', '11:00', '12:00', '13:00']
            const westData = hours.map((_, idx) => {
              const match = peakData.find(p => p.hourOfDay === idx + 8)
              return match ? Math.round(match.processCount * 0.4) : 0
            })
            const chineseData = hours.map((_, idx) => {
              const match = peakData.find(p => p.hourOfDay === idx + 8)
              return match ? Math.round(match.processCount * 0.3) : 0
            })
            const emergencyData = hours.map((_, idx) => {
              const match = peakData.find(p => p.hourOfDay === idx + 8)
              return match ? Math.round(match.processCount * 0.3) : 0
            })
            this.trendChart.setOption({
              tooltip: { trigger: 'axis' },
              legend: { data: ['西药房', '中药房', '急诊药房'], top: 0, itemWidth: 12, itemHeight: 12 },
              grid: { left: '2%', right: '2%', bottom: '3%', top: '40px', containLabel: true },
              xAxis: { type: 'category', data: hours, axisLabel: { color: '#5F6368', fontSize: 11 }},
              yAxis: { type: 'value', splitLine: { lineStyle: { color: '#F1F3F4', type: 'dashed' }}, axisLabel: { color: '#5F6368', fontSize: 11 }},
              series: [
                { name: '西药房', type: 'bar', stack: 'total', data: westData, itemStyle: { color: '#1A73E8', borderRadius: [0, 0, 0, 0] }},
                { name: '中药房', type: 'bar', stack: 'total', data: chineseData, itemStyle: { color: '#34A853' }},
                { name: '急诊药房', type: 'bar', stack: 'total', data: emergencyData, itemStyle: { color: '#FBBC04', borderRadius: [3, 3, 0, 0] }}
              ]
            })
          })
          .catch(() => {
            this.trendChart.setOption({
              tooltip: { trigger: 'axis' },
              legend: { data: ['西药房', '中药房', '急诊药房'], top: 0, itemWidth: 12, itemHeight: 12 },
              grid: { left: '2%', right: '2%', bottom: '3%', top: '40px', containLabel: true },
              xAxis: { type: 'category', data: ['08:00', '09:00', '10:00', '11:00', '12:00', '13:00'], axisLabel: { color: '#5F6368', fontSize: 11 }},
              yAxis: { type: 'value', splitLine: { lineStyle: { color: '#F1F3F4', type: 'dashed' }}, axisLabel: { color: '#5F6368', fontSize: 11 }},
              series: [
                { name: '西药房', type: 'bar', stack: 'total', data: [], itemStyle: { color: '#1A73E8' }},
                { name: '中药房', type: 'bar', stack: 'total', data: [], itemStyle: { color: '#34A853' }},
                { name: '急诊药房', type: 'bar', stack: 'total', data: [], itemStyle: { color: '#FBBC04' }}
              ]
            })
          })
      })
      window.addEventListener('resize', this.handleResize)
    },
    handleResize() {
      if (this.trendChart) {
        this.trendChart.resize()
      }
    },
    handleTabClick(tab) {
      if (tab.name === 'dashboard') {
        this.$nextTick(() => {
          this.handleResize()
        })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.director-desk {
  padding: 0;
  height: 100%;
}
.desk-tabs {
  background: #fff;
  border-radius: 8px;
  padding: 0 20px 20px 20px;
  min-height: calc(100vh - 84px);
}
::v-deep .el-tabs__nav-wrap::after {
  height: 1px;
  background-color: #F1F3F4;
}
::v-deep .el-tabs__item {
  font-size: 16px;
  font-weight: 600;
  color: #5F6368;
  padding: 0 24px;
  height: 50px;
  line-height: 50px;
  &.is-active {
    color: #1A73E8;
  }
}
::v-deep .el-tabs__active-bar {
  height: 3px;
  border-radius: 3px;
  background-color: #1A73E8;
}
.desk-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;
  .desk-title { display: flex; align-items: center; gap: 14px;
    i { font-size: 36px; color: '#EA4335'; }
    h2 { margin: 0; font-size: 22px; font-weight: 700; color: #202124; }
    .desk-subtitle { font-size: 13px; color: #80868B; margin-top: 4px; display: block; }
  }
  .header-actions { display: flex; gap: 10px; }
}
.kpi-row {
  display: flex; gap: 20px; margin-bottom: 24px;
  .kpi-card {
    flex: 1; background: #FFF; border-radius: 16px; padding: 20px;
    display: flex; align-items: flex-start; gap: 16px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
    &:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0,0,0,0.08); }
    transition: all 0.2s;
  }
  .kpi-icon-wrapper { width: 52px; height: 52px; border-radius: 50%; display: flex; justify-content: center; align-items: center; font-size: 22px; flex-shrink: 0; }
  .kpi-info { flex: 1; }
  .kpi-title { font-size: 13px; color: #5F6368; margin-bottom: 6px; }
  .kpi-value { font-size: 30px; font-weight: 700; line-height: 1; }
  .kpi-unit { font-size: 14px; color: #80868B; font-weight: 400; }
  .kpi-sub { font-size: 12px; color: #80868B; margin-top: 4px; }
}
.main-grid { display: flex; gap: 20px; }
.col-left { flex: 5; }
.col-right { flex: 6; display: flex; flex-direction: column; }
.section-card { background: #FFF; border-radius: 16px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.section-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; padding-bottom: 14px; border-bottom: 1px solid #F1F3F4;
  .section-title { font-size: 15px; font-weight: 600; color: #202124; i { margin-right: 6px; } display: flex; align-items: center; }
}
.window-list { display: flex; flex-direction: column; gap: 12px; }
.window-item {
  background: #F8F9FA; border-radius: 12px; padding: 14px;
  &.window-closed { opacity: 0.5; }
}
.window-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.window-badge {
  padding: 2px 10px; border-radius: 20px; font-size: 13px; font-weight: 700;
  &.badge-open { background: #E6F4EA; color: #34A853; }
  &.badge-closed { background: #F1F3F4; color: #80868B; }
}
.window-name { flex: 1; font-size: 14px; font-weight: 600; color: #202124; }
.window-stats { display: flex; justify-content: space-around; margin: 10px 0; }
.wstat-item { text-align: center; }
.wstat-value { font-size: 20px; font-weight: 700; color: #202124; }
.wstat-label { font-size: 11px; color: #80868B; margin-top: 2px; }
.window-progress { display: flex; align-items: center; margin-top: 8px; }
.stock-list { display: flex; flex-direction: column; gap: 12px; }
.stock-item { display: flex; align-items: center; gap: 8px; }
.drug-name { width: 130px; font-size: 13px; color: #202124; flex-shrink: 0; }
.drug-info { display: flex; align-items: center; flex-shrink: 0; }
.stock-num { font-size: 15px; font-weight: 700; }
::v-deep .urgent-row { background: #FFF5F5 !important; }
@media (max-width: 1200px) {
  .main-grid { flex-direction: column; }
  .kpi-row { display: grid; grid-template-columns: repeat(2,1fr); }
}
</style>
