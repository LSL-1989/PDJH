<template>
  <div id="Main" :class="screenLayoutClass">
    <div class="content-wrapper">
      <!-- Left Panel: Current Call & Past -->
      <div class="left-panel">
        
        <!-- Current Call Card (Multiple Rooms) -->
        <div class="current-call-card elevation-4">
          <div class="card-header">
            <span class="material-icon"></span>
            <h3>正在{{ actionLabel }} CURRENT CALL</h3>
          </div>
          <div class="card-body call-pulse" style="padding: 0;">
            <div class="carousel-container" style="width: 100%; height: 100%;">
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
                  <div class="table-container">
                    <div class="focus-row" v-for="item in Department.data" :key="item.id">
                      <div class="focus-room">{{ item.Name }}</div>
                      
                      <template v-if="item.PatientName">
                        <div class="focus-patient">
                          <span v-if="showField('QUEUE_NO')" class="focus-seq">{{ item.xh }}</span>
                          <span v-if="showField('PATIENT_NAME')" class="focus-name">{{ formatPatientName(item.PatientName) }}</span>
                        </div>
                        <div class="do-not-disturb-badge">
                          <div class="badge-pulse"></div>
                          <span>正在就诊中，请尊重隐私勿打扰</span>
                        </div>
                      </template>
                      <template v-else>
                        <div class="focus-empty">
                          <span class="empty-text">诊室空闲 - 请等待呼叫</span>
                        </div>
                      </template>
                      
                    </div>
                  </div>
                </el-carousel-item>
              </el-carousel>
            </div>
          </div>
        </div>

        <!-- Past / Finished Staff -->
        <div class="past-call-card elevation-2">
          <div class="card-header border-bottom">
            <h3>过号患者 PAST</h3>
          </div>
          <div class="card-body">
            <div v-if="IsPastStaff" class="carousel-container">
              <el-carousel
                id="PastStaff"
                direction="vertical"
                height="100%"
                style="height: 100%"
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
                  <div class="grid-list">
                    <div class="grid-item past-item" v-for="item in PastStaff.Data" :key="item.id">
                       {{ formatPatientName(item.PatientName) }}
                       <span class="dept-mark">({{ item.OutPatientOffice }})</span>
                    </div>
                  </div>
                </el-carousel-item>
              </el-carousel>
            </div>
            <div v-else class="empty-state">暂无过号</div>
          </div>
        </div>

      </div>

      <!-- Right Panel: Waiting Queue -->
      <div class="right-panel">
        <div class="waiting-queue-card elevation-3">
          <div class="card-header justify-between border-bottom">
            <h3>等待{{ actionLabel }} WAITING</h3>
            <div class="badge">{{ jiaohao ? jiaohao.DeptName : '' }} (共 {{ WaitingStaff.Totle }} 人)</div>
          </div>
          <div class="card-body">
            <div v-if="IsWaitingStaff" class="carousel-container">
              <el-carousel
                direction="vertical"
                height="100%"
                style="height: 100%"
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
                  <div class="list-layout">
                    <div class="list-item" v-for="item in WaitingStaff.Data" :key="item.id"
                         :class="getSourceClass(item.sourceType)">
                      <!-- P1-2d: 来源角标 -->
                      <span class="source-tag" v-if="item.sourceType && item.sourceType !== 'CLINIC'"
                            :class="'src-' + item.sourceType">
                        {{ sourceLabel(item.sourceType) }}
                      </span>
                      <div class="seq-num" v-if="showField('QUEUE_NO') && ScreensInput.IsNum != 1">{{ item.xhlb }}{{ item.xh }}</div>
                      <div v-if="showField('PATIENT_NAME')" class="pat-name">{{ formatPatientName(item.PatientName) }}</div>
                    </div>
                  </div>
                </el-carousel-item>
              </el-carousel>
            </div>
            <div v-else class="empty-state">暂无排队等待患者</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bottom Announcement Bar -->
    <div v-if="showMarqueeBar" class="bottom-bar elevation-8">
      <div class="marquee-container" v-if="Subtitle == null">
        <div class="marquee-text">{{ screenMarqueeText }}</div>
      </div>
      <div class="action-notice" v-if="Subtitle != null">
        <span class="pulse-dot"></span> {{ Subtitle }}
      </div>
    </div>

    <!-- Audio Player -->
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
import Speech from "speak-tts";
import { parseScreenBindingList } from "@/utils/screenBinding";
import {
  shouldSpeakOnThisScreen,
  speakCallText,
  resolveCallTimes,
  resolveSpeakRate
} from "@/utils/screenVoice";
import screenDisplayMixin from "@/mixins/screenDisplayMixin";
export default {
  name: "Main",
  mixins: [screenDisplayMixin],
  data() {
    return {
      Subtitle: null,
      audiosrc: "",
      speech: null,
      MetData: [],
      IsWaitingStaff: true,
      WaitingStaff: {
        state: 0,
        Totle: 0,
        Page: 7,
        Index: 1,
        Data: [],
        PageCount: 0,
      },
      IsPastStaff: false,
      PastStaff: {
        state: 3,
        Totle: 0,
        Page: 6,
        Index: 1,
        PageCount: 0,
        Data: [],
      },
      Department: {
        state: 1,
        Totle: 0,
        Page: 1,
        Index: 1,
        PageCount: 0,
        data: [],
      },
      jiaohaostring: [],
      jiaohao: { Code: null, DeptName: null },
      jiaohaoint: 0,
      CallList: [],
      Calli: 0,
      oAudio: null,
    };
  },
  props: ["ScreensInput"],
  computed: {
    actionLabel() {
      return this.ScreensInput && this.ScreensInput.deptType === 2 ? '检查' : '就诊';
    }
  },
  methods: {
    GetMetData() {
      this.$axios
        .post("/api/CallList/GetCallListInput", { ID: this.ScreensInput.ID })
        .then((res) => {
          this.MetData = res.data.data;
        })
        .catch((err) => {
          console.error("GetCallListInputerr:" + err);
        });
    },
    GetWaitingStaff(e, a) {
      if (this.jiaohaostring && this.jiaohaostring.length != 0) {
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
        }
      }

      this.WaitingStaff.Index = e == undefined ? 1 : e + 1;
      this.$axios
        .post("/api/CallList/GetCallListInputPage", {
          index: this.WaitingStaff.Index,
          page: this.WaitingStaff.Page,
          state: this.WaitingStaff.state,
          ScreensID: this.ScreensInput.ID,
          Code: this.jiaohao ? this.jiaohao.Code : null,
        })
        .then((res) => {
          this.WaitingStaff.Data = res.data.data.Data;
          this.WaitingStaff.Totle = res.data.data.Totle;
          this.WaitingStaff.Index = res.data.data.Index;
          this.WaitingStaff.PageCount = res.data.data.PageCount;
          this.IsWaitingStaff = this.WaitingStaff.Totle > 0;
        })
        .catch((err) => {
          console.error("GetCallListInputPageWaitingStafferr:" + err);
        });
    },
    GetPastStaff(e) {
      this.PastStaff.Index = e == undefined ? 1 : e + 1;
      this.$axios
        .post("/api/CallList/GetCallListInputPage", {
          index: this.PastStaff.Index,
          page: this.PastStaff.Page,
          state: this.PastStaff.state,
          ScreensID: this.ScreensInput.ID,
          Code: this.jiaohao ? this.jiaohao.Code : null,
        })
        .then((res) => {
          this.PastStaff.Data = res.data.data.Data;
          this.PastStaff.Totle = res.data.data.Totle;
          this.PastStaff.PageCount = res.data.data.PageCount;
          this.IsPastStaff = this.PastStaff.Totle > 0;
        })
        .catch((err) => {
          console.error("GetCallListInputPagePastStafferr:" + err);
        });
    },
    GetCallPeople(DepartmentID) {
      var Call = this.MetData.filter(
        (a) => a.state == 1 && a.DeptId == DepartmentID
      );
      return Call;
    },
    speechInit() {
      this.speech = new Speech();
      this.speech.setLanguage("zh-CN");
      this.speech.setRate(this.ScreensInput);
      this.speech.init().then(() => {});
      this.speech.speak({ text: "" });
    },
    CallSpeak(data, CallInput) {
      if (CallInput != null || CallInput != undefined) {
        this.$axios
          .post("api/CallMsg/EditCallMsg", CallInput)
          .then((res) => {})
          .catch((err) => {
            console.error("EditCallMsgerr:" + err);
          });
      }
    },
    async CallListSpeak() {
      if (!this.screenVoiceEnabled()) return;
      const cur = this.CallList[0];
      if (!cur) return;
      this.Subtitle = cur.callText || cur.showMsg || "";
      if (!cur.fileName) {
        if (shouldSpeakOnThisScreen(cur, this.ScreensInput)) {
          const times = resolveCallTimes(this.ScreensInput);
          const rate = resolveSpeakRate(this.ScreensInput);
          speakCallText(this.Subtitle || cur.ttsCallText, times, rate);
        }
        const text = this.Subtitle;
        setTimeout(() => {
          if (this.Subtitle === text) this.Subtitle = null;
        }, 10000);
        this.Calli = -1;
        this.CallList.shift();
        if (this.CallList.length != 0) this.CallListSpeak();
        return;
      }
      if (this.oAudio.paused) {
        this.oAudio.src = cur.fileName;
        this.oAudio.load();
        await this.oAudio.play();
      }
      this.oAudio.onended = () => {
        if (this.Calli < this.ScreensInput.CallNum - 1) {
          this.oAudio.load();
          this.oAudio.play();
        }
        if (this.Calli >= this.ScreensInput.CallNum - 1) {
          this.Subtitle = null;
          this.Calli = -1;
          this.CallList.shift();
          if (this.CallList.length != 0) {
            this.CallListSpeak();
          }
        }
        this.Calli++;
      };
    },
    GetDepartmentInput(e) {
      this.Department.Index = e == undefined ? 1 : e + 1;
      this.$axios
        .post("/api/Department/GetDepartmentInput", {
          ScreensID: this.ScreensInput.ID,
          Index: this.Department.Index,
          Page: this.Department.Page,
        })
        .then((res) => {
          this.Department.data = [];
          this.Department.Totle = res.data.data.Totle;
          this.Department.PageCount = res.data.data.PageCount;
          for (var i = 0; i < this.Department.Page; i++) {
            var CallPeople = this.GetCallPeople(
              res.data.data.Data[i] == undefined ? "" : res.data.data.Data[i].ID
            )[0];
            this.Department.data.push({
              Name: res.data.data.Data[i] != undefined ? res.data.data.Data[i].Name : "",
              xh: CallPeople != undefined && CallPeople.xh != null ? CallPeople.xh : "",
              DeptID: CallPeople != undefined && CallPeople.DeptId != null ? CallPeople.DeptId : "",
              PatientName: CallPeople != undefined && CallPeople.PatientName != null ? CallPeople.PatientName : "",
              Doctors: CallPeople != undefined && CallPeople.Doctors != null ? CallPeople.Doctors : "",
              id: CallPeople != undefined && CallPeople.id != null ? CallPeople.id : 0,
              state: CallPeople != undefined && CallPeople.state != null ? CallPeople.state : 4,
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
                    this.CallSpeak(
                      response.data.data.callListOutput,
                      response.data.data
                    );
                  }
                }
              })
              .catch((err) => {
                console.error("GetCallMsgSingleerr:" + err);
              });
          }
        })
        .catch((err) => {
          console.error("GetDepartmentInputerr:" + err);
        });
    },
    getSourceClass(sourceType) {
      const map = {
        HEALTH_CHECKUP: 'item-checkup',
        OUTPATIENT_EXAM: 'item-medtech',
        PHARMACY: 'item-pharmacy',
        CLINIC: '',
      };
      return map[sourceType] || '';
    },
    sourceLabel(sourceType) {
      const map = {
        HEALTH_CHECKUP: '体检',
        OUTPATIENT_EXAM: '医技',
        PHARMACY: '药房',
      };
      return map[sourceType] || '';
    },
  },
  mounted() {
    this.oAudio = this.$refs.audio;
    this.$proxy.on("addMessageToList", (msg) => {
      var jsonmsg = typeof msg === "string" ? JSON.parse(msg) : msg;
      if (jsonmsg.type === "STATE_UPDATE") return;
      const code = String(jsonmsg.code || "").trim();
      const callCode = String(this.ScreensInput.CallCode || this.ScreensInput.callCode || "").trim();
      const screenId = String(this.ScreensInput.ID || this.ScreensInput.id || "").trim();
      const isCall =
        jsonmsg.type === "CALL_MSG" ||
        !!(jsonmsg.callText || jsonmsg.showMsg || jsonmsg.patientName);
      if (!isCall) return;
      if (code && (code === callCode || code === screenId || code == this.ScreensInput.CallCode)) {
        this.CallList.push(jsonmsg);
        this.CallListSpeak();
        this.GetWaitingStaff(this.WaitingStaff.Index - 1, "呼叫触发刷新");
        this.GetMetData();
      }
    });
  },
  watch: {
    ScreensInput() {
      if (this.ScreensInput.DataMode == 1) {
        this.jiaohaostring = parseScreenBindingList(this.ScreensInput)
        if (this.jiaohaostring.length > 0) {
          this.jiaohao = this.jiaohaostring[0]
        }
      }
      this.GetWaitingStaff(this.WaitingStaff.Index - 1);
      this.GetMetData();

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
  padding: 2vh 3vw;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  color: #e8eaed;
}

.content-wrapper {
  flex: 1;
  display: flex;
  gap: 3vw;
  height: calc(100% - 8vh - 2vh);
}

/* Material Elevations */
.elevation-2 { box-shadow: 0 3px 6px rgba(0,0,0,0.16), 0 3px 6px rgba(0,0,0,0.23); }
.elevation-3 { box-shadow: 0 10px 20px rgba(0,0,0,0.19), 0 6px 6px rgba(0,0,0,0.23); }
.elevation-4 { box-shadow: 0 14px 28px rgba(0,0,0,0.25), 0 10px 10px rgba(0,0,0,0.22); }
.elevation-8 { box-shadow: 0 19px 38px rgba(0,0,0,0.30), 0 15px 12px rgba(0,0,0,0.22); }

/* Common Card Styles */
.current-call-card, .past-call-card, .waiting-queue-card {
  background: #2d2e33;
  border-radius: 1.5rem;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.card-header {
  padding: 1.5vh 1.5vw;
  background: #3c4043;
  display: flex;
  align-items: center;
  h3 {
    margin: 0;
    font-size: 1.8vw;
    font-weight: 600;
    color: #9aa0a6;
    letter-spacing: 1px;
  }
}
.border-bottom {
  border-bottom: 1px solid #5f6368;
}
.justify-between {
  justify-content: space-between;
}
.card-body {
  flex: 1;
  padding: 1.5vw;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
.empty-state {
  font-size: 2vw;
  color: #5f6368;
  font-weight: 500;
  text-align: center;
}

/* Left Panel */
.left-panel {
  flex: 1.25;
  display: flex;
  flex-direction: column;
  gap: 2.5vh;
}

.current-call-card {
  flex: 1.8;
  background: linear-gradient(135deg, #1f3a5f 0%, #172a45 100%);
  border: 2px solid rgba(138, 180, 248, 0.4);
  .card-header {
    background: transparent;
    border-bottom: 2px dashed rgba(138, 180, 248, 0.2);
    h3 { color: #8ab4f8; }
  }
}

.call-pulse {
  animation: bg-pulse 3s infinite alternate;
}
@keyframes bg-pulse {
  0% { background: rgba(138, 180, 248, 0.05); }
  100% { background: rgba(138, 180, 248, 0.15); }
}

/* Focus Section for Current Patient */
.table-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 1vh 2vw;
}
.focus-row {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  gap: 2vh;
}
.focus-room {
  font-size: 3vw;
  color: #e8eaed;
  font-weight: 800;
  margin-bottom: 1vh;
  text-shadow: 0 2px 10px rgba(0,0,0,0.5);
}
.focus-patient {
  display: flex;
  align-items: baseline;
  gap: 2vw;
}
.focus-seq {
  font-family: monospace;
  font-size: 4vw;
  color: #9aa0a6;
}
.focus-name {
  font-size: 6vw;
  color: #fca311;
  font-weight: 900;
  text-shadow: 0 4px 20px rgba(252, 163, 17, 0.4);
}
.do-not-disturb-badge {
  background: rgba(217, 48, 37, 0.15);
  border: 2px solid #d93025;
  border-radius: 50px;
  padding: 1vh 3vw;
  display: flex;
  align-items: center;
  gap: 1.5vw;
  margin-top: 2vh;
  box-shadow: 0 0 20px rgba(217, 48, 37, 0.3);
}
.badge-pulse {
  width: 1.5vw;
  height: 1.5vw;
  background: #d93025;
  border-radius: 50%;
  animation: pulse-red 1.5s infinite;
}
@keyframes pulse-red {
  0% { box-shadow: 0 0 0 0 rgba(217,48,37,0.7); }
  70% { box-shadow: 0 0 0 15px rgba(217,48,37,0); }
  100% { box-shadow: 0 0 0 0 rgba(217,48,37,0); }
}
.do-not-disturb-badge span {
  font-size: 2.2vw;
  color: #ff8a80;
  font-weight: bold;
  letter-spacing: 2px;
}
.focus-empty {
  padding: 4vh;
  background: rgba(255,255,255,0.05);
  border-radius: 1rem;
}
.empty-text {
  font-size: 2.5vw;
  color: #5f6368;
}

.past-call-card {
  flex: 1;
  .card-body { padding: 0.5vw 1.5vw; }
}

.carousel-container {
  width: 100%;
  height: 100%;
  flex: 1;
}

:deep(.el-carousel) {
  height: 100% !important;
}
:deep(.el-carousel__container) {
  height: 100% !important;
}

.grid-list {
  display: flex;
  flex-wrap: wrap;
  gap: 1vw;
  height: 100%;
  align-content: flex-start;
  padding-top: 1vh;
}
.grid-item {
  width: calc(33.333% - 0.7vw);
  background: #3c4043;
  border-radius: 0.5rem;
  padding: 1.2vh 0.5vw;
  font-size: 1.8vw;
  text-align: center;
  color: #bdc1c6;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dept-mark {
  font-size: 1.2vw;
  color: #9aa0a6;
  font-weight: normal;
}

/* Right Panel */
.right-panel {
  flex: 1.5;
  display: flex;
  flex-direction: column;
}

.waiting-queue-card {
  flex: 1;
  .badge {
    background: #4285f4;
    color: #fff;
    padding: 0.5vh 1vw;
    border-radius: 1rem;
    font-size: 1.8vw;
    font-weight: bold;
  }
  .card-body { padding: 1.5vw; justify-content: flex-start; }
}

.list-layout {
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: 1.5vw;
  height: 100%;
}
.list-item {
  width: calc(50% - 0.75vw);
  background: rgba(255, 255, 255, 0.05);
  border-radius: 1rem;
  padding: 2.5vh 2vw;
  display: flex;
  align-items: center;
  gap: 2vw;
  border-left: 8px solid #8ab4f8;
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.seq-num {
  font-size: 2.4vw;
  color: #9aa0a6;
  font-weight: bold;
  font-family: monospace;
}
.pat-name {
  font-size: 3vw;
  color: #e8eaed;
  font-weight: 600;
  letter-spacing: 2px;
  flex: 1;
}

/* P1-2d: 来源角标 */
.source-tag {
  font-size: 1.1vw;
  font-weight: 700;
  padding: 0.2vh 0.6vw;
  border-radius: 4px;
  letter-spacing: 1px;
  white-space: nowrap;
}
.src-HEALTH_CHECKUP { background: rgba(52,168,83,0.25); color: #81c995; border: 1px solid #34a853; }
.src-OUTPATIENT_EXAM { background: rgba(66,133,244,0.25); color: #8ab4f8; border: 1px solid #4285f4; }
.src-PHARMACY { background: rgba(251,188,4,0.25); color: #fdd663; border: 1px solid #fbbc04; }

/* 来源影响左边框颜色 */
.item-checkup { border-left-color: #34a853 !important; }
.item-medtech { border-left-color: #4285f4 !important; }
.item-pharmacy { border-left-color: #fbbc04 !important; }

/* Bottom Bar */
.bottom-bar {
  height: 8vh;
  margin-top: 2vh;
  background: #d93025;
  border-radius: 1rem;
  display: flex;
  align-items: center;
  color: white;
  overflow: hidden;
  position: relative;
}

.marquee-container {
  width: 100%;
  padding: 0 2vw;
  overflow: hidden;
  white-space: nowrap;
}
.marquee-text {
  display: inline-block;
  font-size: 2.5vw;
  font-weight: bold;
  letter-spacing: 2px;
  animation: scroll-left 15s linear infinite;
  padding-left: 100%;
}

@keyframes scroll-left {
  0%   { transform: translate(0, 0); }
  100% { transform: translate(-100%, 0); }
}

.action-notice {
  width: 100%;
  text-align: center;
  font-size: 3vw;
  font-weight: bold;
  letter-spacing: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1.5vw;
}

.pulse-dot {
  width: 2vw;
  height: 2vw;
  background: white;
  border-radius: 50%;
  animation: dot-flash 1s infinite alternate;
}
@keyframes dot-flash {
  from { opacity: 0.4; }
  to { opacity: 1; box-shadow: 0 0 10px white; }
}
</style>