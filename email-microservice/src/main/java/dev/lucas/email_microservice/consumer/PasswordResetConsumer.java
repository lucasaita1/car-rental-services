package dev.lucas.email_microservice.consumer;

import dev.lucas.email_microservice.Config.RabbitConfig;
import dev.lucas.email_microservice.dto.EmailDto;
import dev.lucas.email_microservice.entity.Email;
import dev.lucas.email_microservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordResetConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitConfig.PASSWORD_RESET_QUEUE)
    public void listenPasswordResetQueue(@Payload EmailDto message) {
        var email = new Email();
        BeanUtils.copyProperties(message, email);
        emailService.sendEmail(email);
    }
}
