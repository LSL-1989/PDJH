<template>
  <el-popover
    v-model:visible="pickerVisible"
    :width="348"
    :placement="placement"
    trigger="click"
    popper-class="icon-picker-popper"
  >
    <icon-select :value="currentValue" @selected="onSelected" />
    <template #reference><div class="icon-picker-field">
      <span v-if="currentValue" class="icon-picker-field__preview">
        <i v-if="isFontIcon(currentValue)" :class="currentValue" />
        <svg-icon v-else :icon-class="currentValue" />
      </span>
      <el-input
        :model-value="currentValue"
        :placeholder="placeholder"
        :clearable="clearable"
        class="icon-picker-field__input"
        @input="onInput"
      />
    </div></template>
  </el-popover>
</template>

<script>
/**
 * 菜单图标输入：点击弹出图标网格，也可直接改名称。
 * 消费者：菜单管理新增/编辑抽屉。
 * 依赖：SvgSelect、icons/catalog。
 */
import IconSelect from '@/components/SvgSelect/index.vue'
import { isFontIcon } from '@/icons/catalog'

export default {
  name: 'IconPickerInput',
  components: {
    IconSelect
  },
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: '选择图标'
    },
    placement: {
      type: String,
      default: 'bottom-start'
    },
    clearable: {
      type: Boolean,
      default: true
    }
  },
  computed: {
    currentValue() {
      return this.modelValue
    }
  },
  data() {
    return {
      pickerVisible: false
    }
  },
  methods: {
    isFontIcon,
    onInput(val) {
      this.$emit('update:modelValue', val)
    },
    onSelected(name) {
      this.$emit('update:modelValue', name)
      this.$emit('selected', name)
      this.pickerVisible = false
    }
  }
}
</script>

<style lang="scss" scoped>
.icon-picker-field {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 10px;
  cursor: pointer;
}

.icon-picker-field__preview {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  background: transparent;
  border: none;
  box-shadow: none;
}

.icon-picker-field__preview .svg-icon,
.icon-picker-field__preview i {
  font-size: 18px;
  width: 1em;
  height: 1em;
  line-height: 1;
}

.icon-picker-field__input {
  flex: 1;
  min-width: 0;
}
</style>

<style lang="scss">
/* 弹层挂到 body，宽度必须盖住网格，避免第二列溢出到表单上 */
.icon-picker-popper.el-popper {
  box-sizing: border-box;
  padding: 12px;
  overflow: hidden;
}
</style>
