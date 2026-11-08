/**
 * 候诊大屏叫号弹窗：放大高亮展示当前被叫患者
 * 消费者：各模板 index.vue（经 pdjhScreenMixin 驱动）
 */
<template>
  <Transition name="call-popup">
    <div v-if="visible" class="call-popup" role="alertdialog" aria-live="assertive">
      <div class="call-popup__mask" />
      <div class="call-popup__card">
        <div class="call-popup__badge">
          <span class="call-popup__pulse" />
          正在呼叫
        </div>
        <div class="call-popup__no">{{ showNoText }}</div>
        <div class="call-popup__name">{{ patientName || '—' }}</div>
        <div v-if="office" class="call-popup__office">请至 {{ office }}</div>
        <div v-if="callText" class="call-popup__hint">{{ callText }}</div>
      </div>
    </div>
  </Transition>
</template>

<script>
export default {
  name: 'CallPopup',
  props: {
    visible: { type: Boolean, default: false },
    showNo: { type: [String, Number], default: '' },
    patientName: { type: String, default: '' },
    office: { type: String, default: '' },
    callText: { type: String, default: '' }
  },
  computed: {
    showNoText() {
      const no = this.showNo == null || this.showNo === '' ? '' : String(this.showNo)
      if (!no) return '—'
      return /号$/.test(no) ? no : `${no}号`
    }
  }
}
</script>

<style lang="scss" scoped>
.call-popup {
  position: fixed;
  inset: 0;
  z-index: 9000;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.call-popup__mask {
  position: absolute;
  inset: 0;
  background: rgba(10, 12, 18, 0.72);
}

.call-popup__card {
  position: relative;
  z-index: 1;
  width: min(78vw, 920px);
  padding: 5vh 4vw 4.5vh;
  box-sizing: border-box;
  text-align: center;
  border-radius: 1.6rem;
  background: linear-gradient(165deg, #1f3a5f 0%, #1a2332 48%, #141820 100%);
  border: 2px solid rgba(252, 163, 17, 0.55);
  box-shadow:
    0 0 0 6px rgba(252, 163, 17, 0.12),
    0 24px 64px rgba(0, 0, 0, 0.55),
    0 0 48px rgba(138, 180, 248, 0.18);
  animation: call-card-glow 1.6s ease-in-out infinite;
}

.call-popup__badge {
  display: inline-flex;
  align-items: center;
  gap: 0.7vw;
  margin-bottom: 2.2vh;
  padding: 0.6vh 1.6vw;
  border-radius: 999px;
  font-size: 1.6vw;
  font-weight: 700;
  letter-spacing: 0.2em;
  color: #202124;
  background: #fca311;
}

.call-popup__pulse {
  width: 0.7vw;
  height: 0.7vw;
  border-radius: 50%;
  background: #202124;
  animation: call-pulse 1s infinite;
}

.call-popup__no {
  font-size: 6.5vw;
  font-weight: 900;
  line-height: 1.05;
  color: #fca311;
  letter-spacing: 0.06em;
  text-shadow: 0 0 28px rgba(252, 163, 17, 0.55);
}

.call-popup__name {
  margin-top: 1.6vh;
  font-size: 5.2vw;
  font-weight: 800;
  line-height: 1.15;
  color: #fff;
  letter-spacing: 0.12em;
  text-shadow: 0 4px 24px rgba(0, 0, 0, 0.45);
}

.call-popup__office {
  margin-top: 2.4vh;
  font-size: 2.6vw;
  font-weight: 700;
  color: #8ab4f8;
  letter-spacing: 0.08em;
}

.call-popup__hint {
  margin-top: 1.8vh;
  font-size: 1.8vw;
  font-weight: 600;
  color: rgba(232, 234, 237, 0.88);
  line-height: 1.4;
}

.call-popup-enter-active,
.call-popup-leave-active {
  transition: opacity 0.28s ease;
}
.call-popup-enter-active .call-popup__card,
.call-popup-leave-active .call-popup__card {
  transition: transform 0.28s ease, opacity 0.28s ease;
}
.call-popup-enter-from,
.call-popup-leave-to {
  opacity: 0;
}
.call-popup-enter-from .call-popup__card,
.call-popup-leave-to .call-popup__card {
  opacity: 0;
  transform: scale(0.88);
}

@keyframes call-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.25;
  }
}

@keyframes call-card-glow {
  0%,
  100% {
    box-shadow:
      0 0 0 6px rgba(252, 163, 17, 0.12),
      0 24px 64px rgba(0, 0, 0, 0.55),
      0 0 48px rgba(138, 180, 248, 0.18);
  }
  50% {
    box-shadow:
      0 0 0 10px rgba(252, 163, 17, 0.22),
      0 24px 64px rgba(0, 0, 0, 0.55),
      0 0 64px rgba(252, 163, 17, 0.28);
  }
}
</style>
