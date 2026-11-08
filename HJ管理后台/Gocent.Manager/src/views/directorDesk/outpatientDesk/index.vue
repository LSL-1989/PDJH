<template>
  <div class="director-desk">
    <el-tabs v-model="activeTab" class="desk-tabs" @tab-click="handleTabClick">
      <el-tab-pane label="态势大盘" name="dashboard">
        <!-- 页面标题栏 -->
        <div class="desk-header">
          <div class="desk-title">
            <i class="el-icon-s-custom" />
            <div>
              <h2>门诊主任工作台</h2>
              <span class="desk-subtitle">实时掌握门诊运营态势 · 数据截至 {{ currentTime }}</span>
            </div>
          </div>
          <div class="header-actions">
            <el-button type="danger" plain size="small" icon="el-icon-bell">全院广播通知</el-button>
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
                <i
                  :class="kpi.trend > 0 ? 'el-icon-top' : 'el-icon-bottom'"
                  :style="{ color: kpi.trend > 0 ? '#EA4335' : '#34A853' }"
                />
                <span :style="{ color: kpi.trend > 0 ? '#EA4335' : '#34A853' }">{{ Math.abs(kpi.trend) }}% 较昨日</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 主体内容区 -->
        <div class="main-grid">

          <!-- 左列：医生出诊实时看板 -->
          <div class="col-left">
            <div class="section-card">
              <div class="section-header">
                <div class="section-title"><i class="el-icon-s-cooperation" style="color:#1A73E8;" /> 医生出诊实时看板</div>
                <el-tag type="success" size="small" effect="plain">实时</el-tag>
              </div>
              <div class="doctor-list">
                <div v-if="!doctorStatusList.length" style="font-size:13px;color:#80868B;padding:12px 0;">暂无在诊/停诊医生</div>
                <div v-for="doc in doctorStatusList" :key="doc.id" class="doctor-item">
                  <div class="doctor-avatar" :style="{ background: doc.color }">{{ (doc.name || '?').charAt(0) }}</div>
                  <div class="doctor-info">
                    <div class="doctor-name">{{ doc.name }} <el-tag :type="doc.status === '在诊' ? 'success' : 'info'" size="small" effect="dark">{{ doc.status }}</el-tag></div>
                    <div class="doctor-dept">{{ doc.dept }}<span v-if="doc.title"> · {{ doc.title }}</span><span v-if="doc.officeName"> · {{ doc.officeName }}</span></div>
                  </div>
                  <div class="doctor-stats">
                    <div class="stat-item">
                      <span class="stat-label">排队</span>
                      <span class="stat-value" :style="{ color: doc.queueCount > 15 ? '#EA4335' : '#1A73E8' }">{{ doc.queueCount }}</span>
                    </div>
                    <div class="stat-item">
                      <span class="stat-label">均等候</span>
                      <span class="stat-value">{{ doc.avgWait }}min</span>
                    </div>
                  </div>
                  <el-button
                    v-if="doc.status === '在诊'"
                    type="warning"
                    plain
                    size="small"
                    style="margin-left:8px;"
                    @click="stopDoctor(doc)"
                  >临时停诊</el-button>
                  <el-button
                    v-else-if="doc.status === '停诊'"
                    type="success"
                    plain
                    size="small"
                    style="margin-left:8px;"
                    @click="resumeDoctor(doc)"
                  >恢复出诊</el-button>
                </div>
              </div>
            </div>

            <!-- 科室过号率排行 -->
            <div class="section-card" style="margin-top:20px;">
              <div class="section-header">
                <div class="section-title"><i class="el-icon-warning-outline" style="color:#EA4335;" /> 科室过号率排行</div>
                <span style="font-size:12px;color:#80868B;">超过15%标红</span>
              </div>
              <div class="skip-rate-list">
                <div v-if="!skipRateList.length" style="font-size:13px;color:#80868B;padding:12px 0;">暂无过号数据</div>
                <div v-for="(dept, idx) in skipRateList" :key="dept.name" class="skip-item">
                  <div class="skip-rank" :class="idx < 3 ? 'rank-top' : ''">{{ idx + 1 }}</div>
                  <div class="skip-name">{{ dept.name }}</div>
                  <div class="skip-bar-wrapper">
                    <div class="skip-bar" :style="{ width: Math.min(dept.rate, 100) + '%', background: dept.rate > 15 ? '#EA4335' : '#1A73E8' }" />
                  </div>
                  <div class="skip-rate" :style="{ color: dept.rate > 15 ? '#EA4335' : '#202124', fontWeight: dept.rate > 15 ? 700 : 400 }">
                    {{ dept.rate }}%
                    <i v-if="dept.rate > 15" class="el-icon-warning" style="color:#EA4335;" />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 右列：图表区 -->
          <div class="col-right">
            <!-- 分时接诊折线图 -->
            <div class="section-card">
              <div class="section-header">
                <div class="section-title"><i class="el-icon-data-analysis" style="color:#1A73E8;" /> 今日分时接诊趋势</div>
                <el-date-picker v-model="selectedDate" type="date" size="small" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:140px;" :clearable="false" />
              </div>
              <div id="outpatientTrendChart" style="width:100%;height:280px;" />
            </div>

            <!-- 紧急告警列表 -->
            <div class="section-card" style="margin-top:20px;">
              <div class="section-header">
                <div class="section-title"><i class="el-icon-bell" style="color:#FBBC04;" /> 超时等候预警（> 45分钟）</div>
                <el-tag type="danger" size="small">{{ overtimeList.length }} 人需关注</el-tag>
              </div>
              <el-table :data="overtimeList" size="small" :stripe="true" style="width:100%;">
                <el-table-column prop="patientName" label="患者" width="80" />
                <el-table-column prop="deptName" label="科室" />
                <el-table-column prop="waitMinutes" label="等候时长" width="90">
                  <template #default="scope">
                    <span style="color:#EA4335;font-weight:700;">{{ scope.row.waitMinutes }}min</span>
                  </template>
                </el-table-column>
                <el-table-column prop="doctor" label="候诊医生" />
                <table-action-column :count="1">
                  <template #default="{ row }">
                    <table-action-icon icon="el-icon-bell" tip="催促提醒" type="warning" @click="sendReminder(row)" />
                  </template>
                </table-action-column>
              </el-table>
            </div>

            <!-- 号源配额管理 -->
            <div class="section-card" style="margin-top:20px;">
              <div class="section-header">
                <div class="section-title"><i class="el-icon-tickets" style="color:#34A853;" /> 今日号源配额总览</div>
                <el-button text size="small">配额管理 &gt;</el-button>
              </div>
              <div v-if="!quotaList.length" style="font-size:13px;color:#80868B;padding:12px 0;">暂无号源配额（请先在医生排班中维护 max_quota）</div>
              <div class="quota-grid">
                <div v-for="dept in quotaList" :key="dept.name" class="quota-item">
                  <div class="quota-name">{{ dept.name }}</div>
                  <el-progress
                    :percentage="dept.total > 0 ? Math.min(100, Math.round(dept.used / dept.total * 100)) : 0"
                    :color="dept.total > 0 && dept.used / dept.total > 0.85 ? '#EA4335' : '#1A73E8'"
                    :stroke-width="8"
                  />
                  <div class="quota-text">已用 {{ dept.used }} / {{ dept.total }}</div>
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
import {
  getOverviewMetrics,
  getOvertimeAlerts,
  getPeakAnalysis,
  getOutpatientDoctorDutyBoard,
  getOutpatientSkipRateRanking,
  getOutpatientQuotaOverview
} from '@/api/dashboard'
import request from '@/utils/request'

const DOCTOR_COLORS = ['#1A73E8', '#34A853', '#EA4335', '#FBBC04', '#9C27B0', '#FF6D00']

export default {
  name: 'OutpatientDesk',
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
      kpiCards: [
        { key: 'total', title: '今日挂号总量', value: 0, unit: '人', icon: 'el-icon-user', iconBg: '#E8F0FE', iconColor: '#1A73E8', trend: 0 },
        { key: 'done', title: '已接诊完成', value: 0, unit: '人', icon: 'el-icon-circle-check', iconBg: '#E6F4EA', iconColor: '#34A853', trend: 0 },
        { key: 'waiting', title: '当前在排', value: 0, unit: '人', icon: 'el-icon-time', iconBg: '#FEF7E0', iconColor: '#B06000' },
        { key: 'skip', title: '今日过号', value: 0, unit: '人', icon: 'el-icon-warning', iconBg: '#FCE8E6', iconColor: '#EA4335', valueColor: '#EA4335', trend: 0 }
      ],
      doctorStatusList: [],
      skipRateList: [],
      overtimeList: [],
      quotaList: []
    }
  },
  watch: {
    selectedDate() {
      this.loadTrendChart()
    }
  },
  mounted() {
    this.updateTime()
    this.timer = setInterval(this.updateTime, 1000)
    this.$nextTick(() => { this.initTrendChart() })
    this.refreshAll(false)
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
    refreshAll(showTip = true) {
      this.refreshing = true
      Promise.all([
        this.loadKpiStats(),
        this.loadDoctorStatus(),
        this.loadSkipRateList(),
        this.loadOvertimeList(),
        this.loadQuotaList(),
        this.loadTrendChart()
      ]).finally(() => {
        this.refreshing = false
        if (showTip) this.$message.success('数据已刷新')
      })
    },
    loadKpiStats() {
      return getOverviewMetrics()
        .then(res => {
          const data = res.data || {}
          const waiting = Number(data.waitingCount) || 0
          const completed = Number(data.completedCount) || 0
          const skip = Number(data.skipCount) || 0
          const total = Number(data.totalCount) || (waiting + completed + skip)
          this.kpiCards = [
            { key: 'total', title: '今日挂号总量', value: total, unit: '人', icon: 'el-icon-user', iconBg: '#E8F0FE', iconColor: '#1A73E8', trend: 0 },
            { key: 'done', title: '已接诊完成', value: completed, unit: '人', icon: 'el-icon-circle-check', iconBg: '#E6F4EA', iconColor: '#34A853', trend: 0 },
            { key: 'waiting', title: '当前在排', value: waiting, unit: '人', icon: 'el-icon-time', iconBg: '#FEF7E0', iconColor: '#B06000' },
            { key: 'skip', title: '今日过号', value: skip, unit: '人', icon: 'el-icon-warning', iconBg: '#FCE8E6', iconColor: '#EA4335', valueColor: '#EA4335', trend: 0 }
          ]
        })
        .catch(() => {})
    },
    loadDoctorStatus() {
      return getOutpatientDoctorDutyBoard()
        .then(res => {
          const data = Array.isArray(res.data) ? res.data : []
          this.doctorStatusList = data.map((d, idx) => ({
            id: d.doctorId,
            doctorId: d.doctorId,
            name: d.doctorName || '未知医生',
            dept: d.deptName || '',
            title: d.title || '',
            officeName: d.officeName || '',
            status: d.status || (Number(d.isOnDuty) === 1 ? '在诊' : '停诊'),
            queueCount: Number(d.waitingCount) || 0,
            avgWait: Math.round(Number(d.avgWaitMin) || 0),
            color: DOCTOR_COLORS[idx % DOCTOR_COLORS.length]
          }))
        })
        .catch(() => { this.doctorStatusList = [] })
    },
    loadSkipRateList() {
      return getOutpatientSkipRateRanking()
        .then(res => {
          const data = Array.isArray(res.data) ? res.data : []
          this.skipRateList = data.map(d => ({
            name: d.name || '未知科室',
            rate: Math.round((Number(d.rate) || 0) * 10) / 10
          }))
        })
        .catch(() => { this.skipRateList = [] })
    },
    loadOvertimeList() {
      return getOvertimeAlerts(45)
        .then(res => {
          const data = Array.isArray(res.data) ? res.data : []
          this.overtimeList = data.map(d => ({
            patientName: d.patientName || '未知',
            deptName: d.deptName || d.deptId || '未知科室',
            waitMinutes: Number(d.waitMinutes) || 0,
            doctor: d.doctor || '—'
          }))
        })
        .catch(() => { this.overtimeList = [] })
    },
    loadQuotaList() {
      return getOutpatientQuotaOverview()
        .then(res => {
          const data = Array.isArray(res.data) ? res.data : []
          this.quotaList = data.map(d => ({
            name: d.name || '未知科室',
            used: Number(d.used) || 0,
            total: Number(d.total) || 0
          }))
        })
        .catch(() => { this.quotaList = [] })
    },
    stopDoctor(doc) {
      this.$confirm(`确认将 ${doc.name} 医生设置为临时停诊？将离诊并标记当日排班为停诊。`, '停诊确认', { type: 'warning' }).then(() => {
        request({
          url: '/api/Doctor/SuspendDuty',
          method: 'post',
          data: { doctorId: doc.doctorId || doc.id, remark: '主任台临时停诊' }
        }).then(() => {
          this.$message.success(`${doc.name} 已设置为停诊`)
          this.loadDoctorStatus()
        }).catch(err => {
          this.$message.error((err && err.message) || '停诊失败')
        })
      }).catch(() => {})
    },
    resumeDoctor(doc) {
      this.$confirm(`确认取消 ${doc.name} 的临时停诊？取消后医生需重新登录叫号器开诊。`, '恢复出诊', { type: 'info' }).then(() => {
        request({
          url: '/api/Doctor/ResumeDuty',
          method: 'post',
          data: { doctorId: doc.doctorId || doc.id }
        }).then(() => {
          this.$message.success(`${doc.name} 已取消停诊`)
          this.loadDoctorStatus()
        }).catch(err => {
          this.$message.error((err && err.message) || '恢复失败')
        })
      }).catch(() => {})
    },
    sendReminder(row) {
      this.$message.success(`已向 ${row.patientName} 发送等候提醒`)
    },
    initTrendChart() {
      const el = document.getElementById('outpatientTrendChart')
      if (!el) return
      this.trendChart = this.$echarts.init(el)
      window.addEventListener('resize', this.handleResize)
      this.loadTrendChart()
    },
    loadTrendChart() {
      if (!this.trendChart) return Promise.resolve()
      const hours = ['07:00', '08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00']
      const emptyOption = {
        tooltip: { trigger: 'axis', axisPointer: { type: 'cross' }},
        legend: { data: ['挂号量', '接诊量', '过号量'], top: 0 },
        grid: { left: '3%', right: '4%', bottom: '3%', top: '40px', containLabel: true },
        xAxis: { type: 'category', data: hours, axisLine: { lineStyle: { color: '#DADCE0' }}, axisLabel: { color: '#5F6368' }},
        yAxis: { type: 'value', splitLine: { lineStyle: { color: '#F1F3F4', type: 'dashed' }}, axisLabel: { color: '#5F6368' }},
        series: [
          { name: '挂号量', type: 'line', smooth: true, data: [], itemStyle: { color: '#1A73E8' }},
          { name: '接诊量', type: 'line', smooth: true, data: [], itemStyle: { color: '#34A853' }},
          { name: '过号量', type: 'bar', data: [], itemStyle: { color: 'rgba(234,67,53,0.6)' }}
        ]
      }
      return getPeakAnalysis(this.selectedDate)
        .then(res => {
          const peakData = Array.isArray(res.data) ? res.data : []
          const enqueueData = hours.map((_, idx) => {
            const match = peakData.find(p => Number(p.hourOfDay) === idx + 7)
            return match ? Number(match.enqueueCount) || 0 : 0
          })
          const processData = hours.map((_, idx) => {
            const match = peakData.find(p => Number(p.hourOfDay) === idx + 7)
            return match ? Number(match.processCount) || 0 : 0
          })
          const skipData = hours.map((_, idx) => {
            const match = peakData.find(p => Number(p.hourOfDay) === idx + 7)
            return match ? Number(match.skipCount) || 0 : 0
          })
          this.trendChart.setOption({
            tooltip: { trigger: 'axis', axisPointer: { type: 'cross' }},
            legend: { data: ['挂号量', '接诊量', '过号量'], top: 0 },
            grid: { left: '3%', right: '4%', bottom: '3%', top: '40px', containLabel: true },
            xAxis: { type: 'category', data: hours, axisLine: { lineStyle: { color: '#DADCE0' }}, axisLabel: { color: '#5F6368' }},
            yAxis: { type: 'value', splitLine: { lineStyle: { color: '#F1F3F4', type: 'dashed' }}, axisLabel: { color: '#5F6368' }},
            series: [
              { name: '挂号量', type: 'line', smooth: true, data: enqueueData, itemStyle: { color: '#1A73E8' }, areaStyle: { color: 'rgba(26,115,232,0.08)' }},
              { name: '接诊量', type: 'line', smooth: true, data: processData, itemStyle: { color: '#34A853' }, areaStyle: { color: 'rgba(52,168,83,0.08)' }},
              { name: '过号量', type: 'bar', data: skipData, itemStyle: { color: 'rgba(234,67,53,0.6)', borderRadius: [3, 3, 0, 0] }}
            ]
          })
        })
        .catch(() => { this.trendChart.setOption(emptyOption) })
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
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 24px;
  .desk-title {
    display: flex; align-items: center; gap: 14px;
    i { font-size: 36px; color: #1A73E8; }
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
    transition: transform 0.2s, box-shadow 0.2s;
    &:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0,0,0,0.08); }
  }
  .kpi-icon-wrapper {
    width: 52px; height: 52px; border-radius: 50%;
    display: flex; justify-content: center; align-items: center; font-size: 22px; flex-shrink: 0;
  }
  .kpi-info { flex: 1; }
  .kpi-title { font-size: 13px; color: #5F6368; margin-bottom: 6px; }
  .kpi-value { font-size: 30px; font-weight: 700; line-height: 1; }
  .kpi-unit { font-size: 14px; color: #80868B; font-weight: 400; }
  .kpi-trend { font-size: 12px; margin-top: 6px; i { margin-right: 2px; } }
}

.main-grid { display: flex; gap: 20px; }
.col-left { flex: 5; display: flex; flex-direction: column; }
.col-right { flex: 6; display: flex; flex-direction: column; }

.section-card {
  background: #FFF; border-radius: 16px; padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.section-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; padding-bottom: 14px; border-bottom: 1px solid #F1F3F4;
  .section-title { font-size: 15px; font-weight: 600; color: #202124; i { margin-right: 6px; } }
}

.doctor-list { display: flex; flex-direction: column; gap: 10px; max-height: 350px; overflow-y: auto; }
.doctor-item {
  display: flex; align-items: center; padding: 10px; border-radius: 10px;
  background: #F8F9FA; gap: 10px;
  &:hover { background: #F1F3F4; }
}
.doctor-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; justify-content: center; align-items: center;
  color: white; font-weight: 700; font-size: 15px; flex-shrink: 0;
}
.doctor-info { flex: 1; min-width: 0; }
.doctor-name { font-size: 14px; font-weight: 600; color: #202124; margin-bottom: 3px; }
.doctor-dept { font-size: 12px; color: #80868B; }
.doctor-stats { display: flex; gap: 14px; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-label { font-size: 11px; color: #80868B; }
.stat-value { font-size: 18px; font-weight: 700; line-height: 1.2; color: #202124; }

.skip-rate-list { display: flex; flex-direction: column; gap: 10px; }
.skip-item { display: flex; align-items: center; gap: 10px; }
.skip-rank {
  width: 22px; height: 22px; border-radius: 50%; background: #F1F3F4;
  display: flex; justify-content: center; align-items: center;
  font-size: 12px; font-weight: 700; color: #5F6368; flex-shrink: 0;
  &.rank-top { background: #FEF7E0; color: #B06000; }
}
.skip-name { width: 80px; font-size: 13px; color: #202124; flex-shrink: 0; }
.skip-bar-wrapper { flex: 1; height: 8px; background: #F1F3F4; border-radius: 4px; overflow: hidden; }
.skip-bar { height: 100%; border-radius: 4px; transition: width 0.6s ease; }
.skip-rate { width: 55px; font-size: 13px; text-align: right; flex-shrink: 0; display: flex; align-items: center; justify-content: flex-end; gap: 3px; }

.quota-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.quota-item .quota-name { font-size: 13px; font-weight: 600; color: #202124; margin-bottom: 6px; }
.quota-item .quota-text { font-size: 12px; color: #80868B; margin-top: 4px; text-align: right; }

@media (max-width: 1200px) {
  .main-grid { flex-direction: column; }
  .kpi-row { display: grid; grid-template-columns: repeat(2,1fr); }
}
</style>
