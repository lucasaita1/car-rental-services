package dev.lucas.car_microservice;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CarMicroserviceApplication {

    public static void main(String[] args) {
        // Carrega as variáveis do .env antes do Spring inicializar
        Dotenv dotenv = Dotenv.load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(CarMicroserviceApplication.class, args);
    }
}