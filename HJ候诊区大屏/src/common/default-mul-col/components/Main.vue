/**
 * 候诊区大屏「多诊室按列」模板 Main
 * 消费者：default-mul-col/index.vue
 * 诊室横向并排；每列：诊室名 → 医生(姓名+职称) → 正在就诊 → 候诊队列（竖向滚动）
 * 每页固定 PAGE_SIZE 列；超出则定时翻页
 * 配色对齐 /default、/default-mul
 */
<template>
  <div id="Main" class="mul-col-main">
    <div class="cols-card elevation-4">
      <div v-if="!roomCols.length" class="empty-state">暂无绑定诊室</div>
      <div v-else class="cols-wrap">
        <div
          v-for="col in visibleRoomCols"
          :key="col.roomId"
          class="room-col"
          :style="roomColStyle"
        >
          <div class="cell-room">
            <div class="room-name">{{ col.roomName }}</div>
            <div class="doctor-name" :class="{ 'is-empty': !col.doctorName }">
              {{ col.doctorName || '暂无坐诊' }}
            </div>
            <div class="doctor-title" :class="{ 'is-empty': !col.doctorTitle }">
              {{ col.doctorTitle || ' ' }}
            </div>
          </div>
          <div class="cell-current">
            <template v-if="col.current.length">
              <div
                v-for="(p, idx) in col.current"
                :key="p.id || idx"
                class="patient-line current-line"
              >{{ formatVisitLine(p) }}</div>
            </template>
            <div v-else class="idle-text">空闲</div>
          </div>
          <div class="cell-wait">
            <div
              class="wait-viewport"
              :ref="(el) => setWaitRef(col.roomId, el)"
            >
              <div
                class="wait-track"
                :ref="(el) => setTrackRef(col.roomId, el)"
                :class="{ 'is-scrolling': overflowMap[col.roomId] }"
                :style="scrollStyle(col.roomId)"
              >
                <div
                  class="wait-list"
                  :ref="(el) => setListRef(col.roomId, el)"
                >
                  <template v-if="col.waiting.length">
                    <div
                      v-for="(p, idx) in col.waiting"
                      :key="'a-' + (p.id || idx)"
                      class="patient-line wait-line"
                    >{{ formatVisitLine(p) }}</div>
                  </template>
                  <div v-else class="idle-text">暂无候诊</div>
                </div>
                <div
                  v-if="overflowMap[col.roomId] && col.waiting.length"
                  class="wait-list"
                  aria-hidden="true"
                >
                  <div
                    v-for="(p, idx) in col.waiting"
                    :key="'b-' + (p.id || idx)"
                    class="patient-line wait-line"
                  >{{ formatVisitLine(p) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="pageCount > 1" class="page-dots" aria-hidden="true">
      <span
        v-for="i in pageCount"
        :key="i"
        class="page-dot"
        :class="{ active: i - 1 === pageIndex }"
      />
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

/** 每页固定显示诊室列数（与门诊大屏常见 6 列布局一致） */
const PAGE_SIZE = 6

export default {
  name: 'DefaultMulColMain',
  mixins: [screenDisplayMixin],
  data() {
    return {
      Subtitle: null,
      CallList: [],
      Calli: 0,
      oAudio: null,
      roomCols: [],
      pageIndex: 0,
      overflowMap: {},
      /** 每列滚动时长（秒），按内容高度计算 */
      scrollSecMap: {},
      waitEls: {},
      trackEls: {},
      listEls: {},
      _pollTimer: null,
      _overflowTimer: null,
      _pageTimer: null
    }
  },
  props: ['ScreensInput'],
  computed: {
    screenId() {
      return this.ScreensInput?.ID || this.ScreensInput?.id || ''
    },
    scrollMs() {
      const sec = this.ScreensInput?.ScrollTime || this.ScreensInput?.scrollTime || 5
      return Math.max(3, Number(sec) || 5) * 1000
    },
    /** 翻页间隔：略长于数据轮询，避免过快切换 */
    pageFlipMs() {
      return Math.max(this.scrollMs * 2, 10000)
    },
    pageCount() {
      if (!this.roomCols.length) return 0
      return Math.ceil(this.roomCols.length / PAGE_SIZE)
    },
    visibleRoomCols() {
      const start = this.pageIndex * PAGE_SIZE
      return this.roomCols.slice(start, start + PAGE_SIZE)
    },
    roomColStyle() {
      return {
        flex: `1 1 ${100 / PAGE_SIZE}%`,
        maxWidth: `${100 / PAGE_SIZE}%`,
        minWidth: 0
      }
    }
  },
  methods: {
    setWaitRef(roomId, el) {
      if (el) this.waitEls[roomId] = el
      else delete this.waitEls[roomId]
    },
    setTrackRef(roomId, el) {
      if (el) this.trackEls[roomId] = el
      else delete this.trackEls[roomId]
    },
    setListRef(roomId, el) {
      if (el) this.listEls[roomId] = el
      else delete this.listEls[roomId]
    },
    scrollStyle(roomId) {
      if (!this.overflowMap[roomId]) return undefined
      const sec = this.scrollSecMap[roomId] || 24
      return { animationDuration: `${sec}s` }
    },
    /** 初/复 序号 姓名；复=REVISIT */
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
    doctorOf(p) {
      return (p?.Doctors || p?.doctors || '').trim()
    },
    /**
     * 以 office_room.doctor_id + 在岗医生为准；回落 CallList.Doctors（无职称）
     * @returns {Record<string, { name: string, title: string }>}
     */
    async fetchSittingDoctorMap(roomIds) {
      const map = {}
      if (!roomIds.length) return map
      try {
        const [officeRes, dutyRes] = await Promise.all([
          this.$axios.post('/api/Doctor/GetAllOffice'),
          this.$axios.post('/api/Doctor/GetOnDuty')
        ])
        const offices = (officeRes.data && officeRes.data.data) || []
        const onDuty = (dutyRes.data && dutyRes.data.data) || []
        const dutyById = {}
        for (const d of onDuty) {
          const id = d.id || d.ID
          if (!id) continue
          dutyById[id] = {
            name: d.name || d.Name || '',
            title: d.title || d.Title || ''
          }
        }
        const idSet = new Set(roomIds)
        for (const room of offices) {
          const rid = room.id || room.ID
          if (!rid || !idSet.has(rid)) continue
          const did = room.doctorId || room.DoctorId || ''
          if (did && dutyById[did]?.name) {
            map[rid] = dutyById[did]
          }
        }
      } catch (err) {
        console.error('[default-mul-col] sitting doctors', err)
      }
      return map
    },
    resolveDoctorInfo(sittingMap, roomId, current, waiting) {
      if (sittingMap[roomId]?.name) {
        return sittingMap[roomId]
      }
      for (const p of current) {
        const n = this.doctorOf(p)
        if (n) return { name: n, title: '' }
      }
      for (const p of waiting) {
        const n = this.doctorOf(p)
        if (n) return { name: n, title: '' }
      }
      return { name: '', title: '' }
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
      return (res.data && res.data.data && res.data.data.Data) || []
    },
    async refreshRoomCols() {
      if (!this.screenId) return
      const rooms = parseScreenBindingList(this.ScreensInput)
      if (!rooms.length) {
        this.roomCols = []
        this.pageIndex = 0
        this.stopPageFlip()
        return
      }
      let attending = []
      try {
        attending = await this.fetchAttending()
      } catch (err) {
        console.error('[default-mul-col] GetCallListInput', err)
      }

      const roomIds = rooms.map((r) => r.Code)
      const sittingMap = await this.fetchSittingDoctorMap(roomIds)

      const cols = []
      for (const room of rooms) {
        let waiting = []
        try {
          waiting = await this.fetchWaiting(room.Code)
        } catch (err) {
          console.error('[default-mul-col] waiting', room.Code, err)
        }
        const current = attending.filter((p) => this.roomIdOf(p) === room.Code)
        const doc = this.resolveDoctorInfo(sittingMap, room.Code, current, waiting)
        cols.push({
          roomId: room.Code,
          roomName: room.DeptName,
          doctorName: doc.name,
          doctorTitle: doc.title,
          current,
          waiting
        })
      }
      this.roomCols = cols
      if (this.pageIndex >= this.pageCount) {
        this.pageIndex = 0
      }
      this.ensurePageFlip()
      this.$nextTick(() => {
        requestAnimationFrame(() => this.measureOverflow())
      })
    },
    /**
     * 候诊区是否需要竖向滚动。
     * 用「单份列表」高度与固定 viewport 比较；viewport 高度不足时不误开动画。
     * 滚动一周时长取自屏幕「滚动间隔」（秒），至少 3 秒。
     */
    measureOverflow() {
      const nextOverflow = {}
      const nextSec = { ...this.scrollSecMap }
      const visibleIds = new Set(this.visibleRoomCols.map((c) => c.roomId))
      for (const id of Object.keys(nextSec)) {
        if (!visibleIds.has(id)) delete nextSec[id]
      }
      const scrollSec = Math.max(
        3,
        Number(this.ScreensInput?.ScrollTime || this.ScreensInput?.scrollTime) || 5
      )
      for (const col of this.visibleRoomCols) {
        const vp = this.waitEls[col.roomId]
        const list = this.listEls[col.roomId]
        if (!vp || !list || !col.waiting.length) {
          nextOverflow[col.roomId] = false
          continue
        }
        const vh = vp.clientHeight
        const lh = list.scrollHeight
        if (vh < 8) {
          nextOverflow[col.roomId] = false
          continue
        }
        const overflow = lh > vh + 2
        nextOverflow[col.roomId] = overflow
        if (overflow) {
          nextSec[col.roomId] = scrollSec
        } else {
          delete nextSec[col.roomId]
        }
      }
      this.overflowMap = nextOverflow
      this.scrollSecMap = nextSec
    },
    flipPage() {
      if (this.pageCount <= 1) return
      this.pageIndex = (this.pageIndex + 1) % this.pageCount
      this.overflowMap = {}
      this.scrollSecMap = {}
      this.$nextTick(() => {
        requestAnimationFrame(() => this.measureOverflow())
      })
    },
    /** 仅在需要翻页且定时器未启动时开启；数据刷新不重置间隔 */
    ensurePageFlip() {
      if (this.pageCount <= 1) {
        this.stopPageFlip()
        this.pageIndex = 0
        return
      }
      if (this._pageTimer) return
      this._pageTimer = setInterval(() => this.flipPage(), this.pageFlipMs)
    },
    stopPageFlip() {
      if (this._pageTimer) {
        clearInterval(this._pageTimer)
        this._pageTimer = null
      }
    },
    startPoll() {
      this.stopDataTimers()
      this._pollTimer = setInterval(() => {
        void this.refreshRoomCols()
      }, this.scrollMs)
      this._overflowTimer = setInterval(() => this.measureOverflow(), 2000)
    },
    stopDataTimers() {
      if (this._pollTimer) {
        clearInterval(this._pollTimer)
        this._pollTimer = null
      }
      if (this._overflowTimer) {
        clearInterval(this._overflowTimer)
        this._overflowTimer = null
      }
    },
    stopPoll() {
      this.stopDataTimers()
      this.stopPageFlip()
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
        void this.refreshRoomCols()
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
        void this.refreshRoomCols()
        this.startPoll()
      },
      deep: true,
      immediate: true
    }
  }
}
</script>

<style lang="scss" scoped>
.mul-col-main {
  width: 100%;
  height: 100%;
  padding: 2vh 2vw;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  color: #e8eaed;
  gap: 1vh;
  min-height: 0;
  overflow: hidden;
}

.elevation-4 {
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.25), 0 10px 10px rgba(0, 0, 0, 0.22);
}
.elevation-8 {
  box-shadow: 0 19px 38px rgba(0, 0, 0, 0.3), 0 15px 12px rgba(0, 0, 0, 0.22);
}

.cols-card {
  flex: 1 1 auto;
  min-height: 0;
  background: #2d2e33;
  border-radius: 1.2rem;
  overflow: hidden;
  display: flex;
  border: 1px solid rgba(138, 180, 248, 0.15);
}

.cols-wrap {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  overflow: hidden;
}

.room-col {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border-right: 1px solid #3c4043;
  background: linear-gradient(180deg, rgba(31, 58, 95, 0.35) 0%, transparent 28%);
  box-sizing: border-box;
  &:last-child {
    border-right: none;
  }
  &:nth-child(even) {
    background: linear-gradient(180deg, rgba(31, 58, 95, 0.22) 0%, rgba(255, 255, 255, 0.02) 28%);
  }
}

.cell-room {
  flex: 0 0 16vh;
  height: 16vh;
  box-sizing: border-box;
  padding: 1vh 0.5vw;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.35vh;
  border-bottom: 1px solid #3c4043;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.45);
}

.room-name {
  width: 100%;
  height: 4.8vh;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 1.4vw;
  font-weight: 800;
  color: #e8eaed;
  letter-spacing: 1px;
  line-height: 1.2;
  overflow: hidden;
  word-break: keep-all;
}

.doctor-name {
  width: 100%;
  height: 4.2vh;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 2vw;
  font-weight: 800;
  color: #8ab4f8;
  letter-spacing: 1px;
  line-height: 1.15;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  &.is-empty {
    color: #5f6368;
    font-weight: 500;
    font-size: 1.4vw;
  }
}

.doctor-title {
  width: 100%;
  height: 2.8vh;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 1.15vw;
  font-weight: 600;
  color: #9aa0a6;
  letter-spacing: 0.5px;
  line-height: 1.2;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  &.is-empty {
    visibility: hidden;
  }
}

.cell-current {
  flex: 0 0 11vh;
  height: 11vh;
  box-sizing: border-box;
  padding: 1vh 0.6vw;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5vh;
  border-bottom: 1px solid #3c4043;
  background: rgba(138, 180, 248, 0.08);
  overflow: hidden;
}

.cell-wait {
  flex: 1 1 auto;
  min-height: 0;
  position: relative;
  /* 绝对定位 viewport，候诊列表再长也不会撑开诊室列 */
  overflow: hidden;
}

.patient-line {
  text-align: center;
  font-weight: 700;
  letter-spacing: 0.5px;
  word-break: keep-all;
  white-space: nowrap;
}
.current-line {
  font-size: 1.45vw;
  color: #fca311;
  text-shadow: 0 2px 12px rgba(252, 163, 17, 0.35);
}
.wait-line {
  flex: 0 0 auto;
  box-sizing: border-box;
  font-size: 1.25vw;
  color: #e8eaed;
  padding: 0.6vh 0.5vw;
  margin: 0.3vh 0;
  background: rgba(138, 180, 248, 0.12);
  border: 1px solid rgba(138, 180, 248, 0.25);
  border-radius: 0.45rem;
}
.idle-text {
  font-size: 1.15vw;
  color: #5f6368;
  font-weight: 500;
  text-align: center;
  padding: 1.2vh 0;
}

.wait-viewport {
  position: absolute;
  inset: 0.8vh 0.5vw 1.2vh;
  overflow: hidden;
}
.wait-track {
  width: 100%;
  will-change: transform;
}
.wait-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: stretch;
}
.wait-track.is-scrolling {
  animation-name: wait-scroll-y;
  animation-timing-function: linear;
  animation-iteration-count: infinite;
}

@keyframes wait-scroll-y {
  0% {
    transform: translateY(0);
  }
  100% {
    transform: translateY(-50%);
  }
}

.page-dots {
  flex: 0 0 auto;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.6vw;
  padding: 0.2vh 0;
}
.page-dot {
  width: 0.55vw;
  height: 0.55vw;
  border-radius: 50%;
  background: #5f6368;
  &.active {
    background: #8ab4f8;
    transform: scale(1.25);
  }
}

.empty-state {
  width: 100%;
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
