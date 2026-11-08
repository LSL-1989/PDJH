<template>
  <div class="app-container system-config-page">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="page-tip"
      title="全局字典用于存储键值型系统参数。叫号规则（RULE_*）建议在「叫号规则配置中心」可视化维护；本页可查看、编辑全部原始配置项。"
    />

    <el-card shadow="never">
      <div class="filter-container list-toolbar">
        <div class="list-toolbar__left">
          <el-input
            v-model="listQuery.keyword"
            placeholder="搜索配置键 / 值 / 说明"
            clearable
            class="filter-item search-input"
            prefix-icon="el-icon-search"
            @keyup.enter="handleFilter"
            @clear="handleFilter"
          />
          <el-select
            v-model="listQuery.category"
            placeholder="类型筛选"
            clearable
            class="filter-item"
            @change="handleFilter"
          >
            <el-option label="全部" value="" />
            <el-option label="叫号规则 (RULE_*)" value="rule" />
            <el-option label="业务参数" value="biz" />
            <el-option label="其他" value="other" />
          </el-select>
          <el-button type="primary" icon="el-icon-search" @click="handleFilter">查询</el-button>
          <el-button icon="el-icon-refresh" @click="fetchData">刷新</el-button>
        </div>
        <div class="list-toolbar__right">
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增配置</el-button>
          <el-button icon="el-icon-s-operation" @click="goRuleCenter">叫号规则中心</el-button>
        </div>
      </div>

      <el-table
        v-loading="listLoading"
        :data="filteredList"
        element-loading-text="加载中"
        border
        fit
        highlight-current-row
        empty-text="暂无配置项，可点击「新增配置」添加"
      >
        <el-table-column align="center" label="序号" width="60">
          <template #default="scope">
            {{ scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column label="配置键 (Key)" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <el-tag v-if="isRuleKey(scope.row.ConfigKey)" size="small" type="warning" style="margin-right:6px">规则</el-tag>
            <span>{{ scope.row.ConfigKey }}</span>
          </template>
        </el-table-column>
        <el-table-column label="配置值 (Value)" min-width="260">
          <template #default="scope">
            <el-tooltip :content="scope.row.ConfigValue || '—'" placement="top" :disabled="!scope.row.ConfigValue || scope.row.ConfigValue.length < 80">
              <span class="value-preview">{{ formatValuePreview(scope.row.ConfigValue) }}</span>
            </el-tooltip>
            <el-button
              v-if="scope.row.ConfigValue" text
              size="small"
              icon="el-icon-document-copy"
              @click="copyText(scope.row.ConfigValue)"
            >复制</el-button>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ scope.row.Description || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="租户" width="100" align="center">
          <template #default="scope">
            <span>{{ scope.row.TenantId || '—' }}</span>
          </template>
        </el-table-column>
        <table-action-column :count="2">
          <template #default="{ row }">
            <table-action-icon icon="el-icon-edit" tip="编辑" @click="handleUpdate(row)" />
            <table-action-icon
              icon="el-icon-delete"
              tip="删除"
              type="danger"
              :disabled="isProtectedKey(row.ConfigKey)"
              @click="handleDelete(row)"
            />
          </template>
        </table-action-column>
      </el-table>

      <div class="table-footer">
        共 {{ filteredList.length }} 条
        <span v-if="listQuery.keyword || listQuery.category">（已筛选，全量 {{ list.length }} 条）</span>
      </div>
    </el-card>

    <form-drawer
      :title="dialogStatus === 'create' ? '新增系统配置' : '编辑系统配置'"
      v-model="dialogFormVisible"
      size="520px"
      @close="onDialogClose"
    >
      <el-form
        ref="dataForm"
        :model="temp"
        :rules="formRules"
        label-position="left"
        label-width="120px"
      >
        <el-form-item v-if="dialogStatus === 'create'" label="常用模板">
          <el-select
            v-model="presetKey"
            placeholder="可选：从模板快速填充"
            clearable
            style="width: 100%"
            @change="applyPreset"
          >
            <el-option
              v-for="p in presetOptions"
              :key="p.key"
              :label="p.label"
              :value="p.key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="配置键 (Key)" prop="ConfigKey">
          <el-input
            v-model="temp.ConfigKey"
            :disabled="dialogStatus === 'update'"
            placeholder="字母开头，仅含字母数字下划线，如 HospitalName"
          />
        </el-form-item>
        <el-form-item label="配置值 (Value)" prop="ConfigValue">
          <el-input v-model="temp.ConfigValue" type="textarea" :rows="6" placeholder="普通文本或 JSON（RULE_* 建议为合法 JSON）" />
          <div v-if="isRuleKey(temp.ConfigKey)" class="form-hint">
            <el-button text size="small" @click="formatJsonValue">格式化 JSON</el-button>
            <el-button text size="small" @click="validateJsonValue">校验 JSON</el-button>
          </div>
        </el-form-item>
        <el-form-item label="配置说明">
          <el-input v-model="temp.Description" type="textarea" :rows="2" placeholder="便于运维理解的说明" />
        </el-form-item>
        <el-form-item label="租户 ID">
          <el-input v-model="temp.TenantId" placeholder="选填，多租户场景使用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确认</el-button>
      </template>
    </form-drawer>
  </div>
</template>

<script>
import { getAllConfigs, saveConfig, deleteConfig } from '@/api/systemConfig'
import { getApiErrorMessage } from '@/utils/request'

const PROTECTED_KEYS = ['RULE_OUTPATIENT', 'RULE_PHARMACY', 'RULE_EXAM', 'RULE_MEDICAL_TECH', 'RULE_NOTIFICATION']

const PRESET_OPTIONS = [
  { key: 'HospitalName', label: '医院名称 (HospitalName)', value: '欢匠医院', desc: '全院显示用医院名称' },
  { key: 'HospitalLogo', label: '医院 Logo (HospitalLogo)', value: '/uploads/intros/hospital-logo.png', desc: '门边屏/终端顶部医院 Logo 图片地址' },
  { key: 'HospitalShortName', label: '医院简称 (HospitalShortName)', value: '欢匠', desc: '大屏/叫号端简短名称' },
  { key: 'SignInTip', label: '签到提示语 (SignInTip)', value: '请在自助机完成签到', desc: '签到端默认提示' },
  { key: 'CallVoicePrefix', label: '叫号语音前缀 (CallVoicePrefix)', value: '请', desc: '语音播报前缀文案' }
]

function normalizeConfig(row) {
  if (!row) return null
  return {
    ConfigKey: row.ConfigKey || row.configKey || '',
    ConfigValue: row.ConfigValue != null ? row.ConfigValue : (row.configValue != null ? row.configValue : ''),
    Description: row.Description != null ? row.Description : (row.description != null ? row.description : ''),
    TenantId: row.TenantId || row.tenantId || ''
  }
}

export default {
  name: 'SystemConfig',
  data() {
    const validateConfigKey = (rule, value, callback) => {
      if (!value || !String(value).trim()) {
        callback(new Error('配置键不能为空'))
        return
      }
      if (!/^[A-Za-z][A-Za-z0-9_]*$/.test(String(value).trim())) {
        callback(new Error('须字母开头，仅允许字母、数字、下划线'))
        return
      }
      callback()
    }
    return {
      list: [],
      filteredList: [],
      listLoading: true,
      submitLoading: false,
      dialogFormVisible: false,
      dialogStatus: '',
      presetKey: '',
      listQuery: {
        keyword: '',
        category: ''
      },
      temp: {
        ConfigKey: '',
        ConfigValue: '',
        Description: '',
        TenantId: ''
      },
      presetOptions: PRESET_OPTIONS,
      formRules: {
        ConfigKey: [{ required: true, validator: validateConfigKey, trigger: 'blur' }],
        ConfigValue: [{ required: true, message: '配置值不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    isRuleKey(key) {
      return key && String(key).startsWith('RULE_')
    },
    isProtectedKey(key) {
      return PROTECTED_KEYS.includes(key)
    },
    formatValuePreview(val) {
      if (val == null || val === '') return '—'
      const s = String(val)
      if (s.length <= 120) return s
      return s.slice(0, 120) + '…'
    },
    normalizeList(rows) {
      return (rows || []).map(normalizeConfig).filter(Boolean)
    },
    applyFilter() {
      let data = [...this.list]
      const kw = (this.listQuery.keyword || '').trim().toLowerCase()
      if (kw) {
        data = data.filter((row) => {
          const blob = [row.ConfigKey, row.ConfigValue, row.Description].join(' ').toLowerCase()
          return blob.includes(kw)
        })
      }
      const cat = this.listQuery.category
      if (cat === 'rule') {
        data = data.filter((row) => this.isRuleKey(row.ConfigKey))
      } else if (cat === 'biz') {
        data = data.filter((row) => !this.isRuleKey(row.ConfigKey) && row.ConfigKey && !row.ConfigKey.startsWith('_'))
      } else if (cat === 'other') {
        data = data.filter((row) => row.ConfigKey && row.ConfigKey.startsWith('_'))
      }
      this.filteredList = data
    },
    handleFilter() {
      this.applyFilter()
    },
    fetchData() {
      this.listLoading = true
      getAllConfigs()
        .then((response) => {
          this.list = this.normalizeList(response.data)
          this.applyFilter()
        })
        .catch((err) => {
          this.$message.error(getApiErrorMessage(err, '加载配置列表失败'))
          this.list = []
          this.filteredList = []
        })
        .finally(() => {
          this.listLoading = false
        })
    },
    resetTemp() {
      this.temp = {
        ConfigKey: '',
        ConfigValue: '',
        Description: '',
        TenantId: ''
      }
      this.presetKey = ''
    },
    onDialogClose() {
      this.resetTemp()
      this.$refs.dataForm && this.$refs.dataForm.clearValidate()
    },
    applyPreset(key) {
      const preset = PRESET_OPTIONS.find((p) => p.key === key)
      if (!preset) return
      this.temp.ConfigKey = preset.key
      this.temp.ConfigValue = preset.value
      this.temp.Description = preset.desc
    },
    handleAdd() {
      this.resetTemp()
      this.dialogStatus = 'create'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs.dataForm && this.$refs.dataForm.clearValidate()
      })
    },
    handleUpdate(row) {
      this.temp = Object.assign({}, normalizeConfig(row))
      this.dialogStatus = 'update'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs.dataForm && this.$refs.dataForm.clearValidate()
      })
    },
    buildPayload() {
      return {
        ConfigKey: String(this.temp.ConfigKey).trim(),
        ConfigValue: this.temp.ConfigValue,
        Description: this.temp.Description || '',
        TenantId: this.temp.TenantId || ''
      }
    },
    submitForm() {
      this.$refs.dataForm.validate((valid) => {
        if (!valid) return
        if (this.isRuleKey(this.temp.ConfigKey)) {
          try {
            if (this.temp.ConfigValue && this.temp.ConfigValue.trim()) {
              JSON.parse(this.temp.ConfigValue)
            }
          } catch (e) {
            this.$message.warning('RULE_* 配置值应为合法 JSON，可使用「校验 JSON」检查')
            return
          }
        }
        this.submitLoading = true
        saveConfig(this.buildPayload())
          .then(() => {
            this.dialogFormVisible = false
            this.$message.success(this.dialogStatus === 'create' ? '创建成功' : '更新成功')
            this.fetchData()
          })
          .catch((err) => {
            this.$message.error(getApiErrorMessage(err, '保存失败'))
          })
          .finally(() => {
            this.submitLoading = false
          })
      })
    },
    handleDelete(row) {
      const key = row.ConfigKey
      const extra = this.isProtectedKey(key)
        ? '该键为系统叫号规则配置，删除后相关调度将回退默认逻辑，请谨慎操作。'
        : ''
      this.$confirm(`确认删除配置项「${key}」？${extra}`, '提示', {
        type: 'warning',
        confirmButtonText: extra ? '仍要删除' : '确定',
        cancelButtonText: '取消'
      })
        .then(() => {
          return deleteConfig({ ConfigKey: key })
        })
        .then(() => {
          this.$message.success('删除成功')
          this.fetchData()
        })
        .catch((err) => {
          if (err !== 'cancel') {
            this.$message.error(getApiErrorMessage(err, '删除失败'))
          }
        })
    },
    formatJsonValue() {
      if (!this.temp.ConfigValue || !this.temp.ConfigValue.trim()) {
        this.$message.info('请先输入配置值')
        return
      }
      try {
        this.temp.ConfigValue = JSON.stringify(JSON.parse(this.temp.ConfigValue), null, 2)
        this.$message.success('已格式化')
      } catch (e) {
        this.$message.error('不是合法的 JSON')
      }
    },
    validateJsonValue() {
      if (!this.temp.ConfigValue || !this.temp.ConfigValue.trim()) {
        this.$message.info('配置值为空')
        return
      }
      try {
        JSON.parse(this.temp.ConfigValue)
        this.$message.success('JSON 格式正确')
      } catch (e) {
        this.$message.error('JSON 格式错误：' + e.message)
      }
    },
    copyText(text) {
      const input = document.createElement('textarea')
      input.value = text
      document.body.appendChild(input)
      input.select()
      try {
        document.execCommand('copy')
        this.$message.success('已复制到剪贴板')
      } catch (e) {
        this.$message.error('复制失败')
      }
      document.body.removeChild(input)
    },
    goRuleCenter() {
      this.$router.push({ name: 'ruleCenterIndex' })
    }
  }
}
</script>

<style lang="scss" scoped>
.system-config-page {
  .page-tip {
    margin-bottom: 16px;
  }
  .filter-container {
    .filter-item {
      width: 160px;
    }
    .search-input {
      width: 260px;
    }
  }
  .value-preview {
    word-break: break-all;
    vertical-align: middle;
  }
  .table-footer {
    margin-top: 12px;
    font-size: 13px;
    color: #909399;
  }
  .form-hint {
    margin-top: 4px;
  }
}
</style>
