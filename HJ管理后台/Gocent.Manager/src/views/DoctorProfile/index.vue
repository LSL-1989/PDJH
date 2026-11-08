<template>
  <div class="app-container">
    <el-card>
      <div class="list-toolbar doctor-toolbar">
        <div class="list-toolbar__left">
          <el-input
            v-model="query.workNo"
            class="doctor-toolbar__search"
            placeholder="医生工号"
            clearable
            @keyup.enter="loadList"
          />
          <el-input
            v-model="query.name"
            class="doctor-toolbar__search"
            placeholder="医生姓名，支持模糊查询"
            clearable
            @keyup.enter="loadList"
          />
          <el-button type="primary" icon="el-icon-search" @click="loadList">搜索</el-button>
        </div>
        <div class="list-toolbar__right">
          <el-button type="primary" icon="el-icon-plus" @click="openAdd">新增医生</el-button>
          <el-button type="warning" icon="el-icon-connection" @click="showHisSyncDialog = true">对接 HIS 同步</el-button>
        </div>
      </div>
      <el-table v-loading="loading" :data="query.Data" border row-key="id">
        <el-table-column prop="id" label="ID" width="160" show-overflow-tooltip />
        <el-table-column prop="workNo" label="工号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="120" show-overflow-tooltip />
        <el-table-column label="科室" min-width="160" show-overflow-tooltip>
          <template #default="scope">{{ deptNames(scope.row) }}</template>
        </el-table-column>
        <el-table-column prop="title" label="职称" width="110" />
        <el-table-column label="展示状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.displayStatus === 0 ? 'info' : 'success'" size="small">
              {{ scope.row.displayStatus === 0 ? '冻结' : '启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="头像" width="72" align="center">
          <template #default="scope">
            <img
              class="doctor-avatar-thumb"
              :src="doctorAvatarSrc(scope.row.avatar)"
              alt=""
              @error="useDefaultAvatar"
            >
          </template>
        </el-table-column>
        <el-table-column prop="specialty" label="擅长" min-width="140" show-overflow-tooltip />
        <el-table-column prop="remarks" label="简介" min-width="140" show-overflow-tooltip />
        <table-action-column :count="3">
          <template #default="{ row }">
            <table-action-icon icon="el-icon-date" tip="查看排班" @click="openDoctorSchedule(row)" />
            <table-action-icon icon="el-icon-edit" tip="编辑" @click="openEdit(row)" />
            <table-action-icon icon="el-icon-delete" tip="删除" type="danger" @click="removeDoctor(row)" />
          </template>
        </table-action-column>
      </el-table>
      <el-pagination
        background
        class="doctor-pagination"
        :current-page="query.Index"
        :page-sizes="[5, 10, 20, 30]"
        :page-size="query.Page"
        layout="total, sizes, prev, pager, next, jumper"
        :total="query.Totle"
        @size-change="onSizeChange"
        @current-change="onPageChange"
      />
    </el-card>

    <form-drawer
      :before-close="beforeClose"
      :title="dialogTitle"
      v-model="dialogVisible"
      size="520px"
      @close="resetForm"
    >
      <el-form ref="doctorForm" :model="form" :rules="formRules" label-width="88px" size="small">
        <el-form-item label="医生工号" prop="workNo">
          <el-input v-model="form.workNo" placeholder="工号" :disabled="dialogMode === 'edit'" clearable />
        </el-form-item>
        <el-form-item label="医生姓名" prop="name">
          <el-input v-model="form.name" placeholder="姓名" />
        </el-form-item>
        <el-form-item label="医生职称" prop="title">
          <el-select v-model="form.title" filterable placeholder="请选择职称" style="width:100%;">
            <el-option v-for="item in titleOptions" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="可用于叫号器登录" clearable maxlength="20" />
        </el-form-item>
        <el-form-item label="所属科室" prop="deptIds">
          <el-select v-model="form.deptIds" multiple filterable clearable placeholder="可多选叶子科室" style="width:100%;">
            <el-option v-for="d in leafDeptList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
          <div style="font-size:12px;color:#909399;margin-top:4px;">仅可选无下级节点的科室；未绑定时将出现在首页待处理</div>
        </el-form-item>
        <el-form-item label="医生擅长" prop="specialty">
          <el-input v-model="form.specialty" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
        </el-form-item>
        <el-form-item label="医生简介" prop="remarks">
          <el-input v-model="form.remarks" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
        </el-form-item>
        <el-form-item label="医生照片">
          <el-upload
            ref="upload"
            class="doctor-avatar-upload"
            :class="{ 'doctor-avatar-upload--full': avatarHasImage }"
            list-type="picture-card"
            action=""
            accept="image/*"
            :multiple="false"
            :auto-upload="false"
            :on-change="onAvatarChange"
            :on-remove="onAvatarRemove"
            :on-exceed="onAvatarExceed"
            :limit="1"
            :file-list="filelist"
          >
            <div class="doctor-avatar-upload__placeholder">
              <img :src="defaultDoctorAvatar" alt="默认医生照片">
              <span>默认照片</span>
            </div>
          </el-upload>
          <div class="doctor-avatar-upload__hint">
            {{ avatarHasImage ? '每位医生仅可上传 1 张头像，删除后恢复默认照片' : '未配置照片时使用默认头像，点击可上传替换' }}
          </div>
        </el-form-item>
        <el-form-item label="展示状态" prop="displayStatus">
          <el-radio-group v-model="form.displayStatus">
            <el-radio :label="1" border>启用</el-radio>
            <el-radio :label="0" border>冻结</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="beforeClose">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
      </template>
    </form-drawer>

    <his-sync-dialog
      v-model="showHisSyncDialog"
      entity-type="doctor"
      title="HIS 业务数据对接与同步策略 (医生档案)"
      count-unit="名医生"
      @synced="loadList"
    />

    <form-drawer
      :title="scheduleDoctorName ? `出诊排班 · ${scheduleDoctorName}` : '出诊排班'"
      v-model="scheduleDrawerVisible"
      size="440px"
    >
      <div v-if="scheduleLoading" v-loading="true" class="schedule-empty" />
      <div v-else-if="scheduleLoadError" class="schedule-empty">{{ scheduleLoadError }}</div>
      <div v-else-if="!scheduleRows.length" class="schedule-empty">无排班</div>
      <div v-else>
        <div v-for="group in scheduleGroups" :key="group.date" class="schedule-day">
          <div class="schedule-day__date">{{ group.date }}</div>
          <div v-for="item in group.items" :key="item.id || item.shiftType" class="schedule-plan">
            <div class="schedule-plan__title">{{ shiftLabel(item.shiftType) }}<template v-if="item.stopFlag === 1"> 停诊</template></div>
            <template v-if="isStopped(item)">
              <div>{{ item.remark || '停诊，无需诊室和号源' }}</div>
            </template>
            <template v-else>
              <div>诊室：{{ roomName(item.officeRoomId) || '未指定' }}</div>
              <div>号源：{{ item.maxQuota ? item.maxQuota : '不限' }}</div>
              <div v-if="item.remark">备注：{{ item.remark }}</div>
            </template>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="scheduleDrawerVisible = false">关闭</el-button>
      </template>
    </form-drawer>
  </div>
</template>

<script>
import {
  getDoctorPageList,
  getDoctorByWorkNo,
  saveDoctor,
  deleteDoctor,
  uploadDoctorAvatar
} from '@/api/doctor'
import { getAllDepartment } from '@/api/department'
import { getDoctorTitles } from '@/api/doctorTitle'
import { listDoctorSchedules } from '@/api/doctorSchedule'
import { getShiftTypes } from '@/api/shiftType'
import { getAllOfficeRooms } from '@/api/officeRoom'
import { getApiErrorMessage } from '@/utils/request'
import { normalizeDepartmentList } from '@/utils/department'
import { resolveBackendStaticUrl } from '@/utils/staticUrl'
import defaultDoctorAvatar from '@/assets/doctor-default-avatar.png'
import HisSyncDialog from '@/components/HisSyncDialog'

const emptyForm = () => ({
  id: '',
  workNo: '',
  name: '',
  phone: '',
  title: '',
  deptId: '',
  deptIds: [],
  specialty: '',
  remarks: '',
  avatar: '',
  displayStatus: 1
})

export default {
  name: 'DoctorProfile',
  components: { HisSyncDialog },
  data() {
    return {
      loading: false,
      saving: false,
      dialogVisible: false,
      dialogTitle: '',
      dialogMode: 'add',
      showHisSyncDialog: false,
      deptList: [],
      titleList: [],
      scheduleDrawerVisible: false,
      scheduleLoading: false,
      scheduleLoadError: '',
      scheduleDoctorName: '',
      scheduleRows: [],
      shiftTypes: [],
      roomMap: {},
      query: {
        Totle: 0,
        PageCount: 0,
        Index: 1,
        Page: 10,
        Data: [],
        workNo: '',
        name: ''
      },
      form: emptyForm(),
      filelist: [],
      file: null,
      defaultDoctorAvatar,
      formRules: {
        workNo: [{ required: true, message: '医生工号不能为空', trigger: 'blur' }],
        name: [{ required: true, message: '医生姓名不能为空', trigger: 'blur' }],
        title: [{ required: true, message: '请选择医生职称', trigger: 'change' }],
        displayStatus: [{ required: true, message: '必须选择展示状态', trigger: 'change' }]
      }
    }
  },
  computed: {
    avatarHasImage() {
      return this.filelist.length >= 1
    },
    deptMap() {
      const map = {}
      this.deptList.forEach(d => {
        map[d.id] = d.name || d.id
      })
      return map
    },
    titleOptions() {
      const enabled = this.titleList.filter(item => item.enabled !== 0)
      const current = (this.form.title || '').trim()
      if (current && !enabled.some(item => item.name === current)) {
        return [{ id: `current-${current}`, name: current }, ...enabled]
      }
      return enabled
    },
    scheduleGroups() {
      const map = {}
      this.scheduleRows.forEach(row => {
        const date = row.scheduleDate
        if (!date) return
        if (!map[date]) map[date] = []
        map[date].push(row)
      })
      return Object.keys(map).sort().map(date => ({ date, items: map[date] }))
    },
    leafDeptList() {
      const parentIds = new Set(
        this.deptList
          .map(d => d.FatherID || d.fatherID)
          .filter(f => f && f !== '00000000-0000-0000-0000-000000000000')
      )
      return this.deptList.filter(d => {
        if (d.IsLeaf === false || d.isLeaf === false) return false
        if (d.HasChildren === true || d.hasChildren === true) return false
        return !parentIds.has(d.id)
      })
    }
  },
  mounted() {
    this.loadDepartments()
    this.loadTitles()
    this.loadList()
  },
  methods: {
    /** 已配置头像走后端静态路径，空值使用默认医生照片 */
    doctorAvatarSrc(avatar) {
      const path = (avatar || '').trim()
      return path ? resolveBackendStaticUrl(path) : this.defaultDoctorAvatar
    },
    useDefaultAvatar(event) {
      const img = event && event.target
      if (!img || img.dataset.fallback === '1') return
      img.dataset.fallback = '1'
      img.src = this.defaultDoctorAvatar
    },
    deptName(deptId) {
      return deptId ? (this.deptMap[deptId] || deptId) : '—'
    },
    deptNames(row) {
      const ids = row.deptIds && row.deptIds.length ? row.deptIds : (row.deptId ? [row.deptId] : [])
      if (!ids.length) return '未绑定'
      return ids.map(id => this.deptName(id)).join('、')
    },
    loadTitles() {
      getDoctorTitles()
        .then(res => {
          this.titleList = (res && res.data) || []
        })
        .catch(err => {
          this.titleList = []
          this.$message.error(getApiErrorMessage(err, '加载职称失败'))
        })
    },
    loadDepartments() {
      getAllDepartment()
        .then(res => {
          this.deptList = normalizeDepartmentList(res && res.data)
        })
        .catch(err => {
          this.deptList = []
          this.$message.error(getApiErrorMessage(err, '加载科室列表失败'))
        })
    },
    loadList() {
      this.loading = true
      getDoctorPageList({
        Index: this.query.Index,
        Page: this.query.Page,
        workNo: this.query.workNo,
        name: this.query.name
      })
        .then(res => {
          const data = (res && res.data) || {}
          this.query = {
            ...this.query,
            Data: data.Data || [],
            Totle: data.Totle || 0,
            PageCount: data.PageCount || 0,
            Index: data.Index || this.query.Index,
            Page: data.Page || this.query.Page
          }
        })
        .catch(() => {
          this.$message.error('加载医生列表失败')
        })
        .finally(() => {
          this.loading = false
        })
    },
    openAdd() {
      this.resetForm()
      this.dialogMode = 'add'
      this.dialogTitle = '新增医生'
      this.dialogVisible = true
    },
    openEdit(row) {
      this.dialogMode = 'edit'
      this.dialogTitle = '修改医生'
      this.form = {
        id: row.id,
        workNo: row.workNo || '',
        name: row.name || '',
        phone: row.phone || '',
        title: row.title || '',
        deptId: row.deptId || '',
        deptIds: (row.deptIds && row.deptIds.length) ? [...row.deptIds] : (row.deptId ? [row.deptId] : []),
        specialty: row.specialty || '',
        remarks: row.remarks || '',
        avatar: row.avatar || '',
        displayStatus: row.displayStatus === 0 ? 0 : 1
      }
      this.file = null
      this.filelist = row.avatar
        ? [{ uid: `doc-${row.id}`, name: row.name || 'photo', url: resolveBackendStaticUrl(row.avatar) }]
        : []
      this.dialogVisible = true
    },
    /** 右侧抽屉查看该医生全部排班；没有记录时只提示无排班 */
    openDoctorSchedule(row) {
      this.scheduleDoctorName = row.name || row.workNo || ''
      this.scheduleDrawerVisible = true
      this.scheduleLoading = true
      this.scheduleLoadError = ''
      this.scheduleRows = []
      const tasks = [listDoctorSchedules({ doctorId: row.id })]
      if (!this.shiftTypes.length) {
        tasks.push(getShiftTypes().then(res => {
          this.shiftTypes = (res && res.data) || []
        }).catch(() => {
          this.shiftTypes = []
        }))
      }
      if (!Object.keys(this.roomMap).length) {
        tasks.push(getAllOfficeRooms().then(res => {
          const map = {}
          ;((res && res.data) || []).forEach(room => {
            map[room.id] = room.name || room.id
          })
          this.roomMap = map
        }).catch(() => {
          this.roomMap = {}
        }))
      }
      Promise.all(tasks)
        .then(results => {
          const res = results[0]
          if (res && res.success === false) {
            this.scheduleLoadError = res.msg || '加载排班失败'
            this.scheduleRows = []
            return
          }
          this.scheduleRows = (res && res.data) || []
        })
        .catch(err => {
          this.scheduleLoadError = getApiErrorMessage(err, '加载排班失败')
          this.scheduleRows = []
        })
        .finally(() => {
          this.scheduleLoading = false
        })
    },
    shiftLabel(code) {
      const item = this.shiftTypes.find(row => row.code === code)
      if (item) return item.name
      return { AM: '上午班', PM: '下午班', FULL: '全天班', OFF: '停诊/休假' }[code] || code || '—'
    },
    isStopped(item) {
      if (!item) return false
      if (item.stopFlag === 1) return true
      const type = this.shiftTypes.find(row => row.code === item.shiftType)
      return !!(type && type.exclusiveFlag === 1) || item.shiftType === 'OFF'
    },
    roomName(id) {
      return id ? (this.roomMap[id] || '') : ''
    },
    removeDoctor(row) {
      this.$confirm(`确定删除医生「${row.name || row.workNo}」？`, '提示', { type: 'warning' })
        .then(() => deleteDoctor(row.id))
        .then(() => {
          this.$message.success('删除成功')
          this.loadList()
        })
        .catch(err => {
          if (err === 'cancel') return
          const msg = getApiErrorMessage(err, '删除失败')
          if (msg.includes('排班')) {
            this.$confirm(
              `${msg}。是否前往「出诊排班管理」清除该医生的排班？`,
              '无法删除',
              { type: 'warning', confirmButtonText: '前往排班管理', cancelButtonText: '取消' }
            )
              .then(() => {
                this.$router.push({
                  path: '/businessConfig/doctorSchedule',
                  query: {
                    doctorId: row.id,
                    doctorName: row.name || '',
                    deptId: row.deptId || undefined
                  }
                })
              })
              .catch(() => {})
            return
          }
          this.$message.error(msg)
        })
    },
    async checkWorkNoAvailable() {
      if (this.dialogMode !== 'add') return true
      const workNo = (this.form.workNo || '').trim()
      if (!workNo) return false
      try {
        await getDoctorByWorkNo(workNo)
        this.$message.warning('该工号已存在，请更换工号或直接编辑原记录')
        return false
      } catch (err) {
        const msg = getApiErrorMessage(err, '')
        if (msg.includes('未找到') || msg.includes('404')) {
          return true
        }
        this.$message.error(msg || '工号校验失败')
        return false
      }
    },
    submitForm() {
      this.$refs.doctorForm.validate(async valid => {
        if (!valid) return
        const ok = await this.checkWorkNoAvailable()
        if (!ok) return

        const needUpload = this.file && this.file.raw

        this.saving = true
        try {
          if (needUpload) {
            const fd = new FormData()
            fd.append('file', this.file.raw)
            const up = await uploadDoctorAvatar(fd)
            const path = up && up.data
            if (!path) {
              this.$message.error('图片上传失败')
              return
            }
            this.form.avatar = path
          }
          await saveDoctor({ ...this.form })
          this.$message.success('保存成功')
          this.dialogVisible = false
          this.loadList()
        } catch (err) {
          this.$message.error(getApiErrorMessage(err, '保存失败'))
        } finally {
          this.saving = false
        }
      })
    },
    onAvatarChange(file, fileList) {
      const latest = fileList.length ? fileList[fileList.length - 1] : file
      this.file = latest
      this.filelist = latest ? [latest] : []
    },
    onAvatarRemove() {
      this.file = null
      this.filelist = []
      this.form.avatar = ''
      if (this.$refs.upload) this.$refs.upload.clearFiles()
    },
    onAvatarExceed() {
      this.$message.warning('每位医生只能上传 1 张头像，请先删除当前照片')
    },
    beforeClose() {
      this.dialogVisible = false
      this.resetForm()
    },
    resetForm() {
      this.form = emptyForm()
      this.file = null
      this.filelist = []
      this.$nextTick(() => {
        if (this.$refs.upload) this.$refs.upload.clearFiles()
        if (this.$refs.doctorForm) this.$refs.doctorForm.clearValidate()
      })
    },
    onPageChange(page) {
      this.query.Index = page
      this.loadList()
    },
    onSizeChange(size) {
      this.query.Page = size
      this.query.Index = 1
      this.loadList()
    }
  }
}
</script>

<style scoped>
.doctor-toolbar {
  margin-bottom: 16px;
}
.doctor-toolbar__search {
  width: 220px;
}
.doctor-pagination {
  margin-top: 16px;
}
.doctor-avatar-thumb {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  vertical-align: middle;
  background: #e8f0fe;
}
.doctor-avatar-upload--full ::v-deep .el-upload--picture-card {
  display: none;
}
.doctor-avatar-upload__placeholder {
  position: relative;
  width: 100%;
  height: 100%;
}
.doctor-avatar-upload__placeholder img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.doctor-avatar-upload__placeholder span {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(25, 103, 210, 0.72);
  color: #fff;
  font-size: 12px;
  line-height: 22px;
}
.doctor-avatar-upload__hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
.schedule-empty {
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #80868b;
  font-size: 14px;
}
.schedule-day {
  margin-bottom: 14px;
}
.schedule-day__date {
  font-size: 13px;
  font-weight: 600;
  color: #202124;
  margin-bottom: 8px;
}
.schedule-plan {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #5f6368;
  line-height: 1.6;
}
.schedule-plan__title {
  font-weight: 600;
  color: #202124;
}
</style>
