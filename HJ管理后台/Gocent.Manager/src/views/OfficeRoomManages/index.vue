<template>
  <div class="app-container">
    <div class="list-toolbar">
      <div class="list-toolbar__left">
        <el-radio-group v-model="searchScenario" size="small">
          <el-radio-button label="">全场景</el-radio-button>
          <el-radio-button label="OUTPATIENT">门诊</el-radio-button>
          <el-radio-button label="PHARMACY">药房</el-radio-button>
          <el-radio-button label="LAB">检验</el-radio-button>
          <el-radio-button label="MEDTECH">医技</el-radio-button>
          <el-radio-button label="EXAM">体检</el-radio-button>
        </el-radio-group>
      </div>
      <div class="list-toolbar__right">
        <el-button type="primary" icon="el-icon-plus" round @click="showAddDialog">新增物理环境资源 (诊室/窗口)</el-button>
      </div>
    </div>

    <el-card>
      <el-table v-loading="loading" :data="filteredRoomList" style="width: 100%; border-radius: 8px;">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="name" label="设施名称 (诊室/窗口)" min-width="180">
          <template #default="scope">
            <span style="font-weight:600; color:#202124;">{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="所属科室(业务区)" min-width="150">
          <template #default="scope">
            <div style="font-weight: 500; color: #606266; margin-bottom: 4px;">{{ getDeptName(scope.row.deptId) || scope.row.deptId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="业务场景" width="100">
          <template #default="scope">
            <el-tag
              :type="getSceneType(scope.row.deptId, scope.row)"
              effect="plain"
              size="small"
            >
              {{ getSceneName(scope.row.deptId, scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="floor" label="楼层指示" width="100" />
        <el-table-column prop="roomNo" label="房间号" width="100" />
        <el-table-column prop="state" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.state === 1 ? 'success' : 'danger'" effect="light" size="small">
              {{ scope.row.state === 1 ? "开放" : "关闭" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联硬件映射" min-width="180">
          <template #default="scope">
            <div v-if="scope.row.screenId" style="font-size: 12px; color: #409EFF"><i class="el-icon-monitor" /> {{ scope.row.screenId }}</div>
            <div v-if="scope.row.voiceDeviceId" style="font-size: 12px; color: #E6A23C"><i class="el-icon-microphone" /> {{ scope.row.voiceDeviceId }}</div>
          </template>
        </el-table-column>

        <table-action-column :count="2">
          <template #default="{ row }">
            <table-action-icon icon="el-icon-edit" tip="编辑" @click="showEditDialog(row)" />
            <table-action-icon icon="el-icon-delete" tip="删除" type="danger" @click="handleDelete(row)" />
          </template>
        </table-action-column>
      </el-table>
    </el-card>

    <form-drawer :title="dialogTitle" v-model="dialogVisible" size="520px" @close="resetForm">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="资源名称" prop="name">
          <el-input v-model="form.name" placeholder="如：内科一诊室、中药3号窗" />
        </el-form-item>

        <el-form-item label="所属科室" prop="deptId">
          <el-cascader
            v-model="deptCascader"
            :options="deptCascaderOptions"
            :props="{ checkStrictly: true, value: 'value', label: 'label', children: 'children', emitPath: false }"
            filterable
            clearable
            placeholder="请选择叶子科室（诊区节点不可选）"
            style="width: 100%"
            @change="onDeptCascaderChange"
          />
          <div class="form-field-hint">树中展示诊区层级，仅叶子科室可选为所属科室</div>
        </el-form-item>

        <el-form-item label="专有业务场景" prop="businessType">
          <el-select v-model="form.businessType" placeholder="设置该诊室独立的业务场景（默认跟随科室）" style="width: 100%" clearable>
            <el-option label="门诊 (医生叫号)" value="OUTPATIENT" />
            <el-option label="药房 (配药发药)" value="PHARMACY" />
            <el-option label="检验 (采血打条码)" value="LAB" />
            <el-option label="医技 (大型设备/超声等)" value="MEDTECH" />
            <el-option label="体检 (健康管理/导检)" value="EXAM" />
          </el-select>
          <div v-if="!form.businessType && effectiveBusinessType" class="form-field-hint">
            当前生效：{{ getSceneName(form.deptId, { businessType: effectiveBusinessType }) }}（跟随所属科室，未单独设置）
          </div>
        </el-form-item>

        <el-form-item label="楼层指示" prop="floor">
          <el-input v-model="form.floor" placeholder="如：3F" />
        </el-form-item>
        <el-form-item label="房间号" prop="roomNo">
          <el-input v-model="form.roomNo" placeholder="如：301" />
        </el-form-item>

        <el-divider content-position="left">硬件资源绑定配置 (选填)</el-divider>

        <el-form-item label="关联门边屏" prop="screenId">
          <el-select v-model="form.screenId" placeholder="选择未绑定的诊室门边屏 (CLINIC)" clearable filterable style="width:100%;">
            <el-option
              v-for="s in selectableClinicScreens"
              :key="screenOptionId(s)"
              :label="screenOptionLabel(s)"
              :value="screenOptionId(s)"
            />
          </el-select>
          <div class="form-field-hint">
            仅列出终端类型为诊室门边屏、且尚未在「屏幕管理」绑定诊室的设备；终端生效绑定请在屏幕管理完成。
          </div>
        </el-form-item>

        <el-form-item label="独立音响" prop="voiceDeviceId">
          <el-select
            v-model="form.voiceDeviceId"
            placeholder="选择已启用的独立音响（叫号直推播报）"
            clearable
            filterable
            style="width:100%;"
          >
            <el-option
              v-for="v in voiceDeviceOptions"
              :key="v.id"
              :label="(v.name || v.deviceCode) + (v.location ? ' · ' + v.location : '')"
              :value="v.id"
            />
          </el-select>
          <div class="form-field-hint">
            推荐在「屏幕管理」中为候诊大屏绑定音响；此处诊室直绑为兼容旧配置。音响故障时可在对应大屏将播报通道改为「大屏播报」。
          </div>
        </el-form-item>

        <el-form-item label="是否开放" prop="state">
          <el-radio v-model="form.state" :label="1" border>开放</el-radio>
          <el-radio v-model="form.state" :label="0" border>关闭</el-radio>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定保存</el-button>
      </template>
    </form-drawer>
  </div>
</template>

<script>
/**
 * 物理诊室/窗口资源管理
 * 消费者：后台「物理诊室与终端资源」页；门边屏下拉仅展示未绑定诊室的 CLINIC 设备
 */
import { getAllOfficeRooms, saveOfficeRoom, removeOfficeRoom } from '@/api/officeRoom'
import { GetScreens } from '@/api/ScreenManages'
import { listVoiceDevices } from '@/api/VoiceDeviceManages'

export default {
  name: 'OfficeRoomManages',
  data() {
    return {
      searchScenario: '',
      loading: false,
      roomList: [],
      deptOptions: [],
      deptCascader: null,
      deptCascaderOptions: [],
      /** 全部 CLINIC 门边屏（含已绑定），由 fetchScreenOptions 拉取 */
      clinicScreenOptions: [],
      voiceDeviceOptions: [],
      dialogVisible: false,
      dialogTitle: '新增物理环境资源',
      form: {
        id: '',
        name: '',
        deptId: '',
        businessType: '',
        floor: '',
        roomNo: '',
        screenId: '',
        voiceDeviceId: '',
        state: 1
      },
      rules: {
        name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
        deptId: [{ required: true, message: '必须选择所属科室', trigger: 'change' }]
      }
    }
  },
  computed: {
    effectiveBusinessType() {
      if (this.form.businessType) return this.form.businessType
      const dept = this.deptOptions.find(d => String(d.id || d.ID) === String(this.form.deptId))
      return dept ? (dept.businessType || dept.BusinessType || '') : ''
    },
    /** 下拉可选：未绑定诊室的 CLINIC；编辑时保留当前已选 screenId */
    selectableClinicScreens() {
      const currentId = this.form.screenId ? String(this.form.screenId) : ''
      return this.clinicScreenOptions.filter(s => {
        const id = this.screenOptionId(s)
        if (currentId && id === currentId) return true
        return !this.isClinicScreenBound(s)
      })
    },
    filteredRoomList() {
      if (!this.searchScenario) return this.roomList
      return this.roomList.filter(room => {
        let bType = room.businessType || room.BusinessType
        if (!bType) {
          const dept = this.deptOptions.find(d => String(d.id || d.ID) === String(room.deptId))
          if (dept) {
            bType = dept.businessType || dept.BusinessType
          }
        }
        return bType === this.searchScenario
      })
    }
  },
  created() {
    this.fetchDeptOptions()
    this.fetchScreenOptions()
    this.fetchVoiceDevices()
    this.fetchData()
  },
  methods: {
    fetchData() {
      this.loading = true
      // 如果后端需要载荷，按理传入空也能获取
      getAllOfficeRooms().then(res => {
        this.roomList = res.data || []
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    fetchVoiceDevices() {
      listVoiceDevices().then(res => {
        const rows = (res && res.data) || []
        this.voiceDeviceOptions = rows.filter(v => v.state == null || v.state === 1)
      }).catch(() => {
        this.voiceDeviceOptions = []
      })
    },
    fetchDeptOptions() {
      this.$store.dispatch('department/getAllDepartment3').then(res => {
        this.deptOptions = res || []
      })
      this.$store.dispatch('department/getAllDepartment').then(tree => {
        this.deptCascaderOptions = this.markNonLeafDisabled(JSON.parse(JSON.stringify(tree || [])))
      })
    },
    /** 诊区/有子节点：disabled，仅叶子可选 */
    markNonLeafDisabled(nodes) {
      if (!Array.isArray(nodes)) return []
      return nodes.map(n => {
        const hasChildren = n.children && n.children.length
        const copy = { ...n, disabled: !!hasChildren || n.disabled }
        if (hasChildren) {
          copy.children = this.markNonLeafDisabled(n.children)
        }
        return copy
      })
    },
    onDeptCascaderChange(val) {
      this.form.deptId = val || ''
    },
    /** 拉取 CLINIC 门边屏列表；roomInfo 非空表示已在屏幕管理绑定诊室 */
    fetchScreenOptions() {
      return GetScreens({ terminalType: 'CLINIC' }).then(res => {
        this.clinicScreenOptions = (res.data && res.data.data) || res.data || []
      }).catch(() => {
        this.clinicScreenOptions = []
      })
    },
    screenOptionId(screen) {
      return String(screen.id || screen.ID || '')
    },
    screenOptionLabel(screen) {
      const name = screen.name || screen.Name || '未命名'
      const code = screen.deviceCode || screen.DeviceCode
      return code ? `${name} (${code})` : name
    },
    isClinicScreenBound(screen) {
      const roomInfo = screen.roomInfo || screen.RoomInfo || ''
      return String(roomInfo).trim() !== ''
    },
    getDeptName(deptId) {
      const dept = this.deptOptions.find(d => String(d.id || d.ID) === String(deptId))
      return dept ? (dept.name || dept.Name) : ''
    },
    getDeptBusinessType(deptId, room) {
      if (room && (room.businessType || room.BusinessType)) {
        return room.businessType || room.BusinessType
      }
      const dept = this.deptOptions.find(d => String(d.id || d.ID) === String(deptId))
      return dept ? (dept.businessType || dept.BusinessType) : null
    },
    getSceneName(deptId, room) {
      const bType = this.getDeptBusinessType(deptId, room)
      if (bType === 'OUTPATIENT') return '门诊'
      if (bType === 'PHARMACY') return '药房'
      if (bType === 'LAB') return '检验'
      if (bType === 'MEDTECH') return '医技'
      if (bType === 'EXAM') return '体检'
      return '未定'
    },
    getSceneType(deptId, room) {
      const bType = this.getDeptBusinessType(deptId, room)
      if (bType === 'OUTPATIENT') return ''
      if (bType === 'PHARMACY') return 'success'
      if (bType === 'LAB') return 'danger'
      if (bType === 'MEDTECH') return 'warning'
      if (bType === 'EXAM') return 'info'
      return 'info'
    },
    showAddDialog() {
      this.dialogTitle = '新增物理环境资源'
      this.form.id = ''
      this.deptCascader = null
      this.fetchScreenOptions()
      this.dialogVisible = true
    },
    showEditDialog(row) {
      this.dialogTitle = '编辑物理环境资源与硬件绑定'
      this.fetchScreenOptions()
      this.form = {
        id: row.id || row.ID || '',
        name: row.name || row.Name || '',
        deptId: row.deptId || row.DeptId || '',
        businessType: row.businessType || row.BusinessType || '',
        floor: row.floor || row.Floor || '',
        roomNo: row.roomNo || row.RoomNo || '',
        screenId: row.screenId || row.ScreenId || '',
        voiceDeviceId: row.voiceDeviceId || row.VoiceDeviceId || '',
        state: row.state != null ? row.state : (row.State != null ? row.State : 1)
      }
      this.deptCascader = this.form.deptId || null
      this.dialogVisible = true
    },
    resetForm() {
      this.form = { id: '', name: '', deptId: '', businessType: '', floor: '', roomNo: '', screenId: '', voiceDeviceId: '', state: 1 }
      this.deptCascader = null
      this.$refs.form && this.$refs.form.resetFields()
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          saveOfficeRoom(this.form).then(res => {
            if (res.success || res.code === 200) {
              this.$message.success('保存成功')
              this.dialogVisible = false
              this.fetchData()
            } else {
              this.$message.error(res.msg || '保存失败')
            }
          }).catch(err => {
            this.$message.error((err && err.message) || '保存失败')
          })
        }
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除 [${row.name}] 吗? 将校验坐诊医生与排班绑定。`, '高危操作警告', {
        confirmButtonText: '确定解绑并删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        removeOfficeRoom(row.id).then(res => {
          if (res.success || res.code === 200) {
            this.$message.success('删除成功')
            this.fetchData()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        }).catch(err => {
          this.$message.error((err && err.message) || '删除失败')
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.app-container {
  padding: 20px;
}
.form-field-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}
</style>
