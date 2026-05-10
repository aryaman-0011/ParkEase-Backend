package com.parkease.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"app.jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci1jaS1waXBlbGluZS1vbmx5",
	"spring.cloud.gateway.routes[0].id=test",
	"spring.cloud.gateway.routes[0].uri=http://localhost:8080",
	"spring.cloud.gateway.routes[0].predicates[0]=Path=/test/**"
})
class GatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
