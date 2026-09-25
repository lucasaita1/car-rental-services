<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import CarFormModal from '@/components/CarFormModal.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { deleteCar, listCars } from '@/api/cars'
import { assetUrl, carApi, errorMessage } from '@/api/http'
import type { Car } from '@/api/types'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

const cars = ref<Car[]>([])
const loading = ref(true)
const error = ref('')

const formOpen = ref(false)
const editing = ref<Car | null>(null)

const deleteOpen = ref(false)
const deleting = ref(false)
const target = ref<Car | null>(null)

const stats = computed(() => ({
  total: cars.value.length,
  available: cars.value.filter((c) => c.status === 'AVAILABLE').length,
  rented: cars.value.filter((c) => c.status === 'RENTED').length,
  maintenance: cars.value.filter((c) => c.status === 'MAINTENANCE').length,
}))

async function load() {
  loading.value = true
  error.value = ''
  try {
    cars.value = await listCars()
  } catch (e) {
    error.value = errorMessage(e, 'Não foi possível carregar a frota.')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  formOpen.value = true
}

function openEdit(car: Car) {
  editing.value = car
  formOpen.value = true
}

function askDelete(car: Car) {
  target.value = car
  deleteOpen.value = true
}

async function confirmDelete() {
  if (!target.value) return
  deleting.value = true
  try {
    await deleteCar(target.value.id)
    toast.success('Carro removido.')
    deleteOpen.value = false
    await load()
  } catch (e) {
    toast.error(errorMessage(e, 'Não foi possível remover o carro.'))
  } finally {
    deleting.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="space-y-6">
    <div class="flex items-end justify-between">
      <div>
        <h1 class="text-3xl font-bold">Frota</h1>
        <p class="text-base-content/70">Cadastre, edite e remova carros do catálogo.</p>
      </div>
      <button class="btn btn-primary" @click="openCreate">Novo carro</button>
    </div>

    <div class="stats stats-vertical w-full border border-base-300 bg-base-100 sm:stats-horizontal">
      <div class="stat">
        <div class="stat-title">Total</div>
        <div class="stat-value">{{ stats.total }}</div>
      </div>
      <div class="stat">
        <div class="stat-title">Disponíveis</div>
        <div class="stat-value text-success">{{ stats.available }}</div>
      </div>
      <div class="stat">
        <div class="stat-title">Alugados</div>
        <div class="stat-value text-warning">{{ stats.rented }}</div>
      </div>
      <div class="stat">
        <div class="stat-title">Manutenção</div>
        <div class="stat-value text-error">{{ stats.maintenance }}</div>
      </div>
    </div>

    <div v-if="loading" class="flex justify-center py-16">
      <span class="loading loading-spinner loading-lg"></span>
    </div>
    <div v-else-if="error" role="alert" class="alert alert-error alert-soft">{{ error }}</div>

    <div v-else class="overflow-x-auto rounded-box border border-base-300 bg-base-100">
      <table class="table">
        <thead>
          <tr>
            <th></th>
            <th>Modelo</th>
            <th>Ano</th>
            <th>Cor</th>
            <th>Placa</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="cars.length === 0">
            <td colspan="7" class="py-10 text-center text-base-content/70">
              Nenhum carro cadastrado.
            </td>
          </tr>
          <tr v-for="car in cars" :key="car.id">
            <td class="w-20">
              <div class="bg-base-200 h-10 w-16 overflow-hidden rounded">
                <img
                  v-if="car.photoUrl"
                  :src="assetUrl(carApi, car.photoUrl) ?? ''"
                  alt=""
                  class="size-full object-cover"
                />
              </div>
            </td>
            <td class="font-medium">{{ car.model }}</td>
            <td>{{ car.year }}</td>
            <td>{{ car.color }}</td>
            <td class="font-mono">{{ car.plate }}</td>
            <td>
              <StatusBadge :status="car.status" />
              <span v-if="car.reserved" class="badge badge-warning badge-soft ml-1"
                >Em checkout</span
              >
            </td>
            <td class="space-x-2 text-right">
              <button class="btn btn-sm btn-ghost" @click="openEdit(car)">Editar</button>
              <button
                class="btn btn-sm btn-ghost text-error"
                :disabled="car.status === 'RENTED'"
                :title="car.status === 'RENTED' ? 'Registre a devolução antes de remover' : ''"
                @click="askDelete(car)"
              >
                Remover
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <CarFormModal v-model:open="formOpen" :car="editing" @saved="load" />
    <ConfirmModal
      v-model:open="deleteOpen"
      title="Remover carro"
      :message="`Remover ${target?.model} (${target?.plate}) do catálogo? Esta ação não pode ser desfeita.`"
      confirm-label="Remover"
      danger
      :loading="deleting"
      @confirm="confirmDelete"
    />
  </section>
</template>
