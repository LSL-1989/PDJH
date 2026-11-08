<script setup>
/**
 * 9:16 竖屏缩放壳（450×800 设计稿，transform 等比适配物理屏）
 * 消费者：BindingGuide（候诊大屏绑定页）；业务大屏模板不包此壳。
 * 布局对齐自助签到机：fixed + --viewport-height。
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { useScreenScale } from '@/composables/useScreenScale'

const scaleWrapRef = ref(null)
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
  background: #1e1f25;
  overflow: hidden;
}

.screen-scale {
  width: 450px;
  height: 800px;
  flex-shrink: 0;
  transform: scale(var(--screen-scale, 1));
  transform-origin: center center;
}
</style>
