<script setup lang="ts">
import { ref } from 'vue'
import { forgotPassword } from '@/api/auth'
import { errorMessage } from '@/api/http'

const email = ref('')
const loading = ref(false)
const sent = ref('')
const error = ref('')

async function submit() {
  loading.value = true
  error.value = ''
  try {
    sent.value = await forgotPassword(email.value.trim())
  } catch (e) {
    error.value = errorMessage(e, 'Não foi possível enviar o pedido.')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="mx-auto max-w-md">
    <div class="card bg-base-100 border-base-300 border shadow-sm">
      <div v-if="sent" class="card-body gap-4">
        <h1 class="card-title text-2xl">Verifique seu e-mail</h1>
        <p>{{ sent }}</p>
        <p class="text-base-content/70 text-sm">O link vale por 30 minutos.</p>
        <RouterLink :to="{ name: 'login' }" class="btn btn-primary">Voltar ao login</RouterLink>
      </div>
      <form v-else class="card-body gap-4" @submit.prevent="submit">
        <h1 class="card-title text-2xl">Esqueci minha senha</h1>
        <p class="text-base-content/70 text-sm">
          Informe seu e-mail e enviaremos um link para criar uma nova senha.
        </p>
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
        <div v-if="error" role="alert" class="alert alert-error alert-soft">{{ error }}</div>
        <button class="btn btn-primary" :disabled="loading">
          <span v-if="loading" class="loading loading-spinner loading-sm"></span>
          Enviar link
        </button>
        <RouterLink :to="{ name: 'login' }" class="link text-center text-sm"
          >Voltar ao login</RouterLink
        >
      </form>
    </div>
  </div>
</template>
