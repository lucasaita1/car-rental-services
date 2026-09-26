<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthShell from '@/components/auth/AuthShell.vue'
import { resetPassword } from '@/api/auth'
import { errorMessage } from '@/api/http'
import { useToastStore } from '@/stores/toast'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()

const token = computed(() => (typeof route.query.token === 'string' ? route.query.token : ''))
const password = ref('')
const confirm = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (password.value.length < 8) {
    error.value = 'A senha precisa ter pelo menos 8 caracteres.'
    return
  }
  if (password.value !== confirm.value) {
    error.value = 'As senhas não conferem.'
    return
  }
  loading.value = true
  try {
    await resetPassword(token.value, password.value)
    toast.success('Senha redefinida. Entre com a nova senha.')
    router.push({ name: 'login' })
  } catch (e) {
    error.value = errorMessage(e, 'Não foi possível redefinir a senha.')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell
    :title="token ? 'Crie uma nova senha' : 'Link inválido'"
    :subtitle="token ? 'Use pelo menos 8 caracteres.' : ''"
  >
    <div v-if="!token" class="flex flex-col gap-4">
      <p>Este link de redefinição está incompleto. Peça um novo.</p>
      <RouterLink :to="{ name: 'forgot-password' }" class="btn btn-primary"
        >Pedir novo link</RouterLink
      >
    </div>
    <form v-else class="flex flex-col gap-4" @submit.prevent="submit">
      <label class="floating-label">
        <span>Nova senha</span>
        <input
          v-model="password"
          type="password"
          class="input w-full"
          placeholder="Nova senha"
          autocomplete="new-password"
          required
        />
      </label>
      <label class="floating-label">
        <span>Confirmar senha</span>
        <input
          v-model="confirm"
          type="password"
          class="input w-full"
          placeholder="Confirmar senha"
          autocomplete="new-password"
          required
        />
      </label>
      <div v-if="error" role="alert" class="alert alert-error alert-soft">
        <span>{{ error }}</span>
        <RouterLink :to="{ name: 'forgot-password' }" class="link">Pedir novo link</RouterLink>
      </div>
      <button class="btn btn-primary" :disabled="loading">
        <span v-if="loading" class="loading loading-spinner loading-sm"></span>
        Redefinir senha
      </button>
    </form>
  </AuthShell>
</template>
