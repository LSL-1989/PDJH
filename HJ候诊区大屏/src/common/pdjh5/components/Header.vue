<template>
  <div id="Header">
    <!-- 上方图片 -->
    <div class="HeaderUpLeft"><img src="/Image/HeaderImg/little1.png" /></div>

    <div class="HeaderUpRight">
      <img src="/Image/HeaderImg/little3.png" />
    </div>
    <el-row id="Header_row">
      <!-- 医院名称 -->
      <el-col class="Header_col" :xs="0" :sm="8" :md="7" :lg="6" :xl="6">
        <div class="HospitalName">
        </div>
      </el-col>
      <!-- 科室 -->
      <el-col class="Header_col" :xs="24" :sm="9" :md="6" :lg="8" :xl="10">
        <div class="HeaderTitle">
          <h1 class="HospitalType">{{ ScreensInput.Name }}</h1>
          <!-- <img class="Hospitalimg" src="/Image/HeaderImg/headline.png" /> -->
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
            <el-col :md="8" :lg="6" :xl="4">
              <b>
                <span class="HeaderDay hidden-sm-and-down">{{ Day }}</span></b
              >
            </el-col>
            <!-- 时分秒 -->
            <el-col :sm="24" :md="4" :lg="6" :xl="9">
              <b
                ><span class="HeaderHMS">{{ HMS }}</span></b
              >
            </el-col>
          </el-row>
        </div>
      </el-col>
    </el-row>
    <!-- 下方图片 -->
    <div class="HeaderLoLeft"><img src="/Image/HeaderImg/little2.png" /></div>
    <div class="HeaderLoRight"><img src="/Image/HeaderImg/little4.png" /></div>
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
  border: 1px solid rgb(64, 27, 0);
  position: relative;
}
/*
Header中的四边图片
 */
#Header .HeaderUpLeft {
  position: absolute;
  top: -5%;
  left: 0px;
}
#Header .HeaderUpRight {
  position: absolute;
  top: -5%;
  right: 0px;
}
#Header .HeaderLoLeft {
  position: absolute;
  bottom: -8%;
  left: 0px;
}
#Header .HeaderLoRight {
  position: absolute;
  bottom: -8%;
  right: 0px;
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
  color: rgb(88, 51, 14);
  height: 100%;
  display: flex;
  font-size: 18px;
  justify-content: center;
  align-items: center; /*垂直居中*/
}
#Header .HeaderTitle {
  height: 100%;
  display: flex;
  justify-content: center; /*水平居中*/
  align-items: center;
  background: url("/Image/HeaderImg/headline.png") no-repeat;
  background-position-x: 50%;
  background-size: 100%;
  width: 60%;
  margin: auto;
  //
}
#Header .HeaderTitle .HospitalType {
  color: white;
  z-index: 2;
  display: flex;
  text-align: center;
  font-size: 2em;
}
#Header .HeaderTitle .Hospitalimg {
  height: 100%;
  position: absolute;
}
#Header .HeaderDate {
  height: 100%;
  display: flex;

  text-align: center;
  flex-direction: column;
  justify-content: center;
  font-size: 2em;
}
#Header .HeaderDate .HeaderYTD,
#Header .HeaderDate .HeaderDay,
#Header .HeaderDate .HeaderHMS {
  color: rgb(106, 73, 39);
}
</style>