package dev.lucas.email_microservice;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRabbit
public class EmailMicroserviceApplication {

	public static void main(String[] args) {
        //carrega as variaveis de ambiente do .env
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry ->
                        System.setProperty(entry.getKey(), entry.getValue())
        );
		SpringApplication.run(EmailMicroserviceApplication.class, args);
	}

}
