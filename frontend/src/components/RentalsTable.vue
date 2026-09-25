<script setup lang="ts">
import type { Rental } from '@/api/types'
import { formatDate } from '@/utils/format'

defineProps<{ rentals: Rental[]; showCustomer?: boolean }>()
const emit = defineEmits<{ return: [rental: Rental] }>()
</script>

<template>
  <div class="overflow-x-auto rounded-box border border-base-300 bg-base-100">
    <table class="table">
      <thead>
        <tr>
          <th v-if="showCustomer">Cliente</th>
          <th>Carro</th>
          <th>Retirada</th>
          <th>Devolução prevista</th>
          <th>Devolvido em</th>
          <th>Situação</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in rentals" :key="r.id">
          <td v-if="showCustomer">
            <div class="font-medium">{{ r.userName }}</div>
            <div class="text-sm text-base-content/70">{{ r.userEmail }}</div>
          </td>
          <td>
            <div class="font-medium">{{ r.carModel }}</div>
            <div class="font-mono text-sm text-base-content/70">{{ r.carPlate }}</div>
          </td>
          <td>{{ formatDate(r.rentalDate) }}</td>
          <td>{{ formatDate(r.expectedReturnDate) }}</td>
          <td>{{ formatDate(r.returnDate) }}</td>
          <td>
            <span v-if="r.overdue" class="badge badge-error badge-soft">Atrasada</span>
            <span v-else-if="r.status === 'ACTIVE'" class="badge badge-info badge-soft"
              >Em andamento</span
            >
            <span v-else class="badge badge-ghost">Encerrada</span>
          </td>
          <td class="text-right">
            <button
              v-if="r.status === 'ACTIVE'"
              class="btn btn-sm btn-outline"
              @click="emit('return', r)"
            >
              Devolver
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
