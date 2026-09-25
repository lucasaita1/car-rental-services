<script setup lang="ts">
import { onMounted, ref } from 'vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import RentalsTable from '@/components/RentalsTable.vue'
import { rentalsByUser, returnCar } from '@/api/rentals'
import { errorMessage } from '@/api/http'
import type { Rental } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToastStore()

const rentals = ref<Rental[]>([])
const loading = ref(true)
const error = ref('')

const confirmOpen = ref(false)
const returning = ref(false)
const target = ref<Rental | null>(null)

async function load() {
  if (auth.userId === null) return
  loading.value = true
  error.value = ''
  try {
    rentals.value = await rentalsByUser(auth.userId)
  } catch (e) {
    error.value = errorMessage(e, 'Não foi possível carregar suas locações.')
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

onMounted(load)
</script>

<template>
  <section class="space-y-6">
    <div>
      <h1 class="text-3xl font-bold">Minhas locações</h1>
      <p class="text-base-content/70">Acompanhe seus aluguéis em andamento e o histórico.</p>
    </div>

    <div v-if="loading" class="flex justify-center py-16">
      <span class="loading loading-spinner loading-lg"></span>
    </div>
    <div v-else-if="error" role="alert" class="alert alert-error alert-soft">{{ error }}</div>
    <div v-else-if="rentals.length === 0" class="py-16 text-center text-base-content/70">
      Você ainda não alugou nenhum carro.
      <RouterLink :to="{ name: 'catalog' }" class="link link-primary">Ver catálogo</RouterLink>
    </div>
    <RentalsTable v-else :rentals="rentals" @return="askReturn" />

    <ConfirmModal
      v-model:open="confirmOpen"
      title="Devolver carro"
      :message="`Confirmar a devolução do ${target?.carModel} (${target?.carPlate})?`"
      confirm-label="Devolver"
      :loading="returning"
      @confirm="confirmReturn"
    />
  </section>
</template>
