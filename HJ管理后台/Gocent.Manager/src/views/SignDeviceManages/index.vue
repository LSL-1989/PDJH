<template>
  <div class="app-container">
    <el-card>
      <div class="list-toolbar list-toolbar--flat">
        <div class="list-toolbar__left">
          <el-button icon="el-icon-refresh" @click="loadList">刷新</el-button>
        </div>
        <div class="list-toolbar__right">
          <el-button type="primary" icon="el-icon-plus" @click="openEdit()">新增签到终端</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="devices" border stripe>
        <el-table-column prop="deviceCode" label="设备编号" width="160" />
        <el-table-column prop="name" label="名称" width="160" />
        <el-table-column prop="title" label="标题" min-width="140" />
        <el-table-column prop="location" label="位置" width="140" />
        <el-table-column prop="boundRoomSummary" label="绑定诊室" min-width="220" show-overflow-tooltip />
        <el-table-column label="配置状态" width="100">
          <template #default="scope">
            <el-tag
              :type="getSignStatusTagType(scope.row.status)"
              size="small"
            >
              {{ getSignStatusLabel(scope.row.status, scope.row.statusModel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="在线状态" width="100">
          <template #default="scope">
            <el-tag
              :type="getOnlineTagType(scope.row.online, scope.row.deviceCode)"
              size="small"
              effect="plain"
            >
              {{ getOnlineStatusLabel(scope.row.online, scope.row.deviceCode) }}
            </el-tag>
          </template>
        </el-table-column>
        <table-action-column :count="3">
          <template #default="{ row }">
            <table-action-icon icon="el-icon-edit" tip="编辑" @click="openEdit(row)" />
            <table-action-icon icon="el-icon-link" tip="绑定诊室" @click="openRoomDialog(row)" />
            <table-action-icon icon="el-icon-delete" tip="删除" type="danger" @click="removeDevice(row)" />
          </template>
        </table-action-column>
      </el-table>
    </el-card>

    <form-drawer :title="editForm.id ? '编辑签到终端' : '新增签到终端'" v-model="editVisible">
      <el-form ref="editFormRef" :model="editForm" label-width="100px">
        <el-form-item label="设备编号" required>
          <el-input
            v-model="editForm.deviceCode"
            placeholder="如 SIGN-NK-01"
            :disabled="!!editForm.id"
            @input="onDeviceCodeInput"
          />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="editForm.location" placeholder="如 3F 内科候诊区" />
        </el-form-item>
        <el-form-item label="提示语">
          <el-input v-model="editForm.showText" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item v-if="!editForm.id" label="初始状态">
          <el-tag type="warning" effect="plain">待配置（保存后自动设置，绑定诊室后自动启用）</el-tag>
        </el-form-item>
        <el-form-item v-else label="设备状态">
          <el-tag :type="getSignStatusTagType(editForm.status)" effect="dark" size="small" style="margin-right:8px;">
            配置：{{ getSignStatusLabel(editForm.status, editForm.statusModel) }}
          </el-tag>
          <el-tag :type="getOnlineTagType(editForm.online, editForm.deviceCode)" effect="plain" size="small">
            连接：{{ getOnlineStatusLabel(editForm.online, editForm.deviceCode) }}
          </el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button
          v-if="editForm.id && !isSignDisabled(editForm.status)"
          type="warning"
          plain
          @click="applyLifecycleAction('DISABLE')"
        >停用设备</el-button>
        <el-button
          v-if="editForm.id && isSignDisabled(editForm.status)"
          type="success"
          plain
          @click="applyLifecycleAction('ENABLE')"
        >解除停用</el-button>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDevice()">保存</el-button>
      </template>
    </form-drawer>

    <el-dialog :title="'绑定诊室 - ' + (roomDialogRow.name || roomDialogRow.deviceCode)" v-model="roomVisible" width="70%">
      <div style="text-align:center">
        <el-transfer
          v-model="selectedRoomIds"
          style="text-align:left;display:inline-block"
          :titles="['可选物理诊室', '当前终端绑定诊室']"
          filterable
          :data="roomOptions"
        />
      </div>
      <template #footer><div>
        <el-button @click="roomVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRoomMapping">确定</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import {
  getOnlineStatusLabel,
  getOnlineTagType,
  getSignStatusLabel,
  getSignStatusTagType,
  isSignDisabled,
  normalizeSignDeviceRow,
  sanitizeDeviceCodeInput
} from '@/utils/signState'

export default {
  name: 'SignDeviceManages',
  data() {
    return {
      loading: false,
      devices: [],
      editVisible: false,
      editForm: {},
      roomVisible: false,
      roomDialogRow: {},
      roomOptions: [],
      selectedRoomIds: []
    }
  },
  mounted() {
    this.loadList()
    this.loadAllRooms()
  },
  methods: {
    getSignStatusLabel,
    getSignStatusTagType,
    getOnlineStatusLabel,
    getOnlineTagType,
    isSignDisabled,
    loadList() {
      this.loading = true
      this.$store.dispatch('SignDeviceManages/listSignDevices', {})
        .then(res => {
          const rows = (res && res.data) || []
          this.devices = rows.map(normalizeSignDeviceRow)
        })
        .finally(() => { this.loading = false })
    },
    loadAllRooms() {
      this.$store.dispatch('SignDeviceManages/getAllOfficeRooms')
        .then(res => {
          const rooms = (res && res.data) || []
          this.roomOptions = rooms.map(r => ({
            key: r.id,
            label: [r.floor, r.name, r.roomNo].filter(Boolean).join(' ')
          }))
        })
    },
    onDeviceCodeInput(value) {
      this.editForm.deviceCode = sanitizeDeviceCodeInput(value)
    },
    openEdit(row) {
      if (row) {
        this.editForm = normalizeSignDeviceRow({ ...row })
      } else {
        this.editForm = {
          deviceCode: '',
          name: '',
          title: '',
          location: '',
          showText: '',
          status: 2
        }
      }
      this.editVisible = true
    },
    normalizeLifecycleAction(action) {
      if (typeof action !== 'string') return ''
      const normalized = action.trim().toUpperCase()
      return normalized === 'DISABLE' || normalized === 'ENABLE' ? normalized : ''
    },
    mapEditFormForApi(lifecycleAction) {
      const mapped = {
        id: this.editForm.id || '',
        deviceCode: this.editForm.deviceCode,
        name: this.editForm.name,
        title: this.editForm.title,
        location: this.editForm.location,
        showText: this.editForm.showText
      }
      const action = this.normalizeLifecycleAction(lifecycleAction)
      if (action) {
        mapped.lifecycleAction = action
      }
      return mapped
    },
    saveDevice(lifecycleAction) {
      if (!this.editForm.deviceCode) {
        this.$message.warning('请填写设备编号')
        return
      }
      const action = this.normalizeLifecycleAction(lifecycleAction)
      this.$store.dispatch('SignDeviceManages/saveSignDevice', this.mapEditFormForApi(action))
        .then((res) => {
          if (action === 'DISABLE') {
            this.$message.success('设备已停用')
          } else if (action === 'ENABLE') {
            this.$message.success('已解除停用，配置状态已自动更新')
          } else {
            this.$message.success('保存成功')
          }
          if (res && res.data && this.editForm.id) {
            this.editForm = normalizeSignDeviceRow({ ...this.editForm, ...res.data })
          }
          if (!action || action === 'ENABLE') {
            this.editVisible = false
          }
          this.loadList()
        })
        .catch((error) => {
          this.$message.error(error && error.message ? error.message : '保存失败')
        })
    },
    applyLifecycleAction(action) {
      this.saveDevice(action)
    },
    removeDevice(row) {
      this.$confirm('确认删除该签到终端？', '提示', { type: 'warning' })
        .then(() => this.$store.dispatch('SignDeviceManages/removeSignDevice', row.id))
        .then(() => {
          this.$message.success('删除成功')
          this.loadList()
        })
        .catch(() => {})
    },
    openRoomDialog(row) {
      this.roomDialogRow = row
      this.selectedRoomIds = (row.boundRooms || []).map(r => r.id)
      this.roomVisible = true
    },
    saveRoomMapping() {
      this.$store.dispatch('SignDeviceManages/relevancySignDeviceAndRooms', {
        signDeviceId: this.roomDialogRow.id,
        officeRoomIds: this.selectedRoomIds
      }).then(() => {
        this.$message.success('绑定成功')
        this.roomVisible = false
        this.loadList()
      })
    }
  }
}
</script>
