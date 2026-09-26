<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from './AppIcon.vue'
import { buildCatalogQuery } from '@/utils/catalogQuery'
import { todayIso } from '@/utils/format'

withDefaults(defineProps<{ inline?: boolean }>(), { inline: false })

const router = useRouter()
const search = ref('')
const returnDate = ref('')

function submit() {
  router.push({ name: 'catalog', query: buildCatalogQuery(search.value, returnDate.value) })
}
</script>

<template>
  <form
    class="bg-base-100 text-base-content rounded-box shadow-2xl"
    :class="inline ? 'flex flex-col lg:flex-row lg:items-center' : ''"
    @submit.prevent="submit"
  >
    <label
      class="border-base-300 flex items-center gap-3 px-5 py-4"
      :class="inline ? 'border-b lg:flex-[1.4] lg:border-r lg:border-b-0' : 'border-b'"
    >
      <AppIcon name="search" class="text-primary size-5 shrink-0" />
      <span class="flex-1">
        <span class="text-base-content/60 block text-xs font-semibold uppercase"
          >Qual carro você procura?</span
        >
        <input
          v-model="search"
          type="search"
          placeholder="Ex.: Civic, Onix, T-Cross"
          class="w-full bg-transparent text-base font-medium outline-none"
        />
      </span>
    </label>
    <div
      class="flex flex-col gap-3 p-3 sm:flex-row sm:items-center"
      :class="{ 'lg:flex-1': inline }"
    >
      <label class="flex flex-1 items-center gap-3 px-2">
        <AppIcon name="calendar" class="text-primary size-5 shrink-0" />
        <span class="flex-1">
          <span class="text-base-content/60 block text-xs font-semibold uppercase"
            >Devolver até (opcional)</span
          >
          <input
            v-model="returnDate"
            type="date"
            :min="todayIso()"
            class="w-full bg-transparent font-medium outline-none"
            :class="{ 'text-base-content/50': !returnDate }"
          />
        </span>
      </label>
      <button class="btn btn-primary btn-lg">
        {{ inline ? 'Buscar' : 'Ver carros disponíveis' }}
        <AppIcon name="arrowRight" class="size-5" />
      </button>
    </div>
  </form>
</template>
