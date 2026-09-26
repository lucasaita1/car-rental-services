<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CarCard from '@/components/CarCard.vue'
import RentModal from '@/components/RentModal.vue'
import { listCars } from '@/api/cars'
import { holdCar } from '@/api/rentals'
import { errorMessage } from '@/api/http'
import type { Car, CarStatus } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()
const route = useRoute()

const cars = ref<Car[]>([])
const loading = ref(true)
const error = ref('')
const search = ref(typeof route.query.busca === 'string' ? route.query.busca : '')
const returnDate = typeof route.query.devolucao === 'string' ? route.query.devolucao : null
const statusFilter = ref<CarStatus | ''>('')

const rentOpen = ref(false)
const selected = ref<Car | null>(null)
const expiresAt = ref<string | null>(null)
const heldCarId = ref<number | null>(null)
const holding = ref<number | null>(null)

const filtered = computed(() => {
  const term = search.value.trim().toLowerCase()
  return cars.value.filter(
    (c) =>
      (!statusFilter.value || c.status === statusFilter.value) &&
      (!term || c.model.toLowerCase().includes(term) || c.plate.toLowerCase().includes(term)),
  )
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    cars.value = await listCars()
  } catch (e) {
    error.value = errorMessage(e, 'Não foi possível carregar o catálogo.')
  } finally {
    loading.value = false
  }
}

async function openRent(car: Car) {
  if (!auth.isAuthenticated) {
    toast.info('Entre na sua conta para alugar um carro.')
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  holding.value = car.id
  try {
    const hold = await holdCar(car.id)
    selected.value = car
    expiresAt.value = hold.expiresAt
    heldCarId.value = car.id
    rentOpen.value = true
  } catch (e) {
    toast.warning(errorMessage(e, 'Não foi possível reservar este carro.'))
    await load()
  } finally {
    holding.value = null
  }
}

function onCheckoutClosed() {
  heldCarId.value = null
  load()
}

onMounted(load)
</script>

<template>
  <section class="space-y-6">
    <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 class="text-3xl font-bold">Catálogo</h1>
        <p class="text-base-content/70">Escolha um carro disponível e reserve em poucos cliques.</p>
      </div>
      <div class="flex flex-col gap-2 sm:flex-row">
        <input v-model="search" class="input" placeholder="Buscar por modelo ou placa" />
        <select v-model="statusFilter" class="select">
          <option value="">Todos os status</option>
          <option value="AVAILABLE">Disponível</option>
          <option value="RENTED">Alugado</option>
          <option value="MAINTENANCE">Manutenção</option>
        </select>
      </div>
    </div>

    <div v-if="loading" class="flex justify-center py-16">
      <span class="loading loading-spinner loading-lg"></span>
    </div>

    <div v-else-if="error" role="alert" class="alert alert-error alert-soft">
      <span>{{ error }}</span>
      <button class="btn btn-sm" @click="load">Tentar de novo</button>
    </div>

    <div v-else-if="filtered.length === 0" class="py-16 text-center text-base-content/70">
      Nenhum carro encontrado.
    </div>

    <div v-else class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <CarCard
        v-for="car in filtered"
        :key="car.id"
        :car="car"
        :held-by-me="car.id === heldCarId"
        :class="{ 'pointer-events-none opacity-60': holding === car.id }"
        @rent="openRent"
      />
    </div>

    <RentModal
      v-model:open="rentOpen"
      :car="selected"
      :expires-at="expiresAt"
      :initial-return-date="returnDate"
      @rented="onCheckoutClosed"
      @released="onCheckoutClosed"
    />
  </section>
</template>
