<script setup lang="ts">
import { computed } from 'vue'
import type { CarDetail } from '@/api/types'

const props = withDefaults(defineProps<{ details: CarDetail[] | null; limit?: number }>(), {
  limit: 0,
})

const shown = computed(() => {
  const list = props.details ?? []
  return props.limit > 0 ? list.slice(0, props.limit) : list
})
const hidden = computed(() => (props.details?.length ?? 0) - shown.value.length)
</script>

<template>
  <ul v-if="shown.length" class="flex flex-wrap gap-1.5">
    <li
      v-for="detail in shown"
      :key="detail.label"
      class="bg-base-200 text-base-content/80 rounded-full px-2.5 py-1 text-xs"
    >
      <span class="text-base-content/50">{{ detail.label }}:</span> {{ detail.value }}
    </li>
    <li v-if="hidden > 0" class="text-base-content/50 px-1 py-1 text-xs">+{{ hidden }}</li>
  </ul>
</template>
