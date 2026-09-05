package dev.lucas.user_microservice.producer;

import dev.lucas.user_microservice.dtos.EmailDto;
import dev.lucas.user_microservice.entity.UserModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * O UserProducer é a ponta de saída da integração assíncrona. A routing key
 * "register_email" precisa bater com o nome da fila declarada no
 * email-microservice: se divergir, a mensagem some sem erro em nenhum dos lados.
 */
@ExtendWith(MockitoExtension.class)
class UserProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UserProducer userProducer;

    private UserModel usuario() {
        UserModel user = new UserModel();
        user.setId(1L);
        user.setName("Lucas Aita");
        user.setEmail("lucas@email.com");
        return user;
    }

    @Test
    @DisplayName("Deve publicar na routing key register_email pela exchange padrão")
    void shouldPublishToExpectedQueue() {
        userProducer.sendRegisterEmail(usuario());

        // Exchange vazia é o default do RabbitMQ, que roteia direto para a fila
        // cujo nome é igual à routing key.
        verify(rabbitTemplate).convertAndSend(eq(""), eq("register_email"), any(EmailDto.class));
    }

    @Test
    @DisplayName("Deve enviar o evento com destinatário, assunto e corpo preenchidos")
    void shouldBuildEventPayload() {
        userProducer.sendRegisterEmail(usuario());

        ArgumentCaptor<EmailDto> captor = ArgumentCaptor.forClass(EmailDto.class);
        verify(rabbitTemplate).convertAndSend(eq(""), eq("register_email"), captor.capture());

        EmailDto evento = captor.getValue();
        assertThat(evento.getEmailTo()).isEqualTo("lucas@email.com");
        assertThat(evento.getSubject()).isEqualTo("Register Email");
        assertThat(evento.getText()).contains("obrigado por se cadastrar");
        assertThat(evento.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("O corpo do e-mail deve trazer o nome do cliente")
    void shouldGreetUserByName() {
        userProducer.sendRegisterEmail(usuario());

        ArgumentCaptor<EmailDto> captor = ArgumentCaptor.forClass(EmailDto.class);
        verify(rabbitTemplate).convertAndSend(eq(""), eq("register_email"), captor.capture());

        // Usar getUsername() aqui produziria "Olá ,", porque UserModel sobrescreve
        // esse método do UserDetails para devolver string vazia.
        assertThat(captor.getValue().getText())
                .startsWith("Olá Lucas Aita,")
                .doesNotContain("Olá ,");
    }
}
