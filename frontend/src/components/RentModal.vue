<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import BaseModal from './BaseModal.vue'
import CarSpecs from './CarSpecs.vue'
import { releaseHold, rentCar } from '@/api/rentals'
import { assetUrl, carApi, errorMessage } from '@/api/http'
import type { Car } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import {
  estimateTotal,
  formatCountdown,
  formatCurrency,
  formatDate,
  rentalDays,
  todayIso,
} from '@/utils/format'

const props = defineProps<{
  car: Car | null
  expiresAt: string | null
  initialReturnDate?: string | null
}>()
const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{ rented: []; released: [] }>()

const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

const expectedReturnDate = ref('')
const loading = ref(false)
const remaining = ref(0)
let rented = false
let timer: ReturnType<typeof setInterval> | undefined

const photo = computed(() => assetUrl(carApi, props.car?.photoUrl))
const days = computed(() =>
  expectedReturnDate.value ? rentalDays(todayIso(), expectedReturnDate.value) : null,
)
const total = computed(() =>
  props.car ? estimateTotal(props.car.dailyRate, todayIso(), expectedReturnDate.value) : null,
)

function tick() {
  remaining.value = props.expiresAt ? new Date(props.expiresAt).getTime() - Date.now() : 0
  if (open.value && remaining.value <= 0) {
    toast.warning('O tempo da sua reserva acabou. O carro foi liberado.')
    open.value = false
  }
}

function stopTimer() {
  if (timer) clearInterval(timer)
  timer = undefined
}

watch(open, (value) => {
  if (value) {
    expectedReturnDate.value =
      props.initialReturnDate && props.initialReturnDate >= todayIso()
        ? props.initialReturnDate
        : ''
    rented = false
    tick()
    timer = setInterval(tick, 1000)
    return
  }
  stopTimer()
  if (!rented && props.car) {
    releaseHold(props.car.id).catch(() => undefined)
    emit('released')
  }
})

onBeforeUnmount(stopTimer)

async function confirm() {
  if (!props.car || auth.userId === null || !expectedReturnDate.value) return
  loading.value = true
  try {
    const message = await rentCar(props.car.id, auth.userId, expectedReturnDate.value)

    if (message.includes('sucesso')) {
      rented = true
      toast.success(`${props.car.model} alugado com sucesso!`)
      emit('rented')
      open.value = false
    } else if (message.includes('login novamente')) {
      rented = true
      open.value = false
      auth.clearSession()
      toast.warning('Sua sessão de aluguel expirou. Entre novamente.')
      router.push({ name: 'login', query: { redirect: '/carros' } })
    } else {
      toast.warning(message)
    }
  } catch (e) {
    toast.error(errorMessage(e, 'Não foi possível concluir o aluguel.'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <BaseModal v-model:open="open" title="Confirmar aluguel">
    <div v-if="car" class="space-y-4">
      <div role="alert" class="alert alert-info alert-soft">
        <span>
          Carro reservado para você por
          <span class="font-mono font-bold">{{ formatCountdown(remaining) }}</span
          >. Outros clientes não conseguem alugá-lo enquanto isso.
        </span>
      </div>
      <div class="rounded-box bg-base-200 flex gap-4 overflow-hidden">
        <img v-if="photo" :src="photo" :alt="car.model" class="h-24 w-36 object-cover" />
        <div class="p-4">
          <p class="font-semibold">{{ car.model }} · {{ car.year }}</p>
          <p class="text-base-content/70 text-sm">{{ car.color }} · Placa {{ car.plate }}</p>
        </div>
      </div>
      <CarSpecs :details="car.details" />
      <label class="floating-label">
        <span>Data de devolução</span>
        <input
          v-model="expectedReturnDate"
          type="date"
          class="input w-full"
          :min="todayIso()"
          required
        />
      </label>
      <div class="rounded-box border-base-300 space-y-2 border p-4">
        <div class="flex justify-between text-sm">
          <span class="text-base-content/70">Diária</span>
          <span>{{ formatCurrency(car.dailyRate) }}</span>
        </div>
        <div class="flex justify-between text-sm">
          <span class="text-base-content/70">Período</span>
          <span v-if="days"
            >{{ days }} {{ days === 1 ? 'diária' : 'diárias' }} · até
            {{ formatDate(expectedReturnDate) }}</span
          >
          <span v-else class="text-base-content/50">Escolha a data de devolução</span>
        </div>
        <div class="border-base-300 flex items-end justify-between border-t pt-2">
          <span class="font-semibold">Total previsto</span>
          <span class="text-2xl font-extrabold">{{ formatCurrency(total) }}</span>
        </div>
      </div>
    </div>

    <template #actions>
      <button class="btn btn-ghost" :disabled="loading" @click="open = false">Cancelar</button>
      <button
        class="btn btn-primary"
        :disabled="loading || remaining <= 0 || !expectedReturnDate"
        @click="confirm"
      >
        <span v-if="loading" class="loading loading-spinner loading-sm"></span>
        Alugar{{ total !== null ? ` por ${formatCurrency(total)}` : '' }}
      </button>
    </template>
  </BaseModal>
</template>
