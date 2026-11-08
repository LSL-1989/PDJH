<template>
  <div class="app-container">
    <el-card>
      <div class="list-toolbar list-toolbar--flat">
        <div class="list-toolbar__left">
          <el-button icon="el-icon-refresh" @click="loadList">刷新</el-button>
          <span class="toolbar-hint">
            独立电脑安装「HJ音响终端」；此处仅维护音响台账。与候诊大屏的绑定请到「屏幕管理」中按屏勾选（支持一诊区多屏共用一台音响）。
          </span>
        </div>
        <div class="list-toolbar__right">
          <el-button type="primary" icon="el-icon-plus" @click="openEdit()">新增音响</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="devices" border stripe>
        <el-table-column prop="deviceCode" label="设备编号" width="140" />
        <el-table-column prop="name" label="名称" width="140" />
        <el-table-column prop="location" label="位置" min-width="100" />
        <el-table-column prop="boundSummary" label="被哪些大屏绑定" min-width="200" />
        <el-table-column prop="playMode" label="模式" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.state === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.state === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <table-action-column :count="2">
          <template #default="{ row }">
            <table-action-icon icon="el-icon-edit" tip="编辑" @click="openEdit(row)" />
            <table-action-icon icon="el-icon-delete" tip="删除" type="danger" @click="removeRow(row)" />
          </template>
        </table-action-column>
      </el-table>
    </el-card>

    <form-drawer :title="form.id ? '编辑音响' : '新增音响'" v-model="visible">
      <el-form ref="formRef" :model="form" label-width="110px">
        <el-form-item label="设备编号" required>
          <el-input v-model="form.deviceCode" placeholder="如 VOICE-3F-01" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="如 内科候诊区音响" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="播报模式">
          <el-radio-group v-model="form.playMode">
            <el-radio label="WS">桌面终端 (WS)</el-radio>
            <el-radio label="HTTP">HTTP 推送</el-radio>
            <el-radio label="BOTH">同时</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.playMode === 'HTTP' || form.playMode === 'BOTH'" label="推送地址" required>
          <el-input v-model="form.endpointUrl" placeholder="http://音箱IP/api/speak" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.state">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="绑定关系：请到「屏幕管理」编辑候诊大屏，在「绑定音响」中勾选本设备；播报文案与叫号范围跟随各块大屏自身配置。"
        />
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </form-drawer>
  </div>
</template>

<script>
import {
  listVoiceDevices,
  saveVoiceDevice,
  removeVoiceDevice
} from '@/api/VoiceDeviceManages'
import { getApiErrorMessage } from '@/utils/request'

export default {
  name: 'VoiceDeviceManages',
  data() {
    return {
      loading: false,
      saving: false,
      devices: [],
      visible: false,
      form: {
        id: '',
        deviceCode: '',
        name: '',
        location: '',
        playMode: 'WS',
        endpointUrl: '',
        state: 1
      }
    }
  },
  mounted() {
    this.loadList()
  },
  methods: {
    loadList() {
      this.loading = true
      listVoiceDevices()
        .then(res => {
          this.devices = (res && res.data) || []
        })
        .finally(() => { this.loading = false })
    },
    openEdit(row) {
      if (row) {
        this.form = {
          id: row.id,
          deviceCode: row.deviceCode,
          name: row.name,
          location: row.location,
          playMode: row.playMode || 'WS',
          endpointUrl: row.endpointUrl || '',
          state: row.state == null ? 1 : row.state
        }
      } else {
        this.form = {
          id: '',
          deviceCode: '',
          name: '',
          location: '',
          playMode: 'WS',
          endpointUrl: '',
          state: 1
        }
      }
      this.visible = true
    },
    save() {
      if (!this.form.deviceCode) {
        this.$message.warning('请填写设备编号')
        return
      }
      this.saving = true
      saveVoiceDevice(this.form)
        .then(() => {
          this.$message.success('保存成功')
          this.visible = false
          this.loadList()
        })
        .catch(err => {
          this.$message.error(getApiErrorMessage(err) || '保存失败')
        })
        .finally(() => { this.saving = false })
    },
    removeRow(row) {
      this.$confirm(`确认删除音响 [${row.deviceCode}]？`, '提示', { type: 'warning' })
        .then(() => removeVoiceDevice(row.id))
        .then(() => {
          this.$message.success('已删除')
          this.loadList()
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.toolbar-hint {
  margin-left: 12px;
  color: #909399;
  font-size: 13px;
}
</style>
