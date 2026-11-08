<template>
  <div class="app-container">
    <el-card>
      <!-- 查询条件：左侧搜索，右侧添加 -->
      <div class="list-toolbar screen-filter-bar">
        <el-form inline class="list-toolbar__left screen-filter-bar__query" @submit.prevent="GetScreens">
          <el-form-item label="查询范围">
            <el-date-picker
              v-model="CreatTimeRange"
              type="datetimerange"
              :shortcuts="pickerOptions.shortcuts"
              range-separator="至"
              format="YYYY 年 MM 月 DD 日"
              value-format="YYYY-MM-DD hh:mm:ss"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              align="right"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="ScreenInput.State" placeholder="请选择" clearable style="width: 140px;">
              <el-option
                v-for="item in States"
                :key="item.Value"
                :label="item.Name"
                :value="item.Value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="名称">
            <el-input
              v-model="ScreenInput.Name"
              placeholder="支持模糊查询"
              clearable
              style="width: 180px;"
              @keyup.enter="GetScreens"
            />
          </el-form-item>
          <el-form-item label="终端类型">
            <el-select v-model="filterTerminalType" placeholder="全部" clearable style="width: 150px;" @change="GetScreens">
              <el-option label="全部" value="" />
              <el-option label="候诊区大屏" value="SCREEN" />
              <el-option label="诊室门边屏" value="CLINIC" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              icon="el-icon-search"
              native-type="button"
              @click="GetScreens"
            >搜索</el-button>
          </el-form-item>
        </el-form>
        <div class="list-toolbar__right screen-filter-bar__actions">
          <el-button
            type="primary"
            @click="dialogAUScreen('Add')"
          >添加屏幕信息</el-button>
        </div>
      </div>
      <div v-loading="loading" class="screen-card-grid">
        <el-empty
          v-if="Screens.length === 0 && !loading"
          class="screen-card-grid__empty"
          description="暂无设备数据"
        />
        <el-card
          v-for="(item, index) in Screens"
          :key="item.ID || item.id || index"
          shadow="hover"
          class="device-card"
          :body-style="{ padding: '0px' }"
        >
            <div class="card-header" :class="getScreenStateHeaderClass(item.State)">
              <div class="header-title">
                <i class="el-icon-monitor" />
                <span class="device-name" :title="item.Name">{{ item.Name }}</span>
              </div>
              <div class="header-status-tags">
                <el-tag :type="getScreenStateTagType(item.State)" effect="dark" size="small" round class="status-badge">
                  {{ item.stateModel || getScreenStateLabel(item.State) }}
                </el-tag>
                <el-tag :type="getOnlineTagType(item.online, item.deviceCode)" effect="plain" size="small" round class="status-badge">
                  {{ getOnlineStatusLabel(item.online, item.deviceCode) }}
                </el-tag>
              </div>
            </div>
            <div class="card-body">
              <div class="info-row info-row--device-code">
                <span class="label">设备编号:</span>
                <span class="value code-font" :title="item.deviceCode || '--'">{{ item.deviceCode || '--' }}</span>
              </div>
              <div class="info-row">
                <span class="label">终端类型:</span>
                <span class="value">{{ terminalTypeLabel(item.terminalType) }}</span>
              </div>
              <div class="info-row">
                <span class="label">呼叫代码:</span>
                <span class="value font-bold">{{ item.CallCode }}</span>
              </div>
              <div v-if="(item.terminalType || 'SCREEN') !== 'CLINIC'" class="info-row">
                <span class="label">播报:</span>
                <span class="value" :title="item.voiceBoundSummary || ''">
                  {{ voiceChannelLabel(item.voiceChannel) }} · {{ item.voiceBoundSummary || '未绑音响' }}
                </span>
              </div>
              <div class="info-row">
                <span class="label">入网时间:</span>
                <span class="value text-gray">{{ item.CreateDate }}</span>
              </div>

              <div class="stats-grid">
                <div class="stat-item">
                  <div class="stat-value text-blue">{{ item.MaxNum }}</div>
                  <div class="stat-label">最大诊室</div>
                </div>
                <div class="stat-item">
                  <div class="stat-value text-green">{{ item.CallNum }}</div>
                  <div class="stat-label">呼音次数</div>
                </div>
              </div>

              <div class="bulletin-text" :title="item.Bulletin">
                <i class="el-icon-chat-dot-square mr-1" /> {{ item.Bulletin || '无公告' }}
              </div>
            </div>
            <div class="card-footer">
              <el-button type="primary" round size="small" icon="el-icon-edit" class="btn-settings" @click="dialogAUScreen(item)">设置</el-button>
              <el-button type="success" plain round size="small" icon="el-icon-connection" @click="dialogSet(item)">诊室</el-button>
              <el-button type="danger" plain round size="small" icon="el-icon-delete" @click="DelScreen(item)" />
            </div>
          </el-card>
      </div>
    </el-card>
    <!-- 新增/编辑：右侧抽屉 -->
    <form-drawer
      v-model="dialogAddUpdate"
      :title="dialogTitle"
      size="520px"
      :before-close="CancelAUScreen"
    >
      <el-form :model="ScreenInput" label-width="96px" label-position="right" class="screen-dialog-form">
        <el-form-item
          prop="Name"
          :rules="[{ required: true, message: '屏幕名称不能为空' }]"
          label="屏幕名称"
        >
          <el-input v-model="ScreenInput.Name" />
        </el-form-item>
        <el-form-item v-if="AUTiggle" label="初始状态" class="screen-dialog-status-item">
          <el-tag type="warning" effect="plain">待配置（保存后自动设置，填写设备编号并绑定诊室齐全后自动启用）</el-tag>
        </el-form-item>
        <el-form-item v-else label="设备状态" class="screen-dialog-status-item">
          <el-tag :type="getScreenStateTagType(ScreenInput.State)" effect="dark" size="small" style="margin-right:8px;">
            配置：{{ getScreenStateLabel(ScreenInput.State, ScreenInput.stateModel) }}
          </el-tag>
          <el-tag :type="getOnlineTagType(ScreenInput.online, ScreenInput.deviceCode)" effect="plain" size="small">
            连接：{{ getOnlineStatusLabel(ScreenInput.online, ScreenInput.deviceCode) }}
          </el-tag>
        </el-form-item>
        <el-form-item prop="IsNum" label="显示序号">
          <el-checkbox v-model="showSeqNum">显示</el-checkbox>
        </el-form-item>
        <el-form-item
          prop="CallCode"
          :rules="[{ required: true, message: '呼叫代码不能为空' }]"
          label="呼叫代码"
        >
          <el-input v-model="ScreenInput.CallCode" />
        </el-form-item>
        <el-form-item
          prop="Subtitle"
          :rules="[{ required: true, message: '副标题不能为空' }]"
          label="副标题"
        >
          <el-input v-model="ScreenInput.Subtitle" />
        </el-form-item>
        <el-form-item label="设备编号" prop="deviceCode">
          <el-input
            v-model="ScreenInput.deviceCode"
            placeholder="Shell UUID 或后台自定义编号"
            autocomplete="off"
            :disabled="isDeviceCodeLocked"
            @input="onDeviceCodeInput"
          />
        </el-form-item>
        <el-form-item label="终端类型">
          <el-select v-model="ScreenInput.terminalType" @change="onTerminalTypeChange">
            <el-option label="候诊区大屏 (SCREEN)" value="SCREEN" />
            <el-option label="诊室门边屏 (CLINIC)" value="CLINIC" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isClinicForm" label="模板路由">
          <el-select v-model="ScreenInput.templateRoute" clearable placeholder="默认 /default">
            <el-option v-for="r in templateRoutes" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-alert
          v-if="isClinicForm"
          type="info"
          :closable="false"
          show-icon
          title="诊室门边屏由 HJ诊室门边屏终端 (/clinic/) 渲染，无需配置候诊模板路由；请绑定 1 个物理诊室。"
          style="margin-bottom: 16px;"
        />
        <el-form-item
          v-if="!isClinicForm"
          label="最大诊室数"
          prop="MaxNum"
          :rules="[{ type: 'number', message: '最大诊室数必须为数字值' }]"
        >
          <el-input
            v-model.number="ScreenInput.MaxNum"
            :max="5"
            :min="0"
            type="number"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item
          label="呼叫次数"
          prop="CallNum"
          :rules="[{ type: 'number', message: '呼叫次数必须为数字值' }]"
        >
          <el-input
            v-model.number="ScreenInput.CallNum"
            type="number"
            :min="1"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item
          label="滚动间隔"
          prop="ScrollTime"
          :rules="[
            { type: 'number', message: '屏幕滚动时间间隔必须为数字值' },
          ]"
        >
          <el-input
            v-model.number="ScreenInput.ScrollTime"
            type="number"
            :min="1"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item
          label="语速"
          prop="Rate"
          :rules="[{ type: 'number', message: '语速必须为数字值' }]"
        >
          <el-input
            v-model.number="ScreenInput.Rate"
            :max="10"
            :min="-10"
            type="number"
            autocomplete="off"
            placeholder="-10~10"
          />
        </el-form-item>
        <el-form-item
          prop="CallContent"
          :rules="[{ required: true, message: '语音播报不能为空' }]"
          label="语音播报"
        >
          <el-input
            v-model="ScreenInput.CallContent"
            type="text"
            placeholder="请{0}{1}，到{2}检查（0=号序 1=姓名 2=诊室）"
          />
        </el-form-item>
        <template v-if="!isClinicForm">
          <el-form-item label="播报通道">
            <el-radio-group v-model="ScreenInput.voiceChannel" size="small">
              <el-radio-button label="VOICE">独立音响</el-radio-button>
              <el-radio-button label="SCREEN">大屏播报</el-radio-button>
              <el-radio-button label="BOTH">同时</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="绑定音响" class="voice-bind-item">
            <el-select
              v-model="ScreenInput.voiceDeviceIds"
              multiple
              filterable
              clearable
              collapse-tags
              placeholder="选择独立音响（可多选、可跨屏共用）"
              class="voice-bind-select"
            >
              <el-option
                v-for="v in voiceDeviceOptions"
                :key="v.id"
                :label="voiceOptionLabel(v)"
                :value="v.id"
              >
                <span class="voice-opt-name">{{ v.name || v.deviceCode }}</span>
                <span class="voice-opt-meta">{{ v.deviceCode }}{{ v.location ? ' · ' + v.location : '' }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <div class="form-field-hint form-field-hint--offset">
            音响故障时切「大屏播报」（本屏需外接音箱）；无可用音响会自动降级。多屏可各自绑定，同一台音响可共用。
          </div>
        </template>
        <el-form-item
          prop="Bulletin"
          :rules="[{ required: true, message: '公告内容不能为空' }]"
          label="公告内容"
        >
          <el-input
            v-model="ScreenInput.Bulletin"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="!AUTiggle && !isScreenFrozen(ScreenInput.State)" type="warning" plain @click="applyLifecycleAction('FREEZE')">冻结设备</el-button>
        <el-button v-if="!AUTiggle && isScreenFrozen(ScreenInput.State)" type="success" plain @click="applyLifecycleAction('UNFREEZE')">解除冻结</el-button>
        <el-button @click="CancelAUScreen">取 消</el-button>
        <el-button type="primary" @click="AUScreen">确 定</el-button>
      </template>
    </form-drawer>
    <!-- 设置屏幕对应诊室 -->

    <el-dialog
      v-loading="loading"
      :show-close="false"
      :title="dialogSettingTitle"
      v-model="dialogSetting"
      width="900px"
    >
      <div class="screen-room-transfer">
        <el-transfer
          v-model="ScreenDeptMapData"
          :titles="['可选物理诊室', '已关联诊室']"
          :button-texts="['删除诊室', '添加诊室']"
          :filterable="true"
          :format="{
            noChecked: '${total}',
            hasChecked: '${checked}/${total}',
          }"
          :data="DepartmentData"
          @change="handleChange"
        >
          <template #default="{ option }"><span>
            {{ option.label }}
          </span></template>
        </el-transfer>
      </div>
      <template #footer><div class="dialog-footer">
        <el-button @click="dialogSetting = false">取 消</el-button>
        <el-button
          type="primary"
          @click="RelevancyScreenAndDepts"
        >确 定</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>
<script>
import {
  getScreenStateHeaderClass,
  getScreenStateLabel,
  getScreenStateTagType,
  getOnlineStatusLabel,
  getOnlineTagType,
  isScreenFrozen,
  normalizeScreenRow
} from '@/utils/screenState'
import { sanitizeDeviceCodeInput } from '@/utils/signState'
import { listVoiceDevices } from '@/api/VoiceDeviceManages'
export default {
  name: 'ScreenManages',
  data() {
    return {
      loading: false,
      Screens: [],
      voiceDeviceOptions: [],
      pickerOptions: {
        shortcuts: [
          {
            text: '最近一周',
            value() {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
              return [start, end]
            }
          },
          {
            text: '最近一个月',
            value() {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
              return [start, end]
            }
          },
          {
            text: '最近三个月',
            value() {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
              return [start, end]
            }
          }
        ]
      },
      CreatTimeRange: '',
      ScreenInput: {
        ID: '',
        Name: '',
        State: '',
        StartTime: null,
        EndTime: null,
        CallNum: 1,
        CallCode: '',
        Bulletin: '',
        ScrollTime: 1,
        Subtitle: '',
        Rate: 0,
        MaxNum: 1,
        CallContent: '请{0}{1}，到{2}检查',
        IsNum: null,
        deviceCode: '',
        terminalType: 'SCREEN',
        templateRoute: '/default',
        voiceChannel: 'VOICE',
        voiceDeviceIds: []
      },
      States: [
        { Name: '请选择', Value: '' },
        { Name: '启用', Value: 1 },
        { Name: '待配置', Value: 2 },
        { Name: '冻结', Value: 0 }
      ],
      templateRoutes: [
        { value: '/default', label: '/default · 默认双栏' },
        { value: '/default-mul', label: '/default-mul · 多诊室按行' },
        { value: '/default-mul-col', label: '/default-mul-col · 多诊室按列' },
        { value: '/pdjh3', label: '/pdjh3' },
        { value: '/pdjh4', label: '/pdjh4' },
        { value: '/pdjh5', label: '/pdjh5' },
        { value: '/pdjh6', label: '/pdjh6' },
        { value: '/pdjh7', label: '/pdjh7' },
        { value: '/pdjh8', label: '/pdjh8' },
        { value: '/pharmacy-window', label: '/pharmacy-window · 药房窗口屏' },
        { value: '/lab-window', label: '/lab-window · 检验窗口屏' },
        { value: '/GlobalHub', label: '/GlobalHub · 全院枢纽' },
        { value: '/directorDashboard', label: '/directorDashboard · 院长看板' },
        { value: '/pdjh1', label: '/pdjh1（兼容，等同 /default）' }
      ],
      dialogAddUpdate: false,
      dialogTitle: '',
      AUTiggle: true,
      dialogSettingTitle: '',
      dialogSetting: false,
      dialogSettingRow: null,
      filterTerminalType: '',
      DepartmentData: [], // 诊室集合
      ScreenDeptMapData: [], // 屏幕管理诊室集合
      /** 打开编辑弹窗时已有的设备编号；非空则禁止修改 */
      lockedDeviceCode: ''
    }
  },
  computed: {
    isClinicForm() {
      return (this.ScreenInput.terminalType || 'SCREEN') === 'CLINIC'
    },
    /** 编辑且已登记设备编号时锁定输入框 */
    isDeviceCodeLocked() {
      return !this.AUTiggle && !!this.lockedDeviceCode
    },
    showSeqNum: {
      get() {
        return this.ScreenInput.IsNum != 1
      },
      set(val) {
        this.ScreenInput.IsNum = val ? 0 : 1
      }
    }
  },
  watch: {
    CreatTimeRange() {
      if (this.CreatTimeRange != null) {
        this.ScreenInput.StartTime = this.CreatTimeRange[0]
        this.ScreenInput.EndTime = this.CreatTimeRange[1]
      } else {
        this.ScreenInput.StartTime = null
        this.ScreenInput.EndTime = null
      }
    },
    dialogSetting() {
      if (this.dialogSetting) {
        this.GetNotassociatedDepartments()
      }
    }
  },
  mounted() {
    const q = this.$route && this.$route.query
    if (q && q.terminalType) {
      this.filterTerminalType = String(q.terminalType).toUpperCase()
    }
    this.GetScreens()
    this.loadVoiceDevices()
  },
  methods: {
    getScreenStateHeaderClass,
    getScreenStateLabel,
    getScreenStateTagType,
    getOnlineStatusLabel,
    getOnlineTagType,
    isScreenFrozen,
    voiceOptionLabel(v) {
      // 选中标签宜短，避免多选框内换行；下拉项另用 slot 展示完整信息
      const name = v.name || v.deviceCode || v.id
      return v.deviceCode ? `${name} (${v.deviceCode})` : String(name)
    },
    loadVoiceDevices() {
      listVoiceDevices()
        .then(res => {
          const rows = (res && res.data) || []
          this.voiceDeviceOptions = rows.filter(v => v.state == null || v.state === 1)
        })
        .catch(() => { this.voiceDeviceOptions = [] })
    },
    terminalTypeLabel(type) {
      const t = type || 'SCREEN'
      return { SCREEN: '候诊区大屏', CLINIC: '诊室门边屏' }[t] || t
    },
    voiceChannelLabel(ch) {
      return { VOICE: '独立音响', SCREEN: '大屏播报', BOTH: '双通道' }[ch] || '独立音响'
    },
    onTerminalTypeChange(val) {
      if (val === 'CLINIC') {
        this.ScreenInput.MaxNum = 1
        this.ScreenInput.templateRoute = ''
      } else if (!this.ScreenInput.templateRoute) {
        this.ScreenInput.templateRoute = '/default'
      }
    },
    /** 设备编号：仅允许数字、大写字母与连字符（与 SignDeviceManages 一致） */
    onDeviceCodeInput(value) {
      this.ScreenInput.deviceCode = sanitizeDeviceCodeInput(value)
    },
    /** 列表查询参数：与新增/编辑表单 ScreenInput 分离，避免默认 terminalType 污染「全部」筛选 */
    buildScreenQueryPayload() {
      const payload = {
        Name: this.ScreenInput.Name,
        State: this.ScreenInput.State,
        StartTime: this.ScreenInput.StartTime,
        EndTime: this.ScreenInput.EndTime
      }
      if (this.filterTerminalType) {
        payload.terminalType = this.filterTerminalType
      }
      return payload
    },
    GetScreens() {
      this.Screens.splice(0, this.Screens.length)
      this.loading = true
      const payload = this.buildScreenQueryPayload()
      this.$store
        .dispatch('ScreenManages/GetScreens', payload)
        .then((response) => {
          const list = response.data || []
          list.forEach((item) => {
            const normalized = this.normalizeScreenFromApi(item)
            normalized.CreateDate = this.formatDate({
              date: item.createDate || item.CreateDate,
              formatStr: 'yyyy-MM-dd HH:mm:ss'
            })
            this.Screens.push(normalized)
          })

          this.loading = false
        })
        .catch((error) => {
          this.loading = false
        })
    },
    dialogAUScreen(row) {
      this.dialogAddUpdate = true
      if (row != 'Add') {
        this.AUTiggle = false
        this.dialogTitle = '修改屏幕信息'
        this.$store
          .dispatch('ScreenManages/GetScreensSingle', { id: row.ID || row.id })
          .then((response) => {
            this.ScreenInput = this.normalizeScreenFromApi(response.data || {})
            this.lockedDeviceCode = (this.ScreenInput.deviceCode || '').trim()
          })
          .catch((error) => {
            this.$message.error('服务器错误！')
          })
      } else {
        this.AUTiggle = true
        this.dialogTitle = '添加屏幕信息'
        this.lockedDeviceCode = ''
        this.ScreenInput = {
          ID: '',
          Name: '',
          State: 2,
          StartTime: null,
          EndTime: null,
          CallNum: 1,
          CallCode: '',
          Bulletin: '',
          ScrollTime: 1,
          Subtitle: '',
          Rate: 0,
          MaxNum: 1,
          CallContent: '请{0}{1}，到{2}检查',
          IsNum: 0,
          deviceCode: '',
          terminalType: 'SCREEN',
          templateRoute: '/default',
          voiceChannel: 'VOICE',
          voiceDeviceIds: [],
          online: false,
          configComplete: false
        }
      }
    },
    CancelAUScreen() {
      this.dialogAddUpdate = false
      this.lockedDeviceCode = ''
      this.ScreenInput = {
        ID: '',
        Name: '',
        State: '',
        StartTime: null,
        EndTime: null,
        CallNum: 1,
        CallCode: '',
        Bulletin: '',
        ScrollTime: 1,
        Subtitle: '',
        MaxNum: 1,
        Rate: 0,
        CallContent: '请{0}{1}，到{2}检查',
        IsNum: 0,
        deviceCode: '',
        terminalType: 'SCREEN',
        templateRoute: '/default',
        voiceChannel: 'VOICE',
        voiceDeviceIds: []
      }
    },
    isApiSuccess(response) {
      return response && (response.success === true || response.code === 200)
    },
    getApiMessage(response, fallback) {
      return (response && (response.msg || response.message)) || fallback
    },
    mapScreenInputForApi(input, lifecycleAction) {
      const terminalType = input.terminalType || 'SCREEN'
      const mapped = {
        id: input.ID || input.id || '',
        name: input.Name || input.name,
        callCode: input.CallCode || input.callCode,
        callNum: input.CallNum ?? input.callNum,
        callContent: input.CallContent || input.callContent,
        maxNum: terminalType === 'CLINIC' ? 1 : (input.MaxNum ?? input.maxNum),
        bulletin: input.Bulletin || input.bulletin,
        scrollTime: input.ScrollTime ?? input.scrollTime,
        subtitle: input.Subtitle || input.subtitle,
        rate: input.Rate ?? input.rate,
        startTime: input.StartTime || input.startTime,
        endTime: input.EndTime || input.endTime,
        isNum: input.IsNum != null ? String(input.IsNum) : input.isNum,
        deviceCode: input.deviceCode,
        terminalType,
        voiceChannel: input.voiceChannel || 'VOICE',
        voiceDeviceIds: Array.isArray(input.voiceDeviceIds) ? input.voiceDeviceIds : []
      }
      if (lifecycleAction) {
        mapped.lifecycleAction = lifecycleAction
      }
      if (terminalType !== 'CLINIC') {
        mapped.templateRoute = input.templateRoute || '/default'
      }
      return mapped
    },
    normalizeScreenFromApi(data) {
      const row = normalizeScreenRow(data)
      return {
        ID: row.id || row.ID || '',
        Name: row.name || row.Name || '',
        State: row.state,
        StartTime: row.startTime || row.StartTime || null,
        EndTime: row.endTime || row.EndTime || null,
        CallNum: row.callNum ?? row.CallNum ?? 1,
        CallCode: row.callCode || row.CallCode || '',
        Bulletin: row.bulletin || row.Bulletin || '',
        ScrollTime: row.scrollTime ?? row.ScrollTime ?? 1,
        Subtitle: row.subtitle || row.Subtitle || '',
        MaxNum: row.maxNum ?? row.MaxNum ?? 1,
        Rate: row.rate ?? row.Rate ?? 0,
        CallContent: row.callContent || row.CallContent || '请{0}{1}，到{2}检查',
        IsNum: row.isNum != null ? Number(row.isNum) : (row.IsNum != null ? Number(row.IsNum) : 0),
        deviceCode: row.deviceCode || '',
        terminalType: row.terminalType || 'SCREEN',
        templateRoute: row.templateRoute || '/default',
        voiceChannel: row.voiceChannel || 'VOICE',
        voiceDeviceIds: Array.isArray(row.voiceDeviceIds) ? row.voiceDeviceIds : [],
        voiceBoundSummary: row.voiceBoundSummary || '',
        stateModel: row.stateModel,
        configComplete: row.configComplete === true,
        online: row.online === true
      }
    },
    applyLifecycleAction(action) {
      if (!this.ScreenInput.ID) return
      this.$store
        .dispatch('ScreenManages/UpdateScreen', this.mapScreenInputForApi(this.ScreenInput, action))
        .then((response) => {
          if (this.isApiSuccess(response)) {
            this.$message.success(action === 'FREEZE' ? '设备已冻结' : '已解除冻结，配置状态已自动更新')
            this.$store
              .dispatch('ScreenManages/GetScreensSingle', { id: this.ScreenInput.ID })
              .then((res) => {
                this.ScreenInput = this.normalizeScreenFromApi(res.data || {})
              })
            this.GetScreens()
          } else {
            this.$message.error(this.getApiMessage(response, '操作失败'))
          }
        })
        .catch(() => {
          this.$message.error('服务器异常，请联系后台工作人员')
        })
    },
    AUScreen() {
      // 异常处理
      if (this.ScreenInput.Name.trim() == '') {
        this.$message({
          message: '屏幕名称不能为空',
          type: 'error'
        })
        return
      } else if (this.ScreenInput.CallCode.trim() == '') {
        this.$message({
          message: '呼叫代码不能为空',
          type: 'error'
        })
        return
      } else if (this.ScreenInput.Subtitle.trim() == '') {
        this.$message({
          message: '副标题不能为空',
          type: 'error'
        })
        return
      }
      if (this.AUTiggle) {
        // 新增屏幕信息
        this.$store
          .dispatch('ScreenManages/AddScreen', this.mapScreenInputForApi(this.ScreenInput))
          .then((response) => {
            if (this.isApiSuccess(response)) {
              this.$message({
                message: this.getApiMessage(response, '新增成功'),
                type: 'success'
              })
              this.CancelAUScreen()
              this.GetScreens()
            } else {
              this.$message({
                message: '新增失败：' + this.getApiMessage(response, '未知错误'),
                type: 'error'
              })
            }
          })
          .catch((error) => {
            this.$message.error('服务器异常，请联系后台工作人员')
          })
      } else {
        // 修改屏幕信息
        this.$store
          .dispatch('ScreenManages/UpdateScreen', this.mapScreenInputForApi(this.ScreenInput))
          .then((response) => {
            if (this.isApiSuccess(response)) {
              this.$message({
                message: this.getApiMessage(response, '修改成功'),
                type: 'success'
              })
              this.CancelAUScreen()
              this.GetScreens()
            } else {
              this.$message({
                message: '修改失败：' + this.getApiMessage(response, '未知错误'),
                type: 'error'
              })
            }
          })
          .catch((error) => {
            this.$message.error('服务器异常，请联系后台工作人员')
          })
      }
    },
    DelScreen(row) {
      // 删除屏幕信息
      this.$confirm('此操作将永久删除该屏幕信息, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.$store
            .dispatch('ScreenManages/DelScreen', { id: row.ID || row.id })
            .then((response) => {
              this.$message({
                type: 'success',
                message: '删除成功!'
              })
              this.GetScreens()
            })
            .catch((error) => {
              this.$message.error('服务器异常，请联系后台工作人员')
            })
        })
        .catch(() => {
          this.$message({
            type: 'info',
            message: '已取消删除'
          })
        })
    },

    handleChange(value, direction, movedKeys) {
      const maxRooms = this.dialogSettingRow.terminalType === 'CLINIC'
        ? 1
        : this.dialogSettingRow.MaxNum
      if (direction == 'right') {
        if (value.length > maxRooms) {
          value.splice(
            maxRooms,
            value.length - maxRooms
          )
          this.$message({
            message: this.dialogSettingRow.terminalType === 'CLINIC'
              ? '诊室门边屏只能绑定 1 个诊室'
              : '警告，屏幕最大诊室数量已达上线',
            type: 'warning'
          })
        }
      }
    },
    GetNotassociatedDepartments() {
      this.loading = true
      this.DepartmentData.splice(0, this.DepartmentData.length)
      this.ScreenDeptMapData.splice(0, this.ScreenDeptMapData.length)
      this.$store.dispatch('SignDeviceManages/getAllOfficeRooms')
        .then((response) => {
          const rooms = (response && response.data) || []
          rooms.forEach((item) => {
            this.DepartmentData.push({
              key: item.id,
              label: [item.floor, item.name, item.roomNo].filter(Boolean).join(' '),
              disabled: false
            })
          })
        })
        .finally(() => { this.loading = false })

      this.$store
        .dispatch('ScreenManages/GetHasassociatedRooms', { id: this.dialogSettingRow.ID })
        .then((response) => {
          this.ScreenDeptMapData.splice(0, this.ScreenDeptMapData.length)
          ;((response && response.data) || []).forEach((item) => {
            this.ScreenDeptMapData.push(item.id)
          })
        })
        .catch(() => {})
    },
    dialogSet(row) {
      this.dialogSetting = true
      this.dialogSettingTitle = row.Name
      this.dialogSettingRow = row
    },
    RelevancyScreenAndDepts() {
      this.loading = true
      this.$store
        .dispatch('ScreenManages/RelevancyScreenAndRooms', {
          screenId: this.dialogSettingRow.ID,
          officeRoomIds: this.ScreenDeptMapData
        })
        .then((response) => {
          this.$message({
            type: 'success',
            message: '设置成功!'
          })
          this.loading = false
          this.GetNotassociatedDepartments()
        })
        .catch((error) => {
          this.loading = false
        })
    },
    // 格式化时间
    formatDate(params) {
      const defalutParams = {
        date: new Date(),
        formatStr: 'yyyy-MM-dd HH:mm:ss'
      }
      params = { ...defalutParams, ...params }
      let date = params.date
      let formatStr = params.formatStr
      // 传入日期字符串 - 转成时间戳 - 转成标准时间
      const timeStamp = new Date(date).getTime()
      date = new Date(timeStamp)
      formatStr = formatStr.replace(
        new RegExp('yyyy'),
        `${date.getFullYear()}`
      )
      const month = date.getMonth() + 1
      formatStr = formatStr.replace(
        new RegExp('MM'),
        `${month > 9 ? month : '0' + month}`
      )
      const day = date.getDate()
      formatStr = formatStr.replace(
        new RegExp('dd'),
        `${day > 9 ? day : '0' + day}`
      )
      const hour = date.getHours()
      formatStr = formatStr.replace(
        new RegExp('HH'),
        `${hour > 9 ? hour : '0' + hour}`
      )
      const min = date.getMinutes()
      formatStr = formatStr.replace(
        new RegExp('mm'),
        `${min > 9 ? min : '0' + min}`
      )
      const sec = date.getSeconds()
      formatStr = formatStr.replace(
        new RegExp('ss'),
        `${sec > 9 ? sec : '0' + sec}`
      )
      return formatStr
    }
  }
}
</script>

<style lang="scss" scoped>
/* 卡片网格：避免 el-row/el-col 浮动布局在卡片高度不一致时第二行错位留白 */
.screen-card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
  min-height: 400px;
  padding-top: 10px;

  @media (max-width: 1199px) {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  @media (max-width: 991px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  @media (max-width: 767px) {
    grid-template-columns: 1fr;
  }

  &__empty {
    grid-column: 1 / -1;
    justify-self: center;
    padding: 48px 0;
  }
}

.device-card {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  }

  .card-header {
    background: linear-gradient(135deg, #7f8c8d 0%, #bdc3c7 100%);
    padding: 16px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #fff;

    &.is-active {
      background: linear-gradient(135deg, #4285F4 0%, #34A853 100%);
    }

    &.is-pending {
      background: linear-gradient(135deg, #FB8C00 0%, #F9A825 100%);
    }

    &.is-frozen {
      background: linear-gradient(135deg, #78909C 0%, #455A64 100%);
    }

    .header-status-tags {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 4px;
    }

    .header-title {
      display: flex;
      align-items: center;
      font-size: 16px;
      font-weight: 600;

      i { margin-right: 8px; font-size: 18px; }
      .device-name {
        max-width: 140px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }

  .card-body {
    padding: 20px;
    background: #fff;

    .info-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 12px;
      font-size: 13px;

      .label {
        flex-shrink: 0;
        color: #5f6368;
      }

      .value {
        min-width: 0;
        color: #202124;
      }

      &--device-code .value.code-font {
        margin-left: 8px;
        flex: 1;
        text-align: right;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .code-font { font-family: 'Courier New', Courier, monospace; background: #f1f3f4; padding: 2px 6px; border-radius: 4px; font-size: 12px; }
      .font-bold { font-weight: 600; }
      .text-gray { color: #80868b; }
    }

    .stats-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      margin: 16px 0;
      padding: 12px 0;
      border-top: 1px solid #f1f3f4;
      border-bottom: 1px solid #f1f3f4;

      .stat-item {
        text-align: center;
        .stat-value { font-size: 20px; font-weight: 700; margin-bottom: 4px; }
        .stat-label { font-size: 12px; color: #5f6368; }
        .text-blue { color: #1a73e8; }
        .text-green { color: #1e8e3e; }
      }
    }

    .bulletin-text {
      font-size: 12px;
      color: #5f6368;
      background: #f8f9fa;
      padding: 10px;
      border-radius: 6px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      margin-top: 12px;
      border-left: 3px solid #dadce0;
    }
  }

  .card-footer {
    padding: 12px 20px;
    background: #fafafa;
    border-top: 1px solid #ebeef5;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .btn-settings {
      color: #fff;

      &:hover,
      &:focus {
        color: #fff;
      }
    }
  }
}

.screen-filter-bar {
  margin-bottom: 4px;
}
.screen-filter-bar__query .el-form-item {
  margin-bottom: 8px;
}
.screen-filter-bar__actions {
  padding-top: 4px;
}

.screen-room-transfer {
  display: flex;
  justify-content: center;

  ::v-deep .el-transfer {
    display: flex;
    align-items: center;
    flex-wrap: nowrap;
  }

  ::v-deep .el-transfer-panel {
    width: 320px;
    flex-shrink: 0;
  }

  ::v-deep .el-transfer__buttons {
    flex-shrink: 0;
    display: inline-flex;
    flex-direction: column;
    justify-content: center;
    padding: 0 20px;
  }

  ::v-deep .el-transfer-panel__item {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.screen-dialog-form {
  ::v-deep .el-form-item {
    margin-bottom: 16px;
  }

  ::v-deep .el-form-item__label {
    white-space: nowrap;
    line-height: 32px;
    padding-right: 8px;
  }

  ::v-deep .el-form-item__content {
    line-height: 32px;

    .el-input,
    .el-select {
      width: 100%;
    }

    .el-textarea {
      width: 100%;
    }
  }
}

.screen-dialog-status-item {
  margin-bottom: 0;
}

.form-field-hint {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.form-field-hint--offset {
  margin: -4px 0 16px 96px;
}

.voice-bind-select {
  width: 100%;

  ::v-deep .el-select__tags {
    flex-wrap: nowrap;
    overflow: hidden;
  }

  ::v-deep .el-tag {
    max-width: 220px;
    display: inline-flex;
    align-items: center;
    height: 24px;
    line-height: 22px;

    .el-select__tags-text {
      display: inline-block;
      max-width: 180px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      vertical-align: middle;
    }
  }

  ::v-deep .el-input__inner {
    min-height: 32px;
    height: 32px !important;
  }
}

.voice-opt-name {
  float: left;
  color: #303133;
}

.voice-opt-meta {
  float: right;
  color: #909399;
  font-size: 12px;
  margin-left: 12px;
}
</style>
