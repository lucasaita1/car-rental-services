<script setup lang="ts">
import type { CarStatus } from '@/api/types'
import { carStatusBadge, carStatusLabel } from '@/utils/format'

defineProps<{ status: CarStatus | null }>()
</script>

<template>
  <span v-if="status" class="badge badge-soft" :class="carStatusBadge[status]">
    {{ carStatusLabel[status] }}
  </span>
  <span v-else class="badge badge-ghost">Sem status</span>
</template>
