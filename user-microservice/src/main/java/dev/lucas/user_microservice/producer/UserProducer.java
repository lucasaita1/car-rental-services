package dev.lucas.user_microservice.producer;

import dev.lucas.user_microservice.dtos.EmailDto;
import dev.lucas.user_microservice.entity.UserModel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProducer {

    public static final String REGISTER_QUEUE = "register_email";
    public static final String PASSWORD_RESET_QUEUE = "password_reset_email";

    private final RabbitTemplate rabbitTemplate;

    public void sendRegisterEmail(UserModel user) {
      var email = new EmailDto();
      email.setUserId(user.getId());
      email.setEmailTo(user.getEmail());
      email.setSubject("Register Email");
      email.setText("Olá " + user.getName() + ", obrigado por se cadastrar em nosso sistema. Esta é uma mensagem automática de boas-vindas.");

      rabbitTemplate.convertAndSend("", REGISTER_QUEUE, email);
  }

    public void sendPasswordResetEmail(UserModel user, String resetLink) {
        var email = new EmailDto();
        email.setUserId(user.getId());
        email.setEmailTo(user.getEmail());
        email.setSubject("Redefinição de senha");
        email.setText("Olá " + user.getName() + ",\n\n"
                + "Recebemos um pedido para redefinir a sua senha. Use o link abaixo, válido por 30 minutos:\n\n"
                + resetLink + "\n\n"
                + "Se não foi você, ignore este e-mail. Sua senha continua a mesma.");

        rabbitTemplate.convertAndSend("", PASSWORD_RESET_QUEUE, email);
    }
}
