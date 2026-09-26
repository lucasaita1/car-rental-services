<script setup lang="ts">
import { ref } from 'vue'
import AuthShell from '@/components/auth/AuthShell.vue'
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
  <AuthShell
    :title="sent ? 'Verifique seu e-mail' : 'Esqueceu a senha?'"
    :subtitle="sent ? '' : 'Informe seu e-mail e enviaremos um link para criar uma nova senha.'"
  >
    <div v-if="sent" class="flex flex-col gap-4">
      <p>{{ sent }}</p>
      <p class="text-base-content/70 text-sm">O link vale por 30 minutos.</p>
      <RouterLink :to="{ name: 'login' }" class="btn btn-primary">Voltar ao login</RouterLink>
    </div>
    <form v-else class="flex flex-col gap-4" @submit.prevent="submit">
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
  </AuthShell>
</template>
