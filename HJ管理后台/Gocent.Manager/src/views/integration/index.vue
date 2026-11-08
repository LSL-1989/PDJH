<template>
  <!--
    数据源接口配置：连接（HTTP / JDBC / WEBHOOK）→ 端点 → 字段映射 → Ping / 整表试跑。
    消费者：集成网关管理。依赖 /api/integration/config 与 /marketplace。
  -->
  <div class="app-container">
    <h2>数据源接口配置</h2>
    <p style="color:#909399;margin-top:-8px;">主动拉取用 HTTP，只读视图用 JDBC，院方推送用 WEBHOOK。定时拉取只记审计，不会自动入队。</p>

    <!-- Connections Section -->
    <div class="filter-container list-toolbar">
      <div class="list-toolbar__left">
        <el-button icon="el-icon-refresh" @click="fetchConnections">刷新</el-button>
      </div>
      <div class="list-toolbar__right">
        <el-button type="primary" icon="el-icon-plus" @click="handleCreateConnection">新增数据源</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="connectionList" border stripe>
      <el-table-column prop="name" label="数据源名称" width="200" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{row}">
          <el-tag :type="row.type === 'HTTP' ? 'success' : 'warning'">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="baseUrl" label="连接地址/URL" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{row}">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="toggleConnStatus(row)" />
        </template>
      </el-table-column>
      <table-action-column :count="4">
        <template #default="{ row }">
          <table-action-icon icon="el-icon-odometer" tip="Ping测试" type="success" @click="pingConn(row.id)" />
          <table-action-icon icon="el-icon-setting" tip="管理业务端点" @click="openEndpoints(row)" />
          <table-action-icon icon="el-icon-edit" tip="编辑" @click="handleEditConnection(row)" />
          <table-action-icon icon="el-icon-delete" tip="删除" type="danger" @click="deleteConn(row.id)" />
        </template>
      </table-action-column>
    </el-table>

    <form-drawer :title="connForm.id ? '编辑数据源' : '新建数据源'" v-model="dialogConnVisible">
      <el-form ref="connFormRef" :model="connForm" label-width="120px">
        <el-form-item label="名称" required><el-input v-model="connForm.name" /></el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="connForm.type" style="width:100%">
            <el-option label="HTTP（主动拉取）" value="HTTP" />
            <el-option label="JDBC（只读数据库）" value="JDBC" />
            <el-option label="WEBHOOK（被动接收）" value="WEBHOOK" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="connForm.type!=='WEBHOOK'" label="请求地址" required>
          <el-input v-model="connForm.baseUrl" :placeholder="connForm.type==='JDBC' ? 'jdbc:mysql://...' : 'https://api.his.local/v1'" />
        </el-form-item>
        <el-form-item v-if="connForm.type==='HTTP'" label="鉴权头">
          <el-input v-model="connForm.username" placeholder="Bearer Token，写入 Authorization" />
        </el-form-item>
        <el-form-item v-if="connForm.type==='JDBC'" label="用户名"><el-input v-model="connForm.username" placeholder="只读账号" /></el-form-item>
        <el-form-item v-if="connForm.type==='JDBC'" label="密码">
          <el-input v-model="connForm.password" type="password" show-password :placeholder="connForm.id ? '留空则不修改' : ''" />
        </el-form-item>
        <el-form-item v-if="connForm.type==='JDBC'" label="驱动类"><el-input v-model="connForm.driverClass" placeholder="com.mysql.cj.jdbc.Driver" /></el-form-item>
        <el-form-item v-if="connForm.type==='WEBHOOK'" label="来源 IP 白名单">
          <el-input v-model="connForm.allowIps" type="textarea" :rows="3" placeholder="留空不限制。多个 IP 用逗号分隔" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="connForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogConnVisible = false">取消</el-button>
        <el-button type="primary" @click="submitConnection">确定</el-button>
      </template>
    </form-drawer>

    <!-- Endpoints Drawer -->
    <el-drawer :title="`管理业务端点 - ${currentConnection.name || ''}`" v-model="drawerEndpointsVisible" size="70%">
      <div style="padding: 20px;">
        <el-button type="primary" size="small" icon="el-icon-plus" style="margin-bottom:15px" @click="handleCreateEndpoint">新增业务端点</el-button>
        <el-button type="warning" size="small" icon="el-icon-download" style="margin-left:10px; margin-bottom:15px" @click="openMarketplace">从行业模板库导入</el-button>
        <el-table :data="endpointList" border>
          <el-table-column prop="name" label="业务点" width="180" />
          <el-table-column prop="endpointCode" label="端点编码" width="180" />
          <el-table-column prop="method" label="方式(GET/POLLING)" width="150" />
          <el-table-column label="运行环境" width="130" align="center">
            <template #default="{row}">
              <el-switch
                v-model="row.envMode"
                :active-value="1"
                :inactive-value="0"
                active-text="测试"
                inactive-text="生产"
                active-color="#e6a23c"
                inactive-color="#13ce66"
                @change="handleEnvModeSwitch(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{row}">
              <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="toggleEndpointStatus(row)" />
            </template>
          </el-table-column>
          <el-table-column prop="path" label="路径/SQL" show-overflow-tooltip />
          <table-action-column label="动作" :count="5">
            <template #default="{ row }">
              <table-action-icon icon="el-icon-edit" tip="编辑" @click="handleEditEndpoint(row)" />
              <table-action-icon icon="el-icon-share" tip="字段映射规则" type="warning" @click="openMappings(row)" />
              <table-action-icon icon="el-icon-cpu" tip="整表试跑" type="success" @click="openDryRun(row)" />
              <table-action-icon icon="el-icon-time" tip="版本快照" type="info" @click="openHistory(row)" />
              <table-action-icon icon="el-icon-delete" tip="删除" type="danger" @click="deleteEp(row)" />
            </template>
          </table-action-column>
        </el-table>
      </div>

      <form-drawer :title="epForm.id ? '编辑端点' : '新建端点'" v-model="dialogEndpointVisible" size="520px" append-to-body>
        <el-form :model="epForm" label-width="120px">
          <el-form-item label="端点名称"><el-input v-model="epForm.name" placeholder="获取挂号信息" /></el-form-item>
          <el-form-item label="端点编码"><el-input v-model="epForm.endpointCode" placeholder="HIS_GET_REG" /></el-form-item>
          <el-form-item label="请求方式">
            <el-select v-model="epForm.method" style="width:100%" :disabled="methodOptions.length===1">
              <el-option v-for="opt in methodOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <p v-if="epForm.method==='POLLING'" style="color:#909399;font-size:12px;margin:-6px 0 12px 120px;">定时拉取只记审计，不会自动入队。</p>
          <p v-if="epForm.method==='WEBHOOK'" style="color:#909399;font-size:12px;margin:-6px 0 12px 120px;">自定义编码接收 /api/integration/webhook/{端点编码}。HIS_REGISTER、EXAM_STATUS 只给固定挂号和检查完成接口挂 IP 白名单。</p>
          <el-form-item v-if="epForm.method!=='WEBHOOK'" label="运行环境">
            <el-radio-group v-model="epForm.envMode">
              <el-radio :label="0">生产</el-radio>
              <el-radio :label="1">Mock</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="epForm.method!=='WEBHOOK' && epForm.envMode !== 1" :label="epForm.method==='POLLING' ? 'SELECT 语句' : '请求路径'">
            <el-input v-model="epForm.path" type="textarea" placeholder="/api/reg?code={patientCode} 或 SELECT ..." />
          </el-form-item>
          <el-form-item v-if="epForm.method==='WEBHOOK'" label="回调路径说明">
            <el-input v-model="epForm.path" type="textarea" placeholder="/integration/webhook/his/register" />
          </el-form-item>
          <el-form-item v-if="epForm.envMode === 1 && epForm.method!=='WEBHOOK'" label="Mock 路径">
            <el-input v-model="epForm.mockPath" type="textarea" placeholder="/mock/his/patients/today" />
          </el-form-item>
          <el-form-item v-if="epForm.method==='POLLING'" label="Cron表达式"><el-input v-model="epForm.cronExpression" placeholder="0/30 * * * * ?" /></el-form-item>
          <el-form-item label="启用">
            <el-switch v-model="epForm.status" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogEndpointVisible = false">取消</el-button>
          <el-button type="primary" @click="submitEndpoint">保存端点</el-button>
        </template>
      </form-drawer>
    </el-drawer>

    <!-- Mappings Dialog -->
    <el-dialog :title="`字段映射 (${currentEndpoint.endpointCode || ''})`" v-model="dialogMappingVisible" width="980px" append-to-body>
      <div style="display:flex; justify-content:space-between; margin-bottom: 10px;">
        <span style="color:gray;font-size:12px">源字段用 JSONPath 或数据库列名。表达式支持 CONST:固定值 与 MAP:原值=新值。</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="addMappingRow">添加规则</el-button>
      </div>
      <el-table :data="mappingList" border>
        <el-table-column label="源字段" min-width="180">
          <template #default="scope">
            <el-input v-model="scope.row.sourceField" placeholder="$.data[*].name 或 pat_id" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="目标字段" min-width="200">
          <template #default="scope">
            <el-select v-model="scope.row.targetField" filterable allow-create default-first-option placeholder="选择或输入" size="small" style="width:100%">
              <el-option-group v-for="group in targetFieldGroups" :key="group.label" :label="group.label">
                <el-option v-for="opt in group.options" :key="group.label + opt.value" :label="opt.label" :value="opt.value" />
              </el-option-group>
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="表达式" width="170">
          <template #default="scope">
            <el-input v-model="scope.row.ruleExpression" placeholder="CONST: 或 MAP:" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="说明" width="140">
          <template #default="scope">
            <el-input v-model="scope.row.description" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button text @click="saveMappingEntity(scope.row)">保存</el-button>
            <el-button text style="color:red" @click="saveOrDelMapping(scope.row, scope.$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- History Drawer -->
    <el-drawer :title="`版本快照及回滚 - [${currentEndpoint.name || ''}]`" v-model="drawerHistoryVisible" size="50%" append-to-body>
      <div style="padding: 20px;">
        <el-alert title="注意：一键回滚将覆盖当前的所有端点信息及对应字段的映射规则，并自动产生一条回滚快照。请谨慎操作。" type="warning" show-icon style="margin-bottom:15px;" />
        <el-table :data="historyList" border>
          <el-table-column prop="createTime" label="快照时间" width="180">
            <template #default="{row}">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="operator" label="操作人" width="100" />
          <el-table-column prop="operationType" label="操作类型" width="150">
            <template #default="{row}">
              <el-tag size="small">{{ row.operationType }}</el-tag>
            </template>
          </el-table-column>
          <table-action-column :count="1">
            <template #default="{ row }">
              <el-popconfirm title="确定要回滚到这个版本吗？" @confirm="handleRollback(row.id)">
                <template #reference><table-action-icon icon="el-icon-refresh-left" tip="一键回滚" type="danger" /></template>
              </el-popconfirm>
            </template>
          </table-action-column>
        </el-table>
      </div>
    </el-drawer>

    <!-- Dry-Run Drawer -->
    <el-drawer :title="`整表试跑 - ${currentEndpoint.name || ''}`" v-model="drawerDryRunVisible" size="65%" append-to-body>
      <div style="padding: 20px; display: flex; gap: 20px; height: calc(100vh - 100px);">
        <div style="flex: 1; display: flex; flex-direction: column;">
          <h4>{{ currentConnection.type === 'JDBC' ? '1. 粘贴一行或一个数组，键用列名' : '1. 粘贴源系统返回的 JSON' }}</h4>
          <el-input
            v-model="dryRunForm.mockData"
            type="textarea"
            :rows="22"
            :placeholder="currentConnection.type === 'JDBC' ? '[{&quot;pat_id&quot;:&quot;P1&quot;,&quot;pat_name&quot;:&quot;张三&quot;}]' : '{&quot;data&quot;:[{&quot;PATIENT_ID&quot;:&quot;P1&quot;}]}'"
            style="flex: 1; font-family: monospace;"
          />
        </div>
        <div style="width: 420px; display: flex; flex-direction: column;">
          <h4>2. 已保存的映射（{{ dryRunRules.length }} 条）</h4>
          <el-table :data="dryRunRules" size="small" border max-height="220">
            <el-table-column prop="sourceField" label="源" show-overflow-tooltip />
            <el-table-column prop="targetField" label="目标" width="110" />
            <el-table-column prop="ruleExpression" label="表达式" width="110" show-overflow-tooltip />
          </el-table>
          <el-button type="primary" size="large" icon="el-icon-video-play" :loading="dryRanning" style="margin-top:16px" @click="executeDryRun">按映射试跑</el-button>
          <div style="margin-top: 20px; overflow:auto;">
            <h4>3. 映射结果</h4>
            <pre v-if="dryRunResult !== null" style="background: #f5f7fa; color: #303133; padding: 15px; border-radius: 4px; overflow: auto; min-height: 150px; font-family: monospace;">{{ JSON.stringify(dryRunResult, null, 2) }}</pre>
            <div v-else style="color: gray; font-size: 13px;">等待执行...</div>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- Marketplace Drawer -->
    <el-drawer title="行业接口标准模板库 (Marketplace)" v-model="drawerMarketplaceVisible" size="40%" append-to-body>
      <div style="padding: 20px;">
        <el-alert title="一键导入大厂标准对接模板，系统会自动为您创建 Endpoint 及相关的提取映射规则。" type="info" style="margin-bottom:20px" />

        <el-card v-for="tpl in marketplaceTemplates" :key="tpl.id" style="margin-bottom: 15px;" shadow="hover">
          <template #header><div style="display:flex; justify-content:space-between; align-items:center;">
            <b>{{ tpl.vendor }} - {{ tpl.name }}</b>
            <el-button type="primary" size="small" @click="doImportTemplate(tpl.id)">一键导入</el-button>
          </div></template>
          <div style="font-size: 13px; color: #666; margin-bottom: 10px;">{{ tpl.description }}</div>
          <div style="font-size: 12px; background: #fdfdfd; padding: 10px; border-radius: 4px;">
            <div><b>连接类型:</b> <el-tag size="small">{{ tpl.connectionType }}</el-tag>
              <b style="margin-left:8px">请求方式:</b> <el-tag size="small">{{ tpl.endpoint.method }}</el-tag></div>
            <div style="margin-top:5px; word-break:break-all;"><b>路径:</b> {{ tpl.endpoint.path }}</div>
            <div style="margin-top:5px;"><b>预设字段数:</b> <el-tag type="success" size="small" round>{{ tpl.rules.length }} 个</el-tag></div>
          </div>
        </el-card>
      </div>
    </el-drawer>

  </div>
</template>

<script>
import {
  getConnections, saveConnection, deleteConnection, pingConnection,
  getEndpoints, saveEndpoint, deleteEndpoint,
  getMappings, saveMapping, deleteMapping,
  getEndpointHistory, rollbackEndpoint, dryRunMapping,
  getMarketplaceTemplates, importMarketplaceTemplate
} from '@/api/integration'
import { ElMessage as Message } from 'element-plus'

export default {
  name: 'IntegrationConfig',
  data() {
    return {
      loading: false,
      connectionList: [],
      endpointList: [],
      mappingList: [],
      dryRunRules: [],
      statusReady: false,
      endpointStatusReady: false,
      targetFieldGroups: [
        { label: '门诊', options: [
          { value: 'patientCode', label: 'patientCode 患者标识' },
          { value: 'patientName', label: 'patientName 姓名' },
          { value: 'deptID', label: 'deptID 科室' },
          { value: 'officeRoomId', label: 'officeRoomId 诊室' },
          { value: 'showNo', label: 'showNo 号码' },
          { value: 'doctorID', label: 'doctorID 医生' },
          { value: 'sex', label: 'sex 性别' },
          { value: 'ageStr', label: 'ageStr 年龄' },
          { value: 'sourceType', label: 'sourceType 来源' }
        ] },
        { label: '药房', options: [
          { value: 'sflbbigtype', label: 'sflbbigtype 药品大类' },
          { value: 'sourceType', label: 'sourceType 来源' }
        ] },
        { label: '检验 / 体检', options: [
          { value: 'itemCode', label: 'itemCode 项目编码' },
          { value: 'state', label: 'state 对方状态' }
        ] }
      ],

      dialogConnVisible: false,
      drawerEndpointsVisible: false,
      dialogEndpointVisible: false,
      dialogMappingVisible: false,
      drawerHistoryVisible: false,
      drawerDryRunVisible: false,
      drawerMarketplaceVisible: false,
      historyList: [],
      marketplaceTemplates: [],

      connForm: this.initConnForm(),
      currentConnection: {},

      epForm: this.initEpForm(),
      currentEndpoint: {},

      dryRanning: false,
      dryRunForm: { mockData: '' },
      dryRunResult: null
    }
  },
  computed: {
    methodOptions() {
      const type = this.currentConnection.type
      if (type === 'JDBC') return [{ value: 'POLLING', label: 'POLLING 定时查询' }]
      if (type === 'WEBHOOK') return [{ value: 'WEBHOOK', label: 'WEBHOOK 被动接收' }]
      return [{ value: 'GET', label: 'GET' }, { value: 'POST', label: 'POST' }]
    }
  },
  created() {
    this.fetchConnections()
  },
  methods: {
    initConnForm() {
      return { id: null, name: '', type: 'HTTP', baseUrl: '', username: '', password: '', driverClass: '', allowIps: '', status: 1 }
    },
    initEpForm() {
      return { id: null, connectionId: null, endpointCode: '', name: '', path: '', mockPath: '', method: 'GET', cronExpression: '', envMode: 0, status: 1 }
    },
    defaultMethod() {
      return (this.methodOptions[0] && this.methodOptions[0].value) || 'GET'
    },

    // --- Connections ---
    async fetchConnections() {
      this.statusReady = false
      this.loading = true
      try {
        const res = await getConnections()
        this.connectionList = res.data || []
      } finally {
        this.loading = false
        this.$nextTick(() => { this.statusReady = true })
      }
    },
    handleCreateConnection() {
      this.connForm = this.initConnForm()
      this.dialogConnVisible = true
    },
    handleEditConnection(row) {
      this.connForm = { ...row, password: '' }
      this.dialogConnVisible = true
    },
    async submitConnection() {
      const err = this.validateConn()
      if (err) return Message.warning(err)
      try {
        await saveConnection(this.connForm)
        Message.success('保存成功')
        this.dialogConnVisible = false
        this.fetchConnections()
      } catch (e) {
        Message.error(e.message || '保存失败')
      }
    },
    validateConn() {
      if (!this.connForm.name || !this.connForm.name.trim()) return '请填写名称'
      if (this.connForm.type !== 'WEBHOOK' && !this.connForm.baseUrl) return '请填写连接地址'
      if (this.connForm.type === 'JDBC' && !this.connForm.driverClass) return '请填写驱动类'
      return ''
    },
    async toggleConnStatus(row) {
      if (!this.statusReady) return
      try {
        await saveConnection({ ...row, password: '' })
        Message.success(row.status === 1 ? '已启用' : '已停用')
      } catch (e) {
        row.status = row.status === 1 ? 0 : 1
        Message.error(e.message || '切换失败')
      }
    },
    async deleteConn(id) {
      this.$confirm('确定删除该数据源？').then(async() => {
        try {
          await deleteConnection(id)
          Message.success('删除成功')
          this.fetchConnections()
        } catch (e) {
          Message.error(e.message || '删除失败')
        }
      }).catch(() => {})
    },
    async pingConn(id) {
      try {
        const res = await pingConnection(id)
        if (res.success) {
          Message.success(res.msg || '连接可用')
        } else {
          Message.error(res.msg || 'Ping 失败，请检查配置')
        }
      } catch (e) {
        Message.error('连接失败: ' + e.message)
      }
    },

    // --- Endpoints ---
    async openEndpoints(conn) {
      this.currentConnection = conn
      this.drawerEndpointsVisible = true
      this.fetchEndpoints(conn.id)
    },
    async fetchEndpoints(connId) {
      this.endpointStatusReady = false
      const res = await getEndpoints(connId)
      this.endpointList = res.data || []
      this.$nextTick(() => { this.endpointStatusReady = true })
    },
    handleCreateEndpoint() {
      this.epForm = {
        connectionId: this.currentConnection.id,
        name: '',
        endpointCode: '',
        method: this.defaultMethod(),
        path: '',
        mockPath: '',
        envMode: 0,
        cronExpression: '',
        status: 1
      }
      this.dialogEndpointVisible = true
    },
    handleEditEndpoint(row) {
      this.epForm = { ...row }
      this.dialogEndpointVisible = true
    },
    async submitEndpoint() {
      const err = this.validateEndpoint()
      if (err) return Message.warning(err)
      try {
        await saveEndpoint(this.epForm)
        Message.success('端点保存成功')
        this.dialogEndpointVisible = false
        this.fetchEndpoints(this.currentConnection.id)
      } catch (e) {
        Message.error(e.message || '端点保存失败')
      }
    },
    validateEndpoint() {
      if (!this.epForm.name || !this.epForm.name.trim()) return '请填写端点名称'
      if (!this.epForm.endpointCode || !this.epForm.endpointCode.trim()) return '请填写端点编码'
      if (this.epForm.method === 'POLLING' && !this.epForm.cronExpression) return '请填写 Cron'
      if ((this.epForm.method === 'GET' || this.epForm.method === 'POST') && this.epForm.envMode !== 1 && !this.epForm.path) return '请填写请求路径'
      if (this.epForm.envMode === 1 && this.epForm.method !== 'WEBHOOK' && !this.epForm.mockPath) return '请填写 Mock 路径'
      return ''
    },
    async deleteEp(row) {
      this.$confirm('确定删除该端点及其映射？').then(async() => {
        try {
          await deleteEndpoint(row.id)
          Message.success('已删除')
          this.fetchEndpoints(this.currentConnection.id)
        } catch (e) {
          Message.error(e.message || '删除失败')
        }
      }).catch(() => {})
    },
    async toggleEndpointStatus(row) {
      if (!this.endpointStatusReady) return
      try {
        await saveEndpoint(row)
        Message.success(row.status === 1 ? '已启用' : '已停用')
      } catch (e) {
        row.status = row.status === 1 ? 0 : 1
        Message.error(e.message || '切换失败')
      }
    },
    async handleEnvModeSwitch(row) {
      try {
        await saveEndpoint(row)
        this.$message.success(`已切换该端点至 ${row.envMode === 1 ? 'MOCK 环境' : 'PROD 环境'}`)
      } catch (e) {
        row.envMode = row.envMode === 1 ? 0 : 1
        this.$message.error('切换失败')
      }
    },
    // --- Mappings ---
    async openMappings(endpoint) {
      this.currentEndpoint = endpoint
      this.dialogMappingVisible = true
      const res = await getMappings(endpoint.id)
      this.mappingList = res.data || []
    },
    addMappingRow() {
      this.mappingList.push({
        id: null,
        endpointId: this.currentEndpoint.id,
        sourceField: '',
        targetField: ''
      })
    },
    async saveMappingEntity(row) {
      const expr = (row.ruleExpression || '').trim().toUpperCase()
      if ((!row.sourceField && !expr.startsWith('CONST:')) || !row.targetField) {
        return Message.warning('请填写映射源和目标')
      }
      try {
        const res = await saveMapping(row)
        row.id = res.data.id
        Message.success('单行保存成功')
      } catch (e) {
        Message.error(e.message || '保存失败')
      }
    },
    async saveOrDelMapping(row, index) {
      if (row.id) {
        await deleteMapping(row.id)
      }
      this.mappingList.splice(index, 1)
      Message.success('移除成功')
    },

    // --- History & Rollback ---
    formatDate(time) {
      if (!time) return ''
      return new Date(time).toLocaleString()
    },
    async openHistory(endpoint) {
      this.currentEndpoint = endpoint
      this.drawerHistoryVisible = true
      await this.fetchHistory(endpoint.id)
    },
    async fetchHistory(endpointId) {
      const res = await getEndpointHistory(endpointId)
      this.historyList = res.data || []
    },
    async handleRollback(historyId) {
      try {
        await rollbackEndpoint(historyId)
        Message.success('版本回滚成功！')
        this.fetchEndpoints(this.currentConnection.id)
        this.fetchHistory(this.currentEndpoint.id)
      } catch (e) {
        Message.error('回滚失败')
      }
    },

    // --- Dry Run ---
    openDryRun(endpoint) {
      this.currentEndpoint = endpoint
      this.dryRunForm = { mockData: '' }
      this.dryRunResult = null
      this.dryRunRules = []
      this.drawerDryRunVisible = true
      getMappings(endpoint.id).then(res => {
        this.dryRunRules = res.data || []
      })
    },
    async executeDryRun() {
      if (!this.dryRunForm.mockData) {
        return Message.warning('请粘贴样本数据')
      }
      this.dryRanning = true
      try {
        const res = await dryRunMapping({ endpointId: this.currentEndpoint.id, mockData: this.dryRunForm.mockData })
        this.dryRunResult = res.data
        Message.success('试跑完成')
      } catch (e) {
        this.dryRunResult = e.message
        Message.error(e.message || '试跑失败')
      } finally {
        this.dryRanning = false
      }
    },

    // --- Marketplace ---
    async openMarketplace() {
      this.drawerMarketplaceVisible = true
      const res = await getMarketplaceTemplates()
      this.marketplaceTemplates = res.data || []
    },
    async doImportTemplate(templateId) {
      this.$confirm('确定导入该模板吗？将自动创建相应的端点配置和规则。', '提示').then(async() => {
        try {
          await importMarketplaceTemplate(templateId, this.currentConnection.id)
          Message.success('模板导入成功！')
          this.drawerMarketplaceVisible = false
          this.fetchEndpoints(this.currentConnection.id)
        } catch (e) {
          Message.error('导入失败: ' + e.message)
        }
      })
    }
  }
}
</script>
