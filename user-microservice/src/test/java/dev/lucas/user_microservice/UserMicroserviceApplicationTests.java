package dev.lucas.user_microservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Requer recursos externos (MySQL/RabbitMQ); coberto pelos testes unitarios.")
@SpringBootTest
class UserMicroserviceApplicationTests {

	@Test
	void contextLoads() {
	}

}