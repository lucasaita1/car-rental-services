package dev.lucas.email_microservice.service;


import dev.lucas.email_microservice.entity.Email;
import dev.lucas.email_microservice.enums.EmailStatus;
import dev.lucas.email_microservice.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${EMAIL_FROM:${EMAIL_USERNAME:}}")
    private String emailFrom;


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
