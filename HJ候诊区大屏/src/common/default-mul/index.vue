<template>
  <div id="app" :class="screenThemeClasses">
    <el-container id="AppContainer">
      <el-row id="AppRow" :gutter="0">
        <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24" id="HeaderCol">
          <Header id="Header" :ScreensInput="ScreensInput" />
        </el-col>
        <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24" id="MainCol">
          <Main id="Main" :ScreensInput="ScreensInput" />
        </el-col>
      </el-row>
    </el-container>
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
 * 候诊区大屏默认多诊室按行模板（/default-mul）
 * 配色对齐 /default（原 pdjh1）
 */
import Header from './components/Header.vue'
import Main from './components/Main.vue'
import pdjhScreenMixin from '@/mixins/pdjhScreenMixin'

export default {
  name: 'DefaultMulScreen',
  mixins: [pdjhScreenMixin],
  components: {
    Header,
    Main
  }
}
</script>

<style scoped>
#app {
  height: 100vh;
  width: 100vw;
  padding: 0px;
  margin: 0px;
  background-color: #202124;
  overflow: hidden;
  font-family: 'Inter', 'Microsoft YaHei', -apple-system, sans-serif;
}
#AppContainer {
  height: 100%;
}
#AppContainer #AppRow {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}
#AppContainer #AppRow #HeaderCol {
  width: 100%;
  height: 10vh;
  flex: none;
}
#AppContainer #AppRow #MainCol {
  width: 100%;
  flex: 1;
  background-color: #202124;
}
</style>
