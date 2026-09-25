<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'

defineProps<{ title: string }>()
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
    <div class="modal-box">
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
