<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
import type { IconName } from '@/utils/icons'

const cases: { icon: IconName; title: string; text: string; tone: string }[] = [
  {
    icon: 'users',
    title: 'Viagem em família',
    text: 'Espaço para todo mundo e a bagagem, sem depender de horário de ônibus.',
    tone: 'from-primary to-secondary text-primary-content',
  },
  {
    icon: 'briefcase',
    title: 'Trabalho',
    text: 'Visite clientes e cumpra a agenda do dia com um carro à disposição.',
    tone: 'from-secondary to-neutral text-neutral-content',
  },
  {
    icon: 'sun',
    title: 'Fim de semana',
    text: 'Aquela praia ou serra que você vem adiando. Devolva quando voltar.',
    tone: 'from-accent to-warning text-accent-content',
  },
]
</script>

<template>
  <section class="bg-base-100 py-20">
    <div class="mx-auto max-w-6xl px-4">
      <div class="mb-12 max-w-2xl space-y-3">
        <p class="text-primary font-semibold tracking-wide uppercase">Para cada momento</p>
        <h2 class="text-3xl font-extrabold tracking-tight md:text-4xl">
          Um carro para o que você tiver em mente
        </h2>
      </div>
      <div class="grid gap-5 md:grid-cols-3">
        <RouterLink
          v-for="c in cases"
          :key="c.title"
          :to="{ name: 'catalog' }"
          class="group rounded-box relative flex min-h-64 flex-col justify-end overflow-hidden bg-gradient-to-br p-7 shadow-lg transition hover:-translate-y-1"
          :class="c.tone"
        >
          <AppIcon
            :name="c.icon"
            class="absolute -top-6 -right-6 size-44 opacity-15 transition group-hover:rotate-6"
          />
          <h3 class="text-2xl font-extrabold">{{ c.title }}</h3>
          <p class="mt-2 opacity-85">{{ c.text }}</p>
          <p class="mt-5 flex items-center gap-1 font-semibold">
            Escolher carro
            <AppIcon name="arrowRight" class="size-5 transition group-hover:translate-x-1" />
          </p>
        </RouterLink>
      </div>
    </div>
  </section>
</template>
