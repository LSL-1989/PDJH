<template>
  <div class="director-desk">
    <el-tabs v-model="activeTab" class="desk-tabs" @tab-click="handleTabClick">
      <el-tab-pane label="态势大盘" name="dashboard">
        <!-- 页面标题栏 -->
        <div class="desk-header">
          <div class="desk-title">
            <i class="el-icon-first-aid-kit" />
            <div>
              <h2>体检中心主任工作台</h2>
              <span class="desk-subtitle">全员体检进度实时跟踪 · 数据截至 {{ currentTime }}</span>
            </div>
          </div>
          <div class="header-actions">
            <el-button type="primary" size="small" icon="el-icon-refresh" :loading="refreshing" @click="refreshAll">刷新数据</el-button>
          </div>
        </div>

        <!-- ▶ 算路策略控制台（P0-3） -->
        <div class="strategy-console">
          <div class="strategy-header">
            <span class="strategy-title"><i class="el-icon-magic-stick" style="color:#1A73E8;" /> 体检导检算路策略控制台</span>
            <div style="display:flex;align-items:center;gap:12px;">
              <span class="strategy-current-label">当前生效：</span>
              <el-tag :type="currentStrategyTag.type" effect="dark">
                <i :class="currentStrategyTag.icon" /> {{ currentStrategyTag.label }}
              </el-tag>
              <el-button type="primary" size="small" :loading="strategySaving" @click="saveStrategy">应用策略</el-button>
            </div>
          </div>
          <div class="strategy-cards">
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
              <div class="kpi-bar">
                <el-progress :percentage="kpi.pct" :color="kpi.iconColor" :stroke-width="4" :show-text="false" style="flex:1;" />
                <span class="kpi-pct-label">{{ kpi.pct }}%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 主体区域 -->
        <div class="main-grid">

          <!-- 左列 -->
          <div class="col-left">

            <!-- 科室拥堵热力矩阵 -->
            <div class="section-card">
              <div class="section-header">
                <div class="section-title"><i class="el-icon-hot-water" style="color:#EA4335;" /> 各检查科室实时拥堵热力</div>
                <div style="display:flex;align-items:center;gap:6px;font-size:12px;color:#80868B;">
                  <span class="legend-dot" style="background:#E6F4EA;border:1px solid #34A853;" />空闲
                  <span class="legend-dot" style="background:#FEF7E0;border:1px solid #FBBC04;" />拥堵
                  <span class="legend-dot" style="background:#FCE8E6;border:1px solid #EA4335;" />告急
                </div>
              </div>
              <div v-loading="heatmapLoading" class="heatmap-grid">
                <div v-for="dept in heatmapData" :key="dept.deptName" class="heat-cell" :style="getCellStyle(dept)">
                  <div class="heat-dept-name">{{ dept.deptName }}</div>
                  <div class="heat-count">{{ dept.waitingCount }}</div>
                  <div class="heat-label">人等候</div>
                </div>
              </div>
            </div>

            <!-- 各项目完成率 -->
            <div class="section-card" style="margin-top:20px;">
              <div class="section-header">
                <div class="section-title"><i class="el-icon-data-analysis" style="color:#1A73E8;" /> 体检大项完成率进度</div>
                <span style="font-size:12px;color:#80868B;">今日安排 {{ totalScheduled }} 人</span>
              </div>
              <div id="examProgressChart" style="width:100%;height:220px;" />
            </div>
          </div>

          <!-- 右列 -->
          <div class="col-right">

            <!-- ▶ P1-4b: el-tabs 包裹右列 -->
            <el-tabs v-model="rightTab" type="card" class="right-tabs">

              <!-- Tab 1: 今日体检人员进度表 -->
              <el-tab-pane label="📋 今日完成进度" name="progress">
                <div class="section-card" style="height:100%;border-radius:0 12px 12px 12px;">
                  <div class="section-header">
                    <div class="section-title"><i class="el-icon-s-check" style="color:#34A853;" /> 今日体检人员完成进度</div>
                    <div style="display:flex;gap:10px;align-items:center;">
                      <el-input v-model="searchPatient" size="small" placeholder="搜索患者" prefix-icon="el-icon-search" style="width:160px;" clearable />
                      <el-select v-model="filterStatus" size="small" style="width:110px;" placeholder="状态筛选">
                        <el-option label="全部" value="" />
                        <el-option label="已全部完成" value="done" />
                        <el-option label="进行中" value="doing" />
                        <el-option label="未开始" value="pending" />
                      </el-select>
                    </div>
                  </div>
                  <el-table :data="filteredPatients" size="small" :stripe="true" style="width:100%;" height="480">
                    <el-table-column prop="name" label="患者" width="80">
                      <template #default="scope">
                        <span style="font-weight:600;">{{ scope.row.name }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column prop="package" label="套餐" width="90" show-overflow-tooltip>
                      <template #default="scope">
                        <el-tag type="info" size="small" effect="plain">{{ scope.row.package }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="血液" width="55" align="center">
                      <template #default="scope"><i :class="getItemIcon(scope.row.items.blood)" :style="{ color: getItemColor(scope.row.items.blood) }" /></template>
                    </el-table-column>
                    <el-table-column label="彩超" width="55" align="center">
                      <template #default="scope"><i :class="getItemIcon(scope.row.items.ultrasound)" :style="{ color: getItemColor(scope.row.items.ultrasound) }" /></template>
                    </el-table-column>
                    <el-table-column label="心电" width="55" align="center">
                      <template #default="scope"><i :class="getItemIcon(scope.row.items.ecg)" :style="{ color: getItemColor(scope.row.items.ecg) }" /></template>
                    </el-table-column>
                    <el-table-column label="胸片" width="55" align="center">
                      <template #default="scope"><i :class="getItemIcon(scope.row.items.xray)" :style="{ color: getItemColor(scope.row.items.xray) }" /></template>
                    </el-table-column>
                    <el-table-column label="内科" width="55" align="center">
                      <template #default="scope"><i :class="getItemIcon(scope.row.items.internal)" :style="{ color: getItemColor(scope.row.items.internal) }" /></template>
                    </el-table-column>
                    <el-table-column label="外科" width="55" align="center">
                      <template #default="scope"><i :class="getItemIcon(scope.row.items.surgery)" :style="{ color: getItemColor(scope.row.items.surgery) }" /></template>
                    </el-table-column>
                    <el-table-column label="进度" width="100" align="center">
                      <template #default="scope">
                        <el-tag :type="getStatusType(scope.row.overallStatus)" size="small" effect="dark">{{ scope.row.overallStatus }}</el-tag>
                      </template>
                    </el-table-column>
                    <table-action-column :count="1">
                      <template #default="{ row }">
                        <table-action-icon icon="el-icon-sort" tip="调路" type="primary" @click="adjustRoute(row)" />
                      </template>
                    </table-action-column>
                  </el-table>
                  <div style="display:flex;align-items:center;justify-content:flex-end;margin-top:12px;gap:16px;font-size:12px;color:#5F6368;">
                    <span><i class="el-icon-circle-check" style="color:#34A853;" /> 已完成</span>
                    <span><i class="el-icon-loading" style="color:#1A73E8;" /> 进行中</span>
                    <span><i class="el-icon-minus" style="color:#DADCE0;" /> 未包含/不需要</span>
                    <span><i class="el-icon-circle-close" style="color:#DADCE0;" /> 未开始</span>
                  </div>
                </div>
              </el-tab-pane>

              <!-- Tab 2: 在途患者实时看板 (P1-4b) -->
              <el-tab-pane name="inprogress">
                <template #label><span><i class="el-icon-position" style="color:#1A73E8;" /> 在途患者 <el-badge :value="inProgressList.length" :max="99" type="primary" style="margin-left:4px;" /></span></template>
                <div class="section-card" style="border-radius:0 12px 12px 12px;">
                  <div class="section-header">
                    <div class="section-title"><i class="el-icon-position" style="color:#1A73E8;" /> 在途患者实时状态</div>
                    <div style="font-size:12px;color:#80868B;">
                      <i class="el-icon-refresh" :class="{'el-icon-loading': inProgressLoading}" />
                      10秒自动刷新 · 共 {{ inProgressList.length }} 人在途
                    </div>
                  </div>
                  <el-table v-loading="inProgressLoading" :data="inProgressList" size="small" stripe height="480">
                    <el-table-column prop="patientName" label="患者" width="75" show-overflow-tooltip>
                      <template #default="{row}">
                        <span style="font-weight:600;">{{ row.patientName }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="进度" min-width="100">
                      <template #default="{row}">
                        <div style="display:flex;align-items:center;gap:6px;">
                          <el-progress :percentage="row.progressPct" :stroke-width="6" :show-text="false" style="flex:1;" :color="row.progressPct < 30 ? '#EA4335' : row.progressPct < 70 ? '#FBBC04' : '#34A853'" />
                          <span style="font-size:11px;color:#5F6368;white-space:nowrap;">{{ row.completedCount }}/{{ row.totalCount }}</span>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column label="当前科室" width="90" show-overflow-tooltip>
                      <template #default="{row}">
                        <el-tag size="small" type="info" effect="plain">{{ row.currentDept || '待分配' }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="前面" width="55" align="center">
                      <template #default="{row}">
                        <span :style="{ color: row.currentWaitCount > 5 ? '#EA4335' : '#34A853', fontWeight: '600' }">
                          {{ row.currentWaitCount }} 人
                        </span>
                      </template>
                    </el-table-column>
                    <el-table-column label="预计完成" width="70" align="center">
                      <template #default="{row}">
                        <span style="font-size:12px;color:#5F6368;">约 {{ row.estimatedFinishMin }} 分</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="状态" width="60" align="center">
                      <template #default="{row}">
                        <el-tag :type="row.status === '未开始' ? 'info' : 'warning'" size="small">{{ row.status }}</el-tag>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </el-tab-pane>

            </el-tabs>
          </div>
        </div></el-tab-pane>

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
  name: 'ExamDesk',
  components: {
    QueueList
  },
  data() {
    return {
      activeTab: 'dashboard',
      currentTime: '',
      refreshing: false,
      searchPatient: '',
      filterStatus: '',
      progressChart: null,
      totalScheduled: 0,
      // ── P1-4b 在途患者看板 ──
      rightTab: 'progress',
      inProgressList: [],
      inProgressLoading: false,
      inProgressTimer: null,
      heatmapLoading: false,
      heatmapTimer: null,
      // ── P0-3 算路策略 ──
      selectedStrategy: 'FASTING_FIRST',
      activeStrategy: 'FASTING_FIRST',
      strategySaving: false,
      strategies: [
        {
          key: 'FASTING_FIRST',
          name: '空腹优先',
          desc: '优先安排需要空腹的检查项（血液/腹部超声），确保早晨空腹项目不被耽误',
          icon: 'el-icon-sunrise',
          bg: 'linear-gradient(135deg,#FF8F00,#FFB300)'
        },
        {
          key: 'LEAST_WAIT',
          name: '最少等待',
          desc: '实时比较各科室当前排队人数，优先将患者送至等候人最少的科室',
          icon: 'el-icon-data-line',
          bg: 'linear-gradient(135deg,#1A73E8,#0D47A1)'
        },
        {
          key: 'SHORTEST_PATH',
          name: '最短动线',
          desc: '按科室物理位置规划最短行走路径，减少患者在院内的步行距离',
          icon: 'el-icon-map-location',
          bg: 'linear-gradient(135deg,#34A853,#1E7E34)'
        },
        {
          key: 'NONE',
          name: '手动导检',
          desc: '关闭自动算路，所有路由由护士工作站手动干预分配，适合特殊体检日',
          icon: 'el-icon-user',
          bg: 'linear-gradient(135deg,#80868B,#5F6368)'
        }
      ],
      kpiCards: [
        { key: 'total', title: '今日体检总人数', value: 0, unit: '人', icon: 'el-icon-user', iconBg: '#E8F0FE', iconColor: '#1A73E8', pct: 100 },
        { key: 'done', title: '已全部完成', value: 0, unit: '人', icon: 'el-icon-circle-check', iconBg: '#E6F4EA', iconColor: '#34A853', pct: 0 },
        { key: 'doing', title: '正在体检中', value: 0, unit: '人', icon: 'el-icon-loading', iconBg: '#E8F0FE', iconColor: '#1A73E8', pct: 0 },
        { key: 'pending', title: '尚未开始', value: 0, unit: '人', icon: 'el-icon-time', iconBg: '#F5F5F5', iconColor: '#80868B', pct: 0 }
      ],
      heatmapData: [],
      patientList: []
    }
  },
  computed: {
    filteredPatients() {
      return this.patientList.filter(p => {
        const nameMatch = !this.searchPatient || p.name.includes(this.searchPatient)
        const statusMap = { done: '已完成', doing: '进行中', pending: '未开始' }
        const statusMatch = !this.filterStatus || p.overallStatus === (statusMap[this.filterStatus] || p.overallStatus)
        return nameMatch && statusMatch
      })
    },
    currentStrategyTag() {
      const map = {
        FASTING_FIRST: { label: '空腹优先', type: 'warning', icon: 'el-icon-sunrise' },
        LEAST_WAIT: { label: '最少等待', type: '', icon: 'el-icon-data-line' },
        SHORTEST_PATH: { label: '最短动线', type: 'success', icon: 'el-icon-map-location' },
        NONE: { label: '手动导检', type: 'info', icon: 'el-icon-user' }
      }
      return map[this.activeStrategy] || map['FASTING_FIRST']
    }
  },
  mounted() {
    this.updateTime()
    this.timer = setInterval(this.updateTime, 1000)
    this.$nextTick(() => { this.initProgressChart() })
    this.loadKpiStats()
    this.loadStrategy()
    this.loadInProgress()
    this.loadHeatmap()
    this.inProgressTimer = setInterval(this.loadInProgress, 10000)
    this.heatmapTimer = setInterval(this.loadHeatmap, 15000)
  },
  beforeUnmount() {
    clearInterval(this.timer)
    clearInterval(this.inProgressTimer)
    clearInterval(this.heatmapTimer)
    if (this.progressChart) { this.progressChart.dispose(); this.progressChart = null }
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
      this.loadStrategy()
      this.loadInProgress()
      this.loadHeatmap()
      setTimeout(() => { this.refreshing = false; this.$message.success('数据已刷新') }, 800)
    },
    loadKpiStats() {
      import('@/utils/request').then(m => {
        // 获取体检统计 KPI 数据
        m.default({ url: '/api/exam/getPatientTodoList', method: 'post', data: {} })
          .then(res => {
            const data = res.data || {}
            const pendingList = data.pending || []
            const completedList = data.completed || []
            const currentItem = data.current || null

            const totalPatients = pendingList.length + completedList.length + (currentItem ? 1 : 0)
            const donePatients = completedList.length + (currentItem ? 1 : 0)
            const doingPatients = pendingList.length
            const pendingPatients = 0

            // 更新 KPI 卡片
            this.kpiCards = [
              { key: 'total', title: '今日体检总人数', value: totalPatients, unit: '人', icon: 'el-icon-user', iconBg: '#E8F0FE', iconColor: '#1A73E8', pct: 100 },
              { key: 'done', title: '已全部完成', value: donePatients, unit: '人', icon: 'el-icon-circle-check', iconBg: '#E6F4EA', iconColor: '#34A853', pct: totalPatients > 0 ? Math.round(donePatients / totalPatients * 100) : 0 },
              { key: 'doing', title: '正在体检中', value: doingPatients, unit: '人', icon: 'el-icon-loading', iconBg: '#E8F0FE', iconColor: '#1A73E8', pct: totalPatients > 0 ? Math.round(doingPatients / totalPatients * 100) : 0 },
              { key: 'pending', title: '尚未开始', value: pendingPatients, unit: '人', icon: 'el-icon-time', iconBg: '#F5F5F5', iconColor: '#80868B', pct: 0 }
            ]
            this.totalScheduled = totalPatients
          })
          .catch(() => {
            this.$message.warning('体检统计数据加载失败')
          })
      })
    },
    loadStrategy() {
      // 读取后端已存配置
      const request = this.$http || window.axios
      if (!request) return
      import('@/utils/request').then(m => {
        m.default({ url: '/api/systemConfig/getByKey', method: 'get', params: { key: 'RULE_EXAM' }})
          .then(res => {
            try {
              const cfg = JSON.parse(res.data?.configValue || '{}')
              if (cfg.triageAlgorithm) {
                this.activeStrategy = cfg.triageAlgorithm
                this.selectedStrategy = cfg.triageAlgorithm
              }
            } catch (e) {}
          }).catch(() => {})
      })
    },
    saveStrategy() {
      this.strategySaving = true
      getConfigByKey('RULE_EXAM').then(res => {
        let cfg = {}
        try { cfg = JSON.parse((res.data && (res.data.configValue || res.data.ConfigValue)) || '{}') } catch (e) {}
        cfg.triageAlgorithm = this.selectedStrategy
        return saveConfig({ configKey: 'RULE_EXAM', configValue: JSON.stringify(cfg), description: '体检TSP策略' })
      }).then(() => {
        this.activeStrategy = this.selectedStrategy
        const name = this.strategies.find(s => s.key === this.selectedStrategy)?.name
        this.$message.success(`算路策略已切换为「${name}」`)
      }).catch(() => this.$message.error('策略保存失败'))
        .finally(() => { this.strategySaving = false })
    },
    adjustRoute(row) {
      import('@/utils/request').then(m => {
        m.default({ url: '/api/exam/adjustQueue', method: 'post', data: { patientCode: row.patientCode || row.code }})
          .then(() => this.$message.success(`已为 ${row.patientName || row.name} 重新算路`))
          .catch(() => this.$message.error('算路调整失败'))
      })
    },
    getCellStyle(dept) {
      if (dept.statusLevel === 'CONGESTED') return { background: '#FCE8E6', borderColor: '#EA4335', color: '#B31412' }
      if (dept.statusLevel === 'BUSY') return { background: '#FEF7E0', borderColor: '#FBBC04', color: '#B06000' }
      return { background: '#E6F4EA', borderColor: '#34A853', color: '#137333' }
    },
    getItemIcon(status) {
      return { done: 'el-icon-circle-check', doing: 'el-icon-loading', pending: 'el-icon-circle-close', none: 'el-icon-minus' }[status] || 'el-icon-minus'
    },
    getItemColor(status) {
      return { done: '#34A853', doing: '#1A73E8', pending: '#DADCE0', none: '#DADCE0' }[status] || '#DADCE0'
    },
    getStatusType(status) {
      return { '已完成': 'success', '进行中': 'primary', '未开始': 'info' }[status] || 'info'
    },
    initProgressChart() {
      const el = document.getElementById('examProgressChart')
      if (!el) return
      this.progressChart = this.$echarts.init(el)
      const items = ['采血/化验', '彩超检查', '心电图', '胸部X光', '内科检查', '外科检查']
      const done = [118, 95, 110, 88, 102, 115]
      const total = [128, 128, 128, 110, 128, 128]
      const pending = total.map((t, i) => t - done[i])
      const option = {
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: (params) => {
          const d = params[0]; const p = params[1]
          return `${d.name}<br/>已完成: <b>${d.value}</b><br/>未完成: <b>${p.value}</b><br/>完成率: <b>${Math.round(d.value / (d.value + p.value) * 100)}%</b>`
        } },
        legend: { data: ['已完成', '未完成'], top: 0 },
        grid: { left: '2%', right: '2%', bottom: '3%', top: '36px', containLabel: true },
        xAxis: { type: 'value', max: 128, axisLabel: { color: '#5F6368', fontSize: 11 }},
        yAxis: { type: 'category', data: items, axisLabel: { color: '#5F6368', fontSize: 12 }},
        series: [
          { name: '已完成', type: 'bar', stack: 'total', data: done, itemStyle: { color: '#34A853', borderRadius: [0, 0, 0, 0] }, label: { show: true, position: 'inside', formatter: '{c}', color: '#fff', fontSize: 11 }},
          { name: '未完成', type: 'bar', stack: 'total', data: pending, itemStyle: { color: '#F1F3F4', borderRadius: [4, 4, 4, 4] }}
        ]
      }
      this.progressChart.setOption(option)
      window.addEventListener('resize', this.handleResize)
    },
    handleResize() {
      if (this.progressChart) {
        this.progressChart.resize()
      }
    },
    handleTabClick(tab) {
      if (tab.name === 'dashboard') {
        this.$nextTick(() => {
          this.handleResize()
        })
      }
    },
    loadInProgress() {
      this.inProgressLoading = true
      import('@/utils/request').then(m => {
        m.default({ url: '/api/exam/inProgress', method: 'get' })
          .then(res => {
            this.inProgressList = Array.isArray(res.data) ? res.data : (res.data?.data || [])
          })
          .catch(() => {
            // Mock 降级数据
            this.inProgressList = []
          })
          .finally(() => { this.inProgressLoading = false })
      })
    },
    loadHeatmap() {
      this.heatmapLoading = true
      import('@/utils/request').then(m => {
        m.default({ url: '/api/dashboard/heatmap-matrix', method: 'get' })
          .then(res => {
            this.heatmapData = Array.isArray(res.data) ? res.data : (res.data?.data || [])
          })
          .catch(() => {
            this.$message.warning('真实热力数据加载失败')
          })
          .finally(() => { this.heatmapLoading = false })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.director-desk { padding: 0; }

/* ── 算路策略控制台 ── */
.strategy-console {
  background: #FFF;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.strategy-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #F1F3F4;
}
.strategy-title { font-size: 15px; font-weight: 600; color: #202124; i { margin-right: 6px; } }
.strategy-current-label { font-size: 13px; color: #5F6368; }
.strategy-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.strategy-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 12px;
  border: 2px solid #F1F3F4;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  &:hover { border-color: #AECBFA; background: #F0F5FF; transform: translateY(-1px); }
  &.active {
    border-color: #1A73E8;
    background: linear-gradient(135deg, #E8F0FE, #F0F5FF);
    box-shadow: 0 4px 14px rgba(26,115,232,0.15);
  }
}
.sc-icon {
  width: 44px; height: 44px; border-radius: 12px; flex-shrink: 0;
  display: flex; justify-content: center; align-items: center;
  i { font-size: 20px; color: #FFF; }
}
.sc-body { flex: 1; min-width: 0; }
.sc-name { font-size: 14px; font-weight: 600; color: #202124; margin-bottom: 3px; }
.sc-desc { font-size: 12px; color: #80868B; line-height: 1.4; }
.sc-check { position: absolute; top: 10px; right: 10px; color: #1A73E8; font-size: 16px; font-weight: 700; }

.desk-tabs {
  background: #fff;
  border-radius: 8px;
  padding: 0 20px 20px 20px;
  min-height: calc(100vh - 84px);
}
::v-deep .desk-tabs > .el-tabs__header .el-tabs__nav-wrap::after {
  height: 1px;
  background-color: #F1F3F4;
}
::v-deep .desk-tabs > .el-tabs__header .el-tabs__item {
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
::v-deep .desk-tabs > .el-tabs__header .el-tabs__active-bar {
  height: 3px;
  border-radius: 3px;
  background-color: #1A73E8;
}

/* ── 右列 Tabs ── */
.right-tabs {
  height: 100%;
  :deep(.el-tabs__header) {
    margin-bottom: 0;
    .el-tabs__nav-wrap::after { display: none; }
    .el-tabs__item {
      height: 36px; line-height: 36px; font-size: 13px;
      border-radius: 8px 8px 0 0 !important;
      &.is-active { background: #FFF; color: #1A73E8; font-weight: 600; }
    }
  }
  :deep(.el-tabs__content) { padding: 0; }
}
.desk-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;
  .desk-title { display: flex; align-items: center; gap: 14px;
    i { font-size: 36px; color: #34A853; }
    h2 { margin: 0; font-size: 22px; font-weight: 700; color: #202124; }
    .desk-subtitle { font-size: 13px; color: #80868B; margin-top: 4px; display: block; }
  }
  .header-actions { display: flex; gap: 10px; }
}
.kpi-row {
  display: flex; gap: 20px; margin-bottom: 24px;
  .kpi-card {
    flex: 1; background: #FFF; border-radius: 16px; padding: 20px;
    display: flex; align-items: flex-start; gap: 14px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04); transition: all 0.2s;
    &:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0,0,0,0.08); }
  }
  .kpi-icon-wrapper { width: 52px; height: 52px; border-radius: 50%; display: flex; justify-content: center; align-items: center; font-size: 22px; flex-shrink: 0; }
  .kpi-info { flex: 1; }
  .kpi-title { font-size: 13px; color: #5F6368; margin-bottom: 6px; }
  .kpi-value { font-size: 28px; font-weight: 700; line-height: 1; margin-bottom: 8px; }
  .kpi-unit { font-size: 13px; color: #80868B; font-weight: 400; }
  .kpi-bar { display: flex; align-items: center; gap: 8px; }
  .kpi-pct-label { font-size: 12px; color: #80868B; flex-shrink: 0; }
}
.main-grid { display: flex; gap: 20px; }
.col-left { flex: 5; display: flex; flex-direction: column; }
.col-right { flex: 7; }
.section-card { background: #FFF; border-radius: 16px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.section-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; padding-bottom: 14px; border-bottom: 1px solid #F1F3F4;
  .section-title { font-size: 15px; font-weight: 600; color: #202124; i { margin-right: 6px; } display: flex; align-items: center; }
}
.heatmap-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.heat-cell {
  border: 1.5px solid; border-radius: 12px; padding: 12px; text-align: center; cursor: default; transition: transform 0.2s;
  &:hover { transform: scale(1.04); }
}
.heat-dept-name { font-size: 12px; font-weight: 600; margin-bottom: 4px; }
.heat-count { font-size: 26px; font-weight: 700; line-height: 1; }
.heat-label { font-size: 11px; opacity: 0.75; }
.legend-dot { display: inline-block; width: 12px; height: 12px; border-radius: 3px; margin-right: 2px; vertical-align: middle; }
@media (max-width: 1200px) {
  .main-grid { flex-direction: column; }
  .kpi-row { display: grid; grid-template-columns: repeat(2,1fr); }
}
</style>
