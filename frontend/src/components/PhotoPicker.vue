<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { ACCEPTED_IMAGE_TYPES, validateImage } from '@/utils/image'

const props = withDefaults(
  defineProps<{ currentUrl?: string | null; label?: string; round?: boolean }>(),
  {
    currentUrl: null,
    label: 'Foto',
    round: false,
  },
)
const emit = defineEmits<{ select: [file: File]; remove: [] }>()

const preview = ref<string | null>(null)
const error = ref('')
const input = ref<HTMLInputElement>()

const shown = computed(() => preview.value ?? props.currentUrl)

function clearPreview() {
  if (preview.value) URL.revokeObjectURL(preview.value)
  preview.value = null
}

function onChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  error.value = validateImage(file)
  if (error.value) {
    if (input.value) input.value.value = ''
    return
  }
  clearPreview()
  preview.value = URL.createObjectURL(file)
  emit('select', file)
}

function remove() {
  clearPreview()
  if (input.value) input.value.value = ''
  emit('remove')
}

onBeforeUnmount(clearPreview)
</script>

<template>
  <div class="flex items-center gap-4">
    <div
      class="bg-base-200 flex shrink-0 items-center justify-center overflow-hidden"
      :class="round ? 'size-20 rounded-full' : 'h-24 w-36 rounded-box'"
    >
      <img v-if="shown" :src="shown" alt="" class="size-full object-cover" />
      <span v-else class="text-base-content/50 text-xs">Sem foto</span>
    </div>
    <div class="space-y-2">
      <label class="btn btn-sm btn-outline">
        {{ shown ? 'Trocar' : 'Escolher' }} {{ label.toLowerCase() }}
        <input
          ref="input"
          type="file"
          class="hidden"
          :accept="ACCEPTED_IMAGE_TYPES.join(',')"
          @change="onChange"
        />
      </label>
      <button v-if="shown" type="button" class="btn btn-sm btn-ghost text-error" @click="remove">
        Remover
      </button>
      <p class="text-base-content/60 text-xs">JPG, PNG ou WebP até 5 MB.</p>
      <p v-if="error" class="text-error text-xs">{{ error }}</p>
    </div>
  </div>
</template>
