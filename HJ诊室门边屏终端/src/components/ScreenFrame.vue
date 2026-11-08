<script setup lang="ts">
/**
 * 9:16 竖屏缩放壳（450×800 设计稿，transform 等比适配物理屏）
 * 消费者：App.vue 包裹全部路由页面
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { useScreenScale } from '../composables/useScreenScale'

const scaleWrapRef = ref<HTMLElement | null>(null)
const { bindScaleListeners, unbindScaleListeners } = useScreenScale(scaleWrapRef)

onMounted(bindScaleListeners)
onUnmounted(unbindScaleListeners)
</script>

<template>
  <div class="screen-frame">
    <div ref="scaleWrapRef" class="screen-scale">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.screen-frame {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  width: 100%;
  height: var(--viewport-height, 100vh);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-frame-outer);
  overflow: hidden;
  font-family: var(--font-sans);
}

.screen-scale {
  width: 450px;
  height: 800px;
  flex-shrink: 0;
  transform: scale(var(--screen-scale, 1));
  transform-origin: center center;
}
</style>
