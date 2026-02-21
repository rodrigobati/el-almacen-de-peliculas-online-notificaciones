package unrn.notificaciones.adapter.in.rabbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import unrn.model.notificaciones.EnvioEmailCompra;
import unrn.model.notificaciones.EstadoEnvio;
import unrn.notificaciones.application.ProcesarCompraConfirmada;
import unrn.notificaciones.application.ProveedorTiempoFijo;
import unrn.notificaciones.application.RepositorioEnvioEmailCompraEnMemoria;
import unrn.notificaciones.application.ServicioEnvioEmailEnMemoria;
import unrn.notificaciones.application.port.ProveedorTiempo;
import unrn.notificaciones.application.port.RepositorioEnvioEmailCompra;
import unrn.notificaciones.application.port.ServicioEnvioEmail;
import unrn.notificaciones.event.CompraConfirmadaEvent;

@Testcontainers
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@Import(CompraConfirmadaRabbitListenerIT.TestConfig.class)
class CompraConfirmadaRabbitListenerIT {

    @Container
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-alpine");

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RepositorioEnvioEmailCompra repositorioEnvioEmailCompra;

    @Autowired
    private ServicioEnvioEmail servicioEnvioEmail;

    @Value("${rabbitmq.compras.events.exchange}")
    private String comprasEventsExchange;

    @Value("${rabbitmq.compras.compra.confirmada.routing-key}")
    private String compraConfirmadaRoutingKey;

    @Test
    @DisplayName("CompraConfirmadaRabbitListener mensajeValido_envioMarcadoEnviado")
    void mensajeValido_envioMarcadoEnviado() throws Exception {
        // Setup: construir evento válido
        UUID compraId = UUID.randomUUID();
        CompraConfirmadaEvent evento = eventoDeCompra(compraId);

        // Ejercitación: publicar mensaje en RabbitMQ
        rabbitTemplate.convertAndSend(comprasEventsExchange, compraConfirmadaRoutingKey, evento);

        // Verificación: el envío queda marcado como ENVIADO
        EnvioEmailCompra envio = esperarEnvioPorCompraId(compraId);

        assertNotNull(envio, "Debe haberse registrado un envío para la compra");
        assertEquals(EstadoEnvio.ENVIADO, envio.estado(), "El envío debe quedar marcado como ENVIADO");
        assertNotNull(envio.enviadoEn(), "La fecha de envío debe estar informada");
    }

    @Test
    @DisplayName("CompraConfirmadaRabbitListener falloEnvio_marcaPendienteReintento")
    void falloEnvio_marcaPendienteReintento() throws Exception {
        // Setup: configurar el servicio de envío para fallar
        ServicioEnvioEmailEnMemoria servicioEnMemoria = (ServicioEnvioEmailEnMemoria) servicioEnvioEmail;
        servicioEnMemoria.configurarFalla("Fallo simulado en el envío de email");

        UUID compraId = UUID.randomUUID();
        CompraConfirmadaEvent evento = eventoDeCompra(compraId);

        // Ejercitación: publicar mensaje en RabbitMQ
        rabbitTemplate.convertAndSend(comprasEventsExchange, compraConfirmadaRoutingKey, evento);

        // Verificación: el envío queda pendiente de reintento
        EnvioEmailCompra envio = esperarEnvioPorCompraId(compraId);

        assertNotNull(envio, "Debe haberse registrado un envío para la compra");
        assertEquals(EstadoEnvio.PENDIENTE_REINTENTO, envio.estado(),
                "El envío debe quedar marcado como PENDIENTE_REINTENTO tras un fallo");
    }

    private CompraConfirmadaEvent eventoDeCompra(UUID compraId) {
        CompraConfirmadaEvent.ItemCompraConfirmada item = new CompraConfirmadaEvent.ItemCompraConfirmada("Pelicula X",
                2, new BigDecimal("500.00"));
        CompraConfirmadaEvent.TotalCompraConfirmada total = new CompraConfirmadaEvent.TotalCompraConfirmada(
                new BigDecimal("1000.00"), BigDecimal.ZERO, "Sin descuento");

        CompraConfirmadaEvent.Data data = new CompraConfirmadaEvent.Data(
                compraId,
                "cliente@example.com",
                Instant.now(),
                List.of(item),
                total);

        return new CompraConfirmadaEvent(
                UUID.randomUUID(),
                CompraConfirmadaEvent.EVENT_TYPE,
                CompraConfirmadaEvent.EVENT_VERSION,
                Instant.now(),
                "ventas-service",
                data);
    }

    private EnvioEmailCompra esperarEnvioPorCompraId(UUID compraId) throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            EnvioEmailCompra envio = repositorioEnvioEmailCompra.buscarPorCompraId(compraId);
            if (envio != null) {
                return envio;
            }
            Thread.sleep(200L);
        }
        fail("No se procesó el evento para la compraId " + compraId);
        return null;
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        RepositorioEnvioEmailCompra repositorioEnvioEmailCompra() {
            return new RepositorioEnvioEmailCompraEnMemoria();
        }

        @Bean
        ServicioEnvioEmail servicioEnvioEmail() {
            return new ServicioEnvioEmailEnMemoria();
        }

        @Bean
        ProveedorTiempo proveedorTiempo() {
            return new ProveedorTiempoFijo(List.of(Instant.parse("2024-01-01T10:00:00Z")));
        }

        @Bean
        ProcesarCompraConfirmada procesarCompraConfirmada(
                RepositorioEnvioEmailCompra repositorioEnvioEmailCompra,
                ServicioEnvioEmail servicioEnvioEmail,
                ProveedorTiempo proveedorTiempo) {
            return new ProcesarCompraConfirmada(repositorioEnvioEmailCompra, servicioEnvioEmail, proveedorTiempo, 3);
        }
    }
}
