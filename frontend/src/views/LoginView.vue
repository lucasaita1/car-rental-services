<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import AppIcon from '@/components/AppIcon.vue'
import AuthShell from '@/components/auth/AuthShell.vue'
import PasswordInput from '@/components/auth/PasswordInput.vue'
import { errorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { firstName } from '@/utils/format'

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
    toast.success(`Bem-vindo de volta, ${firstName(auth.userName)}!`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : null
    router.push(redirect ?? { name: 'home' })
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
  <AuthShell title="Bem-vindo de volta" subtitle="Entre para reservar e acompanhar suas locações.">
    <form class="space-y-5" @submit.prevent="submit">
      <fieldset class="fieldset gap-2">
        <legend class="fieldset-legend text-sm">E-mail</legend>
        <label class="input input-lg w-full">
          <AppIcon name="envelope" class="text-base-content/50 size-5" />
          <input
            v-model="email"
            type="email"
            class="grow"
            placeholder="voce@email.com"
            autocomplete="email"
            required
          />
        </label>
      </fieldset>

      <fieldset class="fieldset gap-2">
        <div class="flex items-center justify-between">
          <legend class="fieldset-legend text-sm">Senha</legend>
          <RouterLink
            :to="{ name: 'forgot-password' }"
            class="link link-primary link-hover text-sm font-medium"
          >
            Esqueci minha senha
          </RouterLink>
        </div>
        <PasswordInput v-model="password" />
      </fieldset>

      <div v-if="error" role="alert" class="alert alert-error alert-soft">
        <AppIcon name="alert" class="size-5" />
        <span>{{ error }}</span>
      </div>

      <button class="btn btn-primary btn-lg w-full" :disabled="loading">
        <span v-if="loading" class="loading loading-spinner loading-sm"></span>
        Entrar
        <AppIcon v-if="!loading" name="arrowRight" class="size-5" />
      </button>
    </form>

    <div class="divider text-base-content/50 my-8 text-sm">Novo por aqui?</div>

    <RouterLink :to="{ name: 'register' }" class="btn btn-outline btn-lg w-full">
      Criar conta grátis
    </RouterLink>
  </AuthShell>
</template>
