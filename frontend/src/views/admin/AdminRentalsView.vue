<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import RentalsTable from '@/components/RentalsTable.vue'
import { activeRentals, overdueRentals, returnCar } from '@/api/rentals'
import { errorMessage } from '@/api/http'
import type { Rental } from '@/api/types'
import { useToastStore } from '@/stores/toast'

type Tab = 'active' | 'overdue'

const toast = useToastStore()

const tab = ref<Tab>('active')
const rentals = ref<Rental[]>([])
const loading = ref(true)
const error = ref('')

const confirmOpen = ref(false)
const returning = ref(false)
const target = ref<Rental | null>(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    rentals.value = tab.value === 'active' ? await activeRentals() : await overdueRentals()
  } catch (e) {
    error.value = errorMessage(e, 'Não foi possível carregar as locações.')
  } finally {
    loading.value = false
  }
}

function askReturn(rental: Rental) {
  target.value = rental
  confirmOpen.value = true
}

async function confirmReturn() {
  if (!target.value) return
  returning.value = true
  try {
    toast.success(await returnCar(target.value.carId))
    confirmOpen.value = false
    await load()
  } catch (e) {
    toast.error(errorMessage(e, 'Não foi possível registrar a devolução.'))
  } finally {
    returning.value = false
  }
}

watch(tab, load)
onMounted(load)
</script>

<template>
  <section class="space-y-6">
    <div>
      <h1 class="text-3xl font-bold">Locações</h1>
      <p class="text-base-content/70">Acompanhe os aluguéis em aberto e registre devoluções.</p>
    </div>

    <div role="tablist" class="tabs tabs-box w-fit">
      <button
        role="tab"
        class="tab"
        :class="{ 'tab-active': tab === 'active' }"
        @click="tab = 'active'"
      >
        Em andamento
      </button>
      <button
        role="tab"
        class="tab"
        :class="{ 'tab-active': tab === 'overdue' }"
        @click="tab = 'overdue'"
      >
        Atrasadas
      </button>
    </div>

    <div v-if="loading" class="flex justify-center py-16">
      <span class="loading loading-spinner loading-lg"></span>
    </div>
    <div v-else-if="error" role="alert" class="alert alert-error alert-soft">{{ error }}</div>
    <div v-else-if="rentals.length === 0" class="py-16 text-center text-base-content/70">
      {{ tab === 'active' ? 'Nenhuma locação em andamento.' : 'Nenhuma locação atrasada.' }}
    </div>
    <RentalsTable v-else :rentals="rentals" show-customer @return="askReturn" />

    <ConfirmModal
      v-model:open="confirmOpen"
      title="Registrar devolução"
      :message="`Registrar a devolução do ${target?.carModel} (${target?.carPlate}) de ${target?.userName}?`"
      confirm-label="Registrar devolução"
      :loading="returning"
      @confirm="confirmReturn"
    />
  </section>
</template>
