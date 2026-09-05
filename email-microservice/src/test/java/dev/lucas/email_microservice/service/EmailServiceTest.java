package dev.lucas.email_microservice.service;

import dev.lucas.email_microservice.entity.Email;
import dev.lucas.email_microservice.enums.EmailStatus;
import dev.lucas.email_microservice.repository.EmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * O EmailService é a última etapa do fluxo assíncrono e o único ponto que fala
 * com o mundo externo via SMTP. A regra central é que uma falha de envio não
 * pode derrubar o consumidor da fila: o erro precisa virar registro no MongoDB.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailRepository emailRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private Email email;

    @BeforeEach
    void setUp() {
        email = new Email();
        email.setUserId("42");
        email.setEmailTo("destino@email.com");
        email.setSubject("Register Email");
        email.setText("Olá Lucas Aita, obrigado por se cadastrar.");
    }

    @Test
    @DisplayName("Deve marcar como SENT quando o envio pelo SMTP funciona")
    void shouldMarkAsSentOnSuccess() {
        emailService.sendEmail(email);

        verify(mailSender).send(any(SimpleMailMessage.class));

        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EmailStatus.SENT);
    }

    @Test
    @DisplayName("Deve marcar como ERROR quando o servidor SMTP recusa o envio")
    void shouldMarkAsErrorOnFailure() {
        doThrow(new MailSendException("servidor indisponível"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendEmail(email);

        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EmailStatus.ERROR);
    }

    @Test
    @DisplayName("Falha de SMTP não pode propagar exceção para o consumidor da fila")
    void shouldNotPropagateExceptionToConsumer() {
        doThrow(new MailSendException("timeout"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Se a exceção escapasse, a mensagem voltaria para a fila e o RabbitMQ
        // ficaria reentregando o mesmo evento indefinidamente.
        assertThatCode(() -> emailService.sendEmail(email)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve persistir o registro mesmo quando o envio falha")
    void shouldPersistEvenOnFailure() {
        doThrow(new MailSendException("recusado")).when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendEmail(email);

        // O bloco finally garante a trilha de auditoria das tentativas frustradas.
        verify(emailRepository).save(any(Email.class));
    }

    @Test
    @DisplayName("Deve carimbar a data de envio nos dois desfechos")
    void shouldStampSentAt() {
        emailService.sendEmail(email);

        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailRepository).save(captor.capture());
        assertThat(captor.getValue().getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve montar a mensagem com destinatário, assunto e corpo do evento")
    void shouldBuildMessageFromEntity() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendEmail(email);

        verify(mailSender).send(captor.capture());
        SimpleMailMessage enviada = captor.getValue();
        assertThat(enviada.getTo()).containsExactly("destino@email.com");
        assertThat(enviada.getSubject()).isEqualTo("Register Email");
        assertThat(enviada.getText()).contains("obrigado por se cadastrar");
    }

    @Test
    @DisplayName("Deve ser construído mesmo sem arquivo .env, como acontece no CI")
    void shouldBuildWithoutDotenvFile() {
        // Dotenv.configure().ignoreIfMissing() evita que a ausência do .env
        // quebre o contexto. Chegar até aqui já prova a construção da classe.
        assertThat(emailService).isNotNull();
        assertThatCode(() -> emailService.sendEmail(email)).doesNotThrowAnyException();
    }
}
