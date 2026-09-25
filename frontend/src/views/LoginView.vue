<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { errorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToastStore()
const route = useRoute()
const router = useRouter()

const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  loading.value = true
  error.value = ''
  try {
    await auth.login(email.value.trim(), password.value)
    toast.success(`Bem-vindo, ${auth.userName}!`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : null
    router.push(redirect ?? (auth.isAdmin ? { name: 'admin-cars' } : { name: 'catalog' }))
  } catch (e) {
    error.value =
      axios.isAxiosError(e) && e.response?.status === 401
        ? 'E-mail ou senha inválidos.'
        : errorMessage(e, 'Não foi possível entrar.')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="mx-auto max-w-md">
    <div class="card bg-base-100 border border-base-300 shadow-sm">
      <form class="card-body gap-4" @submit.prevent="submit">
        <h1 class="card-title text-2xl">Entrar</h1>
        <label class="floating-label">
          <span>E-mail</span>
          <input
            v-model="email"
            type="email"
            class="input w-full"
            placeholder="E-mail"
            autocomplete="email"
            required
          />
        </label>
        <label class="floating-label">
          <span>Senha</span>
          <input
            v-model="password"
            type="password"
            class="input w-full"
            placeholder="Senha"
            autocomplete="current-password"
            required
          />
        </label>
        <div v-if="error" role="alert" class="alert alert-error alert-soft">{{ error }}</div>
        <button class="btn btn-primary" :disabled="loading">
          <span v-if="loading" class="loading loading-spinner loading-sm"></span>
          Entrar
        </button>
        <p class="text-center text-sm">
          Não tem conta?
          <RouterLink :to="{ name: 'register' }" class="link link-primary">Crie a sua</RouterLink>
        </p>
      </form>
    </div>
  </div>
</template>
