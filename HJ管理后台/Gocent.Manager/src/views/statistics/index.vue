<template>
  <div class="app-container">

    <!-- 页面标题 -->
    <div class="stats-header">
      <div>
        <h2 style="margin:0;font-size:20px;font-weight:700;color:#202124;">排队数据统计分析</h2>
        <p style="margin:4px 0 0;font-size:13px;color:#80868B;">多维度分析门诊运营效率，支持按日期和科室筛选</p>
      </div>
      <div style="display:flex;gap:10px;">
        <el-date-picker
          v-model="QueryForm.Date"
          align="right"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          size="small"
          :clearable="true"
          :disabled-date="(time) => time.getTime() > Date.now()"
          style="width:150px;"
          @change="DateChange"
        />
        <el-button type="primary" icon="el-icon-search" size="small" @click="refreshAll">查询数据</el-button>
      </div>
    </div>

    <!-- 全院KPI摘要 -->
    <div class="kpi-row">
      <div v-for="kpi in summaryKpis" :key="kpi.key" class="kpi-card">
        <div class="kpi-icon" :style="{ background: kpi.bg, color: kpi.color }"><i :class="kpi.icon" /></div>
        <div class="kpi-body">
          <div class="kpi-label">{{ kpi.label }}</div>
          <div class="kpi-val" :style="{ color: kpi.color }">{{ kpi.value }}</div>
          <div class="kpi-unit">{{ kpi.unit }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-grid">

      <!-- 科室接诊量横向对比 -->
      <div class="chart-card chart-wide">
        <div class="chart-header">
          <div class="chart-title"><i class="el-icon-bar-chart" style="color:#1A73E8;" /> 各科室接诊量与过号率对比</div>
          <el-button-group>
            <el-button size="small" :type="deptChartType==='bar'?'primary':''" @click="deptChartType='bar'">柱状图</el-button>
            <el-button size="small" :type="deptChartType==='line'?'primary':''" @click="deptChartType='line'">折线图</el-button>
          </el-button-group>
        </div>
        <div id="deptCompareChart" style="width:100%;height:280px;" />
      </div>

      <!-- 分时趋势 -->
      <div class="chart-card">
        <div class="chart-header">
          <div class="chart-title"><i class="el-icon-data-line" style="color:#34A853;" /> 分时段排队趋势</div>
        </div>
        <div id="hourlyTrendChart" style="width:100%;height:240px;" />
      </div>

      <!-- 平均等候时长对比（雷达图） -->
      <div class="chart-card">
        <div class="chart-header">
          <div class="chart-title"><i class="el-icon-coordinate" style="color:#9C27B0;" /> 各科室SLA质量雷达图</div>
        </div>
        <div id="slaRadarChart" style="width:100%;height:240px;" />
      </div>

    </div>

    <!-- 医生工作量表 -->
    <div class="table-section">
      <div class="chart-card">
        <div class="chart-header">
          <div class="chart-title"><i class="el-icon-s-cooperation" style="color:#1A73E8;" /> 医生接诊工作量排行</div>
          <div style="display:flex;gap:10px;align-items:center;">
            <el-input v-model="QueryForm.Name" size="small" placeholder="搜索医生姓名" prefix-icon="el-icon-search" style="width:180px;" clearable />
            <el-button type="primary" icon="el-icon-search" size="small" circle @click="getDoctorCallCount" />
          </div>
        </div>
        <el-table v-loading="tableLoading" :data="DoctorCallCountData" style="width: 100%" size="small" :stripe="true">
          <el-table-column type="index" label="排名" width="60" align="center">
            <template #default="scope">
              <div class="rank-badge" :class="scope.$index < 3 ? 'rank-top-' + (scope.$index + 1) : ''">{{ scope.$index + 1 }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="UserNo" label="工号" width="120">
            <template #default="scope">
              <span style="font-family:monospace;color:#5F6368;">{{ scope.row.UserNo }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="Name" label="医生姓名" width="120">
            <template #default="scope">
              <span style="font-weight:600;color:#202124;">{{ scope.row.Name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="Count" label="累计接诊人数" align="center">
            <template #default="scope">
              <span style="font-size:18px;font-weight:700;color:#1A73E8;">{{ scope.row.Count }}</span>
            </template>
          </el-table-column>
          <el-table-column label="完成率" align="center" width="180">
            <template #default="scope">
              <el-progress :percentage="Math.min(100, Math.round(scope.row.Count / 150 * 100))" :stroke-width="8" :color="'#1A73E8'" />
            </template>
          </el-table-column>
          <el-table-column label="平均等候" align="center" width="130">
            <template #default="scope">
              <el-tag size="small">{{ scope.row.avgWaitMin != null ? scope.row.avgWaitMin + 'min' : '-' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          background
          :current-page="PageIndex"
          :page-sizes="[10, 20, 30, 50]"
          :page-size="PageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="TotalCount"
          style="margin-top:16px;text-align:right;"
          @size-change="SizeChange"
          @current-change="PageChange"
        />
      </div>
    </div>

  </div>
</template>

<script>
import request from '@/utils/request'
import { getConfigByKey, saveConfig } from '@/api/systemConfig'
export default {
  name: 'Statistics',
  data() {
    return {
      PageIndex: 1, PageSize: 10, TotalCount: 0,
      tableLoading: false,
      deptChartType: 'bar',
      deptChart: null, hourlyChart: null, slaChart: null,
      DoctorCallCountData: [],
      QueryForm: { Name: '', Date: '' },
      pickerOptions: { disabledDate(time) { return time.getTime() > Date.now() } },
      summaryKpis: [
        { key: 'total', label: '今日挂号总量', value: '-', unit: '人次', icon: 'el-icon-user', bg: '#E8F0FE', color: '#1A73E8' },
        { key: 'avg_wait', label: '平均等候时长', value: '-', unit: '分钟', icon: 'el-icon-time', bg: '#E6F4EA', color: '#34A853' },
        { key: 'skip', label: '全院过号率', value: '-', unit: '%', icon: 'el-icon-warning-outline', bg: '#FEF7E0', color: '#B06000' },
        { key: 'done', label: '今日完成率', value: '-', unit: '%', icon: 'el-icon-circle-check', bg: '#F3E8FF', color: '#7B1FA2' }
      ],
      deptRadarData: [],
      hourlyData: []
    }
  },
  watch: {
    deptChartType() { this.initDeptCompareChart() }
  },
  mounted() {
    this.loadSummary()
    this.loadChartData()
    this.getDoctorCallCount()
    this.$nextTick(() => {
      this.initDeptCompareChart()
      this.initHourlyChart()
      this.initSlaChart()
      window.addEventListener('resize', () => {
        [this.deptChart, this.hourlyChart, this.slaChart].forEach(c => c && c.resize())
      })
    })
  },
  beforeUnmount() {
    [this.deptChart, this.hourlyChart, this.slaChart].forEach(c => { if (c) { c.dispose() } })
    window.removeEventListener('resize', () => {})
  },
  methods: {
    refreshAll() {
      this.loadSummary()
      this.loadChartData()
      this.getDoctorCallCount()
    },
    loadSummary() {
      const date = this.QueryForm.Date || ''
      request({ url: '/api/dashboard/statistics/summary', method: 'get', params: { date }})
        .then(res => {
          const s = res.data || {}
          this.summaryKpis[0].value = String(s.totalPatients != null ? s.totalPatients : 0)
          this.summaryKpis[1].value = Number(s.avgWaitMin || 0).toFixed(1)
          this.summaryKpis[2].value = Number(s.skipRate || 0).toFixed(1)
          this.summaryKpis[3].value = Number(s.completionRate || 0).toFixed(1)
        }).catch(() => {})
    },
    loadChartData() {
      const date = this.QueryForm.Date || ''
      request({ url: '/api/dashboard/statistics/hourly', method: 'get', params: { date }})
        .then(res => { this.hourlyData = res.data || []; this.initHourlyChart() }).catch(() => {})
      request({ url: '/api/dashboard/statistics/dept-radar', method: 'get', params: { date }})
        .then(res => { this.deptRadarData = res.data || []; this.initDeptCompareChart(); this.initSlaChart() }).catch(() => {})
    },
    DateChange(date) { this.refreshAll() },
    SizeChange(size) { this.PageSize = size; this.getDoctorCallCount() },
    PageChange(index) { this.PageIndex = index; this.getDoctorCallCount() },
    getDoctorCallCount() {
      this.tableLoading = true
      const input = { PageIndex: this.PageIndex, PageSize: this.PageSize, Name: this.QueryForm.Name, Date: this.QueryForm.Date }
      this.$store.dispatch('dashboard/getDoctorCallCount', input).then(response => {
        this.DoctorCallCountData = response.data
        this.TotalCount = response.pageInfo.totalCount
        this.tableLoading = false
      }).catch(() => { this.tableLoading = false })
    },
    initDeptCompareChart() {
      const el = document.getElementById('deptCompareChart')
      if (!el) return
      if (!this.deptChart) { this.deptChart = this.$echarts.init(el) }
      const rows = this.deptRadarData.length ? this.deptRadarData : []
      const depts = rows.map(r => r.deptName)
      const callCounts = rows.map(r => r.callCount || 0)
      const skipRates = rows.map(r => Number(r.skipRate || 0).toFixed(1))
      const option = {
        tooltip: { trigger: 'axis', axisPointer: { type: 'cross' }},
        legend: { data: ['接诊量（人次）', '过号率（%）'], top: 0 },
        grid: { left: '3%', right: '60px', bottom: '3%', top: '40px', containLabel: true },
        xAxis: [{ type: 'category', data: depts, axisLabel: { color: '#5F6368' }}],
        yAxis: [
          { type: 'value', name: '接诊量', axisLabel: { color: '#5F6368' }},
          { type: 'value', name: '过号率(%)', min: 0, max: 30, axisLabel: { formatter: '{value}%', color: '#EA4335' }}
        ],
        series: [
          {
            name: '接诊量（人次）', type: this.deptChartType, yAxisIndex: 0, data: callCounts,
            itemStyle: { color: '#1A73E8', borderRadius: this.deptChartType === 'bar' ? [4, 4, 0, 0] : undefined },
            areaStyle: this.deptChartType === 'line' ? { color: 'rgba(26,115,232,0.1)' } : undefined,
            smooth: this.deptChartType === 'line',
            label: { show: this.deptChartType === 'bar', position: 'top', color: '#1A73E8', fontSize: 11, formatter: '{c}' }
          },
          {
            name: '过号率（%）', type: 'line', yAxisIndex: 1, data: skipRates,
            itemStyle: { color: '#EA4335' }, lineStyle: { width: 2 }, symbol: 'circle', symbolSize: 6,
            label: { show: true, position: 'top', color: '#EA4335', fontSize: 11, formatter: '{c}%' }
          }
        ]
      }
      this.deptChart.setOption(option, true)
    },
    initHourlyChart() {
      const el = document.getElementById('hourlyTrendChart')
      if (!el) return
      if (!this.hourlyChart) this.hourlyChart = this.$echarts.init(el)
      const hours = (this.hourlyData.length ? this.hourlyData : []).map(h => String(h.hourOfDay).padStart(2, '0') + ':00')
      const enqueue = this.hourlyData.map(h => h.enqueueCount || 0)
      const done = this.hourlyData.map(h => h.doneCount || 0)
      const option = {
        tooltip: { trigger: 'axis' },
        legend: { data: ['入队量', '完成量'], top: 0, itemWidth: 12, right: 0 },
        grid: { left: '2%', right: '2%', bottom: '3%', top: '36px', containLabel: true },
        xAxis: { type: 'category', data: hours, axisLabel: { color: '#5F6368', fontSize: 11 }},
        yAxis: { type: 'value', splitLine: { lineStyle: { color: '#F1F3F4', type: 'dashed' }}, axisLabel: { color: '#5F6368', fontSize: 11 }},
        series: [
          { name: '入队量', type: 'line', data: enqueue, smooth: true, itemStyle: { color: '#1A73E8' }, areaStyle: { color: 'rgba(26,115,232,0.08)' }},
          { name: '完成量', type: 'line', data: done, smooth: true, itemStyle: { color: '#34A853' }, areaStyle: { color: 'rgba(52,168,83,0.08)' }}
        ]
      }
      this.hourlyChart.setOption(option)
    },
    initSlaChart() {
      const el = document.getElementById('slaRadarChart')
      if (!el) return
      if (!this.slaChart) this.slaChart = this.$echarts.init(el)
      const rows = this.deptRadarData.slice(0, 5)
      const maxWait = Math.max(30, ...rows.map(r => Number(r.avgWaitMin || 0)))
      const todayVals = rows.length ? [
        Number(rows[0]?.avgWaitMin || 0),
        Math.min(100, (rows[0]?.callCount || 0) * 2),
        88,
        Math.max(0, 100 - Number(rows[0]?.skipRate || 0)),
        Math.min(100, (rows[0]?.callCount || 0) * 3)
      ] : [0, 0, 0, 0, 0]
      const option = {
        tooltip: {},
        legend: { data: ['本日', '上月均值'], bottom: 0, itemWidth: 12 },
        radar: {
          indicator: [
            { name: '平均等候', max: maxWait },
            { name: '接诊效率', max: 100 },
            { name: '患者满意度', max: 100 },
            { name: '过号率低', max: 100 },
            { name: '接诊量', max: 100 }
          ],
          center: ['50%', '48%'], radius: '60%',
          nameGap: 5, axisLine: { lineStyle: { color: '#E8EAED' }},
          splitLine: { lineStyle: { color: '#F1F3F4' }}
        },
        series: [{
          type: 'radar',
          data: [
            { value: todayVals, name: '本日', itemStyle: { color: '#1A73E8' }, areaStyle: { color: 'rgba(26,115,232,0.2)' }}
          ]
        }]
      }
      this.slaChart.setOption(option)
    }
  }
}
</script>

<style lang="scss" scoped>
.stats-header {
  display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px;
}
.kpi-row {
  display: flex; gap: 16px; margin-bottom: 20px;
  .kpi-card {
    flex: 1; background: #FFF; border-radius: 14px; padding: 16px 18px;
    display: flex; align-items: center; gap: 14px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04); transition: all 0.2s;
    &:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0,0,0,0.08); }
  }
  .kpi-icon { width: 48px; height: 48px; border-radius: 50%; display: flex; justify-content: center; align-items: center; font-size: 20px; flex-shrink: 0; }
  .kpi-body { display: flex; flex-direction: column; }
  .kpi-label { font-size: 12px; color: #80868B; margin-bottom: 4px; }
  .kpi-val { font-size: 26px; font-weight: 700; line-height: 1; }
  .kpi-unit { font-size: 12px; color: #80868B; margin-top: 2px; }
}
.chart-grid {
  display: grid; grid-template-columns: 1fr 1fr; grid-template-rows: auto auto; gap: 16px; margin-bottom: 16px;
  .chart-wide { grid-column: 1 / -1; }
}
.table-section { margin-bottom: 0; }
.chart-card {
  background: #FFF; border-radius: 16px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.chart-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 14px; padding-bottom: 12px; border-bottom: 1px solid #F1F3F4;
  .chart-title { font-size: 15px; font-weight: 600; color: #202124; i { margin-right: 6px; } }
}
.rank-badge {
  width: 24px; height: 24px; border-radius: 50%; background: #F1F3F4; color: #5F6368;
  display: flex; justify-content: center; align-items: center; font-size: 12px; font-weight: 700; margin: 0 auto;
  &.rank-top-1 { background: #FBBC04; color: #FFF; }
  &.rank-top-2 { background: #80868B; color: #FFF; }
  &.rank-top-3 { background: #C76B28; color: #FFF; }
}
@media (max-width: 1200px) {
  .chart-grid { grid-template-columns: 1fr; }
  .kpi-row { display: grid; grid-template-columns: repeat(2, 1fr); }
}
</style>
