<template>
  <section class="app-main">
    <router-view v-slot="{ Component }">
      <transition name="fade-transform" mode="out-in">
        <keep-alive :include="cachedViews">
          <component :is="Component" :key="key" />
        </keep-alive>
      </transition>
    </router-view>
  </section>
</template>

<script>
export default {
  name: 'AppMain',
  computed: {
    cachedViews() {
      return this.$store.state.tagsView.cachedViews
    },
    key() {
      return this.$route.path
    }
  },
  mounted() {
    this.$Bus.$on('removeCache', this.removeCache)
    this.$Bus.$on('removeRedirect', this.removeRedirect)
  },
  beforeUnmount() {
    this.$Bus.$off('removeCache', this.removeCache)
    this.$Bus.$off('removeRedirect', this.removeRedirect)
  },
  methods: {
    /** Vue 3 已无 $children/$vnode，标签关闭时依靠 keep-alive include 失效缓存 */
    removeCache() {},
    removeRedirect() {}
  }
}
</script>

<style lang="scss" scoped>
.app-main {
  min-height: calc(100vh - 50px);
  width: 100%;
  position: relative;
  overflow: hidden;
}
.fixed-header + .app-main {
  padding-top: 50px;
}
.hasTagsView {
  .app-main {
    min-height: calc(100vh - 84px);
  }
}

.fixed-header + .app-main {
  padding-top: 84px;
}
</style>

<style lang="scss">
.el-popup-parent--hidden {
  .fixed-header {
    padding-right: 15px;
  }
}
</style>
