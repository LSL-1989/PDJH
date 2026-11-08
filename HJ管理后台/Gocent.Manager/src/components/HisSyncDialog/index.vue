<template>
  <el-dialog
    :title="title"
    v-model="dialogVisible"
    width="650px"
    @open="onOpen"
    @close="onClose"
  >
    <el-form label-width="140px">
      <el-form-item label="数据交互模式">
        <el-radio-group v-model="form.mode">
          <el-radio-button label="VIEW">数据库视图(中间表)</el-radio-button>
          <el-radio-button label="API">Webservice/HTTP接口</el-radio-button>
          <el-radio-button label="MANUAL">手工脱机维护</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-show="form.mode !== 'MANUAL'" label="数据源/接口地址">
        <el-input
          v-model="form.url"
          :placeholder="urlPlaceholder"
        />
      </el-form-item>
      <el-form-item v-show="form.mode !== 'MANUAL'" label="自动同步频率">
        <el-select v-model="form.cron" style="width: 100%">
          <el-option label="每天凌晨 02:00 (推荐)" value="0 2 * * *" />
          <el-option label="每小时定时增量" value="0 * * * *" />
          <el-option label="仅通过手动触发" value="MANUAL_ONLY" />
        </el-select>
      </el-form-item>
      <el-form-item v-show="form.mode !== 'MANUAL'" label="上次调度状态">
        <el-descriptions :column="1" border size="small" style="width: 100%;">
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType" size="small" effect="light">
              {{ statusLabel }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="form.lastSyncTime" label="最近执行">
            {{ formatTime(form.lastSyncTime) }}
          </el-descriptions-item>
          <el-descriptions-item
            v-if="form.lastStatus === 'SUCCESS' && form.lastSyncCount != null"
            label="合流数量"
          >
            {{ form.lastSyncCount }} {{ countUnit }}
          </el-descriptions-item>
          <el-descriptions-item v-if="form.lastError" label="错误信息">
            <span style="color: #F56C6C;">{{ form.lastError }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="probeMessage" label="拨测结果">
            {{ probeMessage }}
          </el-descriptions-item>
        </el-descriptions>
      </el-form-item>
    </el-form>
    <template #footer><div class="dialog-footer">
      <el-button plain @click="dialogVisible = false">关 闭</el-button>
      <el-button
        v-if="form.mode !== 'MANUAL'"
        plain
        :loading="probing"
        @click="handleProbe"
      >通道拨测</el-button>
      <el-button :loading="saving" @click="handleSave">保存配置</el-button>
      <el-button
        v-if="form.mode !== 'MANUAL'"
        type="primary"
        :loading="syncing"
        @click="handleSync"
      >立即同步</el-button>
    </div></template>
  </el-dialog>
</template>

<script>
import {
  syncDeptFromHis,
  getDeptHisSyncConfig,
  probeDeptHisSync,
  syncDoctorFromHis,
  getDoctorHisSyncConfig,
  probeDoctorHisSync
} from '@/api/department'

const DEFAULT_FORM = () => ({
  mode: 'MANUAL',
  url: '',
  cron: 'MANUAL_ONLY',
  lastStatus: 'NEVER',
  lastSyncTime: null,
  lastSyncCount: 0,
  lastError: null
})

export default {
  name: 'HisSyncDialog',
  props: {
    modelValue: { type: Boolean, default: false },
    visible: { type: Boolean, default: false },
    entityType: {
      type: String,
      required: true,
      validator: v => ['dept', 'doctor'].includes(v)
    },
    title: { type: String, required: true },
    countUnit: { type: String, default: '条' }
  },
  data() {
    return {
      form: DEFAULT_FORM(),
      saving: false,
      syncing: false,
      probing: false,
      probeMessage: ''
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.modelValue || this.visible },
      set(val) {
        this.$emit('update:modelValue', val)
        this.$emit('update:visible', val)
      }
    },
    urlPlaceholder() {
      return this.entityType === 'dept'
        ? '示例: v_his_dept_list 或 http://his.local:8080/api/depts'
        : '示例: v_his_doctor_list 或 http://his.local:8080/api/doctors'
    },
    statusLabel() {
      const map = {
        NEVER: '尚未同步',
        SAVED: '配置已保存，待同步',
        SUCCESS: '同步成功',
        FAILED: '同步失败'
      }
      return map[this.form.lastStatus] || '未知'
    },
    statusTagType() {
      const map = {
        NEVER: 'info',
        SAVED: '',
        SUCCESS: 'success',
        FAILED: 'danger'
      }
      return map[this.form.lastStatus] || 'info'
    }
  },
  methods: {
    getConfigApi() {
      return this.entityType === 'dept' ? getDeptHisSyncConfig : getDoctorHisSyncConfig
    },
    syncApi() {
      return this.entityType === 'dept' ? syncDeptFromHis : syncDoctorFromHis
    },
    probeApi() {
      return this.entityType === 'dept' ? probeDeptHisSync : probeDoctorHisSync
    },
    onOpen() {
      this.probeMessage = ''
      this.loadConfig()
    },
    onClose() {
      this.form = DEFAULT_FORM()
      this.probeMessage = ''
    },
    loadConfig() {
      this.getConfigApi()().then(res => {
        if (res.data) {
          this.form = { ...DEFAULT_FORM(), ...res.data }
        }
      }).catch(() => {})
    },
    formatTime(t) {
      if (!t) return '—'
      const d = new Date(t)
      if (Number.isNaN(d.getTime())) return String(t)
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    },
    validateNonManual() {
      if (this.form.mode === 'MANUAL') return true
      if (!this.form.url || !this.form.url.trim()) {
        this.$message.warning('请填写数据源地址或视图名称')
        return false
      }
      if (this.form.mode === 'API' && !/^https?:\/\//i.test(this.form.url.trim())) {
        this.$message.warning('API 模式请填写以 http 或 https 开头的完整地址')
        return false
      }
      return true
    },
    handleSave() {
      this.saving = true
      this.syncApi()({ ...this.form, triggerNow: false })
        .then(res => {
          const msg = (res.data && res.data.message) || res.msg || '配置已保存'
          this.$message.success(msg)
          this.loadConfig()
        })
        .catch(err => {
          this.$message.error(err.message || '保存失败')
          this.loadConfig()
        })
        .finally(() => { this.saving = false })
    },
    handleProbe() {
      if (!this.validateNonManual()) return
      this.probing = true
      this.probeApi()({ ...this.form })
        .then(res => {
          const data = res.data || {}
          this.probeMessage = data.message || '拨测完成'
          if (data.reachable) {
            this.$message.success(this.probeMessage)
          } else {
            this.$message.warning(this.probeMessage)
          }
        })
        .catch(err => {
          this.probeMessage = err.message || '拨测失败'
          this.$message.error(this.probeMessage)
        })
        .finally(() => { this.probing = false })
    },
    handleSync() {
      if (!this.validateNonManual()) return
      this.$confirm('立即同步将从配置的数据源拉取并写入本地数据，是否继续？', '确认同步', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.syncing = true
        this.syncApi()({ ...this.form, triggerNow: true })
          .then(res => {
            const msg = (res.data && res.data.message) || res.msg || '同步完成'
            this.$message.success(msg)
            this.loadConfig()
            this.$emit('synced')
            this.dialogVisible = false
          })
          .catch(err => {
            this.$message.error(err.message || 'HIS 同步失败')
            this.loadConfig()
          })
          .finally(() => { this.syncing = false })
      }).catch(() => {})
    }
  }
}
</script>
