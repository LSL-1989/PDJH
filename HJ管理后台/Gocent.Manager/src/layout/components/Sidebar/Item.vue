<script>
import { isFontIcon } from '@/icons/catalog'

export default {
  name: 'MenuItem',
  props: {
    icon: {
      type: String,
      default: ''
    },
    title: {
      type: String,
      default: ''
    }
  },
  methods: {
    isFontIcon
  }
}
</script>

<template>
  <span>
    <i v-if="isFontIcon(icon)" :class="[icon, 'sub-el-icon']" />
    <svg-icon v-else-if="icon" :icon-class="icon" />
    <span v-if="title">{{ title }}</span>
  </span>
</template>

<style scoped>
.sub-el-icon {
  color: currentColor;
  width: 1em;
  height: 1em;
}
</style>
