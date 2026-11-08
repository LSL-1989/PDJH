<template>
  <div id="app" :class="screenThemeClasses">
    <WindowBoard :ScreensInput="ScreensInput" kind="pharmacy" />
    <CallPopup
      :visible="callPopupVisible"
      :show-no="callPopupShowNo"
      :patient-name="callPopupPatientName"
      :office="callPopupOffice"
      :call-text="callPopupCallText"
    />
  </div>
</template>

<script>
/**
 * 药房窗口屏（路由 /pharmacy-window）
 * 职责：一块屏一个窗口。左半屏只显示窗口序号，右上叫号与右下队列分模块底色，患者行只显示序号和姓名
 * 消费者：候诊区大屏壳，templateRoute=/pharmacy-window（屏幕角色 PHARMACY_WINDOW）
 * 依赖：GetScreensInput；CallList/GetCallListInput；GetCallListInputPage(includePreparing)
 */
import WindowBoard from '../window-screen/WindowBoard.vue'
import pdjhScreenMixin from '@/mixins/pdjhScreenMixin'

export default {
  name: 'PharmacyWindowScreen',
  mixins: [pdjhScreenMixin],
  components: { WindowBoard }
}
</script>

<style scoped>
#app {
  height: 100vh;
  width: 100vw;
  margin: 0;
  padding: 0;
  overflow: hidden;
  background-color: #202124;
  font-family: 'Inter', 'Microsoft YaHei', -apple-system, sans-serif;
}
</style>
