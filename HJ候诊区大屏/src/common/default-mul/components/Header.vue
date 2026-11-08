<template>
  <div id="Header">
    <el-row id="Header_row" type="flex" align="middle" justify="space-between">
      <!-- 医院名称 -->
      <el-col class="Header_col" :span="8">
        <div class="HospitalName">
        </div>
      </el-col>
      <!-- 科室 -->
      <el-col class="Header_col" :span="8">
        <div class="HeaderTitle">
          <h1 class="HospitalType">{{ ScreensInput.Name }}</h1>
        </div>
      </el-col>
      <!-- 日期 -->
      <el-col class="Header_col" :span="8">
        <div class="HeaderDate">
          <div class="date-part">{{ YTD }} &nbsp; {{ Day }}</div>
          <div class="time-part">{{ HMS }}</div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import screenDisplayMixin from "@/mixins/screenDisplayMixin";
export default {
  name: "Header",
  mixins: [screenDisplayMixin],
  data() {
    return {
      YTD: "", //年月日
      Day: "", //星期几
      HMS: "", //获取当前时分秒
    };
  },
  props: ["ScreensInput"],
  created() {
    this.getDateTime();
    this.nowTimes();
  },
  methods: {
    getDateTime() {
      this.$axios
        .get("/api/System/GetDataTime")
        .then((res) => {
          let resDate = JSON.parse(res.data.data);
          this.YTD =
            resDate.Year + "年" + resDate.Month + "月" + resDate.Day + "日";
          this.Day = resDate.Week;
          this.HMS = resDate.Hour24 + ":" + resDate.Minute;
        })
        .catch((err) => {
          console.error(err);
        });
    },
    nowTimes() {
      setInterval(() => {
        this.getDateTime();
      }, 60000);
    },
  },
};
</script>

<style lang="scss" scoped>
#Header {
  width: 100%;
  height: 100%;
  background: #1c1d21; /* Slightly darker/different shade for header material */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5); /* Material elevation */
  color: #e8eaed;
  z-index: 10;
  position: relative;
}

#Header_row {
  width: 100%;
  height: 100%;
  padding: 0 3vw;
}

.Header_col {
  height: 100%;
  display: flex;
  align-items: center;
}

.HospitalName {
  height: 100%;
  display: flex;
  align-items: center;
  h2 {
    font-size: 1.8vw;
    font-weight: 500;
    margin: 0;
    color: #9aa0a6; /* Subdued text for subtitle */
    letter-spacing: 2px;
  }
}

.HeaderTitle {
  height: 100%;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  
  .HospitalType {
    font-size: 2.8vw;
    font-weight: 800;
    margin: 0;
    color: #8ab4f8; /* Google Blue Light for Dark Theme */
    letter-spacing: 4px;
    text-shadow: 0 2px 8px rgba(138, 180, 248, 0.3);
  }
}

.HeaderDate {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-end;
  
  .date-part {
    font-size: 1.2vw;
    color: #9aa0a6;
    margin-bottom: 0.5vh;
    font-weight: 500;
  }
  
  .time-part {
    font-size: 2.5vw;
    font-weight: 700;
    color: #e8eaed;
    line-height: 1;
  }
}
</style>