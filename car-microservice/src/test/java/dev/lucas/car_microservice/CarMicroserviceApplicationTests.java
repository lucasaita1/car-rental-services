package dev.lucas.car_microservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Requer MySQL/Redis externos; coberto pelos testes unitários com Mockito.")
@SpringBootTest
class CarMicroserviceApplicationTests {

	@Test
	void contextLoads() {
	}

}