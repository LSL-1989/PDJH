<template>
  <div class="app-container">
    <el-card>
      <div class="list-toolbar doctor-title-toolbar">
        <div class="list-toolbar__left">
          <el-input
            v-model="keyword"
            class="doctor-title-toolbar__search"
            placeholder="职称名称"
            clearable
          />
        </div>
        <div class="list-toolbar__right">
          <el-button type="primary" icon="el-icon-plus" @click="openAdd">新增职称</el-button>
        </div>
      </div>
      <el-table v-loading="loading" :data="filteredRows" border row-key="id">
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column prop="name" label="职称名称" min-width="180" />
        <el-table-column label="状态" width="100">
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
      size="420px"
      @close="resetForm"
    >
      <el-form ref="titleForm" :model="form" :rules="formRules" label-width="88px" size="small">
        <el-form-item label="职称名称" prop="name">
          <el-input v-model="form.name" placeholder="如：主任医师" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-radio-group v-model="form.enabled">
            <el-radio :label="1" border>启用</el-radio>
            <el-radio :label="0" border>停用</el-radio>
          </el-radio-group>
          <div class="doctor-title-hint">停用后不再出现在医生档案的职称下拉中；改名会同步已使用该职称的医生</div>
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
 * 医生职称字典维护
 * 消费者：系统与权限管理菜单；依赖 DoctorTitle/GetAll、Save、Delete
 */
import { getDoctorTitles, saveDoctorTitle, deleteDoctorTitle } from '@/api/doctorTitle'
import { getApiErrorMessage } from '@/utils/request'

const emptyForm = () => ({
  id: null,
  name: '',
  sortOrder: 1,
  enabled: 1
})

export default {
  name: 'DoctorTitle',
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
        name: [{ required: true, message: '职称名称不能为空', trigger: 'blur' }],
        sortOrder: [{ required: true, message: '排序不能为空', trigger: 'change' }],
        enabled: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  computed: {
    filteredRows() {
      const key = (this.keyword || '').trim()
      if (!key) return this.rows
      return this.rows.filter(row => (row.name || '').includes(key))
    }
  },
  mounted() {
    this.loadList()
  },
  methods: {
    loadList() {
      this.loading = true
      getDoctorTitles()
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
          this.$message.error(getApiErrorMessage(err, '加载职称失败'))
        })
        .finally(() => {
          this.loading = false
        })
    },
    openAdd() {
      const maxOrder = this.rows.reduce((max, row) => Math.max(max, Number(row.sortOrder) || 0), 0)
      this.form = { ...emptyForm(), sortOrder: maxOrder + 1 }
      this.dialogTitle = '新增职称'
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.titleForm) this.$refs.titleForm.clearValidate()
      })
    },
    openEdit(row) {
      this.form = {
        id: row.id,
        name: row.name || '',
        sortOrder: row.sortOrder == null ? 0 : row.sortOrder,
        enabled: row.enabled === 0 ? 0 : 1
      }
      this.dialogTitle = '修改职称'
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.titleForm) this.$refs.titleForm.clearValidate()
      })
    },
    submitForm() {
      this.$refs.titleForm.validate(valid => {
        if (!valid) return
        this.saving = true
        saveDoctorTitle({
          id: this.form.id,
          name: (this.form.name || '').trim(),
          sortOrder: this.form.sortOrder,
          enabled: this.form.enabled
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
      this.$confirm(`确定删除职称「${row.name}」？`, '提示', { type: 'warning' })
        .then(() => deleteDoctorTitle(row.id))
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
.doctor-title-toolbar {
  margin-bottom: 16px;
}
.doctor-title-toolbar__search {
  width: 220px;
}
.doctor-title-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>
