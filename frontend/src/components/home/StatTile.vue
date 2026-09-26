<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import type { IconName } from '@/utils/icons'

withDefaults(
  defineProps<{
    icon: IconName
    label: string
    value: number | null
    to: RouteLocationRaw
    tone?: 'primary' | 'success' | 'warning' | 'error'
  }>(),
  { tone: 'primary' },
)

const toneClass = {
  primary: 'bg-primary/10 text-primary',
  success: 'bg-success/10 text-success',
  warning: 'bg-warning/15 text-warning',
  error: 'bg-error/10 text-error',
}
</script>

<template>
  <RouterLink
    :to="to"
    class="group rounded-box bg-base-100 border-base-300 flex items-center gap-4 border p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
  >
    <span class="grid size-12 shrink-0 place-items-center rounded-xl" :class="toneClass[tone]">
      <AppIcon :name="icon" class="size-6" />
    </span>
    <span class="min-w-0 flex-1">
      <span class="block text-3xl leading-none font-extrabold">
        <span v-if="value === null" class="loading loading-dots loading-sm"></span>
        <template v-else>{{ value }}</template>
      </span>
      <span class="text-base-content/60 mt-1 block truncate text-sm">{{ label }}</span>
    </span>
    <AppIcon
      name="arrowRight"
      class="text-base-content/30 group-hover:text-primary size-5 transition group-hover:translate-x-1"
    />
  </RouterLink>
</template>
