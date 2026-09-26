<script setup lang="ts">
import AnchorLink from './AnchorLink.vue'
import BrandLogo from './BrandLogo.vue'
import AppIcon from './AppIcon.vue'
import { brand } from '@/brand'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const year = new Date().getFullYear()
</script>

<template>
  <footer class="bg-neutral text-neutral-content">
    <div class="mx-auto grid max-w-6xl gap-10 px-4 py-14 md:grid-cols-[2fr_1fr_1fr_1fr]">
      <div class="space-y-4">
        <BrandLogo />
        <p class="text-neutral-content/70 max-w-xs">{{ brand.description }}</p>
        <a
          :href="`mailto:${brand.contactEmail}`"
          class="link link-hover text-neutral-content/80 flex items-center gap-2 text-sm"
        >
          <AppIcon name="envelope" class="size-4" /> {{ brand.contactEmail }}
        </a>
      </div>
      <nav class="flex flex-col gap-2">
        <h6 class="footer-title">Aluguel</h6>
        <RouterLink :to="{ name: 'catalog' }" class="link link-hover">Frota</RouterLink>
        <template v-if="!auth.isAuthenticated">
          <AnchorLink :to="{ name: 'home', hash: '#como-funciona' }" class="link link-hover">
            Como funciona
          </AnchorLink>
          <AnchorLink :to="{ name: 'home', hash: '#duvidas' }" class="link link-hover">
            Dúvidas
          </AnchorLink>
        </template>
        <RouterLink v-else :to="{ name: 'home' }" class="link link-hover">Início</RouterLink>
      </nav>
      <nav class="flex flex-col gap-2">
        <h6 class="footer-title">Sua conta</h6>
        <template v-if="auth.isAuthenticated">
          <RouterLink :to="{ name: 'my-rentals' }" class="link link-hover"
            >Minhas locações</RouterLink
          >
          <RouterLink :to="{ name: 'profile' }" class="link link-hover">Meu perfil</RouterLink>
        </template>
        <template v-else>
          <RouterLink :to="{ name: 'login' }" class="link link-hover">Entrar</RouterLink>
          <RouterLink :to="{ name: 'register' }" class="link link-hover">Criar conta</RouterLink>
          <RouterLink :to="{ name: 'forgot-password' }" class="link link-hover"
            >Esqueci minha senha</RouterLink
          >
        </template>
      </nav>
      <div class="flex flex-col gap-2">
        <h6 class="footer-title">Atendimento</h6>
        <p class="text-neutral-content/70 text-sm">
          Reservas online 24 horas por dia, direto pelo site.
        </p>
      </div>
    </div>
    <div class="border-neutral-content/10 border-t">
      <div
        class="text-neutral-content/60 mx-auto flex max-w-6xl flex-col justify-between gap-2 px-4 py-6 text-sm md:flex-row"
      >
        <p>© {{ year }} {{ brand.name }}. Todos os direitos reservados.</p>
        <p>Projeto acadêmico · Car Rental Services</p>
      </div>
    </div>
  </footer>
</template>
