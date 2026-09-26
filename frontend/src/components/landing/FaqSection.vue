<script setup lang="ts">
const faqs = [
  {
    q: 'Preciso de CNH para alugar?',
    a: 'Sim. No cadastro pedimos seu CPF e o número da sua CNH. Você pode conferir e atualizar os dados em "Meu perfil".',
  },
  {
    q: 'Como funciona a reserva de 10 minutos?',
    a: 'Quando você clica em alugar, o carro fica reservado só para você por 10 minutos. Durante esse tempo ele aparece como "Reservado" para os outros clientes. Se você cancelar ou o tempo acabar, ele volta a ficar disponível.',
  },
  {
    q: 'Preciso informar a data de devolução?',
    a: 'Não é obrigatório. Você pode informar uma data prevista ou deixar em aberto e devolver quando precisar, pelo painel "Minhas locações".',
  },
  {
    q: 'Posso alugar mais de um carro ao mesmo tempo?',
    a: 'Você pode ter várias locações, mas reserva apenas um carro por vez durante o checkout. Ao reservar outro, a reserva anterior é liberada.',
  },
  {
    q: 'Esqueci minha senha. E agora?',
    a: 'Na tela de login, clique em "Esqueci minha senha". Enviamos um link para o seu e-mail, válido por 30 minutos e de uso único.',
  },
  {
    q: 'Meus dados estão seguros?',
    a: 'Sua senha é guardada criptografada e nunca em texto. Ao sair da conta, a sessão é encerrada no servidor, e trocar a senha desconecta todos os dispositivos.',
  },
]
</script>

<template>
  <section id="duvidas" class="mx-auto max-w-3xl scroll-mt-20 px-4 py-20">
    <div class="mb-10 space-y-3 text-center">
      <p class="text-primary font-semibold tracking-wide uppercase">Dúvidas frequentes</p>
      <h2 class="text-3xl font-extrabold tracking-tight md:text-4xl">
        Tudo o que você precisa saber
      </h2>
    </div>
    <div class="join join-vertical bg-base-100 w-full shadow-sm">
      <div
        v-for="(f, i) in faqs"
        :key="f.q"
        class="collapse-arrow join-item border-base-300 collapse border"
      >
        <input type="radio" name="faq" :checked="i === 0" :aria-label="f.q" />
        <div class="collapse-title font-semibold">{{ f.q }}</div>
        <div class="collapse-content text-base-content/75">{{ f.a }}</div>
      </div>
    </div>
  </section>
</template>
