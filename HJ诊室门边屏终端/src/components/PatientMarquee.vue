<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  text: string
}>()

const wrapRef = ref<HTMLElement | null>(null)
const trackRef = ref<HTMLElement | null>(null)
const needScroll = ref(false)
let observer: ResizeObserver | null = null

const measure = () => {
  const wrap = wrapRef.value
  const track = trackRef.value
  if (!wrap || !track) {
    needScroll.value = false
    return
  }
  const textEl = track.querySelector('.patient-marquee__text') as HTMLElement | null
  if (!textEl) {
    needScroll.value = false
    return
  }
  needScroll.value = textEl.scrollWidth > wrap.clientWidth + 2
}

onMounted(() => {
  measure()
  if (wrapRef.value) {
    observer = new ResizeObserver(measure)
    observer.observe(wrapRef.value)
  }
})

onUnmounted(() => {
  observer?.disconnect()
})

watch(() => props.text, () => {
  requestAnimationFrame(measure)
})
</script>

<template>
  <div ref="wrapRef" class="patient-marquee">
    <div
      ref="trackRef"
      class="patient-marquee__track"
      :class="{ 'is-scroll': needScroll }"
    >
      <span class="patient-marquee__text">{{ text }}</span>
      <span v-if="needScroll" class="patient-marquee__text" aria-hidden="true">{{ text }}</span>
    </div>
  </div>
</template>

<style scoped>
.patient-marquee {
  width: 100%;
  overflow: hidden;
  text-align: center;
}

.patient-marquee__track {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  max-width: 100%;
}

.patient-marquee__track.is-scroll {
  justify-content: flex-start;
  animation: patient-scroll 10s linear infinite;
}

.patient-marquee__text {
  flex-shrink: 0;
  white-space: nowrap;
  font-size: inherit;
  font-weight: inherit;
  color: inherit;
}

.patient-marquee__track.is-scroll .patient-marquee__text + .patient-marquee__text {
  padding-left: 48px;
}

@keyframes patient-scroll {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-50%);
  }
}
</style>
