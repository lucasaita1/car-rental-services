import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { installSessionExpiryHandler } from './api/http'
import { useAuthStore } from './stores/auth'
import { useToastStore } from './stores/toast'

const app = createApp(App)

app.use(createPinia())
app.use(router)

installSessionExpiryHandler(router, () => {
  useAuthStore().clearSession()
  useToastStore().warning('Sua sessão expirou. Entre novamente.')
})

useAuthStore().loadProfile()

app.mount('#app')
