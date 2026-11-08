<template>
  <div id="Main" :class="screenLayoutClass">
    <el-row id="MainTop" :gutter="6">
      <!-- 于都 -->
      <el-col id="Left" :xs="13" :sm="13" :md="13" :lg="13" :xl="13">
        <!-- 兴国 -->
        <!-- <el-col id="Left" :xs="15" :sm="15" :md="15" :lg="15" :xl="15"> -->
        <!-- 正在呼叫 -->
        <div id="Callout">
          <div class="Top">
            <h4 class="TopTitle">正在呼叫</h4>
          </div>
          <div class="Bottom LeftBottom" style="height: 60p%">
            <!-- 上方图片 -->
            <div class="ImgUpLeft">
              <img src="/Image/MainImg/head1.png" />
            </div>
            <div class="ImgUpRight">
              <img src="/Image/MainImg/head3.png" />
            </div>
            <!-- 下方图片 -->
            <div class="ImgLoLeft">
              <img src="/Image/MainImg/head2.png" />
            </div>
            <div class="ImgLoRight">
              <img src="/Image/MainImg/head4.png" />
            </div>
            <div id="CallDiv">
              <!-- <swiper
                ref="mySwiper"
                class="swiper"
                style="height: 100%; width: 100%"
                :options="swiperOption"
                @slide-change-transition-start="GetDepartmentInput"
              >
                <swiper-slide
                  v-for="(item, index) in Department.PageCount"
                  :key="index"
                >
                  <div id="CallName" cellpadding="0" cellspacing="0">
                    <el-row
                      class="tr"
                      v-for="(item, Index) in Department.data"
                      :key="item.ID"
                    >
                      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
                        {{ item.Name }}
                      </el-col>
                      <el-col :xs="0" :sm="6" :md="6" :lg="6" :xl="6">
                        {{ item.xh }}
                      </el-col>
                      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
                        {{ item.PatientName }}
                      </el-col>
                      <el-col
                        :xs="0"
                        :sm="6"
                        :md="6"
                        :lg="6"
                        :xl="6"
                        class="hidden-sm-and-down"
                      >
                        {{ item.Doctors }}
                      </el-col>
                    </el-row>
                  </div>
                </swiper-slide>
              </swiper> -->
              <el-carousel
                direction="vertical"
                height="100%"
                style="height: 100%"
                :loop="true"
                :interval="ScreensInput.ScrollTime * 1000"
                arrow="never"
                ref="carousel"
                autoplay
                @change="GetDepartmentInput"
                indicator-position="none"
              >
                <el-carousel-item
                  v-for="(item, index) in Department.PageCount"
                  :key="index"
                  class="autoplayHandler"
                >
                  <div id="CallName" cellpadding="0" cellspacing="0">
                    <!--  style="border-spacing: 0em 1.2em" -->
                    <el-row class="tr">
                      <el-col :xs="12" :sm="8" :md="8" :lg="8" :xl="8"
                        >诊室</el-col
                      >
                      <el-col :xs="0" :sm="8" :md="8" :lg="8" :xl="8"
                        >序号</el-col
                      >
                      <el-col :xs="12" :sm="8" :md="8" :lg="8" :xl="8"
                        >患者</el-col
                      >

                      <!-- <el-col
                        :xs="0"
                        :sm="6"
                        :md="6"
                        :lg="6"
                        :xl="6"
                        class="hidden-sm-and-down"
                        >医生</el-col
                      > -->
                    </el-row>
                    <el-row
                      class="tr"
                      v-for="(item, Index) in Department.data"
                      :key="item.ID"
                    >
                      <el-col :xs="12" :sm="8" :md="8" :lg="8" :xl="8">
                        {{ item.Name }}
                      </el-col>
                      <el-col
                        class="xm"
                        :xs="0"
                        :sm="8"
                        :md="8"
                        :lg="8"
                        :xl="8"
                      >
                        {{ item.xh }}
                      </el-col>
                      <el-col
                        class="xm PatientName"
                        :xs="12"
                        :sm="8"
                        :md="8"
                        :lg="8"
                        :xl="8"
                      >
                        {{ item.PatientName }}
                      </el-col>
                      <!-- <el-col
                        :xs="0"
                        :sm="6"
                        :md="6"
                        :lg="6"
                        :xl="6"
                        class="hidden-sm-and-down xm"
                      >
                        {{ item.Doctors }}
                      </el-col> -->
                    </el-row>
                  </div>
                </el-carousel-item>
              </el-carousel>
            </div>
          </div>
        </div>
        <!-- 过号人员 -->
        <div id="CallNumber">
          <div class="Top">
            <h4 class="TopTitle">已捡药</h4>
          </div>
          <div class="Bottom LeftBottom" style="height: 100p%">
            <!-- 上方图片 -->
            <div class="ImgUpLeft">
              <img src="/Image/MainImg/head1.png" />
            </div>
            <div class="ImgUpRight">
              <img src="/Image/MainImg/head3.png" />
            </div>
            <!-- 过号人员-->
            <el-row v-if="IsPastStaff" style="width: 100%; height: 100%">
              <div style="margin-top: 10px; width: 100%; height: 100%">
                <!-- <swiper
                  ref="mySwiper"
                  class="swiper1"
                  style="height: 80%; width: 100%"
                  :options="swiperOption"
                  @slide-change-transition-start="GetPastStaff"
                >
                  <swiper-slide
                    v-for="(item, index) in PastStaff.PageCount"
                    :key="index"
                  >
                    <el-col
                      v-for="(item, index) in PastStaff.Data"
                      :key="item.id"
                      class="AwaitList"
                      :xs="8"
                      :sm="8"
                      :md="8"
                      :lg="8"
                      :xl="8"
                    >
                      <span>
                        {{ item.PatientName }}
                        ({{ item.OutPatientOffice }})
                      </span>
                    </el-col>
                  </swiper-slide>
                </swiper> -->

                <el-carousel
                  id="PastStaff"
                  direction="vertical"
                  height="100%"
                  style="height: 80%"
                  :loop="true"
                  :interval="ScreensInput.ScrollTime * 1000"
                  arrow="never"
                  ref="carousel2"
                  autoplay
                  @change="GetPastStaff"
                  indicator-position="none"
                >
                  <el-carousel-item
                    v-for="(item, index) in PastStaff.PageCount"
                    :key="index"
                    class="autoplayHandler"
                  >
                    <el-col
                      v-for="(item, index) in PastStaff.Data"
                      :key="item.id"
                      class="PastStaffList"
                      :xs="8"
                      :sm="8"
                      :md="8"
                      :lg="8"
                      :xl="8"
                    >
                      <span>
                        {{ item.PatientName }}({{
                          item.OutPatientOffice
                        }})</span
                      >
                    </el-col>
                  </el-carousel-item>
                </el-carousel>
              </div>
              <!-- </transition> -->
            </el-row>
            <!-- 无过号人员 -->
            <div v-if="!IsPastStaff" style="display: flex">
              <h1 class="wu">暂无</h1>
              <!-- <el-empty description="描述文字"></el-empty> -->
            </div>
            <!-- 下方图片 -->
            <div class="ImgLoLeft">
              <img src="/Image/MainImg/head2.png" />
            </div>
            <div class="ImgLoRight">
              <img src="/Image/MainImg/head4.png" />
            </div>
          </div>
        </div>
      </el-col>
      <!-- 于都 -->
      <el-col id="Right" :xs="11" :sm="11" :md="11" :lg="11" :xl="11">
        <!-- 兴国 -->
        <!-- <el-col id="Right" :xs="9" :sm="9" :md="9" :lg="9" :xl="9"> -->
        <!-- 等待人员 -->
        <div id="AwaitPerson">
          <div class="Top">
            <h4 class="TopTitle">
              <span>待捡药</span>
              <span style="float: right"
                >{{ jiaohao.DeptName }}({{ WaitingStaff.Totle }})</span
              >
            </h4>
          </div>
          <div class="Bottom RightBottom">
            <!-- 上方图片 -->
            <div class="ImgUpLeft">
              <img src="/Image/MainImg/head1.png" />
            </div>
            <div class="ImgUpRight">
              <img src="/Image/MainImg/head3.png" />
            </div>
            <!-- 等待人员内容 -->
            <el-row v-if="IsWaitingStaff" class="Await">
              <!-- <transition
              
                name="animate__animated animate__bounce"
                enter-active-class="animate__backInDown"
                leave-active-class="animate__backOutUp"
              > -->
              <h1 style="color: yellow">
                <el-row>
                  <el-col :xs="8" :sm="8" :md="8" :lg="8" :xl="8"
                    >【A-急诊</el-col
                  >
                  <el-col :xs="8" :sm="8" :md="8" :lg="8" :xl="8"
                    >B-优先</el-col
                  >
                  <el-col :xs="8" :sm="8" :md="8" :lg="8" :xl="8"
                    >C-普通门诊】</el-col
                  >
                </el-row>
              </h1>

              <div style="width: 100%; height: 100%">
                <el-carousel
                  direction="vertical"
                  height="100%"
                  style="height: 87%"
                  :loop="true"
                  :interval="ScreensInput.ScrollTime * 1000"
                  arrow="never"
                  ref="carousel3"
                  autoplay
                  @change="GetWaitingStaff"
                  indicator-position="none"
                >
                  <el-carousel-item
                    class="autoplayHandler"
                    v-for="item in WaitingStaff.PageCount"
                    :key="item"
                  >
                    <el-col
                      v-for="(item, index) in WaitingStaff.Data"
                      :key="item.id"
                      class="AwaitList"
                      :xs="12"
                      :sm="12"
                      :md="12"
                      :lg="12"
                      :xl="12"
                    >
                      <div style="">
                        <span v-if="ScreensInput.IsNum != 1">
                          {{ item.xhlb }}{{ item.xh }}
                        </span>
                        {{ item.PatientName }}
                      </div>
                    </el-col>
                  </el-carousel-item>
                </el-carousel>
              </div>
              <!-- </transition> -->
            </el-row>
            <!-- 无患者 -->
            <div v-if="!IsWaitingStaff" style="display: flex">
              <h1 class="wu">暂无患者</h1>
            </div>
            <!-- 下方图片 -->
            <div class="ImgLoLeft">
              <img src="/Image/MainImg/head2.png" />
            </div>
            <div class="ImgLoRight">
              <img src="/Image/MainImg/head4.png" />
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
    <!-- 提示栏 -->
    <el-row v-if="showMarqueeBar" id="MainBottom">
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
    </el-row>
    <!-- 语音播报测试 -->
    <audio
      class="audio"
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
// import { Swiper, SwiperSlide, directive } from "vue-awesome-swiper";
// import "swiper/css/swiper.css";
export default {
  name: "Main",
  mixins: [screenDisplayMixin],
  // components: { Swiper, SwiperSlide },
  // directives: { swiper: directive },
  data() {
    return {
      // swiperOption: {
      //   loop: true,
      //   // autoplay: true,
      //   speed: 1000,
      //   direction: "vertical",
      //   autoplay: {
      //     delay: 10000,
      //     disableOnInteraction: false, // 手动滑动后继续自动播放
      //   },
      // },
      Subtitle: null,
      audiosrc: "",
      speech: null,
      MetData: [], //队列元数据，包含过号人员，等待人员，正在呼叫人员
      IsWaitingStaff: true, //是否有等待人员
      WaitingStaff: {
        state: 0,
        Totle: 0,
        Page: 10,
        Index: 1,
        Data: [
          // { id: 1, xhlb: "C", xh: "1", PatientName: "张三" },
          // { id: 2, xhlb: "C", xh: "2", PatientName: "张三" },
          // { id: 3, xhlb: "C", xh: "3", PatientName: "张三" },
          // { id: 4, xhlb: "C", xh: "4", PatientName: "张三" },
          // { id: 5, xhlb: "C", xh: "5", PatientName: "张三" },
          // { id: 6, xhlb: "C", xh: "6", PatientName: "张三" },
          // { id: 7, xhlb: "C", xh: "7", PatientName: "张三" },
          // { id: 8, xhlb: "C", xh: "8", PatientName: "张三" },
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
        Data: [], //过号人员列表
      },
      // Callmsg: [], //语音播报队列
      // audio: true, //默认语音播报队列中无数据
      Department: {
        state: 1,
        Totle: 0,
        Page: 3,
        Index: 1,
        PageCount: 0,
        data: [
          // {
          //   Name: "诊室",
          //   xh: "序号",
          //   PatientName: "姓名",
          //   DeptID: "0",
          //   Doctors: "医生",
          //   id: 0,
          //   state: "",
          // },
        ],
      }, //诊室数组
      // MedicalPersonnels: [], //正在就诊人员
      jiaohaostring: [],
      jiaohao: { Code: null, DeptName: null },
      jiaohaoint: 0,
      CallList: [],
      Calli: 0,
      oAudio:null,
    };
  },
  props: ["ScreensInput"],
  methods: {
    GetMetData() {
      //异步获取元数据
      this.$axios
        .post("/api/CallList/GetCallListInput", { ID: this.ScreensInput.ID })
        .then((res) => {
          // console.log("获取元数据完成");
          // console.log(res.data);
          this.MetData = res.data.data;
        })

        .catch((err) => {
          console.error(err);
        });
    },
    GetWaitingStaff(e, a) {
      // console.log("waitingstaffA", a);
      if (this.jiaohaostring.length != 0) {
        if (
          (this.WaitingStaff.Index >= this.WaitingStaff.PageCount ||
            this.WaitingStaff.PageCount == 0) &&
          a == undefined
        ) {
          if (this.jiaohaoint + 1 == this.jiaohaostring.length) {
            this.jiaohaoint = 0;
          } else {
            this.jiaohaoint++;
          }
          this.jiaohao = this.jiaohaostring[this.jiaohaoint];
          this.WaitingStaff.Index = 0;
          //更换诊室，当等待就诊的页数等于总页数就相当于到了最后一页，就更换诊室
        }
      }

      this.WaitingStaff.Index = e == undefined ? 1 : e + 1; //设置等待人员列表显示页，轮播图会传参进来
      // console.log("this.WaitingStaff.Index", this.WaitingStaff.Index);
      //异步获取正在排队数据
      console.log(
        "a:" +
          JSON.stringify({
            index: this.WaitingStaff.Index,
            page: this.WaitingStaff.Page,
            state: this.WaitingStaff.state,
            ScreensID: this.ScreensInput.ID,
            Code: this.jiaohao.Code,
          })
      );
      this.$axios
        .post("/api/CallList/GetCallListInputPage", {
          index: this.WaitingStaff.Index,
          page: this.WaitingStaff.Page,
          state: this.WaitingStaff.state,
          ScreensID: this.ScreensInput.ID,
          Code: this.jiaohao.Code,
        })
        .then((res) => {
          // console.log("WaitingStaff传输过来的数据", res.data);
          this.WaitingStaff.Data = res.data.data.Data; //获取等待数据
          this.WaitingStaff.Totle = res.data.data.Totle; //获取等待状态总人数
          this.WaitingStaff.Index = res.data.data.Index; //获取第几页
          this.WaitingStaff.PageCount = res.data.data.PageCount; //获取总页数
          this.IsWaitingStaff = this.WaitingStaff.Totle > 0; //判断是否有人再等待
          // console.log("WaitingStaff接收后的数据", this.WaitingStaff);
          // if (this.WaitingStaff.PageCount <= 1) {
          //   window.setTimeout(() => {
          //     console.log("进入延迟");
          //     this.GetMetData();
          //   }, this.ScreensInput.ScrollTime * 1000);
          // }
        })
        .catch((err) => {
          console.error(err);
        });
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
          Code: this.jiaohao.Code,
        })
        .then((res) => {
          // console.log("PastStaff传输过来的", res);
          this.PastStaff.Data = res.data.data.Data;
          this.PastStaff.Totle = res.data.data.Totle;
          this.PastStaff.PageCount = res.data.data.PageCount;

          this.IsPastStaff = this.PastStaff.Totle > 0;
          // console.log("PastStaff接收后的数据", this.PastStaff);
        })
        .catch((err) => {
          console.error(err);
        });
    },
    //呼叫列表
    GetCallPeople(DepartmentID) {
      var Call = this.MetData.filter(
        (a) => a.state == 1 && a.DeptId == DepartmentID
      );
      return Call;
    },
    //初始化语音播报组件
    speechInit() {
      this.speech = new Speech();
      this.speech.setLanguage("zh-CN");
      this.speech.setRate(10);
      this.speech.init().then(() => {
        // console.log("语音播报初始化完成...");
      });
      this.speech.speak({
        text: "",
      });
    },
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
    // CallListSpeak() {
    //   this.Subtitle = this.CallList[0].showMsg;
    //   // this.audiosrc = this.CallList[0].fileName;

    //   let oAudio = this.$refs.audio;

    //   if (oAudio.paused) {
    //     //暂停中
    //     oAudio.src = this.CallList[0].fileName;
    //     oAudio.play();
    //   }

    //   oAudio.onended = () => {
    //     //播放完毕，重新循环播放
    //     // console.log("播放完毕");

    //     if (this.Calli < this.ScreensInput.CallNum - 1) {
    //       oAudio.load();
    //       oAudio.play();
    //     }
    //     if (this.Calli >= this.ScreensInput.CallNum - 1) {
    //       this.Subtitle = null;
    //       this.Calli = -1;
    //       // console.log("清理呼叫列表");
    //       this.CallList.shift(); //音频播放完毕，删除
    //       if (this.CallList.length != 0) {
    //         // console.log("继续播放队列");
    //         this.CallListSpeak();
    //       }
    //     }
    //     this.Calli++;
    //   };
    // },
    async CallListSpeak() {
      this.Subtitle = this.CallList[0].showMsg;
      // this.audiosrc = this.CallList[0].fileName;
      // let oAudio = this.$refs.audio;
      console.log("oAudio.paused", this.oAudio.paused);
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
    GetDepartmentInput(e) {
      this.Department.Index = e == undefined ? 1 : e + 1; //获取轮播图第几页
      this.$axios
        .post("/api/Department/GetDepartmentInput", {
          ScreensID: this.ScreensInput.ID,
          Index: this.Department.Index,
          Page: this.Department.Page,
        })
        .then((res) => {
          this.Department.data = []; //初始话正在就诊数组
          // console.log("dept", res);
          this.Department.Totle = res.data.data.Totle; //获取正在就诊总人数
          this.Department.PageCount = res.data.data.PageCount; //获取正在就诊总页数
          for (var i = 0; i < this.Department.Page; i++) {
            //判断显示人数
            var CallPeople = this.GetCallPeople(
              res.data.data.Data[i] == undefined ? "" : res.data.data.Data[i].ID
            )[0]; //根据诊室ID获取在该诊室就诊的人
            this.Department.data.push({
              Name:
                res.data.data.Data[i] != undefined
                  ? res.data.data.Data[i].Name
                  : "",
              xh:
                CallPeople != undefined && CallPeople.xh != null
                  ? CallPeople.xh
                  : "",
              DeptID:
                CallPeople != undefined && CallPeople.DeptId != null
                  ? CallPeople.DeptId
                  : "",
              PatientName:
                CallPeople != undefined && CallPeople.PatientName != null
                  ? CallPeople.PatientName
                  : "",
              Doctors:
                CallPeople != undefined && CallPeople.Doctors != null
                  ? CallPeople.Doctors
                  : "",
              id:
                CallPeople != undefined && CallPeople.id != null
                  ? CallPeople.id
                  : 0,
              state:
                CallPeople != undefined && CallPeople.state != null
                  ? CallPeople.state
                  : 4,
            });
          }
          for (var i = 0; i < this.Department.data.length; i++) {
            this.$axios
              .post("api/CallMsg/GetCallMsgSingle", {
                DeptID: this.Department.data[i].DeptID,
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
          }
        })
        .catch((err) => {
          console.error(err);
        });
    },
  },
  mounted() {
      this.oAudio = this.$refs.audio;
    this.$proxy.on("addMessageToList", (msg) => {
      var jsonmsg = JSON.parse(msg);
      if (jsonmsg.code == this.ScreensInput.CallCode) {
        this.CallList.push(jsonmsg);
        this.CallListSpeak();
        this.GetWaitingStaff(this.WaitingStaff.Index - 1, "呼叫触发刷新");
        this.GetMetData();
      }
    });
  },
  watch: {
    async ScreensInput() {
      if (this.ScreensInput.DataMode == 1) {
        let DeptInfos = (this.ScreensInput.roomInfo || this.ScreensInput.RoomInfo || this.ScreensInput.deptInfo || this.ScreensInput.DeptInfo || "").split(";"); //第一次分割
        for (let index = 0; index < DeptInfos.length; index++) {
          let DeptInfos2 = DeptInfos[index].split("|");
          let jiaohao = { Code: DeptInfos2[0], DeptName: DeptInfos2[1] };
          this.jiaohaostring.push(jiaohao);
        }
        this.jiaohao = this.jiaohaostring[0];
      }
      this.GetWaitingStaff(this.WaitingStaff.Index - 1);
      this.GetMetData();

      // window.setInterval(() => {
      //   // this.GetWaitingStaff(this.WaitingStaff.Index - 1);
      //   // this.GetPastStaff(this.PastStaff.Index - 1);
      //   // this.GetDepartmentInput();
      //   this.GetMetData();
      // }, this.ScreensInput.ScrollTime * 1000);
      window.setInterval(() => {
        if (this.WaitingStaff.PageCount <= 1) {
          this.GetWaitingStaff(this.WaitingStaff.Index - 1);
          this.GetMetData();
        }
      }, this.ScreensInput.ScrollTime * 1000);
    },
    MetData(a) {
      this.GetPastStaff(this.PastStaff.Index - 1);
      this.GetDepartmentInput(this.Department.Index - 1);
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
  height: 100%;
}
#Main #Left {
  height: 100%;
}
#Main #MainTop {
  height: 82%;
}
#Main #MainBottom {
  height: 8%;
  display: flex;
  background-color: rgba(0, 0, 0, 0.2);
}
#Main #Left #Callout,
#Main #Left #CallNumber {
  display: flex;
  flex-direction: column;
}
#Main #Left #Callout {
  height: 60%;
}
#Main #Left #CallNumber {
  height: 40%;
}
#Main #Right #AwaitPerson {
  display: flex;
  flex-direction: column;
  height: 100%;
}

#Main #Left #Callout .Bottom #CallDiv {
  display: flex;
  flex-direction: column;
  justify-content: center;
  line-height: 2em;
  width: 80%;
  height: 100%;
}
#Main #Left #Callout .Bottom #CallDiv #CallName {
  text-align: left;
  color: rgb(255, 255, 0);
  width: 100%;
  text-align: center;
  height: 100%;
}
#Main #Left #Callout .Bottom #CallDiv #CallName .tr {
  margin-top: 0.5em;
  border-radius: 8px;
  width: 100%;
  background-color: rgb(124, 88, 50);
  font-size: 2.5em;
  display: flex;
  align-items: center;
  height: 18%;
  //
}
#Main #Left #Callout .Bottom #CallDiv #CallName tr td:first-child {
  border-bottom-left-radius: 12px;
  border-top-left-radius: 12px;
}

#Main #Left #Callout .Bottom #CallDiv #CallName tr td:last-child {
  border-bottom-right-radius: 12px;
  border-top-right-radius: 12px;
}

/*Main内部共用样式 */
.Top {
  padding: 2px;
  border-radius: 10px 10px 0px 0px;
  background-color: rgb(106, 73, 39);
  margin-top: 10px;
  width: 99%;
  height: 2em;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.Top .TopTitle {
  margin-left: 10px;
  letter-spacing: 5px;
  color: rgb(255, 255, 0);
  font-size: 2em;
}
.Bottom {
  position: relative;
  width: 99%;
  background-color: rgb(147, 105, 63);
  border: 2px solid rgb(64, 27, 0);
}
.LeftBottom {
  height: 85%;
  display: flex;
  justify-content: center;
}
.RightBottom {
  height: 96%;
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
  text-align: center;
  width: 100%;
  height: 100%;
}

.PastStaffList {
  text-align: center;
  margin: 0.5em auto;
  color: rgb(255, 255, 0);
  font-size: 2.3em;
  font-weight: bold;
  //
}
.AwaitList {
  text-align: center;
  margin: 0.5em auto;
  color: rgb(255, 255, 255);
  font-size: 2.8em;
  font-weight: bold;
}
// 于都（名字加大字体）
.PatientName {
  font-size: 1.24em;
}

.xm {
  font-weight: bold;
  // color: rgb(255, 0, 0);
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