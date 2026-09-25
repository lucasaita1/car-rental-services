<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import BaseModal from './BaseModal.vue'
import { rentCar } from '@/api/rentals'
import { errorMessage } from '@/api/http'
import type { Car } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { todayIso } from '@/utils/format'

const props = defineProps<{ car: Car | null }>()
const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{ rented: [] }>()

const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

const expectedReturnDate = ref('')
const loading = ref(false)

watch(open, (value) => {
  if (value) expectedReturnDate.value = ''
})

async function confirm() {
  if (!props.car || auth.userId === null) return
  loading.value = true
  try {
    const message = await rentCar(props.car.id, auth.userId, expectedReturnDate.value || undefined)

    if (message.includes('sucesso')) {
      toast.success(`${props.car.model} alugado com sucesso!`)
      emit('rented')
      open.value = false
    } else if (message.includes('login novamente')) {
      await auth.logout()
      toast.warning('Sua sessão de aluguel expirou. Entre novamente.')
      router.push({ name: 'login', query: { redirect: '/' } })
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
      <div class="rounded-box bg-base-200 p-4">
        <p class="font-semibold">{{ car.model }} · {{ car.year }}</p>
        <p class="text-sm text-base-content/70">{{ car.color }} · Placa {{ car.plate }}</p>
      </div>
      <label class="floating-label">
        <span>Devolução prevista (opcional)</span>
        <input v-model="expectedReturnDate" type="date" class="input w-full" :min="todayIso()" />
      </label>
    </div>

    <template #actions>
      <button class="btn btn-ghost" :disabled="loading" @click="open = false">Cancelar</button>
      <button class="btn btn-primary" :disabled="loading" @click="confirm">
        <span v-if="loading" class="loading loading-spinner loading-sm"></span>
        Alugar
      </button>
    </template>
  </BaseModal>
</template>
