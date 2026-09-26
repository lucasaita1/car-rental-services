<script setup lang="ts">
import { defineAsyncComponent } from 'vue'
import { useAuthStore } from '@/stores/auth'

const DashboardView = defineAsyncComponent(() => import('./DashboardView.vue'))
const LandingView = defineAsyncComponent(() => import('./LandingView.vue'))

const auth = useAuthStore()
</script>

<template>
  <DashboardView v-if="auth.isAuthenticated" />
  <LandingView v-else />
</template>
