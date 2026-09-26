<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'

withDefaults(defineProps<{ title: string; wide?: boolean }>(), { wide: false })
const open = defineModel<boolean>('open', { required: true })
const dialog = ref<HTMLDialogElement>()

function sync(value: boolean) {
  const el = dialog.value
  if (!el) return
  if (value && !el.open) el.showModal()
  if (!value && el.open) el.close()
}

watch(open, sync)
onMounted(() => sync(open.value))
</script>

<template>
  <dialog ref="dialog" class="modal" @close="open = false">
    <div class="modal-box" :class="{ 'max-w-2xl': wide }">
      <h3 class="text-lg font-bold">{{ title }}</h3>
      <div class="py-4">
        <slot />
      </div>
      <div class="modal-action">
        <slot name="actions" />
      </div>
    </div>
    <form method="dialog" class="modal-backdrop">
      <button>fechar</button>
    </form>
  </dialog>
</template>
