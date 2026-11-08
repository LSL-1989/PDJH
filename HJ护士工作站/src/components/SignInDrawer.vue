<template>
  <el-drawer
    v-model="visible"
    title="签到兜底"
    size="480px"
    class="sign-in-drawer"
    destroy-on-close
    @close="$emit('close')"
  >
    <div class="sign-in-drawer__intro nd-card">
      <el-icon :size="20" color="#1E88E5"><EditPen /></el-icon>
      <span>检索患者后代签，适用于扫码失败或预约校验异常场景</span>
    </div>

    <el-input
      v-model="keyword"
      size="large"
      placeholder="卡号 / 排队号 / 姓名"
      clearable
      class="sign-in-drawer__search"
      @keyup.enter="search"
    >
      <template #append>
        <el-button type="primary" :loading="loading" @click="search">搜索</el-button>
      </template>
    </el-input>

    <div class="sign-in-drawer__results">
      <button
        v-for="item in results"
        :key="item.id"
        type="button"
        class="sign-in-item nd-card"
        :class="{ 'sign-in-item--active': selected?.id === item.id }"
        @click="selected = item"
      >
        <span class="sign-in-item__no">{{ item.showNo }}</span>
        <span class="sign-in-item__name">{{ item.patientName }}</span>
        <span class="sign-in-item__meta">{{ item.patientCode }} · {{ item.triageState }}</span>
      </button>
      <el-empty v-if="searched && !results.length" description="未找到患者" />
    </div>

    <template v-if="selected">
      <el-divider />
      <ReasonSelect v-model:reason-code="reasonCode" v-model:remark="remark" />
      <div class="sign-in-drawer__actions">
        <el-button type="primary" size="large" :loading="acting" @click="signIn(false)">代签到</el-button>
        <el-button size="large" :loading="acting" @click="signIn(true)">Override 签到</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import ReasonSelect from './ReasonSelect.vue'
import { searchPatient, manualSignIn } from '../api/nurseDesk'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'close', 'done'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const keyword = ref('')
const results = ref([])
const selected = ref(null)
const loading = ref(false)
const acting = ref(false)
const searched = ref(false)
const reasonCode = ref('')
const remark = ref('')

watch(visible, (v) => {
  if (!v) {
    keyword.value = ''
    results.value = []
    selected.value = null
    searched.value = false
  }
})

async function search() {
  if (!keyword.value.trim()) return
  loading.value = true
  try {
    const res = await searchPatient(keyword.value.trim())
    results.value = res.data || []
    searched.value = true
    selected.value = results.value[0] || null
  } finally {
    loading.value = false
  }
}

async function signIn(override) {
  if (!selected.value || !reasonCode.value) {
    ElMessage.warning('请选择患者和原因')
    return
  }
  acting.value = true
  try {
    const res = await manualSignIn({
      callListId: selected.value.id,
      reasonCode: reasonCode.value,
      remark: remark.value,
      skipSecondScan: true,
      skipApptCheck: override
    })
    if (res?.success) {
      ElMessage.success('签到成功')
      emit('done')
      visible.value = false
    }
  } finally {
    acting.value = false
  }
}
</script>

<style scoped>
.sign-in-drawer__intro {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  font-size: 0.8125rem;
  color: var(--nd-text-secondary);
  line-height: 1.45;
}
.sign-in-drawer__search {
  margin-bottom: 1rem;
}
.sign-in-drawer__results {
  max-height: 280px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.sign-in-item {
  width: 100%;
  text-align: left;
  padding: 0.875rem 1rem;
  border: 2px solid transparent;
  cursor: pointer;
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto;
  gap: 0.15rem 0.75rem;
  transition: border-color 0.15s;
}
.sign-in-item--active {
  border-color: var(--nd-primary);
}
.sign-in-item__no {
  font-size: 1.375rem;
  font-weight: 800;
  color: var(--nd-primary);
  grid-row: span 2;
  align-self: center;
}
.sign-in-item__name {
  font-weight: 600;
  color: var(--nd-text);
}
.sign-in-item__meta {
  font-size: 0.8125rem;
  color: var(--nd-text-secondary);
}
.sign-in-drawer__actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
}
.sign-in-drawer__actions .el-button {
  flex: 1;
  min-height: 44px;
}
</style>
