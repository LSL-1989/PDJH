<template>
  <div class="app-container patient-flow-page">
    <div class="filter-container list-toolbar">
      <div class="list-toolbar__left">
        <el-select
          v-model="listQuery.eventType"
          placeholder="事件类型"
          clearable
          class="filter-item"
          style="width: 160px;"
        >
          <el-option
            v-for="item in eventOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-input
          v-model="listQuery.keyword"
          placeholder="患者/排队号/操作人"
          clearable
          class="filter-item"
          style="width: 220px;"
          @keyup.enter="handleFilter"
        />
        <el-input
          v-model="listQuery.patientCode"
          placeholder="患者编号精确"
          clearable
          class="filter-item"
          style="width: 160px;"
          @keyup.enter="handleFilter"
        />
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          class="filter-item"
        />
        <el-button type="primary" icon="el-icon-search" class="filter-item" @click="handleFilter">
          查询
        </el-button>
        <el-button class="filter-item" @click="handleReset">重置</el-button>
      </div>
    </div>

    <el-table
      v-loading="listLoading"
      :data="list"
      border
      fit
      highlight-current-row
      style="width: 100%;"
    >
      <el-table-column label="时间" width="180" align="center">
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="事件" width="120" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="eventTagType(row.eventType)">{{ row.eventLabel || row.eventType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排队号" prop="showNo" width="100" align="center" />
      <el-table-column label="患者" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.patientName || '—' }}</span>
          <span v-if="row.patientCode" style="color:#909399; margin-left:6px;">{{ row.patientCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="排队单" prop="callListId" width="90" align="center" />
      <el-table-column label="来源" width="110" align="center">
        <template #default="{ row }">
          {{ sourceLabel(row) }}
        </template>
      </el-table-column>
      <el-table-column label="操作人" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ operatorText(row) }}
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ remarkText(row) }}
        </template>
      </el-table-column>
      <el-table-column label="轨迹" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" icon="el-icon-time" @click="openTimeline(row)">
            时间轴
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="listQuery.pageindex"
      v-model:limit="listQuery.pagesize"
      @pagination="getList"
    />

    <!-- 右侧抽屉：节点时间轴 -->
    <el-drawer
      v-model="drawerVisible"
      :title="drawerTitle"
      direction="rtl"
      size="420px"
      class="patient-flow-drawer"
    >
      <div v-loading="timelineLoading" class="timeline-wrap">
        <el-empty v-if="!timelineLoading && timeline.length === 0" description="暂无流程节点" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="node in timeline"
            :key="node.id"
            :timestamp="formatTime(node.createTime)"
            placement="top"
            :type="timelineItemType(node.eventType)"
            :color="timelineColor(node.eventType)"
          >
            <div class="timeline-node">
              <div class="timeline-node__title">{{ node.eventLabel || node.eventType }}</div>
              <div class="timeline-node__meta">
                <span v-if="node.showNo">号 {{ node.showNo }}</span>
                <span v-if="sourceLabel(node) !== '—'"> · {{ sourceLabel(node) }}</span>
                <span v-if="operatorText(node) !== '—'"> · {{ operatorText(node) }}</span>
              </div>
              <div v-if="remarkText(node) !== '—'" class="timeline-node__remark">{{ remarkText(node) }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
  </div>
</template>

<script>
/**
 * 患者全流程操作日志：列表 + 右侧时间轴抽屉
 * 覆盖签到/叫号/完成/复诊/过号/取药链路及护士干预
 */
import Pagination from '@/components/Pagination'
import { parseTime } from '@/utils'
import { queryPatientFlowLog, timelineByCallList, timelineByPatient } from '@/api/patientFlowLog'

const EVENT_OPTIONS = [
  { value: 'SIGN_IN', label: '门诊签到' },
  { value: 'CALL', label: '医生叫号' },
  { value: 'COMPLETE', label: '就诊完成' },
  { value: 'REVISIT', label: '复诊' },
  { value: 'SKIP', label: '过号' },
  { value: 'PHARMACY_SIGN_IN', label: '取药签到' },
  { value: 'PHARMACY_READY', label: '配药完成' },
  { value: 'PHARMACY_CALL', label: '取药叫号' },
  { value: 'PHARMACY_COMPLETE', label: '取药完成' },
  { value: 'PHARMACY_SKIP', label: '取药过号' },
  { value: 'NURSE_MANUAL_SIGN_IN', label: '护士代签到' },
  { value: 'NURSE_REVIVE', label: '过号复活' },
  { value: 'NURSE_CUT_IN', label: '手动插队' },
  { value: 'NURSE_SWAP', label: '转诊' },
  { value: 'NURSE_FREEZE', label: '冻结' },
  { value: 'NURSE_UNFREEZE', label: '解冻' },
  { value: 'NURSE_REFUND', label: 'HIS退号' },
  { value: 'NURSE_BIND_ROOM', label: '补绑诊室' }
]

export default {
  name: 'PatientFlowLog',
  components: { Pagination },
  data() {
    return {
      eventOptions: EVENT_OPTIONS,
      list: [],
      total: 0,
      listLoading: false,
      dateRange: [],
      listQuery: {
        pageindex: 1,
        pagesize: 10,
        keyword: '',
        eventType: '',
        patientCode: ''
      },
      drawerVisible: false,
      drawerTitle: '患者操作时间轴',
      timelineLoading: false,
      timeline: []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    handleFilter() {
      this.listQuery.pageindex = 1
      this.getList()
    },
    handleReset() {
      this.listQuery = {
        pageindex: 1,
        pagesize: 10,
        keyword: '',
        eventType: '',
        patientCode: ''
      }
      this.dateRange = []
      this.getList()
    },
    buildQuery() {
      const q = {
        pageindex: this.listQuery.pageindex,
        pagesize: this.listQuery.pagesize
      }
      if (this.listQuery.keyword) q.keyword = this.listQuery.keyword.trim()
      if (this.listQuery.eventType) q.eventType = this.listQuery.eventType
      if (this.listQuery.patientCode) q.patientCode = this.listQuery.patientCode.trim()
      if (this.dateRange && this.dateRange.length === 2) {
        q.startTime = this.dateRange[0]
        q.endTime = this.dateRange[1] + ' 23:59:59'
      }
      return q
    },
    getList() {
      this.listLoading = true
      queryPatientFlowLog(this.buildQuery())
        .then((res) => {
          const inner = res.data || {}
          this.list = Array.isArray(inner.data) ? inner.data : []
          this.total = (inner.pageInfo && inner.pageInfo.totalCount) || 0
        })
        .catch(() => {
          this.list = []
          this.total = 0
        })
        .finally(() => {
          this.listLoading = false
        })
    },
    openTimeline(row) {
      this.drawerVisible = true
      this.timeline = []
      this.timelineLoading = true
      const name = row.patientName || row.patientCode || ''
      this.drawerTitle = name ? `操作时间轴 · ${name}` : '患者操作时间轴'

      const loader = row.callListId
        ? timelineByCallList(row.callListId)
        : timelineByPatient(row.patientCode)

      loader
        .then((res) => {
          this.timeline = Array.isArray(res.data) ? res.data : []
        })
        .catch(() => {
          this.timeline = []
          this.$message.error('加载时间轴失败')
        })
        .finally(() => {
          this.timelineLoading = false
        })
    },
    /** 列表与时间轴：统一为 yyyy-MM-dd HH:mm:ss；空值不展示 */
    formatTime(t) {
      if (t == null || t === '') return ''
      const formatted = parseTime(t, '{y}-{m}-{d} {h}:{i}:{s}')
      if (formatted && !formatted.includes('NaN')) return formatted
      const text = String(t).replace('T', ' ')
      return text.length >= 19 ? text.substring(0, 19) : text
    },
    /** 来源是操作端，不是操作人。优先用接口中文名 */
    sourceLabel(row) {
      if (!row) return '—'
      if (row.operatorSourceLabel) return row.operatorSourceLabel
      const map = {
        TERMINAL: '签到终端',
        DOCTOR: '医生站',
        NURSE: '护士站',
        PHARMACY: '药房',
        LAB: '检验',
        SYS: '系统'
      }
      const code = (row.operatorSource || '').trim()
      return map[code.toUpperCase()] || code || '—'
    },
    /** 操作人：医生/护士为姓名（工号），设备为名称（终端号） */
    operatorText(row) {
      if (!row) return '—'
      return row.operatorDisplay || row.operatorName || row.operatorNo || '—'
    },
    /** 把 state=1 这类备注显示成结果句；已是中文则原样 */
    remarkText(row) {
      if (!row || row.remark == null || String(row.remark).trim() === '') return '—'
      const text = String(row.remark).trim()
      const state = text.match(/^state\s*=\s*(\d+)$/i)
      if (!state) {
        const fail = text.match(/^(?:fail|failed|error|失败)\s*[:=]\s*(.+)$/i)
        if (fail) return `失败，原因：${fail[1].trim()}`
        const upper = text.toUpperCase()
        if (upper === 'TIMEOUT' || upper === 'TIME_OUT') return '操作超时'
        if (upper === 'FAIL' || upper === 'FAILED' || upper === 'ERROR') return '操作失败'
        return text
      }
      const n = Number(state[1])
      const event = (row.eventType || '').toUpperCase()
      const specific = {
        CALL: { 1: '叫号成功，患者开始就诊' },
        PHARMACY_CALL: { 1: '叫号成功，请患者到窗口取药' },
        LAB_CALL: { 1: '叫号成功，患者开始采血' },
        COMPLETE: { 4: '就诊完成' },
        PHARMACY_COMPLETE: { 4: '取药完成' },
        LAB_COMPLETE: { 4: '采血完成' },
        SKIP: { 6: '已过号' },
        PHARMACY_SKIP: { 6: '取药已过号' },
        LAB_SKIP: { 6: '检验已过号' }
      }
      if (specific[event] && specific[event][n]) return specific[event][n]
      const generic = { 0: '成功，已回到等候', 1: '叫号成功', 4: '办理完成', 6: '已过号', 7: '尚未签到' }
      return generic[n] || '操作已完成'
    },
    eventTagType(type) {
      if (!type) return 'info'
      if (type.includes('COMPLETE') || type === 'SIGN_IN' || type === 'PHARMACY_SIGN_IN') return 'success'
      if (type.includes('SKIP') || type.includes('REFUND') || type.includes('FREEZE')) return 'danger'
      if (type.includes('CALL') || type === 'REVISIT') return 'warning'
      if (type.startsWith('NURSE_')) return ''
      return 'info'
    },
    timelineItemType(type) {
      return this.eventTagType(type) === 'danger' ? 'danger' : 'primary'
    },
    timelineColor(type) {
      const t = this.eventTagType(type)
      if (t === 'success') return '#67C23A'
      if (t === 'danger') return '#F56C6C'
      if (t === 'warning') return '#E6A23C'
      return '#409EFF'
    }
  }
}
</script>

<style scoped>
.timeline-wrap {
  padding: 0 8px 24px;
  min-height: 200px;
}
.timeline-node__title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}
.timeline-node__meta {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
.timeline-node__remark {
  margin-top: 6px;
  font-size: 12px;
  color: #606266;
  background: #f5f7fa;
  padding: 6px 8px;
  border-radius: 4px;
}
</style>
