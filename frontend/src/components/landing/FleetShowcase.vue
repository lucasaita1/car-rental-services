<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { assetUrl, carApi } from '@/api/http'
import type { Car } from '@/api/types'
import { formatCurrency } from '@/utils/format'

const props = defineProps<{ cars: Car[]; loading: boolean; failed: boolean }>()

const isAvailable = (car: Car) =>
  car.status !== 'RENTED' && car.status !== 'MAINTENANCE' && !car.reserved

const featured = computed(() =>
  [...props.cars]
    .sort(
      (a, b) =>
        Number(isAvailable(b)) - Number(isAvailable(a)) ||
        Number(!!b.photoUrl) - Number(!!a.photoUrl),
    )
    .slice(0, 6),
)
const availableCount = computed(() => props.cars.filter(isAvailable).length)
</script>

<template>
  <section id="frota" class="mx-auto max-w-6xl scroll-mt-20 px-4 py-20">
    <div class="mb-10 flex flex-col justify-between gap-4 md:flex-row md:items-end">
      <div class="max-w-2xl space-y-3">
        <p class="text-primary font-semibold tracking-wide uppercase">Nossa frota</p>
        <h2 class="text-3xl font-extrabold tracking-tight md:text-4xl">
          Carros prontos para a sua próxima viagem
        </h2>
        <p class="text-base-content/70">
          Disponibilidade atualizada em tempo real. O que aparece aqui como disponível pode ser
          reservado agora.
        </p>
      </div>
      <div
        v-if="!loading && !failed && cars.length"
        class="stats border-base-300 bg-base-100 border shadow-sm"
      >
        <div class="stat px-5 py-3">
          <div class="stat-title">Disponíveis</div>
          <div class="stat-value text-success text-3xl">{{ availableCount }}</div>
        </div>
        <div class="stat px-5 py-3">
          <div class="stat-title">Na frota</div>
          <div class="stat-value text-3xl">{{ cars.length }}</div>
        </div>
      </div>
    </div>

    <div v-if="loading" class="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      <div v-for="i in 3" :key="i" class="skeleton h-72 w-full"></div>
    </div>

    <div
      v-else-if="failed || !cars.length"
      class="rounded-box border-base-300 bg-base-100 border p-10 text-center"
    >
      <p class="text-lg font-semibold">A frota está sendo preparada.</p>
      <p class="text-base-content/70 mt-1">Volte em instantes ou confira o catálogo completo.</p>
    </div>

    <div v-else class="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      <RouterLink
        v-for="car in featured"
        :key="car.id"
        :to="{ name: 'catalog', query: { busca: car.model } }"
        class="group card bg-base-100 border-base-300 overflow-hidden border shadow-sm transition hover:-translate-y-1 hover:shadow-xl"
      >
        <figure class="bg-base-200 aspect-[16/10] overflow-hidden">
          <img
            v-if="car.photoUrl"
            :src="assetUrl(carApi, car.photoUrl) ?? ''"
            :alt="car.model"
            class="size-full object-cover transition duration-500 group-hover:scale-105"
            loading="lazy"
          />
          <AppIcon v-else name="key" class="text-base-content/20 size-14" />
        </figure>
        <div class="card-body gap-1">
          <div class="flex items-start justify-between gap-2">
            <h3 class="card-title">{{ car.model }}</h3>
            <span v-if="car.reserved" class="badge badge-warning badge-soft">Reservado</span>
            <StatusBadge v-else :status="car.status" />
          </div>
          <p class="text-base-content/70">{{ car.year }} · {{ car.color }}</p>
          <div class="mt-3 flex items-end justify-between gap-2">
            <p class="text-primary flex items-center gap-1 text-sm font-semibold">
              {{ isAvailable(car) ? 'Reservar este carro' : 'Ver detalhes' }}
              <AppIcon name="arrowRight" class="size-4 transition group-hover:translate-x-1" />
            </p>
            <p v-if="car.dailyRate !== null" class="text-right leading-tight">
              <span class="text-lg font-extrabold">{{ formatCurrency(car.dailyRate) }}</span>
              <span class="text-base-content/60 text-xs">/dia</span>
            </p>
          </div>
        </div>
      </RouterLink>
    </div>

    <div class="mt-10 text-center">
      <RouterLink :to="{ name: 'catalog' }" class="btn btn-outline btn-primary btn-lg">
        Ver a frota completa
        <AppIcon name="arrowRight" class="size-5" />
      </RouterLink>
    </div>
  </section>
</template>
