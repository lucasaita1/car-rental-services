package dev.lucas.email_microservice.service;


import dev.lucas.email_microservice.entity.Email;
import dev.lucas.email_microservice.enums.EmailStatus;
import dev.lucas.email_microservice.repository.EmailRepository;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailRepository emailRepository;
    private final JavaMailSender mailSender;

    // ignoreIfMissing permite que a classe seja construída sem o arquivo .env,
    // como acontece no pipeline de CI, onde o .env não é versionado.
    private final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private final String emailFrom = dotenv.get("EMAIL_FROM");


    @Transactional
    public void sendEmail(Email email) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(email.getEmailTo());
            message.setSubject(email.getSubject());
            message.setText(email.getText());

            mailSender.send(message);

            email.setStatus(EmailStatus.SENT);
        }catch (Exception e){
            email.setStatus(EmailStatus.ERROR);
            System.out.println("erro ao enviar email" + e.getMessage());
        }finally {
            email.setSentAt(LocalDateTime.now());
            email.setFrom(emailFrom);
            emailRepository.save(email);
        }
    }

}
