<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PhotoPicker from '@/components/PhotoPicker.vue'
import { assetUrl, errorMessage, userApi } from '@/api/http'
import { changePassword, removeMyPhoto, updateMe, uploadMyPhoto } from '@/api/users'
import type { ProfilePayload } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

const form = reactive<ProfilePayload>({ name: '', email: '', cpf: '', cnh: '' })
const saving = ref(false)
const profileError = ref('')

const passwords = reactive({ current: '', next: '', confirm: '' })
const changing = ref(false)
const passwordError = ref('')

watch(
  () => auth.profile,
  (profile) => {
    if (!profile) return
    Object.assign(form, {
      name: profile.name,
      email: profile.email,
      cpf: profile.cpf ?? '',
      cnh: profile.cnh ?? '',
    })
  },
  { immediate: true },
)

async function saveProfile() {
  saving.value = true
  profileError.value = ''
  try {
    auth.profile = await updateMe({ ...form, cpf: form.cpf || null, cnh: form.cnh || null })
    toast.success('Perfil atualizado.')
  } catch (e) {
    profileError.value = errorMessage(e, 'Não foi possível salvar o perfil.')
  } finally {
    saving.value = false
  }
}

async function onPhoto(file: File) {
  try {
    auth.profile = await uploadMyPhoto(file)
    toast.success('Foto atualizada.')
  } catch (e) {
    toast.error(errorMessage(e, 'Não foi possível enviar a foto.'))
  }
}

async function onRemovePhoto() {
  try {
    auth.profile = await removeMyPhoto()
    toast.info('Foto removida.')
  } catch (e) {
    toast.error(errorMessage(e, 'Não foi possível remover a foto.'))
  }
}

async function submitPassword() {
  passwordError.value = ''
  if (passwords.next.length < 8) {
    passwordError.value = 'A nova senha precisa ter pelo menos 8 caracteres.'
    return
  }
  if (passwords.next !== passwords.confirm) {
    passwordError.value = 'As senhas não conferem.'
    return
  }
  changing.value = true
  try {
    await changePassword(passwords.current, passwords.next)
    auth.clearSession()
    toast.success('Senha alterada. Entre novamente com a nova senha.')
    router.push({ name: 'login' })
  } catch (e) {
    passwordError.value = errorMessage(e, 'Não foi possível alterar a senha.')
  } finally {
    changing.value = false
  }
}
</script>

<template>
  <section class="mx-auto max-w-2xl space-y-6">
    <div>
      <h1 class="text-3xl font-bold">Meu perfil</h1>
      <p class="text-base-content/70">Atualize sua foto, seus dados e sua senha.</p>
    </div>

    <div class="card bg-base-100 border-base-300 border shadow-sm">
      <form class="card-body gap-4" @submit.prevent="saveProfile">
        <h2 class="card-title">Dados pessoais</h2>
        <PhotoPicker
          round
          label="Foto"
          :current-url="assetUrl(userApi, auth.profile?.photoUrl)"
          @select="onPhoto"
          @remove="onRemovePhoto"
        />
        <label class="floating-label">
          <span>Nome completo</span>
          <input v-model="form.name" class="input w-full" placeholder="Nome completo" required />
        </label>
        <label class="floating-label">
          <span>E-mail</span>
          <input
            v-model="form.email"
            type="email"
            class="input w-full"
            placeholder="E-mail"
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
              maxlength="14"
            />
          </label>
          <label class="floating-label">
            <span>CNH</span>
            <input
              v-model="form.cnh"
              class="input w-full"
              placeholder="CNH"
              inputmode="numeric"
              maxlength="14"
            />
          </label>
        </div>
        <div v-if="profileError" role="alert" class="alert alert-error alert-soft">
          {{ profileError }}
        </div>
        <div class="card-actions justify-end">
          <button class="btn btn-primary" :disabled="saving">
            <span v-if="saving" class="loading loading-spinner loading-sm"></span>
            Salvar
          </button>
        </div>
      </form>
    </div>

    <div class="card bg-base-100 border-base-300 border shadow-sm">
      <form class="card-body gap-4" @submit.prevent="submitPassword">
        <h2 class="card-title">Trocar senha</h2>
        <p class="text-base-content/70 text-sm">
          Por segurança, todas as sessões abertas serão encerradas.
        </p>
        <label class="floating-label">
          <span>Senha atual</span>
          <input
            v-model="passwords.current"
            type="password"
            class="input w-full"
            placeholder="Senha atual"
            autocomplete="current-password"
            required
          />
        </label>
        <div class="grid grid-cols-2 gap-3">
          <label class="floating-label">
            <span>Nova senha</span>
            <input
              v-model="passwords.next"
              type="password"
              class="input w-full"
              placeholder="Nova senha"
              autocomplete="new-password"
              required
            />
          </label>
          <label class="floating-label">
            <span>Confirmar</span>
            <input
              v-model="passwords.confirm"
              type="password"
              class="input w-full"
              placeholder="Confirmar"
              autocomplete="new-password"
              required
            />
          </label>
        </div>
        <div v-if="passwordError" role="alert" class="alert alert-error alert-soft">
          {{ passwordError }}
        </div>
        <div class="card-actions justify-end">
          <button class="btn btn-outline" :disabled="changing">
            <span v-if="changing" class="loading loading-spinner loading-sm"></span>
            Alterar senha
          </button>
        </div>
      </form>
    </div>
  </section>
</template>
