<template>
  <div id="Main" :class="screenLayoutClass">
    <el-row id="MainTop" :gutter="6">
      <el-col id="Left">
        <!-- 正在呼叫 -->
        <div id="Callout">
          <div class="Bottom LeftBottom" style="height: 60p%">
            <div id="CallDiv">
              <div id="CallName" cellpadding="0" cellspacing="0">
                <!--  style="border-spacing: 0em 1.2em" -->
                <el-row class="tr">
                  <el-col
                    :xs="6"
                    :sm="6"
                    class="outpatcol"
                    :md="6"
                    :lg="6"
                    :xl="6"
                    ><b>就诊中</b></el-col
                  >
                  <el-col
                    :xs="18"
                    :sm="18"
                    class="outpatPatientName"
                    :md="18"
                    :lg="18"
                    :xl="18"
                  >
                    <!-- 1号 -->
                    <b style="margin-right: 12px">{{
                      CallPeople.xh == undefined
                        ? "&nbsp;"
                        : CallPeople.xh + "号"
                    }}</b>
                    <b>{{ CallPeople.PatientName }}</b>
                    <!-- {{CallPeople.xh}} -->
                  </el-col>
                  <!-- <el-col :xs="6" :sm="6" -->
                  <!-- :md="6" -->
                  <!-- :lg="6" -->
                  <!-- :xl="6" -->
                  <!-- > -->
                  <!-- 周庆梅 -->
                  <!-- {{CallPeople.PatientName}} -->
                  <!-- </el-col> -->

                  <!-- <el-col -->
                  <!-- :xs="6" -->
                  <!-- :sm="6" -->
                  <!-- :md="6" -->
                  <!-- :lg="6" -->
                  <!-- :xl="6" -->
                  <!-- class="hidden-sm-and-down" -->
                  <!-- >就诊</el-col -->
                  <!-- >  -->
                </el-row>
                <el-row class="WaitingTitleRow">
                  <el-col
                    class="WaitingTitleCol"
                    :xs="24"
                    :sm="24"
                    :md="24"
                    :lg="24"
                    :xl="24"
                  >
                    <b>等待就诊</b>
                  </el-col>
                </el-row>
                <el-row
                  class="Waitingtr"
                  v-for="(i, index) in WaitingStaff.Page"
                  :key="index"
                >
                  <el-col
                    :xs="6"
                    :sm="6"
                    class="waitingxh"
                    :md="6"
                    :lg="6"
                    :xl="6"
                  >
                    {{
                      WaitingStaff.Data[i - 1] == undefined
                        ? "&nbsp;"
                        : WaitingStaff.Data[i - 1].xh + "号"
                    }}
                  </el-col>
                  <el-col
                    :xs="18"
                    :sm="18"
                    class="waitingPatientName"
                    :md="18"
                    :lg="18"
                    :xl="18"
                  >
                    {{
                      WaitingStaff.Data[i - 1] == undefined
                        ? "&nbsp;"
                        : WaitingStaff.Data[i - 1].PatientName
                    }}
                  </el-col>
                </el-row>
              </div>
            </div>
          </div>
        </div>
        <!-- 过号人员 -->
      </el-col>
      <!-- Right -->
    </el-row>
    <!-- 提示栏 -->
    <!-- <el-row v-if="showMarqueeBar" id="MainBottom">
      <el-col
        :xs="24"
        :sm="24"
        :md="24"
        :lg="24"
        :xl="24"
        id="BottomSubtitle"
        style="display: flex; justify-content: center; align-items: center"
      >
        <marquee v-if="Subtitle == null">
          <h1 class="Subtitle">
            {{ screenMarqueeText }}
          </h1>
        </marquee>
        <h1 class="Subtitle ShowSubtitle" v-if="Subtitle != null">
          {{ Subtitle }}
        </h1>
      </el-col>
    </el-row> -->
    <!-- 语音播报测试 -->
    <audio
      class="audio"
      :src="audiosrc"
      autoplay="autoplay"
      :hidden="true"
      id="audio"
      ref="audio"
    ></audio>
  </div>
</template>

<script>
import screenDisplayMixin from "@/mixins/screenDisplayMixin";
import Speech from "speak-tts";
export default {
  name: "Main",
  mixins: [screenDisplayMixin],
  data() {
    return {
      Subtitle: null,
      audiosrc: "",
      speech: null,
      MetData: [], //队列元数据，包含过号人员，等待人员，正在呼叫人员
      IsWaitingStaff: true, //false, //是否有等待人员
      WaitingStaff: {
        // Totle: 0,
        // state: 0,
        // Page: 24,
        // Index: 1,
        // Data: [],
        // PageCount: 0,
        Totle: 25,
        state: 0,
        Page: 6,
        Index: 1,
        Data: [
          {
            id: 0,
            PatientCode: "001",
            PatientName: "古手羽打野",
            xhlb: "C",
            xh: "1",
          },
        ],
        PageCount: 0,
      }, //等待人员列表
      IsPastStaff: false, //是否有过号人员列表
      PastStaff: {
        state: 3,
        Totle: 0,
        Page: 6,
        Index: 1,
        PageCount: 0,
        Data: [],
      }, //过号人员列表
      CallPeople: { PatientName: "暂无患者" },
      jiaohaostring: [],
      jiaohao: {},
      jiaohaoint: 0,
      CallList: [],
      Calli: 0,
      DepartmentInput: {},
      oAudio: null,
    };
  },
  props: ["ScreensInput"],
  methods: {
    GetMetData() {
      //异步获取元数据
      this.$axios
        .post("/api/CallList/GetCallListInput", { ID: this.ScreensInput.ID })
        .then((res) => {
          this.MetData = res.data.data;
          this.GetCallPeople();
        })
        .catch((err) => {
          console.error(err);
        });
    },
    GetWaitingStaff(e) {
      if (this.jiaohaostring.length != 0) {
        if (
          this.WaitingStaff.Index >= this.WaitingStaff.PageCount ||
          this.WaitingStaff.PageCount == 0
        ) {
          if (this.jiaohaoint + 1 == this.jiaohaostring.length) {
            this.jiaohaoint = 0;
          } else {
            this.jiaohaoint++;
          }
          this.jiaohao = this.jiaohaostring[this.jiaohaoint];
          this.WaitingStaff.Index = 0;
        }
      }

      this.WaitingStaff.Index = e == undefined ? 1 : e + 1; //设置等待人员列表显示页，轮播图会传参进来
      // console.log({
      //   index: this.WaitingStaff.Index,
      //   page: this.WaitingStaff.Page,
      //   state: this.WaitingStaff.state,
      //   ScreensID: this.ScreensInput.ID,
      //   isSingle: true,
      //   Code: this.jiaohao.Code,
      //   // DeptId: this.DepartmentInput.ID,
      // });
      //异步获取正在排队数据
      this.$axios
        .post("/api/CallList/GetCallListInputPage", {
          index: this.WaitingStaff.Index,
          page: this.WaitingStaff.Page,
          state: this.WaitingStaff.state,
          ScreensID: this.ScreensInput.ID,
          isSingle: true,
          Code: this.jiaohao.Code,
          // DeptId: this.DepartmentInput.ID,
        })
        .then((res) => {
          // console.log("WaitingStaffParams", {
          //   index: this.WaitingStaff.Index,
          //   page: this.WaitingStaff.Page,
          //   state: this.WaitingStaff.state,
          //   ScreensID: this.ScreensInput.ID,
          //   isSingle: true,
          //   Code: this.jiaohao.Code,
          //   DeptId: this.DepartmentInput.ID,
          // });
          // console.log("WaitingStaff", res.data.data.Data);
          // console.log(this.DepartmentInput.ID);
          // console.log(res.data.data.Data);
          // res.data.data.Data.filter((a) => {
          //   if (a.DeptId == this.DepartmentInput.ID) {
          //     this.WaitingStaff.Data.push(a);
          //   }
          // });
          // console.log("WaitingStaff", res.data.data);
          this.WaitingStaff.Data = res.data.data.Data; //获取等待数据
          this.WaitingStaff.Totle = res.data.data.Totle;
          // this.WaitingStaff.Totle = this.WaitingStaff.Data.length;
          this.WaitingStaff.Index = res.data.data.Index;

          this.WaitingStaff.PageCount = res.data.data.PageCount;
          // this.WaitingStaff.PageCount = Math.floor(
          //   (this.WaitingStaff.Totle - 1 + this.WaitingStaff.Page) /
          //     this.WaitingStaff.Page
          // );

          // console.log(this.WaitingStaff);
          this.IsWaitingStaff = this.WaitingStaff.Totle > 0;
          // if (this.WaitingStaff.PageCount <= 1) {
          //   window.setTimeout(() => {
          //     this.GetMetData();
          //   }, this.ScreensInput.ScrollTime * 1000);
          // }
        })
        .catch((err) => {
          console.error(err);
        });

      // this.WaitingStaff.Data = this.MetData.filter((a) => a.state == 0).slice(
      //   (this.WaitingStaff.Index - 1) * this.WaitingStaff.Page,
      //   (this.WaitingStaff.Index - 1) * this.WaitingStaff.Page +
      //     this.WaitingStaff.Page
      // ); //过滤源数据后分页显示
    },
    GetPastStaff(e) {
      this.PastStaff.Index = e == undefined ? 1 : e + 1; //设置等待人员列表显示页
      //异步获取过号人员数据
      this.$axios
        .post("/api/CallList/GetCallListInputPage", {
          index: this.PastStaff.Index,
          page: this.PastStaff.Page,
          state: this.PastStaff.state,
          ScreensID: this.ScreensInput.ID,
          isSingle: true,
          Code: this.jiaohao.Code,
        })
        .then((res) => {
          // console.log("GetPastStaff", res.data.data);
          // res.data.data.Data.filter((a) => {
          //   if (a.DeptId == this.DepartmentInput.ID) {
          //     // console.log(a);
          //     this.PastStaff.Data.push(a);
          //   }
          // });

          this.PastStaff.Data = res.data.data.Data; //获取等待数据
          this.PastStaff.Totle = res.data.data.Totle;
          // this.WaitingStaff.Totle = this.WaitingStaff.Data.length;
          this.PastStaff.Index = res.data.data.Index;

          this.PastStaff.PageCount = res.data.data.PageCount;
          // this.PastStaff.Totle = this.PastStaff.Data.length;
          // this.PastStaff.PageCount = Math.floor(
          //   (this.PastStaff.Totle - 1 + this.PastStaff.Page) /
          //     this.PastStaff.Page
          // );
          // console.log(this.PastStaff)
          // console.log(this.PastStaff);
          this.IsPastStaff = this.PastStaff.Totle > 0;
        })
        .catch((err) => {
          console.error(err);
        });
    },
    GetCallPeople() {
      this.CallPeople =
        this.MetData.filter((a) => a.state == 1)[0] == undefined
          ? { PatientName: "暂无患者" }
          : this.MetData.filter((a) => a.state == 1)[0];
    },
    //语音播报队列
    CallSpeak(data, CallInput) {
      //修改叫号信息表中的状态
      if (CallInput != null || CallInput != undefined) {
        this.$axios
          .post("api/CallMsg/EditCallMsg", CallInput)
          .then((res) => {})
          .catch((err) => {
            console.error(err);
          });
      }
    },
    //之前的
    // CallListSpeak() {
    //   try {
    //     this.Subtitle = this.CallList[0].showMsg;
    //     // this.audiosrc = this.CallList[0].fileName;

    //     let oAudio = this.$refs.audio;
    //     // console.log("audio", oAudio);
    //     if (oAudio.paused) {
    //       //暂停中
    //       oAudio.src = this.CallList[0].fileName;
    //       oAudio.play();
    //     }

    //     oAudio.onended = () => {
    //       //播放完毕，重新循环播放
    //       // console.log("播放完毕");

    //       if (this.Calli < this.ScreensInput.CallNum - 1) {
    //         oAudio.load();
    //         oAudio.play();
    //       }
    //       if (this.Calli >= this.ScreensInput.CallNum - 1) {
    //         this.Subtitle = null;
    //         this.Calli = -1;
    //         // console.log("清理呼叫列表");
    //         this.CallList.shift(); //音频播放完毕，删除
    //         if (this.CallList.length != 0) {
    //           this.CallListSpeak();
    //         }
    //       }
    //       this.Calli++;
    //     };
    //   } catch (error) {
    //     alert(JSON.stringify(error));
    //   }
    // },
    //新的呼叫
    async CallListSpeak() {
      this.Subtitle = this.CallList[0].showMsg;
      // this.audiosrc = this.CallList[0].fileName;
      // let oAudio = this.$refs.audio;
      console.log("判断oAudio.paused", this.oAudio.paused);
      //console.log("oAudio.ended", this.oAudio.ended); //播放结束返回true否则返回false
      if (this.oAudio.paused) {
        //暂停中,这里使用paused的初衷是判断audio是否播放完毕，上一段播放完毕了再继续播放，达到一个排队播放的效果，paused可以替换成更好的属性
        this.oAudio.src = this.CallList[0].fileName;
        this.oAudio.load(); //更改了来源重新加载音频元素
        await this.oAudio.play();
      }
      // if (oAudio.paused) {
      //   //暂停中,这里使用paused的初衷是判断audio是否播放完毕，上一段播放完毕了再继续播放，达到一个排队播放的效果，paused可以替换成更好的属性
      //   oAudio.src = this.CallList[0].fileName;
      //   oAudio.load(); //更改了来源重新加载音频元素
      //   oAudio.play();
      // }
      //  oAudio.onended = () => {
      this.oAudio.onended = () => {
        //音频播放完成触发
        console.log("OnendedoAudio.paused", this.oAudio.paused);
        //播放完毕，重新循环播放
        // console.log("播放完毕");
        if (this.Calli < this.ScreensInput.CallNum - 1) {
          this.oAudio.load();
          this.oAudio.play();
        }
        // if (this.Calli < this.ScreensInput.CallNum - 1) {
        //   oAudio.load();
        //   oAudio.play();
        // }
        if (this.Calli >= this.ScreensInput.CallNum - 1) {
          this.Subtitle = null;
          this.Calli = -1;
          // console.log("清理呼叫列表");
          this.CallList.shift(); //音频播放完毕，删除数组第一个
          console.log("CallList", this.CallList);
          if (this.CallList.length != 0) {
            // console.log("继续播放队列");
            this.CallListSpeak();
          }
        }
        this.Calli++;
        // this.oAudio.pause();//有偶发状态音频未更改到暂停状态就需要播放所以音频结束手动暂停音频
      };
    },
    //获取屏幕关联诊室信息，庐山市中医医院
    GetDepartmentInput() {
      var Prams = {
        PageCount: 1,
        Index: 1,
        Page: 1,
        state: 0,
        ScreensID: this.ScreensInput.ID,
      };
      this.$axios
        .post("/api/Department/GetDepartmentInput", Prams)
        .then((res) => {
          // console.log("GetDepartmentInput", res);
          this.DepartmentInput = res.data.data.Data[0];
          // console.log("DepartmentInput", this.DepartmentInput);
        })
        .catch((err) => {
          console.error(err);
        });
    },
  },
  mounted() {
    // this.speechInit();

    this.$axios
      .post("api/CallMsg/GetCallMsgSingle", {
        DeptID: this.CallPeople.DeptId,
      })
      .then((response) => {
        if (response.data.data != null) {
          if (response.data.data.State == 1) {
            // console.log("呼叫", response);
            this.CallSpeak(
              response.data.data.callListOutput,
              response.data.data
            );
          }
        }
      })
      .catch((err) => {
        console.error(err);
      });
  },
  watch: {
    ScreensInput() {
      // this.speech.setRate(this.ScreensInput.Rate + 1);
      console.log(this.ScreensInput.IsAudio != "1");
      if (this.ScreensInput.IsAudio != "1") {
        //是否可以播放
        this.oAudio = this.$refs.audio;
        this.$proxy.on("addMessageToList", (msg) => {
          var jsonmsg = JSON.parse(msg);
          if (jsonmsg.code == this.ScreensInput.CallCode) {
            this.CallList.push(jsonmsg);
            this.CallListSpeak();
            this.GetMetData();
          }

          // this.GetWaitingStaff();
          // this.GetPastStaff();
        });
      }
      this.GetMetData();
      this.GetDepartmentInput();
      if (this.ScreensInput.DataMode == 1) {
        let DeptInfos = (this.ScreensInput.roomInfo || this.ScreensInput.RoomInfo || this.ScreensInput.deptInfo || this.ScreensInput.DeptInfo || "").split(";"); //第一次分割
        for (let index = 0; index < DeptInfos.length; index++) {
          let DeptInfos2 = DeptInfos[index].split("|");
          let jiaohao = { Code: DeptInfos2[0], DeptName: DeptInfos2[1] };
          this.jiaohaostring.push(jiaohao);
        }
        this.jiaohao = this.jiaohaostring[0];
        // console.log("jiaohaostring", this.jiaohaostring);
      }
      // window.setInterval(() => {
      //   this.GetMetData();
      //   // this.GetDepartmentInput();
      // }, this.ScreensInput.ScrollTime * 1000);
      window.setInterval(() => {
        if (this.WaitingStaff.PageCount <= 1) {
          this.GetMetData();
        }
      }, this.ScreensInput.ScrollTime * 1000);
    },
    MetData() {
      this.GetWaitingStaff(this.WaitingStaff.Index - 1);
      this.GetPastStaff(this.PastStaff.Index - 1);
      // this.GetDepartmentInput(this.Department.Index - 1);
    },
  },
};
</script>

<style lang="scss" scoped>
#Main {
  width: 100%;
  height: 100%;
}
/*
Main中的四边图片
 */
#Main .ImgUpLeft {
  position: absolute;
  top: 0%;
  left: 0%;
}
#Main .ImgUpRight {
  position: absolute;
  top: 0%;
  right: 0%;
}
#Main .ImgLoLeft {
  position: absolute;
  bottom: -1%;
  left: 0%;
}
#Main .ImgLoRight {
  position: absolute;
  bottom: -1%;
  right: 0%;
}
/*Main单独样式 */
#Main #Right {
  // min-height: 500px;
  // max-height: 648px;
  // height: 100%;
  height: 77%;
}
#Main #Left {
  height: 100%;
  // height:22%;
}
#Main #MainTop {
  height: 100%;
}
#Main #MainBottom {
  height: 10%;
  display: flex;
  background-color: rgba(0, 0, 0, 0.2);
}
#Main #Left #Callout,
#Main #Left #CallNumber {
  display: flex;
  flex-direction: column;
}
#Main #Left #Callout {
  // height: 38%;
  height: 100%;
}
#Main #Left #CallNumber {
  margin-top: 2%;
  height: 61.7%;
}
#Main #Right #AwaitPerson {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
}

#Main #Left #Callout .Bottom #CallDiv {
  display: flex;
  flex-direction: column;
  justify-content: center;
  line-height: 2.8em;
  width: 100%;
  .tr {
    background-color: white;
    text-align: center;
    width: 100%;
    border-radius: 5px;
    height: 90px;
    margin-bottom: 10px;
  }
  .WaitingTitleRow {
    font-size: 40px;
    border-radius: 5px;
    background-color: #29afa8;
    text-align: center;
    color: white;
    letter-spacing: 50px;
    height: 16%;
    .WaitingTitleCol {
      height: 100%;
      display: flex;
      flex-direction: column;
      justify-content: center;
    }
  }
}

#Main #Left #Callout .Bottom #CallDiv #CallWindow {
  display: flex;
  justify-content: center;
  color: rgb(255, 255, 255);
  font-size: 1.8em;
}
#Main #Left #Callout .Bottom #CallDiv #CallName {
  // display: flex;
  // justify-content: center;
  color: rgb(255, 255, 0);
  // font-size: 3em;
  font-size: 24px;
  .outpatcol {
    background: #ff6d6d;
    color: white;
    border-radius: 5px 0px 0px 5px;
    height: 90px;
    font-size: 42px;
    line-height: 90px;
  }
  .Waitingtr {
    text-align: center;
    width: 100%;
    margin-top: 12px;
    border-radius: 5px;
    height: 16%;
  }
  .waitingxh {
    background: #29afa8;
    color: white;
    border-radius: 5px 0px 0px 5px;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 38px;
  }
  .waitingPatientName {
    color: #000000;
    background: white;
    border-radius: 0px 5px 5px 0px;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 38px;
    // font-size:18px;
  }
  .outpatPatientName {
    color: #ff6d6d;
    font-size: 50px;
    height: 90px;
    line-height: 90px;
  }
}
/*Main内部共用样式 */
.Top {
  padding: 2px;
  border-radius: 10px 10px 0px 0px;
  background-color: rgb(106, 73, 39);
  margin-top: 10px;
  width: 99%;
  height: 2.4em;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.Top .TopTitle {
  margin-left: 30px;
  font-size: 2.3em;
  letter-spacing: 5px;
  color: rgb(255, 255, 0);
}
.Bottom {
  position: relative;
  width: 99%;
  // background-color: rgb(147, 105, 63);
  // border: 2px solid rgb(64, 27, 0);
}
.LeftBottom {
  width: 100%;

  // height: 100%;
  display: flex;
  justify-content: center;
}
.RightBottom {
  height: 96%;
  // height:100%;
  background-color: rgb(1, 115, 175);
  width: 100%;
  display: flex;
  justify-content: center;
}
.wu {
  color: rgb(255, 255, 255);
  display: flex;
  align-items: center;
  font-size: 4em;
}
.Await {
  width: 100%;
  height: 100%;
}
.AwaitList {
  text-align: center;
  margin: 0.5em auto;
  color: rgb(255, 255, 255);
  // font-size: 2.6em;
  font-size: 24px;
}
.el-carousel__container {
  height: 100%;
}
.Subtitle {
  color: red;
  font-size: 2em;
}
.ShowSubtitle {
  text-align: center;
}
</style>