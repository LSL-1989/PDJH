<template>
  <div class="app-container role-page">
    <div class="filter-container list-toolbar">
      <div class="list-toolbar__left" />
      <div class="list-toolbar__right">
        <el-button type="primary" icon="el-icon-plus" @click="ShowCreateOrEditor">新增角色</el-button>
      </div>
    </div>

    <!-- 左右分栏：列表与权限树各自滚动，分页固定在左侧底部 -->
    <el-card class="role-split-card">
      <div class="role-split">
        <div class="role-pane">
          <div class="role-list-hint">
            <span v-if="selectedRoleName" class="role-list-hint--active">
              当前选中角色：<strong>{{ selectedRoleName }}</strong>
            </span>
            <span v-else class="role-list-hint--placeholder">请点击角色行以配置权限</span>
          </div>
          <div ref="roleTableWrap" class="role-pane__scroll">
            <el-table
              ref="roleTable"
              class="role-table"
              :data="pagedRoles"
              :height="roleTableHeight"
              border
              highlight-current-row
              :row-class-name="roleRowClassName"
              style="width: 100%"
              @row-click="GetPermission"
            >
              <el-table-column prop="RoleName" label="角色名称" width="120" />
              <el-table-column prop="IsDelete" label="状态" width="120">
                <template #default="scope">
                  <el-tag
                    :type="scope.row.IsDelete === false ? 'success' : 'error'"
                    disable-transitions
                  >{{ scope.row.IsDelete == false ? "启用" : "禁用" }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="CreateDateTime" label="创建时间" />
              <table-action-column :count="3">
                <template #default="{ row, $index }">
                  <table-action-icon icon="el-icon-edit" tip="编辑" @click="ShowEditorRole($index, row)" />
                  <table-action-icon icon="el-icon-user" tip="批量绑定用户" @click="openBindUsers(row)" />
                  <table-action-icon icon="el-icon-delete" tip="删除" type="danger" @click="RemoveRole(row)" />
                </template>
              </table-action-column>
            </el-table>
          </div>
          <el-pagination
            class="role-pagination"
            background
            v-model:current-page="pageIndex"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 30, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="Roles.length"
            @size-change="onRolePageSizeChange"
            @current-change="onRolePageChange"
          />
        </div>

        <div class="permission-pane">
          <div class="permission-div">
            <el-alert
              v-if="selectedRoleName"
              class="permission-role-alert"
              :title="`正在配置「${selectedRoleName}」的权限`"
              type="success"
              :closable="false"
              show-icon
            />
            <el-row type="flex" align="middle">
              <el-col :span="24">
                <el-button
                  type="primary"
                  size="small"
                  :disabled="commitdisabled"
                  @click="CommitPermission()"
                >
                  <span>
                    保存权限
                    <i class="el-icon-upload el-icon--right" />
                  </span>
                </el-button>
                <el-checkbox
                  v-model="selectAllPermissions"
                  class="select-all-permissions"
                  :disabled="!RoleId || !Pagedata || !Pagedata.length"
                  @change="handleSelectAllChange"
                >
                  全选权限
                </el-checkbox>
              </el-col>
            </el-row>
          </div>
          <el-alert
            v-if="permissionTreeLoaded && (!Pagedata || Pagedata.length === 0)"
            type="info"
            :closable="false"
            show-icon
            class="permission-empty-alert"
          >
            当前没有可分配的菜单。权限与
            <router-link :to="{ name: 'maintenanceMenu' }">菜单管理</router-link>
            中的条目一致，请先在菜单管理中维护菜单后再回到本页勾选并保存权限。
          </el-alert>
          <!-- 权限目录树 -->
          <div class="permission-tree">
            <el-tree
              ref="tree"
              v-loading="loadingBool"
              :data="Pagedata"
              show-checkbox
              node-key="id"
              :default-expand-all="true"
              :default-checked-keys="checkedkeys"
              :props="defaultProps"
              @check="onPermissionTreeCheck"
            />
          </div>
        </div>
      </div>
    </el-card>

    <form-drawer
      :title="CreateOrEditorTile"
      v-model="CreateOrEditordialogVisible"
      @close="CreateOrEditorColose"
    >
      <el-form
        ref="form"
        :model="EditorRole"
        label-position="right"
        :rules="rules"
        size="small"
        label-width="100px"
      >
        <el-form-item label="角色名称" prop="RoleName">
          <el-input v-model="EditorRole.RoleName" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio v-model="EditorRole.IsDelete" label="0" border>启用</el-radio>
          <el-radio v-model="EditorRole.IsDelete" label="1" border>禁用</el-radio>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="CreateOrEditordialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="EditorRoles('form')">确 定</el-button>
      </template>
    </form-drawer>

    <form-drawer
      :title="bindDrawerTitle"
      v-model="bindVisible"
      size="760px"
      @closed="resetBindDrawer"
    >
      <div class="bind-toolbar">
        <el-input
          v-model="bindQuery.keyword"
          placeholder="帐号/工号 / 姓名 / 手机号"
          clearable
          style="width: 220px;"
          @keyup.enter="searchBindUsers"
        />
        <el-select v-model="bindQuery.loginScene" placeholder="使用场景" clearable style="width: 160px;">
          <el-option
            v-for="item in loginSceneOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="bindQuery.roleId" placeholder="已绑定角色" clearable style="width: 160px;">
          <el-option
            v-for="item in Roles"
            :key="item.RoleId"
            :label="item.RoleName"
            :value="item.RoleId"
          />
        </el-select>
        <el-button type="primary" icon="el-icon-search" @click="searchBindUsers">查询</el-button>
        <el-button @click="resetBindQuery">重置</el-button>
      </div>
      <p class="bind-hint">已绑定「{{ bindRoleName }}」的用户默认勾选。确定后只处理勾选用户：未绑定本角色的会绑定，已绑定其他角色的会更换；取消勾选不会解除原有绑定。</p>
      <el-table
        ref="bindTable"
        v-loading="bindLoading"
        :data="bindUsers"
        row-key="UserNo"
        border
        max-height="420"
        @selection-change="onBindSelectionChange"
      >
        <el-table-column type="selection" width="48" reserve-selection />
        <el-table-column prop="UserNo" label="帐号/工号" min-width="120" show-overflow-tooltip />
        <el-table-column prop="UserName" label="姓名" min-width="100" show-overflow-tooltip />
        <el-table-column prop="Phone" label="手机" min-width="120" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ scope.row.Phone || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="LoginSceneName" label="使用场景" min-width="120" show-overflow-tooltip />
        <el-table-column label="已绑定角色" min-width="120" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ scope.row.RoleName || scope.row.Role || '未绑定' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="bind-pagination"
        background
        :current-page="bindPage"
        :page-size="bindPageSize"
        :page-sizes="[10, 20, 30, 50]"
        layout="total, sizes, prev, pager, next"
        :total="bindTotal"
        @size-change="onBindPageSizeChange"
        @current-change="onBindPageChange"
      />
      <template #footer>
        <span class="bind-selected-count">已选 {{ bindSelectedNos.length }} 人</span>
        <el-button :loading="bindSaving" :disabled="!bindRoleId || bindRoleDisabled" type="primary" @click="bindSelectedToCurrentRole">
          绑定到当前角色
        </el-button>
        <el-select v-model="bindTargetRoleId" placeholder="更换为" clearable style="width: 150px; margin: 0 8px;">
          <el-option
            v-for="item in enabledRoles"
            :key="item.RoleId"
            :label="item.RoleName"
            :value="item.RoleId"
          />
        </el-select>
        <el-button :loading="bindSaving" :disabled="!bindTargetRoleId" @click="changeSelectedUsersRole">更换角色</el-button>
      </template>
    </form-drawer>
  </div>
</template>

<script>
import { ElMessageBox } from 'element-plus'

/** 与用户管理页一致，对应后端 LoginScene */
const LOGIN_SCENE_OPTIONS = [
  { value: 'ADMIN', label: '后台管理' },
  { value: 'NURSE', label: '护士站' },
  { value: 'DOCTOR', label: '叫号器（门诊）' },
  { value: 'PHARMACY', label: '叫号器（药房）' },
  { value: 'LAB', label: '叫号器（检验）' },
  { value: 'EXAM', label: '叫号器（医技）' }
]

/**
 * 角色管理页：左侧角色列表分页，右侧菜单权限树；两栏在视口内各自滚动。
 * 消费者：后台「系统与权限管理 / 角色管理」。
 * 列表复用 Role/GetRole；删角色前用 User/GetUsers 判断绑定；批量绑定复用 GetUsers 与 EditorUser。
 */
export default {
  name: 'RoleMaintenance',
  data() {
    return {
      checkedkeys: [],
      Roles: [],
      pageIndex: 1,
      pageSize: 10,
      roleTableHeight: 320,
      RoleId: '',
      selectedRoleName: '',
      CreateOrEditordialogVisible: false,
      AdddialogVisible: false,
      commitdisabled: false,
      loadingBool: false,
      CreateOrEditorTile: '？',
      CreateOrEditor: false, // 为true 新增 为false 修改
      formLabelWidth: '100px',
      EditorRole: {
        RoleId: '',
        RoleName: '',
        IsDelete: '1'
      },
      rules: {
        RoleName: [
          { required: true, message: '此项不能为空', trigger: 'change' }
        ]
      },

      Pagedata: [],
      permissionTreeLoaded: false,
      selectAllPermissions: false,
      defaultProps: {
        children: 'children',
        label: 'label'
      },
      loginSceneOptions: LOGIN_SCENE_OPTIONS,
      bindVisible: false,
      bindRoleId: '',
      bindRoleName: '',
      bindRoleDisabled: false,
      bindLoading: false,
      bindSaving: false,
      bindUsers: [],
      bindTotal: 0,
      bindPage: 1,
      bindPageSize: 10,
      bindQuery: {
        keyword: '',
        loginScene: '',
        roleId: ''
      },
      bindSelectedNos: [],
      bindTargetRoleId: '',
      bindSyncing: false,
      bindRowCache: {}
    }
  },
  computed: {
    /** 当前页角色；总条数用 Roles.length，不另请求分页接口 */
    pagedRoles() {
      const start = (this.pageIndex - 1) * this.pageSize
      return this.Roles.slice(start, start + this.pageSize)
    },
    bindDrawerTitle() {
      return this.bindRoleName ? `批量绑定用户 — ${this.bindRoleName}` : '批量绑定用户'
    },
    enabledRoles() {
      return this.Roles.filter((item) => item.IsDelete === false)
    }
  },
  mounted() {
    this.Init()
    this.$nextTick(() => this.observeRoleTableWrap())
  },
  beforeUnmount() {
    if (this.roleTableObserver) {
      this.roleTableObserver.disconnect()
      this.roleTableObserver = null
    }
  },
  methods: {
    /** 让角色表格高度贴合左侧滚动区，表体单独滚动，表头保持可见 */
    observeRoleTableWrap() {
      const el = this.$refs.roleTableWrap
      if (!el || typeof ResizeObserver === 'undefined') return
      const update = () => {
        const next = el.clientHeight
        if (next > 0 && next !== this.roleTableHeight) {
          this.roleTableHeight = next
        }
      }
      this.roleTableObserver = new ResizeObserver(update)
      this.roleTableObserver.observe(el)
      update()
    },
    formatRoleRows(list) {
      if (!Array.isArray(list)) return []
      list.forEach((item) => {
        const dt = item.CreateDateTime != null ? item.CreateDateTime : item.createDateTime
        if (dt != null && typeof dt === 'string' && dt.indexOf('T') !== -1) {
          item.CreateDateTime = dt.replace('T', ' ')
        }
      })
      return list
    },
    clampRolePage() {
      const maxPage = Math.max(1, Math.ceil(this.Roles.length / this.pageSize) || 1)
      if (this.pageIndex > maxPage) this.pageIndex = maxPage
      if (this.pageIndex < 1) this.pageIndex = 1
    },
    /** 刷新后把已选角色所在页露出来；翻页本身不调用，避免把用户拽回原页 */
    focusSelectedRolePage() {
      if (this.RoleId) {
        const index = this.Roles.findIndex((row) => row.RoleId === this.RoleId)
        if (index >= 0) {
          this.pageIndex = Math.floor(index / this.pageSize) + 1
        } else {
          this.clampRolePage()
        }
      } else {
        this.clampRolePage()
      }
      this.restoreSelectedRoleRow()
    },
    onRolePageSizeChange() {
      this.pageIndex = 1
      this.restoreSelectedRoleRow()
    },
    onRolePageChange() {
      this.restoreSelectedRoleRow()
      this.$nextTick(() => {
        if (this.$refs.roleTable && this.$refs.roleTable.setScrollTop) {
          this.$refs.roleTable.setScrollTop(0)
        }
      })
    },
    // 获取角色
    GetRole() {
      this.$store
        .dispatch('role/getRole')
        .then((response) => {
          this.Roles = this.formatRoleRows(response)
          this.focusSelectedRolePage()
        })
        .catch((error) => {
          this.$message.error('服务器错误，无法获取角色信息！')
        })
    },
    roleRowClassName({ row }) {
      return row.RoleId === this.RoleId ? 'role-row-selected' : ''
    },
    restoreSelectedRoleRow() {
      this.$nextTick(() => {
        if (!this.$refs.roleTable || !this.RoleId) return
        const row = this.pagedRoles.find((r) => r.RoleId === this.RoleId)
        if (row) {
          this.$refs.roleTable.setCurrentRow(row)
          this.selectedRoleName = row.RoleName
        }
      })
    },
    /**
     * 删除角色：先用 User/GetUsers 按 roleId 查绑定人数。
     * 已绑定则只提示、不删除；未绑定再二次确认后调用 Role/RemoveRole。
     */
    RemoveRole(row) {
      this.$store
        .dispatch('user/getAllUser', {
          pageindex: 1,
          pagesize: 8,
          roleId: row.RoleId
        })
        .then((res) => {
          const users = (res && res.data) || []
          const total = (res && res.pageInfo && res.pageInfo.totalCount) || 0
          if (total > 0) {
            ElMessageBox.alert(
              this.formatBoundUserTip(row.RoleName, users, total),
              '该角色已绑定用户',
              { confirmButtonText: '知道了', type: 'warning' }
            )
            return
          }
          this.confirmRemoveUnboundRole(row)
        })
        .catch(() => {
          this.$message.error('无法确认该角色是否已绑定用户，已取消删除')
        })
    },
    formatBoundUserTip(roleName, users, total) {
      const names = users.map((user) => {
        if (user.UserName && user.UserNo) return `${user.UserName}（${user.UserNo}）`
        return user.UserName || user.UserNo || ''
      }).filter(Boolean)
      const shown = names.join('、')
      const tail = total > names.length ? `等共 ${total} 人` : `共 ${total} 人`
      return `角色「${roleName}」已绑定用户：${shown}，${tail}。请先通过「批量绑定用户」把这些用户更换到其他角色后再删除。`
    },
    confirmRemoveUnboundRole(row) {
      this.$confirm(`角色「${row.RoleName}」当前没有绑定用户，确定删除？`, '删除确认', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.$store
            .dispatch('role/romoveRole', row.RoleId)
            .then((response) => {
              if (response == true) {
                if (this.RoleId === row.RoleId) {
                  this.RoleId = ''
                  this.selectedRoleName = ''
                }
                this.$message.success('删除成功!')
                this.GetRole()
              } else {
                this.$message.error('删除失败！')
              }
            })
            .catch((error) => {
              this.$message.error((error && error.message) || '服务器错误，无法删除角色!')
            })
        })
        .catch(() => {
          this.$message.info('已取消删除')
        })
    },
    /** 打开批量绑定抽屉，并默认勾选已绑定当前角色的用户 */
    openBindUsers(row) {
      this.bindRoleId = row.RoleId
      this.bindRoleName = row.RoleName
      this.bindRoleDisabled = row.IsDelete !== false
      this.bindTargetRoleId = ''
      this.bindQuery = { keyword: '', loginScene: '', roleId: '' }
      this.bindPage = 1
      this.bindPageSize = 10
      this.bindSelectedNos = []
      this.bindRowCache = {}
      this.bindVisible = true
      this.seedBindSelection(row.RoleId)
    },
    resetBindDrawer() {
      this.bindUsers = []
      this.bindTotal = 0
      this.bindSelectedNos = []
      this.bindRowCache = {}
      this.bindSyncing = false
    },
    /** 拉齐已绑定本角色的用户并写入勾选；与列表筛选分开，避免筛选把默认勾选漏掉 */
    seedBindSelection(roleId) {
      this.collectUsersByRole(roleId)
        .then((users) => {
          this.bindRowCache = {}
          users.forEach((user) => {
            if (user.UserNo) this.bindRowCache[user.UserNo] = user
          })
          this.bindSelectedNos = users.map((user) => user.UserNo).filter(Boolean)
          this.loadBindUsers()
        })
        .catch(() => {
          this.$message.error('无法加载已绑定用户')
          this.loadBindUsers()
        })
    },
    collectUsersByRole(roleId) {
      const pageSize = 200
      const loadPage = (page, acc) => {
        return this.$store.dispatch('user/getAllUser', {
          pageindex: page,
          pagesize: pageSize,
          roleId
        }).then((res) => {
          const rows = (res && res.data) || []
          const total = (res && res.pageInfo && res.pageInfo.totalCount) || 0
          const next = acc.concat(rows)
          if (next.length < total && rows.length > 0 && page < 20) {
            return loadPage(page + 1, next)
          }
          return next
        })
      }
      return loadPage(1, [])
    },
    searchBindUsers() {
      this.bindPage = 1
      this.loadBindUsers()
    },
    resetBindQuery() {
      this.bindQuery = { keyword: '', loginScene: '', roleId: '' }
      this.searchBindUsers()
    },
    onBindPageSizeChange(size) {
      this.bindPageSize = size
      this.bindPage = 1
      this.loadBindUsers()
    },
    onBindPageChange(page) {
      this.bindPage = page
      this.loadBindUsers()
    },
    loadBindUsers() {
      this.bindLoading = true
      const query = {
        pageindex: this.bindPage,
        pagesize: this.bindPageSize
      }
      const keyword = this.bindQuery.keyword != null ? String(this.bindQuery.keyword).trim() : ''
      if (keyword) query.keyword = keyword
      if (this.bindQuery.loginScene) query.loginScene = this.bindQuery.loginScene
      if (this.bindQuery.roleId) query.roleId = this.bindQuery.roleId
      this.$store
        .dispatch('user/getAllUser', query)
        .then((res) => {
          this.bindUsers = (res && res.data) || []
          this.bindTotal = (res && res.pageInfo && res.pageInfo.totalCount) || 0
          this.bindUsers.forEach((user) => {
            if (user.UserNo) {
              this.bindRowCache[user.UserNo] = Object.assign({}, this.bindRowCache[user.UserNo], user)
            }
          })
          this.applyBindSelection()
        })
        .catch(() => {
          this.bindUsers = []
          this.bindTotal = 0
          this.$message.error('无法加载用户列表')
        })
        .finally(() => {
          this.bindLoading = false
        })
    },
    applyBindSelection() {
      this.$nextTick(() => {
        const table = this.$refs.bindTable
        if (!table) return
        this.bindSyncing = true
        table.clearSelection()
        this.bindUsers.forEach((row) => {
          if (this.bindSelectedNos.indexOf(row.UserNo) !== -1) {
            table.toggleRowSelection(row, true)
          }
        })
        this.$nextTick(() => {
          this.bindSyncing = false
        })
      })
    },
    onBindSelectionChange(selection) {
      if (this.bindSyncing) return
      const selectedOnPage = {}
      ;(selection || []).forEach((row) => {
        if (row && row.UserNo) selectedOnPage[row.UserNo] = true
      })
      const next = {}
      this.bindSelectedNos.forEach((userNo) => {
        next[userNo] = true
      })
      this.bindUsers.forEach((row) => {
        if (!row.UserNo) return
        if (selectedOnPage[row.UserNo]) next[row.UserNo] = true
        else delete next[row.UserNo]
      })
      this.bindSelectedNos = Object.keys(next)
    },
    bindSelectedToCurrentRole() {
      if (this.bindRoleDisabled) {
        this.$message.warning('当前角色已禁用，不能绑定用户')
        return
      }
      this.applyRoleToSelected(this.bindRoleId, this.bindRoleName)
    },
    changeSelectedUsersRole() {
      const role = this.enabledRoles.find((item) => item.RoleId === this.bindTargetRoleId)
      if (!role) {
        this.$message.warning('请选择要更换的角色')
        return
      }
      this.applyRoleToSelected(role.RoleId, role.RoleName)
    },
    /**
     * 把勾选用户的角色改为 targetRoleId。已是该角色的跳过。
     * 只提交 UserNo + RoleId，走 User/EditorUser，避免另建批量接口。
     */
    applyRoleToSelected(targetRoleId, targetRoleName) {
      if (!this.bindSelectedNos.length) {
        this.$message.warning('请先勾选用户')
        return
      }
      const toChange = this.bindSelectedNos.filter((userNo) => {
        const cached = this.bindRowCache[userNo]
        return !cached || cached.RoleId !== targetRoleId
      })
      if (!toChange.length) {
        this.$message.info(`所选用户已绑定「${targetRoleName}」`)
        return
      }
      ElMessageBox.confirm(
        `将把 ${toChange.length} 名用户绑定到「${targetRoleName}」。已是该角色的不会重复修改，未勾选的用户不会解除绑定。`,
        '确认绑定',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
      )
        .then(() => {
          this.bindSaving = true
          const failed = []
          const run = (index) => {
            if (index >= toChange.length) {
              this.bindSaving = false
              if (failed.length) {
                this.$message.error(`成功 ${toChange.length - failed.length} 人，失败 ${failed.length} 人：${failed.join('、')}`)
              } else {
                this.$message.success(`已将 ${toChange.length} 名用户绑定到「${targetRoleName}」`)
              }
              this.loadBindUsers()
              return
            }
            const userNo = toChange[index]
            this.$store
              .dispatch('user/editorUser', { UserNo: userNo, RoleId: targetRoleId })
              .then(() => {
                const prev = this.bindRowCache[userNo] || { UserNo: userNo }
                this.bindRowCache[userNo] = Object.assign({}, prev, {
                  RoleId: targetRoleId,
                  RoleName: targetRoleName,
                  Role: targetRoleName
                })
                run(index + 1)
              })
              .catch(() => {
                failed.push(userNo)
                run(index + 1)
              })
          }
          run(0)
        })
        .catch(() => {})
    },
    // 获取菜单数据 用于权限目录树
    GetPage() {
      this.loadingBool = true
      this.$store
        .dispatch('page/getPage3')
        .then((response) => {
          this.Pagedata = response
        })
        .catch((error) => {
          this.$message.error('无法加载权限目录树！')
        })
        .finally(() => {
          this.loadingBool = false
          this.permissionTreeLoaded = true
        })
    },
    // 保存权限
    CommitPermission() {
      if (this.RoleId && this.RoleId != '') {
        this.commitdisabled = true
        this.loadingBool = true

        // 仅保存完全勾选的节点；半选父节点不入库，避免回显时父节点级联勾选全部子节点
        const pages = this.normalizePermissionKeys(
          this.$refs.tree.getCheckedKeys()
        )

        this.$store
          .dispatch('role/commitPermission', {
            Pages: pages,
            RoleId: this.RoleId
          })
          .then((response) => {
            if (response == true) {
              this.$message({
                type: 'success',
                message: '保存成功！'
              })
              this.commitdisabled = false
              this.loadingBool = false
              this.$store.dispatch('user/getInfo')
            }
          })
          .catch((error) => {
            this.commitdisabled = false
            this.loadingBool = false
            this.$message.error('保存失败！')
          })
      } else {
        this.$message({
          type: 'warning',
          message: '请选择一个角色'
        })
      }
    },
    normalizePermissionKey(key) {
      return key != null ? String(key).toUpperCase() : ''
    },
    normalizePermissionKeys(keys) {
      return (keys || [])
        .map((key) => this.normalizePermissionKey(key))
        .filter(Boolean)
    },
    collectAllNodeKeys(nodes) {
      const keys = []
      const walk = (list) => {
        if (!list || !list.length) return
        list.forEach((node) => {
          if (node.id != null) keys.push(node.id)
          if (node.children && node.children.length) walk(node.children)
        })
      }
      walk(nodes)
      return keys
    },
    // 历史数据可能含“半选父节点”id，回显前剔除，否则会级联勾选其下全部子节点
    sanitizePermissionKeysForLoad(keys) {
      const keySet = new Set(this.normalizePermissionKeys(keys))
      const dropParentIds = new Set()
      const walk = (nodes) => {
        if (!nodes || !nodes.length) return
        nodes.forEach((node) => {
          const children = node.children || []
          if (!children.length) return
          const descIds = this.collectAllNodeKeys(children)
          if (
            keySet.has(node.id) &&
            descIds.length &&
            !descIds.every((id) => keySet.has(id))
          ) {
            dropParentIds.add(node.id)
          }
          walk(children)
        })
      }
      walk(this.Pagedata)
      return Array.from(keySet).filter((id) => !dropParentIds.has(id))
    },
    // 子节点全部在权限列表中时，显式勾选父节点（避免仅回显子节点时父节点未选中）
    ensureParentChecks(keySet) {
      if (!this.$refs.tree || !this.Pagedata) return
      this.Pagedata.forEach((parent) => {
        const children = parent.children || []
        if (!children.length) return
        const allChildrenChecked = children.every((child) => keySet.has(child.id))
        if (allChildrenChecked) {
          this.$refs.tree.setChecked(parent.id, true, false)
        }
      })
    },
    applyPermissionKeysToTree(keys) {
      if (!this.$refs.tree) return
      const loadKeys = this.sanitizePermissionKeysForLoad(keys)
      const keySet = new Set(loadKeys)
      this.$refs.tree.setCheckedKeys(loadKeys, false)
      this.$nextTick(() => {
        this.ensureParentChecks(keySet)
        this.updateSelectAllChecked()
      })
    },
    updateSelectAllChecked() {
      if (!this.$refs.tree || !this.Pagedata || !this.Pagedata.length) {
        this.selectAllPermissions = false
        return
      }
      const allKeys = this.collectAllNodeKeys(this.Pagedata)
      if (!allKeys.length) {
        this.selectAllPermissions = false
        return
      }
      const checkedSet = new Set(this.$refs.tree.getCheckedKeys())
      this.selectAllPermissions = allKeys.every((id) => checkedSet.has(id))
    },
    handleSelectAllChange(checked) {
      if (!this.$refs.tree) return
      if (checked) {
        this.$refs.tree.setCheckedKeys(this.collectAllNodeKeys(this.Pagedata))
      } else {
        this.$refs.tree.setCheckedKeys([])
      }
    },
    onPermissionTreeCheck() {
      this.$nextTick(() => {
        this.updateSelectAllChecked()
      })
    },
    // 每一行的点击事件
    GetPermission(row) {
      this.RoleId = row.RoleId
      this.selectedRoleName = row.RoleName
      if (this.$refs.roleTable) {
        this.$refs.roleTable.setCurrentRow(row)
      }
      this.loadingBool = true
      this.$store
        .dispatch('role/getPermission', row.RoleId)
        .then((response) => {
          const keys = response || []
          this.checkedkeys = keys
          this.$nextTick(() => {
            this.applyPermissionKeysToTree(keys)
          })
          this.loadingBool = false
        })
        .catch((error) => {
          this.$message.error(`无法获取${row.RoleName}的权限!`)
          this.loadingBool = false
        })
    },
    // 新增修改窗体关闭事件
    CreateOrEditorColose() {
      this.EditorRole.RoleName = ''
      this.EditorRole.IsDelete = '0'
    },
    // 弹出新增窗体
    ShowCreateOrEditor() {
      this.CreateOrEditorTile = '新增角色'
      this.CreateOrEditor = true
      this.CreateOrEditordialogVisible = true
    },
    // 弹出修改角色窗体
    ShowEditorRole(index, row) {
      this.CreateOrEditorTile = '修改角色'
      this.CreateOrEditor = false
      this.EditorRole.RoleId = row.RoleId
      this.EditorRole.RoleName = row.RoleName
      this.EditorRole.IsDelete = row.IsDelete == false ? '0' : '1'
      this.CreateOrEditordialogVisible = true
    },
    CreateRole() {},
    // 新增/修改
    EditorRoles(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          if (this.CreateOrEditor) {
            this.Create()
          } else {
            this.Editor()
          }
        } else {
          this.$message.error('修改错误！')
          return false
        }
      })
    },
    Create() {
      this.$store
        .dispatch('role/createRole', this.EditorRole)
        .then((response) => {
          if (response == true) {
            this.CreateOrEditordialogVisible = false
            this.$message({
              message: '新增成功！',
              type: 'success'
            })
            this.GetRole()
          } else {
            this.$message.error('新增失败！')
          }
        })
        .catch((error) => {
          this.$message.error('服务器错误，无法新增角色!')
        })
    },
    Editor() {
      this.$store
        .dispatch('role/editorRole', this.EditorRole)
        .then((response) => {
          if (response == true) {
            this.CreateOrEditordialogVisible = false
            this.$message({
              message: '修改成功！',
              type: 'success'
            })
            this.GetRole()
          } else {
            this.$message.error('修改失败！')
          }
        })
        .catch((error) => {
          this.$message.error('服务器错误，无法修改角色信息!')
        })
    },
    Init() {
      if (
        this.$store.state.role.RoleArry &&
        this.$store.state.role.RoleArry.length > 0
      ) {
        this.Roles = this.formatRoleRows(this.$store.state.role.RoleArry)
        this.focusSelectedRolePage()
      } else {
        this.GetRole()
      }

      if (
        this.$store.state.page.PageMenuArry3 &&
        this.$store.state.page.PageMenuArry3.length > 0
      ) {
        this.Pagedata = this.$store.state.page.PageMenuArry3
        this.permissionTreeLoaded = true
      } else {
        this.GetPage()
      }
    }
  }
}
</script>

<style scoped>
.role-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
}
.role-page .list-toolbar {
  flex-shrink: 0;
}
.role-split-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.role-split-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
.role-split {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 16px;
}
.role-pane,
.permission-pane {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}
.role-pane {
  flex: 16;
}
.permission-pane {
  flex: 8;
  padding-left: 16px;
  border-left: 1px solid #ebeef5;
}
.role-pane__scroll {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.role-pagination {
  flex-shrink: 0;
  margin-top: 12px;
}
.role-list-hint {
  flex-shrink: 0;
  margin-bottom: 10px;
  font-size: 13px;
  line-height: 1.5;
}
.role-list-hint--active {
  color: #409eff;
}
.role-list-hint--active strong {
  font-size: 14px;
}
.role-list-hint--placeholder {
  color: #909399;
}
.role-table >>> .el-table__body tr.current-row > td,
.role-table >>> .el-table__body tr.role-row-selected > td {
  background-color: #c6e2ff !important;
  color: #303133;
}
.role-table >>> .el-table__body tr.current-row > td:first-child,
.role-table >>> .el-table__body tr.role-row-selected > td:first-child {
  box-shadow: inset 3px 0 0 #409eff;
}
.permission-div {
  flex-shrink: 0;
}
.permission-role-alert {
  margin-bottom: 10px;
}
.permission-empty-alert {
  flex-shrink: 0;
  margin: 8px 0;
}
.select-all-permissions {
  margin-left: 12px;
}
.permission-tree {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.bind-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.bind-hint {
  margin: 0 0 10px;
  color: #909399;
  font-size: 13px;
  line-height: 1.5;
}
.bind-pagination {
  margin-top: 12px;
}
.bind-selected-count {
  margin-right: auto;
  color: #606266;
  font-size: 13px;
}
</style>
