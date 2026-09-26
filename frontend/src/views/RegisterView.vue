<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import CnhDocumentField from '@/components/CnhDocumentField.vue'
import PhotoPicker from '@/components/PhotoPicker.vue'
import AuthShell from '@/components/auth/AuthShell.vue'
import { errorMessage } from '@/api/http'
import { uploadMyCnhDocument, uploadMyPhoto } from '@/api/users'
import type { RegisterPayload } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

const form = reactive<RegisterPayload>({ name: '', email: '', cpf: '', cnh: '', password: '' })
const confirmPassword = ref('')
const photo = ref<File | null>(null)
const cnhDocument = ref<File | null>(null)
const loading = ref(false)
const error = ref('')

function validate(): string {
  if (form.password.length < 8) return 'A senha precisa ter pelo menos 8 caracteres.'
  if (form.password !== confirmPassword.value) return 'As senhas não conferem.'
  if (!/^\d{11}$/.test(form.cpf)) return 'CPF deve ter 11 dígitos, sem pontuação.'
  if (!/^\d{11}$/.test(form.cnh)) return 'CNH deve ter 11 dígitos.'
  if (!cnhDocument.value) return 'Anexe a sua CNH em PDF.'
  return ''
}

async function submit() {
  error.value = validate()
  if (error.value) return
  loading.value = true
  try {
    await auth.register({ ...form, email: form.email.trim() })
    if (photo.value) {
      try {
        auth.profile = await uploadMyPhoto(photo.value)
      } catch (e) {
        toast.warning(errorMessage(e, 'Conta criada, mas a foto não foi enviada.'))
      }
    }
    try {
      auth.profile = await uploadMyCnhDocument(cnhDocument.value!)
    } catch (e) {
      toast.warning(errorMessage(e, 'Conta criada, mas a CNH não foi enviada. Envie pelo perfil.'))
    }
    toast.success('Conta criada! Você já está conectado.')
    router.push({ name: 'home' })
  } catch (e) {
    error.value = errorMessage(e, 'Não foi possível criar a conta.')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell
    wide
    title="Crie sua conta"
    subtitle="Leva menos de um minuto. Depois é só escolher o carro."
  >
    <form class="flex flex-col gap-4" @submit.prevent="submit">
      <PhotoPicker round label="Foto (opcional)" @select="photo = $event" @remove="photo = null" />
      <label class="floating-label">
        <span>Nome completo</span>
        <input
          v-model="form.name"
          class="input w-full"
          placeholder="Nome completo"
          autocomplete="name"
          required
        />
      </label>
      <label class="floating-label">
        <span>E-mail</span>
        <input
          v-model="form.email"
          type="email"
          class="input w-full"
          placeholder="E-mail"
          autocomplete="email"
          required
        />
      </label>
      <div class="grid grid-cols-2 gap-3">
        <label class="floating-label">
          <span>CPF</span>
          <input
            v-model="form.cpf"
            class="input w-full"
            placeholder="CPF"
            inputmode="numeric"
            maxlength="11"
            required
          />
        </label>
        <label class="floating-label">
          <span>CNH</span>
          <input
            v-model="form.cnh"
            class="input w-full"
            placeholder="CNH"
            inputmode="numeric"
            maxlength="11"
            required
          />
        </label>
      </div>
      <CnhDocumentField required @select="cnhDocument = $event" />
      <label class="floating-label">
        <span>Senha</span>
        <input
          v-model="form.password"
          type="password"
          class="input w-full"
          placeholder="Senha"
          autocomplete="new-password"
          required
        />
      </label>
      <label class="floating-label">
        <span>Confirmar senha</span>
        <input
          v-model="confirmPassword"
          type="password"
          class="input w-full"
          placeholder="Confirmar senha"
          autocomplete="new-password"
          required
        />
      </label>
      <div v-if="error" role="alert" class="alert alert-error alert-soft">{{ error }}</div>
      <button class="btn btn-primary" :disabled="loading">
        <span v-if="loading" class="loading loading-spinner loading-sm"></span>
        Criar conta
      </button>
      <p class="text-center text-sm">
        Já tem conta?
        <RouterLink :to="{ name: 'login' }" class="link link-primary">Entrar</RouterLink>
      </p>
    </form>
  </AuthShell>
</template>
