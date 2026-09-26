<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import BaseModal from './BaseModal.vue'
import CarDetailsEditor from './CarDetailsEditor.vue'
import PhotoPicker from './PhotoPicker.vue'
import { createCar, removeCarPhoto, updateCar, uploadCarPhoto } from '@/api/cars'
import { assetUrl, carApi, errorMessage } from '@/api/http'
import type { Car, CarDetail, CarPayload, CarStatus } from '@/api/types'
import { useToastStore } from '@/stores/toast'

const props = defineProps<{ car: Car | null }>()
const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{ saved: [car: Car] }>()

const toast = useToastStore()
const maxYear = new Date().getFullYear() + 1
const platePattern = /^[A-Z]{3}-?\d[A-Z0-9]\d{2}$/

const form = reactive<CarPayload>({
  model: '',
  color: '',
  plate: '',
  year: maxYear - 1,
  dailyRate: null,
  details: [],
  status: 'AVAILABLE',
})
const saving = ref(false)
const error = ref('')
const photo = ref<File | null>(null)
const removePhoto = ref(false)
const pickerKey = ref(0)

const isEdit = computed(() => props.car !== null)
const isRented = computed(() => props.car?.status === 'RENTED')

watch(open, (value) => {
  if (!value) return
  error.value = ''
  photo.value = null
  removePhoto.value = false
  pickerKey.value++
  Object.assign(form, {
    model: props.car?.model ?? '',
    color: props.car?.color ?? '',
    plate: props.car?.plate ?? '',
    year: props.car?.year ?? maxYear - 1,
    dailyRate: props.car?.dailyRate ?? null,
    details: (props.car?.details ?? []).map((d) => ({ ...d })),
    status: props.car?.status ?? 'AVAILABLE',
  })
})

function validate(): string {
  if (!form.model.trim() || !form.color.trim()) return 'Preencha modelo e cor.'
  if (!platePattern.test(form.plate)) return 'Placa inválida. Use ABC-1234 ou ABC1D23.'
  if (form.year < 1950 || form.year > maxYear) return `Ano deve estar entre 1950 e ${maxYear}.`
  if (form.dailyRate === null || !(form.dailyRate >= 1)) return 'Informe a diária (mínimo R$ 1,00).'
  if (form.details.some((d) => !!d.label.trim() !== !!d.value.trim())) {
    return 'Preencha nome e valor de cada detalhe, ou remova a linha.'
  }
  return ''
}

async function submit() {
  form.plate = form.plate.trim().toUpperCase()
  error.value = validate()
  if (error.value) return

  const details: CarDetail[] = form.details
    .map((d) => ({ label: d.label.trim(), value: d.value.trim() }))
    .filter((d) => d.label && d.value)
  const payload: CarPayload = {
    ...form,
    details,
    status: isRented.value ? undefined : (form.status as CarStatus),
  }
  saving.value = true
  try {
    let saved = props.car ? await updateCar(props.car.id, payload) : await createCar(payload)
    if (photo.value) {
      saved = await uploadCarPhoto(saved.id, photo.value)
    } else if (removePhoto.value && props.car?.photoUrl) {
      saved = await removeCarPhoto(saved.id)
    }
    toast.success(isEdit.value ? 'Carro atualizado.' : 'Carro cadastrado.')
    emit('saved', saved)
    open.value = false
  } catch (e) {
    error.value = errorMessage(e, 'Não foi possível salvar o carro.')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal v-model:open="open" wide :title="isEdit ? 'Editar carro' : 'Novo carro'">
    <form id="car-form" class="grid grid-cols-2 gap-3" @submit.prevent="submit">
      <div class="col-span-2">
        <PhotoPicker
          :key="pickerKey"
          :current-url="removePhoto ? null : assetUrl(carApi, car?.photoUrl)"
          @select="((photo = $event), (removePhoto = false))"
          @remove="((photo = null), (removePhoto = true))"
        />
      </div>
      <label class="floating-label col-span-2">
        <span>Modelo</span>
        <input v-model="form.model" class="input w-full" placeholder="Modelo" required />
      </label>
      <label class="floating-label">
        <span>Cor</span>
        <input v-model="form.color" class="input w-full" placeholder="Cor" required />
      </label>
      <label class="floating-label">
        <span>Ano</span>
        <input
          v-model.number="form.year"
          type="number"
          class="input w-full"
          placeholder="Ano"
          :min="1950"
          :max="maxYear"
          required
        />
      </label>
      <label class="floating-label">
        <span>Placa</span>
        <input
          v-model="form.plate"
          class="input w-full uppercase"
          placeholder="Placa"
          maxlength="8"
          required
        />
      </label>
      <label class="floating-label">
        <span>Status</span>
        <select v-model="form.status" class="select w-full" :disabled="isRented">
          <option value="AVAILABLE">Disponível</option>
          <option value="MAINTENANCE">Manutenção</option>
          <option v-if="isRented" value="RENTED">Alugado</option>
        </select>
      </label>
      <label class="input col-span-2 w-full">
        <span class="text-base-content/60">R$</span>
        <input
          v-model.number="form.dailyRate"
          type="number"
          step="0.01"
          min="1"
          class="grow"
          placeholder="Valor da diária"
          aria-label="Valor da diária"
          required
        />
        <span class="text-base-content/60 text-sm">por dia</span>
      </label>
      <div class="col-span-2">
        <CarDetailsEditor v-model="form.details" />
      </div>
      <p v-if="isRented" class="col-span-2 text-sm text-base-content/70">
        Carro com locação ativa: o status só muda após a devolução.
      </p>
      <div v-if="error" role="alert" class="alert alert-error alert-soft col-span-2">
        {{ error }}
      </div>
    </form>

    <template #actions>
      <button class="btn btn-ghost" :disabled="saving" @click="open = false">Cancelar</button>
      <button type="submit" form="car-form" class="btn btn-primary" :disabled="saving">
        <span v-if="saving" class="loading loading-spinner loading-sm"></span>
        {{ isEdit ? 'Salvar alterações' : 'Cadastrar' }}
      </button>
    </template>
  </BaseModal>
</template>
