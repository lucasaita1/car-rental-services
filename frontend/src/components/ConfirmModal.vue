<script setup lang="ts">
import BaseModal from './BaseModal.vue'

withDefaults(
  defineProps<{
    title: string
    message: string
    confirmLabel?: string
    danger?: boolean
    loading?: boolean
  }>(),
  { confirmLabel: 'Confirmar', danger: false, loading: false },
)
const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{ confirm: [] }>()
</script>

<template>
  <BaseModal v-model:open="open" :title="title">
    <p>{{ message }}</p>
    <template #actions>
      <button class="btn btn-ghost" :disabled="loading" @click="open = false">Cancelar</button>
      <button
        class="btn"
        :class="danger ? 'btn-error' : 'btn-primary'"
        :disabled="loading"
        @click="emit('confirm')"
      >
        <span v-if="loading" class="loading loading-spinner loading-sm"></span>
        {{ confirmLabel }}
      </button>
    </template>
  </BaseModal>
</template>
