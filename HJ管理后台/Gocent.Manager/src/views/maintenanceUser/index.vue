<template>
  <div class="app-container">
    <div class="filter-container list-toolbar">
      <div class="list-toolbar__left">
        <el-input
          v-model="listQuery.keyword"
          placeholder="帐号/工号 / 用户名 / 手机号"
          clearable
          class="filter-item"
          style="width: 240px;"
          @keyup.enter="handleFilter"
        />
        <el-select
          v-model="listQuery.loginScene"
          placeholder="使用场景"
          clearable
          class="filter-item"
          style="width: 160px;"
        >
          <el-option
            v-for="item in loginSceneOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select
          v-model="listQuery.passwordResetRequested"
          placeholder="密码申请"
          clearable
          class="filter-item"
          style="width: 140px;"
        >
          <el-option label="待重置" :value="1" />
        </el-select>
        <el-select
          v-model="listQuery.roleId"
          placeholder="角色类型"
          clearable
          class="filter-item"
          style="width: 200px;"
        >
          <el-option
            v-for="item in RoleArry"
            :key="item.RoleId"
            :label="item.RoleName"
            :value="item.RoleId"
            :disabled="item.IsDelete"
          />
        </el-select>
        <el-button type="primary" icon="el-icon-search" native-type="button" class="filter-item" @click="handleFilter">
          查询
        </el-button>
        <el-button native-type="button" class="filter-item" @click="handleReset">重置</el-button>
      </div>
      <div class="list-toolbar__right">
        <el-button type="primary" icon="el-icon-plus" @click="ShowAddUserDialog">新增用户</el-button>
      </div>
    </div>

    <!-- 内容部分 -->

    <el-row>
      <el-col>
        <el-card
          v-loading="loadingBool"
          element-loading-background="rgba(250, 250, 250,0.9)"
        >
          <el-table
            ref="singleTable"
            :data="UserData"
            highlight-current-row
            style="width: 100%"
            @current-change="handleCurrentChange"
          >
            <el-table-column property="UserNo" label="帐号/工号" min-width="120" show-overflow-tooltip />
            <el-table-column property="UserName" label="用户名" min-width="100" show-overflow-tooltip />
            <el-table-column property="Phone" label="手机号" min-width="120" show-overflow-tooltip>
              <template #default="scope">
                <span>{{ scope.row.Phone || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column property="LoginSceneName" label="使用场景" width="100">
              <template #default="scope">
                <span>{{ scope.row.LoginSceneName || scope.row.LoginScene || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="密码申请" width="90">
              <template #default="scope">
                <el-tag v-if="scope.row.PasswordResetRequested === 1" type="danger" size="small">待重置</el-tag>
                <span v-else style="color:#c0c4cc;">—</span>
              </template>
            </el-table-column>
            <el-table-column property="CreateDate" label="创建时间" min-width="170" show-overflow-tooltip />
            <el-table-column property="State" label="状态" width="80">
              <template #default="scope">
                <el-tag
                  :type="scope.row.State === 1 ? 'success' : 'error'"
                  disable-transitions
                >{{ scope.row.State === 1 ? "启用" : "禁用" }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column property="DeptID" label="诊室ID" min-width="140" show-overflow-tooltip>
              <template #default="scope">
                <span>{{ scope.row.DeptID || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column property="Role" label="角色" min-width="120" show-overflow-tooltip />
            <table-action-column :count="3">
              <template #default="{ row, $index }">
                <table-action-icon icon="el-icon-edit" tip="编辑" @click="EditUser($index, row)" />
                <table-action-icon icon="el-icon-key" tip="重置密码" type="warning" @click="AdminResetPassword(row)" />
                <table-action-icon icon="el-icon-delete" tip="删除" type="danger" @click="RemoveUser($index, row)" />
              </template>
            </table-action-column>
          </el-table>
          <!-- 分页 -->
          <el-pagination
            background
            :current-page="PageIndex"
            :page-sizes="[5, 20, 30, 40]"
            :page-size="PageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="TotalCount"
            @size-change="SizeChange"
            @prev-click="PagePrev"
            @current-change="PageChange"
            @next-click="PageNext"
          />
        </el-card>
      </el-col>
    </el-row>

    <form-drawer title="新增用户" v-model="AddUserdialogVisible">
      <el-form
        ref="AddForm"
        :model="AddUser"
        :rules="AddUserRules"
        size="small"
        label-width="100px"
      >
        <el-form-item label="帐号/工号" prop="UserNo">
          <el-input
            v-model="AddUser.UserNo"
            placeholder="请输入帐号或工号"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="AddUser.PassWord"
            placeholder="默认 123456"
            show-password
          />
        </el-form-item>
        <el-form-item label="用户名" prop="UserName">
          <el-input
            v-model="AddUser.UserName"
            placeholder="请输入用户名"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="AddUser.Phone"
            placeholder="选填，可用于登录"
            maxlength="20"
          />
        </el-form-item>
        <el-form-item label="使用场景" prop="LoginScene">
          <el-select v-model="AddUser.LoginScene" placeholder="请选择登录场景" style="width: 100%;">
            <el-option
              v-for="item in loginSceneOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <div class="form-field-hint">{{ loginSceneHint }}</div>
        </el-form-item>
        <el-form-item label="状态" prop="State">
          <el-radio v-model="AddUser.State" label="1" border>启用</el-radio>
          <el-radio v-model="AddUser.State" label="0" border>禁用</el-radio>
        </el-form-item>
        <el-form-item label="常驻科室">
          <el-cascader
            v-model="AddUser.DeptID"
            :options="DepartmentArry"
            :show-all-levels="false"
            clearable
            placeholder="选填；护士站为常驻诊区，当班另见排班"
            @change="AddDeptChange"
          />
        </el-form-item>
        <el-form-item label="角色" prop="RoleId">
          <el-select v-model="AddUser.RoleId" placeholder="请选择角色">
            <el-option
              v-for="(item, index) in RoleArry"
              :key="index"
              :label="item.RoleName"
              :value="item.RoleId"
              :disabled="item.IsDelete"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="AddUserdialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="AddUsers('AddForm')">确 定</el-button>
      </template>
    </form-drawer>

    <form-drawer title="修改信息" v-model="UpdateUserdialogVisible">
      <el-form
        ref="FromUser"
        :model="FromUser"
        :rules="EditorUserRules"
        size="small"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="UserName">
          <el-input v-model="FromUser.UserName" autocomplete="off" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="FromUser.Phone"
            placeholder="选填，可用于登录"
            maxlength="20"
          />
        </el-form-item>
        <el-form-item label="使用场景" prop="LoginScene">
          <el-select v-model="FromUser.LoginScene" placeholder="请选择登录场景" style="width: 100%;">
            <el-option
              v-for="item in loginSceneOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <div class="form-field-hint">{{ loginSceneHint }}</div>
        </el-form-item>
        <el-form-item label="状态" prop="State">
          <el-radio v-model="FromUser.State" label="1" border>启用</el-radio>
          <el-radio v-model="FromUser.State" label="0" border>禁用</el-radio>
        </el-form-item>
        <el-form-item label="常驻科室">
          <el-cascader
            v-model="FromUser.DeptID"
            :options="DepartmentArry"
            :show-all-levels="false"
            clearable
            placeholder="选填；护士站为常驻诊区，当班另见排班"
            @change="EditorDeptChange"
          />
        </el-form-item>
        <el-form-item label="角色" prop="RoleId">
          <el-select v-model="FromUser.RoleId" placeholder="请选择角色">
            <el-option
              v-for="(item, index) in RoleArry"
              :key="index"
              :label="item.RoleName"
              :value="item.RoleId"
              :disabled="item.IsDelete"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="UpdateUserdialogVisible = false">取 消</el-button>
        <el-button type="danger" @click="ShowUpdatePassword('FromUser')">修改密码</el-button>
        <el-button type="primary" @click="UpdateUser('FromUser')">确 定</el-button>
      </template>
    </form-drawer>

    <!-- 修改密码弹窗 -->
    <el-dialog
      title="修改密码"
      v-model="ShowUpdatePasswordVisible"
      width="40%"
      @close="ColseChangePwddialog"
    >
      <el-form
        ref="ChangePwdForm"
        :model="ChangePassWord"
        size="medium "
        :rules="ChangePwdRules"
      >
        <el-form-item label="用户名" label-width="80px">
          <el-input
            v-model="ChangePassWord.UserName"
            :disabled="true"
          />
        </el-form-item>

        <el-form-item label="旧密码" label-width="80px" prop="OldPwd">
          <el-input v-model="ChangePassWord.OldPwd" show-password />
        </el-form-item>

        <el-form-item label="新密码" label-width="80px" prop="NewPwd">
          <el-input v-model="ChangePassWord.NewPwd" show-password />
        </el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer">
        <el-button @click="ShowUpdatePasswordVisible = false">取 消</el-button>
        <el-button
          type="primary"
          @click="UpdatePassword('ChangePwdForm')"
        >确定</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>
<script>
import { ElMessage as Message } from 'element-plus'
import { adminResetPassword } from '@/api/user'

/** 登录场景选项，与后端 LoginScene 枚举及说明书 §23.8 一致 */
const LOGIN_SCENE_OPTIONS = [
  { value: 'ADMIN', label: '后台管理' },
  { value: 'NURSE', label: '护士站' },
  { value: 'DOCTOR', label: '叫号器（门诊）' },
  { value: 'PHARMACY', label: '叫号器（药房）' },
  { value: 'LAB', label: '叫号器（检验）' },
  { value: 'EXAM', label: '叫号器（医技）' }
]
const LOGIN_SCENE_HINT = '后台管理→管理后台；护士站→护士工作站；叫号器各岗位→HJ叫号器（不可跨端登录）'

export default {
  name: 'UserManagement',
  data() {
    return {
      loginSceneOptions: LOGIN_SCENE_OPTIONS,
      loginSceneHint: LOGIN_SCENE_HINT,
      UserData: [],
      ChangePassWord: {
        UserNo: '',
        UserName: '',
        OldPwd: '',
        NewPwd: ''
      },
      ChangePwdRules: {
        OldPwd: [
          { required: true, message: '请输入旧密码', trigger: 'change' }
        ],
        NewPwd: [
          { required: true, message: '请输入新密码', trigger: 'change' }
        ]
      },
      AddUser: {
        UserNo: '',
        UserName: '',
        PassWord: '123456',
        Phone: '',
        LoginScene: '',
        State: '1',
        DeptID: '',
        RoleId: ''
      },
      FromUser: {
        UserName: '',
        Phone: '',
        LoginScene: '',
        DeptID: '',
        State: '',
        RoleId: '',
        UserNo: ''
      },
      EditorUserRules: {
        UserName: [
          { required: true, message: '此项不能为空', trigger: 'change' }
        ],
        LoginScene: [
          { required: true, message: '请选择使用场景', trigger: 'change' }
        ],
        RoleId: [
          { required: true, message: '此项不能为空', trigger: 'change' }
        ],
        State: [{ required: true, message: '此项不能为空', trigger: 'change' }]
      },
      AddUserRules: {
        UserNo: [
          { required: true, message: '此项不能为空', trigger: 'change' }
        ],
        UserName: [
          { required: true, message: '此项不能为空', trigger: 'change' }
        ],
        LoginScene: [
          { required: true, message: '请选择使用场景', trigger: 'change' }
        ],
        RoleId: [
          { required: true, message: '请选择角色', trigger: 'change' }
        ]
      },
      DepartmentArry: [], // 诊室信息
      RoleArry: [], // 角色信息
      UpdateUserdialogVisible: false, // 控制修改用户的弹窗
      AddUserdialogVisible: false, // 控制删除用户的弹窗
      ShowUpdatePasswordVisible: false, // 控制修改密码的弹窗
      loadingBool: false,
      currentRow: null,
      PageIndex: 1,
      PageSize: 5,
      TotalCount: 0,
      listQuery: {
        keyword: '',
        roleId: '',
        loginScene: '',
        passwordResetRequested: null
      },
      formLabelWidth: '100px'
    }
  },
  mounted() {
    this.GetAllRole()
    this.PageInit(this.PageIndex, this.PageSize)
  },

  methods: {
    handleFilter() {
      this.PageIndex = 1
      this.PageInit(this.PageIndex, this.PageSize)
    },
    handleReset() {
      this.listQuery.keyword = ''
      this.listQuery.roleId = ''
      this.listQuery.loginScene = ''
      this.listQuery.passwordResetRequested = null
      this.PageIndex = 1
      this.PageInit(this.PageIndex, this.PageSize)
    },
    buildPageQuery(pageindex, pagesize) {
      const query = { pageindex, pagesize }
      const kw = (this.listQuery.keyword || '').trim()
      if (kw) query.keyword = kw
      if (this.listQuery.roleId) query.roleId = this.listQuery.roleId
      if (this.listQuery.loginScene) query.loginScene = this.listQuery.loginScene
      if (this.listQuery.passwordResetRequested === 1) {
        query.passwordResetRequested = 1
      }
      return query
    },
    /** 管理员重置为默认密码 123456，清除待重置标记 */
    AdminResetPassword(row) {
      this.$confirm(
        `将「${row.UserName || row.UserNo}」的密码重置为默认密码 123456，并要求下次登录修改。是否继续？`,
        '重置密码',
        { type: 'warning' }
      )
        .then(() => adminResetPassword(row.UserNo))
        .then(() => {
          this.$message.success('密码已重置为 123456')
          this.PageInit(this.PageIndex, this.PageSize)
        })
        .catch((err) => {
          if (err === 'cancel') return
          this.$message.error((err && err.message) || '重置失败')
        })
    },
    // 修密码
    UpdatePassword(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.$store
            .dispatch('user/resetPassWord', this.ChangePassWord)
            .then((response) => {
              if (response == true) {
                this.$message({
                  message: '修改成功！',
                  type: 'success'
                })
              }
              this.ShowUpdatePasswordVisible = false
            })
            .catch((error) => {})
        } else {
          this.$message.error('修改错误！')
        }
      })
    },
    // 修改密码弹窗
    ShowUpdatePassword() {
      this.ChangePassWord.UserNo = this.FromUser.UserNo
      this.ChangePassWord.UserName = this.FromUser.UserName
      this.ShowUpdatePasswordVisible = true
    },
    ColseChangePwddialog() {
      this.ChangePassWord.OldPwd = ''
      this.ChangePassWord.NewPwd = ''
    },
    // 选择诊室的更改事件 选出最后一次选择的诊室id
    EditorDeptChange(value) {
      this.FromUser.DeptID = value && value.length > 1 ? value[1] : ''
    },
    // 选择诊室的更改事件 选出最后一次选择的诊室id
    AddDeptChange(value) {
      this.AddUser.DeptID = value && value.length > 1 ? value[1] : ''
    },
    // 获取所有的角色信息
    GetAllRole() {
      this.$store
        .dispatch('role/getRole')
        .then((response) => {
          this.RoleArry = response
        })
        .catch((error) => {
          Message.error('服务器错误！')
        })
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
    },
    // 修改用户信息
    UpdateUser(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          const payload = {
            ...this.FromUser,
            State: parseInt(this.FromUser.State, 10),
            Phone: (this.FromUser.Phone || '').trim(),
            DeptID: this.FromUser.DeptID || undefined
          }
          this.$store
            .dispatch('user/editorUser', payload)
            .then((response) => {
              this.PageInit(this.PageIndex, this.PageSize)
              this.$message({
                message: '修改成功！',
                type: 'success'
              })
            })
            .catch((error) => {
              Message.error('服务器错误！')
            })

          // 关闭弹窗
          this.UpdateUserdialogVisible = false
        } else {
          this.$message.error('修改错误!')
          return false
        }
      })
    },

    // 新增用户
    AddUsers(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          const payload = {
            ...this.AddUser,
            State: parseInt(this.AddUser.State, 10),
            PassWord: (this.AddUser.PassWord || '').trim() || '123456',
            Phone: (this.AddUser.Phone || '').trim() || undefined,
            DeptID: this.AddUser.DeptID || undefined
          }
          this.$store
            .dispatch('user/addUser', payload)
            .then((response) => {
              this.PageInit(this.PageIndex, this.PageSize)
              this.$message({
                message: '新增成功！',
                type: 'success'
              })
              this.AddUserdialogVisible = false
            })
            .catch((error) => {
              this.$message.error(error.message)
            })
        } else {
          this.$message.error('无法新增!')
          return false
        }
      })
    },

    // 弹出新增用户界面
    ShowAddUserDialog() {
      if (this.DepartmentArry.length <= 0) this.GetAllDepartment()
      if (this.RoleArry.length <= 0) this.GetAllRole()
      this.AddUser = {
        UserNo: '',
        UserName: '',
        PassWord: '123456',
        Phone: '',
        LoginScene: '',
        State: '1',
        DeptID: '',
        RoleId: ''
      }
      this.AddUserdialogVisible = true
    },

    // 弹出修改界面
    EditUser(index, row) {
      // 如果有诊室数据 那么就用vuex里的 没有数据我就去后端请求
      if (this.DepartmentArry.length <= 0) this.GetAllDepartment()
      // 如果有角色数据 那么就是用vuex里的 没有数据我就去后端请求
      if (this.RoleArry.length <= 0) this.GetAllRole()

      // 赋默认值
      this.UpdateUserdialogVisible = true
      this.FromUser.UserName = row.UserName
      this.FromUser.Phone = row.Phone || ''
      this.FromUser.LoginScene = row.LoginScene || ''
      this.FromUser.State = row.State.toString()
      this.FromUser.DeptID = row.DeptID
      this.FromUser.RoleId = row.RoleId
      this.FromUser.UserNo = row.UserNo
    },

    // 删除用户
    RemoveUser(index, row) {
      this.$confirm('是否删除此用户?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.$store
            .dispatch('user/removeUser', row.UserNo)
            .then((response) => {
              this.PageInit(this.PageIndex, this.PageSize)
              this.$message({
                type: 'success',
                message: '删除成功!'
              })
            })
            .catch((error) => {
              this.$message.error('删除错误，服务器错误，无法删除该用户！')
            })
        })
        .catch((error) => {
          this.$message({
            type: 'info',
            message: '已取消删除'
          })
        })
    },

    // 点击表格的事件
    handleCurrentChange() {},
    // 初始用户数据
    PageInit(pageindex, pagesize) {
      this.loadingBool = true
      this.$store
        .dispatch('user/getAllUser', this.buildPageQuery(pageindex, pagesize))
        .then((response) => {
          this.UserData = response.data
          this.TotalCount = response.pageInfo.totalCount
          this.loadingBool = false
        })
        .catch(() => {
          this.loadingBool = false
        })
    },
    // PageSize 改变
    SizeChange(Size) {
      this.PageSize = Size
      this.PageInit(this.PageIndex, this.PageSize)
    },
    // 上一页点击事件
    PagePrev(index) {
      this.PageIndex = index
      this.PageInit(this.PageIndex, this.PageSize)
    },
    // 点击下面的页码事件
    PageChange(index) {
      this.PageIndex = index
      this.PageInit(this.PageIndex, this.PageSize)
    },
    // 下一页点击事件
    PageNext(index) {
      this.PageIndex = index
      this.PageInit(this.PageIndex, this.PageSize)
    }
  }
}
</script>

<style scoped>
.form-field-hint {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: #909399;
}
</style>
