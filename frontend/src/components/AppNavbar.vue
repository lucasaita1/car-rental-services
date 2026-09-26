<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { assetUrl, userApi } from '@/api/http'
import AnchorLink from './AnchorLink.vue'
import BrandLogo from './BrandLogo.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

const avatar = computed(() => assetUrl(userApi, auth.profile?.photoUrl))
const initial = computed(() => auth.userName.charAt(0).toUpperCase())

async function logout() {
  await auth.logout()
  toast.info('Você saiu da sua conta.')
  router.push({ name: 'home' })
}
</script>

<template>
  <div class="navbar bg-base-100/90 border-base-300 sticky top-0 z-30 border-b px-4 backdrop-blur">
    <div class="navbar-start gap-1">
      <div class="dropdown lg:hidden">
        <div tabindex="0" role="button" class="btn btn-ghost btn-square" aria-label="Abrir menu">
          <svg
            viewBox="0 0 24 24"
            class="size-6"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path stroke-linecap="round" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </div>
        <ul
          tabindex="0"
          class="dropdown-content menu bg-base-100 rounded-box z-40 mt-3 w-60 p-2 shadow-lg"
        >
          <li><RouterLink :to="{ name: 'catalog' }">Frota</RouterLink></li>
          <li>
            <AnchorLink :to="{ name: 'home', hash: '#como-funciona' }">Como funciona</AnchorLink>
          </li>
          <li><AnchorLink :to="{ name: 'home', hash: '#duvidas' }">Dúvidas</AnchorLink></li>
          <li v-if="auth.isAuthenticated">
            <RouterLink :to="{ name: 'my-rentals' }">Minhas locações</RouterLink>
          </li>
          <template v-if="auth.isAdmin">
            <li class="menu-title">Administração</li>
            <li><RouterLink :to="{ name: 'admin-cars' }">Frota</RouterLink></li>
            <li><RouterLink :to="{ name: 'admin-rentals' }">Locações</RouterLink></li>
            <li><RouterLink :to="{ name: 'admin-users' }">Usuários</RouterLink></li>
          </template>
        </ul>
      </div>
      <AnchorLink :to="{ name: 'home' }" class="btn btn-ghost px-2" aria-label="Página inicial">
        <BrandLogo />
      </AnchorLink>
    </div>

    <div class="navbar-center hidden lg:flex">
      <ul class="menu menu-horizontal gap-1 px-1 font-medium">
        <li><RouterLink :to="{ name: 'catalog' }" active-class="menu-active">Frota</RouterLink></li>
        <li>
          <AnchorLink :to="{ name: 'home', hash: '#como-funciona' }">Como funciona</AnchorLink>
        </li>
        <li><AnchorLink :to="{ name: 'home', hash: '#duvidas' }">Dúvidas</AnchorLink></li>
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
    </div>

    <div class="navbar-end gap-2">
      <template v-if="auth.isAuthenticated">
        <div class="dropdown dropdown-end">
          <div tabindex="0" role="button" class="btn btn-ghost gap-2 px-2">
            <div class="avatar" :class="{ 'avatar-placeholder': !avatar }">
              <div class="bg-neutral text-neutral-content w-8 rounded-full">
                <img v-if="avatar" :src="avatar" alt="" />
                <span v-else class="text-sm">{{ initial }}</span>
              </div>
            </div>
            <span class="hidden sm:inline">{{ auth.userName }}</span>
            <span v-if="auth.isAdmin" class="badge badge-primary badge-sm">Admin</span>
          </div>
          <ul
            tabindex="0"
            class="dropdown-content menu bg-base-100 rounded-box z-40 mt-3 w-44 p-2 shadow-lg"
          >
            <li><RouterLink :to="{ name: 'profile' }">Meu perfil</RouterLink></li>
            <li><RouterLink :to="{ name: 'my-rentals' }">Minhas locações</RouterLink></li>
            <li><button @click="logout">Sair</button></li>
          </ul>
        </div>
      </template>
      <template v-else>
        <RouterLink :to="{ name: 'login' }" class="btn btn-ghost">Entrar</RouterLink>
        <RouterLink :to="{ name: 'register' }" class="btn btn-primary hidden sm:inline-flex"
          >Criar conta</RouterLink
        >
      </template>
    </div>
  </div>
</template>
