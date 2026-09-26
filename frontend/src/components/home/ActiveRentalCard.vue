<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { assetUrl, carApi } from '@/api/http'
import type { Car, Rental } from '@/api/types'
import { daysUntil, formatCurrency, formatDate, todayIso } from '@/utils/format'

const props = defineProps<{ rental: Rental; car?: Car }>()

const photo = computed(() => assetUrl(carApi, props.car?.photoUrl))
const daysLeft = computed(() =>
  props.rental.expectedReturnDate ? daysUntil(props.rental.expectedReturnDate) : null,
)
const overdue = computed(
  () => props.rental.overdue || (daysLeft.value !== null && daysLeft.value < 0),
)

const status = computed(() => {
  if (overdue.value) return { label: 'Devolução atrasada', badge: 'badge-error' }
  if (daysLeft.value === 0) return { label: 'Devolve hoje', badge: 'badge-warning' }
  return { label: 'Em andamento', badge: 'badge-success' }
})

const countdown = computed(() => {
  if (daysLeft.value === null) return 'Sem data de devolução definida'
  if (daysLeft.value < 0) {
    const n = Math.abs(daysLeft.value)
    return `Atrasada há ${n} ${n === 1 ? 'dia' : 'dias'}`
  }
  if (daysLeft.value === 0) return 'Devolução prevista para hoje'
  return `Faltam ${daysLeft.value} ${daysLeft.value === 1 ? 'dia' : 'dias'} para devolver`
})

const progress = computed(() => {
  if (!props.rental.expectedReturnDate) return null
  const total = daysUntil(props.rental.expectedReturnDate, props.rental.rentalDate)
  if (total <= 0) return 100
  const elapsed = daysUntil(todayIso(), props.rental.rentalDate)
  return Math.min(100, Math.max(4, Math.round((elapsed / total) * 100)))
})
</script>

<template>
  <article
    class="card bg-base-100 border-base-300 overflow-hidden border shadow-sm sm:card-side"
    :class="{ 'border-error/40': overdue }"
  >
    <figure class="bg-base-200 aspect-video sm:aspect-auto sm:w-60 sm:shrink-0">
      <img v-if="photo" :src="photo" :alt="rental.carModel" class="size-full object-cover" />
      <AppIcon v-else name="key" class="text-base-content/20 size-14" />
    </figure>
    <div class="card-body gap-4">
      <div class="flex flex-wrap items-start justify-between gap-2">
        <div>
          <h3 class="text-xl font-bold">{{ rental.carModel }}</h3>
          <p class="text-base-content/60 font-mono text-sm tracking-wider">
            {{ rental.carPlate }}
          </p>
        </div>
        <span class="badge badge-soft" :class="status.badge">{{ status.label }}</span>
      </div>

      <dl class="grid grid-cols-2 gap-4 text-sm sm:grid-cols-3">
        <div>
          <dt class="text-base-content/60">Retirada</dt>
          <dd class="font-semibold">{{ formatDate(rental.rentalDate) }}</dd>
        </div>
        <div>
          <dt class="text-base-content/60">Devolução prevista</dt>
          <dd class="font-semibold">
            {{ rental.expectedReturnDate ? formatDate(rental.expectedReturnDate) : 'Em aberto' }}
          </dd>
        </div>
        <div v-if="rental.estimatedTotal !== null">
          <dt class="text-base-content/60">Total previsto</dt>
          <dd class="font-semibold">{{ formatCurrency(rental.estimatedTotal) }}</dd>
        </div>
      </dl>

      <div class="space-y-2">
        <p class="text-sm font-medium" :class="overdue ? 'text-error' : 'text-base-content/80'">
          {{ countdown }}
        </p>
        <progress
          v-if="progress !== null"
          class="progress w-full"
          :class="overdue ? 'progress-error' : 'progress-primary'"
          :value="progress"
          max="100"
        ></progress>
      </div>
    </div>
  </article>
</template>
