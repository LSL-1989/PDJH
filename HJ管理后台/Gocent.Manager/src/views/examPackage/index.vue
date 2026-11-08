<template>
  <div class="exam-pkg">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon" style="background:linear-gradient(135deg,#9C27B0,#6A1B9A);">
          <i class="el-icon-first-aid-kit" />
        </div>
        <div>
          <h2>体检套餐管理</h2>
          <span class="header-sub">管理体检套餐与检查项目字典，配置后自动影响 TSP 路由引擎的路径规划</span>
        </div>
      </div>
      <div class="header-actions">
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openPackageDialog(null)">新建套餐</el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左：套餐列表 -->
      <el-col :span="10">
        <div class="panel">
          <div class="panel-title">✅ 已有套餐（{{ packages.length }}个）</div>
          <div v-loading="loading" class="pkg-list">
            <div
              v-for="pkg in packages"
              :key="pkg.id"
              class="pkg-item"
              :class="{ active: selectedPkg && selectedPkg.id === pkg.id }"
              @click="selectPackage(pkg)"
            >
              <div class="pkg-icon" :style="{ background: pkg.color || '#9C27B0' }">
                <i class="el-icon-first-aid-kit" />
              </div>
              <div class="pkg-info">
                <div class="pkg-name">{{ pkg.name }}</div>
                <div class="pkg-meta">{{ pkg.items ? pkg.items.length : 0 }} 项 · {{ pkg.targetGroup }} · ¥{{ pkg.price }}</div>
              </div>
              <div class="pkg-actions">
                <el-button text size="small" icon="el-icon-edit" @click.stop="openPackageDialog(pkg)" />
                <el-button text size="small" icon="el-icon-delete" style="color:#EA4335;" @click.stop="deletePackage(pkg)" />
              </div>
              <el-tag :type="pkg.enabled ? 'success' : 'info'" size="small" effect="dark">{{ pkg.enabled ? '启用' : '停用' }}</el-tag>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右：套餐详情/检查项目 -->
      <el-col :span="14">
        <div v-if="selectedPkg" class="panel">
          <div class="panel-title">🔬 「{{ selectedPkg.name }}」包含的检查项目</div>
          <div style="display:flex;justify-content:flex-end;margin-bottom:8px;">
            <el-button size="small" type="primary" plain icon="el-icon-plus" @click="openItemDialog(null)">添加检查项</el-button>
          </div>
          <el-table :data="selectedPkg.items || []" size="small" border>
            <el-table-column prop="itemName" label="检查项目" min-width="120" />
            <el-table-column prop="associatedDeptId" label="检查科室" width="100">
              <template #default="{row}">
                <el-tag type="info" size="small">{{ row.deptName || row.associatedDeptId }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="avgTimeMinutes" label="平均耗时" width="90" align="center">
              <template #default="{row}">{{ row.avgTimeMinutes }}分钟</template>
            </el-table-column>
            <el-table-column label="空腹" width="70" align="center">
              <template #default="{row}">
                <el-tag :type="row.isFasting ? 'warning' : 'info'" size="small">{{ row.isFasting ? '需要' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="性别限制" width="85" align="center">
              <template #default="{row}">
                <span v-if="row.genderLimit === 0">不限</span>
                <el-tag v-else-if="row.genderLimit === 1" type="primary" size="small">男性</el-tag>
                <el-tag v-else-if="row.genderLimit === 2" size="small" style="background:#FF69B4;border-color:#FF69B4;color:#fff;">女性</el-tag>
              </template>
            </el-table-column>
            <table-action-column :count="1">
              <template #default="{ $index }">
                <table-action-icon icon="el-icon-delete" tip="移除" type="danger" @click="removeItem($index)" />
              </template>
            </table-action-column>
          </el-table>

          <!-- 路由预估 -->
          <div v-if="selectedPkg.items && selectedPkg.items.length > 0" class="route-estimate">
            <div class="est-title"><i class="el-icon-aim" /> TSP 路由预估</div>
            <el-row :gutter="16">
              <el-col :span="8"><div class="est-card"><div class="est-num">{{ totalTime }}</div><div class="est-label">预计总耗时</div></div></el-col>
              <el-col :span="8"><div class="est-card"><div class="est-num" style="color:#FBBC04;">{{ fastingCount }}</div><div class="est-label">空腹项目数</div></div></el-col>
              <el-col :span="8"><div class="est-card"><div class="est-num" style="color:#9C27B0;">{{ selectedPkg.items.length }}</div><div class="est-label">检查项总数</div></div></el-col>
            </el-row>
          </div>
        </div>
        <div v-else class="panel" style="display:flex;align-items:center;justify-content:center;height:300px;color:#80868B;">
          <div style="text-align:center;">
            <i class="el-icon-first-aid-kit" style="font-size:48px;color:#DADCE0;display:block;margin-bottom:8px;" />
            <p>← 请从左侧选择套餐查看详情</p>
          </div>
        </div>

        <!-- 检查项字典管理 -->
        <div class="panel" style="margin-top:20px;">
          <div class="panel-title">📚 检查项目字典库（{{ dictItems.length }}项）</div>
          <div class="list-toolbar list-toolbar--flat">
            <div class="list-toolbar__left">
              <el-input v-model="dictFilter" placeholder="搜索项目名称" size="small" prefix-icon="el-icon-search" clearable style="width:200px;" />
            </div>
            <div class="list-toolbar__right">
              <el-button size="small" type="primary" plain icon="el-icon-plus" @click="openItemDialog(null)">新增项目</el-button>
            </div>
          </div>
          <el-table :data="filteredDictItems" size="small" border height="220">
            <el-table-column prop="id" label="ID" width="55" align="center" />
            <el-table-column prop="itemName" label="项目名称" min-width="120" />
            <el-table-column prop="avgTimeMinutes" label="耗时(分)" width="80" align="center" />
            <el-table-column label="空腹" width="65" align="center">
              <template #default="{row}"><el-tag :type="row.isFasting ? 'warning' : 'info'" size="small">{{ row.isFasting ? '是' : '否' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="前置项目" min-width="120">
              <template #default="{row}">
                <template v-if="getDependNames(row.dependsOnIds).length > 0">
                  <el-tag
                    v-for="name in getDependNames(row.dependsOnIds)"
                    :key="name"
                    size="small"
                    type="danger"
                    style="margin-right:4px;margin-bottom:2px;"
                  >→ {{ name }}</el-tag>
                </template>
                <span v-else style="color:#DADCE0;font-size:12px;">无前置依赖</span>
              </template>
            </el-table-column>
            <table-action-column :count="2">
              <template #default="{ row }">
                <table-action-icon icon="el-icon-edit" tip="编辑" @click="openItemDialog(row)" />
                <table-action-icon icon="el-icon-delete" tip="删除" type="danger" @click="deleteDictItem(row)" />
              </template>
            </table-action-column>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <form-drawer :title="pkgDialogMode === 'add' ? '新建体检套餐' : '编辑套餐'" v-model="pkgDialogVisible" size="560px">
      <div class="pkg-dialog-body">
        <div class="pkg-form-col">
          <div class="dialog-section-title"><i class="el-icon-document" /> 基本信息</div>
          <el-form :model="pkgForm" label-width="90px" size="small">
            <el-form-item label="套餐名称" required><el-input v-model="pkgForm.name" placeholder="如：标准成人体检套餐A" /></el-form-item>
            <el-form-item label="适用人群">
              <el-select v-model="pkgForm.targetGroup" style="width:100%;">
                <el-option label="成人通用" value="adult" />
                <el-option label="老年人（≥60岁）" value="elder" />
                <el-option label="职工体检" value="staff" />
                <el-option label="婚前检查" value="premarital" />
                <el-option label="儿童体检" value="child" />
              </el-select>
            </el-form-item>
            <el-form-item label="定价（元）"><el-input-number v-model="pkgForm.price" :min="0" :step="50" /></el-form-item>
            <el-form-item label="主题色"><el-color-picker v-model="pkgForm.color" :predefine="['#9C27B0','#1A73E8','#34A853','#EA4335','#FF6D00']" /></el-form-item>
            <el-form-item label="是否启用"><el-switch v-model="pkgForm.enabled" active-text="启用" inactive-text="停用" /></el-form-item>
            <el-form-item label="备注说明"><el-input v-model="pkgForm.remark" type="textarea" :rows="2" /></el-form-item>
          </el-form>

          <div v-if="pkgSelectedItems.length > 0" class="pkg-summary">
            <div class="dialog-section-title" style="margin-bottom:8px;"><i class="el-icon-data-line" /> 套餐概览</div>
            <div class="sum-row"><span>检查项目</span><b>{{ pkgSelectedItems.length }}项</b></div>
            <div class="sum-row"><span>预计总耗时</span><b style="color:#34A853;">{{ pkgTotalTime }}</b></div>
            <div class="sum-row"><span>空腹项目</span><b style="color:#FBBC04;">{{ pkgFastingCount }}项</b></div>
          </div>
        </div>

        <div class="pkg-picker-col">
          <div class="dialog-section-title"><i class="el-icon-s-grid" /> 检查项目配置</div>
          <div class="dual-picker">
            <div class="picker-panel">
              <div class="picker-header">项目库（{{ filteredPkgDict.length }}）
                <el-input v-model="pkgDictSearch" size="small" placeholder="搜索" prefix-icon="el-icon-search" style="width:100px;margin-left:8px;" />
              </div>
              <div class="picker-list">
                <div
                  v-for="item in filteredPkgDict"
                  :key="item.id"
                  class="picker-item"
                  :class="{ 'is-selected': isPkgItemSelected(item.id) }"
                  @click="addToPkg(item)"
                >
                  <div class="pi-name">{{ item.itemName }}</div>
                  <div class="pi-meta">
                    <el-tag v-if="item.isFasting" size="small" type="warning">空腹</el-tag>
                    <span>{{ item.avgTimeMinutes }}min</span>
                  </div>
                  <i v-if="!isPkgItemSelected(item.id)" class="el-icon-right pi-add" />
                  <i v-else class="el-icon-check pi-check" style="color:#34A853;" />
                </div>
              </div>
            </div>

            <div class="picker-arrows">
              <i class="el-icon-d-arrow-right" style="font-size:24px;color:#DADCE0;" />
            </div>

            <div class="picker-panel">
              <div class="picker-header">已选项目（{{ pkgSelectedItems.length }}）</div>
              <div v-if="pkgSelectedItems.length > 0" class="picker-list">
                <div
                  v-for="(item, idx) in pkgSelectedItems"
                  :key="item.id"
                  class="picker-item selected-item"
                >
                  <div class="pi-order">{{ idx + 1 }}</div>
                  <div class="pi-name">{{ item.itemName }}</div>
                  <div class="pi-meta"><span>{{ item.avgTimeMinutes }}min</span></div>
                  <div class="pi-ops">
                    <i v-if="idx > 0" class="el-icon-top" @click="movePkgItem(idx, -1)" />
                    <i v-if="idx < pkgSelectedItems.length - 1" class="el-icon-bottom" @click="movePkgItem(idx, 1)" />
                    <i class="el-icon-delete" style="color:#EA4335;" @click="removeFromPkg(idx)" />
                  </div>
                </div>
              </div>
              <div v-else class="picker-empty">点击左列项目添加 →</div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="pkgDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pkgSaving" @click="savePackage">保存套餐</el-button>
      </template>
    </form-drawer>

    <form-drawer title="添加/编辑检查项目" v-model="itemDialogVisible">
      <el-form :model="itemForm" label-width="120px" size="small">
        <el-form-item label="项目名称" required><el-input v-model="itemForm.itemName" placeholder="如：腔部B超" /></el-form-item>
        <el-form-item label="检查科室ID"><el-input v-model="itemForm.associatedDeptId" placeholder="科室ID" /></el-form-item>
        <el-form-item label="是否需要空腹"><el-switch v-model="itemForm.isFasting" :active-value="1" :inactive-value="0" active-text="需要空腹" inactive-text="不需要" /></el-form-item>
        <el-form-item label="性别限制">
          <el-radio-group v-model="itemForm.genderLimit">
            <el-radio :label="0">不限</el-radio><el-radio :label="1">男性专享</el-radio><el-radio :label="2">女性专享</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="平均耗时(分)"><el-input-number v-model="itemForm.avgTimeMinutes" :min="1" :max="180" /></el-form-item>
        <el-form-item label="必须先完成">
          <el-select
            v-model="itemForm.dependsOnIdArr"
            multiple
            filterable
            placeholder="选择前置项目（多选）"
            style="width:100%;"
          >
            <el-option
              v-for="d in dictItems.filter(d => d.id !== itemForm.id)"
              :key="d.id"
              :label="d.itemName"
              :value="d.id"
            />
          </el-select>
          <div v-if="itemForm.dependsOnIdArr && itemForm.dependsOnIdArr.length > 0" style="margin-top:8px;display:flex;flex-wrap:wrap;gap:6px;">
            <el-tag
              v-for="id in itemForm.dependsOnIdArr"
              :key="id"
              closable
              size="small"
              type="danger"
              @close="removeDepend(id)"
            >先完成: {{ getItemNameById(id) }}</el-tag>
          </div>
          <div style="font-size:11px;color:#80868B;margin-top:6px;">&#128161; 选中出现在列表中的项目，必须先完成后，才会自动安排本项目</div>
        </el-form-item>
        <el-form-item label="是否启用"><el-switch v-model="itemForm.isEnabled" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDictItem">保存</el-button>
      </template>
    </form-drawer>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'ExamPackage',
  data() {
    return {
      loading: false,
      selectedPkg: null,
      dictFilter: '',
      pkgDialogVisible: false,
      itemDialogVisible: false,
      pkgDialogMode: 'add',
      pkgSaving: false,
      pkgSelectedItems: [], // P1-5d: 双列选择器已选项目
      pkgDictSearch: '', // P1-5d: 左列搜索
      dictItems: [],
      packages: [],
      pkgForm: { name: '', targetGroup: 'adult', price: 299, color: '#9C27B0', enabled: true, remark: '' },
      itemForm: { id: null, itemName: '', associatedDeptId: '', isFasting: 0, genderLimit: 0, avgTimeMinutes: 15, isEnabled: 1, dependsOnIds: '', dependsOnIdArr: [] }
    }
  },
  computed: {
    filteredDictItems() {
      if (!this.dictFilter) return this.dictItems
      return this.dictItems.filter(d => d.itemName && d.itemName.includes(this.dictFilter))
    },
    // P1-5d: 双列选择器计算属性
    filteredPkgDict() {
      if (!this.pkgDictSearch) return this.dictItems
      return this.dictItems.filter(d => d.itemName && d.itemName.includes(this.pkgDictSearch))
    },
    pkgTotalTime() {
      const mins = this.pkgSelectedItems.reduce((s, i) => s + (i.avgTimeMinutes || 0), 0)
      return mins >= 60 ? `${Math.floor(mins / 60)}小时${mins % 60}分钟` : `${mins}分钟`
    },
    pkgFastingCount() { return this.pkgSelectedItems.filter(i => i.isFasting === 1).length },
    totalTime() {
      const mins = (this.selectedPkg?.items || []).reduce((s, i) => s + (i.avgTimeMinutes || 0), 0)
      return mins >= 60 ? `${Math.floor(mins / 60)}小时${mins % 60}分钟` : `${mins}分钟`
    },
    fastingCount() { return (this.selectedPkg?.items || []).filter(i => i.isFasting === 1).length }
  },
  created() { this.loadDictItems(); this.loadPackages() },
  methods: {
    loadDictItems() {
      request({ url: '/api/exam/dict/items', method: 'get' })
        .then(res => { this.dictItems = res.data || res || [] })
        .catch(() => { this.dictItems = []; this.$message.error('加载检查项目字典失败') })
    },
    async loadPackages() {
      this.loading = true
      try {
        const res = await request({ url: '/api/exam/packages', method: 'get' })
        const list = res.data || res || []
        const withItems = await Promise.all(list.map(async pkg => {
          const itemsRes = await request({ url: `/api/exam/package/${pkg.id}/items`, method: 'get' })
          return { ...pkg, items: itemsRes.data || itemsRes || [] }
        }))
        this.packages = withItems
        if (this.selectedPkg) {
          this.selectedPkg = this.packages.find(p => p.id === this.selectedPkg.id) || null
        }
      } catch (e) {
        this.packages = []
      } finally {
        this.loading = false
      }
    },
    selectPackage(pkg) { this.selectedPkg = pkg },
    openPackageDialog(pkg) {
      if (pkg) {
        this.pkgForm = { ...pkg }
        this.pkgDialogMode = 'edit'
        // P1-5d: 初始化已选项目列表
        this.pkgSelectedItems = JSON.parse(JSON.stringify(pkg.items || []))
      } else {
        this.pkgForm = { name: '', targetGroup: 'adult', price: 299, color: '#9C27B0', enabled: true, remark: '' }
        this.pkgDialogMode = 'add'
        this.pkgSelectedItems = []
      }
      this.pkgDictSearch = ''
      this.pkgDialogVisible = true
    },
    // P1-5d: 双列选择器操作方法
    isPkgItemSelected(itemId) {
      return this.pkgSelectedItems.some(i => i.id === itemId)
    },
    addToPkg(item) {
      if (!this.isPkgItemSelected(item.id)) {
        this.pkgSelectedItems.push({ ...item })
      } else {
        this.$message.info('该项目已在套餐中')
      }
    },
    removeFromPkg(idx) { this.pkgSelectedItems.splice(idx, 1) },
    movePkgItem(idx, dir) {
      const target = idx + dir
      if (target < 0 || target >= this.pkgSelectedItems.length) return
      const tmp = this.pkgSelectedItems[idx]
      this.pkgSelectedItems[idx] = this.pkgSelectedItems[target]
      this.pkgSelectedItems[target] = tmp
    },
    async savePackage() {
      if (!this.pkgForm.name) { this.$message.warning('套餐名称不能为空'); return }
      this.pkgSaving = true
      try {
        if (this.pkgDialogMode === 'add') {
          const saveRes = await request({ url: '/api/exam/package/save', method: 'post', data: this.pkgForm })
          const newPkg = saveRes.data || saveRes
          await request({ url: `/api/exam/package/${newPkg.id}/items/save`, method: 'post',
            data: this.pkgSelectedItems.map((it, i) => ({ itemId: it.id, sortOrder: i, remark: '' })) })
          this.$message.success('套餐创建成功！')
        } else {
          await request({ url: '/api/exam/package/save', method: 'post', data: this.pkgForm })
          await request({ url: `/api/exam/package/${this.pkgForm.id}/items/save`, method: 'post',
            data: this.pkgSelectedItems.map((it, i) => ({ itemId: it.id, sortOrder: i, remark: '' })) })
          this.$message.success('套餐更新成功！')
        }
        this.pkgDialogVisible = false
        await this.loadPackages()
      } finally {
        this.pkgSaving = false
      }
    },
    deletePackage(pkg) {
      this.$confirm(`确认删除套餐「${pkg.name}」？`, '确认删除', { type: 'warning' }).then(async() => {
        await request({ url: `/api/exam/package/${pkg.id}`, method: 'delete' })
        if (this.selectedPkg?.id === pkg.id) this.selectedPkg = null
        this.$message.success('已删除')
        this.loadPackages()
      }).catch(() => {})
    },
    removeItem(idx) { this.selectedPkg.items.splice(idx, 1) },
    openItemDialog(item) {
      if (item) {
        // 将 dependsOnIds 字符串转为数字数组以供多选组件使用
        const arr = item.dependsOnIds
          ? item.dependsOnIds.split(',').map(s => s.trim()).filter(Boolean).map(Number)
          : []
        this.itemForm = { ...item, dependsOnIdArr: arr }
      } else {
        this.itemForm = { id: null, itemName: '', associatedDeptId: '', isFasting: 0, genderLimit: 0, avgTimeMinutes: 15, isEnabled: 1, dependsOnIds: '', dependsOnIdArr: [] }
      }
      this.itemDialogVisible = true
    },
    async saveDictItem() {
      if (!this.itemForm.itemName) { this.$message.warning('项目名称不能为空'); return }
      this.itemForm.dependsOnIds = (this.itemForm.dependsOnIdArr || []).join(',')
      try {
        await request({ url: '/api/exam/dict/item/save', method: 'post', data: this.itemForm })
        this.$message.success('检查项目已保存')
        this.itemDialogVisible = false
        this.loadDictItems()
      } catch (e) {
        this.$message.error('保存失败')
      }
    },
    removeDepend(id) {
      this.itemForm.dependsOnIdArr = (this.itemForm.dependsOnIdArr || []).filter(i => i !== id)
    },
    getItemNameById(id) {
      const item = this.dictItems.find(d => d.id === id || d.id === String(id) || String(d.id) === String(id))
      return item ? item.itemName : `ID:${id}`
    },
    getDependNames(dependsOnIds) {
      if (!dependsOnIds) return []
      return dependsOnIds.split(',').map(s => s.trim()).filter(Boolean).map(id => {
        const item = this.dictItems.find(d => String(d.id) === String(id))
        return item ? item.itemName : null
      }).filter(Boolean)
    },
    deleteDictItem(item) {
      this.$confirm(`确认删除「${item.itemName}」？`, '确认', { type: 'warning' }).then(() => {
        this.dictItems = this.dictItems.filter(d => d.id !== item.id)
        this.$message.success('已删除')
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.exam-pkg { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
  .header-left { display: flex; align-items: center; gap: 16px; }
  .header-icon { width: 52px; height: 52px; border-radius: 14px; display: flex; justify-content: center; align-items: center; i { font-size: 24px; color: white; } }
  h2 { margin: 0; font-size: 22px; font-weight: 700; color: #202124; }
  .header-sub { font-size: 13px; color: #80868B; margin-top: 3px; display: block; }
}
.panel { background: #FFF; border-radius: 12px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.panel-title { font-size: 14px; font-weight: 600; color: #202124; margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px solid #F1F3F4; }
.pkg-list { display: flex; flex-direction: column; gap: 8px; max-height: 400px; overflow-y: auto; }
.pkg-item {
  display: flex; align-items: center; gap: 10px; padding: 10px 12px;
  border-radius: 10px; cursor: pointer; border: 1.5px solid transparent; transition: all 0.15s;
  &:hover { background: #F8F9FA; }
  &.active { background: #F3E5F5; border-color: #9C27B0; }
}
.pkg-icon { width: 36px; height: 36px; border-radius: 10px; display: flex; justify-content: center; align-items: center; flex-shrink: 0; i { font-size: 18px; color: white; } }
.pkg-info { flex: 1; min-width: 0; }
.pkg-name { font-size: 13px; font-weight: 600; color: #202124; }
.pkg-meta { font-size: 12px; color: #80868B; }
.pkg-actions { display: flex; gap: 4px; }
.route-estimate { margin-top: 16px; background: #F3E5F5; border-radius: 10px; padding: 14px; }
.est-title { font-size: 13px; font-weight: 600; color: #7B1FA2; margin-bottom: 12px; i { margin-right: 6px; } }
.est-card { background: #FFF; border-radius: 8px; padding: 12px; text-align: center; }
.est-num { font-size: 20px; font-weight: 700; color: #34A853; }
.est-label { font-size: 12px; color: #80868B; margin-top: 4px; }

/* P1-5d: 套餐选择器（抽屉内上下排布） */
.pkg-dialog-body { display: flex; flex-direction: column; gap: 20px; }
.pkg-form-col { width: 100%;
  .sum-row { display: flex; justify-content: space-between; font-size: 12px; color: #5F6368; padding: 5px 0; b { font-size: 14px; font-weight: 700; color: #202124; } }
}
.pkg-picker-col { width: 100%; display: flex; flex-direction: column; }
.dialog-section-title { font-size: 13px; font-weight: 600; color: #5F6368; margin-bottom: 10px; display: flex; align-items: center; gap: 6px; }
.pkg-summary { background: #F3E5F5; border-radius: 8px; padding: 12px; margin-top: 12px; }
.dual-picker { display: flex; gap: 10px; flex: 1; align-items: stretch; }
.picker-panel {
  flex: 1; border: 1px solid #DADCE0; border-radius: 8px; display: flex; flex-direction: column; overflow: hidden; min-height: 200px; max-height: 280px;
  .picker-header { background: #F8F9FA; padding: 8px 12px; font-size: 12px; font-weight: 600; color: #5F6368; display: flex; align-items: center; }
  .picker-list { flex: 1; overflow-y: auto; padding: 4px; }
  .picker-empty { display: flex; align-items: center; justify-content: center; height: 100%; color: #DADCE0; font-size: 13px; }
}
.picker-item {
  display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 6px;
  cursor: pointer; transition: background .15s; margin-bottom: 2px;
  &:hover { background: #E8F0FE; }
  &.is-selected { opacity: 0.5; cursor: default; background: #F1F3F4; }
  .pi-name { flex: 1; font-size: 13px; color: #202124; font-weight: 500; }
  .pi-meta { display: flex; align-items: center; gap: 4px; font-size: 11px; color: #80868B; }
  .pi-add { color: #1A73E8; }
  .pi-check { color: #34A853; }
  .pi-order { width: 20px; text-align: center; font-size: 12px; font-weight: 700; color: #9AA0A6; }
  .pi-ops { display: flex; gap: 4px; i { color: #80868B; font-size: 13px; cursor: pointer; &:hover { color: #1A73E8; } } }
}
.selected-item { background: #E8F0FE !important; }</style>
