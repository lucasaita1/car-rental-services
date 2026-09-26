<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>

<template>
  <section class="mx-auto max-w-6xl px-4 pb-24">
    <div
      class="rounded-box bg-primary text-primary-content relative isolate overflow-hidden px-8 py-14 shadow-2xl md:px-16"
    >
      <div class="road-line absolute right-0 bottom-6 left-0 -z-10 h-1 opacity-50"></div>
      <div class="flex flex-col items-start justify-between gap-8 md:flex-row md:items-center">
        <div class="max-w-xl space-y-3">
          <h2 class="text-3xl font-extrabold tracking-tight md:text-4xl">
            Pronto para pegar a estrada?
          </h2>
          <p class="opacity-85">
            {{
              auth.isAuthenticated
                ? 'Seu carro está a poucos cliques. Escolha e reserve agora.'
                : 'Crie sua conta em menos de um minuto e reserve o seu carro hoje mesmo.'
            }}
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <RouterLink
            v-if="!auth.isAuthenticated"
            :to="{ name: 'register' }"
            class="btn btn-accent btn-lg"
          >
            Criar conta grátis
          </RouterLink>
          <RouterLink
            :to="{ name: 'catalog' }"
            class="btn btn-lg"
            :class="
              auth.isAuthenticated
                ? 'btn-accent'
                : 'btn-ghost border-primary-content/40 text-primary-content'
            "
          >
            Ver carros disponíveis
            <AppIcon name="arrowRight" class="size-5" />
          </RouterLink>
        </div>
      </div>
    </div>
  </section>
</template>
