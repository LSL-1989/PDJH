<template>
  <div class="screen-config">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon" style="background:linear-gradient(135deg,#34A853,#0D7A31);">
          <i class="el-icon-monitor" />
        </div>
        <div>
          <h2>候诊区大屏显示配置</h2>
          <span class="header-sub">配置候诊区大屏（SCREEN）的列布局、号码与姓名、字幕和语音开关；门边屏请使用「门边屏显示配置」</span>
        </div>
      </div>
      <div class="header-actions">
        <el-button type="primary" size="small" icon="el-icon-refresh" :loading="loading" @click="loadScreens">刷新设备列表</el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：设备列表 -->
      <el-col :span="7">
        <div class="device-panel">
          <div class="panel-title"><i class="el-icon-s-operation" /> 已注册屏幕设备（{{ screens.length }}台）</div>
          <div v-loading="loading" class="device-list">
            <div
              v-for="s in screens"
              :key="s.id"
              class="device-item"
              :class="{ active: selectedScreen && selectedScreen.id === s.id }"
              @click="selectScreen(s)"
            >
              <div class="device-icon-wrap" :style="{ background: getScreenStateGradient(s.state) }">
                <i :class="getScreenIcon(s.screenType)" />
              </div>
              <div class="device-info">
                <div class="device-name">{{ s.screenName || s.name || '未命名屏幕' }}</div>
                <div class="device-meta">候诊区大屏 · {{ s.deptName || '未绑定科室' }}</div>
              </div>
              <div class="device-status-tags">
                <el-tag :type="getScreenStateTagType(s.state)" size="small" effect="dark">
                  {{ getScreenStateLabel(s.state, s.stateModel) }}
                </el-tag>
                <el-tag :type="getOnlineTagType(s.online, s.deviceCode)" size="small" effect="plain">
                  {{ getOnlineStatusLabel(s.online, s.deviceCode) }}
                </el-tag>
              </div>
            </div>
            <div v-if="screens.length === 0 && !loading" class="empty-tip">
              <i class="el-icon-monitor" style="font-size:40px;color:#DADCE0;display:block;margin-bottom:8px;" />
              暂无注册设备
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右侧：配置面板 -->
      <el-col :span="17">
        <div v-if="!selectedScreen" class="no-select-tip">
          <i class="el-icon-arrow-left" style="font-size:48px;color:#DADCE0;" />
          <p>← 请从左侧选择一台屏幕设备进行配置</p>
        </div>

        <template v-else>
          <el-tabs v-model="configTab" type="border-card">

            <!-- Tab1: 显示布局 -->
            <el-tab-pane label="📐 显示布局" name="layout">
              <el-form :model="cfg" label-width="160px" size="small">
                <el-form-item label="叫号列展示模式">
                  <el-radio-group v-model="cfg.layout">
                    <el-radio-button label="1COL">单列（6行）</el-radio-button>
                    <el-radio-button label="2COL">双列（8行）</el-radio-button>
                    <el-radio-button label="3COL">三列（12行）</el-radio-button>
                    <el-radio-button label="FULL">全屏大号</el-radio-button>
                  </el-radio-group>
                  <span class="tip">只作用于默认候诊模板 /default 的左右分区和每页行数。多诊室按行、按列，以及药房、检验窗口屏版式是固定的，改这项画面不变</span>
                </el-form-item>
                <el-form-item label="显示字段">
                  <el-checkbox-group v-model="cfg.displayFields">
                    <el-checkbox label="QUEUE_NO">叫号号码</el-checkbox>
                    <el-checkbox label="PATIENT_NAME">患者姓名</el-checkbox>
                  </el-checkbox-group>
                  <span class="tip">当前模板可开关号码和姓名；诊室名、医生、排队人数按版式固定显示</span>
                </el-form-item>
                <el-form-item label="患者姓名脱敏">
                  <el-switch v-model="cfg.nameDesensitize" active-text="开启（张**）" inactive-text="显示全名" />
                </el-form-item>
                <el-form-item label="字体大小">
                  <el-slider v-model="cfg.fontSize" :min="12" :max="48" show-input style="width:300px;" />
                  <span class="tip">叫号号码与患者姓名的主字体大小（px）</span>
                </el-form-item>
                <el-form-item label="主题色">
                  <el-color-picker v-model="cfg.themeColor" :predefine="themePresets" />
                  <span class="tip" style="margin-left:12px;">当前: {{ cfg.themeColor }}。底栏会跟着变；药房、检验窗口屏的标题、窗口号和叫号号码也会跟着变。诊室卡片底色不变。保存后刷新大屏才生效</span>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- Tab2: 滚动字幕 -->
            <el-tab-pane label="📢 滚动字幕" name="marquee">
              <el-form :model="cfg" label-width="160px" size="small">
                <el-form-item label="底部字幕开关">
                  <el-switch v-model="cfg.marqueeEnabled" active-text="开启" inactive-text="关闭" />
                </el-form-item>
                <el-form-item v-if="cfg.marqueeEnabled" label="字幕内容">
                  <el-input
                    v-model="cfg.marqueeText"
                    type="textarea"
                    :rows="3"
                    placeholder="例如：如有特殊情况，请告知导诊护士。就诊时请携带就诊卡及相关资料。"
                  />
                </el-form-item>
                <el-form-item v-if="cfg.marqueeEnabled" label="滚动速度">
                  <el-select v-model="cfg.marqueeSpeed" style="width:140px;">
                    <el-option label="慢速" value="slow" />
                    <el-option label="中速" value="normal" />
                    <el-option label="快速" value="fast" />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="cfg.marqueeEnabled" label="字幕说明">
                  <span class="tip">屏幕管理里已填写公告时，底栏优先显示公告，此处文案作为没有公告时的内容</span>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- Tab3: 语音播报 -->
            <el-tab-pane label="🔊 语音播报" name="voice">
              <el-form :model="cfg" label-width="160px" size="small">
                <el-form-item label="语音叫号开关">
                  <el-switch v-model="cfg.voiceEnabled" active-text="开启" inactive-text="关闭" />
                  <span class="tip">关闭后本屏不播报。播报文案、次数和语速使用屏幕管理与叫号广播，不在此页单独配置</span>
                </el-form-item>
              </el-form>
            </el-tab-pane>

          </el-tabs>

          <div class="save-bar">
            <el-button type="primary" icon="el-icon-check" :loading="saving" @click="saveScreenConfig">
              保存 「{{ selectedScreen.screenName || selectedScreen.name }}」 配置
            </el-button>
            <el-button plain @click="previewConfig">预览效果</el-button>
            <span v-if="lastSave" class="last-save">最后保存：{{ lastSave }}</span>
          </div>
        </template>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getConfigByKey, saveConfig } from '@/api/systemConfig'
import request, { getApiErrorMessage } from '@/utils/request'
import {
  getScreenStateGradient,
  getScreenStateLabel,
  getScreenStateTagType,
  getOnlineStatusLabel,
  getOnlineTagType,
  normalizeScreenRow
} from '@/utils/screenState'

/** 当前候诊模板实际会读取的展示字段：号码、姓名 */
const SUPPORTED_FIELDS = ['QUEUE_NO', 'PATIENT_NAME']

/**
 * 模板没有对应画面或播报能力的旧字段。
 * 保存时丢掉，避免页面上已经删除的项继续写回 SCREEN_CFG。
 */
const DROPPED_CFG_KEYS = [
  'darkMode',
  'welcomeTitle',
  'mediaEnabled',
  'mediaType',
  'mediaIntervalSec',
  'mediaUrls',
  'voiceTemplate',
  'voiceVolume',
  'voiceLang'
]

const DEFAULT_CFG = {
  layout: '2COL',
  displayFields: ['QUEUE_NO', 'PATIENT_NAME'],
  nameDesensitize: true,
  fontSize: 32,
  themeColor: '#1A73E8',
  marqueeEnabled: true,
  marqueeText: '如有特殊情况，请告知导诊护士。就诊时请携带就诊卡及相关资料。',
  marqueeSpeed: 'normal',
  voiceEnabled: true
}

export default {
  name: 'ScreenContentConfig',
  data() {
    return {
      loading: false,
      saving: false,
      screens: [],
      selectedScreen: null,
      configTab: 'layout',
      lastSave: '',
      cfg: { ...DEFAULT_CFG, displayFields: [...DEFAULT_CFG.displayFields] },
      themePresets: ['#1A73E8', '#34A853', '#EA4335', '#FBBC04', '#9C27B0', '#FF6D00', '#00BCD4', '#212121']
    }
  },
  created() { this.loadScreens() },
  methods: {
    loadScreens() {
      this.loading = true
      request({ url: '/api/Screens/GetScreens', method: 'post', data: {}})
        .then(res => {
          const list = res.data || []
          this.screens = list
            .filter(s => (s.terminalType || s.TerminalType || 'SCREEN') === 'SCREEN')
            .map(s => {
              const row = normalizeScreenRow(s)
              return {
                ...row,
                id: row.id || row.ID,
                screenName: row.name || row.screenName,
                deptName: row.deptName || row.DeptName
              }
            })
        })
        .catch(() => {
          this.screens = []
          this.$message.error('加载屏幕列表失败')
        })
        .finally(() => { this.loading = false })
    },
    selectScreen(s) {
      this.selectedScreen = s
      this.configTab = 'layout'
      this.loadScreenConfig(s.id)
    },
    loadScreenConfig(screenId) {
      const key = `SCREEN_CFG_${screenId}`
      getConfigByKey(key).then(res => {
        const item = res.data
        if (item && (item.configValue || item.ConfigValue)) {
          try {
            this.cfg = this.normalizeCfg(JSON.parse(item.configValue || item.ConfigValue))
          } catch (e) {
            this.cfg = { ...DEFAULT_CFG, displayFields: [...DEFAULT_CFG.displayFields] }
          }
        } else {
          this.cfg = { ...DEFAULT_CFG, displayFields: [...DEFAULT_CFG.displayFields] }
        }
      }).catch(() => {
        this.cfg = { ...DEFAULT_CFG, displayFields: [...DEFAULT_CFG.displayFields] }
        this.$message.warning('未找到已保存配置，使用默认值')
      })
    },
    /**
     * 只保留当前模板会生效的配置。
     * 展示字段仅号码、姓名；浅色主题、欢迎标题、侧栏素材和语音文案/音量/语言不写回。
     */
    normalizeCfg(raw) {
      const src = raw && typeof raw === 'object' ? raw : {}
      const next = { ...DEFAULT_CFG, ...src, displayFields: [...DEFAULT_CFG.displayFields] }
      const fields = Array.isArray(src.displayFields) ? src.displayFields : DEFAULT_CFG.displayFields
      next.displayFields = fields.filter(field => SUPPORTED_FIELDS.includes(field))
      DROPPED_CFG_KEYS.forEach(key => { delete next[key] })
      return next
    },
    saveScreenConfig() {
      this.saving = true
      const key = `SCREEN_CFG_${this.selectedScreen.id}`
      const payload = this.normalizeCfg(this.cfg)
      this.cfg = payload
      saveConfig({ configKey: key, configValue: JSON.stringify(payload), description: `大屏内容配置 - ${this.selectedScreen.screenName || this.selectedScreen.name}` })
        .then(() => {
          this.lastSave = new Date().toLocaleTimeString('zh-CN')
          this.$message.success('屏幕配置已保存，候诊区大屏刷新后将按新配置显示')
        })
        .catch((err) => { this.$message.error(getApiErrorMessage(err, '保存失败')) })
        .finally(() => { this.saving = false })
    },
    /**
     * 新开候诊区大屏对应模板页，只读观察现场画面。
     * 地址带 preview=1：不走设备绑定、不播报、不调用叫号写接口。
     * 展示的是已保存配置和当前队列，与现场屏同一路由（templateRoute + screenId）。
     */
    previewConfig() {
      const screen = this.selectedScreen
      if (!screen || !screen.id) {
        this.$message.warning('请先选择一台屏幕')
        return
      }
      const route = this.normalizePreviewRoute(screen.templateRoute || screen.TemplateRoute)
      const params = new URLSearchParams({
        screenId: String(screen.id),
        preview: '1'
      })
      const origin = this.screenAppOrigin()
      const url = `${origin}/screen/#${route}?${params.toString()}`
      const winName = 'pdjhScreenPreview_' + String(screen.id).replace(/[^A-Za-z0-9_-]/g, '')
      const opened = window.open(url, winName)
      if (!opened) {
        this.$message.warning('浏览器拦截了新窗口，请允许弹出窗口后重试')
        return
      }
      opened.focus()
      const name = screen.screenName || screen.name || '候诊屏'
      this.$message.success(`已打开「${name}」只读预览，不参与现场绑定与叫号`)
    },
    /** 模板路由规范化；空值与旧 /pdjh1 都进默认候诊模板 */
    normalizePreviewRoute(raw) {
      let route = String(raw || '/default').trim()
      if (!route.startsWith('/')) route = `/${route}`
      if (route === '/pdjh1') route = '/default'
      return route
    },
    /**
     * 候诊大屏站点根地址。
     * 开发环境大屏在 Vite 5173；生产与后台同域，路径为 /screen/。
     */
    screenAppOrigin() {
      const configured = (process.env.VUE_APP_SCREEN_ORIGIN || '').trim().replace(/\/$/, '')
      if (configured) return configured
      if (process.env.NODE_ENV === 'development') return 'http://127.0.0.1:5173'
      return window.location.origin
    },
    getScreenIcon(type) {
      return { GLOBAL: 'el-icon-full-screen', CLINIC: 'el-icon-user', PHARMACY: 'el-icon-s-shop', EXAM: 'el-icon-first-aid-kit' }[type] || 'el-icon-monitor'
    },
    getScreenStateGradient,
    getScreenStateLabel,
    getScreenStateTagType,
    getOnlineStatusLabel,
    getOnlineTagType,
    getScreenTypeName(type) {
      return { GLOBAL: '全院导诊大屏', CLINIC: '诊室叫号屏', PHARMACY: '药房取药屏', EXAM: '体检引导屏' }[type] || '通用屏幕'
    }
  }
}
</script>

<style lang="scss" scoped>
.screen-config { padding: 0; }
.page-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
  .header-left { display: flex; align-items: center; gap: 16px; }
  .header-icon { width: 52px; height: 52px; border-radius: 14px; display: flex; justify-content: center; align-items: center; i { font-size: 24px; color: white; } }
  h2 { margin: 0; font-size: 22px; font-weight: 700; color: #202124; }
  .header-sub { font-size: 13px; color: #80868B; margin-top: 3px; display: block; }
  .header-actions { display: flex; gap: 10px; }
}
.device-panel { background: #FFF; border-radius: 12px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); height: 600px; display: flex; flex-direction: column; }
.panel-title { font-size: 14px; font-weight: 600; color: #202124; margin-bottom: 12px; i { margin-right: 6px; color: #1A73E8; } }
.device-list { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.device-item {
  display: flex; align-items: center; gap: 10px; padding: 10px 12px;
  border-radius: 10px; cursor: pointer; transition: all 0.2s; border: 1.5px solid transparent;
  &:hover { background: #F8F9FA; }
  &.active { background: #E8F0FE; border-color: #1A73E8; }
}
.device-icon-wrap { width: 36px; height: 36px; border-radius: 8px; display: flex; justify-content: center; align-items: center; i { font-size: 18px; color: white; } flex-shrink: 0; }
.device-info { flex: 1; min-width: 0; }
.device-name { font-size: 13px; font-weight: 600; color: #202124; }
.device-meta { font-size: 12px; color: #80868B; margin-top: 2px; }
.device-status-tags { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; flex-shrink: 0; }
.no-select-tip { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 400px; color: #80868B; font-size: 14px; background: #FFF; border-radius: 12px; }
.empty-tip { text-align: center; color: #80868B; padding: 40px 0; font-size: 14px; }
.tip { font-size: 12px; color: #80868B; display: block; margin-top: 4px; }
.save-bar { margin-top: 16px; padding: 14px 20px; background: #F8F9FA; border-radius: 8px; display: flex; align-items: center; gap: 12px; .last-save { font-size: 12px; color: #80868B; margin-left: auto; } }
</style>
