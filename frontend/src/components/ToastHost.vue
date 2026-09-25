<script setup lang="ts">
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

const alertClass = {
  success: 'alert-success',
  error: 'alert-error',
  info: 'alert-info',
  warning: 'alert-warning',
} as const
</script>

<template>
  <div class="toast toast-end toast-bottom z-50">
    <div
      v-for="t in toast.toasts"
      :key="t.id"
      role="alert"
      class="alert shadow-lg"
      :class="alertClass[t.type]"
    >
      <span>{{ t.message }}</span>
      <button class="btn btn-ghost btn-xs" @click="toast.dismiss(t.id)">✕</button>
    </div>
  </div>
</template>
