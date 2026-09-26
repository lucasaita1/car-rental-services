<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
import type { IconName } from '@/utils/icons'

const benefits: { icon: IconName; title: string; text: string }[] = [
  {
    icon: 'clock',
    title: 'Reserva garantida no checkout',
    text: 'Ninguém aluga o seu carro enquanto você finaliza. São 10 minutos exclusivos, com contagem na tela.',
  },
  {
    icon: 'bolt',
    title: 'Disponibilidade em tempo real',
    text: 'O catálogo mostra na hora o que está livre, alugado, em manutenção ou reservado.',
  },
  {
    icon: 'calendar',
    title: 'Devolução flexível',
    text: 'Informe uma data prevista se quiser, ou deixe em aberto. Você devolve pelo próprio painel.',
  },
  {
    icon: 'wrench',
    title: 'Frota revisada',
    text: 'Carro em manutenção nunca é oferecido. Só entra no catálogo o que está pronto para rodar.',
  },
  {
    icon: 'shield',
    title: 'Conta protegida',
    text: 'Senha criptografada, sessão encerrada de verdade ao sair e recuperação de senha por e-mail.',
  },
  {
    icon: 'list',
    title: 'Histórico completo',
    text: 'Todas as suas locações ficam registradas, com datas de retirada e devolução.',
  },
]
</script>

<template>
  <section class="mx-auto max-w-6xl px-4 py-20">
    <div class="grid gap-12 lg:grid-cols-[1fr_2fr]">
      <div class="space-y-4 lg:sticky lg:top-24 lg:self-start">
        <p class="text-primary font-semibold tracking-wide uppercase">Por que alugar com a gente</p>
        <h2 class="text-3xl font-extrabold tracking-tight md:text-4xl">
          Pensado para você dirigir sem dor de cabeça
        </h2>
        <p class="text-base-content/70">
          Cada detalhe do aluguel foi desenhado para evitar surpresa: do carro que some no meio da
          compra à senha esquecida.
        </p>
      </div>
      <div class="grid gap-5 sm:grid-cols-2">
        <div
          v-for="b in benefits"
          :key="b.title"
          class="rounded-box border-base-300 bg-base-100 hover:border-primary/40 border p-6 transition hover:shadow-lg"
        >
          <div
            class="bg-primary/10 text-primary mb-4 flex size-11 items-center justify-center rounded-xl"
          >
            <AppIcon :name="b.icon" class="size-6" />
          </div>
          <h3 class="font-bold">{{ b.title }}</h3>
          <p class="text-base-content/70 mt-1 text-sm leading-relaxed">{{ b.text }}</p>
        </div>
      </div>
    </div>
  </section>
</template>
