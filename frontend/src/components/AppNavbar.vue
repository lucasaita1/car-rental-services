<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

function logout() {
  auth.logout()
  toast.info('Você saiu da sua conta.')
  router.push({ name: 'catalog' })
}
</script>

<template>
  <div class="navbar bg-base-100 border-b border-base-300 px-4">
    <div class="flex-1">
      <RouterLink :to="{ name: 'catalog' }" class="btn btn-ghost text-xl font-bold">
        Car Rental
      </RouterLink>
    </div>

    <ul class="menu menu-horizontal gap-1 px-1">
      <li>
        <RouterLink :to="{ name: 'catalog' }" active-class="menu-active">Catálogo</RouterLink>
      </li>
      <li v-if="auth.isAuthenticated">
        <RouterLink :to="{ name: 'my-rentals' }" active-class="menu-active"
          >Minhas locações</RouterLink
        >
      </li>
      <li v-if="auth.isAdmin">
        <details>
          <summary>Administração</summary>
          <ul class="bg-base-100 rounded-box z-10 w-44 p-2 shadow">
            <li><RouterLink :to="{ name: 'admin-cars' }">Frota</RouterLink></li>
            <li><RouterLink :to="{ name: 'admin-rentals' }">Locações</RouterLink></li>
            <li><RouterLink :to="{ name: 'admin-users' }">Usuários</RouterLink></li>
          </ul>
        </details>
      </li>
    </ul>

    <div class="flex-none gap-2">
      <template v-if="auth.isAuthenticated">
        <div class="dropdown dropdown-end">
          <div tabindex="0" role="button" class="btn btn-ghost gap-2">
            <span>{{ auth.userName }}</span>
            <span v-if="auth.isAdmin" class="badge badge-primary badge-sm">Admin</span>
          </div>
          <ul
            tabindex="0"
            class="dropdown-content menu bg-base-100 rounded-box z-10 w-40 p-2 shadow"
          >
            <li><button @click="logout">Sair</button></li>
          </ul>
        </div>
      </template>
      <template v-else>
        <RouterLink :to="{ name: 'login' }" class="btn btn-ghost">Entrar</RouterLink>
        <RouterLink :to="{ name: 'register' }" class="btn btn-primary">Criar conta</RouterLink>
      </template>
    </div>
  </div>
</template>
