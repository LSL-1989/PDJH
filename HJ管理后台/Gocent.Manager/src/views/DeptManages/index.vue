<template>
  <div class="app-container">
    <div class="list-toolbar">
      <div class="list-toolbar__left">
        <el-button
          type="warning"
          icon="el-icon-connection"
          round
          @click="showHisSyncDialog = true"
        >对接 HIS 同步字典表</el-button>
      </div>
      <div class="list-toolbar__right">
        <el-button
          type="primary"
          icon="el-icon-plus"
          round
          @click="ShowdialogAddVisible"
        >新增科室/诊区</el-button>
      </div>
    </div>
    <el-row>
      <el-col>
        <el-card>
          <el-table
            v-loading="loadingBool"
            :data="DeptData"
            style="width: 100%; border-radius: 8px;"
            row-key="ID"
            default-expand-all
            :tree-props="{ children: 'children' }"
          >
            <el-table-column prop="ID" label="科室/诊区 ID" sortable width="180" show-overflow-tooltip />
            <el-table-column type="index" label="序号" sortable width="70" />
            <el-table-column prop="Name" label="名称" min-width="160" show-overflow-tooltip>
              <template #default="scope">
                <span style="font-weight:600; color:#202124;">{{ scope.row.Name }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="Type" label="节点类型" width="110" align="center">
              <template #default="scope">
                <el-tag
                  :type="scope.row.Type === 1 ? 'info' : ''"
                  effect="plain"
                  size="small"
                >
                  {{ formatDeptType(scope.row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="Queue" label="队列编码" width="120" show-overflow-tooltip>
              <template #default="scope">
                {{ formatDeptQueue(scope.row) }}
              </template>
            </el-table-column>
            <el-table-column prop="State" label="运行状态" width="120">
              <template #default="scope">
                <el-tag
                  :type="scope.row.State === 1 ? 'success' : 'danger'"
                  effect="light"
                  round
                  size="small"
                >
                  <i :class="scope.row.State === 1 ? 'el-icon-check' : 'el-icon-close'" />
                  {{ scope.row.State === 1 ? "启用中" : "已关闭" }}
                </el-tag>
              </template>
            </el-table-column>
            <table-action-column :count="2">
              <template #default="{ row }">
                <table-action-icon icon="el-icon-edit" tip="编辑" @click="ShowEditDept(row)" />
                <table-action-icon icon="el-icon-delete" tip="删除" type="danger" @click="RemoveDept(row)" />
              </template>
            </table-action-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <form-drawer
      title="编辑科室/诊区"
      v-model="dialogEditorVisible"
      @close="dialogEditorVisibleClose"
    >
      <el-form
        ref="EditorDept"
        :rules="EditorRules"
        :model="EditorDept"
        label-width="100px"
      >
        <el-form-item label="名称" prop="Name">
          <el-input v-model="EditorDept.Name" />
        </el-form-item>
        <el-form-item label="状态" prop="State">
          <el-radio v-model="EditorDept.State" label="1" border>启用</el-radio>
          <el-radio v-model="EditorDept.State" label="0" border>关闭</el-radio>
        </el-form-item>
        <el-form-item v-show="QueueisShow" label="队列编码" prop="Queue">
          <el-input v-model="EditorDept.Queue" />
        </el-form-item>
        <el-form-item label="业务类型" prop="BusinessType">
          <el-select v-model="EditorDept.BusinessType" placeholder="请选择该科室的叫号管线" style="width: 100%">
            <el-option label="门诊 (医生叫号)" value="OUTPATIENT" />
            <el-option label="药房 (配药发药)" value="PHARMACY" />
            <el-option label="检验 (采血打条码)" value="LAB" />
            <el-option label="医技/体检 (大型设备/导检)" value="EXAM" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogEditorVisible = false">取 消</el-button>
        <el-button type="primary" @click="EditorDepts('EditorDept')">确 定</el-button>
      </template>
    </form-drawer>

    <form-drawer
      title="新增科室/诊区"
      v-model="dialogAddVisible"
      @close="dialogAddVisibleClose"
    >
      <el-form
        ref="AddDeptForm"
        :model="AddDept"
        :rules="AddDeptRules"
        label-width="110px"
      >
        <el-form-item label="节点类型" prop="Type">
          <el-radio-group v-model="AddDept.Type" @change="onAddTypeChange">
            <el-radio label="1" border>诊区/分组</el-radio>
            <el-radio label="2" border>叶子科室</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="AddDept.Type === '2'" label="科室 ID" prop="ID">
          <el-input v-model="AddDept.ID" placeholder="与 HIS 一致的科室 ID（必填）" />
        </el-form-item>
        <el-form-item label="名称" prop="Name">
          <el-input v-model="AddDept.Name" placeholder="如：3楼内科诊区 / 内分泌科" />
        </el-form-item>
        <el-form-item label="挂载位置" prop="DeptID">
          <el-cascader
            v-model="AddDept.DeptID"
            :options="DepartmentArry"
            :show-all-levels="true"
            :props="{ checkStrictly: true }"
            clearable
            style="width:100%;"
            @change="AddDeptChange"
          />
          <div style="font-size:12px;color:#909399;margin-top:4px;">选「挂到根级」为顶级诊区；也可挂到已有诊区/分组下（支持多层）</div>
        </el-form-item>
        <el-form-item label="状态" prop="State">
          <el-radio v-model="AddDept.State" label="1" border>启用</el-radio>
          <el-radio v-model="AddDept.State" label="0" border>禁用</el-radio>
        </el-form-item>
        <el-form-item v-show="AddDept.Type === '2'" label="队列编码" prop="Queue">
          <el-input v-model="AddDept.Queue" placeholder="可选，队列/HIS 编码" />
        </el-form-item>
        <el-form-item v-show="AddDept.Type === '2'" label="业务类型" prop="BusinessType">
          <el-select v-model="AddDept.BusinessType" placeholder="请选择该科室的叫号管线" style="width: 100%">
            <el-option label="门诊 (医生叫号)" value="OUTPATIENT" />
            <el-option label="药房 (配药发药)" value="PHARMACY" />
            <el-option label="检验 (采血打条码)" value="LAB" />
            <el-option label="医技/体检 (大型设备/导检)" value="EXAM" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogAddVisible = false">取 消</el-button>
        <el-button type="primary" @click="AddDeparts('AddDeptForm')">确 定</el-button>
      </template>
    </form-drawer>

    <his-sync-dialog
      v-model="showHisSyncDialog"
      entity-type="dept"
      title="HIS 业务数据对接与同步策略 (科室字典)"
      count-unit="条科室"
      @synced="onHisSynced"
    />
  </div>
</template>

<script>
import HisSyncDialog from '@/components/HisSyncDialog'
import { getApiErrorMessage } from '@/utils/request'
export default {
  name: 'DeptManages',
  components: { HisSyncDialog },
  data() {
    return {
      QueueisShow: false,
      loadingBool: false,
      AddDept: {
        ID: '',
        Name: '',
        Type: '1',
        DeptID: ['__ROOT__'],
        State: '1',
        FatherID: '',
        Queue: '',
        BusinessType: 'OUTPATIENT'
      },
      AddDeptRules: {
        Name: [{ required: true, message: '此项不能为空', trigger: 'change' }],
        Type: [{ required: true, message: '请选择节点类型', trigger: 'change' }],
        DeptID: [{ required: true, message: '请选择挂载位置', trigger: 'change' }],
        State: [{ required: true, message: '此项不能为空', trigger: 'change' }],
        ID: [{
          validator: (rule, value, callback) => {
            if (this.AddDept.Type === '2' && (!value || !String(value).trim())) {
              callback(new Error('叶子科室必须录入 HIS 科室 ID'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }]
      },
      EditorDept: {
        ID: '',
        Name: '',
        State: '0',
        Queue: ' ',
        BusinessType: ''
      },
      EditorRules: {
        Name: [{ required: true, message: '此项不能为空', trigger: 'change' }],
        State: [{ required: true, message: '此项不能为空', trigger: 'change' }]
      },
      DeptData: [],
      DepartmentArry: [],
      dialogEditorVisible: false,
      dialogAddVisible: false,
      showHisSyncDialog: false
    }
  },
  mounted() {
    this.GetAllDepartment()
    this.GetDept()
  },
  methods: {
    /** 节点类型：有子节点或 Type=1 为诊区/分组；否则为叶子科室 */
    formatDeptType(row) {
      if (row.children && row.children.length) return '诊区/分组'
      if (row.Type === 1) return '诊区/分组'
      if (row.Type === 2) return '叶子科室'
      return row.IsLeaf === false ? '诊区/分组' : '叶子科室'
    },
    /** 叶子科室展示队列编码 */
    formatDeptQueue(row) {
      if (row.Type === 1 || (row.children && row.children.length)) return '-'
      const q = (row.Queue || '').trim()
      if (!q || q === 'song') return '-'
      return q
    },
    onAddTypeChange() {
      this.QueueisShow = this.AddDept.Type === '2'
      if (this.AddDept.Type === '1') {
        this.AddDept.ID = ''
        this.AddDept.Queue = ''
      }
    },
    // 新增
    AddDeparts(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          const payload = {
            ID: this.AddDept.Type === '2' ? (this.AddDept.ID || '').trim() : undefined,
            Name: this.AddDept.Name,
            Type: Number(this.AddDept.Type),
            State: Number(this.AddDept.State),
            FatherID: this.AddDept.FatherID || null,
            Queue: this.AddDept.Type === '2' ? this.AddDept.Queue : null,
            BusinessType: this.AddDept.Type === '2' ? this.AddDept.BusinessType : null
          }
          this.$store
            .dispatch('department/createDepartment', payload)
            .then((response) => {
              if (response) {
                this.$message({ type: 'success', message: '新增成功！' })
                this.GetDept()
                this.GetAllDepartment()
                this.dialogAddVisible = false
              }
            })
            .catch((error) => {
              this.$message.error(getApiErrorMessage(error, '新增失败'))
            })
        } else {
          this.$message.error('请完善表单')
          return false
        }
      })
    },
    ShowdialogAddVisible() {
      this.dialogAddVisible = true
    },
    AddDeptChange(value) {
      if (!value || !value.length) {
        this.AddDept.FatherID = ''
        return
      }
      const last = value[value.length - 1]
      if (last === '__ROOT__' || last === 'true') {
        this.AddDept.FatherID = null
      } else {
        this.AddDept.FatherID = last
      }
    },
    ShowEditDept(row) {
      this.QueueisShow = row.Type === 2 || !(row.children && row.children.length)
      this.EditorDept.Queue = row.Queue || ''
      this.EditorDept.ID = row.ID
      this.EditorDept.Name = row.Name
      this.EditorDept.State = row.State.toString()
      this.EditorDept.BusinessType = row.BusinessType
      this.dialogEditorVisible = true
    },
    dialogAddVisibleClose() {
      this.AddDept = {
        ID: '',
        Name: '',
        Type: '1',
        DeptID: ['__ROOT__'],
        State: '1',
        FatherID: '',
        Queue: '',
        BusinessType: 'OUTPATIENT'
      }
    },
    dialogEditorVisibleClose() {
      this.QueueisShow = false
      this.EditorDept.Queue = ' '
      this.EditorDept.BusinessType = ''
    },
    EditorDepts(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.$store
            .dispatch('department/editorDepartment', this.EditorDept)
            .then((response) => {
              if (response == true) {
                this.$message({ type: 'success', message: '修改成功！' })
                this.GetAllDepartment()
                this.RefreshDept()
                this.dialogEditorVisible = false
              }
            })
            .catch((error) => {
              this.$message.error(getApiErrorMessage(error, '修改失败'))
            })
        } else {
          this.$message.error('修改错误!')
          return false
        }
      })
    },
    RefreshDept() {
      this.$store
        .dispatch('department/getAllDepartment')
        .then((response) => {
          this.$Bus.$emit('RefreshDept', response)
        })
        .catch(() => {})
    },
    GetDept() {
      this.$store
        .dispatch('department/getAllDepartment')
        .then((response) => {
          this.DepartmentArry = [
            { label: '挂到根级（顶级诊区）', value: '__ROOT__' },
            ...(response || [])
          ]
        })
        .catch(() => {})
    },
    RemoveDept(row) {
      const hasChildren = row.children && row.children.length > 0
      if (hasChildren) {
        this.$message.warning('该节点下仍有子科室/诊区，请先删除或迁移子节点')
        return
      }
      const titletip = `确定删除「${row.Name}」？若有绑定医生将自动解绑并记入首页待处理。`
      this.$confirm(titletip, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.$store
            .dispatch('department/removeDepartments', row.ID)
            .then((response) => {
              if (response) {
                this.$message({ type: 'success', message: '删除成功' })
                this.GetDept()
                this.GetAllDepartment()
                this.RefreshDept()
              } else {
                this.$message.error('删除失败！')
              }
            })
            .catch((error) => {
              this.$message.error(getApiErrorMessage(error, '删除失败！'))
            })
        })
        .catch(() => {
          this.$message({ type: 'info', message: '已取消删除' })
        })
    },
    GetAllDepartment() {
      this.loadingBool = true
      this.$store
        .dispatch('department/getAllDepartment2')
        .then((response) => {
          this.DeptData = response
          this.loadingBool = false
        })
        .catch(() => {
          this.loadingBool = false
        })
    },
    onHisSynced() {
      this.GetAllDepartment()
      this.GetDept()
    }
  }
}
</script>

<style>
</style>
