import { createApp } from 'vue'
import App from './App.vue'

// 引入自定义路由
import router from './router.js'

const app = createApp(App)

// 注册路由
app.use(router)

app.mount('#app')
