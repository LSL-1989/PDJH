<template>
  <div class="app-container">
    <el-card>
      <div class="list-toolbar shift-type-toolbar">
        <div class="list-toolbar__left">
          <el-input
            v-model="keyword"
            class="shift-type-toolbar__search"
            placeholder="班次名称或编码"
            clearable
          />
        </div>
        <div class="list-toolbar__right">
          <el-button type="primary" icon="el-icon-plus" @click="openAdd">新增班次</el-button>
        </div>
      </div>
      <el-table v-loading="loading" :data="filteredRows" border row-key="id">
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="code" label="编码" width="110" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="时段" min-width="140">
          <template #default="scope">{{ timeText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.exclusiveFlag === 1 ? 'danger' : 'info'" size="small">
              {{ scope.row.exclusiveFlag === 1 ? '停诊' : '出诊' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.enabled === 0 ? 'info' : 'success'" size="small">
              {{ scope.row.enabled === 0 ? '停用' : '启用' }}
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

    <form-drawer
      :title="dialogTitle"
      v-model="dialogVisible"
      size="460px"
      @close="resetForm"
    >
      <el-form ref="shiftForm" :model="form" :rules="formRules" label-width="96px" size="small">
        <el-form-item label="班次编码" prop="code">
          <el-input
            v-model="form.code"
            placeholder="如 AM"
            maxlength="16"
            :disabled="form.id != null"
            @input="onCodeInput"
          />
          <div class="shift-type-hint">创建后不可修改，排班记录按编码保存</div>
        </el-form-item>
        <el-form-item label="班次名称" prop="name">
          <el-input v-model="form.name" placeholder="如：上午班" maxlength="50" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-input v-model="form.startTime" placeholder="HH:mm，停诊可不填" maxlength="5" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-input v-model="form.endTime" placeholder="HH:mm，停诊可不填" maxlength="5" />
        </el-form-item>
        <el-form-item label="展示样式" prop="tagStyle">
          <el-select v-model="form.tagStyle" style="width:100%;">
            <el-option label="上午色" value="am" />
            <el-option label="下午色" value="pm" />
            <el-option label="全天色" value="full" />
            <el-option label="停诊色" value="off" />
            <el-option label="默认色" value="default" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="班次类型" prop="exclusiveFlag">
          <el-radio-group v-model="form.exclusiveFlag">
            <el-radio :label="0" border>出诊</el-radio>
            <el-radio :label="1" border>停诊</el-radio>
          </el-radio-group>
          <div class="shift-type-hint">停诊与同日其他班次互斥。出诊班次需填写起止时间，医生同一天可选多个出诊班次</div>
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-radio-group v-model="form.enabled">
            <el-radio :label="1" border>启用</el-radio>
            <el-radio :label="0" border>停用</el-radio>
          </el-radio-group>
          <div class="shift-type-hint">停用后不再出现在新增排班的下拉中，已有排班仍按原编码显示</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
      </template>
    </form-drawer>
  </div>
</template>

<script>
/**
 * 医生出诊班次字典维护
 * 消费者：系统与权限管理菜单；依赖 ShiftType/GetAll、Save、Delete
 */
import { getShiftTypes, saveShiftType, deleteShiftType } from '@/api/shiftType'
import { getApiErrorMessage } from '@/utils/request'

const emptyForm = () => ({
  id: null,
  code: '',
  name: '',
  startTime: '',
  endTime: '',
  tagStyle: 'default',
  sortOrder: 1,
  enabled: 1,
  exclusiveFlag: 0
})

export default {
  name: 'ShiftType',
  data() {
    return {
      loading: false,
      saving: false,
      keyword: '',
      rows: [],
      dialogVisible: false,
      dialogTitle: '',
      form: emptyForm(),
      formRules: {
        code: [{ required: true, message: '班次编码不能为空', trigger: 'blur' }],
        name: [{ required: true, message: '班次名称不能为空', trigger: 'blur' }],
        tagStyle: [{ required: true, message: '请选择展示样式', trigger: 'change' }],
        sortOrder: [{ required: true, message: '排序不能为空', trigger: 'change' }],
        enabled: [{ required: true, message: '请选择状态', trigger: 'change' }],
        exclusiveFlag: [{ required: true, message: '请选择班次类型', trigger: 'change' }]
      }
    }
  },
  computed: {
    filteredRows() {
      const key = (this.keyword || '').trim().toUpperCase()
      if (!key) return this.rows
      return this.rows.filter(row => {
        const name = row.name || ''
        const code = (row.code || '').toUpperCase()
        return name.includes(this.keyword.trim()) || code.includes(key)
      })
    }
  },
  mounted() {
    this.loadList()
  },
  methods: {
    timeText(row) {
      if (row && row.startTime && row.endTime) return `${row.startTime}-${row.endTime}`
      return '—'
    },
    loadList() {
      this.loading = true
      getShiftTypes()
        .then(res => {
          if (res && res.success === false) {
            this.$message.error(res.msg || '加载失败')
            this.rows = []
            return
          }
          this.rows = (res && res.data) || []
        })
        .catch(err => {
          this.rows = []
          this.$message.error(getApiErrorMessage(err, '加载班次失败'))
        })
        .finally(() => {
          this.loading = false
        })
    },
    onCodeInput(value) {
      this.form.code = (value || '').toUpperCase().replace(/[^A-Z0-9_]/g, '').slice(0, 16)
    },
    openAdd() {
      const maxOrder = this.rows.reduce((max, row) => Math.max(max, Number(row.sortOrder) || 0), 0)
      this.form = { ...emptyForm(), sortOrder: maxOrder + 1 }
      this.dialogTitle = '新增班次'
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.shiftForm) this.$refs.shiftForm.clearValidate()
      })
    },
    openEdit(row) {
      this.form = {
        id: row.id,
        code: row.code || '',
        name: row.name || '',
        startTime: row.startTime || '',
        endTime: row.endTime || '',
        tagStyle: row.tagStyle || 'default',
        sortOrder: row.sortOrder == null ? 0 : row.sortOrder,
        enabled: row.enabled === 0 ? 0 : 1,
        exclusiveFlag: row.exclusiveFlag === 1 ? 1 : 0
      }
      this.dialogTitle = '修改班次'
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.shiftForm) this.$refs.shiftForm.clearValidate()
      })
    },
    submitForm() {
      this.$refs.shiftForm.validate(valid => {
        if (!valid) return
        this.saving = true
        saveShiftType({
          id: this.form.id,
          code: (this.form.code || '').trim(),
          name: (this.form.name || '').trim(),
          startTime: (this.form.startTime || '').trim(),
          endTime: (this.form.endTime || '').trim(),
          tagStyle: this.form.tagStyle,
          sortOrder: this.form.sortOrder,
          enabled: this.form.enabled,
          exclusiveFlag: this.form.exclusiveFlag
        })
          .then(res => {
            if (res && res.success === false) {
              this.$message.error(res.msg || '保存失败')
              return
            }
            this.$message.success('保存成功')
            this.dialogVisible = false
            this.loadList()
          })
          .catch(err => {
            this.$message.error(getApiErrorMessage(err, '保存失败'))
          })
          .finally(() => {
            this.saving = false
          })
      })
    },
    removeRow(row) {
      this.$confirm(`确定删除班次「${row.name}」？`, '提示', { type: 'warning' })
        .then(() => deleteShiftType(row.id))
        .then(res => {
          if (res && res.success === false) {
            this.$message.error(res.msg || '删除失败')
            return
          }
          this.$message.success('删除成功')
          this.loadList()
        })
        .catch(err => {
          if (err === 'cancel') return
          this.$message.error(getApiErrorMessage(err, '删除失败'))
        })
    },
    resetForm() {
      this.form = emptyForm()
    }
  }
}
</script>

<style scoped>
.shift-type-toolbar {
  margin-bottom: 16px;
}
.shift-type-toolbar__search {
  width: 220px;
}
.shift-type-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>
