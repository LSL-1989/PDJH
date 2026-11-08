<template>
  <div class="window-board" :class="{ pharmacy: isPharmacy, lab: !isPharmacy }">
    <header class="topbar">
      <div class="screen-title">{{ screenTitle }}</div>
      <div class="clock">
        <div class="clock-date">{{ dateText }}</div>
        <div class="clock-time">{{ timeText }}</div>
      </div>
    </header>

    <div v-if="!loaded" class="status-block">正在连接窗口队列…</div>
    <div v-else-if="!sections.length" class="status-block">{{ loadError || '尚未绑定窗口' }}</div>

    <div v-else class="room">
      <div class="ordinal">
        <div class="ordinal-no" :class="'digits-' + String(active.ordinal).length">{{ active.ordinal }}</div>
      </div>

      <div class="queue-pane">
        <div class="pane call-pane">
          <div class="pane-head">正在叫号</div>
          <div class="call-body">
            <template v-if="active.current[0]">
              <div class="call-no">{{ ticketOf(active.current[0]) }}</div>
              <div v-if="showField('PATIENT_NAME')" class="call-name">{{ nameOf(active.current[0]) }}</div>
            </template>
            <div v-else class="call-idle">等待叫号</div>
          </div>
        </div>

        <div class="pane wait-pane">
          <div class="pane-head">
            <span>{{ queueTitle }}</span>
            <span class="count">{{ active.queue.length }}</span>
            <span v-if="pageOf(active.queue, queueMax).pages > 1" class="page-mark">
              {{ pageOf(active.queue, queueMax).page }}/{{ pageOf(active.queue, queueMax).pages }}
            </span>
          </div>
          <div v-if="!active.queue.length" class="queue-empty">暂无排队</div>
          <div v-else class="queue-grid">
            <div
              v-for="slot in queueSlots(active)"
              :key="'q' + slot.index"
              class="queue-cell"
            >
              <template v-if="slot.item">
                <span class="ticket-no">{{ ticketOf(slot.item) }}</span>
                <span v-if="showField('PATIENT_NAME')" class="ticket-name">{{ nameOf(slot.item) }}</span>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="loadError && sections.length" class="load-error">{{ loadError }}</div>

    <div v-if="showMarqueeBar" class="bottom-bar">
      <div class="marquee-container">
        <div class="marquee-text">{{ screenMarqueeText }}</div>
      </div>
    </div>
  </div>
</template>

<script>
/**
 * 药房 / 检验窗口屏默认模板。一块屏只显示绑定的第一个窗口。
 * 左侧半宽只放大号序号；右上正在叫号、右下队列各自有底色。患者行只显示序号和姓名。
 * 队列固定 3 列 × 4 行，超出按滚动间隔翻页。只展示，不发起叫号。
 */
import { parseScreenBindingList } from '@/utils/screenBinding'
import screenDisplayMixin from '@/mixins/screenDisplayMixin'

const WEEKS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

/** 队列固定 3 列 × 4 行 */
const QUEUE_COLS = 3
const QUEUE_ROWS = 4
const QUEUE_MAX = QUEUE_COLS * QUEUE_ROWS

export default {
  name: 'WindowBoard',
  mixins: [screenDisplayMixin],
  props: {
    ScreensInput: { type: Object, default: () => ({}) },
    /** pharmacy | lab */
    kind: { type: String, default: 'lab' }
  },
  data() {
    return {
      dateText: '',
      timeText: '',
      attending: [],
      queue: [],
      loaded: false,
      loadError: '',
      pageCursor: 0,
      _seq: 0,
      _alive: true,
      _pollTimer: null,
      _clockTimer: null,
      _hallHandler: null
    }
  },
  computed: {
    isPharmacy() {
      return this.kind === 'pharmacy'
    },
    queueTitle() {
      return this.isPharmacy ? '取药队列' : '采血队列'
    },
    screenTitle() {
      const name = this.ScreensInput && (this.ScreensInput.Name || this.ScreensInput.name)
      if (name) return name
      return this.isPharmacy ? '药房窗口' : '检验窗口'
    },
    screenId() {
      return (this.ScreensInput && (this.ScreensInput.ID || this.ScreensInput.id)) || ''
    },
    scrollMs() {
      const sec = Number(this.ScreensInput?.ScrollTime || this.ScreensInput?.scrollTime || 5)
      return Math.max(3, sec || 5) * 1000
    },
    queueMax() {
      return QUEUE_MAX
    },
    /** 一块屏只取绑定列表中的第一个窗口 */
    active() {
      return this.sections[0] || null
    },
    sections() {
      const rooms = parseScreenBindingList(this.ScreensInput)
      const primary = rooms[0]
      if (!primary) return []
      const section = {
        id: primary.Code,
        name: primary.DeptName || '本窗口',
        ordinal: this.windowOrdinal(primary.DeptName, 0),
        current: [],
        queue: []
      }
      const accept = (row) => {
        const roomId = this.roomIdOf(row)
        return !roomId || roomId === primary.Code
      }
      this.attending.forEach((row) => {
        if (accept(row)) section.current.push(row)
      })
      this.queue.forEach((row) => {
        if (accept(row)) section.queue.push(row)
      })
      return [section]
    }
  },
  watch: {
    screenId() {
      this.pageCursor = 0
      this.refresh()
      this.startPoll()
    }
  },
  mounted() {
    this._alive = true
    this.tickClock()
    this._clockTimer = setInterval(() => this.tickClock(), 1000)
    this.bindHall()
    this.refresh()
    this.startPoll()
  },
  beforeUnmount() {
    this._alive = false
    this.stopPoll()
    if (this._clockTimer) {
      clearInterval(this._clockTimer)
      this._clockTimer = null
    }
  },
  methods: {
    tickClock() {
      const now = new Date()
      const pad = (n) => String(n).padStart(2, '0')
      this.dateText = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${WEEKS[now.getDay()]}`
      this.timeText = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
    },
    /**
     * 从诊室名取出窗口序号：优先「3号」，否则名称中的数字，再否则用绑定顺序。
     * @param {string} name
     * @param {number} index
     */
    windowOrdinal(name, index) {
      const text = String(name || '')
      const labeled = text.match(/(\d+)\s*号/)
      if (labeled) return labeled[1]
      const digit = text.match(/(\d+)/)
      if (digit) return digit[1]
      return String((index || 0) + 1)
    },
    ticketOf(row) {
      if (!row) return ''
      const showNo = row.ShowNo || row.showNo
      if (showNo) return String(showNo)
      const xh = row.xh != null ? String(row.xh) : ''
      return `${row.xhlb || ''}${xh}`
    },
    nameOf(row) {
      if (!row) return ''
      return this.formatPatientName(row.PatientName || row.patientName || '')
    },
    roomIdOf(row) {
      return String((row && (row.OfficeRoomId || row.officeRoomId)) || '')
    },
    queueSlots(sec) {
      return this.slotsOf(sec.queue, this.queueMax)
    },
    /** 固定长度的行槽，空位留给未排满的格子 */
    slotsOf(list, max) {
      const page = this.pageOf(list, max)
      const slots = []
      for (let i = 0; i < max; i += 1) {
        slots.push({ index: i + 1, item: page.items[i] || null })
      }
      return slots
    },
    /** 队列按固定格数翻页 */
    pageOf(list, max) {
      const rows = list || []
      const size = max > 0 ? max : 1
      if (rows.length <= size) return { items: rows, page: 1, pages: 1 }
      const pages = Math.ceil(rows.length / size)
      const page = this.pageCursor % pages
      return { items: rows.slice(page * size, page * size + size), page: page + 1, pages }
    },
    unwrap(res) {
      const body = (res && res.data) || {}
      if (body.success === false || (body.code && body.code !== 200)) {
        throw new Error(body.msg || body.message || '队列加载失败')
      }
      return body.data
    },
    async fetchAttending() {
      const res = await this.$axios.post('/api/CallList/GetCallListInput', { ID: this.screenId })
      const data = this.unwrap(res)
      return Array.isArray(data) ? data : []
    },
    /** 药房多传 includePreparing，把配药中的记录并进等候队列，界面不展示状态标签 */
    async fetchQueue() {
      const body = {
        index: 1,
        page: 80,
        state: 0,
        ScreensID: this.screenId
      }
      if (this.isPharmacy) body.includePreparing = true
      const res = await this.$axios.post('/api/CallList/GetCallListInputPage', body)
      const data = this.unwrap(res) || {}
      return Array.isArray(data.Data) ? data.Data : []
    },
    async refresh() {
      if (!this._alive || !this.screenId) return
      const seq = ++this._seq
      try {
        const [attending, queue] = await Promise.all([this.fetchAttending(), this.fetchQueue()])
        if (!this._alive || seq !== this._seq) return
        this.attending = attending
        this.queue = queue
        this.loadError = ''
        this.loaded = true
      } catch (err) {
        if (!this._alive || seq !== this._seq) return
        this.loadError = (err && err.message) || '队列加载失败'
        this.loaded = true
        console.error('[window-screen] refresh', err)
      }
    },
    startPoll() {
      this.stopPoll()
      if (!this.screenId) return
      this._pollTimer = setInterval(() => {
        this.pageCursor += 1
        this.refresh()
      }, this.scrollMs)
    },
    stopPoll() {
      if (this._pollTimer) {
        clearInterval(this._pollTimer)
        this._pollTimer = null
      }
    },
    bindHall() {
      if (!this.$proxy || this._hallHandler) return
      this._hallHandler = (raw) => {
        if (!this._alive) return
        let msg = raw
        if (typeof raw === 'string') {
          try {
            msg = JSON.parse(raw)
          } catch (e) {
            return
          }
        }
        if (!msg || (msg.type !== 'STATE_UPDATE' && msg.type !== 'CALL_MSG')) return
        this.refresh()
      }
      this.$proxy.on('addMessageToList', this._hallHandler)
    }
  }
}
</script>

<style scoped>
.window-board {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  background: #202124;
  color: #e8eaed;
  overflow: hidden;
  font-family: 'Inter', 'Microsoft YaHei', -apple-system, sans-serif;
}

.topbar {
  height: 8vh;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2.4vw;
  background: #1c1d21;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.45);
}

.screen-title {
  font-size: 3vh;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--screen-theme-color, #8ab4f8);
}

.clock {
  text-align: right;
  flex: none;
}

.clock-date {
  font-size: 1.6vh;
  color: #9aa0a6;
  white-space: nowrap;
}

.clock-time {
  font-size: 2.8vh;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.room {
  --module-gap: 1.2vw;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: row;
  gap: var(--module-gap);
  padding: 1.2vh 1.4vw 0;
  overflow: hidden;
}

.ordinal {
  width: 50%;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2d2e33;
  border-radius: 1.2rem;
  overflow: hidden;
  container-type: size;
}

.ordinal-no {
  font-size: 86cqh;
  line-height: 0.8;
  font-weight: 800;
  color: var(--screen-theme-color, #8ab4f8);
  font-variant-numeric: tabular-nums;
}

.ordinal-no.digits-2 {
  font-size: min(46cqw, 72cqh);
}

.ordinal-no.digits-3,
.ordinal-no.digits-4 {
  font-size: min(30cqw, 56cqh);
}

.queue-pane {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--module-gap);
}

.pane {
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 1.2vh 1.4vw 1vh;
  border-radius: 1.2rem;
  overflow: hidden;
}

.call-pane {
  flex: 0 0 38%;
  background: #34363d;
}

.wait-pane {
  flex: 1;
  background: #2a2b31;
}

.pane-head {
  flex: none;
  display: flex;
  align-items: center;
  gap: 0.8vw;
  font-size: 2.2vh;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--screen-theme-color, #8ab4f8);
  margin-bottom: 0.6vh;
}

.count {
  color: #9aa0a6;
  font-weight: 600;
}

.page-mark {
  margin-left: auto;
  color: #9aa0a6;
  font-size: 0.75em;
  letter-spacing: 0;
  font-weight: 500;
}

.call-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.call-no {
  font-size: 11vh;
  line-height: 1;
  font-weight: 800;
  color: var(--screen-theme-color, #8ab4f8);
  font-variant-numeric: tabular-nums;
}

.call-name {
  margin-top: 1.2vh;
  font-size: 7vh;
  font-weight: 700;
  line-height: 1.1;
}

.call-idle,
.queue-empty {
  color: #9aa0a6;
  letter-spacing: 0.12em;
  font-size: 4vh;
  font-weight: 700;
}

.queue-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3vh;
}

.queue-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-template-rows: repeat(4, minmax(0, 1fr));
  align-items: center;
}

.queue-cell {
  min-width: 0;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 0.6vw;
  background: none;
  border: none;
  overflow: hidden;
}

.ticket-no {
  flex: none;
  font-size: 3.6vh;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.ticket-name {
  min-width: 0;
  font-size: 3.2vh;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-block,
.load-error {
  color: #9aa0a6;
  letter-spacing: 0.08em;
}

.status-block {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 4vh;
}

.load-error {
  padding: 0.4vh 1.6vw 0;
  font-size: 1.6vh;
}

.bottom-bar {
  height: 6.5vh;
  flex: none;
  margin: 1vh 1.6vw 1.2vh;
  border-radius: 1rem;
  display: flex;
  align-items: center;
  color: #fff;
  overflow: hidden;
}

.marquee-container {
  width: 100%;
  overflow: hidden;
  white-space: nowrap;
}

.marquee-text {
  display: inline-block;
  font-size: 2.4vh;
  font-weight: 700;
  letter-spacing: 0.08em;
  padding-left: 100%;
  animation: window-marquee var(--screen-marquee-duration, 15s) linear infinite;
}

@keyframes window-marquee {
  0% { transform: translateX(0); }
  100% { transform: translateX(-100%); }
}

</style>
