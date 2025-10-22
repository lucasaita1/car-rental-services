package dev.lucas.user_microservice.producer;

import dev.lucas.user_microservice.dtos.EmailDto;
import dev.lucas.user_microservice.entity.UserModel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProducer {

    private final RabbitTemplate rabbitTemplate;
    private final String routingKey = "register_email";


    public void sendRegisterEmail(UserModel user) {
      var email = new EmailDto();
      email.setUserId(user.getId());
      email.setEmailTo(user.getEmail());
      email.setSubject("Register Email");
      email.setText("Olá " + user.getUsername() + ", obrigado por se cadastrar em nosso sistema. Esta é uma mensagem automática de boas-vindas.");

      rabbitTemplate.convertAndSend("", routingKey, email);
  }
}
