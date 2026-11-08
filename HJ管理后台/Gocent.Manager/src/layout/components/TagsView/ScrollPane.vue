<template>
  <el-scrollbar ref="scrollContainer" :vertical="false" class="scroll-container" @wheel.prevent="handleScroll">
    <slot />
  </el-scrollbar>
</template>

<script>
const tagAndTagSpacing = 4 // tagAndTagSpacing

export default {
  name: 'ScrollPane',
  data() {
    return {
      left: 0
    }
  },
  computed: {
    scrollWrapper() {
      const bar = this.$refs.scrollContainer
      return bar && bar.wrapRef ? bar.wrapRef : null
    }
  },
  mounted() {
    this.$nextTick(() => {
      if (this.scrollWrapper) {
        this.scrollWrapper.addEventListener('scroll', this.emitScroll, true)
      }
    })
  },
  beforeUnmount() {
    if (this.scrollWrapper) {
      this.scrollWrapper.removeEventListener('scroll', this.emitScroll)
    }
  },
  methods: {
    handleScroll(e) {
      const eventDelta = e.wheelDelta || -e.deltaY * 40
      const $scrollWrapper = this.scrollWrapper
      if (!$scrollWrapper) return
      $scrollWrapper.scrollLeft = $scrollWrapper.scrollLeft + eventDelta / 4
    },
    emitScroll() {
      this.$emit('scroll')
    },
    moveToTarget(currentTag) {
      const currentEl = (currentTag && currentTag.$el) || currentTag
      const bar = this.$refs.scrollContainer
      if (!currentEl || !bar) return
      const $container = bar.$el
      const $containerWidth = $container.offsetWidth
      const $scrollWrapper = this.scrollWrapper
      if (!$scrollWrapper) return
      const tagList = [...$container.querySelectorAll('.tags-view-item')]

      let firstTag = null
      let lastTag = null

      if (tagList.length > 0) {
        firstTag = tagList[0]
        lastTag = tagList[tagList.length - 1]
      }

      if (firstTag === currentEl) {
        $scrollWrapper.scrollLeft = 0
      } else if (lastTag === currentEl) {
        $scrollWrapper.scrollLeft = $scrollWrapper.scrollWidth - $containerWidth
      } else {
        const currentIndex = tagList.findIndex(item => item === currentEl)
        if (currentIndex < 0) return
        const prevTag = tagList[currentIndex - 1]
        const nextTag = tagList[currentIndex + 1]
        if (!prevTag || !nextTag) return

        const afterNextTagOffsetLeft = nextTag.offsetLeft + nextTag.offsetWidth + tagAndTagSpacing
        const beforePrevTagOffsetLeft = prevTag.offsetLeft - tagAndTagSpacing

        if (afterNextTagOffsetLeft > $scrollWrapper.scrollLeft + $containerWidth) {
          $scrollWrapper.scrollLeft = afterNextTagOffsetLeft - $containerWidth
        } else if (beforePrevTagOffsetLeft < $scrollWrapper.scrollLeft) {
          $scrollWrapper.scrollLeft = beforePrevTagOffsetLeft
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.scroll-container {
  white-space: nowrap;
  position: relative;
  overflow: hidden;
  width: 100%;
  ::v-deep {
    .el-scrollbar__bar {
      bottom: 0px;
    }
    .el-scrollbar__wrap {
      height: 49px;
    }
  }
}
</style>
