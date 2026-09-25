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

@ExtendWith(MockitoExtension.class)
class PasswordResetConsumerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetConsumer consumer;

    @Test
    @DisplayName("Envia o e-mail de redefinição com o link recebido na fila")
    void sendsResetEmail() {
        String link = "http://localhost:5173/redefinir-senha?token=abc";

        consumer.listenPasswordResetQueue(new EmailDto("7", "ana@x.com", "Redefinição de senha", "Use o link: " + link));

        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailService).sendEmail(captor.capture());
        assertThat(captor.getValue().getEmailTo()).isEqualTo("ana@x.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("Redefinição de senha");
        assertThat(captor.getValue().getText()).contains(link);
    }
}
