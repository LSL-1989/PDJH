<template>
  <el-drawer
    :model-value="modelValue"
    :title="title"
    direction="rtl"
    :size="size"
    :before-close="handleBeforeClose"
    :destroy-on-close="destroyOnClose"
    :close-on-click-modal="closeOnClickModal"
    :append-to-body="appendToBody"
    class="form-drawer"
    @update:model-value="$emit('update:modelValue', $event)"
    @open="$emit('open')"
    @close="$emit('close')"
    @closed="$emit('closed')"
  >
    <slot />
    <template v-if="$slots.footer" #footer>
      <div class="form-drawer__footer">
        <slot name="footer" />
      </div>
    </template>
  </el-drawer>
</template>

<script>
/**
 * 右侧表单抽屉：添加/编辑类填写面板
 * 消费者：屏幕管理、科室、诊室、医生档案等后台「添加」表单
 * 依赖：Element Plus el-drawer；页脚插槽固定在底部，表单体可滚动
 */
export default {
  name: 'FormDrawer',
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: ''
    },
    size: {
      type: [String, Number],
      default: '480px'
    },
    beforeClose: {
      type: Function,
      default: null
    },
    destroyOnClose: {
      type: Boolean,
      default: false
    },
    closeOnClickModal: {
      type: Boolean,
      default: true
    },
    appendToBody: {
      type: Boolean,
      default: true
    }
  },
  emits: ['update:modelValue', 'open', 'close', 'closed'],
  watch: {
    '$route'() {
      if (this.modelValue) {
        this.$emit('update:modelValue', false)
      }
    }
  },
  methods: {
    handleBeforeClose(done) {
      if (typeof this.beforeClose === 'function') {
        this.beforeClose()
      }
      done()
    }
  }
}
</script>
