package dev.lucas.email_microservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Requer recursos externos (RabbitMQ/MongoDB); coberto pelos testes unitarios.")
@SpringBootTest
class EmailMicroserviceApplicationTests {

	@Test
	void contextLoads() {
	}

}