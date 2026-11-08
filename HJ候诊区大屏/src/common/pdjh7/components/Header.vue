<template>
  <div id="Header">
    <el-row id="Header_row">
      <!-- 医院名称 -->
      <el-col class="Header_col" :xs="0" :sm="8" :md="7" :lg="6" :xl="6">
        <div class="HospitalName">
        </div>
      </el-col>
      <!-- 科室 -->
      <el-col class="Header_col" :xs="24" :sm="9" :md="6" :lg="8" :xl="10">
        <div class="HeaderTitle">
          <div class="HospitalType">{{ ScreensInput.Name }}</div>
          <!-- <img class="Hospitalimg" src="/Image/HeaderImg/headline1.png" /> -->
        </div>
      </el-col>
      <!-- 日期 -->
      <el-col class="Header_col" :xs="0" :sm="7" :md="11" :lg="10" :xl="8">
        <div class="HeaderDate">
          <!-- 年月日 -->
          <el-row style="width: 100%">
            <el-col :md="10" :lg="12" :xl="9">
              <b>
                <span class="HeaderYTD hidden-sm-and-down">{{ YTD }}</span></b
              >
            </el-col>

            <!-- 星期几 -->
            <el-col :md="8" :lg="5" :xl="4">
              <b>
                <span class="HeaderDay hidden-sm-and-down">{{ Day }}</span></b
              >
            </el-col>
            <!-- 时分秒 -->
            <el-col :sm="24" :md="4" :lg="7" :xl="9">
              <b
                ><span class="HeaderHMS">{{ HMS }}</span></b
              >
            </el-col>
          </el-row>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import screenDisplayMixin from "@/mixins/screenDisplayMixin";
// import { formatDate } from "../common/date";
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
    timeFormate() {
      var aData = new Date(); //Wed Aug 21 2019 10:00:58 GMT+0800 (中国标准时间)
      // 获取当前年月日
      // this.YTD =
      //   aData.getFullYear() +
      //   "年" +
      //   (aData.getMonth() + 1) +
      //   "月" +
      //   aData.getDate() +
      //   "日";
      this.YTD = this.$moment(aData).format("YYYY年MM月DD日");
      // 获取当前星期几
      let weeks = ["日", "一", "二", "三", "四", "五", "六"];
      this.Day = "星期" + weeks[aData.getDay()];
      // 获取当前时分秒
      // this.HMS =
      //   aData.getHours() + ":" + aData.getMinutes() + ":" + aData.getSeconds();
      this.HMS = this.$moment(aData).format("HH:mm:ss");
    },
    getDateTime() {
      this.$axios
        .get("/api/System/GetDataTime")
        .then((res) => {
          // console.log("GetDataTime", res);
          let resDate = JSON.parse(res.data.data);
          this.YTD =
            resDate.Year + "年" + resDate.Month + "月" + resDate.Day + "日";
          this.Day = resDate.Week;
          // this.HMS = resDate.Time;
          this.HMS = resDate.Hour24 + ":" + resDate.Minute;
        })
        .catch((err) => {
          console.error(err);
        });
    },
    nowTimes() {
      setInterval(() => {
        // this.timeFormate();
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
  background: rgb(255, 255, 255);
  position: relative;
}
/*
Header内部样式
 */
#Header #Header_row {
  height: 100%;
}
#Header #Header_row .Header_col {
  height: 100%;
}
/*医院名称 */
#Header .HospitalName {
  height: 100%;
  display: flex;
  font-size: 1.4em;
  justify-content: flex-end;
  align-items: center; /*垂直居中*/
  font-family: 华文新魏;
}
#Header .HeaderTitle {
  height: 100%;
  display: flex;
  font-size: 20px;
  justify-content: center; /*水平居中*/
  background: url("/Image/HeaderImg/headline1.png") no-repeat;
  background-position-x: 50%;
  background-size: 100%;
  width: 60%;
  margin: auto;
}
#Header .HeaderTitle .HospitalType {
  color: white;
  z-index: 2;
  text-align: center;
  display: flex;
  align-items: center;
  font-size: 2em;
  font-family: 华文新魏;
}
#Header .HeaderTitle .Hospitalimg {
  position: absolute;
  height: 100%;
}
#Header .HeaderDate {
  height: 100%;
  display: flex;

  text-align: center;
  flex-direction: column;
  justify-content: center;
  font-size: 2em;
}
</style>