<script setup lang="ts">
import logo from '@/assets/brand/logo-volante.png'
import { brand } from '@/brand'

withDefaults(defineProps<{ size?: number; showName?: boolean }>(), { size: 36, showName: true })
</script>

<template>
  <span class="inline-flex items-center gap-2">
    <img :src="logo" :width="size" :height="size" :alt="showName ? '' : brand.name" />
    <span v-if="showName" class="text-xl font-extrabold tracking-tight">{{ brand.name }}</span>
  </span>
</template>
