<template>
  <div class="auth-wrapper">
    <!-- 背景光晕 -->
    <div class="glow-orb glow-1"></div>
    <div class="glow-orb glow-2"></div>
    
    <div class="auth-container">
      <div class="glass-card">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.auth-wrapper {
  position: relative;
  min-height: 100vh;
  width: 100%;
  background-color: var(--fc-bg);
  background-image: radial-gradient(at 0% 0%, rgba(30, 41, 59, 1) 0, transparent 50%),
                    radial-gradient(at 100% 100%, rgba(30, 41, 59, 1) 0, transparent 50%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  z-index: 1;
  opacity: 0.5;
}

.glow-1 {
  width: 400px;
  height: 400px;
  background: var(--fc-primary);
  top: -100px;
  left: -100px;
  animation: drift 15s infinite alternate;
}

.glow-2 {
  width: 500px;
  height: 500px;
  background: var(--fc-success);
  bottom: -150px;
  right: -150px;
  animation: drift 20s infinite alternate-reverse;
}

@keyframes drift {
  from { transform: translate(0, 0); }
  to { transform: translate(100px, 50px); }
}

.auth-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 450px;
  padding: 20px;
}

.glass-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--fc-border);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.3);
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
