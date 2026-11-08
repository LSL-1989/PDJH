<script setup lang="ts">
/**
 * 设备编号录入软键盘：数字、大写字母、连字符；供 BindingGuide 录入页使用。
 * 按键行高固定，随外层 ScreenFrame 等比缩放，内部不做 flex 拉伸。
 */
const emit = defineEmits<{
  input: [char: string]
  backspace: []
  clear: []
  confirm: []
}>()

/** 数字行不变；字母按 QWERTY 三行，「-」在末行末尾补位 */
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
        class="key-btn"
        @click="onKey(key)"
      >
        {{ key }}
      </button>
    </div>
    <div class="key-row action-row">
      <button type="button" class="key-btn action clear" @click="emit('clear')">清空</button>
      <button type="button" class="key-btn action backspace" @click="emit('backspace')">退格</button>
      <button type="button" class="key-btn action confirm" @click="emit('confirm')">确认</button>
    </div>
  </div>
</template>

<style scoped>
.device-keyboard {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  height: auto;
  flex: 0 0 auto;
}

.key-row {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  gap: 4px;
  width: 100%;
  height: 44px;
  flex-shrink: 0;
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
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background var(--motion-duration) var(--motion-easing), transform 0.1s;
  box-shadow: 0 2px 0 rgba(14, 165, 233, 0.1);
}

.key-btn:active {
  transform: scale(0.96);
  background: var(--color-primary-dim);
}

.key-btn.action {
  font-size: 15px;
}

.key-btn.clear {
  background: rgba(245, 158, 11, 0.15);
  color: var(--color-warning);
  border-color: rgba(245, 158, 11, 0.3);
  box-shadow: 0 2px 0 rgba(245, 158, 11, 0.15);
}

.key-btn.backspace {
  background: rgba(239, 68, 68, 0.12);
  color: var(--color-error);
  border-color: rgba(239, 68, 68, 0.25);
  box-shadow: 0 2px 0 rgba(239, 68, 68, 0.12);
}

.key-btn.confirm {
  background: linear-gradient(135deg, var(--color-primary-deep), var(--color-primary));
  color: var(--color-text-inverse);
  border: none;
  box-shadow: var(--shadow-glow);
}
</style>
