<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

const DEFAULT_BULLETIN = '温馨提示：请保持安静'

const props = defineProps<{
  bulletin?: string
}>()

const wrapRef = ref<HTMLElement | null>(null)
const textRef = ref<HTMLElement | null>(null)
const needScroll = ref(false)
let observer: ResizeObserver | null = null

const displayText = computed(() => {
  const raw = (props.bulletin || '').trim()
  return raw || DEFAULT_BULLETIN
})

const measure = () => {
  const wrap = wrapRef.value
  const text = textRef.value
  if (!wrap || !text) {
    needScroll.value = false
    return
  }
  needScroll.value = text.scrollWidth > wrap.clientWidth + 2
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

watch(displayText, () => {
  requestAnimationFrame(measure)
})
</script>

<template>
  <section class="bulletin-bar clinic-panel">
    <div ref="wrapRef" class="bulletin-wrap">
      <div
        class="bulletin-track"
        :class="{ 'is-scroll': needScroll }"
      >
        <span ref="textRef" class="bulletin-text">{{ displayText }}</span>
        <span v-if="needScroll" class="bulletin-text" aria-hidden="true">{{ displayText }}</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.bulletin-bar {
  flex: 0 0 auto;
  min-height: 44px;
  padding: 10px 14px;
  background: var(--color-bulletin-bg);
  border-top: 1px solid var(--color-border);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bulletin-wrap {
  width: 100%;
  overflow: hidden;
  text-align: center;
}

.bulletin-track {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  max-width: 100%;
}

.bulletin-track.is-scroll {
  justify-content: flex-start;
  animation: bulletin-scroll 16s linear infinite;
}

.bulletin-text {
  flex-shrink: 0;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.bulletin-track.is-scroll .bulletin-text + .bulletin-text {
  padding-left: 48px;
}

@keyframes bulletin-scroll {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-50%);
  }
}
</style>
