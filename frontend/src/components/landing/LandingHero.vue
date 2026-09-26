<script setup lang="ts">
import HeroCar from './HeroCar.vue'
import AppIcon from '@/components/AppIcon.vue'
import CarSearchForm from '@/components/CarSearchForm.vue'
import { brand } from '@/brand'

defineProps<{ availableCount: number | null }>()
</script>

<template>
  <section class="bg-neutral text-neutral-content relative isolate overflow-hidden">
    <div
      class="mx-auto grid max-w-6xl items-center gap-12 px-4 pt-16 pb-20 lg:grid-cols-2 lg:pt-24 lg:pb-28"
    >
      <div class="space-y-8">
        <span class="badge badge-accent badge-soft gap-2 px-3 py-3 font-semibold">
          <AppIcon name="bolt" class="size-4" />
          Aluguel de carros sem burocracia
        </span>

        <h1 class="text-4xl leading-[1.05] font-extrabold tracking-tight md:text-6xl">
          Seu próximo destino começa no
          <span class="text-accent">{{ brand.name.toLowerCase() }}</span
          >.
        </h1>

        <p class="text-neutral-content/75 max-w-xl text-lg">
          Escolha o carro, reserve em poucos cliques e saia dirigindo. Enquanto você confirma, o
          carro fica garantido só para você.
        </p>

        <CarSearchForm />

        <div class="text-neutral-content/70 flex flex-wrap items-center gap-x-6 gap-y-2 text-sm">
          <span v-if="availableCount !== null" class="flex items-center gap-2">
            <span class="status status-success animate-pulse"></span>
            <strong class="text-neutral-content">{{ availableCount }}</strong>
            {{ availableCount === 1 ? 'carro disponível' : 'carros disponíveis' }} agora
          </span>
          <span class="flex items-center gap-2">
            <AppIcon name="clock" class="size-4" /> Reserva garantida por 10 minutos
          </span>
          <span class="flex items-center gap-2">
            <AppIcon name="shield" class="size-4" /> Conta protegida
          </span>
        </div>
      </div>

      <div class="relative">
        <HeroCar />
        <div class="road-line mx-auto mt-2 h-1.5 w-11/12 rounded-full opacity-70"></div>
      </div>
    </div>
  </section>
</template>
