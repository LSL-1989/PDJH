<template>
  <div id="Header">
    <!-- 上方图片 -->
    <!-- <div class="HeaderUpLeft"><img src="/Image/HeaderImg/little1.png" /></div>

    <div class="HeaderUpRight">
      <img src="/Image/HeaderImg/little3.png" />
    </div> -->
    <el-row class="HeaderMainrow">
      <!-- 医院名称 -->
      <el-col class="Header_col" :xs="16" :sm="16" :md="6" :lg="12" :xl="9">
        <div class="HospitalName">
          <!-- <h2>{{ ScreensInput.Subtitle }}</h2> -->
            <img
          class="Logo"
          src="@/assets/icon/图层1@2x.png"
        />.


        <!-- <div>龙胜各族自治县人民医院</div> -->
        </div>
      </el-col>
       <!-- 日期 -->
      <el-col class="Header_col" :xs="8" :sm="8" :md="6" :lg="12" :xl="9">
        <div class="HeaderDate">
          <!-- 年月日 -->
          <el-row style="width: 100%;display: flex;
    justify-content: center;
    align-items: center;">
            <el-col :xs="12" :sm="12" :md="6" :lg="12" :xl="9" style="text-align:right;">
                <div class="HeaderDay hidden-sm-and-down">{{ Day }}</div>
                <span class="HeaderYTD hidden-sm-and-down">{{ YTD }}</span>
            </el-col>
 <!-- 时分秒 -->
            <el-col :xs="12" :sm="12" :md="2" :lg="7" :xl="9">
              <b
                ><span class="HeaderHMS">{{ HMS }}</span></b
              >
            </el-col>
            <!-- 星期几 -->
            <!-- <el-col :xs="3" :md="3" :lg="5" :xl="4">
              
            </el-col> -->
           
          </el-row>
        </div>
      </el-col>
    </el-row>
    <!-- <el-row id="Header_row">
     
    </el-row> -->
    <!-- <el-row> -->
      <!-- 科室 -->
      <!-- <el-col class="Header_col"> -->
        <!-- <div class="HeaderTitle"> -->
          <!-- <h1 class="HospitalType">{{ ScreensInput.Name }}</h1> -->
          <!-- <div class="HospitalType">内科专家门诊</div> -->
           <!-- <div class="HospitalType">{{ ScreensInput.Name }}</div> -->
        <!-- </div> -->
      <!-- </el-col> -->
    <!-- </el-row> -->
    <!-- 下方图片 -->
    <!-- <div class="HeaderLoLeft"><img src="/Image/HeaderImg/little2.png" /></div>
    <div class="HeaderLoRight"><img src="/Image/HeaderImg/little4.png" /></div> -->
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
          console.error(err);xx
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
  display: flex;
    align-items: center;
  // background: linear-gradient(60deg, rgb(4,76,135),rgb(2,93,196));
  // 4,76,135
  //2,93,196
  // border: 1px solid rgb(64, 27, 0);
  position: relative;
  .HeaderMainrow{
    display: flex;
 align-items: center;
 width:100%;
 height:100%;
  }
}
.Logo{
      width: 40px;
    height: 40px;
    object-fit: cover;
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
  // height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}
#Header #Header_row .Header_col {
  height: 100%;
}
/*医院名称 */
#Header .HospitalName {
  // color: rgb(88, 51, 14);
  color:black;
  height: 100%;
  font-size: 32px;
  display: flex;
  justify-content: center;
  align-items: center; /*垂直居中*/
  letter-spacing: 8px;
      margin-bottom: 10px;
}
#Header .HeaderTitle {
  // height: 100%;
  // display: flex;
  // justify-content: center; /*水平居中*/
  // align-items: center;
  // background: url("/Image/HeaderImg/headline.png") no-repeat;
  // background-position-x: 50%;
  // background-size: 100%;
  // width: 60%;
  // margin: auto;
}
 .HospitalType {
  color: black;
  // color:rgb(88, 51, 14);
  z-index: 2;
  text-align: center;
  font-size: 2em;
  // margin-bottom: 15%;
}
#Header .HeaderTitle .Hospitalimg {
  position: absolute;
  height: 100%;
}
#Header .HeaderDate {
  // height: 100%;
  // display: flex;
  // justify-content: flex-end;
  // align-items: center; /*垂直居中*/
  text-align: center;
  font-size: 18px;
}
#Header .HeaderDate .HeaderHMS{
  font-size: 32px;
}
#Header .HeaderDate .HeaderYTD,
#Header .HeaderDate .HeaderDay,
#Header .HeaderDate .HeaderHMS {
  // color: rgb(106, 73, 39);
  color:black;
}
</style>