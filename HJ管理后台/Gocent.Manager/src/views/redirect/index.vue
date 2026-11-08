<template>
  <div>
    <router-view />
  </div>
</template>

<script>
/** 刷新当前页：先进入本路由再立刻 replace 回原 path，避开 keep-alive */
export default {
  created() {
    const { params, query } = this.$route
    const path = Array.isArray(params.path) ? params.path.join('/') : params.path
    this.$router.replace({ path: '/' + path, query })
  }
}
</script>
