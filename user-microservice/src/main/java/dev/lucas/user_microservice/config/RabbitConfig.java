package dev.lucas.user_microservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lucas.user_microservice.producer.UserProducer;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public Queue registerEmailQueue() {
        return new Queue(UserProducer.REGISTER_QUEUE, true);
    }

    @Bean
    public Queue passwordResetEmailQueue() {
        return new Queue(UserProducer.PASSWORD_RESET_QUEUE, true);
    }
}
