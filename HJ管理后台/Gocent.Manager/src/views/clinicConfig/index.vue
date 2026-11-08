/**
 * 门边屏显示内容配置（CLINIC 设备）
 * 消费者：设备与屏幕 → 门边屏显示配置；存储键 CLINIC_CFG_{screenId}
 */
<template>
  <div class="clinic-config">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon" style="background:linear-gradient(135deg,#34A853,#0D7A31);">
          <i class="el-icon-mobile-phone" />
        </div>
        <div>
          <h2>门边屏显示配置</h2>
          <span class="header-sub">配置诊室门边屏的姓名脱敏、下一位等候和轮询间隔；底部公告请在「屏幕管理」维护</span>
        </div>
      </div>
      <div class="header-actions">
        <el-button type="primary" size="small" icon="el-icon-refresh" :loading="loading" @click="loadScreens">刷新设备列表</el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="7">
        <div class="device-panel">
          <div class="panel-title"><i class="el-icon-s-operation" /> 门边屏设备（{{ screens.length }}台）</div>
          <div v-loading="loading" class="device-list">
            <div
              v-for="s in screens"
              :key="s.id"
              class="device-item"
              :class="{ active: selectedScreen && selectedScreen.id === s.id }"
              @click="selectScreen(s)"
            >
              <div class="device-icon-wrap" :style="{ background: getScreenStateGradient(s.state) }">
                <i class="el-icon-mobile-phone" />
              </div>
              <div class="device-info">
                <div class="device-name">{{ s.screenName || s.name || '未命名' }}</div>
                <div class="device-meta">诊室门边屏 · {{ s.roomInfo || '未绑定诊室' }}</div>
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
              暂无 CLINIC 类型设备，请先在「屏幕管理」登记并设为诊室门边屏
            </div>
          </div>
        </div>
      </el-col>

      <el-col :span="17">
        <div v-if="!selectedScreen" class="no-select-tip">
          <p>← 请从左侧选择一台门边屏设备</p>
        </div>
        <template v-else>
          <el-form :model="cfg" label-width="160px" size="small" class="config-form">
            <el-form-item label="患者姓名脱敏">
              <el-switch v-model="cfg.nameDesensitize" active-text="开启" inactive-text="显示全名" />
              <span class="tip">作用于正在就诊、等候患者的姓名</span>
            </el-form-item>
            <el-form-item label="展示等候患者">
              <el-switch v-model="cfg.showWaitList" active-text="显示" inactive-text="隐藏" />
              <span class="tip">当前模板只显示下一位等候患者；关闭后不显示该区域</span>
            </el-form-item>
            <el-form-item label="数据轮询间隔">
              <el-input-number v-model="cfg.scrollIntervalSec" :min="3" :max="60" />
              <span class="tip">秒，终端按该间隔重新拉取队列</span>
            </el-form-item>
            <el-form-item label="底部公告">
              <el-input :value="effectiveBulletin" disabled />
              <span class="tip">当前模板底部公告条显示这段文字。请在「屏幕管理」中修改，本页保存不会改公告</span>
            </el-form-item>
          </el-form>
          <div class="save-bar">
            <el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button>
            <el-button plain @click="previewConfig">预览效果</el-button>
            <span v-if="lastSave" class="last-save">上次保存 {{ lastSave }}</span>
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

const DEFAULT_CFG = {
  nameDesensitize: true,
  showWaitList: true,
  scrollIntervalSec: 5
}

/** 与门边屏 BulletinBar 空公告时的默认文案一致 */
const CLINIC_DEFAULT_BULLETIN = '温馨提示：请保持安静'

export default {
  name: 'ClinicScreenConfig',
  data() {
    return {
      loading: false,
      saving: false,
      screens: [],
      selectedScreen: null,
      lastSave: '',
      cfg: { ...DEFAULT_CFG }
    }
  },
  created() {
    this.loadScreens()
  },
  computed: {
    /** 底部公告的实际展示文案：空公告时与终端默认句相同 */
    effectiveBulletin() {
      const screen = this.selectedScreen
      if (!screen) return CLINIC_DEFAULT_BULLETIN
      const raw = String(screen.bulletin || screen.Bulletin || '').trim()
      return raw || CLINIC_DEFAULT_BULLETIN
    }
  },
  methods: {
    loadScreens() {
      this.loading = true
      request({ url: '/api/Screens/GetScreens', method: 'post', data: { terminalType: 'CLINIC' } })
        .then(res => {
          const list = res.data || []
          this.screens = list.map(s => {
            const row = normalizeScreenRow(s)
            return {
              ...row,
              id: row.id || row.ID,
              screenName: row.name || row.screenName,
              bulletin: row.bulletin || row.Bulletin,
              roomInfo: row.roomInfo || row.RoomInfo
            }
          })
        })
        .catch(() => {
          this.screens = []
          this.$message.error('加载门边屏列表失败')
        })
        .finally(() => { this.loading = false })
    },
    getScreenStateGradient,
    getScreenStateLabel,
    getScreenStateTagType,
    getOnlineStatusLabel,
    getOnlineTagType,
    selectScreen(s) {
      this.selectedScreen = s
      this.loadClinicConfig(s.id)
    },
    loadClinicConfig(screenId) {
      const key = `CLINIC_CFG_${screenId}`
      getConfigByKey(key).then(res => {
        const item = res.data
        if (item && (item.configValue || item.ConfigValue)) {
          try {
            this.cfg = { ...DEFAULT_CFG, ...JSON.parse(item.configValue || item.ConfigValue) }
          } catch (e) {
            this.cfg = { ...DEFAULT_CFG }
          }
        } else {
          this.cfg = { ...DEFAULT_CFG }
        }
      }).catch(() => {
        this.cfg = { ...DEFAULT_CFG }
      })
    },
    saveConfig() {
      this.saving = true
      const key = `CLINIC_CFG_${this.selectedScreen.id}`
      const name = this.selectedScreen.screenName || this.selectedScreen.name
      saveConfig({
        configKey: key,
        configValue: JSON.stringify(this.cfg),
        description: `门边屏内容配置 - ${name}`
      })
        .then(() => {
          this.lastSave = new Date().toLocaleTimeString('zh-CN')
          this.$message.success('门边屏配置已保存，终端刷新后生效')
        })
        .catch(err => { this.$message.error(getApiErrorMessage(err, '保存失败')) })
        .finally(() => { this.saving = false })
    },
    /**
     * 新开门边屏展示页，只读观察现场画面。
     * 地址带 preview=1：不走设备绑定、不播报、GetDisplay 不刷新在线时间。
     * 展示的是已保存配置和当前队列，与现场屏同一设备编号。
     */
    previewConfig() {
      const screen = this.selectedScreen
      if (!screen || !screen.id) {
        this.$message.warning('请先选择一台门边屏')
        return
      }
      const deviceCode = String(screen.deviceCode || screen.DeviceCode || '').trim()
      if (!deviceCode) {
        this.$message.warning('该门边屏尚未登记设备编号，无法预览')
        return
      }
      const params = new URLSearchParams({
        deviceCode,
        preview: '1'
      })
      const url = `${this.clinicAppOrigin()}/clinic/#/?${params.toString()}`
      const winName = 'pdjhClinicPreview_' + String(screen.id).replace(/[^A-Za-z0-9_-]/g, '')
      const opened = window.open(url, winName)
      if (!opened) {
        this.$message.warning('浏览器拦截了新窗口，请允许弹出窗口后重试')
        return
      }
      opened.focus()
      const name = screen.screenName || screen.name || '门边屏'
      this.$message.success(`已打开「${name}」只读预览，不参与现场绑定与叫号`)
    },
    /**
     * 门边屏站点根地址。
     * 开发环境在 Vite 5175；生产与后台同域，路径为 /clinic/。
     */
    clinicAppOrigin() {
      const configured = (process.env.VUE_APP_CLINIC_ORIGIN || '').trim().replace(/\/$/, '')
      if (configured) return configured
      if (process.env.NODE_ENV === 'development') return 'http://127.0.0.1:5175'
      return window.location.origin
    }
  }
}
</script>

<style lang="scss" scoped>
.clinic-config { padding: 0; }
.page-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
  .header-left { display: flex; align-items: center; gap: 16px; }
  .header-icon { width: 52px; height: 52px; border-radius: 14px; display: flex; justify-content: center; align-items: center; i { font-size: 24px; color: white; } }
  h2 { margin: 0; font-size: 22px; font-weight: 700; color: #202124; }
  .header-sub { font-size: 13px; color: #80868B; margin-top: 3px; display: block; }
}
.device-panel { background: #FFF; border-radius: 12px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); height: 520px; display: flex; flex-direction: column; }
.panel-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; }
.device-list { flex: 1; overflow-y: auto; }
.device-item {
  display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 10px; cursor: pointer; margin-bottom: 8px;
  &:hover { background: #F8F9FA; }
  &.active { background: #E6F4EA; border: 1.5px solid #34A853; }
}
.device-icon-wrap { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; i { color: #fff; } }
.device-info { flex: 1; min-width: 0; }
.device-name { font-size: 13px; font-weight: 600; }
.device-meta { font-size: 12px; color: #80868B; }
.device-status-tags { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; flex-shrink: 0; }
.no-select-tip, .empty-tip { text-align: center; color: #80868B; padding: 80px 20px; }
.config-form { background: #fff; padding: 24px; border-radius: 12px; }
.tip { font-size: 12px; color: #80868B; margin-left: 8px; }
.save-bar { margin-top: 16px; display: flex; align-items: center; gap: 12px; }
.last-save { font-size: 12px; color: #80868B; }
</style>
