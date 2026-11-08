<template>
  <div class="icon-body">
    <el-input
      v-model="keyword"
      class="icon-body__search"
      placeholder="搜索图标名称"
      clearable
      size="small"
    />
    <div v-if="iconList.length" class="icon-list">
      <button
        v-for="item in iconList"
        :key="item"
        type="button"
        class="icon-list-item"
        :class="{ 'is-active': item === value }"
        :title="item"
        @click="selectedIcon(item)"
      >
        <i v-if="isFontIcon(item)" :class="item" class="icon-list-item__icon" />
        <svg-icon v-else :icon-class="item" class="icon-list-item__icon" />
        <span class="icon-list-item__name">{{ iconLabel(item) }}</span>
      </button>
    </div>
    <div v-else class="icon-body__empty">没有匹配的图标</div>
  </div>
</template>

<script>
/**
 * 菜单图标选择网格。
 * 消费者：IconPickerInput。
 * 依赖：icons/catalog（本地 SVG + Element 字体图标）。
 */
import { iconLabel, isFontIcon, menuIcons } from '@/icons/catalog'

export default {
  name: 'IconSelect',
  props: {
    value: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      keyword: ''
    }
  },
  computed: {
    iconList() {
      const query = this.keyword.trim().toLowerCase()
      if (!query) return menuIcons
      return menuIcons.filter(name => name.toLowerCase().includes(query) || iconLabel(name).toLowerCase().includes(query))
    }
  },
  methods: {
    isFontIcon,
    iconLabel,
    selectedIcon(name) {
      this.$emit('selected', name)
    }
  }
}
</script>

<style lang="scss" scoped>
.icon-body {
  width: 100%;
  box-sizing: border-box;
}

.icon-body__search {
  margin-bottom: 8px;
}

.icon-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2px 8px;
  max-height: 280px;
  overflow-x: hidden;
  overflow-y: auto;
}

.icon-list-item {
  display: flex;
  align-items: center;
  min-width: 0;
  height: 32px;
  margin: 0;
  padding: 0 8px;
  gap: 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #3c4043;
  font: inherit;
  text-align: left;
  cursor: pointer;

  &:hover,
  &.is-active {
    background: #f1f3f4;
  }
}

.icon-list-item__icon {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  font-size: 16px;
  line-height: 16px;
  text-align: center;
}

.icon-list-item__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  line-height: 16px;
}

.icon-body__empty {
  padding: 24px 0;
  text-align: center;
  color: #80868b;
  font-size: 13px;
}
</style>
