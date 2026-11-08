<template>
  <div class="error-page">
    <div class="error-content">
      <div class="error-code">404</div>
      <h2 class="error-title">页面不存在</h2>
      <p class="error-desc">{{ message }}</p>
      <a href="/" class="error-btn">返回首页</a>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Page404',
  computed: {
    message() {
      return '您访问的页面不存在或已被移除，请检查地址是否正确。'
    }
  }
}
</script>

<style lang="scss" scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #F8F9FA;
}

.error-content {
  text-align: center;
  padding: 40px;
}

.error-code {
  font-size: 120px;
  font-weight: 900;
  color: #4285F4;
  line-height: 1;
  margin-bottom: 16px;
  letter-spacing: -4px;
  opacity: 0.8;
}

.error-title {
  font-size: 28px;
  font-weight: 700;
  color: #202124;
  margin: 0 0 12px;
}

.error-desc {
  font-size: 15px;
  color: #5F6368;
  margin: 0 0 32px;
  max-width: 400px;
}

.error-btn {
  display: inline-block;
  padding: 12px 32px;
  background: #4285F4;
  color: #fff;
  border-radius: 24px;
  text-decoration: none;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(66, 133, 244, 0.3);

  &:hover {
    background: #3367D6;
    box-shadow: 0 4px 16px rgba(66, 133, 244, 0.4);
    transform: translateY(-1px);
  }
}
</style>
