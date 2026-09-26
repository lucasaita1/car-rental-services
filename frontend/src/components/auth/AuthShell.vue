<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
import BrandLogo from '@/components/BrandLogo.vue'
import HeroCar from '@/components/landing/HeroCar.vue'
import { brand } from '@/brand'
import type { IconName } from '@/utils/icons'

withDefaults(defineProps<{ title: string; subtitle?: string; wide?: boolean }>(), {
  subtitle: '',
  wide: false,
})

const highlights: { icon: IconName; text: string }[] = [
  { icon: 'clock', text: 'O carro fica reservado só para você por 10 minutos enquanto confirma.' },
  { icon: 'calendar', text: 'Datas de retirada e devolução sempre à vista.' },
  { icon: 'shield', text: 'Conta protegida: sair da conta encerra o acesso na hora.' },
]
</script>

<template>
  <div class="grid min-h-dvh lg:grid-cols-[1.05fr_1fr]">
    <aside
      class="bg-neutral text-neutral-content relative hidden flex-col justify-between gap-10 overflow-hidden px-12 py-14 lg:flex xl:px-16"
    >
      <RouterLink :to="{ name: 'home' }" class="w-fit" aria-label="Página inicial">
        <BrandLogo :size="40" />
      </RouterLink>

      <div class="max-w-md space-y-8">
        <span class="badge badge-accent badge-soft w-fit gap-2 px-3 py-3 font-semibold">
          <AppIcon name="bolt" class="size-4" />
          Aluguel de carros sem burocracia
        </span>
        <h2 class="text-4xl leading-[1.1] font-extrabold tracking-tight xl:text-5xl">
          Seu próximo destino começa no
          <span class="text-accent">{{ brand.name.toLowerCase() }}</span
          >.
        </h2>
        <ul class="space-y-4">
          <li v-for="item in highlights" :key="item.icon" class="flex items-start gap-3">
            <span class="bg-neutral-content/10 grid size-9 shrink-0 place-items-center rounded-lg">
              <AppIcon :name="item.icon" class="text-accent size-5" />
            </span>
            <span class="text-neutral-content/80 pt-1.5">{{ item.text }}</span>
          </li>
        </ul>
      </div>

      <div class="-mx-12 xl:-mx-16">
        <div class="mx-auto w-4/5 max-w-lg">
          <HeroCar />
        </div>
        <div class="road-line mt-1 h-1.5 opacity-60"></div>
      </div>
    </aside>

    <section class="bg-base-100 relative flex items-center justify-center px-5 py-20 sm:px-10">
      <RouterLink
        :to="{ name: 'home' }"
        class="text-base-content/60 hover:text-base-content absolute top-6 right-6 hidden items-center gap-2 text-sm font-medium transition lg:flex"
      >
        <AppIcon name="arrowRight" class="size-4 rotate-180" /> Voltar ao site
      </RouterLink>
      <div class="w-full" :class="wide ? 'max-w-lg' : 'max-w-sm'">
        <RouterLink
          :to="{ name: 'home' }"
          class="mb-10 inline-block lg:hidden"
          aria-label="Página inicial"
        >
          <BrandLogo :size="36" />
        </RouterLink>
        <header class="mb-8 space-y-2">
          <h1 class="text-3xl font-extrabold tracking-tight">{{ title }}</h1>
          <p v-if="subtitle" class="text-base-content/70">{{ subtitle }}</p>
        </header>
        <slot />
      </div>
    </section>
  </div>
</template>
