<template>
  <div :class="{'has-logo':showLogo}">
    <logo v-if="showLogo" :collapse="isCollapse" />
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :background-color="menuTheme.menuBg"
        :text-color="menuTheme.menuText"
        :unique-opened="false"
        :active-text-color="menuTheme.menuActiveText"
        :collapse-transition="false"
        mode="vertical"
      >
        <sidebar-item v-for="route in routes" :key="route.path" :item="route" :base-path="route.path" />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Logo from './Logo'
import SidebarItem from './SidebarItem'

/** 与 styles/variables.scss 侧栏色值一致。Vue CLI 5 默认不把 .scss 当 CSS module，:export 无法注入 JS。 */
const menuTheme = {
  menuText: '#94A3B8',
  menuActiveText: '#0EA5E9',
  menuBg: '#0F172A'
}

export default {
  components: { SidebarItem, Logo },
  data() {
    return { menuTheme }
  },
  computed: {
    ...mapGetters([
      'sidebar'
    ]),
    routes() {
      return this.$store.state.user.AllRoutes
    },
    activeMenu() {
      const route = this.$route
      const { meta, path } = route
      // if set path, the sidebar will highlight the path you set
      if (meta.activeMenu) {
        return meta.activeMenu
      }
      return path
    },
    showLogo() {
      return this.$store.state.settings.sidebarLogo
    },
    isCollapse() {
      return !this.sidebar.opened
    }
  },
  mounted() {
    // console.log("生成侧边栏的地方",this.$store.state.user.AllRoutes);
  }
}
</script>
