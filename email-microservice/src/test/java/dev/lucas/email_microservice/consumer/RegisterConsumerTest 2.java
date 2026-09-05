package dev.lucas.email_microservice.consumer;

import dev.lucas.email_microservice.dto.EmailDto;
import dev.lucas.email_microservice.entity.Email;
import dev.lucas.email_microservice.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * O RegisterConsumer fecha a integração assíncrona: recebe o evento publicado
 * pelo user-microservice e o converte na entidade persistida. A conversão usa
 * BeanUtils.copyProperties entre um record e um bean, o que depende de os nomes
 * dos campos casarem exatamente nos dois lados dos serviços.
 */
@ExtendWith(MockitoExtension.class)
class RegisterConsumerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RegisterConsumer registerConsumer;

    private EmailDto evento() {
        return new EmailDto("42", "destino@email.com", "Register Email", "Olá, obrigado por se cadastrar.");
    }

    @Test
    @DisplayName("Deve repassar a mensagem da fila para o serviço de envio")
    void shouldDelegateToEmailService() {
        registerConsumer.listenEmailQueue(evento());

        verify(emailService).sendEmail(org.mockito.ArgumentMatchers.any(Email.class));
    }

    @Test
    @DisplayName("Deve copiar todos os campos do evento para a entidade de e-mail")
    void shouldMapEveryFieldFromEvent() {
        registerConsumer.listenEmailQueue(evento());

        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailService).sendEmail(captor.capture());

        Email enviado = captor.getValue();
        // Se qualquer nome de campo divergir entre o DTO do produtor e o do consumidor,
        // o campo chega nulo aqui e o e-mail falha silenciosamente em produção.
        assertThat(enviado.getUserId()).isEqualTo("42");
        assertThat(enviado.getEmailTo()).isEqualTo("destino@email.com");
        assertThat(enviado.getSubject()).isEqualTo("Register Email");
        assertThat(enviado.getText()).isEqualTo("Olá, obrigado por se cadastrar.");
    }

    @Test
    @DisplayName("Não deve definir status nem data de envio antes do envio")
    void shouldLeaveDeliveryMetadataToTheService() {
        registerConsumer.listenEmailQueue(evento());

        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailService).sendEmail(captor.capture());

        // Quem carimba SENT ou ERROR é o EmailService, depois de tentar o SMTP.
        assertThat(captor.getValue().getStatus()).isNull();
        assertThat(captor.getValue().getSentAt()).isNull();
        assertThat(captor.getValue().getId()).isNull();
    }

    @Test
    @DisplayName("Deve tolerar evento com campos opcionais nulos sem estourar")
    void shouldHandleNullFields() {
        registerConsumer.listenEmailQueue(new EmailDto(null, "destino@email.com", null, null));

        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailService).sendEmail(captor.capture());

        assertThat(captor.getValue().getEmailTo()).isEqualTo("destino@email.com");
        assertThat(captor.getValue().getUserId()).isNull();
    }
}
