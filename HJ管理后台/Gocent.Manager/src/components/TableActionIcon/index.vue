<template>
  <el-tooltip :content="tip" placement="top" :open-delay="300" class="table-action-icon-wrap">
    <el-button text
      class="table-action-icon-btn"
      :class="typeClass"
      :icon="icon"
      :disabled="disabled"
      @click.stop="handleClick"
    />
  </el-tooltip>
</template>

<script>
/** 表格操作列图标按钮：仅图标 + Tooltip，供 TableActionColumn 插槽使用 */
export default {
  name: 'TableActionIcon',
  props: {
    icon: { type: String, required: true },
    tip: { type: String, required: true },
    type: { type: String, default: 'primary' },
    disabled: { type: Boolean, default: false }
  },
  computed: {
    typeClass() {
      return `table-action-icon-btn--${this.type}`
    }
  },
  methods: {
    handleClick(event) {
      if (!this.disabled) {
        this.$emit('click', event)
      }
    }
  }
}
</script>
