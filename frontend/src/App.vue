<script setup lang="ts">
import { useRoute } from 'vue-router'
import AppFooter from '@/components/AppFooter.vue'
import AppNavbar from '@/components/AppNavbar.vue'
import ToastHost from '@/components/ToastHost.vue'

const route = useRoute()
</script>

<template>
  <div class="bg-base-200 flex min-h-screen flex-col">
    <AppNavbar v-if="!route.meta.bare" />
    <main :class="route.meta.fullWidth ? 'flex-1' : 'mx-auto w-full max-w-6xl flex-1 px-4 py-8'">
      <RouterView />
    </main>
    <AppFooter v-if="!route.meta.bare" />
    <ToastHost />
  </div>
</template>
