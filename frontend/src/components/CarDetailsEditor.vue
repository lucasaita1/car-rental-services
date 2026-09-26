<script setup lang="ts">
import { computed } from 'vue'
import type { CarDetail } from '@/api/types'

const details = defineModel<CarDetail[]>({ required: true })

const suggestions = [
  'Câmbio',
  'Motor',
  'Potência',
  'Combustível',
  'Condição',
  'Lugares',
  'Porta-malas',
  'Tração',
]
const available = computed(() =>
  suggestions.filter((s) => !details.value.some((d) => d.label.trim() === s)),
)

function add(label = '') {
  if (details.value.length >= 20) return
  details.value = [...details.value, { label, value: '' }]
}

function remove(index: number) {
  details.value = details.value.filter((_, i) => i !== index)
}
</script>

<template>
  <fieldset class="fieldset">
    <legend class="fieldset-legend">Detalhes do veículo (opcional)</legend>
    <p class="text-base-content/60 -mt-1 mb-1 text-xs">
      Aparecem no catálogo e no checkout. Linhas em branco são ignoradas.
    </p>

    <div v-for="(detail, index) in details" :key="index" class="flex items-center gap-2">
      <input
        v-model="detail.label"
        class="input input-sm w-2/5"
        placeholder="Ex.: Motor"
        maxlength="40"
        :aria-label="`Nome do detalhe ${index + 1}`"
      />
      <input
        v-model="detail.value"
        class="input input-sm flex-1"
        placeholder="Ex.: V8 4.0"
        maxlength="80"
        :aria-label="`Valor do detalhe ${index + 1}`"
      />
      <button
        type="button"
        class="btn btn-ghost btn-sm btn-square"
        :aria-label="`Remover detalhe ${index + 1}`"
        @click="remove(index)"
      >
        ✕
      </button>
    </div>

    <div class="mt-1 flex flex-wrap items-center gap-1.5">
      <button
        v-for="label in available"
        :key="label"
        type="button"
        class="badge badge-outline hover:badge-primary cursor-pointer"
        @click="add(label)"
      >
        + {{ label }}
      </button>
      <button type="button" class="btn btn-ghost btn-xs" @click="add()">+ Outro</button>
    </div>
  </fieldset>
</template>
