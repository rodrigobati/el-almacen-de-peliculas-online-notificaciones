package unrn.notificaciones.adapter.in.rabbit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import unrn.notificaciones.event.CompraConfirmadaEvent;

@Testcontainers
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class CompraConfirmadaRabbitListenerDlqIT {

    @Container
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.compras.events.exchange}")
    String comprasEventsExchange;

    @Value("${rabbitmq.compras.compra.confirmada.routing-key}")
    String compraConfirmadaRoutingKey;

    @Value("${rabbitmq.notificaciones.compra.confirmada.dlq}")
    String compraConfirmadaDlqQueue;

    @Test
    @DisplayName("eventoInvalido_vaADLQ")
    void eventoInvalido_vaADLQ() throws Exception {
        // Build event with wrong eventType
        CompraConfirmadaEvent.ItemCompraConfirmada item = new CompraConfirmadaEvent.ItemCompraConfirmada("Pelicula X",
                1, new BigDecimal("100.00"));
        CompraConfirmadaEvent.TotalCompraConfirmada total = new CompraConfirmadaEvent.TotalCompraConfirmada(
                new BigDecimal("100.00"), BigDecimal.ZERO, "");

        UUID compraId = UUID.randomUUID();
        CompraConfirmadaEvent.Data data = new CompraConfirmadaEvent.Data(
                compraId,
                "cliente@example.com",
                Instant.now(),
                List.of(item),
                total);

        // invalid eventType
        CompraConfirmadaEvent invalid = new CompraConfirmadaEvent(
                UUID.randomUUID(),
                "CompraConfirmadaX",
                CompraConfirmadaEvent.EVENT_VERSION,
                Instant.now(),
                "ventas-service",
                data);

        rabbitTemplate.convertAndSend(comprasEventsExchange, compraConfirmadaRoutingKey, invalid);

        // Await for message in DLQ
        long deadline = System.currentTimeMillis() + 10000L; // 10s
        while (System.currentTimeMillis() < deadline) {
            Message msg = rabbitTemplate.receive(compraConfirmadaDlqQueue);
            if (msg != null) {
                assertNotNull(msg);
                return;
            }
            Thread.sleep(200L);
        }

        fail("No se recibió el mensaje en la DLQ " + compraConfirmadaDlqQueue);
    }
}
