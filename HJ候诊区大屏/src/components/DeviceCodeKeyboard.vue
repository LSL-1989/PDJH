<script setup>
/**
 * 设备编号录入软键盘：数字、大写字母、连字符；供 BindingGuide 录入页使用。
 * 规则与自助签到终端 DeviceCodeKeyboard 一致。
 */
const emit = defineEmits(['input', 'backspace', 'clear', 'confirm'])

/** 数字行 + QWERTY 三行，「-」在末行末尾 */
const ROWS = [
  { keys: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0'] },
  { keys: ['Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'] },
  { keys: ['A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L'], rowClass: 'row-asdf' },
  { keys: ['Z', 'X', 'C', 'V', 'B', 'N', 'M', '-'], rowClass: 'row-zxcv' }
]

const onKey = (key) => emit('input', key)
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
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  background: #3a3b42;
  color: #e8eaed;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
}

.key-btn:active {
  transform: scale(0.96);
  background: #4a4b54;
}

.key-btn.action {
  font-size: 14px;
}

.key-btn.clear {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
  border-color: rgba(245, 158, 11, 0.3);
}

.key-btn.backspace {
  background: rgba(242, 139, 130, 0.12);
  color: #f28b82;
  border-color: rgba(242, 139, 130, 0.28);
}

.key-btn.confirm {
  background: linear-gradient(135deg, #1a73e8, #8ab4f8);
  color: #fff;
  border: none;
}
</style>
