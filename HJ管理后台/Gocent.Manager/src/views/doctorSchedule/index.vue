<template>
  <div class="schedule-page">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon" style="background:linear-gradient(135deg,#FBBC04,#F57C00);">
          <i class="el-icon-date" />
        </div>
        <div>
          <h2>医生出诊排班管理</h2>
          <span class="header-sub">医生周视图按人看一周，日历视图按日看全院。新增时可勾选星期，没勾的日期不写入</span>
        </div>
      </div>
    </div>

    <div class="filter-bar list-toolbar">
      <div class="list-toolbar__left">
        <div class="view-switch">
          <button type="button" :class="{ 'is-on': viewMode === 'doctor' }" @click="viewMode = 'doctor'">医生周视图</button>
          <button type="button" :class="{ 'is-on': viewMode === 'calendar' }" @click="viewMode = 'calendar'">日历视图</button>
        </div>
        <el-select v-model="filterDept" placeholder="全部科室" size="small" clearable style="width:150px;" @change="onDeptFilterChange">
          <el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
        <el-select
          v-model="filterDoctorId"
          placeholder="全部医生"
          size="small"
          filterable
          clearable
          style="width:160px;"
          @change="onDoctorFilterChange"
        >
          <el-option v-for="d in scheduledDoctorOptions" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
        <div v-if="viewMode === 'doctor'" class="range-nav">
          <el-button size="small" icon="el-icon-arrow-left" @click="prevWeek" />
          <span>{{ weekRangeLabel }}</span>
          <el-button size="small" icon="el-icon-arrow-right" @click="nextWeek" />
        </div>
        <div v-else class="range-nav">
          <el-button size="small" icon="el-icon-arrow-left" @click="shiftMonth(-1)" />
          <span>{{ monthLabel }}</span>
          <el-button size="small" icon="el-icon-arrow-right" @click="shiftMonth(1)" />
          <el-button size="small" @click="goThisMonth">今天</el-button>
        </div>
      </div>
      <div class="list-toolbar__right">
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openAddDialog()">新增排班</el-button>
        <el-button size="small" icon="el-icon-user" @click="goDoctorManage">医生管理</el-button>
      </div>
    </div>

    <div v-if="viewMode === 'doctor'" v-loading="loading" class="schedule-table-wrap">
      <table class="schedule-table">
        <colgroup>
          <col class="doctor-col">
          <col v-for="day in weekDays" :key="'col-' + day.date" class="day-col">
        </colgroup>
        <thead>
          <tr>
            <th class="doctor-col">医生</th>
            <th v-for="day in weekDays" :key="day.date" class="day-col" :class="{ 'today': day.isToday }">
              <div>{{ day.weekLabel }}</div>
              <div class="date-sub">{{ day.dayNum }}</div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!loading && !pagedDoctors.length">
            <td :colspan="weekDays.length + 1" class="empty-row">暂无已排班医生</td>
          </tr>
          <tr v-for="doc in pagedDoctors" :key="doc.id">
            <td class="doctor-col">
              <div class="doctor-cell">
                <img
                  v-if="doc.avatar"
                  class="doc-avatar doc-avatar--photo"
                  :src="avatarSrc(doc.avatar)"
                  alt=""
                  @error="doc.avatar = ''"
                >
                <div v-else class="doc-avatar" :style="{ background: getDocColor(doc.id) }">{{ doc.name ? doc.name.charAt(0) : '?' }}</div>
                <div class="doctor-cell__info">
                  <div class="doc-name">{{ doc.name }}</div>
                  <div class="doc-dept">{{ doctorMeta(doc) }}</div>
                  <el-button text size="small" class="clear-sched-btn" @click.stop="openClearDialog(doc)">清除排班</el-button>
                </div>
              </div>
            </td>
            <td v-for="day in weekDays" :key="day.date" class="schedule-cell" @click="onCellClick(doc, day)">
              <div v-if="cellSchedules(doc.id, day.date).length" class="shift-stack">
                <div
                  v-for="item in cellSchedules(doc.id, day.date)"
                  :key="item.id || item.shiftType"
                  class="shift-chip"
                  :class="shiftClass(item.shiftType)"
                >
                  <span :class="{ 'is-off': isStopped(item) }">{{ shiftChipTitle(item) }}</span>
                  <span v-if="chipMeta(item)" class="shift-chip__meta">{{ chipMeta(item) }}</span>
                </div>
              </div>
              <div v-else class="shift-empty">
                <span>暂无排班</span>
                <i v-if="day.date >= todayStr" class="el-icon-plus" />
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else v-loading="loading" class="cal-wrap">
      <div class="cal-grid">
        <div v-for="label in calendarWeekLabels" :key="label" class="cal-head">{{ label }}</div>
        <div
          v-for="cell in calendarCells"
          :key="cell.date"
          class="cal-cell"
          :class="{ 'is-out': !cell.inMonth, 'is-today': cell.isToday }"
          @click="openDayList(cell)"
        >
          <div class="cal-cell__top">
            <span class="cal-day">{{ cell.day }}</span>
          </div>
          <div v-if="cell.items.length" class="cal-list">
            <div
              v-for="item in cell.items"
              :key="item.id || (item.doctorId + item.shiftType)"
              class="shift-chip cal-chip"
              :class="shiftClass(item.shiftType)"
            >
              <span :class="{ 'is-off': isStopped(item) }">{{ shiftChipTitle(item) }} {{ doctorName(item.doctorId) }}</span>
              <span v-if="chipMeta(item)" class="shift-chip__meta">{{ chipMeta(item) }}</span>
            </div>
          </div>
          <div v-else class="cal-empty">暂无排班</div>
        </div>
      </div>
    </div>

    <el-pagination
      v-if="viewMode === 'doctor'"
      background
      class="schedule-pagination"
      :current-page="pageIndex"
      :page-sizes="[10, 20, 30, 50]"
      :page-size="pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="filteredDoctors.length"
      @size-change="onSizeChange"
      @current-change="onPageChange"
    />

    <div class="legend-bar">
      <span v-for="item in legendTypes" :key="item.code" class="legend-item">
        <span class="dot" :class="'dot-' + (item.tagStyle || 'default')" />{{ item.name }}
      </span>
    </div>

    <form-drawer :title="editTitle" v-model="dialogVisible" size="520px">
      <el-form :model="form" label-width="100px" size="small">
        <el-form-item label="医生" required>
          <el-select v-model="form.doctorId" placeholder="选择医生" filterable :disabled="form.lockedDay" style="width:100%;" @change="onScheduleDoctorChange">
            <el-option v-for="d in doctorList" :key="d.id" :label="`${d.name}（${d.deptName || d.deptId}）`" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.lockedDay" label="日期">
          <span class="locked-day">{{ form.dateRange[0] }} {{ weekdayText(form.weekdays[0]) }}</span>
        </el-form-item>
        <el-form-item v-else label="日期范围" required>
          <el-date-picker
            v-model="form.dateRange"
            type="daterange"
            size="small"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :disabled-date="disabledPastDate"
            style="width:100%;"
          />
          <div class="tip">从当日起有效。只写入下面勾选的星期，没勾到的日期保持原排班</div>
        </el-form-item>
        <el-form-item v-if="!form.lockedDay" label="出诊星期" required>
          <div class="weekday-picker">
            <button
              v-for="item in weekdayOptions"
              :key="item.value"
              type="button"
              class="weekday-picker__item"
              :class="{ 'is-on': form.weekdays.includes(item.value) }"
              @click="toggleWeekday(item.value)"
            >{{ item.label }}</button>
          </div>
        </el-form-item>
        <el-form-item label="班次类型" required>
          <el-select
            v-model="form.shiftTypes"
            multiple
            collapse-tags
            placeholder="可多选"
            style="width:100%;"
            @change="onShiftTypesChange"
          >
            <el-option
              v-for="item in selectableShiftTypes"
              :key="item.code"
              :label="shiftOptionLabel(item)"
              :value="item.code"
              :disabled="item.enabled === 0"
            />
          </el-select>
          <div class="tip">同一天可排多个午别。每个午别可单独标成停诊，停诊只填写缘由</div>
        </el-form-item>
        <div v-for="code in form.shiftTypes" :key="code" class="shift-block">
          <div class="shift-block__name">
            {{ shiftLabel(code) }}
            <span v-if="timeText(code)">{{ timeText(code) }}</span>
          </div>
          <el-form-item v-if="form.plans[code]" label="出诊状态">
            <el-radio-group v-model="form.plans[code].stopFlag">
              <el-radio :label="0">出诊</el-radio>
              <el-radio :label="1">停诊</el-radio>
            </el-radio-group>
          </el-form-item>
          <template v-if="form.plans[code] && form.plans[code].stopFlag == 1">
            <el-form-item label="停诊缘由">
              <el-input v-model="form.plans[code].remark" placeholder="如：外出会诊、病假、进修" />
            </el-form-item>
          </template>
          <template v-else-if="form.plans[code]">
            <el-form-item label="坐诊诊室">
              <el-select
                v-model="form.plans[code].officeRoomId"
                clearable
                filterable
                :disabled="!form.doctorId"
                :placeholder="roomPlaceholder"
                style="width:100%;"
              >
                <el-option v-for="r in scheduleRoomOptions" :key="r.id" :label="r.name" :value="r.id" />
              </el-select>
              <div class="tip">仅显示该医生所属科室已绑定的诊室</div>
            </el-form-item>
            <el-form-item label="号源限制">
              <el-input-number v-model="form.plans[code].maxQuota" :min="0" :max="200" />
              <span class="tip" style="display:inline;margin-left:8px;">0=不限</span>
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.plans[code].remark" placeholder="如：会诊/进修/特殊安排" />
            </el-form-item>
          </template>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSchedule">保存</el-button>
      </template>
    </form-drawer>

    <form-drawer :title="dayListTitle" v-model="dayDrawerVisible" size="720px">
      <div v-if="!dayList.length" class="empty-row">暂无排班</div>
      <el-table v-else :data="dayList" size="small" border>
        <el-table-column prop="doctorName" label="医生" min-width="88" />
        <el-table-column label="午别" min-width="100">
          <template #default="{ row }">{{ shiftChipTitle(row) }}</template>
        </el-table-column>
        <el-table-column label="诊室" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ isStopped(row) ? '—' : (roomName(row.officeRoomId) || '未指定') }}</template>
        </el-table-column>
        <el-table-column label="号源 / 缘由" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ isStopped(row) ? (row.remark || '停诊') : (row.maxQuota ? row.maxQuota : '不限') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button text size="small" :disabled="dayListDate < todayStr" @click="editDayRow(row)">编辑</el-button>
            <el-button text type="danger" size="small" :disabled="dayListDate < todayStr" @click="deleteDayRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="dayDrawerVisible = false">关闭</el-button>
        <el-button v-if="dayListDate >= todayStr" type="primary" @click="addFromDayList">新增排班</el-button>
      </template>
    </form-drawer>

    <el-dialog title="清除排班" v-model="clearVisible" width="640px" append-to-body>
      <p class="clear-text">请选择要清除「{{ clearTarget.name || '' }}」出诊排班的范围。</p>
      <template #footer>
        <div class="clear-footer">
          <el-button @click="clearVisible = false">取消</el-button>
          <el-button type="warning" plain :loading="clearing" @click="doClear('TODAY')">清除当日</el-button>
          <el-button type="warning" :loading="clearing" @click="doClear('FROM_TODAY')">清除当日及以后</el-button>
          <el-button type="danger" :loading="clearing" @click="doClear('ALL')">清除全部</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
/**
 * 医生出诊排班甘特页
 * 消费者：业务配置中心「出诊排班管理」
 * 依赖 DoctorSchedule、排班类型字典、医生档案、诊室
 * 医生周视图点格子编辑该医生当天；日历视图点格子列出当天排班。
 * 停诊按午别标记（stopFlag），不填诊室和号源，只记缘由。
 */
import { getApiErrorMessage } from '@/utils/request'
import { getAllDoctors } from '@/api/doctor'
import { getAllDepartment } from '@/api/department'
import {
  listDoctorSchedules,
  listScheduledDoctorIds,
  saveDoctorSchedule,
  deleteDoctorScheduleDay,
  clearDoctorSchedules
} from '@/api/doctorSchedule'
import { getShiftTypes } from '@/api/shiftType'
import { buildDepartmentNameMap, normalizeDepartmentList } from '@/utils/department'
import { getAllOfficeRooms } from '@/api/officeRoom'
import { resolveBackendStaticUrl } from '@/utils/staticUrl'

const emptyPlan = () => ({ officeRoomId: '', maxQuota: 0, remark: '', stopFlag: 0 })
const WEEKDAY_OPTIONS = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 0, label: '周日' }
]

export default {
  name: 'DoctorSchedule',
  data() {
    const today = new Date()
    return {
      loading: false,
      saving: false,
      clearing: false,
      dialogVisible: false,
      dayDrawerVisible: false,
      dayListDate: '',
      dayList: [],
      clearVisible: false,
      dialogMode: 'add',
      viewMode: 'doctor',
      filterDept: '',
      filterDoctorId: '',
      pendingDoctorName: '',
      pageIndex: 1,
      pageSize: 10,
      weekdayOptions: WEEKDAY_OPTIONS,
      calendarWeekLabels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      currentWeekStart: this.getMondayOf(today),
      calendarCursor: new Date(today.getFullYear(), today.getMonth(), 1),
      doctorList: [],
      filteredDoctors: [],
      scheduledDoctorIds: [],
      deptList: [],
      shiftTypes: [],
      scheduleMap: {},
      form: { doctorId: '', dateRange: [], weekdays: [1, 2, 3, 4, 5], shiftTypes: [], plans: {}, lockedDay: false },
      roomOptions: [],
      roomMap: {},
      clearTarget: { id: '', name: '' }
    }
  },
  computed: {
    todayStr() {
      return this.formatLocalDate(new Date())
    },
    weekDays() {
      const days = []
      const today = this.todayStr
      const weekLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      for (let i = 0; i < 7; i++) {
        const date = new Date(this.currentWeekStart)
        date.setDate(date.getDate() + i)
        const dateStr = this.formatLocalDate(date)
        days.push({
          date: dateStr,
          weekLabel: weekLabels[i],
          dateLabel: `${date.getMonth() + 1}/${date.getDate()}`,
          dayNum: date.getDate(),
          isToday: dateStr === today
        })
      }
      return days
    },
    weekRangeLabel() {
      const start = this.weekDays[0]
      const end = this.weekDays[6]
      return `${start.date} ~ ${end.date}`
    },
    monthLabel() {
      const date = this.calendarCursor
      return `${date.getFullYear()}年${date.getMonth() + 1}月`
    },
    queryRange() {
      if (this.viewMode === 'calendar') {
        const start = this.getMondayOf(new Date(this.calendarCursor.getFullYear(), this.calendarCursor.getMonth(), 1))
        const end = new Date(start)
        end.setDate(start.getDate() + 41)
        return { startDate: this.formatLocalDate(start), endDate: this.formatLocalDate(end) }
      }
      return { startDate: this.weekDays[0].date, endDate: this.weekDays[6].date }
    },
    calendarCells() {
      const month = this.calendarCursor.getMonth()
      const start = this.getMondayOf(new Date(this.calendarCursor.getFullYear(), month, 1))
      const cells = []
      for (let i = 0; i < 42; i++) {
        const date = new Date(start)
        date.setDate(start.getDate() + i)
        const dateStr = this.formatLocalDate(date)
        cells.push({
          date: dateStr,
          day: date.getDate(),
          inMonth: date.getMonth() === month,
          isToday: dateStr === this.todayStr,
          items: this.schedulesOn(dateStr)
        })
      }
      return cells
    },
    shiftTypeMap() {
      const map = {}
      this.shiftTypes.forEach(item => {
        map[item.code] = item
      })
      return map
    },
    legendTypes() {
      return this.shiftTypes.filter(item => item.enabled !== 0)
    },
    selectableShiftTypes() {
      return this.shiftTypes.filter(item => item.enabled !== 0 && !this.isExclusive(item.code))
    },
    editTitle() {
      if (this.form.lockedDay) {
        const doc = this.doctorList.find(item => item.id === this.form.doctorId)
        const date = (this.form.dateRange && this.form.dateRange[0]) || ''
        return `编辑排班 · ${doc ? doc.name : ''} ${date}`
      }
      return this.dialogMode === 'add' ? '新增排班' : '调整排班'
    },
    dayListTitle() {
      return this.dayListDate ? `${this.dayListDate} 排班` : '当日排班'
    },
    scheduledDoctorOptions() {
      const ids = new Set(this.scheduledDoctorIds)
      return this.doctorList.filter(doc => ids.has(doc.id) && this.matchesDept(doc))
    },
    pagedDoctors() {
      const start = (this.pageIndex - 1) * this.pageSize
      return this.filteredDoctors.slice(start, start + this.pageSize)
    },
    scheduleRoomOptions() {
      const doc = this.doctorList.find(item => item.id === this.form.doctorId)
      const deptIds = new Set(this.doctorDeptIds(doc))
      if (!deptIds.size) return []
      return this.roomOptions.filter(room => room.deptId && deptIds.has(room.deptId))
    },
    roomPlaceholder() {
      if (!this.form.doctorId) return '请先选择医生'
      if (!this.scheduleRoomOptions.length) return '所属科室暂无绑定诊室'
      return '请选择诊室'
    }
  },
  created() {
    this.applyRouteQuery()
    this.loadDepartments()
    this.loadDoctors()
    this.loadSchedules()
    this.loadRooms()
    this.loadShiftTypes()
  },
  watch: {
    currentWeekStart() {
      if (this.viewMode === 'doctor') this.loadSchedules()
    },
    calendarCursor() {
      if (this.viewMode === 'calendar') this.loadSchedules()
    },
    viewMode() { this.loadSchedules() }
  },
  methods: {
    loadRooms() {
      getAllOfficeRooms().then(res => {
        this.roomOptions = res.data || []
        const map = {}
        this.roomOptions.forEach(room => {
          map[room.id] = room.name || room.id
        })
        this.roomMap = map
      }).catch(() => {
        this.roomOptions = []
        this.roomMap = {}
      })
    },
    loadShiftTypes() {
      getShiftTypes().then(res => {
        if (res && res.success === false) {
          this.$message.error(res.msg || '加载排班类型失败')
          this.shiftTypes = []
          return
        }
        this.shiftTypes = (res && res.data) || []
      }).catch(err => {
        this.shiftTypes = []
        this.$message.error(getApiErrorMessage(err, '加载排班类型失败'))
      })
    },
    getMondayOf(date) {
      const value = new Date(date)
      const day = value.getDay()
      const diff = day === 0 ? -6 : 1 - day
      value.setDate(value.getDate() + diff)
      value.setHours(0, 0, 0, 0)
      return value
    },
    prevWeek() {
      const date = new Date(this.currentWeekStart)
      date.setDate(date.getDate() - 7)
      this.currentWeekStart = date
    },
    nextWeek() {
      const date = new Date(this.currentWeekStart)
      date.setDate(date.getDate() + 7)
      this.currentWeekStart = date
    },
    shiftMonth(delta) {
      const date = new Date(this.calendarCursor)
      date.setMonth(date.getMonth() + delta)
      this.calendarCursor = date
    },
    goThisMonth() {
      const today = new Date()
      this.calendarCursor = new Date(today.getFullYear(), today.getMonth(), 1)
      this.currentWeekStart = this.getMondayOf(today)
    },
    goDoctorManage() {
      this.$router.push('/hospitalInfo/DoctorProfile')
    },
    applyRouteQuery() {
      const { doctorId, doctorName, deptId } = this.$route.query || {}
      if (doctorId) this.filterDoctorId = String(doctorId)
      else if (doctorName) this.pendingDoctorName = String(doctorName)
      if (deptId) this.filterDept = String(deptId)
    },
    loadDepartments() {
      getAllDepartment()
        .then(res => {
          this.deptList = normalizeDepartmentList(res && res.data)
        })
        .catch(() => {
          this.deptList = []
        })
    },
    loadDoctors() {
      this.loading = true
      Promise.all([getAllDoctors(), getAllDepartment()])
        .then(([docRes, deptRes]) => {
          const deptMap = buildDepartmentNameMap((deptRes && deptRes.data) || [])
          this.deptList = normalizeDepartmentList((deptRes && deptRes.data) || []) || this.deptList
          this.doctorList = ((docRes && docRes.data) || []).map(doc => ({
            ...doc,
            deptName: deptMap[doc.deptId] || doc.deptId || ''
          }))
          if (this.pendingDoctorName && !this.filterDoctorId) {
            const hit = this.doctorList.find(doc => doc.name === this.pendingDoctorName)
            if (hit) this.filterDoctorId = hit.id
          }
          return this.loadScheduledIds(true)
        })
        .catch(() => {
          this.$message.error('加载医生列表失败')
          this.doctorList = []
          this.filteredDoctors = []
        })
        .finally(() => { this.loading = false })
    },
    loadScheduledIds(resetPage) {
      return listScheduledDoctorIds()
        .then(res => {
          this.scheduledDoctorIds = (res && res.data) || []
          this.filterDoctors(resetPage === true)
        })
        .catch(() => {
          this.scheduledDoctorIds = []
          this.filterDoctors(resetPage === true)
        })
    },
    onDeptFilterChange() {
      this.filterDoctors(true)
      this.loadSchedules()
    },
    onDoctorFilterChange() {
      this.filterDoctors(true)
    },
    loadSchedules() {
      const range = this.queryRange
      if (!range.startDate || !range.endDate) return
      listDoctorSchedules(range)
        .then(res => {
          const map = {}
          ;(res.data || []).forEach(item => {
            const date = this.scheduleDateText(item)
            const row = { ...item, scheduleDate: date }
            const key = `${row.doctorId}_${date}`
            if (!map[key]) map[key] = []
            map[key].push(row)
          })
          this.scheduleMap = map
          this.syncOpenPlan()
        })
        .catch(() => { this.scheduleMap = {} })
    },
    filterDoctors(resetPage) {
      const optionIds = new Set(this.scheduledDoctorOptions.map(doc => doc.id))
      if (this.filterDoctorId && !optionIds.has(this.filterDoctorId)) {
        this.filterDoctorId = ''
      }
      const scheduled = new Set(this.scheduledDoctorIds)
      this.filteredDoctors = this.doctorList.filter(doc => {
        if (!scheduled.has(doc.id)) return false
        const deptMatch = this.matchesDept(doc)
        const doctorMatch = !this.filterDoctorId || doc.id === this.filterDoctorId
        return deptMatch && doctorMatch
      })
      if (resetPage) this.pageIndex = 1
      const maxPage = Math.max(1, Math.ceil(this.filteredDoctors.length / this.pageSize) || 1)
      if (this.pageIndex > maxPage) this.pageIndex = maxPage
    },
    onSizeChange(size) {
      this.pageSize = size
      this.pageIndex = 1
    },
    onPageChange(page) {
      this.pageIndex = page
    },
    matchesDept(doc) {
      if (!this.filterDept) return true
      return this.doctorDeptIds(doc).includes(this.filterDept)
    },
    doctorMeta(doc) {
      return [doc.deptName || doc.deptId, doc.title].filter(Boolean).join(' ')
    },
    doctorName(id) {
      const doc = this.doctorList.find(item => item.id === id)
      return doc ? doc.name : ''
    },
    avatarSrc(path) {
      return resolveBackendStaticUrl(path)
    },
    isStopped(item) {
      if (!item) return false
      return item.stopFlag === 1 || item.stopFlag === '1' || this.isExclusive(item.shiftType)
    },
    shiftChipTitle(item) {
      const name = this.shiftLabel(item.shiftType)
      if (item.stopFlag === 1 || item.stopFlag === '1') return `${name} 停诊`
      return name
    },
    weekdayText(value) {
      const hit = this.weekdayOptions.find(item => item.value === value)
      return hit ? hit.label : ''
    },
    chipMeta(item) {
      if (this.isStopped(item)) return ''
      const parts = []
      const room = this.roomName(item.officeRoomId)
      if (room) parts.push(room)
      if (item.maxQuota) parts.push(`号源${item.maxQuota}`)
      return parts.join(' ')
    },
    scheduleDateText(item) {
      const value = item && item.scheduleDate
      if (!value) return ''
      if (typeof value === 'string') return value.length >= 10 ? value.slice(0, 10) : value
      if (Array.isArray(value) && value.length >= 3) {
        const month = String(value[1]).padStart(2, '0')
        const day = String(value[2]).padStart(2, '0')
        return `${value[0]}-${month}-${day}`
      }
      return String(value).slice(0, 10)
    },
    schedulesOn(date) {
      const allowed = new Set(this.filteredDoctors.map(doc => doc.id))
      const rows = []
      Object.keys(this.scheduleMap).forEach(key => {
        const mark = `_${date}`
        if (!key.endsWith(mark)) return
        const doctorId = key.slice(0, -mark.length)
        if (!allowed.has(doctorId)) return
        ;(this.scheduleMap[key] || []).forEach(item => rows.push(item))
      })
      return this.sortSchedules(rows)
    },
    cellSchedules(docId, date) {
      return this.sortSchedules(this.scheduleMap[`${docId}_${date}`] || [])
    },
    sortSchedules(list) {
      return [...list].sort((a, b) => {
        const left = this.shiftTypeMap[a.shiftType]
        const right = this.shiftTypeMap[b.shiftType]
        return ((left && left.sortOrder) || 0) - ((right && right.sortOrder) || 0)
      })
    },
    shiftLabel(code) {
      const item = this.shiftTypeMap[code]
      if (item) return item.name
      return { AM: '上午班', PM: '下午班', FULL: '全天班', OFF: '停诊/休假' }[code] || code || ''
    },
    shiftClass(code) {
      const item = this.shiftTypeMap[code]
      const style = item && item.tagStyle
        ? item.tagStyle
        : ({ AM: 'am', PM: 'pm', FULL: 'full', OFF: 'off' }[code] || 'default')
      return 'shift-' + style
    },
    timeText(code) {
      const item = this.shiftTypeMap[code]
      if (!item || !item.startTime || !item.endTime) return ''
      return `${item.startTime}-${item.endTime}`
    },
    shiftOptionLabel(item) {
      const time = item.startTime && item.endTime ? ` (${item.startTime}-${item.endTime})` : ''
      return `${item.name}${time}`
    },
    roomName(id) {
      return id ? (this.roomMap[id] || '') : ''
    },
    isExclusive(code) {
      const item = this.shiftTypeMap[code]
      return !!(item && item.exclusiveFlag === 1) || code === 'OFF'
    },
    doctorDeptIds(doc) {
      if (!doc) return []
      const ids = []
      if (Array.isArray(doc.deptIds)) {
        doc.deptIds.forEach(id => {
          if (id && !ids.includes(id)) ids.push(id)
        })
      }
      if (doc.deptId && !ids.includes(doc.deptId)) ids.push(doc.deptId)
      return ids
    },
    onScheduleDoctorChange() {
      const allowed = new Set(this.scheduleRoomOptions.map(room => room.id))
      const plans = { ...this.form.plans }
      let changed = false
      Object.keys(plans).forEach(code => {
        const plan = plans[code]
        if (plan && plan.officeRoomId && !allowed.has(plan.officeRoomId)) {
          plans[code] = { ...plan, officeRoomId: '' }
          changed = true
        }
      })
      if (changed) this.form.plans = plans
    },
    disabledPastDate(date) {
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      return date.getTime() < today.getTime()
    },
    onCellClick(doc, day) {
      if (day.date < this.todayStr) {
        this.$message.warning('过往日期不可排班')
        return
      }
      this.openDayEdit(doc, day.date)
    },
    openDayList(cell) {
      this.dayListDate = cell.date
      this.refreshDayList()
      this.dayDrawerVisible = true
    },
    refreshDayList() {
      if (!this.dayListDate) {
        this.dayList = []
        return
      }
      this.dayList = this.schedulesOn(this.dayListDate).map(item => ({
        ...item,
        doctorName: this.doctorName(item.doctorId)
      }))
    },
    addFromDayList() {
      const date = this.dayListDate
      this.dayDrawerVisible = false
      this.openAddDialog(this.filterDoctorId || '', [date, date])
    },
    editDayRow(row) {
      const doc = this.doctorList.find(item => item.id === row.doctorId) || {
        id: row.doctorId,
        name: row.doctorName || ''
      }
      this.dayDrawerVisible = false
      this.openDayEdit(doc, this.dayListDate)
    },
    deleteDayRow(row) {
      const name = row.doctorName || this.doctorName(row.doctorId)
      this.$confirm(`确定删除 ${name} 在 ${this.dayListDate} 的「${this.shiftChipTitle(row)}」？`, '提示', { type: 'warning' })
        .then(() => deleteDoctorScheduleDay({
          doctorId: row.doctorId,
          scheduleDate: this.dayListDate,
          shiftType: row.shiftType
        }))
        .then(res => {
          if (res && res.success === false) {
            this.$message.error(res.msg || '删除失败')
            return
          }
          this.$message.success('已删除该班次')
          this.loadScheduledIds(false)
          this.loadSchedules()
        })
        .catch(err => {
          if (err !== 'cancel') this.$message.error(getApiErrorMessage(err, '删除失败'))
        })
    },
    syncOpenPlan() {
      if (this.dayDrawerVisible) this.refreshDayList()
    },
    openAddDialog(doctorId, dateRange) {
      let weekdays = [1, 2, 3, 4, 5]
      if (dateRange && dateRange[0] && dateRange[0] === dateRange[1]) {
        weekdays = [this.parseLocalDate(dateRange[0]).getDay()]
      }
      this.form = {
        doctorId: doctorId || '',
        dateRange: dateRange || [],
        weekdays,
        shiftTypes: [],
        plans: {},
        lockedDay: false
      }
      this.dialogMode = 'add'
      this.dialogVisible = true
    },
    openDayEdit(doc, date) {
      const shiftTypes = []
      const plans = {}
      this.cellSchedules(doc.id, date).forEach(plan => {
        if (this.isExclusive(plan.shiftType)) return
        shiftTypes.push(plan.shiftType)
        plans[plan.shiftType] = {
          officeRoomId: plan.officeRoomId || '',
          maxQuota: plan.maxQuota || 0,
          remark: plan.remark || '',
          stopFlag: plan.stopFlag === 1 || plan.stopFlag === '1' ? 1 : 0
        }
      })
      this.form = {
        doctorId: doc.id,
        dateRange: [date, date],
        weekdays: [this.parseLocalDate(date).getDay()],
        shiftTypes,
        plans,
        lockedDay: true
      }
      this.dialogMode = shiftTypes.length ? 'edit' : 'add'
      this.dialogVisible = true
    },
    toggleWeekday(value) {
      const list = (this.form.weekdays || []).slice()
      const index = list.indexOf(value)
      if (index >= 0) list.splice(index, 1)
      else list.push(value)
      this.form.weekdays = list
    },
    onShiftTypesChange(codes) {
      const next = (Array.isArray(codes) ? codes : []).filter(code => !this.isExclusive(code))
      if (next.length !== (codes || []).length) this.form.shiftTypes = next
      const plans = { ...this.form.plans }
      next.forEach(code => {
        if (!plans[code]) plans[code] = emptyPlan()
      })
      this.form.plans = plans
    },
    parseLocalDate(str) {
      const [year, month, day] = str.split('-').map(Number)
      return new Date(year, month - 1, day)
    },
    formatLocalDate(date) {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    collectDateRange(start, end) {
      const dates = []
      const cursor = this.parseLocalDate(start)
      const endDate = this.parseLocalDate(end)
      while (cursor <= endDate) {
        dates.push(this.formatLocalDate(cursor))
        cursor.setDate(cursor.getDate() + 1)
      }
      return dates
    },
    saveSchedule() {
      if (!this.form.doctorId) {
        this.$message.warning('请选择医生')
        return
      }
      if (!this.form.dateRange || this.form.dateRange.length !== 2) {
        this.$message.warning('请选择日期范围')
        return
      }
      if (!this.form.weekdays || !this.form.weekdays.length) {
        this.$message.warning('请选择出诊星期')
        return
      }
      if (!this.form.shiftTypes.length) {
        this.$message.warning('请选择班次')
        return
      }
      const [start, end] = this.form.dateRange
      if (start < this.todayStr || end < this.todayStr) {
        this.$message.warning('过往日期不可排班')
        return
      }
      const picked = new Set(this.form.weekdays)
      const dates = this.collectDateRange(start, end).filter(date => picked.has(this.parseLocalDate(date).getDay()))
      if (!dates.length) {
        this.$message.warning('日期范围内没有勾选的星期')
        return
      }
      const doc = this.doctorList.find(item => item.id === this.form.doctorId)
      const shifts = this.form.shiftTypes.map(code => {
        const plan = this.form.plans[code] || emptyPlan()
        const stopped = Number(plan.stopFlag) === 1
        return {
          shiftType: code,
          stopFlag: stopped ? 1 : 0,
          maxQuota: stopped ? null : (plan.maxQuota || 0),
          remark: plan.remark || '',
          officeRoomId: stopped ? null : (plan.officeRoomId || null)
        }
      })
      this.saving = true
      saveDoctorSchedule({
        doctorId: this.form.doctorId,
        deptId: doc ? doc.deptId : null,
        scheduleDates: dates,
        shifts
      }).then(res => {
        if (res && res.success === false) {
          this.$message.error(res.msg || '保存排班失败')
          return
        }
        this.$message.success('排班已保存')
        this.dialogVisible = false
        this.loadScheduledIds(false)
        this.loadSchedules()
      }).catch(err => {
        this.$message.error(getApiErrorMessage(err, '保存排班失败'))
      }).finally(() => {
        this.saving = false
      })
    },
    openClearDialog(doc) {
      this.clearTarget = { id: doc.id, name: doc.name }
      this.clearVisible = true
    },
    doClear(scope) {
      if (!this.clearTarget.id || this.clearing) return
      this.clearing = true
      clearDoctorSchedules(this.clearTarget.id, scope)
        .then(res => {
          if (res && res.success === false) {
            this.$message.error(res.msg || '清除失败')
            return
          }
          const count = res && res.data != null ? res.data : 0
          const label = { TODAY: '当日', FROM_TODAY: '当日及以后', ALL: '全部' }[scope] || ''
          this.$message.success(count > 0 ? `已清除${label} ${count} 条排班` : '该范围内暂无排班')
          this.clearVisible = false
          this.loadScheduledIds(false)
          this.loadSchedules()
        })
        .catch(err => {
          this.$message.error(getApiErrorMessage(err, '清除失败'))
        })
        .finally(() => {
          this.clearing = false
        })
    },
    getDocColor(id) {
      const colors = ['#1A73E8', '#34A853', '#EA4335', '#FBBC04', '#9C27B0', '#FF6D00']
      const idx = id ? id.charCodeAt(id.length - 1) % colors.length : 0
      return colors[idx]
    }
  }
}
</script>

<style lang="scss" scoped>
.schedule-page { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  .header-left { display: flex; align-items: center; gap: 16px; }
  .header-icon { width: 52px; height: 52px; border-radius: 14px; display: flex; justify-content: center; align-items: center; i { font-size: 24px; color: white; } }
  h2 { margin: 0; font-size: 22px; font-weight: 700; color: #202124; }
  .header-sub { font-size: 13px; color: #80868B; margin-top: 3px; display: block; }
  .header-actions { display: flex; align-items: center; gap: 8px; }
  .week-label { font-size: 14px; font-weight: 600; color: #202124; padding: 0 4px; }
}
.filter-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; background: #FFF; padding: 12px 16px; border-radius: 10px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.view-switch { display: inline-flex; background: #F1F3F4; border-radius: 8px; padding: 3px; margin-right: 8px;
  button { border: 0; background: transparent; color: #5F6368; font-size: 13px; padding: 6px 12px; border-radius: 6px; cursor: pointer; }
  button.is-on { background: #FFF; color: #1A73E8; font-weight: 600; box-shadow: 0 1px 2px rgba(0,0,0,0.08); }
}
.range-nav { display: inline-flex; align-items: center; gap: 6px; margin-left: 4px;
  span { font-size: 13px; font-weight: 600; color: #202124; min-width: 168px; text-align: center; }
}
.schedule-table-wrap { background: #FFF; border-radius: 12px; overflow: auto; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.schedule-table { width: 100%; border-collapse: collapse; table-layout: fixed;
  col.doctor-col { width: 188px; }
  col.day-col { width: calc((100% - 188px) / 7); }
  th, td { border: 1px solid #F1F3F4; padding: 0; text-align: center; }
  th { background: #F8F9FA; font-size: 13px; font-weight: 600; color: #5F6368; padding: 10px 4px; }
  th.today { background: #E8F0FE; color: #1A73E8; }
  .date-sub { font-size: 12px; font-weight: 400; color: #80868B; }
}
.doctor-col { width: 188px; text-align: left; padding: 0 12px !important; }
.day-col { width: calc((100% - 188px) / 7); }
.doctor-cell { display: flex; align-items: center; gap: 8px; padding: 8px 0; }
.doc-avatar { width: 30px; height: 30px; border-radius: 50%; display: flex; justify-content: center; align-items: center; color: white; font-size: 14px; font-weight: 700; flex-shrink: 0; }
.doc-name { font-size: 13px; font-weight: 600; color: #202124; }
.doc-dept { font-size: 11px; color: #80868B; }
.doctor-cell__info { min-width: 0; }
.clear-sched-btn { padding: 0; font-size: 11px; color: #EA4335; }
.schedule-action-footer { display: flex; justify-content: flex-end; flex-wrap: wrap; gap: 8px; }
.schedule-cell { width: calc((100% - 188px) / 7); min-height: 72px; height: auto; cursor: pointer; transition: background 0.15s; padding: 6px 4px; text-align: left; overflow: hidden;
  &:hover { background: #F8F9FA; }
}
.shift-stack { display: flex; flex-direction: column; align-items: stretch; gap: 4px; min-width: 0; max-width: 100%; }
.shift-chip { display: flex; flex-direction: column; align-items: flex-start; gap: 1px; width: 100%; max-width: 100%; box-sizing: border-box; padding: 4px 8px; border-radius: 6px; font-size: 12px; font-weight: 600; line-height: 1.35; text-align: left; border: 0; }
.shift-chip__meta { font-weight: 400; font-size: 11px; opacity: 0.9; white-space: normal; overflow-wrap: anywhere; }
.is-off { text-decoration: line-through; }
.shift-tag { display: inline-block; padding: 4px 8px; border-radius: 6px; font-size: 12px; font-weight: 600; line-height: 1.3; }
.shift-am { background: #E8F0FE; color: #1A73E8; }
.shift-pm { background: #FEF7E0; color: #B06000; }
.shift-full { background: #E6F4EA; color: #137333; }
.shift-off { background: #FCE8E6; color: #B31412; }
.shift-default { background: #F1F3F4; color: #3C4043; }
.shift-empty { color: #BDC1C6; font-size: 12px; line-height: 1.4; min-height: 52px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px;
  i { font-size: 14px; color: #1A73E8; }
}
.doc-avatar--photo { object-fit: cover; }
.cal-wrap { background: #FFF; border-radius: 12px; overflow: auto; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.cal-grid { display: grid; grid-template-columns: repeat(7, minmax(120px, 1fr)); min-width: 860px; }
.cal-head { background: #F8F9FA; text-align: center; font-size: 13px; font-weight: 600; color: #5F6368; padding: 10px 4px; border-bottom: 1px solid #F1F3F4; }
.cal-cell { min-height: 128px; border-top: 1px solid #F1F3F4; border-left: 1px solid #F1F3F4; padding: 6px; background: #FFF; cursor: pointer;
  &:nth-child(7n) { border-right: 0; }
  &.is-out { background: #FAFBFC; .cal-day { color: #BDC1C6; } }
  &.is-today .cal-day { background: #1A73E8; color: #FFF; }
}
.cal-cell__top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.cal-day { width: 24px; height: 24px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600; color: #202124; }
.cal-add { border: 0; background: transparent; color: #1A73E8; cursor: pointer; font-size: 16px; line-height: 1; padding: 0 4px; }
.cal-list { display: flex; flex-direction: column; gap: 4px; max-height: 96px; overflow: auto; }
.cal-chip { cursor: pointer; }
.cal-empty { color: #BDC1C6; font-size: 12px; padding: 8px 2px; }
.locked-day { font-size: 13px; color: #202124; font-weight: 600; }
.weekday-picker { display: flex; flex-wrap: wrap; gap: 6px; }
.weekday-picker__item { border: 1px solid #DADCE0; background: #FFF; color: #5F6368; border-radius: 6px; padding: 4px 10px; font-size: 12px; cursor: pointer;
  &.is-on { border-color: #1A73E8; background: #E8F0FE; color: #1A73E8; font-weight: 600; }
}
.empty-row { padding: 28px 12px !important; color: #80868B; font-size: 13px; }
.schedule-pagination { margin-top: 12px; }
.legend-bar { display: flex; flex-wrap: wrap; gap: 20px; margin-top: 12px; padding: 10px 16px; background: #FFF; border-radius: 8px; }
.legend-item { font-size: 12px; color: #5F6368; display: flex; align-items: center; gap: 6px; }
.dot { width: 12px; height: 12px; border-radius: 3px; display: inline-block; }
.dot-am { background: #1A73E8; } .dot-pm { background: #FBBC04; }
.dot-full { background: #34A853; } .dot-off { background: #EA4335; }
.dot-default { background: #5F6368; }
.tip { font-size: 12px; color: #80868B; line-height: 1.5; margin-top: 4px; }
.shift-block { margin: 0 0 8px 100px; padding: 10px 12px 0; background: #F8F9FA; border-radius: 8px; }
.shift-block__name { font-size: 13px; font-weight: 600; color: #202124; margin-bottom: 8px;
  span { margin-left: 8px; font-weight: 400; color: #80868B; }
}
.plan-meta { display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px; font-size: 13px; color: #202124;
  span { display: inline-block; width: 48px; color: #80868B; }
}
.plan-card { background: #F8F9FA; border-radius: 8px; padding: 10px 12px; margin-bottom: 10px; }
.plan-card__title { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.plan-time { font-size: 12px; color: #80868B; }
.plan-row { font-size: 13px; color: #5F6368; line-height: 1.6; }
.plan-off-form { margin-top: 8px; }
.clear-text { margin: 0; font-size: 14px; color: #202124; line-height: 1.6; }
.clear-footer { display: flex; justify-content: flex-end; flex-wrap: wrap; gap: 8px; }
</style>
