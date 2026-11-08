<template>
  <div class="h5-container">
    <!-- Screen 1: Golden 3 Seconds -->
    <section class="h5-section screen-1">
      <div class="animated-bg">
        <div class="glow-orb orb-1" />
        <div class="glow-orb orb-2" />
      </div>
      <div class="content">
        <img src="@/assets/logo.png" alt="欢匠 Logo" class="h5-logo">
        <h1 class="main-title">欢匠·序流</h1>
        <h2 class="sub-title">做欢快的医疗信息化工匠</h2>
        <p class="desc">南京吾爱网络技术有限公司 倾力打造</p>
      </div>
      <div class="scroll-indicator">
        <span>向下滑动探索</span>
        <i class="el-icon-arrow-down bounce" />
      </div>
    </section>

    <!-- Screen 2: Pain Points -->
    <section class="h5-section screen-2">
      <div class="content left-align">
        <h2 class="section-title">信息孤岛让诊疗断层？</h2>
        <h3 class="section-subtitle">繁杂的流程，正在消耗医护热情</h3>
        <ul class="pain-points">
          <li><i class="el-icon-close" /> 硬件老旧，接口不通</li>
          <li><i class="el-icon-close" /> 患者拥挤，导诊混乱</li>
          <li><i class="el-icon-close" /> 缺乏全量数据分析支撑</li>
        </ul>
        <div class="illustration">
          <div class="broken-line" />
          <span class="emoji">🏥 📉 🙍‍♂️</span>
        </div>
      </div>
    </section>

    <!-- Screen 3: The Solution -->
    <section class="h5-section screen-3">
      <div class="content right-align">
        <h2 class="section-title text-highlight">破局：数智化全栈架构</h2>
        <p class="section-desc">
          我们不仅是一套排队叫号系统，更是打通数据孤岛的神经网络。
          <br><br>
          通过全病程运营平台打通底层数据流，智能导诊与室内导航极致优化患者动线。在欢匠，一切都显得那么从容不迫。
        </p>
        <div class="feature-cards">
          <div class="f-card">智能全感知预警</div>
          <div class="f-card">跨厂商无缝整合</div>
        </div>
      </div>
    </section>

    <!-- Screen 4: Craftsmanship -->
    <section class="h5-section screen-4">
      <div class="content center-align">
        <h2 class="section-title">极致工匠细节打磨</h2>
        <div class="mockup-container">
          <div class="mockup-screen">
            <div class="mockup-header">欢匠·序流 控制台</div>
            <div class="mockup-body">
              <div class="m-line w-80" />
              <div class="m-line w-60" />
              <div class="m-line w-90 highlight" />
              <div class="m-boxes">
                <div class="m-box" />
                <div class="m-box" />
                <div class="m-box active" />
              </div>
            </div>
          </div>
          <div class="floating-tags">
            <span class="tag t1">✨ 智能辅助决策</span>
            <span class="tag t2">⚡ 毫秒级防抖响应</span>
            <span class="tag t3">🛡️ 动态熔断沙盒</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Screen 5: CTA -->
    <section class="h5-section screen-5">
      <div class="content">
        <h2 class="section-title">看见未来，触手可及</h2>
        <p class="final-desc">沉浸式体验满载运转的系统沙盒，带您领略欢快的医疗工匠魅力。</p>

        <div class="cta-wrapper">
          <button class="cta-btn breathing-btn" @click="enterDemo">
            即刻进入 [欢匠] 沉浸式产品演示
          </button>
        </div>

        <div class="contact-links">
          <a href="#" @click.prevent="showContact">获取专属私有化方案</a>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
export default {
  name: 'H5Promo',
  methods: {
    enterDemo() {
      // Navigate to login or directly to dashboard if auth is mocked
      this.$router.push('/login')
    },
    showContact() {
      this.$message({
        message: '感谢您的关注！请致电 400-888-8888 或发送邮件至 hello@wuai.com 沟通专属实施方案。',
        type: 'success',
        duration: 5000
      })
    }
  }
}
</script>

<style scoped>
/* Base Styles */
.h5-container {
  width: 100%;
  height: 100vh;
  overflow-y: scroll;
  scroll-snap-type: y mandatory;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  background-color: #f7f9fa;
  color: #333;
}
.h5-section {
  width: 100%;
  height: 100vh;
  scroll-snap-align: start;
  position: relative;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0 20px;
  box-sizing: border-box;
}

/* Animations */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes bounce {
  0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-10px); }
  60% { transform: translateY(-5px); }
}
@keyframes breathe {
  0% { box-shadow: 0 0 0 0 rgba(255, 153, 0, 0.4); }
  70% { box-shadow: 0 0 0 15px rgba(255, 153, 0, 0); }
  100% { box-shadow: 0 0 0 0 rgba(255, 153, 0, 0); }
}
@keyframes float {
  0% { transform: translateY(0px); }
  50% { transform: translateY(-15px); }
  100% { transform: translateY(0px); }
}

/* Screen 1 */
.screen-1 {
  background: linear-gradient(135deg, #1890ff 0%, #001529 100%);
  color: white;
  text-align: center;
}
.animated-bg {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  overflow: hidden;
  z-index: 1;
}
.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.6;
}
.orb-1 { width: 300px; height: 300px; background: #FF9900; top: -100px; left: -50px; animation: float 6s ease-in-out infinite; }
.orb-2 { width: 400px; height: 400px; background: #00e676; bottom: -150px; right: -100px; animation: float 8s ease-in-out infinite reverse; }

.screen-1 .content {
  z-index: 2;
  animation: fadeInUp 1s ease-out forwards;
}
.h5-logo {
  width: 100px;
  height: 100px;
  border-radius: 20px;
  margin-bottom: 20px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.3);
}
.main-title {
  font-size: 3rem;
  font-weight: 800;
  margin: 0;
  letter-spacing: 2px;
}
.sub-title {
  font-size: 1.5rem;
  font-weight: 300;
  margin: 10px 0 20px;
  color: #e6f7ff;
}
.desc {
  font-size: 0.9rem;
  color: rgba(255,255,255,0.7);
}
.scroll-indicator {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2;
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 0.8rem;
  opacity: 0.8;
}
.scroll-indicator .bounce {
  margin-top: 8px;
  font-size: 1.5rem;
  animation: bounce 2s infinite;
}

/* Screen 2 */
.screen-2 {
  background: white;
}
.left-align { text-align: left; width: 100%; max-width: 400px; }
.section-title { font-size: 2rem; font-weight: bold; color: #1f2d3d; margin-bottom: 10px; }
.section-subtitle { font-size: 1.2rem; color: #8492a6; margin-bottom: 30px; font-weight: normal; }
.pain-points { list-style: none; padding: 0; margin-bottom: 40px; }
.pain-points li { margin-bottom: 15px; font-size: 1.1rem; color: #475669; }
.pain-points i { color: #ff4949; margin-right: 10px; }
.illustration { font-size: 3rem; text-align: center; opacity: 0.5; }

/* Screen 3 */
.screen-3 {
  background: #f0f9eb;
}
.right-align { text-align: right; width: 100%; max-width: 400px; }
.text-highlight { color: #13ce66; }
.section-desc { font-size: 1.1rem; line-height: 1.6; color: #5e6d82; text-align: justify; margin-bottom: 30px; }
.feature-cards { display: flex; gap: 10px; justify-content: flex-end; flex-wrap: wrap; }
.f-card { background: white; padding: 10px 20px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); font-weight: bold; color: #13ce66; border: 1px solid #e1f3d8;}

/* Screen 4 */
.screen-4 {
  background: #fdf6ec;
}
.center-align { text-align: center; width: 100%; max-width: 500px; }
.mockup-container { position: relative; margin-top: 40px; }
.mockup-screen {
  background: white; border-radius: 12px; box-shadow: 0 20px 40px rgba(0,0,0,0.1);
  overflow: hidden; width: 300px; margin: 0 auto; border: 4px solid #333;
}
.mockup-header { background: #f4f4f5; font-size: 0.8rem; padding: 10px; color: #909399; border-bottom: 1px solid #ebeef5; }
.mockup-body { padding: 20px; text-align: left; }
.m-line { height: 8px; background: #e4e7ed; border-radius: 4px; margin-bottom: 12px; }
.w-80 { width: 80%; } .w-60 { width: 60%; } .w-90 { width: 90%; }
.m-line.highlight { background: #e6a23c; width: 40%; }
.m-boxes { display: flex; gap: 10px; margin-top: 20px; }
.m-box { flex: 1; height: 40px; background: #f4f4f5; border-radius: 6px; }
.m-box.active { background: #e6a23c; }

.floating-tags .tag {
  position: absolute; background: white; padding: 6px 12px; border-radius: 20px;
  font-size: 0.8rem; font-weight: bold; box-shadow: 0 4px 10px rgba(0,0,0,0.08);
  animation: float 4s ease-in-out infinite; color: #e6a23c;
}
.t1 { top: -10px; left: -20px; animation-delay: 0s; }
.t2 { bottom: 60px; right: -30px; animation-delay: 1s; }
.t3 { bottom: -15px; left: 10px; animation-delay: 2s; }

/* Screen 5 */
.screen-5 {
  background: #1890ff;
  color: white;
  text-align: center;
}
.screen-5 .section-title { color: white; margin-bottom: 20px; font-size: 2.5rem; }
.final-desc { font-size: 1.2rem; margin-bottom: 50px; opacity: 0.9; }
.cta-wrapper { margin-bottom: 40px; }
.cta-btn {
  background: #FF9900;
  color: white;
  border: none;
  padding: 18px 30px;
  font-size: 1.2rem;
  font-weight: bold;
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.3s;
}
.breathing-btn {
  animation: breathe 2s infinite;
}
.cta-btn:hover {
  background: #ffb84d;
  transform: scale(1.05);
}
.contact-links a { color: rgba(255,255,255,0.8); text-decoration: underline; font-size: 0.9rem; }
.contact-links a:hover { color: white; }

/* Responsiveness */
@media (max-height: 600px) {
  .main-title { font-size: 2rem; }
  .mockup-screen { width: 250px; }
  .section-title { font-size: 1.5rem; }
}
</style>
