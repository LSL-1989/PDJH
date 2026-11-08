<template>
  <div class="app-container">
    <div class="filter-container list-toolbar">
      <div class="list-toolbar__left">
        <span class="sync-hint">按角色已授权的主任工作台，同步当日各科室急诊/优先/普通已发最大序号</span>
      </div>
      <div class="list-toolbar__right">
        <el-button
          type="success"
          icon="el-icon-refresh"
          :loading="syncLoading"
          @click="SyncAutoNumFromCallList"
        >同步发号序号</el-button>
        <el-button type="primary" icon="el-icon-plus" @click="ShowCreatedialog">新增序列</el-button>
      </div>
    </div>
    <el-card class="auto-num-card">
      <el-table
        v-loading="loadingBool"
        :data="pagedAutoNumData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="GID" label="Gid" min-width="200" show-overflow-tooltip />
        <el-table-column prop="Code" label="队列类别" width="100" />
        <el-table-column prop="DID" label="诊室ID" min-width="200" show-overflow-tooltip />
        <el-table-column prop="Name" label="诊室名" min-width="140" show-overflow-tooltip />
        <el-table-column prop="AutoDate" label="排队时间" width="180" />
        <el-table-column prop="TypeA" label="急诊序列号" width="100" />
        <el-table-column prop="TypeB" label="优先序列号" width="100" />
        <el-table-column prop="TypeC" label="普通序列号" width="100" />
        <table-action-column :count="2">
          <template #default="{ row }">
            <table-action-icon icon="el-icon-edit" tip="编辑" @click="ShowEditordialog(row)" />
            <table-action-icon icon="el-icon-delete" tip="删除" type="danger" @click="RemoveAutoNum(row)" />
          </template>
        </table-action-column>
      </el-table>

      <el-pagination
        v-if="totalCount > 0"
        class="auto-num-pagination"
        background
        :current-page="PageIndex"
        :page-sizes="[5, 10, 20, 30]"
        :page-size="PageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="totalCount"
        @size-change="SizeChange"
        @current-change="CurrentChange"
      />

      <form-drawer
        :title="CreateOrEditorTitle"
        v-model="dialogCreateOrEditorVisible"
        @close="dialogCreateOrEditorColse"
      >
        <el-form
          ref="editorForm"
          :model="Editor"
          label-position="right"
          size="small"
          :rules="editorRules"
          label-width="110px"
        >
          <el-form-item label="诊室" prop="DeptID">
            <el-cascader
              v-model="Editor.DeptID"
              :disabled="CreateOrEditordisabled"
              :props="{ emitPath: false }"
              :options="DepartmentArry"
              :show-all-levels="false"
            />
          </el-form-item>
          <el-form-item label="排队时间" prop="AutoDate">
            <el-date-picker
              v-model="Editor.AutoDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              :shortcuts="pickerOptions.shortcuts"
            />
          </el-form-item>
          <el-form-item label="急诊队列序号" prop="TypeA">
            <el-input v-model="Editor.TypeA" />
          </el-form-item>
          <el-form-item label="优先队列序号" prop="TypeB">
            <el-input v-model="Editor.TypeB" />
          </el-form-item>
          <el-form-item label="普通队列序号" prop="TypeC">
            <el-input v-model="Editor.TypeC" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogCreateOrEditorVisible = false">取 消</el-button>
          <el-button type="primary" @click="CreateOrEditorAutoNum('editorForm')">确 定</el-button>
        </template>
      </form-drawer>
    </el-card>
  </div>
</template>

<script>
import { getToken } from '@/utils/auth'
import { getApiErrorMessage } from '@/utils/request'

export default {
  name: 'AutoNumManages',
  data() {
    return {
      Seach: {
        Name: ''
      },
      CreateOrEditorTitle: '',
      CreateOrEditordisabled: false,
      CreateOrEditor: false, // 为true新增 为false修改
      Editor: {
        GID: '',
        DeptID: '',
        AutoDate: '',
        TypeA: 0,
        TypeB: 0,
        TypeC: 0
      },
      editorRules: {
        DeptID: [
          { required: true, message: '此项不能为空', trigger: 'change' }
        ],
        AutoDate: [
          { required: true, message: '此项不能为空', trigger: 'change' }
        ],
        TypeA: [{ required: true, message: '此项不能为空', trigger: 'change' }],
        TypeB: [{ required: true, message: '此项不能为空', trigger: 'change' }],
        TypeC: [{ required: true, message: '此项不能为空', trigger: 'change' }]
      },
      DepartmentArry: [], // 诊室信息
      dialogCreateOrEditorVisible: false,
      PageIndex: 1,
      PageSize: 5,
      totalCount: 20,
      loadingBool: false,
      syncLoading: false,
      CallAutoNumData: [],
      allAutoNumData: [],
      pickerOptions: {
        shortcuts: [
          {
            text: '今天',
            value() {
              return new Date()
            }
          }
        ]
      }
    }
  },

  computed: {
    pagedAutoNumData() {
      const start = (this.PageIndex - 1) * this.PageSize
      return this.allAutoNumData.slice(start, start + this.PageSize)
    }
  },

  mounted() {
    this.InitData()

    this.$Bus.$on('RefreshAutoNum', () => {
      this.InitData()
    })
  },
  methods: {
    /** 按角色主任工作台权限，从 call_list 汇总当日发号并写入 call_auto_num（可重复执行更新） */
    SyncAutoNumFromCallList() {
      const token = getToken()
      if (!token) {
        this.$message.error('请先登录')
        return
      }
      this.$confirm(
        '将根据您角色已授权的主任工作台（如门诊主任、药房主任等），同步今日各科室三类发号序号。已同步记录将更新为最新值，同权限用户共享。是否继续？',
        '同步发号序号',
        {
          confirmButtonText: '开始同步',
          cancelButtonText: '取消',
          type: 'info'
        }
      )
        .then(() => {
          this.syncLoading = true
          const today = this.formatToday()
          return this.$store.dispatch('callAutoNum/syncAutoNum', { token, autoDate: today })
        })
        .then((response) => {
          const msg = (response && response.msg) || '同步成功'
          this.$message.success(msg)
          this.InitData()
        })
        .catch((error) => {
          if (error === 'cancel') {
            this.$message.info('已取消同步')
            return
          }
          this.$message.error(getApiErrorMessage(error, '同步失败'))
        })
        .finally(() => {
          this.syncLoading = false
        })
    },
    formatToday() {
      const d = new Date()
      const m = `${d.getMonth() + 1}`.padStart(2, '0')
      const day = `${d.getDate()}`.padStart(2, '0')
      return `${d.getFullYear()}-${m}-${day}`
    },
    // 初始化序号队列信息
    InitData() {
      (this.loadingBool = true),
      this.$store
        .dispatch('callAutoNum/getAutoNum')
        .then((response) => {
          response.filter((item) => {
            item.AutoDate = item.AutoDate.replace('T', ' ')
          })
          this.allAutoNumData = response
          this.totalCount = response.length
          this.CallAutoNumData = response

          this.loadingBool = false
        })
        .catch((error) => {
          this.loadingBool = false
        })
    },
    // 修改
    CreateOrEditorAutoNum(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          if (this.CreateOrEditor == true) {
            this.Creates()
          } else {
            this.Editors()
          }
        } else {
          this.$message.error('请输入完整信息！')
        }
      })
    },
    // 删除
    RemoveAutoNum(row) {
      this.$confirm(`是否将${row.Name}队列删除？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.$store
            .dispatch('callAutoNum/removeAutoNum', row.GID)
            .then((response) => {
              if (response == true) {
                this.$message({
                  type: 'success',
                  message: '删除成功！'
                })
                this.InitData()
              } else {
                this.$message('删除失败！')
              }
            })
            .catch((error) => {
              this.$message.error('删除失败，服务器错误！')
            })
        })
        .catch(() => {
          this.$message({
            type: 'info',
            message: '已取消删除'
          })
        })
    },
    // 新增
    Creates() {
      this.$store
        .dispatch('callAutoNum/createAutoNum', this.Editor)
        .then((response) => {
          if (response == true) {
            this.$message({
              type: 'success',
              message: '新增成功!'
            })
            this.InitData()
            this.dialogCreateOrEditorVisible = false
          } else {
            this.$message('新增失败!')
          }
        })
        .catch((error) => {
          this.$message.error('新增失败,服务器失败!')
        })
    },
    // 修改
    Editors() {
      this.$store
        .dispatch('callAutoNum/editorAutoNum', this.Editor)
        .then((response) => {
          if (response == true) {
            this.$message({
              type: 'success',
              message: '修改成功！'
            })
            this.InitData()
            this.dialogCreateOrEditorVisible = false
          } else {
            this.$message('删除失败!')
          }
        })
        .catch((error) => {
          this.$message.error('删除失败,服务器失败!')
        })
    },
    // 弹出新增窗体
    ShowCreatedialog() {
      this.CreateOrEditorTitle = '新增队列信息'
      this.CreateOrEditordisabled = false
      this.CreateOrEditor = true
      if (this.DepartmentArry.length == 0) {
        this.GetAllDepartment()
      }
      this.dialogCreateOrEditorVisible = true
    },
    // 弹出修改窗体
    ShowEditordialog(row) {
      this.CreateOrEditorTitle = '修改队列信息'
      this.CreateOrEditordisabled = true
      this.CreateOrEditor = false
      this.Editor.GID = row.GID
      this.Editor.DeptID = row.DID
      this.Editor.AutoDate = row.AutoDate
      this.Editor.TypeA = row.TypeA
      this.Editor.TypeB = row.TypeB
      this.Editor.TypeC = row.TypeC
      if (this.DepartmentArry.length == 0) {
        this.GetAllDepartment()
      }
      this.dialogCreateOrEditorVisible = true
    },
    // 窗体关闭事件
    dialogCreateOrEditorColse() {
      this.Editor.DeptID = ''
      this.Editor.AutoDate = ''
      this.Editor.TypeA = 0
      this.Editor.TypeB = 0
      this.Editor.TypeC = 0
    },
    // 每页显示的条数改变事件
    SizeChange(size) {
      this.PageSize = size
      this.PageIndex = 1
    },
    CurrentChange(index) {
      this.PageIndex = index
    },
    // 获取所有的诊室信息
    GetAllDepartment() {
      this.$store
        .dispatch('department/getAllDepartment')
        .then((response) => {
          this.DepartmentArry = response
        })
        .catch((error) => {
          Message.error('服务器错误！')
        })
    }
  }
}
</script>

<style scoped>
.auto-num-card {
  width: 100%;
}

.auto-num-pagination {
  margin-top: 16px;
  text-align: right;
}

.sync-hint {
  font-size: 13px;
  color: #909399;
  line-height: 32px;
}
</style>
