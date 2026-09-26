<script setup lang="ts">
import { ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'

withDefaults(defineProps<{ autocomplete?: string; placeholder?: string }>(), {
  autocomplete: 'current-password',
  placeholder: 'Sua senha',
})
const model = defineModel<string>({ required: true })
const visible = ref(false)
</script>

<template>
  <label class="input input-lg w-full">
    <AppIcon name="lock" class="text-base-content/50 size-5" />
    <input
      v-model="model"
      :type="visible ? 'text' : 'password'"
      :autocomplete="autocomplete"
      :placeholder="placeholder"
      class="grow"
      required
    />
    <button
      type="button"
      class="btn btn-ghost btn-sm btn-circle -mr-2"
      :aria-label="visible ? 'Ocultar senha' : 'Mostrar senha'"
      @click="visible = !visible"
    >
      <AppIcon :name="visible ? 'eyeSlash' : 'eye'" class="size-5" />
    </button>
  </label>
</template>
