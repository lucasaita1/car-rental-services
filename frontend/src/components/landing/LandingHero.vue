<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import HeroCar from './HeroCar.vue'
import AppIcon from '@/components/AppIcon.vue'
import { brand } from '@/brand'
import { buildCatalogQuery } from '@/utils/catalogQuery'
import { todayIso } from '@/utils/format'

defineProps<{ availableCount: number | null }>()

const router = useRouter()
const search = ref('')
const returnDate = ref('')

function submit() {
  router.push({ name: 'catalog', query: buildCatalogQuery(search.value, returnDate.value) })
}
</script>

<template>
  <section class="bg-neutral text-neutral-content relative isolate overflow-hidden">
    <div
      class="bg-primary/40 pointer-events-none absolute -top-40 -left-32 -z-10 size-[36rem] rounded-full blur-3xl"
    ></div>
    <div
      class="bg-accent/20 pointer-events-none absolute -right-40 bottom-0 -z-10 size-[30rem] rounded-full blur-3xl"
    ></div>
    <div
      class="pointer-events-none absolute inset-0 -z-10 opacity-[0.07]"
      style="
        background-image:
          linear-gradient(currentColor 1px, transparent 1px),
          linear-gradient(90deg, currentColor 1px, transparent 1px);
        background-size: 48px 48px;
      "
    ></div>

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

        <form class="bg-base-100 text-base-content rounded-box shadow-2xl" @submit.prevent="submit">
          <label class="border-base-300 flex items-center gap-3 border-b px-5 py-4">
            <AppIcon name="search" class="text-primary size-5 shrink-0" />
            <span class="flex-1">
              <span class="text-base-content/60 block text-xs font-semibold uppercase"
                >Qual carro você procura?</span
              >
              <input
                v-model="search"
                type="search"
                placeholder="Ex.: Civic, Onix, T-Cross"
                class="w-full bg-transparent text-base font-medium outline-none"
              />
            </span>
          </label>
          <div class="flex flex-col gap-3 p-3 sm:flex-row sm:items-center">
            <label class="flex flex-1 items-center gap-3 px-2">
              <AppIcon name="calendar" class="text-primary size-5 shrink-0" />
              <span class="flex-1">
                <span class="text-base-content/60 block text-xs font-semibold uppercase"
                  >Devolver até (opcional)</span
                >
                <input
                  v-model="returnDate"
                  type="date"
                  :min="todayIso()"
                  class="w-full bg-transparent font-medium outline-none"
                  :class="{ 'text-base-content/50': !returnDate }"
                />
              </span>
            </label>
            <button class="btn btn-primary btn-lg">
              Ver carros disponíveis
              <AppIcon name="arrowRight" class="size-5" />
            </button>
          </div>
        </form>

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
