<template>
  <div class="reason-select">
    <el-form-item label="操作原因" required>
      <el-select v-model="localReason" placeholder="请选择原因" class="w-full" @change="emitChange">
        <el-option v-for="opt in REASON_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
    </el-form-item>
    <el-form-item v-if="localReason === 'OTHER'" label="备注" required>
      <el-input v-model="localRemark" type="textarea" rows="2" placeholder="请说明具体情况" @input="emitChange" />
    </el-form-item>
    <el-form-item v-else label="备注">
      <el-input v-model="localRemark" type="textarea" rows="2" placeholder="可选补充说明" @input="emitChange" />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { REASON_OPTIONS } from '../constants/reasonCodes'

const props = defineProps({
  reasonCode: { type: String, default: '' },
  remark: { type: String, default: '' }
})
const emit = defineEmits(['update:reasonCode', 'update:remark'])

const localReason = ref(props.reasonCode)
const localRemark = ref(props.remark)

watch(() => props.reasonCode, (v) => { localReason.value = v })
watch(() => props.remark, (v) => { localRemark.value = v })

function emitChange() {
  emit('update:reasonCode', localReason.value)
  emit('update:remark', localRemark.value)
}
</script>

<style scoped>
.w-full { width: 100%; }
</style>
