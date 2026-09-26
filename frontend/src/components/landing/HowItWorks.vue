<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
import type { IconName } from '@/utils/icons'

const steps: { icon: IconName; title: string; text: string }[] = [
  {
    icon: 'userPlus',
    title: 'Crie sua conta',
    text: 'Cadastro rápido com nome, e-mail, CPF e CNH. Sem papelada.',
  },
  {
    icon: 'search',
    title: 'Escolha o carro',
    text: 'Veja fotos e a disponibilidade de cada carro em tempo real.',
  },
  {
    icon: 'clock',
    title: 'Reserve com garantia',
    text: 'Ao clicar em alugar, o carro fica seu por 10 minutos enquanto você confirma.',
  },
  {
    icon: 'key',
    title: 'Pegue a estrada',
    text: 'Acompanhe e devolva quando quiser pelo painel de locações.',
  },
]
</script>

<template>
  <section id="como-funciona" class="bg-base-100 scroll-mt-20 py-20">
    <div class="mx-auto max-w-6xl px-4">
      <div class="mx-auto mb-14 max-w-2xl space-y-3 text-center">
        <p class="text-primary font-semibold tracking-wide uppercase">Como funciona</p>
        <h2 class="text-3xl font-extrabold tracking-tight md:text-4xl">
          Do cadastro à estrada em quatro passos
        </h2>
      </div>

      <ol class="relative grid gap-8 md:grid-cols-4">
        <div
          class="bg-base-300 absolute top-7 right-[12%] left-[12%] -z-0 hidden h-0.5 md:block"
        ></div>
        <li v-for="(step, i) in steps" :key="step.title" class="relative space-y-4 text-center">
          <div
            class="bg-primary text-primary-content ring-base-100 mx-auto flex size-14 items-center justify-center rounded-2xl shadow-lg ring-8"
          >
            <AppIcon :name="step.icon" class="size-7" />
          </div>
          <p
            class="text-accent-content bg-accent mx-auto w-fit rounded-full px-2.5 text-xs font-bold"
          >
            Passo {{ i + 1 }}
          </p>
          <h3 class="text-lg font-bold">{{ step.title }}</h3>
          <p class="text-base-content/70">{{ step.text }}</p>
        </li>
      </ol>
    </div>
  </section>
</template>
