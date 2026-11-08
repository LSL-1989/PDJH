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
  </div>
</template>

<script>
import Header from "./components/Header.vue";
import Main from "./components/Main.vue";
import pdjhScreenMixin from "@/mixins/pdjhScreenMixin";

export default {
  name: "App",
  mixins: [pdjhScreenMixin],
  components: {
    Header,
    Main,
  },
};
</script>

<style scoped>
#app {
  height: 100vh;
  width: 100vw;
  padding: 0px;
  margin: 0px;
  background-color: #202124; /* Google Material Dark Surface */
  overflow: hidden;
  font-family: "Inter", "Microsoft YaHei", -apple-system, sans-serif;
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
