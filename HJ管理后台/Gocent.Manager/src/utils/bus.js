import mitt from 'mitt'

/**
 * 兼容 Vue 2 事件总线 API（$on / $off / $emit），供叫号刷新、标签缓存等跨组件通知。
 * 消费者：main.js 挂到 globalProperties.$Bus；各业务页 this.$Bus.$on
 */
const emitter = mitt()

const bus = {
  $on: (event, handler) => emitter.on(event, handler),
  $off: (event, handler) => emitter.off(event, handler),
  $emit: (event, ...args) => emitter.emit(event, ...args)
}

export default bus
