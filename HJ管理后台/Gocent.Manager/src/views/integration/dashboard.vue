<template>
  <div class="app-container">
    <h2>运维护航沙盘 (Integration Monitor)</h2>

    <div class="panel-group">
      <el-row :gutter="40">
        <el-col :span="8">
          <el-card shadow="hover" class="box-card">
            <template #header><div><span>网关总吞吐量 (Requests)</span></div></template>
            <div class="card-num">{{ dashboardData.totalTraffic || 0 }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="box-card">
            <template #header><div><span>活跃端点数 (Active)</span></div></template>
            <div class="card-num" style="color:#67C23A">{{ (dashboardData.endpointStats || []).length }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="box-card" style="border-left: 5px solid #F56C6C">
            <template #header><div><span>近期熔断/错误 (Errors)</span></div></template>
            <div class="card-num" style="color:#F56C6C">{{ (dashboardData.recentErrors || []).length }}</div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-row :gutter="20" style="margin-top:20px;">
      <!-- Echarts Bar Chart -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><div><span>端点调用成功率排布 (Endpoint Stats)</span></div></template>
          <!-- We fallback to simple progressive bars if echarts is complex, but echarts is standard in this plan -->
          <div style="height:350px">
            <el-table :data="dashboardData.endpointStats || []" height="100%">
              <el-table-column prop="endpointCode" label="端点编码" />
              <el-table-column prop="callCount" label="调用总数" />
              <el-table-column label="成功率">
                <template #default="{row}">
                  <el-progress :percentage="Math.round(row.successCount*100/row.callCount)" :status="row.successCount===row.callCount?'success':'exception'" />
                </template>
              </el-table-column>
              <el-table-column prop="avgDuration" label="平均耗时(ms)" />
            </el-table>
          </div>
        </el-card>
      </el-col>

      <!-- Recent Errors Alerts -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><div><span style="color:red">异常告警事件墙 (Alerts)</span></div></template>
          <el-table :data="dashboardData.recentErrors || []" size="small" style="width: 100%" height="350">
            <el-table-column prop="time" label="发生时间" width="160">
              <template #default="{row}">{{ formatTime(row.time) }}</template>
            </el-table-column>
            <el-table-column prop="endpointCode" label="端点" width="120" />
            <el-table-column prop="error" label="异常指纹" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getDashboardStats } from '@/api/monitor'

export default {
  name: 'IntegrationDashboard',
  data() {
    return {
      dashboardData: {}
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    formatTime(val) {
      if (!val) return ''
      return new Date(val).toLocaleString()
    },
    async fetchData() {
      try {
        const res = await getDashboardStats()
        this.dashboardData = res.data || {}
      } catch (e) {
        console.error('Failed to load dashboard stats', e)
      }
    }
  }
}
</script>

<style scoped>
.box-card {
  text-align: center;
}
.card-num {
  font-size: 38px;
  font-weight: bold;
  color: #409EFF;
}
</style>
