<template>
  <div class="medtech-appointment">

    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-title">
        <i class="el-icon-date" style="color:#1A73E8;font-size:22px;" />
        <div>
          <h2>医技科室预约时段管理</h2>
          <span class="header-sub">超声科 / 放射科（CT/MRI）分时段配额配置</span>
        </div>
      </div>
      <div class="header-actions list-toolbar list-toolbar--flat" style="margin-bottom:0;min-width:280px;">
        <div class="list-toolbar__left">
          <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadSlots">刷新</el-button>
        </div>
        <div class="list-toolbar__right">
          <el-button type="primary" size="small" icon="el-icon-plus" @click="openInitDialog">批量初始化时段</el-button>
        </div>
      </div>
    </div>

    <div class="main-layout">

      <!-- 左侧控制面板 -->
      <div class="side-panel">
        <div class="panel-card">
          <div class="panel-title"><i class="el-icon-office-building" /> 选择科室</div>
          <el-select v-model="selectedDept" placeholder="选择医技科室" style="width:100%;margin-bottom:16px;" @change="loadSlots">
            <el-option-group label="超声科">
              <el-option v-for="d in deptOptions.filter(x=>x.subType==='ULTRASOUND')" :key="d.id" :label="d.name" :value="d.id" />
            </el-option-group>
            <el-option-group label="放射科（CT/MRI）">
              <el-option v-for="d in deptOptions.filter(x=>x.subType==='RADIOLOGY')" :key="d.id" :label="d.name" :value="d.id" />
            </el-option-group>
          </el-select>

          <div class="panel-title" style="margin-top:8px;"><i class="el-icon-date" /> 选择日期</div>
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择查询日期"
            style="width:100%;"
            value-format="YYYY-MM-DD"
            first-day-of-week="1"
            @change="loadSlots"
          />

          <!-- 日期快捷 -->
          <div class="quick-dates">
            <div class="quick-date-item" :class="{active: isToday}" @click="setDate(0)">今天</div>
            <div class="quick-date-item" :class="{active: isTomorrow}" @click="setDate(1)">明天</div>
            <div class="quick-date-item" @click="setDate(2)">后天</div>
            <div class="quick-date-item" @click="setDate(7)">下周</div>
          </div>
        </div>

        <!-- 汇总数据 -->
        <div v-if="summary" class="panel-card summary-card">
          <div class="panel-title"><i class="el-icon-data-line" /> 今日统计</div>
          <div class="sum-row"><span>总配额</span><b>{{ summary.totalCapacity }}</b></div>
          <div class="sum-row"><span>已预约</span><b style="color:#1A73E8;">{{ summary.totalBooked }}</b></div>
          <div class="sum-row"><span>剩余名额</span><b :style="{ color: summary.remaining < 5 ? '#EA4335' : '#34A853' }">{{ summary.remaining }}</b></div>
          <el-progress
            :percentage="summary.totalCapacity > 0 ? Math.round(summary.totalBooked / summary.totalCapacity * 100) : 0"
            :color="summary.remaining < 5 ? '#EA4335' : '#1A73E8'"
            style="margin-top:12px;"
          />
        </div>
      </div>

      <!-- 右侧时段看板 -->
      <div class="slot-board">
        <div class="board-header">
          <span class="board-title">
            {{ currentDeptName || '请选择科室' }} · {{ selectedDate || '请选择日期' }}
          </span>
          <div style="display:flex;gap:8px;align-items:center;font-size:12px;color:#80868B;">
            <span><i class="el-icon-circle-check" style="color:#34A853;" /> 充足</span>
            <span><i class="el-icon-warning-outline" style="color:#FBBC04;" /> 紧张</span>
            <span><i class="el-icon-circle-close" style="color:#EA4335;" /> 已满</span>
          </div>
        </div>

        <div v-loading="loading" class="slot-grid">
          <div
            v-for="slot in slots"
            :key="slot.id"
            class="slot-cell"
            :class="getSlotClass(slot)"
          >
            <div class="slot-time">{{ slot.timeSlot }}</div>
            <div class="slot-count">
              <span class="slot-booked">{{ slot.booked }}</span>
              <span class="slot-sep">/</span>
              <span class="slot-cap">{{ slot.capacity }}</span>
            </div>
            <div class="slot-bar">
              <div class="slot-bar-fill" :style="{ width: getSlotPct(slot) + '%', background: getSlotColor(slot) }" />
            </div>
            <div class="slot-actions">
              <el-tooltip content="调整配额上限">
                <i class="el-icon-edit" @click="editSlot(slot)" />
              </el-tooltip>
              <el-tooltip :content="slot.isDisabled ? '点击启用' : '点击暂停'">
                <i
                  :class="slot.isDisabled ? 'el-icon-video-play' : 'el-icon-video-pause'"
                  :style="{ color: slot.isDisabled ? '#34A853' : '#EA4335' }"
                  @click="toggleSlot(slot)"
                />
              </el-tooltip>
            </div>
          </div>

          <!-- 空态 -->
          <div v-if="!loading && slots.length === 0" class="empty-slots">
            <i class="el-icon-calendar" style="font-size:48px;color:#DADCE0;" />
            <p style="color:#80868B;margin-top:12px;">该日期尚未配置时段，请点击「批量初始化时段」</p>
          </div>
        </div>

        <!-- 预约患者列表（点击时段后展开） -->
        <div v-if="selectedSlot" class="booking-list">
          <div class="booking-header">
            <span>{{ currentDeptName }} · {{ selectedDate }} {{ selectedSlot.timeSlot }} 已预约患者</span>
            <i class="el-icon-close" style="cursor:pointer;" @click="selectedSlot=null" />
          </div>
          <el-table :data="bookings" size="small" stripe>
            <el-table-column prop="patientCode" label="患者编号" width="110" />
            <el-table-column prop="patientName" label="姓名" width="80" />
            <el-table-column prop="showNo" label="取号" />
            <el-table-column prop="callTime" label="预约时间" width="150" />
            <el-table-column prop="sourceType" label="来源">
              <template #default="{row}">
                <el-tag size="small" :type="row.sourceType==='HEALTH_CHECKUP'?'success':'primary'" effect="plain">
                  {{ row.sourceType === 'HEALTH_CHECKUP' ? '体检' : '门诊' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 批量初始化弹框 -->
    <el-dialog title="批量初始化时段" v-model="showInitDialog" width="460px">
      <el-form :model="initForm" label-width="90px" size="small">
        <el-form-item label="科室">
          <el-select v-model="initForm.deptId" style="width:100%;">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="initForm.date" type="date" value-format="YYYY-MM-DD" style="width:100%;" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-select v-model="initForm.startTime" start="07:00" step="00:30" end="18:00" style="width:100%;" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-select v-model="initForm.endTime" start="07:00" step="00:30" end="18:00" :min-time="initForm.startTime" style="width:100%;" />
        </el-form-item>
        <el-form-item label="时间间隔">
          <el-select v-model="initForm.intervalMin" style="width:100%;">
            <el-option label="每15分钟" :value="15" />
            <el-option label="每20分钟" :value="20" />
            <el-option label="每30分钟" :value="30" />
            <el-option label="每60分钟" :value="60" />
          </el-select>
        </el-form-item>
        <el-form-item label="每段配额">
          <el-input-number v-model="initForm.capacity" :min="1" :max="20" style="width:100%;" />
        </el-form-item>
      </el-form>
      <template #footer><div>
        <el-button @click="showInitDialog=false">取消</el-button>
        <el-button type="primary" :loading="initLoading" @click="submitInit">初始化</el-button>
      </div></template>
    </el-dialog>

    <!-- 编辑配额弹框 -->
    <el-dialog title="调整时段配额" v-model="showEditDialog" width="360px">
      <el-form v-if="editForm" :model="editForm" label-width="90px" size="small">
        <el-form-item label="时段">
          <span style="font-weight:600;color:#1A73E8;">{{ editForm.timeSlot }}</span>
        </el-form-item>
        <el-form-item label="当前配额">
          <el-input-number v-model="editForm.capacity" :min="editForm.booked || 0" :max="50" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" placeholder="可选备注" maxlength="50" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer><div>
        <el-button @click="showEditDialog=false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </div></template>
    </el-dialog>

  </div>
</template>

<script>
import { getApiErrorMessage } from '@/utils/request'

export default {
  name: 'MedtechAppointment',
  data() {
    const today = new Date()
    const pad = n => String(n).padStart(2, '0')
    const todayStr = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`
    return {
      selectedDept: '',
      selectedDate: todayStr,
      loading: false,
      slots: [],
      summary: null,
      selectedSlot: null,
      bookings: [],
      showInitDialog: false,
      showEditDialog: false,
      initLoading: false,
      editForm: null,
      initForm: {
        deptId: '',
        date: todayStr,
        startTime: '08:00',
        endTime: '17:00',
        intervalMin: 30,
        capacity: 2
      },
      deptOptions: []
    }
  },
  computed: {
    currentDeptName() {
      const d = this.deptOptions.find(x => x.id === this.selectedDept)
      return d ? d.name : ''
    },
    isToday() {
      const t = new Date(); const pad = n => String(n).padStart(2, '0')
      return this.selectedDate === `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}`
    },
    isTomorrow() {
      const t = new Date(Date.now() + 86400000); const pad = n => String(n).padStart(2, '0')
      return this.selectedDate === `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}`
    }
  },
  mounted() {
    this.loadDeptOptions().then(() => {
      if (this.deptOptions.length > 0) {
        this.selectedDept = this.deptOptions[0].id
        this.loadSlots()
      }
    })
  },
  methods: {
    async loadDeptOptions() {
      try {
        const { default: request } = await import('@/utils/request')
        const res = await request({ url: '/api/Department/GetAllDepartment', method: 'post' })
        const list = res.data || []
        this.deptOptions = list
          .filter(d => (d.businessType || d.BusinessType) === 'MEDTECH' || (d.businessType || d.BusinessType) === 'LAB')
          .map(d => ({
            id: d.id || d.ID,
            name: d.name || d.Name,
            subType: d.medTechSubType || d.MedTechSubType || 'ULTRASOUND'
          }))
      } catch (e) {
        this.deptOptions = []
        this.$message.error('加载科室失败')
      }
    },
    setDate(offsetDays) {
      const d = new Date(Date.now() + offsetDays * 86400000)
      const pad = n => String(n).padStart(2, '0')
      this.selectedDate = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
      this.loadSlots()
    },
    async loadSlots() {
      if (!this.selectedDept || !this.selectedDate) return
      this.loading = true
      try {
        const { default: request } = await import('@/utils/request')
        const res = await request({ url: '/api/medtech/slots', method: 'get', params: { deptId: this.selectedDept, date: this.selectedDate }})
        this.slots = res.data?.slots || res.data || []
        this.summary = { totalCapacity: res.data?.totalCapacity || 0, totalBooked: res.data?.totalBooked || 0, remaining: res.data?.remaining || 0 }
      } catch (e) {
        this.slots = []
        this.summary = { totalCapacity: 0, totalBooked: 0, remaining: 0 }
        this.$message.error(getApiErrorMessage(e, '加载时段失败'))
      } finally {
        this.loading = false
      }
    },
    getSlotClass(slot) {
      if (slot.isDisabled) return 'slot-disabled'
      if (slot.booked >= slot.capacity) return 'slot-full'
      if (slot.booked >= slot.capacity * 0.8) return 'slot-tight'
      return 'slot-ok'
    },
    getSlotColor(slot) {
      if (slot.booked >= slot.capacity) return '#EA4335'
      if (slot.booked >= slot.capacity * 0.8) return '#FBBC04'
      return '#34A853'
    },
    getSlotPct(slot) {
      return slot.capacity > 0 ? Math.min(100, Math.round(slot.booked / slot.capacity * 100)) : 0
    },
    editSlot(slot) {
      this.editForm = { ...slot }
      this.showEditDialog = true
    },
    async submitEdit() {
      try {
        const { default: request } = await import('@/utils/request')
        await request({ url: `/api/medtech/slots/${this.editForm.id}`, method: 'put', data: { capacity: this.editForm.capacity, remark: this.editForm.remark }})
        this.$message.success(`时段 ${this.editForm.timeSlot} 已更新`)
        this.showEditDialog = false
        this.loadSlots()
      } catch (e) {
        this.$message.error(getApiErrorMessage(e, '保存失败'))
      }
    },
    openInitDialog() {
      this.initForm.deptId = this.selectedDept || (this.deptOptions[0] && this.deptOptions[0].id) || ''
      this.initForm.date = this.selectedDate || this.initForm.date
      this.showInitDialog = true
    },
    async toggleSlot(slot) {
      const enabled = slot.isDisabled === 1
      try {
        const { default: request } = await import('@/utils/request')
        await request({ url: `/api/medtech/slots/${slot.id}/enabled`, method: 'patch', data: { enabled }})
        this.$message.success(`时段 ${slot.timeSlot} 已${enabled ? '启用' : '暂停'}`)
        this.loadSlots()
      } catch (e) {
        this.$message.error(getApiErrorMessage(e, '操作失败'))
      }
    },
    async submitInit() {
      if (!this.initForm.deptId || !this.initForm.date) {
        this.$message.warning('请选择科室和日期')
        return
      }
      this.initLoading = true
      try {
        const { default: request } = await import('@/utils/request')
        const res = await request({ url: '/api/medtech/slots/init', method: 'post', data: this.initForm })
        this.$message.success(res.msg || '时段初始化成功！')
        this.selectedDept = this.initForm.deptId
        this.selectedDate = this.initForm.date
        this.showInitDialog = false
        this.loadSlots()
      } catch (e) {
        this.$message.error(getApiErrorMessage(e, '时段初始化失败'))
      } finally {
        this.initLoading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.medtech-appointment { padding: 0; }

.page-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;
  .header-title { display: flex; align-items: center; gap: 14px;
    h2 { margin: 0; font-size: 20px; font-weight: 700; color: #202124; }
    .header-sub { font-size: 13px; color: #80868B; }
  }
  .header-actions {
    display: flex;
    gap: 8px;
    .list-toolbar {
      margin-bottom: 0;
    }
  }
}

.main-layout { display: grid; grid-template-columns: 240px 1fr; gap: 20px; }

.side-panel { display: flex; flex-direction: column; gap: 16px; }

.panel-card {
  background: #FFF; border-radius: 12px; padding: 16px;
  box-shadow: 0 1px 4px rgba(60,64,67,.12);
  .panel-title { font-size: 13px; font-weight: 600; color: #5F6368; margin-bottom: 12px;
    display: flex; align-items: center; gap: 6px;
  }
}

.quick-dates {
  display: grid; grid-template-columns: 1fr 1fr; gap: 6px; margin-top: 12px;
  .quick-date-item {
    text-align: center; font-size: 12px; padding: 6px 0; border-radius: 6px;
    border: 1px solid #DADCE0; cursor: pointer; transition: all .2s; color: #5F6368;
    &:hover { border-color: #1A73E8; color: #1A73E8; }
    &.active { background: #E8F0FE; border-color: #1A73E8; color: #1A73E8; font-weight: 600; }
  }
}

.summary-card {
  .sum-row { display: flex; justify-content: space-between; align-items: center;
    padding: 6px 0; border-bottom: 1px solid #F8F9FA;
    font-size: 13px; color: #5F6368;
    b { font-size: 16px; font-weight: 700; color: #202124; }
    &:last-of-type { border-bottom: none; }
  }
}

.slot-board {
  background: #FFF; border-radius: 12px; padding: 20px;
  box-shadow: 0 1px 4px rgba(60,64,67,.12);
}

.board-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  .board-title { font-size: 15px; font-weight: 600; color: #202124; }
}

.slot-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 12px;
  min-height: 200px;
}

.slot-cell {
  border-radius: 10px; padding: 12px; cursor: pointer; position: relative;
  border: 2px solid transparent; transition: all .2s;
  &:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,.1); }
  .slot-time { font-size: 15px; font-weight: 700; color: #202124; }
  .slot-count { display: flex; align-items: baseline; gap: 2px; margin: 6px 0 4px;
    .slot-booked { font-size: 22px; font-weight: 700; }
    .slot-sep { font-size: 13px; color: #80868B; margin: 0 2px; }
    .slot-cap { font-size: 13px; color: #80868B; }
  }
  .slot-bar { height: 4px; border-radius: 2px; background: #F1F3F4; overflow: hidden;
    .slot-bar-fill { height: 100%; border-radius: 2px; transition: width .4s; }
  }
  .slot-actions {
    display: flex; gap: 8px; justify-content: flex-end; margin-top: 8px;
    opacity: 0; transition: opacity .2s; font-size: 14px; color: #80868B;
    i:hover { color: #1A73E8; cursor: pointer; }
  }
  &:hover .slot-actions { opacity: 1; }
  &.slot-ok { background: #E6F4EA; border-color: #34A853; .slot-booked { color: #34A853; } }
  &.slot-tight { background: #FEF7E0; border-color: #FBBC04; .slot-booked { color: #E37400; } }
  &.slot-full { background: #FCE8E6; border-color: #EA4335; .slot-booked { color: #EA4335; } }
  &.slot-disabled { background: #F1F3F4; border-color: #DADCE0; opacity: 0.6; }
}

.empty-slots {
  grid-column: 1 / -1; display: flex; flex-direction: column; align-items: center;
  justify-content: center; padding: 60px 0;
}

.booking-list {
  margin-top: 20px; border-top: 1px solid #F1F3F4; padding-top: 16px;
  .booking-header { display: flex; justify-content: space-between; align-items: center;
    font-size: 14px; font-weight: 600; color: #202124; margin-bottom: 12px;
  }
}
</style>
