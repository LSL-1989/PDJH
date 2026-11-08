<template>
  <div class="schedule-page">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon" style="background:linear-gradient(135deg,#1E88E5,#1565C0);">
          <i class="el-icon-date" />
        </div>
        <div>
          <h2>护士当班排班</h2>
          <span class="header-sub">诊区视图按区看一周，护士视图按人看一周。新增时可勾选星期，没勾的日期不写入</span>
        </div>
      </div>
    </div>

    <div class="filter-bar list-toolbar">
      <div class="list-toolbar__left">
        <div class="view-switch">
          <button type="button" :class="{ 'is-on': viewMode === 'zone' }" @click="viewMode = 'zone'">诊区周视图</button>
          <button type="button" :class="{ 'is-on': viewMode === 'nurse' }" @click="viewMode = 'nurse'">护士视图</button>
        </div>
        <el-select v-model="filterDept" placeholder="全部诊区" size="small" clearable filterable style="width:150px;">
          <el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
        <el-select v-model="filterUserNo" placeholder="全部护士" size="small" clearable filterable style="width:160px;">
          <el-option v-for="u in nurseUsers" :key="u.UserNo" :label="nurseOptionLabel(u)" :value="u.UserNo" />
        </el-select>
        <div class="range-nav">
          <el-button size="small" icon="el-icon-arrow-left" @click="shiftWeek(-1)" />
          <span>{{ weekRangeLabel }}</span>
          <el-button size="small" icon="el-icon-arrow-right" @click="shiftWeek(1)" />
          <el-button size="small" @click="goThisWeek">本周</el-button>
        </div>
      </div>
      <div class="list-toolbar__right">
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openAdd()">新增排班</el-button>
      </div>
    </div>

    <div v-loading="loading" class="schedule-table-wrap">
      <table class="schedule-table">
        <colgroup>
          <col class="name-col">
          <col v-for="day in weekDays" :key="'col-' + day.date" class="day-col">
        </colgroup>
        <thead>
          <tr>
            <th class="name-col">{{ viewMode === 'zone' ? '诊区' : '护士' }}</th>
            <th v-for="day in weekDays" :key="day.date" class="day-col" :class="{ today: day.isToday }">
              <div>{{ day.weekLabel }}</div>
              <div class="date-sub">{{ day.dayNum }}</div>
            </th>
          </tr>
        </thead>
        <tbody v-if="viewMode === 'zone'">
          <tr v-if="!loading && !zoneRows.length">
            <td :colspan="8" class="empty-row">本周暂无排班</td>
          </tr>
          <tr v-for="zone in zoneRows" :key="zone.id">
            <td class="name-col">
              <div class="name-cell">
                <div class="name-main">{{ zone.name }}</div>
              </div>
            </td>
            <td v-for="day in weekDays" :key="day.date" class="schedule-cell" @click="openZoneDay(zone, day)">
              <div v-if="zoneChips(zone.id, day.date).length" class="shift-stack">
                <div
                  v-for="chip in zoneChips(zone.id, day.date)"
                  :key="chip.shift"
                  class="shift-chip"
                  :class="shiftClass(chip.shift)"
                >
                  <span>{{ shiftLabel(chip.shift) }}</span>
                  <span class="shift-chip__meta">{{ chip.names }}</span>
                </div>
              </div>
              <div v-else class="shift-empty">
                <span>暂无排班</span>
                <i v-if="day.date >= todayStr" class="el-icon-plus" />
              </div>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr v-if="!loading && !nurseRows.length">
            <td :colspan="8" class="empty-row">本周暂无排班</td>
          </tr>
          <tr v-for="nurse in nurseRows" :key="nurse.userNo">
            <td class="name-col">
              <div class="name-cell">
                <div class="name-main">{{ nurse.name }}</div>
                <div v-if="nurse.deptName" class="name-sub">{{ nurse.deptName }}</div>
              </div>
            </td>
            <td v-for="day in weekDays" :key="day.date" class="schedule-cell" @click="openNurseDay(nurse, day)">
              <div v-if="nurseDay(nurse.userNo, day.date)" class="shift-stack">
                <div class="shift-chip" :class="shiftClass(nurseDay(nurse.userNo, day.date).shiftType)">
                  <span>{{ shiftLabel(nurseDay(nurse.userNo, day.date).shiftType) }}</span>
                  <span class="shift-chip__meta">{{ nurseDay(nurse.userNo, day.date).zoneName }}</span>
                </div>
              </div>
              <div v-else class="shift-empty">
                <span>休息</span>
                <i v-if="day.date >= todayStr" class="el-icon-plus" />
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="legend-bar">
      <span class="legend-item"><span class="dot dot-full" />全天</span>
      <span class="legend-item"><span class="dot dot-am" />上午</span>
      <span class="legend-item"><span class="dot dot-pm" />下午</span>
    </div>

    <form-drawer :title="editTitle" v-model="dialogVisible" size="520px">
      <el-form :model="form" label-width="100px" size="small">
        <el-form-item label="诊区" required>
          <el-select v-model="form.deptId" filterable placeholder="选择诊区或科室" style="width:100%;">
            <el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="护士" required>
          <el-select
            v-model="form.userNos"
            multiple
            collapse-tags
            filterable
            :disabled="form.lockedNurse"
            placeholder="可多选"
            style="width:100%;"
          >
            <el-option v-for="u in nurseUsers" :key="u.UserNo" :label="nurseOptionLabel(u)" :value="u.UserNo" />
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
            placement="bottom-end"
            :teleported="true"
            popper-class="nurse-schedule-range-popper"
            :popper-options="datePopperOptions"
            :disabled-date="disabledPastDate"
          />
          <div class="tip">从当日起有效。只写入下面勾选的星期</div>
        </el-form-item>
        <el-form-item v-if="!form.lockedDay" label="当班星期" required>
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
        <el-form-item label="班次" required>
          <el-radio-group v-model="form.shiftType">
            <el-radio label="FULL">全天</el-radio>
            <el-radio label="AM">上午</el-radio>
            <el-radio label="PM">下午</el-radio>
          </el-radio-group>
          <div class="tip">同一护士同一天只保留一个班次，再次保存会覆盖当天</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </form-drawer>

    <form-drawer :title="dayListTitle" v-model="dayDrawerVisible" size="640px">
      <div v-if="!dayList.length" class="empty-row">暂无排班</div>
      <el-table v-else :data="dayList" size="small" border>
        <el-table-column prop="userName" label="护士" min-width="100" />
        <el-table-column label="班次" width="90">
          <template #default="{ row }">{{ shiftLabel(row.shiftType) }}</template>
        </el-table-column>
        <table-action-column :count="2">
          <template #default="{ row }">
            <table-action-icon icon="el-icon-edit" tip="编辑" :disabled="dayListDate < todayStr" @click="editDayRow(row)" />
            <table-action-icon icon="el-icon-delete" tip="删除" type="danger" :disabled="dayListDate < todayStr" @click="deleteDayRow(row)" />
          </template>
        </table-action-column>
      </el-table>
      <template #footer>
        <el-button @click="dayDrawerVisible = false">关闭</el-button>
        <el-button v-if="dayListDate >= todayStr" type="primary" @click="addFromDayList">新增排班</el-button>
      </template>
    </form-drawer>
  </div>
</template>

<script>
/**
 * 护士当班周视图
 * 消费者：业务配置中心「护士当班排班」
 * 依赖 NurseSchedule、User/GetUsers（login_scene=NURSE）、科室列表
 * 诊区视图点格子看当天该诊区名单；护士视图点格子编辑该护士当天。不展示头像。
 */
import { ElMessage as Message } from 'element-plus'
import { getAllUser } from '@/api/user'
import { getAllDepartment } from '@/api/department'
import { normalizeDepartmentList } from '@/utils/department'
import { listNurseSchedules, saveNurseSchedule, deleteNurseSchedule } from '@/api/nurseSchedule'
import { getApiErrorMessage } from '@/utils/request'

const WEEKDAY_OPTIONS = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 0, label: '周日' }
]
const SHIFT_ORDER = ['FULL', 'AM', 'PM']

function todayStr() {
  const d = new Date()
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

function mondayOf(date) {
  const value = new Date(date)
  const day = value.getDay()
  const diff = day === 0 ? -6 : 1 - day
  value.setDate(value.getDate() + diff)
  value.setHours(0, 0, 0, 0)
  return value
}

export default {
  name: 'NurseSchedule',
  data() {
    return {
      viewMode: 'zone',
      filterDept: '',
      filterUserNo: '',
      currentWeekStart: mondayOf(new Date()),
      todayStr: todayStr(),
      weekdayOptions: WEEKDAY_OPTIONS,
      loading: false,
      saving: false,
      rows: [],
      nurseUsers: [],
      deptList: [],
      dialogVisible: false,
      dayDrawerVisible: false,
      dayListDate: '',
      dayListDeptId: '',
      dayList: [],
      datePopperOptions: {
        strategy: 'fixed',
        modifiers: [
          { name: 'preventOverflow', options: { boundary: 'viewport', padding: 8, altAxis: true } },
          { name: 'flip', options: { fallbackPlacements: ['top-end', 'bottom-start', 'top-start'] } }
        ]
      },
      form: {
        deptId: '',
        userNos: [],
        dateRange: [],
        weekdays: [1, 2, 3, 4, 5],
        shiftType: 'FULL',
        lockedDay: false,
        lockedNurse: false
      }
    }
  },
  computed: {
    weekDays() {
      const labels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      return labels.map((weekLabel, index) => {
        const date = new Date(this.currentWeekStart)
        date.setDate(this.currentWeekStart.getDate() + index)
        const text = this.formatLocalDate(date)
        return {
          weekLabel,
          date: text,
          dayNum: date.getDate(),
          isToday: text === this.todayStr
        }
      })
    },
    weekRangeLabel() {
      const days = this.weekDays
      return `${days[0].date} ~ ${days[6].date}`
    },
    filteredRows() {
      return this.rows.filter(row => {
        if (this.filterDept && row.deptId !== this.filterDept) return false
        if (this.filterUserNo && row.userNo !== this.filterUserNo) return false
        return true
      })
    },
    zoneRows() {
      if (this.filterDept) {
        return [{ id: this.filterDept, name: this.deptName(this.filterDept) }]
      }
      const map = new Map()
      this.filteredRows.forEach(row => {
        if (row.deptId && !map.has(row.deptId)) map.set(row.deptId, row.zoneName || row.deptId)
      })
      return [...map.entries()]
        .map(([id, name]) => ({ id, name }))
        .sort((a, b) => String(a.name).localeCompare(String(b.name), 'zh'))
    },
    nurseRows() {
      const ids = []
      const push = userNo => {
        if (userNo && !ids.includes(userNo)) ids.push(userNo)
      }
      if (this.filterUserNo) push(this.filterUserNo)
      else this.filteredRows.forEach(row => push(row.userNo))
      return ids
        .map(userNo => this.nurseProfile(userNo))
        .sort((a, b) => String(a.name).localeCompare(String(b.name), 'zh'))
    },
    editTitle() {
      if (this.form.lockedDay) {
        const name = this.form.lockedNurse ? this.nurseName(this.form.userNos[0]) : ''
        const date = (this.form.dateRange && this.form.dateRange[0]) || ''
        return name ? `编辑排班 · ${name} ${date}` : `编辑排班 · ${date}`
      }
      return '新增护士排班'
    },
    dayListTitle() {
      const zone = this.deptName(this.dayListDeptId)
      return this.dayListDate ? `${this.dayListDate} ${zone}` : '当日排班'
    }
  },
  created() {
    this.loadNurses()
    this.loadDepts()
    this.loadList()
  },
  methods: {
    emptyForm() {
      return {
        deptId: '',
        userNos: [],
        dateRange: [],
        weekdays: [1, 2, 3, 4, 5],
        shiftType: 'FULL',
        lockedDay: false,
        lockedNurse: false
      }
    },
    shiftLabel(code) {
      const map = { AM: '上午', PM: '下午', FULL: '全天' }
      return map[code] || '全天'
    },
    shiftClass(code) {
      const map = { AM: 'shift-am', PM: 'shift-pm', FULL: 'shift-full' }
      return map[code] || 'shift-full'
    },
    weekdayText(value) {
      const item = WEEKDAY_OPTIONS.find(row => row.value === value)
      return item ? item.label : ''
    },
    deptName(id) {
      const dept = this.deptList.find(item => item.id === id)
      return dept ? dept.name : (id || '')
    },
    nurseOptionLabel(user) {
      const name = user.UserName || user.UserNo
      return `${name}（${user.UserNo}）`
    },
    nurseName(userNo) {
      const user = this.nurseUsers.find(item => item.UserNo === userNo)
      if (user && user.UserName) return user.UserName
      const row = this.rows.find(item => item.userNo === userNo)
      return (row && row.userName) || userNo || ''
    },
    nurseProfile(userNo) {
      const user = this.nurseUsers.find(item => item.UserNo === userNo)
      return {
        userNo,
        name: this.nurseName(userNo),
        deptName: user ? this.deptName(user.DeptID) : ''
      }
    },
    zoneChips(deptId, date) {
      const rows = this.filteredRows.filter(row => row.deptId === deptId && row.scheduleDate === date)
      return SHIFT_ORDER.map(shift => {
        const people = rows.filter(row => (row.shiftType || 'FULL') === shift)
        if (!people.length) return null
        return {
          shift,
          names: people.map(row => row.userName || row.userNo).join('、')
        }
      }).filter(Boolean)
    },
    nurseDay(userNo, date) {
      return this.rows.find(row => row.userNo === userNo && row.scheduleDate === date) || null
    },
    parseLocalDate(str) {
      const [year, month, day] = str.split('-').map(Number)
      return new Date(year, month - 1, day)
    },
    formatLocalDate(date) {
      const p = n => String(n).padStart(2, '0')
      return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}`
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
    disabledPastDate(date) {
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      return date.getTime() < today.getTime()
    },
    shiftWeek(delta) {
      const date = new Date(this.currentWeekStart)
      date.setDate(date.getDate() + delta * 7)
      this.currentWeekStart = date
      this.loadList()
    },
    goThisWeek() {
      this.currentWeekStart = mondayOf(new Date())
      this.loadList()
    },
    loadNurses() {
      getAllUser({ pageindex: 1, pagesize: 200, loginScene: 'NURSE' }).then(res => {
        const payload = (res && res.data) || {}
        this.nurseUsers = payload.data || payload.list || []
      }).catch(() => {
        this.nurseUsers = []
      })
    },
    loadDepts() {
      getAllDepartment().then(res => {
        this.deptList = normalizeDepartmentList((res && res.data) || [])
      }).catch(() => {
        this.deptList = []
      })
    },
    loadList() {
      const days = this.weekDays
      this.loading = true
      return listNurseSchedules({ startDate: days[0].date, endDate: days[6].date })
        .then(res => {
          if (res && res.success === false) {
            Message.error(res.msg || '加载失败')
            this.rows = []
            return
          }
          this.rows = (res && res.data) || []
          if (this.dayDrawerVisible) this.refreshDayList()
        })
        .catch(err => {
          Message.error(getApiErrorMessage(err, '加载失败'))
          this.rows = []
        })
        .finally(() => {
          this.loading = false
        })
    },
    warnPast(date) {
      if (date < this.todayStr) {
        Message.warning('过往日期不可排班')
        return true
      }
      return false
    },
    openAdd(preset) {
      const extra = preset || {}
      let weekdays = [1, 2, 3, 4, 5]
      const dateRange = extra.dateRange || []
      if (dateRange[0] && dateRange[0] === dateRange[1]) {
        weekdays = [this.parseLocalDate(dateRange[0]).getDay()]
      }
      this.form = {
        ...this.emptyForm(),
        deptId: extra.deptId || this.filterDept || '',
        userNos: extra.userNos || (this.filterUserNo ? [this.filterUserNo] : []),
        dateRange,
        weekdays: extra.weekdays || weekdays,
        shiftType: extra.shiftType || 'FULL',
        lockedDay: !!extra.lockedDay,
        lockedNurse: !!extra.lockedNurse
      }
      this.dialogVisible = true
    },
    openNurseDay(nurse, day) {
      if (this.warnPast(day.date)) return
      const row = this.nurseDay(nurse.userNo, day.date)
      this.openAdd({
        deptId: row ? row.deptId : (this.filterDept || ''),
        userNos: [nurse.userNo],
        dateRange: [day.date, day.date],
        shiftType: row ? (row.shiftType || 'FULL') : 'FULL',
        lockedDay: true,
        lockedNurse: true
      })
    },
    openZoneDay(zone, day) {
      this.dayListDeptId = zone.id
      this.dayListDate = day.date
      this.refreshDayList()
      this.dayDrawerVisible = true
    },
    refreshDayList() {
      this.dayList = this.rows.filter(row => {
        if (row.deptId !== this.dayListDeptId || row.scheduleDate !== this.dayListDate) return false
        if (this.filterUserNo && row.userNo !== this.filterUserNo) return false
        return true
      })
    },
    addFromDayList() {
      const date = this.dayListDate
      const deptId = this.dayListDeptId
      this.dayDrawerVisible = false
      this.openAdd({ deptId, dateRange: [date, date], lockedDay: true })
    },
    editDayRow(row) {
      this.dayDrawerVisible = false
      this.openAdd({
        deptId: row.deptId,
        userNos: [row.userNo],
        dateRange: [this.dayListDate, this.dayListDate],
        shiftType: row.shiftType || 'FULL',
        lockedDay: true,
        lockedNurse: true
      })
    },
    deleteDayRow(row) {
      const name = row.userName || row.userNo
      this.$confirm(`删除「${name}」${this.dayListDate} 的当班排班？`, '删除排班', { type: 'warning' })
        .then(() => deleteNurseSchedule(row.id))
        .then(res => {
          if (res && res.success === false) {
            Message.error(res.msg || '删除失败')
            return
          }
          Message.success('已删除')
          this.loadList()
        })
        .catch(err => {
          if (err !== 'cancel') Message.error(getApiErrorMessage(err, '删除失败'))
        })
    },
    toggleWeekday(value) {
      const list = (this.form.weekdays || []).slice()
      const index = list.indexOf(value)
      if (index >= 0) list.splice(index, 1)
      else list.push(value)
      this.form.weekdays = list
    },
    submit() {
      if (!this.form.deptId) {
        Message.warning('请选择诊区')
        return
      }
      if (!this.form.userNos.length) {
        Message.warning('请选择护士')
        return
      }
      if (!this.form.dateRange || this.form.dateRange.length !== 2) {
        Message.warning('请选择日期范围')
        return
      }
      if (!this.form.weekdays.length) {
        Message.warning('请选择当班星期')
        return
      }
      const [start, end] = this.form.dateRange
      if (start < this.todayStr || end < this.todayStr) {
        Message.warning('过往日期不可排班')
        return
      }
      const picked = new Set(this.form.weekdays)
      const dates = this.collectDateRange(start, end).filter(date => picked.has(this.parseLocalDate(date).getDay()))
      if (!dates.length) {
        Message.warning('日期范围内没有勾选的星期')
        return
      }
      this.saving = true
      saveNurseSchedule({
        userNos: this.form.userNos,
        deptId: this.form.deptId,
        scheduleDates: dates,
        shiftType: this.form.shiftType || 'FULL'
      }).then(res => {
        if (res && res.success === false) {
          Message.error(res.msg || '保存失败')
          return
        }
        Message.success('排班已保存')
        this.dialogVisible = false
        this.loadList()
      }).catch(err => {
        Message.error(getApiErrorMessage(err, '保存失败'))
      }).finally(() => {
        this.saving = false
      })
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
  col.name-col { width: 140px; }
  col.day-col { width: calc((100% - 140px) / 7); }
  th, td { border: 1px solid #F1F3F4; padding: 0; text-align: center; vertical-align: top; }
  th { background: #F8F9FA; font-size: 13px; font-weight: 600; color: #5F6368; padding: 10px 4px; }
  th.today { background: #E8F0FE; color: #1A73E8; }
  .date-sub { font-size: 12px; font-weight: 400; color: #80868B; }
}
.name-col { width: 140px; text-align: left; padding: 0 12px !important; vertical-align: middle; }
.day-col { width: calc((100% - 140px) / 7); }
.name-cell { padding: 10px 0; }
.name-main { font-size: 13px; font-weight: 600; color: #202124; }
.name-sub { font-size: 11px; color: #80868B; margin-top: 2px; }
.schedule-cell { width: calc((100% - 140px) / 7); min-height: 72px; cursor: pointer; padding: 6px 4px; text-align: left; overflow: hidden;
  &:hover { background: #F8F9FA; }
}
.shift-stack { display: flex; flex-direction: column; align-items: flex-start; gap: 4px; min-width: 0; max-width: 100%; }
.shift-chip { display: flex; flex-direction: column; align-items: flex-start; gap: 1px; width: fit-content; max-width: 100%; box-sizing: border-box; padding: 4px 8px; border-radius: 6px; font-size: 12px; font-weight: 600; line-height: 1.35; text-align: left; }
.shift-chip__meta { font-weight: 400; font-size: 11px; white-space: normal; overflow-wrap: anywhere; }
.shift-am { background: #E8F0FE; color: #1A73E8; }
.shift-pm { background: #FEF7E0; color: #B06000; }
.shift-full { background: #E6F4EA; color: #137333; }
.shift-empty { color: #BDC1C6; font-size: 12px; min-height: 52px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px;
  i { font-size: 14px; color: #1A73E8; }
}
.empty-row { padding: 28px 12px; color: #80868B; font-size: 13px; text-align: center; }
.legend-bar { display: flex; gap: 20px; margin-top: 12px; padding: 10px 16px; background: #FFF; border-radius: 8px; }
.legend-item { font-size: 12px; color: #5F6368; display: flex; align-items: center; gap: 6px; }
.dot { width: 12px; height: 12px; border-radius: 3px; display: inline-block; }
.dot-am { background: #1A73E8; }
.dot-pm { background: #FBBC04; }
.dot-full { background: #34A853; }
.locked-day { font-size: 13px; color: #202124; font-weight: 600; }
.weekday-picker { display: flex; flex-wrap: wrap; gap: 6px; }
.weekday-picker__item { border: 1px solid #DADCE0; background: #FFF; color: #5F6368; border-radius: 6px; padding: 4px 10px; font-size: 12px; cursor: pointer;
  &.is-on { border-color: #1A73E8; background: #E8F0FE; color: #1A73E8; font-weight: 600; }
}
.tip { font-size: 12px; color: #80868B; line-height: 1.5; margin-top: 4px; }
:deep(.el-date-editor.el-date-editor--daterange) {
  width: 100%;
  box-sizing: border-box;
  .el-range-separator { box-sizing: border-box; flex: 0 0 auto; width: auto; padding: 0 6px; }
  .el-range-input { flex: 1; width: 0; min-width: 0; }
}
</style>

<style lang="scss">
/* 面板挂到 body，需非 scoped。右侧抽屉里按输入框右缘向左展开，避免双月历超出屏幕 */
.nurse-schedule-range-popper.el-popper {
  max-width: calc(100vw - 16px);
}
</style>
