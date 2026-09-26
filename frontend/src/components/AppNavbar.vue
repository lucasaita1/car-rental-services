<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { assetUrl, userApi } from '@/api/http'
import AnchorLink from './AnchorLink.vue'
import AppIcon from './AppIcon.vue'
import BrandLogo from './BrandLogo.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { firstName } from '@/utils/format'

const auth = useAuthStore()
const toast = useToastStore()
const route = useRoute()
const router = useRouter()

const scrolled = ref(false)
const onScroll = () => (scrolled.value = window.scrollY > 8)
onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
})
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))

const onBrand = computed(() => route.name === 'home' || !!route.meta.bare)
const dark = computed(() => onBrand.value && !scrolled.value)

const wrapperClass = computed(() => {
  if (scrolled.value) return 'bg-transparent'
  return onBrand.value ? 'bg-neutral' : 'bg-base-200'
})
const pillClass = computed(() => {
  if (!onBrand.value) {
    return 'bg-base-100/80 text-base-content ring-base-300 shadow-lg shadow-base-content/5 backdrop-blur-xl'
  }
  if (dark.value) return 'bg-neutral-content/[0.04] text-neutral-content ring-neutral-content/10'
  return 'bg-neutral/80 text-neutral-content ring-neutral-content/15 shadow-2xl shadow-black/40 backdrop-blur-xl backdrop-saturate-150'
})
const linkClass = computed(() =>
  onBrand.value
    ? 'text-neutral-content/75 hover:text-neutral-content hover:bg-neutral-content/10'
    : 'text-base-content/70 hover:text-base-content hover:bg-base-200',
)
const activeClass = computed(() =>
  onBrand.value ? '!bg-neutral-content/10 !text-neutral-content' : '!bg-primary/10 !text-primary',
)

const avatar = computed(() => assetUrl(userApi, auth.profile?.photoUrl))
const initial = computed(() => auth.userName.charAt(0).toUpperCase())

function blur() {
  ;(document.activeElement as HTMLElement | null)?.blur()
}

async function logout() {
  blur()
  await auth.logout()
  toast.info('Você saiu da sua conta.')
  router.push({ name: 'home' })
}
</script>

<template>
  <header class="sticky top-0 z-30 px-3 pt-3 transition-colors duration-300" :class="wrapperClass">
    <nav
      class="mx-auto flex h-14 max-w-6xl items-center gap-2 rounded-full pr-2 pl-4 ring-1 transition-all duration-300 sm:pl-5"
      :class="pillClass"
    >
      <div class="dropdown lg:hidden">
        <div
          tabindex="0"
          role="button"
          class="btn btn-ghost btn-square btn-sm -ml-1"
          aria-label="Abrir menu"
        >
          <svg
            viewBox="0 0 24 24"
            class="size-6"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path stroke-linecap="round" d="M4 7h16M4 12h16M4 17h10" />
          </svg>
        </div>
        <ul
          tabindex="0"
          class="dropdown-content menu bg-base-100 text-base-content rounded-box border-base-300 z-40 mt-3 w-64 border p-2 shadow-xl"
          @click="blur"
        >
          <li v-if="auth.isAuthenticated">
            <RouterLink :to="{ name: 'home' }">Início</RouterLink>
          </li>
          <li><RouterLink :to="{ name: 'catalog' }">Frota</RouterLink></li>
          <li v-if="auth.isAuthenticated">
            <RouterLink :to="{ name: 'my-rentals' }">Minhas locações</RouterLink>
          </li>
          <template v-else>
            <li>
              <AnchorLink :to="{ name: 'home', hash: '#como-funciona' }">Como funciona</AnchorLink>
            </li>
            <li><AnchorLink :to="{ name: 'home', hash: '#duvidas' }">Dúvidas</AnchorLink></li>
          </template>
          <template v-if="auth.isAdmin">
            <li class="menu-title">Administração</li>
            <li><RouterLink :to="{ name: 'admin-cars' }">Gerenciar frota</RouterLink></li>
            <li><RouterLink :to="{ name: 'admin-rentals' }">Locações</RouterLink></li>
            <li><RouterLink :to="{ name: 'admin-users' }">Usuários</RouterLink></li>
          </template>
          <template v-if="!auth.isAuthenticated">
            <div class="divider my-1"></div>
            <li><RouterLink :to="{ name: 'register' }">Criar conta</RouterLink></li>
          </template>
        </ul>
      </div>

      <AnchorLink :to="{ name: 'home' }" class="shrink-0 rounded-lg" aria-label="Página inicial">
        <BrandLogo :size="34" />
      </AnchorLink>

      <ul class="ml-6 hidden items-center gap-1 text-sm font-medium lg:flex">
        <li v-if="auth.isAuthenticated">
          <RouterLink
            :to="{ name: 'home' }"
            class="rounded-full px-4 py-2 transition"
            :class="linkClass"
            :exact-active-class="activeClass"
            >Início</RouterLink
          >
        </li>
        <li>
          <RouterLink
            :to="{ name: 'catalog' }"
            class="rounded-full px-4 py-2 transition"
            :class="linkClass"
            :active-class="activeClass"
            >Frota</RouterLink
          >
        </li>
        <li v-if="auth.isAuthenticated">
          <RouterLink
            :to="{ name: 'my-rentals' }"
            class="rounded-full px-4 py-2 transition"
            :class="linkClass"
            :active-class="activeClass"
            >Minhas locações</RouterLink
          >
        </li>
        <template v-else>
          <li>
            <AnchorLink
              :to="{ name: 'home', hash: '#como-funciona' }"
              class="rounded-full px-4 py-2 transition"
              :class="linkClass"
              >Como funciona</AnchorLink
            >
          </li>
          <li>
            <AnchorLink
              :to="{ name: 'home', hash: '#duvidas' }"
              class="rounded-full px-4 py-2 transition"
              :class="linkClass"
              >Dúvidas</AnchorLink
            >
          </li>
        </template>
        <li v-if="auth.isAdmin" class="dropdown dropdown-bottom">
          <div
            tabindex="0"
            role="button"
            class="flex cursor-pointer items-center gap-1 rounded-full px-4 py-2 transition"
            :class="[linkClass, { [activeClass]: String(route.name ?? '').startsWith('admin-') }]"
          >
            Administração
            <svg viewBox="0 0 20 20" class="size-4 opacity-70" fill="currentColor">
              <path
                fill-rule="evenodd"
                d="M5.22 8.22a.75.75 0 0 1 1.06 0L10 11.94l3.72-3.72a.75.75 0 1 1 1.06 1.06l-4.25 4.25a.75.75 0 0 1-1.06 0L5.22 9.28a.75.75 0 0 1 0-1.06Z"
              />
            </svg>
          </div>
          <ul
            tabindex="0"
            class="dropdown-content menu bg-base-100 text-base-content rounded-box border-base-300 z-40 mt-3 w-52 border p-2 shadow-xl"
            @click="blur"
          >
            <li>
              <RouterLink :to="{ name: 'admin-cars' }"
                ><AppIcon name="grid" class="size-4" /> Gerenciar frota</RouterLink
              >
            </li>
            <li>
              <RouterLink :to="{ name: 'admin-rentals' }"
                ><AppIcon name="calendar" class="size-4" /> Locações</RouterLink
              >
            </li>
            <li>
              <RouterLink :to="{ name: 'admin-users' }"
                ><AppIcon name="users" class="size-4" /> Usuários</RouterLink
              >
            </li>
          </ul>
        </li>
      </ul>

      <div class="ml-auto flex items-center gap-2">
        <div v-if="auth.isAuthenticated" class="dropdown dropdown-end">
          <div
            tabindex="0"
            role="button"
            class="flex cursor-pointer items-center gap-3 rounded-full py-1 pr-3 pl-1 transition"
            :class="linkClass"
          >
            <div class="avatar" :class="{ 'avatar-placeholder': !avatar }">
              <div class="bg-primary text-primary-content w-8 rounded-full">
                <img v-if="avatar" :src="avatar" alt="" />
                <span v-else class="text-sm font-bold">{{ initial }}</span>
              </div>
            </div>
            <span class="hidden text-sm leading-tight font-semibold sm:block">
              {{ firstName(auth.userName) }}
              <span v-if="auth.isAdmin" class="block text-xs font-medium opacity-60"
                >Administrador</span
              >
            </span>
          </div>
          <div
            tabindex="0"
            class="dropdown-content bg-base-100 text-base-content rounded-box border-base-300 z-40 mt-3 w-60 border shadow-xl"
          >
            <div class="border-base-300 border-b px-4 py-3">
              <p class="truncate font-semibold">{{ auth.userName }}</p>
              <p class="text-base-content/60 truncate text-sm">{{ auth.profile?.email }}</p>
            </div>
            <ul class="menu w-full p-2" @click="blur">
              <li>
                <RouterLink :to="{ name: 'profile' }"
                  ><AppIcon name="user" class="size-4" /> Meu perfil</RouterLink
                >
              </li>
              <li>
                <RouterLink :to="{ name: 'my-rentals' }"
                  ><AppIcon name="list" class="size-4" /> Minhas locações</RouterLink
                >
              </li>
              <li>
                <button class="text-error" @click="logout">
                  <AppIcon name="arrowRight" class="size-4" /> Sair
                </button>
              </li>
            </ul>
          </div>
        </div>
        <template v-else>
          <RouterLink
            :to="{ name: 'login' }"
            class="rounded-full px-4 py-2 text-sm font-semibold transition"
            :class="linkClass"
            >Entrar</RouterLink
          >
          <RouterLink
            :to="{ name: 'register' }"
            class="btn btn-primary btn-sm hidden rounded-full px-5 sm:inline-flex"
            >Criar conta</RouterLink
          >
        </template>
      </div>
    </nav>
  </header>
</template>
