<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import LandingHero from '@/components/landing/LandingHero.vue'
import FleetShowcase from '@/components/landing/FleetShowcase.vue'
import HowItWorks from '@/components/landing/HowItWorks.vue'
import BenefitsSection from '@/components/landing/BenefitsSection.vue'
import UseCases from '@/components/landing/UseCases.vue'
import FaqSection from '@/components/landing/FaqSection.vue'
import CtaBanner from '@/components/landing/CtaBanner.vue'
import { listCars } from '@/api/cars'
import type { Car } from '@/api/types'

const cars = ref<Car[]>([])
const loading = ref(true)
const failed = ref(false)

const availableCount = computed(() =>
  failed.value || loading.value
    ? null
    : cars.value.filter((c) => c.status !== 'RENTED' && c.status !== 'MAINTENANCE' && !c.reserved)
        .length,
)

onMounted(async () => {
  try {
    cars.value = await listCars()
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <LandingHero :available-count="availableCount" />
    <FleetShowcase :cars="cars" :loading="loading" :failed="failed" />
    <HowItWorks />
    <BenefitsSection />
    <UseCases />
    <FaqSection />
    <CtaBanner />
  </div>
</template>
