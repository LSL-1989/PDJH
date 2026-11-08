<template>
  <div class="app-container">
    <div class="filter-container list-toolbar">
      <div class="list-toolbar__left">
        <el-select v-model="listQuery.logType" placeholder="业务日志分类" clearable class="filter-item" style="width: 200px;">
          <el-option label="语音播报 (VOICE_BROADCAST)" value="VOICE_BROADCAST" />
          <el-option label="医生叫号 (DOCTOR_CALL)" value="DOCTOR_CALL" />
          <el-option label="消息外推 (MSG_PUSH)" value="MSG_PUSH" />
          <el-option label="系统维护 (SYS_MAINTENANCE)" value="SYS_MAINTENANCE" />
          <el-option label="规则变更 (RULE_CHANGE)" value="RULE_CHANGE" />
        </el-select>

        <el-input v-model="listQuery.keyword" placeholder="操作人或处理信息关键字..." style="width: 250px;" class="filter-item" @keyup.enter="handleFilter" />

        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />

        <el-button class="filter-item" type="primary" icon="el-icon-search" @click="handleFilter">
          查询
        </el-button>
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
      <el-table-column label="ID" prop="id" sortable align="center" width="80" />
      <el-table-column label="发生时间" width="180" align="center">
        <template #default="{row}">
          <span>{{ formatDateTime(row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="日志分类" width="160" align="center">
        <template #default="{row}">
          <el-tag :type="getTypeTag(row.logType)">
            {{ getTypeName(row.logType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作者/对象" width="180" align="center">
        <template #default="{row}">
          <span class="font-bold">{{ row.operator }}</span>
        </template>
      </el-table-column>
      <el-table-column label="事件动作概要" min-width="250">
        <template #default="{row}">
          <span>{{ row.actionDesc }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{row}">
          <el-tag :type="row.isSuccess ? 'success' : 'danger'">
            {{ row.isSuccess ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="查证" width="100" align="center">
        <template #default="{row}">
          <el-button text size="small" icon="el-icon-view" @click="viewPayload(row)">
            载荷快照
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="listQuery.pageIndex"
      v-model:limit="listQuery.pageSize"
      @pagination="getList"
    />

    <el-dialog v-model="dialogVisible" title="业务通信 Payload 快照" width="60%">
      <div v-if="currentLog" style="margin-bottom: 10px;">
        <p><strong>跟踪号:</strong> {{ currentLog.id }} | <strong>分类:</strong> {{ currentLog.logType }}</p>
        <p><strong>错误摘要:</strong> <span style="color: red;">{{ currentLog.errorMessage || '无' }}</span></p>
      </div>
      <pre style="background: #1e1e1e; color: #d4d4d4; padding: 15px; border-radius: 5px; overflow-x: auto; font-family: 'Consolas', monospace; font-size: 13px;">{{ formattedPayload }}</pre>
      <template #footer><span class="dialog-footer">
        <el-button @click="dialogVisible = false">关 闭</el-button>
        <el-button type="primary" @click="copyPayload">复制快照 JSON</el-button>
      </span></template>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { parseTime } from '@/utils'
import Pagination from '@/components/Pagination'

export default {
  name: 'SystemActionLog',
  components: { Pagination },
  data() {
    return {
      list: null,
      total: 0,
      listLoading: true,
      dateRange: [],
      listQuery: {
        pageIndex: 1,
        pageSize: 10,
        logType: undefined,
        keyword: undefined,
        startDate: undefined,
        endDate: undefined
      },
      dialogVisible: false,
      currentLog: null
    }
  },
  computed: {
    formattedPayload() {
      if (!this.currentLog || !this.currentLog.payload) return 'No Payload'
      try {
        const obj = JSON.parse(this.currentLog.payload)
        return JSON.stringify(obj, null, 2)
      } catch (e) {
        return this.currentLog.payload
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.listLoading = true

      if (this.dateRange && this.dateRange.length === 2) {
        this.listQuery.startDate = this.dateRange[0]
        this.listQuery.endDate = this.dateRange[1]
      } else {
        this.listQuery.startDate = undefined
        this.listQuery.endDate = undefined
      }

      request({
        url: '/api/SystemActionLog/Query',
        method: 'post',
        data: this.listQuery
      }).then(response => {
        this.list = response.data.data
        this.total = response.data.TotalCount || 0
        this.listLoading = false
      }).catch(() => {
        this.listLoading = false
      })
    },
    handleFilter() {
      this.listQuery.pageIndex = 1
      this.getList()
    },
    /** 列表时间列：统一为 yyyy-MM-dd HH:mm:ss；空值显示 — */
    formatDateTime(t) {
      if (t == null || t === '') return '—'
      const formatted = parseTime(t, '{y}-{m}-{d} {h}:{i}:{s}')
      if (formatted && !formatted.includes('NaN')) return formatted
      const text = String(t).replace('T', ' ')
      return text.length >= 19 ? text.substring(0, 19) : text
    },
    getTypeTag(type) {
      if (type === 'VOICE_BROADCAST') return 'warning'
      if (type === 'DOCTOR_CALL') return 'primary'
      if (type === 'MSG_PUSH') return 'success'
      return 'info'
    },
    getTypeName(type) {
      if (type === 'VOICE_BROADCAST') return '外放语音 (VOICE)'
      if (type === 'DOCTOR_CALL') return '业务叫号 (CALL)'
      if (type === 'MSG_PUSH') return '集成推送 (PUSH)'
      return type
    },
    viewPayload(row) {
      this.currentLog = row
      this.dialogVisible = true
    },
    copyPayload() {
      if (!navigator.clipboard) {
        this.$message.error('浏览器不支持剪贴板API')
        return
      }
      navigator.clipboard.writeText(this.formattedPayload).then(() => {
        this.$message.success('已复制到剪贴板')
      })
    }
  }
}
</script>

<style scoped>
.font-bold {
  font-weight: 600;
  color: #303133;
}
</style>
