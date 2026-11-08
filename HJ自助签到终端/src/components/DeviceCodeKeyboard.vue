<script setup lang="ts">
/**
 * 设备编号录入软键盘：数字、大写字母、连字符；供 BindingGuide 录入页使用。
 */
const emit = defineEmits<{
  input: [char: string]
  backspace: []
  clear: []
  confirm: []
}>()

const ROWS: { keys: string[]; rowClass?: string }[] = [
  { keys: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0'] },
  { keys: ['Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'] },
  { keys: ['A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L'], rowClass: 'row-asdf' },
  { keys: ['Z', 'X', 'C', 'V', 'B', 'N', 'M', '-'], rowClass: 'row-zxcv' }
]

const onKey = (key: string) => emit('input', key)
</script>

<template>
  <div class="device-keyboard">
    <div
      v-for="(row, ri) in ROWS"
      :key="ri"
      class="key-row"
      :class="row.rowClass"
    >
      <button
        v-for="key in row.keys"
        :key="key"
        type="button"
        class="key-btn neu-raised"
        @click="onKey(key)"
      >
        {{ key }}
      </button>
    </div>
    <div class="key-row action-row">
      <button type="button" class="key-btn action clear neu-raised" @click="emit('clear')">清空</button>
      <button type="button" class="key-btn action backspace neu-raised" @click="emit('backspace')">退格</button>
      <button type="button" class="key-btn action confirm neu-raised" @click="emit('confirm')">确认</button>
    </div>
  </div>
</template>

<style scoped>
.device-keyboard {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.key-row {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  gap: 4px;
  width: 100%;
}

.key-row.row-asdf {
  padding-left: 10px;
}

.key-row.row-zxcv {
  padding-left: 20px;
}

.key-btn {
  flex: 1 1 0;
  min-width: 0;
  height: 44px;
  padding: 0 2px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  color: var(--neu-highlight, #f8fafc);
}

.key-btn:active {
  transform: scale(0.96);
}

.key-btn.action {
  font-size: 14px;
}

.key-btn.clear {
  color: var(--neu-warning, #d97706);
}

.key-btn.backspace {
  color: var(--neu-error);
}

.key-btn.confirm {
  color: var(--neu-success);
  font-weight: 700;
}
</style>
