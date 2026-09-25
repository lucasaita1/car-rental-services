<script setup lang="ts">
import { computed } from 'vue'
import StatusBadge from './StatusBadge.vue'
import { assetUrl, carApi } from '@/api/http'
import type { Car } from '@/api/types'

const props = withDefaults(defineProps<{ car: Car; heldByMe?: boolean }>(), { heldByMe: false })
const emit = defineEmits<{ rent: [car: Car] }>()

const photo = computed(() => assetUrl(carApi, props.car.photoUrl))
const reservedByOther = computed(() => props.car.reserved && !props.heldByMe)
const rentable = computed(
  () =>
    props.car.status !== 'RENTED' && props.car.status !== 'MAINTENANCE' && !reservedByOther.value,
)
const actionLabel = computed(() => {
  if (props.heldByMe) return 'Continuar'
  if (rentable.value) return 'Alugar'
  return reservedByOther.value ? 'Reservado' : 'Indisponível'
})
</script>

<template>
  <div
    class="card bg-base-100 border-base-300 overflow-hidden border shadow-sm transition hover:shadow-md"
  >
    <figure class="bg-base-200 aspect-video">
      <img
        v-if="photo"
        :src="photo"
        :alt="car.model"
        class="size-full object-cover"
        loading="lazy"
      />
      <span v-else class="text-base-content/40 text-sm">Sem foto</span>
    </figure>
    <div class="card-body">
      <div class="flex items-start justify-between gap-2">
        <h2 class="card-title">{{ car.model }}</h2>
        <span v-if="reservedByOther" class="badge badge-warning badge-soft">Reservado</span>
        <StatusBadge v-else :status="car.status" />
      </div>
      <p class="text-base-content/70">{{ car.year }} · {{ car.color }}</p>
      <p class="font-mono text-sm tracking-wider">{{ car.plate }}</p>
      <div class="card-actions mt-2 justify-end">
        <button class="btn btn-primary btn-sm" :disabled="!rentable" @click="emit('rent', car)">
          {{ actionLabel }}
        </button>
      </div>
    </div>
  </div>
</template>
