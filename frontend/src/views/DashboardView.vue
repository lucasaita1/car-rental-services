<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import CarSearchForm from '@/components/CarSearchForm.vue'
import ActiveRentalCard from '@/components/home/ActiveRentalCard.vue'
import StatTile from '@/components/home/StatTile.vue'
import { listCars } from '@/api/cars'
import { assetUrl, carApi } from '@/api/http'
import { activeRentals, overdueRentals, rentalsByUser } from '@/api/rentals'
import type { Car, Rental } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { firstName, formatCurrency, formatDate, greeting } from '@/utils/format'

const auth = useAuthStore()

const cars = ref<Car[]>([])
const myRentals = ref<Rental[]>([])
const fleetActive = ref<Rental[] | null>(null)
const fleetOverdue = ref<Rental[] | null>(null)
const loading = ref(true)
const failed = ref(false)

const isAvailable = (car: Car) =>
  car.status !== 'RENTED' && car.status !== 'MAINTENANCE' && !car.reserved

const carsById = computed(() => new Map(cars.value.map((car) => [car.id, car])))
const available = computed(() => cars.value.filter(isAvailable))
const featured = computed(() =>
  [...available.value].sort((a, b) => Number(!!b.photoUrl) - Number(!!a.photoUrl)).slice(0, 4),
)
const active = computed(() => myRentals.value.filter((r) => r.status === 'ACTIVE'))
const finishedCount = computed(() => myRentals.value.filter((r) => r.status === 'FINISHED').length)
const profileIncomplete = computed(() => !!auth.profile && (!auth.profile.cpf || !auth.profile.cnh))

const headline = computed(() => {
  if (loading.value) return 'Carregando o seu resumo…'
  const current = active.value[0]
  if (!current) return 'Pronto para a próxima viagem? Encontre o carro ideal abaixo.'
  if (current.overdue) return `A devolução do ${current.carModel} está atrasada.`
  if (current.expectedReturnDate) {
    return `Você está com o ${current.carModel}. Devolução prevista para ${formatDate(current.expectedReturnDate)}.`
  }
  return `Você está com o ${current.carModel}. Boa viagem!`
})

onMounted(async () => {
  if (!auth.profile) auth.loadProfile()
  const userId = auth.userId
  const [carsResult, mineResult, activeResult, overdueResult] = await Promise.allSettled([
    listCars(),
    userId !== null ? rentalsByUser(userId) : Promise.resolve([]),
    auth.isAdmin ? activeRentals() : Promise.resolve(null),
    auth.isAdmin ? overdueRentals() : Promise.resolve(null),
  ])
  if (carsResult.status === 'fulfilled') cars.value = carsResult.value
  if (mineResult.status === 'fulfilled') myRentals.value = mineResult.value
  if (activeResult.status === 'fulfilled') fleetActive.value = activeResult.value
  if (overdueResult.status === 'fulfilled') fleetOverdue.value = overdueResult.value
  failed.value = carsResult.status === 'rejected' && mineResult.status === 'rejected'
  loading.value = false
})
</script>

<template>
  <div>
    <section class="bg-neutral text-neutral-content">
      <div class="mx-auto max-w-6xl space-y-8 px-4 pt-12 pb-10 lg:pt-16">
        <div class="flex flex-wrap items-end justify-between gap-6">
          <div class="space-y-3">
            <p class="text-neutral-content/70 font-medium">{{ greeting() }},</p>
            <h1
              class="flex flex-wrap items-center gap-3 text-4xl font-extrabold tracking-tight md:text-5xl"
            >
              {{ firstName(auth.userName) }}
              <span v-if="auth.isAdmin" class="badge badge-accent badge-soft font-semibold"
                >Administrador</span
              >
            </h1>
            <p class="text-neutral-content/75 max-w-2xl text-lg">{{ headline }}</p>
          </div>
          <div v-if="!loading" class="flex gap-3">
            <div class="rounded-box bg-neutral-content/5 px-5 py-3">
              <p class="text-2xl font-extrabold">{{ active.length }}</p>
              <p class="text-neutral-content/60 text-sm">
                {{ active.length === 1 ? 'locação ativa' : 'locações ativas' }}
              </p>
            </div>
            <div class="rounded-box bg-neutral-content/5 px-5 py-3">
              <p class="text-2xl font-extrabold">{{ finishedCount }}</p>
              <p class="text-neutral-content/60 text-sm">
                {{ finishedCount === 1 ? 'viagem concluída' : 'viagens concluídas' }}
              </p>
            </div>
          </div>
        </div>
        <CarSearchForm inline />
      </div>
      <div class="road-line h-1 opacity-40"></div>
    </section>

    <div class="mx-auto max-w-6xl space-y-12 px-4 py-12">
      <div v-if="failed" role="alert" class="alert alert-error alert-soft">
        <AppIcon name="alert" class="size-5" />
        Não foi possível carregar seus dados agora. Tente novamente em instantes.
      </div>

      <section v-if="auth.isAdmin" class="space-y-5">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <p class="text-primary text-sm font-semibold tracking-wide uppercase">Administração</p>
            <h2 class="text-2xl font-extrabold tracking-tight">Painel da operação</h2>
          </div>
          <RouterLink :to="{ name: 'admin-users' }" class="btn btn-ghost btn-sm">
            <AppIcon name="users" class="size-4" /> Gerenciar usuários
          </RouterLink>
        </div>
        <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatTile
            icon="grid"
            label="Carros na frota"
            :value="loading ? null : cars.length"
            :to="{ name: 'admin-cars' }"
          />
          <StatTile
            icon="key"
            label="Disponíveis agora"
            tone="success"
            :value="loading ? null : available.length"
            :to="{ name: 'catalog' }"
          />
          <StatTile
            icon="calendar"
            label="Locações ativas"
            tone="warning"
            :value="loading ? null : (fleetActive?.length ?? 0)"
            :to="{ name: 'admin-rentals' }"
          />
          <StatTile
            icon="alert"
            label="Atrasadas"
            :tone="fleetOverdue?.length ? 'error' : 'success'"
            :value="loading ? null : (fleetOverdue?.length ?? 0)"
            :to="{ name: 'admin-rentals' }"
          />
        </div>
      </section>

      <div class="grid gap-8 lg:grid-cols-3">
        <section class="space-y-5 lg:col-span-2">
          <h2 class="text-2xl font-extrabold tracking-tight">
            {{ active.length > 1 ? 'Suas locações' : 'Sua locação' }}
          </h2>

          <div v-if="loading" class="skeleton h-52 w-full"></div>

          <template v-else-if="active.length">
            <ActiveRentalCard
              v-for="rental in active"
              :key="rental.id"
              :rental="rental"
              :car="carsById.get(rental.carId)"
            />
          </template>

          <div
            v-else
            class="rounded-box border-base-300 bg-base-100 flex flex-col items-center gap-4 border border-dashed px-6 py-12 text-center"
          >
            <span class="bg-primary/10 text-primary grid size-14 place-items-center rounded-2xl">
              <AppIcon name="key" class="size-7" />
            </span>
            <div class="space-y-1">
              <p class="text-lg font-bold">Nenhuma locação em andamento</p>
              <p class="text-base-content/70">
                Escolha um carro e ele fica reservado para você enquanto confirma.
              </p>
            </div>
            <RouterLink :to="{ name: 'catalog' }" class="btn btn-primary">
              Escolher um carro <AppIcon name="arrowRight" class="size-4" />
            </RouterLink>
          </div>
        </section>

        <aside class="space-y-5">
          <h2 class="text-2xl font-extrabold tracking-tight">Atalhos</h2>
          <ul
            class="menu rounded-box bg-base-100 border-base-300 w-full gap-1 border p-2 shadow-sm"
          >
            <li>
              <RouterLink :to="{ name: 'my-rentals' }" class="py-3">
                <AppIcon name="list" class="size-5" />
                <span class="flex-1">Minhas locações</span>
                <span class="badge badge-ghost badge-sm">{{ myRentals.length }}</span>
              </RouterLink>
            </li>
            <li>
              <RouterLink :to="{ name: 'profile' }" class="py-3">
                <AppIcon name="user" class="size-5" />
                <span class="flex-1">Meu perfil</span>
                <span v-if="profileIncomplete" class="badge badge-warning badge-soft badge-sm"
                  >Completar</span
                >
              </RouterLink>
            </li>
            <li>
              <RouterLink :to="{ name: 'catalog' }" class="py-3">
                <AppIcon name="search" class="size-5" />
                <span class="flex-1">Frota completa</span>
              </RouterLink>
            </li>
          </ul>
          <div
            v-if="profileIncomplete"
            role="alert"
            class="alert alert-warning alert-soft items-start"
          >
            <AppIcon name="alert" class="size-5" />
            <span> Cadastre seu CPF e CNH no perfil para agilizar a retirada do carro. </span>
          </div>
        </aside>
      </div>

      <section class="space-y-5">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 class="text-2xl font-extrabold tracking-tight">Disponíveis agora</h2>
            <p class="text-base-content/70">
              {{
                loading ? 'Consultando a frota…' : `${available.length} carros prontos para sair.`
              }}
            </p>
          </div>
          <RouterLink :to="{ name: 'catalog' }" class="btn btn-outline btn-primary btn-sm">
            Ver todos <AppIcon name="arrowRight" class="size-4" />
          </RouterLink>
        </div>

        <div v-if="loading" class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div v-for="i in 4" :key="i" class="skeleton h-56 w-full"></div>
        </div>
        <p
          v-else-if="!featured.length"
          class="rounded-box border-base-300 bg-base-100 text-base-content/70 border p-8 text-center"
        >
          Todos os carros estão em uso no momento. Volte em breve.
        </p>
        <div v-else class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <RouterLink
            v-for="car in featured"
            :key="car.id"
            :to="{ name: 'catalog', query: { busca: car.model } }"
            class="group card bg-base-100 border-base-300 overflow-hidden border shadow-sm transition hover:-translate-y-1 hover:shadow-lg"
          >
            <figure class="bg-base-200 aspect-[16/10] overflow-hidden">
              <img
                v-if="car.photoUrl"
                :src="assetUrl(carApi, car.photoUrl) ?? ''"
                :alt="car.model"
                class="size-full object-cover transition duration-500 group-hover:scale-105"
                loading="lazy"
              />
              <AppIcon v-else name="key" class="text-base-content/20 size-10" />
            </figure>
            <div class="card-body gap-1 p-4">
              <h3 class="font-bold">{{ car.model }}</h3>
              <p class="text-base-content/60 text-sm">{{ car.year }} · {{ car.color }}</p>
              <div class="mt-2 flex items-end justify-between gap-2">
                <p class="text-primary flex items-center gap-1 text-sm font-semibold">
                  Reservar
                  <AppIcon name="arrowRight" class="size-4 transition group-hover:translate-x-1" />
                </p>
                <p v-if="car.dailyRate !== null" class="leading-tight">
                  <span class="font-extrabold">{{ formatCurrency(car.dailyRate) }}</span>
                  <span class="text-base-content/60 text-xs">/dia</span>
                </p>
              </div>
            </div>
          </RouterLink>
        </div>
      </section>
    </div>
  </div>
</template>
