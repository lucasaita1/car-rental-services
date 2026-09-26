<script setup lang="ts">
import { ref } from 'vue'
import AppIcon from './AppIcon.vue'
import { validatePdf } from '@/utils/image'

withDefaults(defineProps<{ uploadedAt?: string | null; busy?: boolean; required?: boolean }>(), {
  uploadedAt: null,
  busy: false,
  required: false,
})
const emit = defineEmits<{ select: [file: File]; view: [] }>()

const input = ref<HTMLInputElement>()
const fileName = ref('')
const error = ref('')

function onChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  error.value = validatePdf(file)
  if (error.value) {
    fileName.value = ''
    if (input.value) input.value.value = ''
    return
  }
  fileName.value = file.name
  emit('select', file)
}

function formatUploadedAt(value: string) {
  return new Date(value).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}
</script>

<template>
  <div class="rounded-box border-base-300 flex flex-col gap-3 border border-dashed p-4">
    <div class="flex items-start gap-3">
      <span
        class="grid size-10 shrink-0 place-items-center rounded-lg"
        :class="
          uploadedAt || fileName ? 'bg-success/10 text-success' : 'bg-warning/15 text-warning'
        "
      >
        <AppIcon :name="uploadedAt || fileName ? 'shield' : 'alert'" class="size-5" />
      </span>
      <div class="min-w-0 flex-1">
        <p class="font-semibold">CNH em PDF<span v-if="required" class="text-error"> *</span></p>
        <p class="text-base-content/70 truncate text-sm">
          <template v-if="fileName">{{ fileName }}</template>
          <template v-else-if="uploadedAt">Enviada em {{ formatUploadedAt(uploadedAt) }}</template>
          <template v-else>Obrigatória para alugar. Até 5 MB.</template>
        </p>
      </div>
    </div>
    <div class="flex flex-wrap gap-2">
      <label class="btn btn-sm" :class="uploadedAt ? 'btn-outline' : 'btn-primary'">
        <span v-if="busy" class="loading loading-spinner loading-xs"></span>
        {{ uploadedAt || fileName ? 'Substituir PDF' : 'Escolher PDF' }}
        <input
          ref="input"
          type="file"
          accept="application/pdf,.pdf"
          class="hidden"
          :disabled="busy"
          @change="onChange"
        />
      </label>
      <button v-if="uploadedAt" type="button" class="btn btn-ghost btn-sm" @click="emit('view')">
        Ver documento
      </button>
    </div>
    <p v-if="error" class="text-error text-sm">{{ error }}</p>
  </div>
</template>
