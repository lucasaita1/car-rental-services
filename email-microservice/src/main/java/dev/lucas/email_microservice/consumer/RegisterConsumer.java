package dev.lucas.email_microservice.consumer;

import dev.lucas.email_microservice.dto.EmailDto;
import dev.lucas.email_microservice.entity.Email;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class RegisterConsumer {

    @RabbitListener(queues = "register_email")
    public void listenEmailQueue(@Payload EmailDto message) {
        var emailModel = new Email();
        BeanUtils.copyProperties(message, emailModel);
        System.out.println(emailModel);
    }

    //test
//    @RabbitListener(queues = "register_email")
//    public void receiveMessage(String message) {
//        System.out.println("Mensagem recebida: " + message);
//    }
}
