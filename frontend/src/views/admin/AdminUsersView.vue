<script setup lang="ts">
import { onMounted, ref } from 'vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import UserFormModal from '@/components/UserFormModal.vue'
import { changeRole, listUsers, openCnhDocument } from '@/api/users'
import { errorMessage } from '@/api/http'
import type { User, UserRole } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToastStore()

const users = ref<User[]>([])
const loading = ref(true)
const error = ref('')

const formOpen = ref(false)
const confirmOpen = ref(false)
const saving = ref(false)
const target = ref<User | null>(null)
const nextRole = ref<UserRole>('ADMIN')

async function viewCnh(user: User) {
  try {
    await openCnhDocument(user.id)
  } catch (e) {
    toast.error(errorMessage(e, 'Não foi possível abrir a CNH.'))
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    users.value = await listUsers()
  } catch (e) {
    error.value = errorMessage(e, 'Não foi possível carregar os usuários.')
  } finally {
    loading.value = false
  }
}

function askChange(user: User) {
  target.value = user
  nextRole.value = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
  confirmOpen.value = true
}

async function confirmChange() {
  if (!target.value) return
  saving.value = true
  try {
    await changeRole(target.value.id, nextRole.value)
    toast.success(
      nextRole.value === 'ADMIN'
        ? 'Usuário promovido a administrador.'
        : 'Acesso de administrador removido.',
    )
    confirmOpen.value = false
    await load()
  } catch (e) {
    toast.error(errorMessage(e, 'Não foi possível alterar o papel.'))
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="space-y-6">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-3xl font-bold">Usuários</h1>
        <p class="text-base-content/70">
          Crie administradores, promova clientes ou remova esse acesso.
        </p>
      </div>
      <button class="btn btn-primary" @click="formOpen = true">Novo usuário</button>
    </div>

    <div v-if="loading" class="flex justify-center py-16">
      <span class="loading loading-spinner loading-lg"></span>
    </div>
    <div v-else-if="error" role="alert" class="alert alert-error alert-soft">{{ error }}</div>

    <div v-else class="overflow-x-auto rounded-box border border-base-300 bg-base-100">
      <table class="table">
        <thead>
          <tr>
            <th>Nome</th>
            <th>E-mail</th>
            <th>Papel</th>
            <th>CNH</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td class="font-medium">{{ user.name }}</td>
            <td>{{ user.email }}</td>
            <td>
              <span
                class="badge badge-soft"
                :class="user.role === 'ADMIN' ? 'badge-primary' : 'badge-ghost'"
              >
                {{ user.role === 'ADMIN' ? 'Administrador' : 'Cliente' }}
              </span>
            </td>
            <td>
              <button
                v-if="user.cnhDocument"
                class="btn btn-ghost btn-xs text-success"
                @click="viewCnh(user)"
              >
                Ver PDF
              </button>
              <span v-else class="badge badge-warning badge-soft badge-sm">Pendente</span>
            </td>
            <td class="text-right">
              <span v-if="user.id === auth.userId" class="text-sm text-base-content/60">Você</span>
              <button v-else class="btn btn-sm btn-ghost" @click="askChange(user)">
                {{ user.role === 'ADMIN' ? 'Remover admin' : 'Tornar admin' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <UserFormModal v-model:open="formOpen" @created="load" />

    <ConfirmModal
      v-model:open="confirmOpen"
      :title="nextRole === 'ADMIN' ? 'Promover a administrador' : 'Remover administrador'"
      :message="
        nextRole === 'ADMIN'
          ? `${target?.name} poderá cadastrar, editar e remover carros e ver todas as locações.`
          : `${target?.name} voltará a ter acesso apenas de cliente.`
      "
      :confirm-label="nextRole === 'ADMIN' ? 'Promover' : 'Remover acesso'"
      :danger="nextRole === 'USER'"
      :loading="saving"
      @confirm="confirmChange"
    />
  </section>
</template>
