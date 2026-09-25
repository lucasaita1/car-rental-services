<script setup lang="ts">
import AppNavbar from '@/components/AppNavbar.vue'
import ToastHost from '@/components/ToastHost.vue'
</script>

<template>
  <div class="min-h-screen bg-base-200">
    <AppNavbar />
    <main class="mx-auto max-w-6xl px-4 py-8">
      <RouterView />
    </main>
    <ToastHost />
  </div>
</template>
