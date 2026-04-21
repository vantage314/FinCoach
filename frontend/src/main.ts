import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './store'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// FinCoach 主题体系（按顺序加载：tokens → Element 覆盖 → 通用样式类）
import './theme/variables.scss'
import './theme/element-overwrite.scss'
import './theme/fc-common.scss'

// 全局基础样式
import './style.css'

// Fc* 暗色 Wrapper 组件
import FcComponents from './components/fc'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.use(FcComponents)

app.mount('#app')
