<script setup lang="ts">
import StatusBadge from './StatusBadge.vue'
import { computed } from 'vue'
import type { Car } from '@/api/types'

const props = defineProps<{ car: Car }>()
const emit = defineEmits<{ rent: [car: Car] }>()

const rentable = computed(() => props.car.status !== 'RENTED' && props.car.status !== 'MAINTENANCE')
</script>

<template>
  <div class="card bg-base-100 border border-base-300 shadow-sm transition hover:shadow-md">
    <div class="card-body">
      <div class="flex items-start justify-between gap-2">
        <h2 class="card-title">{{ car.model }}</h2>
        <StatusBadge :status="car.status" />
      </div>
      <p class="text-base-content/70">{{ car.year }} · {{ car.color }}</p>
      <p class="font-mono text-sm tracking-wider">{{ car.plate }}</p>
      <div class="card-actions mt-2 justify-end">
        <button class="btn btn-primary btn-sm" :disabled="!rentable" @click="emit('rent', car)">
          {{ rentable ? 'Alugar' : 'Indisponível' }}
        </button>
      </div>
    </div>
  </div>
</template>
