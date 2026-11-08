<template>
  <el-table-column
    :fixed="fixedRight"
    :label="label"
    :width="colWidth"
    align="center"
    class-name="table-action-col"
  >
    <template #default="scope">
      <div class="table-action-btns">
        <slot :row="scope.row" :$index="scope.$index" />
      </div>
    </template>
  </el-table-column>
</template>

<script>
import { tableActionColWidth } from '@/utils/tableAction'

/**
 * 列表末列操作列：仅在表格横向溢出时右侧固定，宽度按图标按钮数计算
 * 消费者：各后台列表页 el-table 末列操作区
 */
export default {
  name: 'TableActionColumn',
  props: {
    label: { type: String, default: '操作' },
    /** 操作图标按钮数量，用于计算列宽 */
    count: { type: Number, required: true },
    /** 可选：手动覆盖列宽 */
    width: { type: Number, default: null }
  },
  data() {
    return {
      fixedRight: false,
      layoutTimer: null,
      resizeObserver: null,
      tableDataUnwatch: null
    }
  },
  computed: {
    colWidth() {
      return this.width != null ? this.width : tableActionColWidth(this.count)
    }
  },
  mounted() {
    this.bindLayoutWatchers()
    this.scheduleLayoutCheck()
  },
  updated() {
    this.scheduleLayoutCheck()
  },
  beforeUnmount() {
    this.unbindLayoutWatchers()
  },
  methods: {
    bindLayoutWatchers() {
      window.addEventListener('resize', this.scheduleLayoutCheck)
      const table = this.getTableInstance()
      if (table) {
        this.tableDataUnwatch = table.$watch(
          () => table.data,
          () => this.scheduleLayoutCheck(),
          { deep: false }
        )
      }
      const bodyWrapper = this.getBodyWrapper()
      if (bodyWrapper && typeof ResizeObserver !== 'undefined') {
        this.resizeObserver = new ResizeObserver(() => this.scheduleLayoutCheck())
        this.resizeObserver.observe(bodyWrapper)
      }
    },
    unbindLayoutWatchers() {
      window.removeEventListener('resize', this.scheduleLayoutCheck)
      if (this.layoutTimer) {
        clearTimeout(this.layoutTimer)
        this.layoutTimer = null
      }
      if (this.tableDataUnwatch) {
        this.tableDataUnwatch()
        this.tableDataUnwatch = null
      }
      if (this.resizeObserver) {
        this.resizeObserver.disconnect()
        this.resizeObserver = null
      }
    },
    scheduleLayoutCheck() {
      if (this.layoutTimer) clearTimeout(this.layoutTimer)
      this.layoutTimer = setTimeout(() => {
        this.layoutTimer = null
        this.updateFixedState()
      }, 60)
    },
    getTableInstance() {
      let parent = this.$parent
      while (parent) {
        if (parent.$options.name === 'ElTable') return parent
        parent = parent.$parent
      }
      return null
    },
    getBodyWrapper() {
      const table = this.getTableInstance()
      return table && table.$el ? table.$el.querySelector('.el-table__body-wrapper') : null
    },
    updateFixedState() {
      const table = this.getTableInstance()
      const bodyWrapper = this.getBodyWrapper()
      if (!table || !bodyWrapper) return

      const needsFixed = bodyWrapper.scrollWidth > bodyWrapper.clientWidth + 1
      const nextFixed = needsFixed ? 'right' : false
      if (this.fixedRight === nextFixed) return

      this.fixedRight = nextFixed
      this.$nextTick(() => {
        if (typeof table.doLayout === 'function') {
          table.doLayout()
        }
        // doLayout 后列宽可能变化，再校验一次
        this.$nextTick(() => this.recheckFixedAfterLayout())
      })
    },
    recheckFixedAfterLayout() {
      const bodyWrapper = this.getBodyWrapper()
      if (!bodyWrapper) return
      const needsFixed = bodyWrapper.scrollWidth > bodyWrapper.clientWidth + 1
      const nextFixed = needsFixed ? 'right' : false
      if (this.fixedRight !== nextFixed) {
        this.fixedRight = nextFixed
        const table = this.getTableInstance()
        if (table && typeof table.doLayout === 'function') {
          table.doLayout()
        }
      }
    }
  }
}
</script>
