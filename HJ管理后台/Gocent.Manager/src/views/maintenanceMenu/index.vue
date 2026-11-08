<template>
  <div class="app-container">
    <div class="filter-container list-toolbar">
      <div class="list-toolbar__left" />
      <div class="list-toolbar__right">
        <el-button type="primary" icon="el-icon-plus" @click="ShowAddPage()">新增菜单</el-button>
      </div>
    </div>
    <el-row>
      <el-col>
        <el-card>
          <el-table
            v-loading="loadingBool"
            :data="MenuData"
            style="width: 100%; margin-bottom: 20px"
            row-key="Id"
            border
            default-expand-all
            :tree-props="{ children: 'children' }"
          >
            <el-table-column type="index" label="序号" sortable width="50" />

            <el-table-column prop="PageShowName" label="菜单名称" />

            <el-table-column prop="PageUrl" label="路由" />

            <el-table-column prop="Icon" label="图标">
              <template #default="scope">
                <i
                  v-if="isFontIcon(scope.row.Icon)"
                  :class="scope.row.Icon"
                  class="menu-row-icon"
                />
                <svg-icon
                  v-else-if="scope.row.Icon"
                  class-name="international-icon"
                  :icon-class="scope.row.Icon"
                />
              </template>
            </el-table-column>

            <el-table-column prop="Redirect" label="重定向" />

            <el-table-column prop="Redirect" label="是否显示">
              <template #default="scope">
                <el-tag
                  :type="scope.row.AlwaysShow === true ? 'success' : 'error'"
                  disable-transitions
                >{{ scope.row.AlwaysShow === true ? "显示" : "隐藏" }}</el-tag>
              </template>
            </el-table-column>

            <table-action-column :count="2">
              <template #default="{ row }">
                <table-action-icon icon="el-icon-edit" tip="编辑" @click="ShowEditPage(row)" />
                <table-action-icon icon="el-icon-delete" tip="删除" type="danger" @click="RemovePage(row)" />
              </template>
            </table-action-column>
          </el-table>

          <form-drawer
            :title="dialogTitle"
            v-model="dialogCreateOrEditorVisible"
            @close="dialogCreateOrEditorVisibleClose"
          >
            <el-form
              ref="EditorForm"
              :model="EditorPage"
              label-width="90px"
              size="small"
              :rules="EditorRules"
              class="menu-editor-form"
            >
              <el-form-item label="菜单名称" prop="PageShowName">
                <el-input
                  v-model="EditorPage.PageShowName"
                  placeholder="请输入菜单名称"
                />
              </el-form-item>
              <el-form-item label="上级菜单" prop="ParentPageId">
                <el-cascader
                  v-model="EditorPage.ParentPageId"
                  style="width: 100%"
                  :options="MenuData2"
                  :show-all-levels="true"
                  :props="{ checkStrictly: true, emitPath: false }"
                  clearable
                  placeholder="请选择"
                />
              </el-form-item>
              <el-form-item label="页面路径" prop="PageUrl">
                <el-input
                  v-model="EditorPage.PageUrl"
                  placeholder="请输入页面路径"
                />
              </el-form-item>
              <el-form-item label="重定向">
                <el-input
                  v-model="EditorPage.Redirect"
                  placeholder="请输入重定向地址"
                />
              </el-form-item>
              <el-form-item label="菜单图标">
                <icon-picker-input v-model="EditorPage.Icon" />
              </el-form-item>
              <el-form-item label="是否显示" prop="AlwaysShow">
                <el-select
                  v-model="EditorPage.AlwaysShow"
                  placeholder="请选择"
                  style="width: 100%"
                >
                  <el-option label="显示" :value="true" />
                  <el-option label="隐藏" :value="false" />
                </el-select>
              </el-form-item>
              <el-form-item label="是否缓存" prop="NoCache">
                <el-select
                  v-model="EditorPage.NoCache"
                  placeholder="请选择"
                  style="width: 100%"
                >
                  <el-option label="是" :value="true" />
                  <el-option label="不是" :value="false" />
                </el-select>
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="dialogCreateOrEditorVisible = false">取 消</el-button>
              <el-button type="primary" @click="CreateOrEditorPages('EditorForm')">确 定</el-button>
            </template>
          </form-drawer>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { isFontIcon } from '@/icons/catalog'

export default {
  name: 'MenuMaintenance',
  data() {
    return {
      MenuData: [],
      MenuData2: [],
      loadingBool: false,
      CreateOrEditor: false, // 标识新增还是删除
      dialogTitle: '',
      dialogCreateOrEditorVisible: false,
      EditorPage: {
        Id: '',
        PageUrl: '',
        PageShowName: '',
        ParentPageId: '',
        Icon: '',
        AlwaysShow: '',
        Redirect: '',
        NoCache: ''
      },
      EditorRules: {
        PageUrl: [{ required: true, message: '此项不能为空', trigger: 'blur' }],
        PageShowName: [
          { required: true, message: '此项不能为空', trigger: 'blur' }
        ],
        ParentPageId: [
          { required: true, message: '此项不能为空', trigger: 'blur' }
        ],
        AlwaysShow: [
          { required: true, message: '此项不能为空', trigger: 'blur' }
        ],
        NoCache: [{ required: true, message: '此项不能为空', trigger: 'blur' }]
      }
    }
  },
  mounted() {
    this.Init()
  },
  methods: {
    isFontIcon,
    Init() {
      if (
        this.$store.state.page.PageMenuArry &&
        this.$store.state.page.PageMenuArry.length > 0
      ) {
        this.MenuData = this.$store.state.page.PageMenuArry
      } else {
        this.InitData()
      }
      if (
        this.$store.state.page.PageMenuArry2 &&
        this.$store.state.page.PageMenuArry2.length > 0
      ) {
        this.MenuData2 = this.$store.state.page.PageMenuArry2
      } else {
        this.InitData2()
      }
    },
    // 初始菜单数据 用于表格
    InitData() {
      this.loadingBool = true
      this.$store
        .dispatch('page/getPage')
        .then((response) => {
          this.MenuData = response || []
        })
        .catch((error) => {
          this.MenuData = []
          this.$message.error('无法获取菜单数据！')
        })
        .finally(() => {
          this.loadingBool = false
        })
    },
    // 初识菜单数据 用于级联选择器
    InitData2() {
      this.$store
        .dispatch('page/getPage2')
        .then((response) => {
          response.unshift({
            label: '一级菜单',
            value: '0'
          })
          this.MenuData2 = response
        })
        .catch((error) => {})
    },
    // 删除菜单
    RemovePage(row) {
      this.$confirm(`你确定要删除 ${row.PageShowName} ?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.$store
            .dispatch('page/removePage', row.Id)
            .then((response) => {
              if (response == true) {
                this.$message({
                  type: 'success',
                  message: '删除成功！'
                })
                this.InitData()
                this.InitData2()
                this.$store.dispatch('user/getInfo')
              }
            })
            .catch((error) => {})
        })
        .catch(() => {
          this.$message({
            type: 'info',
            message: '已取消删除'
          })
        })
    },
    // 新增/修改菜单
    CreateOrEditorPages(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          if (this.CreateOrEditor) {
            this.$store
              .dispatch('page/createPage', this.EditorPage)
              .then((response) => {
                if (response == true) {
                  this.$message({
                    type: 'success',
                    message: '创建成功！'
                  })
                  this.InitData()
                  this.InitData2()
                  this.dialogCreateOrEditorVisible = false
                }
              })
              .catch((error) => {
                this.$message.error('创建失败！')
              })
          } else {
            this.$store
              .dispatch('page/editorPage', this.EditorPage)
              .then((response) => {
                if (response == true) {
                  this.$message({
                    type: 'success',
                    message: '修改成功！'
                  })
                  this.InitData()
                  this.InitData2()
                  this.$store.dispatch('user/getInfo')
                  this.dialogCreateOrEditorVisible = false
                  this.$store.dispatch('page/getPage3')
                }
              })
              .catch((error) => {
                this.$message.error('修改失败！')
              })
          }
        } else {
          this.$message.error('请填入完整的信息！')
          return false
        }
      })
    },
    ShowAddPage() {
      this.dialogTitle = '新增菜单'
      this.CreateOrEditor = true
      this.dialogCreateOrEditorVisible = true
    },
    // 弹出修改页面窗体
    ShowEditPage(row) {
      this.CreateOrEditor = false
      this.dialogTitle = '修改菜单'
      this.EditorPage.Id = row.Id
      this.EditorPage.PageUrl = row.PageUrl
      this.EditorPage.PageShowName = row.PageShowName
      if (row.ParentPageId == '0') {
        this.EditorPage.ParentPageId = '0'
      } else this.EditorPage.ParentPageId = row.ParentPageId
      this.EditorPage.Icon = row.Icon
      this.EditorPage.AlwaysShow = row.AlwaysShow
      this.EditorPage.Redirect = row.Redirect
      this.EditorPage.NoCache = row.NoCache
      this.dialogCreateOrEditorVisible = true
    },
    // 关闭弹窗事件
    dialogCreateOrEditorVisibleClose() {
      this.EditorPage.Id = ''
      this.EditorPage.PageUrl = ''
      this.EditorPage.PageShowName = ''
      this.EditorPage.ParentPageId = ''
      this.EditorPage.Icon = ''
      this.EditorPage.AlwaysShow = ''
      this.EditorPage.Redirect = ''
      this.EditorPage.NoCache = ''
    },
  }
}
</script>

<style scoped>
.international-icon,
.menu-row-icon {
  font-size: 20px;
  color: #5f6368;
}

.menu-editor-form >>> .el-form-item {
  margin-bottom: 18px;
}

.menu-editor-form >>> .el-form-item__content > .el-input,
.menu-editor-form >>> .el-form-item__content > .el-select,
.menu-editor-form >>> .el-form-item__content > .el-cascader {
  width: 100%;
}

.menu-editor-form >>> .el-form-item__content > span {
  display: block;
  width: 100%;
}
</style>

<style>
.menu-editor-dialog .el-dialog__body {
  padding-top: 12px;
  padding-bottom: 8px;
}
</style>
