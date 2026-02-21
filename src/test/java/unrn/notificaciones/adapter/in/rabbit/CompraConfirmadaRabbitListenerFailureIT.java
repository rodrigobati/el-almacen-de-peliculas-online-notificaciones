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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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

/**
 * Integration tests for failure scenarios.
 * This file contains two test classes that start independent Spring contexts
 * with different configuration for max retries.
 */

@Testcontainers
class CompraConfirmadaRabbitListenerFailureIT {

    @Container
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-alpine");

    static void registerRabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @SpringBootTest
    @TestInstance(Lifecycle.PER_CLASS)
    @Import(CompraConfirmadaFailureWithRetries.TestConfig.class)
    static class CompraConfirmadaFailureWithRetries {

        @DynamicPropertySource
        static void props(DynamicPropertyRegistry registry) {
            CompraConfirmadaRabbitListenerFailureIT.registerRabbitProperties(registry);
            registry.add("test.max.reintentos", () -> "3");
        }

        @Autowired
        RabbitTemplate rabbitTemplate;

        @Autowired
        RepositorioEnvioEmailCompra repositorioEnvioEmailCompra;

        @Autowired
        ServicioEnvioEmail servicioEnvioEmail;

        @Value("${rabbitmq.compras.events.exchange}")
        String comprasEventsExchange;

        @Value("${rabbitmq.compras.compra.confirmada.routing-key}")
        String compraConfirmadaRoutingKey;

        @Test
        @DisplayName("envioFalla_yHayReintentos_marcaPendienteReintento")
        void envioFalla_yHayReintentos_marcaPendienteReintento() throws Exception {
            // Setup: servicio de envío falla
            ServicioEnvioEmailEnMemoria servicio = (ServicioEnvioEmailEnMemoria) servicioEnvioEmail;
            servicio.configurarFalla("SMTP_DOWN");

            UUID compraId = UUID.randomUUID();
            CompraConfirmadaEvent evento = eventoDeCompra(compraId);

            // Exercise: publish event
            rabbitTemplate.convertAndSend(comprasEventsExchange, compraConfirmadaRoutingKey, evento);

            // Verify: repository eventually shows PENDIENTE_REINTENTO with reintentos == 1
            EnvioEmailCompra envio = esperarEnvioPorCompraId(compraId);
            assertNotNull(envio);
            assertEquals(EstadoEnvio.PENDIENTE_REINTENTO, envio.estado());
            assertEquals(1, envio.reintentos());
            assertEquals("SMTP_DOWN", envio.ultimoError());
        }

        private CompraConfirmadaEvent eventoDeCompra(UUID compraId) {
            CompraConfirmadaEvent.ItemCompraConfirmada item = new CompraConfirmadaEvent.ItemCompraConfirmada(
                    "Pelicula X",
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
            long deadline = System.currentTimeMillis() + 10000L; // 10s
            while (System.currentTimeMillis() < deadline) {
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
                    ProveedorTiempo proveedorTiempo,
                    @Value("${test.max.reintentos:3}") int maxReintentos) {
                return new ProcesarCompraConfirmada(repositorioEnvioEmailCompra, servicioEnvioEmail, proveedorTiempo,
                        maxReintentos);
            }
        }
    }

    @SpringBootTest
    @TestInstance(Lifecycle.PER_CLASS)
    @Import(CompraConfirmadaFailureNoRetries.TestConfig.class)
    static class CompraConfirmadaFailureNoRetries {

        @DynamicPropertySource
        static void props(DynamicPropertyRegistry registry) {
            CompraConfirmadaRabbitListenerFailureIT.registerRabbitProperties(registry);
            registry.add("test.max.reintentos", () -> "1");
        }

        @Autowired
        RabbitTemplate rabbitTemplate;

        @Autowired
        RepositorioEnvioEmailCompra repositorioEnvioEmailCompra;

        @Autowired
        ServicioEnvioEmail servicioEnvioEmail;

        @Value("${rabbitmq.compras.events.exchange}")
        String comprasEventsExchange;

        @Value("${rabbitmq.compras.compra.confirmada.routing-key}")
        String compraConfirmadaRoutingKey;

        @Test
        @DisplayName("envioFalla_yNoHayMasReintentos_marcaFalloPermanente")
        void envioFalla_yNoHayMasReintentos_marcaFalloPermanente() throws Exception {
            ServicioEnvioEmailEnMemoria servicio = (ServicioEnvioEmailEnMemoria) servicioEnvioEmail;
            servicio.configurarFalla("SMTP_DOWN");

            UUID compraId = UUID.randomUUID();
            CompraConfirmadaEvent evento = eventoDeCompra(compraId);

            rabbitTemplate.convertAndSend(comprasEventsExchange, compraConfirmadaRoutingKey, evento);

            EnvioEmailCompra envio = esperarEnvioPorCompraId(compraId);
            assertNotNull(envio);
            assertEquals(EstadoEnvio.FALLO_PERMANENTE, envio.estado());
            assertEquals(1, envio.reintentos());
            assertEquals("SMTP_DOWN", envio.ultimoError());
        }

        private CompraConfirmadaEvent eventoDeCompra(UUID compraId) {
            CompraConfirmadaEvent.ItemCompraConfirmada item = new CompraConfirmadaEvent.ItemCompraConfirmada(
                    "Pelicula X",
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
            long deadline = System.currentTimeMillis() + 10000L; // 10s
            while (System.currentTimeMillis() < deadline) {
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
                    ProveedorTiempo proveedorTiempo,
                    @Value("${test.max.reintentos:1}") int maxReintentos) {
                return new ProcesarCompraConfirmada(repositorioEnvioEmailCompra, servicioEnvioEmail, proveedorTiempo,
                        maxReintentos);
            }
        }
    }
}
