<template>
  <div id="Main" :class="screenLayoutClass">
    <div class="content-wrapper">
      <!-- Left Panel: Doctor Profile (40%) -->
      <div class="doctor-panel elevation-4">
        <div class="doc-header">
          <div class="doc-avatar">
            <!-- Universal Doctor Avatar SVG Placeholder -->
            <svg viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="50" cy="50" r="50" fill="#E8EAED" opacity="0.1"/>
              <path d="M50 25C40.335 25 32.5 32.835 32.5 42.5C32.5 52.165 40.335 60 50 60C59.665 60 67.5 52.165 67.5 42.5C67.5 32.835 59.665 25 50 25ZM50 65C34.16 65 20.665 74.495 15 88.025C23.085 95.395 35.805 100 50 100C64.195 100 76.915 95.395 85 88.025C79.335 74.495 65.84 65 50 65Z" fill="#8AB4F8"/>
              <rect x="45" y="65" width="10" height="25" fill="#8AB4F8"/>
              <circle cx="50" cy="90" r="4" fill="#E8EAED"/>
            </svg>
          </div>
          <div class="doc-info">
            <div class="doc-name">{{ currentDoctorName || '坐诊医生' }}</div>
            <div class="doc-title">{{ currentDoctorName ? '主治医师 / 专家' : '暂无坐诊信息' }}</div>
          </div>
        </div>
        
        <div class="doc-body" v-if="currentDoctorName">
          <div class="info-section">
            <h4><span class="icon">⚕</span> 专业特长</h4>
            <p>擅长所属专科常见病、多发病的诊治。对疑难杂症有丰富的临床诊疗经验。态度和蔼，对待病患细致耐心。</p>
          </div>
          <div class="info-section">
            <h4><span class="icon">⌚</span> 出诊规律</h4>
            <div class="schedule-tags">
              <span class="tag active">今日出诊</span>
            </div>
          </div>
        </div>
        <div class="doc-body-empty" v-else>
          正在等待排班同步...
        </div>
      </div>

      <!-- Right Panel: Calling Queue (60%) -->
      <div class="queue-panel">
        
        <!-- Current Call Highlight -->
        <div class="current-call-box elevation-4">
          <div class="box-tag">正在就诊 CURRENT</div>
          
          <div class="current-call-content pulse-bg" v-if="DepartmentInput && DepartmentInput.PatientName">
            <div class="current-num">{{ DepartmentInput.xh || '--' }}号</div>
            <div class="current-name">{{ formatPatientName(DepartmentInput.PatientName) }}</div>
            <div class="current-status">请您进入诊室就诊</div>
          </div>
          <div class="current-call-empty" v-else>
            <div class="empty-icon">☕</div>
            <div class="empty-text">当前暂无人就诊</div>
          </div>
        </div>

        <!-- Waiting Queue -->
        <div class="waiting-box elevation-2">
          <div class="box-tag">等候排队 WAITING</div>
          <div class="waiting-list-wrapper">
            <div v-if="IsWaitingStaff && WaitingStaff.Data.length > 0" class="waiting-grid">
               <div class="waiting-item" v-for="(item, index) in WaitingStaff.Data.slice(0, 8)" :key="item.id">
                 <div class="wait-seq">{{ item.xh }}号</div>
                 <div class="wait-name">{{ formatPatientName(item.PatientName) }}</div>
               </div>
            </div>
            <div v-else class="waiting-empty">
               无等候人员
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- Bottom Announcement Bar (Optional) -->
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
import screenDisplayMixin from "@/mixins/screenDisplayMixin";
export default {
  name: "Main",
  mixins: [screenDisplayMixin],
  data() {
    return {
      Subtitle: null,
      speech: null,
      MetData: [], 
      IsWaitingStaff: false,
      WaitingStaff: { Index: 1, Page: 8, Data: [], Totle: 0, state: 0, PageCount: 0 },
      DepartmentInput: { Name: '', Doctors: '', PatientName: '', xh: '' }, 
      jiaohaostring: [],
      jiaohao: { Code: null, DeptName: null },
      CallList: [],
      Calli: 0,
      oAudio: null,
    };
  },
  props: ["ScreensInput"],
  computed: {
    // 提取当前实际排班的医生名称
    currentDoctorName() {
      if (this.DepartmentInput && this.DepartmentInput.Doctors) {
        return this.DepartmentInput.Doctors;
      }
      if (this.WaitingStaff.Data && this.WaitingStaff.Data.length > 0 && this.WaitingStaff.Data[0].Doctors) {
        return this.WaitingStaff.Data[0].Doctors;
      }
      return null;
    }
  },
  methods: {
    GetMetData() {
      this.$axios
        .post("/api/CallList/GetCallListInput", { ID: this.ScreensInput.ID })
        .then((res) => { this.MetData = res.data.data; })
        .catch((err) => { console.error(err); });
    },
    GetWaitingStaff() {
      let indexToFetch = this.WaitingStaff.Index;
      this.$axios
        .post("/api/CallList/GetCallListInputPage", {
          index: indexToFetch,
          page: this.WaitingStaff.Page,
          state: this.WaitingStaff.state,
          ScreensID: this.ScreensInput.ID,
          Code: this.jiaohao ? this.jiaohao.Code : null,
        })
        .then((res) => {
          this.WaitingStaff.Data = res.data.data.Data;
          this.WaitingStaff.Totle = res.data.data.Totle;
          this.WaitingStaff.PageCount = res.data.data.PageCount;
          this.IsWaitingStaff = this.WaitingStaff.Totle > 0;
        })
        .catch((err) => { console.error(err); });
    },
    GetDepartmentInput() {
      var Prams = { PageCount: 1, Index: 1, Page: 1, state: 0, ScreensID: this.ScreensInput.ID };
      this.$axios
        .post("/api/Department/GetDepartmentInput", Prams)
        .then((res) => {
          if(res.data.data && res.data.data.Data && res.data.data.Data.length > 0) {
            this.DepartmentInput = res.data.data.Data[0];
          } else {
             this.DepartmentInput = { Name: '', Doctors: '', PatientName: '', xh: '' };
          }
        })
        .catch((err) => { console.error(err); });
    },
    CallSpeak(data, CallInput) {
      if (CallInput != null || CallInput != undefined) {
        this.$axios.post("api/CallMsg/EditCallMsg", CallInput).then(() => {}).catch((err) => { console.error(err); });
      }
    },
    async CallListSpeak() {
      if(this.CallList.length === 0) return;
      this.Subtitle = this.CallList[0].showMsg;
      if (this.oAudio.paused) {
        this.oAudio.src = this.CallList[0].fileName;
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
  },
  mounted() {
    this.oAudio = this.$refs.audio;
    this.$proxy.on("addMessageToList", (msg) => {
      var jsonmsg = JSON.parse(msg);
      if (jsonmsg.code == this.ScreensInput.CallCode) {
        this.CallList.push(jsonmsg);
        this.CallListSpeak();
        this.GetMetData();
      }
    });
  },
  watch: {
    ScreensInput() {
      this.GetMetData();
      this.GetDepartmentInput();
      if (this.ScreensInput.DataMode == 1) {
        if(this.ScreensInput.roomInfo || this.ScreensInput.deptInfo || this.ScreensInput.DeptInfo) {
          let DeptInfos = (this.ScreensInput.roomInfo || this.ScreensInput.RoomInfo || this.ScreensInput.deptInfo || this.ScreensInput.DeptInfo || "").split(";");
          for (let index = 0; index < DeptInfos.length; index++) {
            if(!DeptInfos[index]) continue;
            let DeptInfos2 = DeptInfos[index].split("|");
            if(DeptInfos2.length >= 2) {
               this.jiaohaostring.push({ Code: DeptInfos2[0], DeptName: DeptInfos2[1] });
            }
          }
          if(this.jiaohaostring.length > 0) this.jiaohao = this.jiaohaostring[0];
        }
      }
      window.setInterval(() => {
        this.GetMetData();
      }, this.ScreensInput.ScrollTime * 1000);
    },
    MetData() {
      this.GetWaitingStaff();
      this.GetDepartmentInput();
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
.elevation-4 { box-shadow: 0 14px 28px rgba(0,0,0,0.25), 0 10px 10px rgba(0,0,0,0.22); }
.elevation-8 { box-shadow: 0 19px 38px rgba(0,0,0,0.30), 0 15px 12px rgba(0,0,0,0.22); }

/* Left Panel: Doctor Profile */
.doctor-panel {
  flex: 0.8;
  background: #2d2e33;
  border-radius: 1.5rem;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-top: 6px solid #8ab4f8; /* Highlight Brand Color */
}

.doc-header {
  padding: 4vh 2vw 2vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-bottom: 1px solid #45474a;
  background: rgba(255, 255, 255, 0.02);
}

.doc-avatar {
  width: 14vw;
  height: 14vw;
  border-radius: 50%;
  background: #3c4043;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 2vh;
  box-shadow: 0 8px 16px rgba(0,0,0,0.3);
  svg {
    width: 70%;
    height: 70%;
  }
}

.doc-info {
  text-align: center;
}

.doc-name {
  font-size: 3vw;
  font-weight: 800;
  color: #e8eaed;
  letter-spacing: 2px;
  margin-bottom: 1vh;
}

.doc-title {
  font-size: 1.5vw;
  color: #8ab4f8;
  font-weight: 500;
  background: rgba(138, 180, 248, 0.1);
  padding: 0.5vh 1.5vw;
  border-radius: 2rem;
  display: inline-block;
}

.doc-body {
  padding: 3vh 2vw;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3vh;
}

.doc-body-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.8vw;
  color: #5f6368;
}

.info-section {
  h4 {
    margin: 0 0 1.5vh 0;
    font-size: 1.6vw;
    color: #9aa0a6;
    display: flex;
    align-items: center;
    gap: 0.8vw;
    .icon { color: #8ab4f8; }
  }
  p {
    margin: 0;
    font-size: 1.4vw;
    line-height: 1.6;
    color: #bdc1c6;
  }
}

.schedule-tags {
  display: flex;
  gap: 1vw;
  .tag {
    font-size: 1.2vw;
    padding: 0.6vh 1.2vw;
    border-radius: 0.6rem;
    background: #45474a;
    color: #bdc1c6;
    &.active {
      background: rgba(129, 201, 149, 0.15);
      color: #81c995;
      border: 1px solid rgba(129, 201, 149, 0.3);
    }
  }
}

/* Right Panel: Queue Panel */
.queue-panel {
  flex: 1.2;
  display: flex;
  flex-direction: column;
  gap: 3vh;
}

.current-call-box {
  flex: 1.2;
  background: linear-gradient(135deg, #1f3a5f 0%, #172a45 100%);
  border-radius: 1.5rem;
  border: 2px solid rgba(138, 180, 248, 0.4);
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.pulse-bg {
  animation: bg-pulse 3s infinite alternate;
}
@keyframes bg-pulse {
  0% { background: rgba(138, 180, 248, 0.05); }
  100% { background: rgba(138, 180, 248, 0.15); }
}

.box-tag {
  position: absolute;
  top: 0;
  left: 0;
  background: rgba(138, 180, 248, 0.15);
  color: #8ab4f8;
  padding: 0.8vh 2vw;
  border-bottom-right-radius: 1.5rem;
  font-size: 1.4vw;
  font-weight: bold;
  letter-spacing: 1px;
}

.current-call-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.current-num {
  font-size: 3.5vw;
  color: #bdc1c6;
  font-family: monospace;
  font-weight: bold;
  margin-bottom: 0.5vh;
}

.current-name {
  font-size: 7vw;
  color: #fca311;
  font-weight: 900;
  letter-spacing: 4px;
  text-shadow: 0 4px 12px rgba(252, 163, 17, 0.4);
  line-height: 1.1;
  margin-bottom: 2vh;
}

.current-status {
  font-size: 2.5vw;
  color: #81c995;
  font-weight: 600;
  letter-spacing: 2px;
}

.current-call-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #5f6368;
  .empty-icon { font-size: 6vw; margin-bottom: 2vh; opacity: 0.5; }
  .empty-text { font-size: 2vw; font-weight: bold; }
}

.waiting-box {
  flex: 1;
  background: #2d2e33;
  border-radius: 1.5rem;
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 4.5vh 1.5vw 1.5vw;
}

.waiting-list-wrapper {
  flex: 1;
  overflow: hidden;
}

.waiting-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 1vw;
  height: 100%;
  align-content: flex-start;
}

.waiting-item {
  width: calc(25% - 0.75vw); /* 8 items per page max */
  background: rgba(255, 255, 255, 0.05);
  border-radius: 0.8rem;
  padding: 1.5vh 0.5vw;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-left: 4px solid #8ab4f8;
  gap: 0.5vh;
}

.wait-seq {
  font-size: 1.6vw;
  color: #9aa0a6;
  font-weight: bold;
}
.wait-name {
  font-size: 2vw;
  color: #e8eaed;
  font-weight: 600;
}

.waiting-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.8vw;
  color: #5f6368;
}

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

/* =========================================================
   PORTRAIT (VERTICAL SCREEN) RESPONSIVE DESIGN
   横竖屏双版本自适应：当软硬件感知到屏幕为竖屏时，自动切换上下堆叠排版
   ========================================================= */
@media screen and (orientation: portrait) {
  /* 基础容器切换为上下堆叠 */
  .content-wrapper {
    flex-direction: column;
    gap: 2vh;
  }
  
  /* 医生模块置顶，占 30% 左右的高度，变为横向子排列以节省高度 */
  .doctor-panel {
    flex: 0;
    height: 30vh;
    border-top: none;
    border-left: 8px solid #8ab4f8;
  }
  
  .doc-header {
    flex-direction: row; 
    border-bottom: 1px solid #45474a;
    padding: 2vh 4vw;
    gap: 4vw;
    justify-content: flex-start;
  }
  
  .doc-avatar {
    width: 14vh; /* 使用 vh 保证在竖屏足够大 */
    height: 14vh;
    margin-bottom: 0;
  }
  
  .doc-info {
    text-align: left;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: flex-start;
  }
  
  .doc-name {
    font-size: 6vw; /* 竖屏下字号要放大 */
    margin-bottom: 1vh;
  }
  
  .doc-title {
    font-size: 3vw;
  }
  
  .doc-body {
    padding: 2vh 4vw;
    flex-direction: row; /* 信息区块横向排布 */
    gap: 4vw;
  }
  
  .info-section {
    flex: 1;
    h4 { font-size: 3vw; margin-bottom: 1vh; }
    p { font-size: 2.5vw; }
  }
  
  .schedule-tags .tag {
    font-size: 2.5vw;
  }

  /* 叫号队列模块占据剩余底部高度 */
  .queue-panel {
    flex: 1;
    gap: 2vh;
  }

  /* 正在呼叫模块高度增大 */
  .current-call-box {
    flex: 1.5;
  }
  
  .box-tag {
    font-size: 2.5vw;
    padding: 1vh 3vw;
  }

  .current-num { font-size: 8vw; }
  .current-name { font-size: 16vw; } /* 名字放得极大 */
  .current-status { font-size: 4vw; }

  /* 候诊列表 */
  .waiting-box {
    flex: 1;
    padding: 5vh 3vw 2vh;
  }

  .waiting-item {
    width: calc(50% - 1vw); /* 一行两个候诊者 */
    padding: 2vh 1vw;
  }
  
  .wait-seq { font-size: 3vw; }
  .wait-name { font-size: 4.5vw; }

  /* 底部横幅文字放大 */
  .marquee-text, .action-notice { font-size: 4vw; }
}
</style>