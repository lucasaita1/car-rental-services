<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import BaseModal from './BaseModal.vue'
import { createUser } from '@/api/users'
import { errorMessage } from '@/api/http'
import type { AdminUserPayload } from '@/api/types'
import { useToastStore } from '@/stores/toast'

const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{ created: [] }>()

const toast = useToastStore()

const empty = (): AdminUserPayload => ({
  name: '',
  email: '',
  cpf: '',
  cnh: '',
  password: '',
  role: 'ADMIN',
})

const form = reactive<AdminUserPayload>(empty())
const loading = ref(false)
const error = ref('')

watch(open, (value) => {
  if (value) {
    Object.assign(form, empty())
    error.value = ''
  }
})

function validate(): string {
  if (form.password.length < 8) return 'A senha precisa ter pelo menos 8 caracteres.'
  if (form.cpf && !/^\d{11}$/.test(form.cpf)) return 'CPF deve ter 11 dígitos, sem pontuação.'
  if (form.cnh && !/^\d{11}$/.test(form.cnh)) return 'CNH deve ter 11 dígitos.'
  return ''
}

async function submit() {
  error.value = validate()
  if (error.value) return
  loading.value = true
  try {
    await createUser({
      ...form,
      email: form.email.trim(),
      cpf: form.cpf || null,
      cnh: form.cnh || null,
    })
    toast.success(form.role === 'ADMIN' ? 'Administrador criado.' : 'Cliente criado.')
    emit('created')
    open.value = false
  } catch (e) {
    error.value = errorMessage(e, 'Não foi possível criar o usuário.')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <BaseModal v-model:open="open" title="Novo usuário">
    <form id="user-form" class="space-y-4" @submit.prevent="submit">
      <div role="alert" v-if="error" class="alert alert-error alert-soft">{{ error }}</div>
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
          <span>CPF (opcional)</span>
          <input
            v-model="form.cpf"
            class="input w-full"
            placeholder="CPF (opcional)"
            inputmode="numeric"
            maxlength="11"
          />
        </label>
        <label class="floating-label">
          <span>CNH (opcional)</span>
          <input
            v-model="form.cnh"
            class="input w-full"
            placeholder="CNH (opcional)"
            inputmode="numeric"
            maxlength="11"
          />
        </label>
      </div>
      <label class="floating-label">
        <span>Senha inicial</span>
        <input
          v-model="form.password"
          type="password"
          class="input w-full"
          placeholder="Senha inicial"
          autocomplete="new-password"
          minlength="8"
          required
        />
      </label>
      <fieldset class="fieldset">
        <legend class="fieldset-legend">Papel</legend>
        <div class="join w-full">
          <input
            v-model="form.role"
            type="radio"
            value="ADMIN"
            class="join-item btn flex-1"
            aria-label="Administrador"
          />
          <input
            v-model="form.role"
            type="radio"
            value="USER"
            class="join-item btn flex-1"
            aria-label="Cliente"
          />
        </div>
      </fieldset>
    </form>

    <template #actions>
      <button class="btn btn-ghost" :disabled="loading" @click="open = false">Cancelar</button>
      <button type="submit" form="user-form" class="btn btn-primary" :disabled="loading">
        <span v-if="loading" class="loading loading-spinner loading-sm"></span>
        Criar usuário
      </button>
    </template>
  </BaseModal>
</template>
