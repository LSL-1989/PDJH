<template>
  <div class="audit-log">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon" style="background:linear-gradient(135deg,#5F6368,#202124);">
          <i class="el-icon-document" />
        </div>
        <div>
          <h2>审计日志查询</h2>
          <span class="header-sub">查询系统集成网关的所有接口调用记录，追溯异常根因</span>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-card list-toolbar">
      <div class="list-toolbar__left">
        <el-form :inline="true" :model="query" size="small">
          <el-form-item label="接口代码">
            <el-input v-model="query.endpointCode" placeholder="如: HIS_QUEUE_PUSH" clearable style="width:180px;" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width:120px;">
              <el-option label="✅ 成功" value="success" />
              <el-option label="❌ 失败" value="fail" />
            </el-select>
          </el-form-item>
          <el-form-item label="报文关键词">
            <el-input v-model="query.keyword" placeholder="请求、响应或错误信息" clearable style="width:200px;" />
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="query.dateRange"
              type="datetimerange"
              size="small"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width:360px;"
            />
          </el-form-item>
          <el-form-item label="耗时超过(ms)">
            <el-input-number v-model="query.minDurationMs" :min="0" :step="100" style="width:130px;" placeholder="0=不限" />
          </el-form-item>
        </el-form>
        <el-button type="primary" size="small" icon="el-icon-search" @click="search">查询</el-button>
      </div>
      <div class="list-toolbar__right">
        <el-button size="small" icon="el-icon-download" :loading="exporting" @click="exportCsv">导出 CSV</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom:16px;">
      <el-col :span="6">
        <div class="stat-card"><div class="stat-num">{{ stats.total }}</div><div class="stat-label">总调用次数</div></div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card success"><div class="stat-num">{{ stats.success }}</div><div class="stat-label">成功次数</div></div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card fail"><div class="stat-num">{{ stats.fail }}</div><div class="stat-label">失败次数</div></div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card warn"><div class="stat-num">{{ stats.avgDuration }}ms</div><div class="stat-label">平均耗时</div></div>
      </el-col>
    </el-row>

    <!-- 日志表格 -->
    <div class="table-wrap">
      <el-table v-loading="loading" :data="logs" size="small" border stripe height="420" @row-click="showDetail">
        <el-table-column prop="id" label="ID" width="75" align="center" />
        <el-table-column prop="endpointCode" label="接口代码" width="180">
          <template #default="{row}">
            <code style="font-size:12px;background:#F1F3F4;padding:2px 6px;border-radius:4px;">{{ row.endpointCode }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="connectionId" label="连接ID" width="130" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{row}">
            <el-tag :type="row.isSuccess ? 'success' : 'danger'" size="small" effect="dark">{{ row.isSuccess ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="statusCode" label="状态码" width="75" align="center">
          <template #default="{row}">
            <span :style="{ color: row.statusCode !== 200 ? '#EA4335' : '#34A853', fontWeight: '600' }">{{ row.statusCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时(ms)" width="90" align="center">
          <template #default="{row}">
            <span :style="{ color: row.durationMs > 2000 ? '#EA4335' : row.durationMs > 500 ? '#FBBC04' : '#34A853' }">
              {{ row.durationMs }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" show-overflow-tooltip min-width="180">
          <template #default="{row}">
            <span :style="{ color: row.errorMessage ? '#EA4335' : '#34A853' }">
              {{ row.errorMessage || '—' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="调用时间" width="165" align="center">
          <template #default="{row}">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <table-action-column :count="1">
          <template #default="{ row }">
            <table-action-icon icon="el-icon-view" tip="详情" @click.stop="showDetail(row)" />
          </template>
        </table-action-column>
      </el-table>

      <div style="display:flex;justify-content:flex-end;margin-top:12px;">
        <el-pagination
          :current-page="page.current"
          :page-sizes="[20, 50, 100]"
          :page-size="page.size"
          layout="total, sizes, prev, pager, next, jumper"
          :total="page.total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer title="日志详情" v-model="drawerVisible" direction="rtl" size="55%">
      <div v-if="selectedLog" class="drawer-content">
        <el-descriptions :column="2" border size="small" style="margin-bottom:16px;">
          <el-descriptions-item label="日志ID">{{ selectedLog.id }}</el-descriptions-item>
          <el-descriptions-item label="接口代码"><code>{{ selectedLog.endpointCode }}</code></el-descriptions-item>
          <el-descriptions-item label="连接ID">{{ selectedLog.connectionId }}</el-descriptions-item>
          <el-descriptions-item label="状态码">
            <span :style="{color: selectedLog.statusCode !== 200 ? '#EA4335' : '#34A853', fontWeight:'600'}">{{ selectedLog.statusCode }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="耗时">{{ selectedLog.durationMs }} ms</el-descriptions-item>
          <el-descriptions-item label="执行结果">
            <el-tag :type="selectedLog.isSuccess ? 'success' : 'danger'" size="small" effect="dark">{{ selectedLog.isSuccess ? '成功' : '失败' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="调用时间" :span="2">{{ formatTime(selectedLog.createTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="selectedLog.errorMessage" label="错误信息" :span="2">
            <span style="color:#EA4335;">{{ selectedLog.errorMessage }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="payload-section">
          <div class="payload-title">请求 Payload</div>
          <pre class="payload-pre">{{ formatJson(selectedLog.requestPayload) }}</pre>
        </div>
        <div class="payload-section">
          <div class="payload-title">响应 Payload</div>
          <pre class="payload-pre">{{ formatJson(selectedLog.responsePayload) }}</pre>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
/**
 * 外部网关审计查询页
 * 消费者：集成网关管理 / 外部网关审计查询
 * 依赖：@/api/auditLog（Query 分页与全量统计，Export 导出筛选结果）
 */
import { queryAuditLog, exportAuditLog } from '@/api/auditLog'

export default {
  name: 'AuditLog',
  data() {
    return {
      loading: false,
      exporting: false,
      drawerVisible: false,
      selectedLog: null,
      query: { endpointCode: '', status: '', keyword: '', dateRange: null, minDurationMs: 0 },
      page: { current: 1, size: 20, total: 0 },
      logs: [],
      stats: { total: 0, success: 0, fail: 0, avgDuration: 0 }
    }
  },
  created() { this.loadLogs() },
  methods: {
    buildBody() {
      return {
        endpointCode: this.query.endpointCode || null,
        isSuccess: this.query.status === 'success' ? true : (this.query.status === 'fail' ? false : null),
        keyword: this.query.keyword || null,
        startTime: this.query.dateRange?.[0] || null,
        endTime: this.query.dateRange?.[1] || null,
        minDurationMs: this.query.minDurationMs || 0,
        page: this.page.current,
        size: this.page.size
      }
    },
    search() {
      this.page.current = 1
      this.loadLogs()
    },
    loadLogs() {
      this.loading = true
      queryAuditLog(this.buildBody()).then(res => {
        if (res.code !== 200) {
          this.logs = []
          this.page.total = 0
          this.stats = { total: 0, success: 0, fail: 0, avgDuration: 0 }
          this.$message.error(res.msg || '审计日志加载失败')
          return
        }
        const stats = res.data?.stats || {}
        this.logs = res.data?.list || []
        this.page.total = res.data?.total || 0
        this.stats = {
          total: stats.total || 0,
          success: stats.success || 0,
          fail: stats.fail || 0,
          avgDuration: stats.avgDuration || 0
        }
      }).catch(err => {
        this.logs = []
        this.page.total = 0
        this.stats = { total: 0, success: 0, fail: 0, avgDuration: 0 }
        this.$message.error(err.message || '审计日志加载失败')
      }).finally(() => { this.loading = false })
    },
    showDetail(row) { this.selectedLog = row; this.drawerVisible = true },
    handleSizeChange(size) { this.page.size = size; this.page.current = 1; this.loadLogs() },
    handleCurrentChange(page) { this.page.current = page; this.loadLogs() },
    formatTime(t) { if (!t) return '—'; const d = new Date(t); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}` },
    formatJson(s) { try { return JSON.stringify(JSON.parse(s), null, 2) } catch (e) { return s || '（无）' } },
    exportCsv() {
      this.exporting = true
      exportAuditLog(this.buildBody()).then(res => {
        if (res.code !== 200 || !res.data?.csv) {
          this.$message.error(res.msg || '导出失败')
          return
        }
        const blob = new Blob([res.data.csv], { type: 'text/csv;charset=utf-8;' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `审计日志_${new Date().toLocaleDateString('zh-CN').replace(/\//g, '-')}.csv`
        a.click()
        URL.revokeObjectURL(url)
        if (res.data.truncated) {
          this.$message.warning(`已导出前 ${res.data.count} 条，共命中 ${res.data.total} 条`)
        } else {
          this.$message.success(`已导出 ${res.data.count} 条`)
        }
      }).catch(err => {
        this.$message.error(err.message || '导出失败')
      }).finally(() => { this.exporting = false })
    }
  }
}
</script>

<style lang="scss" scoped>
.audit-log { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  .header-left { display: flex; align-items: center; gap: 16px; }
  .header-icon { width: 52px; height: 52px; border-radius: 14px; display: flex; justify-content: center; align-items: center; i { font-size: 24px; color: white; } }
  h2 { margin: 0; font-size: 22px; font-weight: 700; color: #202124; }
  .header-sub { font-size: 13px; color: #80868B; margin-top: 3px; display: block; }
  .header-actions { display: flex; gap: 8px; }
}
.filter-card { background: #FFF; border-radius: 10px; padding: 16px 20px 6px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.stat-card { background: #FFF; border-radius: 12px; padding: 16px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.04); border-left: 4px solid #DADCE0;
  &.success { border-left-color: #34A853; .stat-num { color: #34A853; } }
  &.fail { border-left-color: #EA4335; .stat-num { color: #EA4335; } }
  &.warn { border-left-color: #FBBC04; .stat-num { color: #FBBC04; } }
}
.stat-num { font-size: 28px; font-weight: 700; color: #202124; }
.stat-label { font-size: 13px; color: #80868B; margin-top: 4px; }
.table-wrap { background: #FFF; border-radius: 12px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.drawer-content { padding: 20px; }
.payload-section { margin-bottom: 16px; }
.payload-title { font-size: 13px; font-weight: 600; color: #5F6368; margin-bottom: 8px; }
.payload-pre { background: #F8F9FA; border: 1px solid #E8EAED; border-radius: 8px; padding: 12px; font-size: 12px; font-family: 'Courier New', monospace; overflow-x: auto; white-space: pre-wrap; max-height: 250px; overflow-y: auto; line-height: 1.6; color: #202124; }
</style>
