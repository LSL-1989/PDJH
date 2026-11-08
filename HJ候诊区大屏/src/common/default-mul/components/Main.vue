/**
 * 候诊区大屏「多诊室按行」模板 Main
 * 消费者：default-mul/index.vue
 * 依赖：CallList/GetCallListInput、GetCallListInputPage；屏幕绑定 roomInfo
 * 行布局：诊室名 | 正在就诊 | 候诊队列（剩余宽、居中，过长横向滚动）
 * 就诊人格式：初/复 序号 姓名
 */
<template>
  <div id="Main" class="mul-main">
    <div class="table-card elevation-4">
      <div class="table-head">
        <div class="col-room">诊室</div>
        <div class="col-current">正在{{ actionLabel }}</div>
        <div class="col-wait">候诊队列</div>
      </div>
      <div class="table-body">
        <div v-if="!roomRows.length" class="empty-state">暂无绑定诊室</div>
        <div
          v-for="row in roomRows"
          :key="row.roomId"
          class="room-row"
        >
          <div class="col-room cell-room">{{ row.roomName }}</div>
          <div class="col-current cell-current">
            <template v-if="row.current.length">
              <span
                v-for="(p, idx) in row.current"
                :key="p.id || idx"
                class="patient-line current-line"
              >{{ formatVisitLine(p) }}</span>
            </template>
            <span v-else class="idle-text">空闲</span>
          </div>
          <div class="col-wait cell-wait">
            <div
              class="wait-viewport"
              :ref="(el) => setWaitRef(row.roomId, el)"
              :class="{ 'is-overflow': overflowMap[row.roomId] }"
            >
              <div
                class="wait-track"
                :ref="(el) => setTrackRef(row.roomId, el)"
                :style="
                  overflowMap[row.roomId]
                    ? { animationDuration: scrollSec + 's' }
                    : undefined
                "
              >
                <template v-if="row.waiting.length">
                  <span
                    v-for="(p, idx) in row.waiting"
                    :key="'a-' + (p.id || idx)"
                    class="patient-line wait-line"
                  >{{ formatVisitLine(p) }}</span>
                  <template v-if="overflowMap[row.roomId]">
                    <span
                      v-for="(p, idx) in row.waiting"
                      :key="'b-' + (p.id || idx)"
                      class="patient-line wait-line"
                      aria-hidden="true"
                    >{{ formatVisitLine(p) }}</span>
                  </template>
                </template>
                <span v-else class="idle-text">暂无候诊</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showMarqueeBar" class="bottom-bar elevation-8">
      <div class="marquee-container" v-if="Subtitle == null">
        <div class="marquee-text">{{ screenMarqueeText }}</div>
      </div>
      <div class="action-notice" v-if="Subtitle != null">
        <span class="pulse-dot"></span> {{ Subtitle }}
      </div>
    </div>

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
import { parseScreenBindingList } from '@/utils/screenBinding'
import {
  shouldSpeakOnThisScreen,
  speakCallText,
  resolveCallTimes,
  resolveSpeakRate
} from '@/utils/screenVoice'
import screenDisplayMixin from '@/mixins/screenDisplayMixin'

export default {
  name: 'DefaultMulMain',
  mixins: [screenDisplayMixin],
  data() {
    return {
      Subtitle: null,
      CallList: [],
      Calli: 0,
      oAudio: null,
      roomRows: [],
      overflowMap: {},
      waitEls: {},
      trackEls: {},
      _pollTimer: null,
      _overflowTimer: null
    }
  },
  props: ['ScreensInput'],
  computed: {
    actionLabel() {
      return this.ScreensInput && this.ScreensInput.deptType === 2 ? '检查' : '就诊'
    },
    screenId() {
      return this.ScreensInput?.ID || this.ScreensInput?.id || ''
    },
    scrollMs() {
      const sec = this.ScreensInput?.ScrollTime || this.ScreensInput?.scrollTime || 5
      return Math.max(3, Number(sec) || 5) * 1000
    },
    /** 候诊横向滚动一周时长（秒），取自「滚动间隔」 */
    scrollSec() {
      const sec = this.ScreensInput?.ScrollTime || this.ScreensInput?.scrollTime || 5
      return Math.max(3, Number(sec) || 5)
    }
  },
  methods: {
    setWaitRef(roomId, el) {
      if (el) this.waitEls[roomId] = el
    },
    setTrackRef(roomId, el) {
      if (el) this.trackEls[roomId] = el
    },
    /**
     * 初/复 序号 姓名；复=REVISIT，其余按初诊
     * @param {object} p CallList 行
     */
    formatVisitLine(p) {
      if (!p) return ''
      const ts = p.TriageState || p.triageState || ''
      const visit = ts === 'REVISIT' ? '复' : '初'
      const no =
        p.ShowNo ||
        p.showNo ||
        `${p.xhlb || ''}${p.xh != null ? p.xh : ''}`.trim() ||
        ''
      const name = this.formatPatientName(p.PatientName || p.patientName || '')
      return [visit, no, name].filter(Boolean).join(' ')
    },
    roomIdOf(p) {
      return p?.OfficeRoomId || p?.officeRoomId || p?.DeptId || p?.DeptID || ''
    },
    async fetchAttending() {
      const res = await this.$axios.post('/api/CallList/GetCallListInput', {
        ID: this.screenId
      })
      return (res.data && res.data.data) || []
    },
    async fetchWaiting(roomCode) {
      const res = await this.$axios.post('/api/CallList/GetCallListInputPage', {
        index: 1,
        page: 100,
        state: 0,
        ScreensID: this.screenId,
        Code: roomCode || null
      })
      const data = (res.data && res.data.data && res.data.data.Data) || []
      return data
    },
    /** 按绑定诊室组装行数据 */
    async refreshRoomRows() {
      if (!this.screenId) return
      const rooms = parseScreenBindingList(this.ScreensInput)
      if (!rooms.length) {
        this.roomRows = []
        return
      }
      let attending = []
      try {
        attending = await this.fetchAttending()
      } catch (err) {
        console.error('[default-mul] GetCallListInput', err)
      }

      const rows = []
      for (const room of rooms) {
        let waiting = []
        try {
          waiting = await this.fetchWaiting(room.Code)
        } catch (err) {
          console.error('[default-mul] waiting', room.Code, err)
        }
        const current = attending.filter((p) => this.roomIdOf(p) === room.Code)
        // 若按 OfficeRoomId 未匹配到，回退：该诊室候诊接口同 Code 的就诊态已在 attending 全量里
        rows.push({
          roomId: room.Code,
          roomName: room.DeptName,
          current,
          waiting
        })
      }
      this.roomRows = rows
      this.$nextTick(() => this.measureOverflow())
    },
    measureOverflow() {
      const next = { ...this.overflowMap }
      for (const row of this.roomRows) {
        const vp = this.waitEls[row.roomId]
        const track = this.trackEls[row.roomId]
        if (!vp || !track) {
          next[row.roomId] = false
          continue
        }
        next[row.roomId] = track.scrollWidth > vp.clientWidth + 4
      }
      this.overflowMap = next
    },
    startPoll() {
      this.stopPoll()
      this._pollTimer = setInterval(() => {
        void this.refreshRoomRows()
      }, this.scrollMs)
      this._overflowTimer = setInterval(() => this.measureOverflow(), 2000)
    },
    stopPoll() {
      if (this._pollTimer) {
        clearInterval(this._pollTimer)
        this._pollTimer = null
      }
      if (this._overflowTimer) {
        clearInterval(this._overflowTimer)
        this._overflowTimer = null
      }
    },
    async CallListSpeak() {
      if (!this.screenVoiceEnabled()) return
      const cur = this.CallList[0]
      if (!cur) return
      this.Subtitle = cur.callText || cur.showMsg || ''
      const audioSrc = cur.fileName
      if (!audioSrc) {
        if (shouldSpeakOnThisScreen(cur, this.ScreensInput)) {
          const times = resolveCallTimes(this.ScreensInput)
          const rate = resolveSpeakRate(this.ScreensInput)
          speakCallText(this.Subtitle || cur.ttsCallText, times, rate)
        }
        const text = this.Subtitle
        setTimeout(() => {
          if (this.Subtitle === text) this.Subtitle = null
        }, 10000)
        this.Calli = -1
        this.CallList.shift()
        if (this.CallList.length !== 0) this.CallListSpeak()
        return
      }
      if (this.oAudio.paused) {
        this.oAudio.src = audioSrc
        this.oAudio.load()
        await this.oAudio.play()
      }
      this.oAudio.onended = () => {
        if (this.Calli < (this.ScreensInput.CallNum || 1) - 1) {
          this.oAudio.load()
          this.oAudio.play()
        }
        if (this.Calli >= (this.ScreensInput.CallNum || 1) - 1) {
          this.Subtitle = null
          this.Calli = -1
          this.CallList.shift()
          if (this.CallList.length !== 0) {
            this.CallListSpeak()
          }
        }
        this.Calli++
      }
    },
    isCallForThisScreen(msg) {
      if (!msg || msg.type === 'STATE_UPDATE') return false
      const isCall =
        msg.type === 'CALL_MSG' || !!(msg.callText || msg.showMsg || msg.patientName)
      if (!isCall) return false
      const code = String(msg.code || '').trim()
      const callCode = String(this.ScreensInput.CallCode || this.ScreensInput.callCode || '').trim()
      const screenId = String(this.ScreensInput.ID || this.ScreensInput.id || '').trim()
      if (code && (code === callCode || code === screenId)) return true
      const rooms = parseScreenBindingList(this.ScreensInput)
      if (rooms.length) {
        const office = String(msg.office || '').trim()
        const roomId = String(msg.officeRoomId || '').trim()
        return rooms.some(
          (r) => (roomId && r.Code === roomId) || (office && r.DeptName === office) || (code && r.Code === code)
        )
      }
      return false
    }
  },
  mounted() {
    this.oAudio = this.$refs.audio
    this.$proxy.on('addMessageToList', (msg) => {
      const jsonmsg = typeof msg === 'string' ? JSON.parse(msg) : msg
      if (this.isCallForThisScreen(jsonmsg)) {
        this.CallList.push(jsonmsg)
        this.CallListSpeak()
        void this.refreshRoomRows()
      }
    })
    window.addEventListener('resize', this.measureOverflow)
  },
  beforeUnmount() {
    this.stopPoll()
    window.removeEventListener('resize', this.measureOverflow)
  },
  watch: {
    ScreensInput: {
      handler() {
        void this.refreshRoomRows()
        this.startPoll()
      },
      deep: true,
      immediate: true
    }
  }
}
</script>

<style lang="scss" scoped>
.mul-main {
  width: 100%;
  height: 100%;
  padding: 2vh 2vw;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  color: #e8eaed;
  gap: 1.5vh;
}

.elevation-4 {
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.25), 0 10px 10px rgba(0, 0, 0, 0.22);
}
.elevation-8 {
  box-shadow: 0 19px 38px rgba(0, 0, 0, 0.3), 0 15px 12px rgba(0, 0, 0, 0.22);
}

.table-card {
  flex: 1;
  min-height: 0;
  background: #2d2e33;
  border-radius: 1.2rem;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(138, 180, 248, 0.15);
}

.table-head,
.room-row {
  display: flex;
  align-items: stretch;
  width: 100%;
}

.table-head {
  flex: none;
  background: #3c4043;
  border-bottom: 1px solid #5f6368;
  font-size: 1.4vw;
  font-weight: 700;
  color: #9aa0a6;
  letter-spacing: 1px;
  > div {
    padding: 1.2vh 1vw;
    display: flex;
    align-items: center;
  }
}

.table-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.room-row {
  border-bottom: 1px solid #3c4043;
  min-height: 9vh;
  background: linear-gradient(90deg, rgba(31, 58, 95, 0.25) 0%, transparent 40%);
  &:nth-child(even) {
    background: rgba(255, 255, 255, 0.02);
  }
}

.col-room {
  flex: 0 0 16vw;
  width: 16vw;
  max-width: 16vw;
}
.col-current {
  flex: 0 0 22vw;
  width: 22vw;
  max-width: 22vw;
}
.col-wait {
  flex: 1 1 auto;
  min-width: 0;
}

.cell-room,
.cell-current,
.cell-wait {
  padding: 1.2vh 1vw;
  display: flex;
  align-items: center;
  box-sizing: border-box;
}

.cell-room {
  font-size: 1.8vw;
  font-weight: 800;
  color: #e8eaed;
  border-right: 1px solid #3c4043;
}

.cell-current {
  flex-wrap: wrap;
  gap: 0.6vw;
  border-right: 1px solid #3c4043;
  background: rgba(138, 180, 248, 0.06);
}

.cell-wait {
  overflow: hidden;
}

.patient-line {
  white-space: nowrap;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.current-line {
  font-size: 1.7vw;
  color: #fca311;
  text-shadow: 0 2px 12px rgba(252, 163, 17, 0.35);
}
.wait-line {
  font-size: 1.5vw;
  color: #e8eaed;
  padding: 0.4vh 0.8vw;
  background: rgba(138, 180, 248, 0.12);
  border: 1px solid rgba(138, 180, 248, 0.25);
  border-radius: 0.5rem;
}
.idle-text {
  font-size: 1.3vw;
  color: #5f6368;
  font-weight: 500;
}

.wait-viewport {
  width: 100%;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
}
.wait-track {
  display: inline-flex;
  align-items: center;
  gap: 1vw;
  white-space: nowrap;
}
.wait-viewport.is-overflow {
  justify-content: flex-start;
}
.wait-viewport.is-overflow .wait-track {
  animation: wait-scroll 22s linear infinite;
  padding-left: 0;
}

@keyframes wait-scroll {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-50%);
  }
}

.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2vw;
  color: #5f6368;
}

.bottom-bar {
  height: 7vh;
  background: #1a73e8;
  border-radius: 1rem;
  display: flex;
  align-items: center;
  overflow: hidden;
  flex: none;
}
.marquee-container {
  width: 100%;
  overflow: hidden;
  white-space: nowrap;
}
.marquee-text {
  display: inline-block;
  padding-left: 100%;
  font-size: 2vw;
  font-weight: 600;
  color: #fff;
  animation: marquee 15s linear infinite;
}
@keyframes marquee {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-100%);
  }
}
.action-notice {
  width: 100%;
  text-align: center;
  font-size: 2.2vw;
  font-weight: 700;
  color: #fff;
}
.pulse-dot {
  display: inline-block;
  width: 0.8vw;
  height: 0.8vw;
  margin-right: 0.6vw;
  border-radius: 50%;
  background: #fbbc04;
  animation: pulse 1s infinite;
}
@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}
</style>
